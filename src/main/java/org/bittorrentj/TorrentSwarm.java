package org.bittorrentj;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

import org.bittorrentj.event.ClientIOFailedEvent;
import org.bittorrentj.event.Event;
import org.bittorrentj.exceptions.*;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.*;
import org.bittorrentj.message.exceptions.UnsupportedExtendedMessageFoundException;
import org.bittorrentj.message.field.Hash;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.torrent.MetaInfo;

/**
 * Created by bedeho on 30.08.2014.
 */
public class TorrentSwarm extends Thread {

    /**
     * Multiplexing selector for swarm connectivity
     */
    private Selector selector;

    /**
     * Info_hash for this torrent of corresponding to this torrent swarm
     */
    private Hash infoHash;

    /**
     * Torrent file meta information
     */
    private MetaInfo metaInformation;

    /**
     * Client object this swarm belongs to
     */
    private Client client;

    /**
     * Maps IP:Port to corresponding connection object
     */
    private HashMap<InetSocketAddress, Connection> connections;

    /**
     * There maximum number of connections allowed for this torrent swarm
     */
    private int maxNumberOfConnections;

    /**
     * The maximum number of peers to upload to simultaneously
     */
    private int maxNumberOfUploads;

    /**
     * Maximum net upload speed
     */
    private int maxUploadSpeed;

    /**
     * Maps the name to the extension for all installed extensions for this swarm.
     */
    private HashMap<Integer, Extension> activeClientExtensions;

    /**
     * Maximum amount of time (ms) before no data from peer
     * results in disconnecting it
     */
    private final static int MAX_SILENCE_DURATION = 3600*1000;

    /**
     * If time from last sending of any data to peer exceeds this
     * a amount of time (ms), then send a keep-alive message.
     *
     * Wiki says:  Peers may close a connection if they receive no
     * messages (keep-alive or any other message) for a certain period of time,
     * so a keep-alive message must be sent to maintain the connection alive
     * if no command have been sent for a given amount of time.
     * This amount of time is generally two minutes.
     */
    private final static int KEEP_ALIVE_INTERVAL = 60*1000;

    /**
     * Received bit field message. Starts out as null, and
     * is evenutally set when messaga arrives. If the metadata
     * is known when the message arrives, then piece availability
     * of the peer can be set, otherwise this availability must
     * be set by the BEP9 extension which also sets the metadata.
     */
    private BitField receivedBitField;

    // diskworker?

    // State

    /**
     * Availability of pieces for this torrent.
     */
    private boolean [] globalPieceAvailability;

    public TorrentSwarm(Hash infoHash, MetaInfo metaInformation, HashMap<Integer, Extension> activeClientExtensions) {

        this.infoHash = infoHash;
        this.metaInformation = metaInformation;
        this.activeClientExtensions = activeClientExtensions;
        this.receivedBitField = null;

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            System.out.println("How can we possibly end up here"); // log later or something
        }

        //
        //this.globalPieceAvailability =

