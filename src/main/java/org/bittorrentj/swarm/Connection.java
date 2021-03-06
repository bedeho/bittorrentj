package org.bittorrentj.swarm;

import org.bittorrentj.bencodej.BencodableByteString;
import org.bittorrentj.bencodej.BencodeableDictionary;
import org.bittorrentj.exceptions.*;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.*;
import org.bittorrentj.message.exceptions.DuplicateExtensionNameInMDictionaryException;
import org.bittorrentj.message.exceptions.MalformedMDictionaryException;
import org.bittorrentj.message.exceptions.PayloadDoesNotContainMDictionaryException;
import org.bittorrentj.message.exceptions.UnsupportedExtendedMessageFoundException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.stream.InputMessageStream;
import org.bittorrentj.message.stream.OutputMessageStream;
import org.bittorrentj.swarm.exception.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by bedeho on 30.08.2014.
 */
public class Connection {

    /**
     * Swarm to which this connection belongs.
     */
    //private Swarm swarm;

    /**
     * State of client side of connection.
     *
     * Ideally, this state should be the same for all connections,
     * but there may be cases where more flexibility is needed.
     *
     * I THINK I SHOULD PERHAPS REMOVE THIS, SINCE MANY FIELDS
     * ARE NOT USEFUL, AND THE ONCE THAT ARE CAN JUST BE MAINTAINED MANUALLY
     *
     */
    private PeerState clientState;

    /**
     * State of peer side of connection
     */
    private PeerState peerState;

    /**
     * Indicates whether this peer is unchoked optimistically,
     * hence this variable should only be true if peerState.choked is true.
     */
    private boolean optimisticallyUnchoked;

    /**
     * Stream used to asynchronously read and write messages
     * to channel.
     */
    private InputMessageStream inputStream;

    /**
     * Stream used to asynchronously read and write messages
     * to channel.
     */
    private OutputMessageStream outputStream;

    /**
     * TheShad0W (BitTornado creator) claimed that putting
     * request messages in same buffer as piece messages
     * would cause latency high request message latency.
     */
    private OutputMessageStream nonPieceOutputStream;

    /**
     * Counts the amount of data received in piece messages
     * which we actually needed, that is raw block content we
     * did not already have.
     *
     * The reason this quantity is required, on top of
     * the rawDownloadCounter variable, is because
     * that variable counts everything, including flooding
     * and pieces we don't need.
     */
    private int validPieceDownloadCounter;

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
     * is eventually set when message arrives. If the metadata
     * is known when the message arrives, then piece availability
     * of the peer can be set, otherwise this availability must
     * be set by the BEP9 extension which also sets the metadata.
     */
    private BitField receivedBitField;

