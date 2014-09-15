package org.bittorrentj;

import org.bittorrentj.bencodej.Bencodable;
import org.bittorrentj.bencodej.BencodableByteString;
import org.bittorrentj.bencodej.BencodableInteger;
import org.bittorrentj.bencodej.BencodeableDictionary;
import org.bittorrentj.exceptions.*;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.*;
import org.bittorrentj.message.exceptions.DuplicateExtensionNameInMDictionaryException;
import org.bittorrentj.message.exceptions.MalformedMDictionaryException;
import org.bittorrentj.message.exceptions.PayloadDoesNotContainMDictionaryException;
import org.bittorrentj.message.exceptions.UnsupportedExtendedMessageFoundException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.PeerId;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Date;

/**
 * Created by bedeho on 30.08.2014.
 */
public class Connection {

    /**
     * TorrentSwarm to which this connection belongs.
     */
    private TorrentSwarm swarm;

    /**
     * State of client side of connection.
     * Ideally, this state should be the same for all connections,
     * but there may be cases where more flexibility is needed.
     */
    private PeerState clientState;

    /**
     * State of peer side of connection
     */
    private PeerState peerState;

    /**
     * Date and time when data was written to channel for this connection
     */
    private Date timeLastDataSent;

    /**
     * Date and time when data was read from channel for this connection
     */
    private Date timeLastDataReceived;

    /**
     * Channel for this connection
     */
    private SocketChannel channel;

    /**
     * Read buffer for network
     */
    private ByteBuffer networkReadBuffer;

    /**
     * Size (bytes) of networkReadBuffer
     */
    public final static int NETWORK_READ_BUFFER_SIZE = 10 * 1024 * 1024;

    /**
     * Position in networkReadBuffer where data
     * begins which has not been processed by readMessagesFromChannel() and
     * subsequently turned into a message in readMessagesQueue
     */
    private int startPositionOfDataInReadBuffer;
    private int copyAtEdgeEvent; // purely a performance statistic to assess whether circluar buffer is needed

    /**
     * Write buffer for network
     */
    private ByteBuffer networkWriteBuffer;

    /**
     * Size (bytes) of networkWriteBuffer
     */
    private final static int NETWORK_WRITE_BUFFER_SIZE = 10 * 1024 * 1024;

    /**
     * The number of bytes in the write buffer.
     * We cannot use hasRemaining on buffer, since that
     * can also be state when buffer is perfectly empty.
     */
    private int bytesInWriteBuffer;

    /**
     * Queue of messages which have been read but not processed
     */
    private LinkedList<MessageWithLengthField> readMessagesQueue;

    /**
     * Queue of messages which have been read but not processed
     */
    private LinkedList<MessageWithLengthField> writeMessagesQueue;

    /**
     * Mapping from id to extension for all active BEP10 extensions
     * installed on client.
     */
    private HashMap<Integer, Extension> activeClientExtensions;

    /**
     * Received bit field message. Starts out as null, and
     * is evenutally set when messaga arrives. If the metadata
     * is known when the message arrives, then piece availability
     * of the peer can be set, otherwise this availability must
     * be set by the BEP9 extension which also sets the metadata.
     */
    private BitField receivedBitField;

    /**
     * Constructor
     */
    public Connection(TorrentSwarm swarm, PeerState clientState, PeerState peerState, HashMap<Integer, Extension> activeClientExtensions) throws DuplicateExtensionNameInMDictionaryException, PayloadDoesNotContainMDictionaryException, MalformedMDictionaryException {

        this.swarm = swarm;
        this.clientState = clientState;
        this.peerState = peerState;

        this.activeClientExtensions = activeClientExtensions;

        this.receivedBitField = null;

        this.networkWriteBuffer = ByteBuffer.allocateDirect(NETWORK_WRITE_BUFFER_SIZE);
        this.networkReadBuffer = ByteBuffer.allocateDirect(NETWORK_READ_BUFFER_SIZE);
        this.readMessagesQueue = new LinkedList<MessageWithLengthField>();
        this.writeMessagesQueue = new LinkedList<MessageWithLengthField>();

        this.bytesInWriteBuffer = 0;
        this.startPositionOfDataInReadBuffer = 0;
        this.copyAtEdgeEvent = 0;

        /**
         * If we have knowledge about piece availability, e.g. because
         * are are resuming a download, then send a bitfield message.
         */
        if(clientState.isPieceAvailabilityKnown())
            enqueueMessageForSending(new BitField(clientState.getPieceAvailability()));

        /**
         * If both client and peer support BEP10, then we also send extended handshake.
         */
        if(clientState.extensionProtocolIsUsed() && peerState.extensionProtocolIsUsed()) {

            // Generate message
            ExtendedHandshake m = buildExtendedHandshake();

            // Queue up message for sending
            enqueueMessageForSending(m);
        }
    }

