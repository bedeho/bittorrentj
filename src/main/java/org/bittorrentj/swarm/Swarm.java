package org.bittorrentj.swarm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.bittorrentj.Client;
import org.bittorrentj.event.ClientIOFailedEvent;
import org.bittorrentj.event.Event;
import org.bittorrentj.exceptions.*;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.*;
import org.bittorrentj.message.exceptions.UnsupportedExtendedMessageFoundException;
import org.bittorrentj.message.field.Hash;
import org.bittorrentj.swarm.exception.*;
import org.bittorrentj.torrent.MetaInfo;

/**
 * Created by bedeho on 30.08.2014.
 */
public class Swarm extends Thread {

    /**
     * Control state given by client of this swarm.
     * ON corresponds to an effort to download the torrent,
     * or alternatively seed if it is fully downloaded.
     * OFF corresponds to rejecting all seed requests.
     */

    public enum SwarmState {
        ON_NORMAL, ON_ENDGAME, OFF;
    }

    private SwarmState swarmState;

    /**
     * When swarm leeches it picks
     *
     * , it can pick
     * the next piece among the peers which are
     * presently unchoking it to be the
     * RAREST_FIRST: most rare piece to be full among ALL peers,
     * STREAMING: piece with lowest piece index among all its incomplete pieces
     * RANDOM: a random piece
     */
    public enum LeechPolicy {
        RAREST_FIRST, STREAMING, RANDOM;
    }

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
     * All open connections in this swarm
     */
    private LinkedList<Connection> connections;

    /**
     * Maps IP:Port string to corresponding connection
     */
    private HashMap<InetSocketAddress, Connection> connectionLookupTable;