                //Extension[i].addTorrent(this or info_hash, does it really need to know - can extension trust info, is it thread safe? ut_metdata would need to even modify!!!);
    }

    @Override
    public void run() {

        while(true) {

            // how to stop???

            // Process network channel events
            processNetwork();

            /**
             * processOther things??
             *
             *
             // So we dealt with all those messages, anything else we do...

             // process event where a piece which was finally written to disk!!
             *
             */


            /**
             *
             *             // Disconnect connections which have taken to long to talk to us,
             // send keep-alive if we have not written anything in a while
             long nowDateInMs = new Date().getTime();

             // Close if it has taken more time than upper limit,
             // otherwise send keep-alive if we have not written in a while
             if(nowDateInMs - connection.getTimeLastDataReceived().getTime() > MAX_SILENCE_DURATION)
             closeConnection(connection);
             else if(nowDateInMs - connection.getTimeLastDataSent().getTime() > KEEP_ALIVE_INTERVAL)
             connection.enqueueMessageForSending(new KeepAlive());
             *
             */

            // if we have to few connections now, how do we get more peers?




        }
    }

    private void processNetwork() {

        // Get next channel event
        int numberOfUpdatedKeys = 0;

        try {
            numberOfUpdatedKeys = selector.select(Client.MAX_SELECTOR_DELAY);
        } catch(IOException e) {
            System.out.println("what can causes us to come here????"); // <= logg later in log4j
        }

        // Iterate keys and process read/write events
        Iterator i = selector.selectedKeys().iterator();

        while (i.hasNext()) {

            SelectionKey key = (SelectionKey) i.next();

            // Get connection
            Connection connection = (Connection)key.attachment();

            // Remove key from selected key set
            selector.selectedKeys().remove(key);

            try {

                // Ready to be read
                if (key.isReadable()) {

                    // Read from channel
                    connection.readMessagesFromChannel();

                    // Process new message
                    MessageWithLengthField m;
                    while((m = connection.getNextReceivedMessage()) != null) {

                        try {
                            processMessage(m, connection);
                        } catch (UnsupportedExtendedMessageFoundException e) {
                            closeConnection(connection);
                            //return;
                            // or just ignore ?
                        } catch (ReceivedBitFieldMoreThanOnce e) {
                            closeConnection(connection);
                            //return;
                            // or just ignore ?
                        } catch (InvalidPieceIndexInHaveMessage e) {
                            closeConnection(connection);    
                            //return;
                            // or just ignore ?
                        } catch (InvalidBitFieldMessage e) {

                        }
                    }
                }

                // Ready to be written to
                if (key.isWritable())
                    connection.writeMessagesToChannel();

            } catch (IOException e) {
                // Notify client
                sendEvent(new ClientIOFailedEvent(e));

                //// closeConnection(connection); ????

            } catch (MessageToLargeForNetworkBufferException e) {

            } catch (InvalidMessageReceivedException e) {

            } finally {
                // Close connection with this peer
                closeConnection(connection);
            }
        }
    }

    /**
     * Process the advent of the given message on the given connection
     * @param m message
     * @param connection connection
     */
    private void processMessage(MessageWithLengthField m, Connection connection) throws
            UnsupportedExtendedMessageFoundException,
            ReceivedBitFieldMoreThanOnce,
            InvalidPieceIndexInHaveMessage,
            InvalidBitFieldMessage {

        // should we move this method into connection class perhaps? since it is on a per connection basis, and then pass in suplementary info like metainfo etc.??
        // write out, then see.




        /**
         * Does it have id?, if not, then its just a keep-alive message,
         * and we do nothing about them here.
         */
        if(m instanceof MessageWithLengthAndIdField) {

            MessageId id = ((MessageWithLengthAndIdField) m).getId();

            switch (id) {

                case CHOKE:

                    // Peer just choked us
                    connection.getPeerState().setChoking(true);

                    break;
                case UNCHOKE:

                    // Peer just unchoked us
                    connection.getPeerState().setChoking(false);

                    break;
                case INTERESTED:

                    // Peer is interested in getting piece from us
                    connection.getPeerState().setInterested(true);

                    break;
                case NOT_INTERESTED:

                    // Peer is not interested in getting piece from us
                    connection.getPeerState().setInterested(false);

                    break;
                case HAVE:

                    // Peer has a new piece

                    // Do we have metainfo yet
                    if(isMetaInformationKnown()) {

                        Have haveMessage = (Have)m;

                        // Check that have message is indeed valid
                        int numberOfPiecesInTorrent = metaInformation.getNumberOfPiecesInTorrent();
                        if(haveMessage.validate(numberOfPiecesInTorrent))
                            connection.getPeerState().isPieceAvailable(haveMessage.getPieceIndex()); // and alter availability based on it
                        else
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
                        if(isMetaInformationKnown()) {

                            int numberOfPieces = metaInformation.getNumberOfPiecesInTorrent();

                            // Check that message is valid, given the number of pieces in torrent
                            if(!receivedBitField.validateBitField(numberOfPieces))
                                throw new InvalidBitFieldMessage(receivedBitField);
                            else // and then alter peer piece availability based
                                connection.getPeerState().setPieceAvailability(receivedBitField.getBooleanBitField(numberOfPieces));
                        }
                    }

                    break;
                case REQUEST:

                    // send them a piece?

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
                        boolean isThisFirstExtendedHandshake = connection.getPeerState().getExtendedHandshake() == null;

                        // if so, then for each extension registered in message, that we support, initialize extension
                        if(isThisFirstExtendedHandshake) {

                            HashMap<Integer, String> enabledExtensions = extendedHandshake.getEnabledExtensions();
                            for (int extensionId : enabledExtensions.keySet()) {

                                // Get name of extension
                                String name = enabledExtensions.get(extensionId);

                                // If we have this extension enabled, then call initialization routine
                                if (activeClientExtensions.containsKey(name))
                                    activeClientExtensions.get(name).init(connection);
                            }
                        }

                        // Save (new) handshake in peer state
                        connection.getPeerState().setExtendedHandshake(extendedHandshake);

                    } else if(activeClientExtensions.containsKey(extendedMessage.getExtendedMessageId())) // If we support this extension, then process it
                        activeClientExtensions.get(extendedMessage.getExtendedMessageId()).processMessage(connection, extendedMessage);
                    else // we don't support this
                        throw new UnsupportedExtendedMessageFoundException((byte)extendedMessage.getExtendedMessageId());

                    break;
                default:
                    //throw new Exception("Coding error: processMessage switch does not cover all messages."); // we should never come here
            }
         }

    }

    /**
     * after creating a message:
     * ------------------------
     * if its a bitfield, check that it matches the number of pieces,if we know that number
     * throw new InvalidMessageLengthFieldException(bitfield.length, numberOfPiecesInTorrent);
     *
     * if its a piece, check that: Check that it is non-negative and also not to large
     if throw new InvalidPieceIndexInHaveMessage(this.pieceIndex, numberOfPiecesInTorrent);
     *
     *
     *
     *
     * if its and extended message, then call upon the correct handler, and it needs to check
     * that the length field was correct.
     */

    /**
     *
     * @param connection
     */
    synchronized public void closeConnection(Connection connection) {

        // called from process*Network, but perhaps also from Client
        // in response to command, figure this out, and whether we need
        // synching. this informs whetehr routine should be public or private


        // actually close connection.close -> what does this do with selector?
        // remove from connections hashmap
        // what to do about various buffer is in connection, and also in diskmanager?



    }

    /**
     * Number of connections in swarm at present
     * @return
     */
    synchronized public int getNumberOfPeers() {
        return connections.size();
    }

    /**
     * Indicates whether this swarm accepts more connections
     * @return
     */
    synchronized public boolean acceptsMoreConnections() {
        return connections.size() > maxNumberOfConnections;
    }

    /**
     *
     */
    synchronized public void addConnection(SocketChannel channel, Handshake m) {

        //register channel with selector with both opread and opwrite

        // new connection (PeerState clientState, PeerState peerState, HashMap<Integer, Extension> activeClientExtensions)

        /*
        public synchronized addPeer(socket, peer_id, reserved) : for when a peer connects and gives you all this info :called by, at thisp point the peer
        p = new Peer(socket, peer_id, reserved, ExtensionsManager [])
        peers.add(p)
        p.start()
        */

        // new peer state (Handshake m, HashMap<Integer, Extension> activeExtensions, boolean [] clientPieceAvailability
    }

    /**
     *
     * @return
     */
    synchronized public void getConnections() {

    }

    /**
     *
     * @return
    synchronized public ConnectionInformation getConnectionInformation() {

    }
     */

    /**
     *
     */
    synchronized public void removeConnection(InetSocketAddress address) {
        // complicated
    }

    /**
     * Registers and event with the management object
     * @param e
     */
    private void sendEvent(Event e) {
        client.getB().registerEvent(e);
    }

    /**
     * Checks whether we have yet learned MetaInfoHash. It may
     * require BEP9 extension to learn if we just knew info_hash to begin with.
     * @return
     */
    private boolean isMetaInformationKnown() { return metaInformation != null;}
}