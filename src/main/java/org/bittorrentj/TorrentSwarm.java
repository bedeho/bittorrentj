package org.bittorrentj;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.bittorrentj.event.ClientIOFailedEvent;
import org.bittorrentj.event.Event;
import org.bittorrentj.exceptions.InvalidMessageReceivedException;
import org.bittorrentj.exceptions.MessageToLargeForNetworkBufferException;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.*;
import org.bittorrentj.message.exceptions.UnsupportedExtendedMessageFoundException;
import org.bittorrentj.message.field.Hash;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.torrent.Info;
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
     * Torrent file metaInfo
     */
    private MetaInfo metaInfo;

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
     * Torrent meta information
     */
    private MetaInfo mInfo;

    /**
     * Torrent (meta)info hash
     */
    private Hash info_hash;

    /**
     *
     */
    private Info metadata;

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

    // diskworker?

    // State

    /**
     * Availability of pieces for this torrent.
     */
    private boolean [] globalPieceAvailability;

    public TorrentSwarm(Hash info_hash, Info metadata, HashMap<Integer, Extension> activeClientExtensions) {

        this.info_hash = info_hash;
        this.metadata = metadata;
        this.activeClientExtensions = activeClientExtensions;

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            System.out.println("How can we possibly end up here"); // log later or something
        }

        //this.globalPieceAvailability =

                //Extension[i].addTorrent(this or info_hash, does it really need to know - can extension trust info, is it thread safe? ut_metdata would need to even modify!!!);
    }

    @Override
    public void run() {

        while(true) {

            // how to stop???

            // Process network channel events
            processNetwork();
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

        // Process any potential channel events
        if(numberOfUpdatedKeys > 0) {

            // Iterate keys and process read/write events
            Iterator i = selector.selectedKeys().iterator();

            while (i.hasNext()) {

                SelectionKey key = (SelectionKey) i.next();

                // Get connection
                Connection connection = (Connection)key.attachment();

                try {

                    // Ready to be read
                    if (key.isReadable())
                        connection.readMessagesFromChannel();

                    // Ready to be written to
                    if (key.isWritable())
                        connection.writeMessagesToChannel();

                } catch (IOException e) {
                    // Notify client
                    sendEvent(new ClientIOFailedEvent(e));
                } catch (MessageToLargeForNetworkBufferException e) {

                } catch (InvalidMessageReceivedException e) {

                } finally {
                    // Close connection with this peer
                    closeConnection(connection);
                }
            }
        }

        // Process each key and corresponding connection
        for(SelectionKey key : selector.keys()) {

            // Process connection
            processKey(key);

            // Remove from selected key set if present
            if(selector.selectedKeys().contains(key))
                selector.selectedKeys().remove(key);
        }
    }

    /**
     * Process each key and corresponding connection
     * @param key
     */
    private void processKey(SelectionKey key) {

        // Get connection
        Connection connection = (Connection)key.attachment();

        // Disconnect connections which have taken to long to talk to us,
        // send keep-alive if we have not written anything in a while
        long nowDateInMs = new Date().getTime();

        // Close if it has taken more time than upper limit,
        // otherwise send keep-alive if we have not written in a while
        if(nowDateInMs - connection.getTimeLastDataReceived().getTime() > MAX_SILENCE_DURATION)
            closeConnection(connection);
        else if(nowDateInMs - connection.getTimeLastDataSent().getTime() > KEEP_ALIVE_INTERVAL)
            connection.enqueueMessageForSending(new KeepAlive());

        // Process new message
        MessageWithLengthField m;
        while((m = connection.getNextReceivedMessage()) != null) {

            try {
                processMessage(m, connection);
            } catch (UnsupportedExtendedMessageFoundException e) {
                closeConnection(connection);
                return;
                // or just ignore ?
            }
        }

        // So we dealt with all those messages, anything else we do...
    }

    /**
     * Process the advent of the given message on the given connection
     * @param m message
     * @param connection connection
     */
    private void processMessage(MessageWithLengthField m, Connection connection) throws UnsupportedExtendedMessageFoundException {

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
                    connection.getPeerState().setInterested(true);

                    break;
                case BITFIELD:

                    // Update peer state
                    //connection.getPeerState().

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
     * throw new InvalidBitFieldLengthInBitFieldMessageException(bitfield.length, numberOfPiecesInTorrent);
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

        // called from process*Netowrk, but perhaps also from Client
        // in response to command, figure this out, and whether we need
        // synching. this informs whetehr routine should be public or private


        // actually close connection.close -> what does this do with selector?
        // remove from connections hashmap
        // what to do about various buffer is in connection, and also in diskmanager?


        // if we have to few connections now, how do we get more peers?
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
     * Checks whether we have yet learned metadata. It may
     * require BEP9 extension to learn if we just knew info_hash to begin with.
     * @return
     */
    private boolean metadataIsKnown() { return metadata != null;}
}
/*
        do we have peers??? reconnect?
        findPeers()
        WHAT DOES THREAD DO, SLEEP ?
synchronized halt()
        check that we we are alredy running??
        call on peers and extensions?
private findPeers(): called either from begin() or from peerDisconnected(), perhaps this should be moved out
        What do we have available?
        MetaInfo
        go to tracker list (announce or announce-list)
        magnet link
        tracker field present
        yes
        go to tracker
        no
        attempts to connect to DHT using info_hash
public synchronized addPeer(socket, peer_id, reserved) : for when a peer connects and gives you all this info :called by, at thisp point the peer
        p = new Peer(socket, peer_id, reserved, ExtensionsManager [])
        peers.add(p)
        p.start()
public synchronized removePeer() : who calls this

public synchronized getPeerList(): who calls this
        x
private stuff only peer threads call
        read from file
        write to file
private synchronized processMessage(peer, PeerMessage): called by peers when they get a classic message (not extension), processing logic
        use settings for torrent to infer what we are doing at present
        willing to upload
        trying to download
        at what limits?
        idling
        etc
private createExtensionHandshakeDictionary() :is called each time a new handshake is made, and any time extensions are removed, to possibly reflect most recent state of affairs for all extensions, and that they may be disabled at any time
        create messageReceived dictionary
        for all i=1:Extension.length
        messageReceived.add (i, Extension[i].getExtentionName())
        create flat keys
        for all i=1:Extension.length
        add( Extension[i].getFlatKeys(this))
private synchronized peerDisconnected(peer)
        remove from peer list ( do we remove a peer which disconnected?)
        if number of connected peers < minimumNumberOfConnections:
        findPeers()
*/

/*
        TorrentSwarm()

        if peer supports BEP10, send handshake with torrent.createExtensionHandshakeDictionary()

        while(1) :
        classic message
        torrent.processMessage(this, msg)
        BEP10
        handshake (note: this may be refreshed)
        update reverse lookup table
        extended messages
        extract extended message ID x
        map x to local Extension i
        call torrent.Extension[i].processMessage(this,ByteBuffer)
        on disconnect exception
        isConnected = false
        call torrent.peerDisConnected(this)
        WHAT DOES THREAD DO, SLEEP ?
private methods for extensions to call
private sendExtendedMessage(ByteBuffer/ExtendedMessage) : allows extensions to send messages to peer
        socket.send(buffer): on disconnect do as in TorrentSwarm() and return false to Extension!! (extension can use to put peer in ban list or something)
private closeConnection()
        something?
private disableExtension() : called to disable itself, perhaps if it is not longer needed
private enableExtension() called to enable itself, however, how will this ever be called?
*/