    /**
     * Attempts to read from channel when OP_READ is registered,
     * and put full messages in readMessagesQueue.
     */
    public void readMessagesFromChannel() throws IOException, MessageToLargeForNetworkBufferException, InvalidMessageReceivedException {

        // The number of bytes read from socket into read buffer in last iteration of next loop
        int numberOfBytesRead;

        /**
         * Read from channel into network read buffer so long as we can,
         * this may be possible multiple times if read buffer is comparatively
         * smaller compared to kernel socket buffer and incoming traffic/bandwidth.
         */
        while((numberOfBytesRead = channel.read(networkReadBuffer)) > 0) {

            // Note where in buffer data ends
            int endPositionOfDataInReadBuffer = networkReadBuffer.position();

            // Each iteration attempts to read one message from channel buffer
            int numberOfUnconsumedBytes;
            while (true) {

                // Remaining space in buffer which has not been processed into a message
                numberOfUnconsumedBytes = endPositionOfDataInReadBuffer - startPositionOfDataInReadBuffer;

                /**
                * Do we have enough space in buffer for length field of new message?,
                * if not we are done reading from buffer, and we stop without
                * advancing buffer read position.
                */
                if (numberOfUnconsumedBytes < MessageWithLengthField.LENGTH_FIELD_SIZE)
                    break;

                // Read length field of new message in as four byte big-endian integer, without altering buffer position
                int messageIdAndPayloadSize = networkReadBuffer.getInt(startPositionOfDataInReadBuffer);

                // Get total size of message as claimed by length field
                int totalMessageSize = MessageWithLengthField.LENGTH_FIELD_SIZE + messageIdAndPayloadSize;

                /**
                * Check if peer is attempting to send a message we can never process,
                * that is a message greater than read buffer. If so we throw an exceptions.
                */
                if (totalMessageSize > NETWORK_READ_BUFFER_SIZE)
                    throw new MessageToLargeForNetworkBufferException(totalMessageSize, NETWORK_READ_BUFFER_SIZE);

                /**
                 * Check that buffer does contain a full new message,
                 * that is: <length><id><payload>.
                 * if not we are done reading from buffer, and we stop without
                 * advancing buffer read position.
                 */
                if (numberOfUnconsumedBytes >= totalMessageSize) {

                    // Wrap in a temporary read only buffer
                    ByteBuffer temporaryBuffer = networkReadBuffer.duplicate().asReadOnlyBuffer();

                    // and make sure this buffer starts where message starts
                    temporaryBuffer.position(startPositionOfDataInReadBuffer);

                    // and ends where message ends
                    temporaryBuffer.limit(startPositionOfDataInReadBuffer + totalMessageSize);

                    // Process buffer and generate new message
                    MessageWithLengthField m;

                    try {
                        m = MessageWithLengthField.create(temporaryBuffer, activeClientExtensions);
                    } catch (Exception e){
                        throw new InvalidMessageReceivedException(e);
                    }

                    // Save message in read queue
                    readMessagesQueue.add(m);

                    // Advance position in buffer
                    startPositionOfDataInReadBuffer += messageIdAndPayloadSize;
                }
            }

            // If buffer is completely consumed, then we reset it
            // to postpone copy at edge event
            if (numberOfUnconsumedBytes == 0) {
                networkReadBuffer.clear();
                startPositionOfDataInReadBuffer = 0;
            } else if (!networkReadBuffer.hasRemaining()) {

                // If unconsumed data touches buffer limit,
                // then copy to the front of the buffer.

                // We use compact() to do this, and therefor we have to
                // first set position to data we want at start of buffer
                networkReadBuffer.position(startPositionOfDataInReadBuffer);

                // and then limit is set to end of data.
                networkReadBuffer.limit(endPositionOfDataInReadBuffer);

                // Do copying
                networkReadBuffer.compact();

                // Note this compacting event, since its costly.
                copyAtEdgeEvent++;
            }

            if(numberOfBytesRead > 0);
                // log bytes written

        }
    }