    /**
     * Constructor
     * @param swarm
     * @param clientState
     * @param peerState
     * @param inputStream
     * @param outputStream
     * @param activeClientExtensions
     * @throws DuplicateExtensionNameInMDictionaryException
     * @throws PayloadDoesNotContainMDictionaryException
     * @throws MalformedMDictionaryException
     */
    public Connection(Swarm swarm, PeerState clientState, PeerState peerState, InputMessageStream inputStream, OutputMessageStream outputStream, HashMap<Integer, Extension> activeClientExtensions) throws DuplicateExtensionNameInMDictionaryException, PayloadDoesNotContainMDictionaryException, MalformedMDictionaryException {

        this.swarm = swarm;
        this.clientState = clientState;
        this.peerState = peerState;
        this.outputStream = outputStream;
        this.nonPieceOutputStream = nonPieceOutputStream;
        this.inputStream = inputStream;
        this.validPieceDownloadCounter = 0;
        this.activeClientExtensions = activeClientExtensions;
        this.receivedBitField = null;
        this.readMessagesQueue = new LinkedList<MessageWithLengthField>();
        this.writeMessagesQueue = new LinkedList<MessageWithLengthField>();

        /**
         * If we have knowledge about piece availability, e.g. because
         * are are resuming a download, then send a bitfield message.
         */
        if(clientState.isPieceAvailabilityKnown())
            enqueueMessageForSending(new BitField(clientState.getAdvertisedPieceAvailability()));

        // ALTER LOGIC SO THAT MESSAGE PARSING RESPECTS WHETHER CLIENT SIDE OF CONNECTION ACTUALLY HAS bep10 ENABLED.

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
     * Is called when OP_READ is registered with channel and put full messages in readMessagesQueue.
     */
    public int readMessagesFromChannel() throws IOException, MessageToLargeForNetworkBufferException, InvalidMessageReceivedException {
        return inputStream.read(readMessagesQueue);
    }

    /**
     * Is called when OP_WRITE is registered with channel and write messages from writeMessagesQueue.
     */
    public int writeMessagesToChannel() throws IOException, MessageToLargeForNetworkBufferException {
        return outputStream.write(writeMessagesQueue);
    }

    public void processReadMessageQueue() throws
            InvalidBitFieldMessage,
            ReceivedBitFieldMoreThanOnce,
            InvalidPieceIndexInHaveMessage,
            UnsupportedExtendedMessageFoundException,
            ExtendedMessageReceivedWithoutEnabling {

        // Process new message
        MessageWithLengthField m;
        while((m = readMessagesQueue.poll()) != null)
            processMessage(m);
    }

    /**
     * Process the advent of the given message on the given connection
     * @param m message
     */

    // I SUSPECT THIS NEEDS TO BE MOVED BACK INTO SWARM....
    private void processMessage(MessageWithLengthField m) throws
            UnsupportedExtendedMessageFoundException,
            ReceivedBitFieldMoreThanOnce,
            InvalidPieceIndexInHaveMessage,
            InvalidBitFieldMessage, ExtendedMessageReceivedWithoutEnabling {

        /**
         * Does it have id?, if not, then its just a keep-alive message,
         * and we do nothing about them here.
         */
        if(m instanceof MessageWithLengthAndIdField) {

            MessageId id = ((MessageWithLengthAndIdField) m).getId();

            switch (id) {

                case CHOKE: // Peer just choked us

                    // Set choking state in peer state to true
                    peerState.setChoking(true);

                    break;
                case UNCHOKE: // Peer just unchoked us

                    // Set choking state in peer state to false
                    peerState.setChoking(false);

                    break;
                case INTERESTED: // Peer is interested in getting piece from us

                    // Set interested state in peer state to true
                    peerState.setInterested(true);

                    break;
                case NOT_INTERESTED: // Peer is not interested in getting piece from us

                    // Set interested state in peer state to false
                    peerState.setInterested(false);

                    break;
                case HAVE: // Peer has a new piece

                    // Do we have metainfo yet
                    if(swarm.isMetaInformationKnown()) {

                        Have haveMessage = (Have)m;

                        // Check that have message is indeed valid
                        int numberOfPiecesInTorrent = swarm.getMetaInformation().getNumberOfPiecesInTorrent();
                        if(haveMessage.validate(numberOfPiecesInTorrent)) {

                            int pieceIndex = haveMessage.getPieceIndex();

                            // Alter availability of piece
                            peerState.alterPieceAvailability(pieceIndex, true);

                            // Update interested state i required
                            alterMyInterestedStateIfNeeded(pieceIndex);

                        } else
                            throw new InvalidPieceIndexInHaveMessage(haveMessage.getPieceIndex(), numberOfPiecesInTorrent); // or throw exceptions if invalid
                    }

                    break;
                case BITFIELD: // Peer announces what pieces it has

                    // If we have already received this message, we raise an exception, since it should only be sent once.
                    if(receivedBitField != null)
                        throw new ReceivedBitFieldMoreThanOnce();
                    else {

                        // otherwise save it the first time
                        receivedBitField = (BitField)m;

                        // if meta information is known, then update piece availability
                        if(swarm.isMetaInformationKnown()) {

                            int numberOfPieces = swarm.getMetaInformation().getNumberOfPiecesInTorrent();

                            // Check that message is valid, given the number of pieces in torrent
                            if(!receivedBitField.validateBitField(numberOfPieces))
                                throw new InvalidBitFieldMessage(receivedBitField);
                            else { // and then alter peer piece availability based
                                peerState.setAdvertisedPieceAvailability(receivedBitField.getBooleanBitField(numberOfPieces));

                                // If we are not presently interested, then check if any pieces in this bitfield
                                // warrant changing client state.
                                if(!clientState.isInterested()) {

                                    // Iterate bitfield
                                    for (int pieceIndex = 0; pieceIndex < numberOfPieces; pieceIndex++) {

                                        // Update interested state i required, and if we change state to positive
                                        // then we are done, no need to keep resending.
                                        if (alterMyInterestedStateIfNeeded(pieceIndex))
                                            break;
                                    }
                                }
                            }
                        }
                    }

                    break;
                case REQUEST: // Peer requests piece block

                    // Add request to list of unprocessed requests from this peer
                    peerState.registerRequest((Request)m);

                    break;
                case PIECE: // Peer sends us a piece block

                    Piece pieceMessage = (Piece)m;

                    int sizeOfNewData = swarm.sizeOfBlockWeDoNotHave(pieceMessage.getIndex(), pieceMessage.getBegin() , pieceMessage.getBlock().length);

                    // Did we actually get any fresh data?
                    if(sizeOfNewData > 0) {

                        // Add the new part as part of download counter
                        validPieceDownloadCounter += sizeOfNewData;

                        // Process piece: save, send out cancelations etc.
                        swarm.processNewPieceMessage(pieceMessage);
                    }

                    break;
                case CANCEL: // Peer wants to cancel a previous request

                    // Remove the request message corresponding to this cancel message
                    // from the list of unprocessed requests form this peer.
                    peerState.unregisterRequest(((Cancel) m).toRequestMessage());

                    break;
                case PORT: // DHT port announcement

                    // Call dhtj

                    break;
                case EXTENDED: // Peer sent a BEP10 extended message

                    if(!clientState.extensionProtocolIsUsed())
                        throw new ExtendedMessageReceivedWithoutEnabling();

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
     * If
     * a) we are downloading
     * b) this is a piece we need
     * c) we are presently not interested in this peer
     * then alter state of client side of connection,
     * and send an INTERESTED message to my peer.
     * @param pieceIndex
     * @return true if we sent altered state and sent message
     */
    public boolean alterMyInterestedStateIfNeeded(int pieceIndex) {

        if(swarm.getSwarmState() != Swarm.SwarmState.OFF &&
           clientState.isPieceAvailabilityKnown() && clientState.isPieceAvailable(pieceIndex) &&
           !clientState.isInterested()) {

            // alter interested state
            clientState.setInterested(true);

            // and let peer know.
            enqueueMessageForSending(new Interested());

            return true;
        } else
            return false;
    }

    /**
     * Grabs a message from the front of the read queue of the connection,
     * or return null if queue empty.
     * @return message

    public MessageWithLengthField getNextReceivedMessage() {
        return readMessagesQueue.poll();
    }
    */

    public PeerState getPeerState() {
        return peerState;
    }

    public PeerState getClientState() {
        return clientState;
    }

    public int getUploadRate() {

    }

    public int getDownloadRate() {

    }
}