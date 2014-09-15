package org.bittorrentj;

import org.bittorrentj.bencodej.Bencodable;
import org.bittorrentj.bencodej.BencodableByteString;
import org.bittorrentj.bencodej.BencodableInteger;
import org.bittorrentj.bencodej.BencodeableDictionary;
import org.bittorrentj.exceptions.InvalidMessageReceivedException;
import org.bittorrentj.exceptions.MessageToLargeForNetworkBufferException;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.*;
import org.bittorrentj.message.exceptions.DuplicateExtensionNameInMDictionaryException;
import org.bittorrentj.message.exceptions.MalformedMDictionaryException;
import org.bittorrentj.message.exceptions.PayloadDoesNotContainMDictionaryException;
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
     * Constructor
     */
    public Connection(PeerState clientState, PeerState peerState, HashMap<Integer, Extension> activeClientExtensions) throws DuplicateExtensionNameInMDictionaryException, PayloadDoesNotContainMDictionaryException, MalformedMDictionaryException {

        this.clientState = clientState;
        this.peerState = peerState;

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
                * that is a message greater than read buffer. If so we throw an exception.
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

                    // Is there space in buffer? otherwise throw exception
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