    /**
     * Attempts to write to channel when OP_WRITE is registered,
     * by writing messages from writeMessagesQueue.
     */
    public void writeMessagesToChannel() throws IOException, MessageToLargeForNetworkBufferException {

        // The number of bytes written into socket from write buffer in last iteration of next loop
        int numberOfBytesWritten = 0;

        do {

            // Does buffer have anything to be written
            if(bytesInWriteBuffer == 0) {

                // Clear buffer: pos = 0, lim = cap, mark discarded
                networkWriteBuffer.clear();

                // If we still have messages to write, then fill buffer
                if(!writeMessagesQueue.isEmpty()) {

                    // Get message
                    MessageWithLengthField m = writeMessagesQueue.poll();

                    // Is there space in buffer? otherwise throw exceptions
                    int messageLength = m.getRawMessageLength();
                    if(messageLength > NETWORK_WRITE_BUFFER_SIZE)
                        throw new MessageToLargeForNetworkBufferException(messageLength, NETWORK_WRITE_BUFFER_SIZE);
                    else
                        bytesInWriteBuffer = messageLength; // buffer will be filled with message

                    // Write raw message into network buffer
                    m.writeMessageToBuffer(networkWriteBuffer);

                    // Write buffer to socket
                    networkWriteBuffer.flip(); // make limit= position and position = 0, to facilitate writing into socket
                    numberOfBytesWritten = channel.write(networkWriteBuffer);

                } else
                    numberOfBytesWritten = 0; // otherwise we are done

            } else
                numberOfBytesWritten = channel.write(networkWriteBuffer); // write what is in buffer to socket

            if(numberOfBytesWritten > 0);
                // log data sent

            // Update number of bytes in buffer
            bytesInWriteBuffer -= numberOfBytesWritten;

        } while(numberOfBytesWritten > 0); // We are done for now if we can't write to socket right now

    }

    public void processReadMessageQueue() throws InvalidBitFieldMessage, ReceivedBitFieldMoreThanOnce, InvalidPieceIndexInHaveMessage, UnsupportedExtendedMessageFoundException {

        // Process new message
        MessageWithLengthField m;
        while((m = getNextReceivedMessage()) != null)
            processMessage(m);
    }

    /**
     * Process the advent of the given message on the given connection
     * @param m message
     */
    private void processMessage(MessageWithLengthField m) throws
            UnsupportedExtendedMessageFoundException,
            ReceivedBitFieldMoreThanOnce,
            InvalidPieceIndexInHaveMessage,
            InvalidBitFieldMessage {

        /**
         * Does it have id?, if not, then its just a keep-alive message,
         * and we do nothing about them here.
         */
        if(m instanceof MessageWithLengthAndIdField) {

            MessageId id = ((MessageWithLengthAndIdField) m).getId();

            switch (id) {

                case CHOKE:

                    // Peer just choked us
                    peerState.setChoking(true);

                    break;
                case UNCHOKE:

                    // Peer just unchoked us
                    peerState.setChoking(false);

                    break;
                case INTERESTED:

                    // Peer is interested in getting piece from us
                    peerState.setInterested(true);

                    break;
                case NOT_INTERESTED:

                    // Peer is not interested in getting piece from us
                    peerState.setInterested(false);

                    break;
                case HAVE:

                    // Peer has a new piece

                    // Do we have metainfo yet
                    if(swarm.isMetaInformationKnown()) {

                        Have haveMessage = (Have)m;

                        // Check that have message is indeed valid
                        int numberOfPiecesInTorrent = swarm.getMetaInformation().getNumberOfPiecesInTorrent();
                        if(haveMessage.validate(numberOfPiecesInTorrent)) {
                            peerState.alterPieceAvailability(haveMessage.getPieceIndex(), true); // and alter availability based on it

                            // If we are downloading, and this was piece we need, then alter interestedness.



                        } else
                            throw new InvalidPieceIndexInHaveMessage(haveMessage.getPieceIndex(), numberOfPiecesInTorrent); // or throw exceptions if invalid
                    }

                    break;
                case BITFIELD:

                    // Peer announces what pieces it has

                    // If we have already received this message, we raise an exception, since it should only be sent once.
                    if(receivedBitField != null)
                        throw new ReceivedBitFieldMoreThanOnce();
                    else {

                        // otherwise save it the first time
                        receivedBitField = (BitField)m;

                        // If meta information is known, then update piece availability
                        if(swarm.isMetaInformationKnown()) {

                            int numberOfPieces = swarm.getMetaInformation().getNumberOfPiecesInTorrent();

                            // Check that message is valid, given the number of pieces in torrent
                            if(!receivedBitField.validateBitField(numberOfPieces))
                                throw new InvalidBitFieldMessage(receivedBitField);
                            else { // and then alter peer piece availability based
                                peerState.setPieceAvailability(receivedBitField.getBooleanBitField(numberOfPieces));

                                // If we are downloading, and this was piece we need, then alter interestedness.

                            }
                        }
                    }

                    break;
                case REQUEST:

                    // We ignore the request unless we are in the ON state
                    if(swarm.getSwarmState() == TorrentSwarm.TorrentSwarmState.OFF)
                        return;



                    break;
                case PIECE:

                    // send out have?

                    break;
                case CANCEL:

                    // dont upload to peer or something?

                    break;
                case PORT:

                    // this is for dht client
                    System.out.print("later");

                    break;

                case EXTENDED:

                    Extended extendedMessage = (Extended)m;

                    // If this is this is extended handshake, then register it
                    if(extendedMessage instanceof ExtendedHandshake){

                        ExtendedHandshake extendedHandshake = (ExtendedHandshake)m;

                        // Is this first extended handshake?
                        boolean isThisFirstExtendedHandshake = peerState.getExtendedHandshake() == null;

                        // if so, then for each extension registered in message, that we support, initialize extension
                        if(isThisFirstExtendedHandshake) {

                            HashMap<Integer, String> enabledExtensions = extendedHandshake.getEnabledExtensions();
                            for (int extensionId : enabledExtensions.keySet()) {

                                // Get name of extension
                                String name = enabledExtensions.get(extensionId);

                                // If we have this extension enabled, then call initialization routine
                                if (activeClientExtensions.containsKey(name))
                                    activeClientExtensions.get(name).init(this);
                            }
                        }

                        // Save (new) handshake in peer state
                        peerState.setExtendedHandshake(extendedHandshake);

                    } else if(activeClientExtensions.containsKey(extendedMessage.getExtendedMessageId())) // If we support this extension, then process it
                        activeClientExtensions.get(extendedMessage.getExtendedMessageId()).processMessage(this, extendedMessage);
                    else // we don't support this
                        throw new UnsupportedExtendedMessageFoundException((byte)extendedMessage.getExtendedMessageId());

                    break;
                default:
                    //throw new Exception("Coding error: processMessage switch does not cover all messages."); // we should never come here
            }
        }

    }