    /**
     * List of known peers in this swarm.
     * This list is filled by ... (dht,tracker list, pex,..)
     */
    private LinkedList<InetSocketAddress> knownPeers;

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
     *
     * @param infoHash
     * @param metaInformation
     * @param activeClientExtensions
     * @param swarmState
     */
    public Swarm(Hash infoHash, MetaInfo metaInformation, HashMap<Integer, Extension> activeClientExtensions, SwarmState swarmState) {

        this.infoHash = infoHash;
        this.metaInformation = metaInformation;
        this.activeClientExtensions = activeClientExtensions;
        this.swarmState = swarmState;

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            System.out.println("How can we possibly end up here"); // log later or something
        }
    }

    @Override
    public void run() {

        while(true) {

            // We may have been paused by client, if so we go to sleep
            goToSleepIfPaused();

            // Try to select some keys in the selector if possible
            int numberOfUpdatedKeys = selectUpdatedKeys(Client.MAX_SELECTOR_DELAY);

            // if there were some selected keys, then we attempt to read/write
            if(numberOfUpdatedKeys > 0) {

                LinkedList<Connection> writtenTo, readFrom;

                Set<SelectionKey> selectedKeys = selector.selectedKeys();

                try {
                    writtenTo = writeMessagesToSocketChannels(selectedKeys);
                    readFrom = readMessagesFromSocketChannels(selectedKeys);
                } catch (IOException e) {
                    // Notify client
                    sendEvent(new ClientIOFailedEvent(e));
                    // closeConnection(connection); ????
                } catch (MessageToLargeForNetworkBufferException e) {
                    // closeConnection(connection); ????
                } catch (InvalidMessageReceivedException e) {

                }

                // Clear all selected keys from selector
                selectedKeys.clear();

                // Process any read messages
                if(readFrom != null) {
                    for (Connection c : readFrom) {

                        // Process message queue just populated
                        try {
                            c.processReadMessageQueue();
                        } catch (UnsupportedExtendedMessageFoundException e) {
                            closeConnection(c);
                            //return;
                            // or just ignore ?
                        } catch (ReceivedBitFieldMoreThanOnce e) {
                            closeConnection(c);
                            //return;
                            // or just ignore ?
                        } catch (InvalidPieceIndexInHaveMessage e) {
                            closeConnection(c);
                            //return;
                            // or just ignore ?
                        } catch (InvalidBitFieldMessage e) {
                            closeConnection(c);
                            //return;
                            // or just ignore ?
                        } catch (ExtendedMessageReceivedWithoutEnabling e) {
                            closeConnection(c);
                            //return;
                            // or just ignore ?
                        }

                    }
                }
            }

            // Implement choking algorithm
            LinkedList<Connection> unchokedConnections = updateChokingState();

            //
            for(Connection c: unchokedConnections) {
                c.requestPieces();
                c.sendPieces();
            }

            // extension processing called as well
            processExtensions();

            // if we have to few connections now, how do we get more peers?
            manageConnectivity(); // should this perhaps  be the first one, so we dont need to bother with all other stuff
        }
    }

    private void goToSleepIfPaused() {

    }

    public void kill() {

    }

    /**
     * Attempt to select keys on selector.
     * @param maxDuration maximum number of milliseconds to block for a selection.
     * @return number of selected keys.
     */
    private int selectUpdatedKeys(long maxDuration) {

        int numberOfSelectedKeys;

        try {
            numberOfSelectedKeys = selector.select(maxDuration);
        } catch(IOException e) {
            System.out.println("what can causes us to come here????"); // <= logg later in log4j
            return 0; // do this?
        }

        return numberOfSelectedKeys;
    }

    /**
     * Attempts to write to all connections
     * for which the corresponding socket channel had a
     * OP_WRITE event associated with its selector key.
     * A list of connection to which at least one byte could
     * be written is returned.
     * @return list of connections to which at least one byte was written.
     */
    LinkedList<Connection> writeMessagesToSocketChannels(Set<SelectionKey> selectedKeys) throws IOException, MessageToLargeForNetworkBufferException {

        // List of connections to which at least one byte was written
        LinkedList<Connection> writtenTo = new LinkedList<Connection>();

        // Iterate keys and process OP_WRITE events
        for(SelectionKey key: selectedKeys) {

            // Write to channel if ready
            if (key.isWritable()) {

                // Get connection
                Connection connection = (Connection)key.attachment();

                // Attempt to write to channel
                int numberOfBytesWritten = connection.writeMessagesToChannel();

                // and save to list if we managed to write something
                if(numberOfBytesWritten > 0)
                    writtenTo.add(connection);
            }

        }

        return writtenTo;
    }

    /**
     * Attempts to read from all connections
     * for which the corresponding socket channel had a
     * OP_RED event associated with its selector key.
     * A list of connections from which at least one byte could
     * be read is returned.
     * @return list of connections from which at least one byte was read.
     */
    LinkedList<Connection> readMessagesFromSocketChannels(Set<SelectionKey> selectedKeys) throws IOException, MessageToLargeForNetworkBufferException, InvalidMessageReceivedException {

        // List of connections from which at least one byte was read
        LinkedList<Connection> readFrom = new LinkedList<Connection>();

        // Iterate keys and process OP_READ events
        for(SelectionKey key: selectedKeys) {

            // Write to channel if ready
            if (key.isReadable()) {

                // Get connection
                Connection connection = (Connection)key.attachment();

                // Attempt to read from channel
                int numberOfBytesRead = connection.readMessagesFromChannel();

                // and save to list if we managed to write something
                if(numberOfBytesRead > 0)
                    readFrom.add(connection);
            }

        }

        return readFrom;
    }

    private LinkedList<Connection> updateChokingState() {

        // stop downloading pieces from someone who is just super slow,or who did not respond to our request?

    }

    private void requestPieces() {



    }

    private void sendPieces() {

    }

    private void pickNextPiece() {

        // should hvae streamin and rarest first

    }

    /**
     * Gives some running time for each extension in question
     */
    private void processExtensions() {

        for(Extension e: activeClientExtensions.values()) {
            if(e.needsProcessing())
                e.processing();
        }
    }

    private void manageConnectivity() {

        // send keep alive

        // dosconnect form silent peers

        // if we have to few peers, then try to connect to known peers, and if we know to few, ask to get more peers from somewhere.

        /**
         *
         // Disconnect connections which have taken to long to talk to us,
         // send keep-alive if we have not written anything in a while
         long nowDateInMs = new Date().getTime();

         // Close if it has taken more time than upper limit,
         // otherwise send keep-alive if we have not written in a while
         if(nowDateInMs - connection.getTimeLastDataReceived().getTime() > MAX_SILENCE_DURATION)
         closeConnection(connection);
         else if(nowDateInMs - connection.getTimeLastDataTransmitted().getTime() > KEEP_ALIVE_INTERVAL)
         connection.enqueueMessageForSending(new KeepAlive());
         *
         */


        // if we have to few connections now, how do we get more peers?

    }


    /**
     *
     * @param connection
     */
    public void closeConnection(Connection connection) {

        // called from process*Network, but perhaps also from Client
        // in response to command, figure this out, and whether we need
        // synchronizing. this informs whether routine should be public or private


        // actually close connection.close -> what does this do with selector?
        // remove from connections hashmap
        // what to do about various buffer is in connection, and also in diskmanager?

    }

    /**
     * Number of connections in swarm at present
     * @return
     */
    public int getNumberOfPeers() {
        return connections.size();
    }

    /**
     * Indicates whether this swarm accepts more connections
     * @return
     */
    public boolean acceptsMoreConnections() {
        return connections.size() > maxNumberOfConnections;
    }

    /**
     *
     */
    public void addConnection(SocketChannel channel, Handshake m) {

        //register channel with selector with both opread and opwrite

        // new connection (PeerState clientState, PeerState peerState, HashMap<Integer, Extension> activeClientExtensions)

        // add to connections, and to connectionLookupTable

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
    public void getConnections() {

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
    public void removeConnection(InetSocketAddress address) {
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
    public boolean isMetaInformationKnown() { return metaInformation != null;}

    public int sizeOfBlockWeDoNotHave(int begin, int block, int length) {

    }

    public void processNewPieceMessage(Piece m) {

        // save to disk

        // Iterate all connections, and remove all outstanding
        // requests which are now satisfied with this new piece.
        // e.g. if we were in end game mode this would be important,
        // but also otherwise

        // switch into end game mode, and send requess to everyone

        // if this was last, then switch out of end game mode, if we were in it

    }

    public MetaInfo getMetaInformation() {
        return metaInformation;
    }

    public SwarmState getSwarmState() {
        return swarmState;
    }

    // who calls this
    public void setSwarmState(SwarmState swarmState) {

        // do lots of other stuff if we are indeed turning stuff off, or going to endgame


        this.swarmState = swarmState;
    }
}