    /**
     * Add a message to the write queue of the connection.
     * @param m message
     * @throws IllegalArgumentException if a m == null
     */
    public void enqueueMessageForSending(MessageWithLengthField m) throws IllegalArgumentException {

        if(m == null)
            throw new IllegalArgumentException();
        else {
            writeMessagesQueue.add(m);

            /**
             * If we just sent an new extended handshake, then
             * update our peer state.
             */
            if(m instanceof ExtendedHandshake)
                clientState.setExtendedHandshake((ExtendedHandshake)m);
        }
    }

    /**
     * Builds an extended handshake message for the client
     * @return
     */
    ExtendedHandshake buildExtendedHandshake() throws DuplicateExtensionNameInMDictionaryException, PayloadDoesNotContainMDictionaryException, MalformedMDictionaryException {

        // Iterate extensions
        BencodeableDictionary handshakePayload = new BencodeableDictionary();
        BencodeableDictionary m = new BencodeableDictionary();

        for(int id: activeClientExtensions.keySet()) {

            // Get the extension object
            Extension e = activeClientExtensions.get(id);

            // Add key
            m.put(new BencodableByteString(""+id), new BencodableByteString(e.getName()));

            // Add extension specific keys to handshake payload
            // NB: duplicate keys will be replaced!!!
            handshakePayload.putAll(e.keysToAddToExtendedHandshake());
        }

        // Add m dictionary to payload
        handshakePayload.put(new BencodableByteString("m"), m);

        // Create message and return
        return new ExtendedHandshake(handshakePayload);
    }

    /**
     * Grabs a message from the front of the read queue of the connection,
     * or return null if queue empty.
     * @return message
     */
    public MessageWithLengthField getNextReceivedMessage() {
        return readMessagesQueue.poll();
    }

    public Date getTimeLastDataSent() {
        return timeLastDataSent;
    }

    public void setTimeLastDataSent(Date timeLastDataSent) {
        this.timeLastDataSent = timeLastDataSent;
    }

    public Date getTimeLastDataReceived() {
        return timeLastDataReceived;
    }

    public void setTimeLastDataReceived(Date timeLastDataReceived) {
        this.timeLastDataReceived = timeLastDataReceived;
    }

    public PeerState getPeerState() {
        return peerState;
    }

    public PeerState getClientState() {
        return clientState;
    }
}