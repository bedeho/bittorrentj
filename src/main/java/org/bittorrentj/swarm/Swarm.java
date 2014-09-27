package org.bittorrentj.swarm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

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
    //private Client client;

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
     * The last time (ms) the choking manager was called. Is
     * used to keep track of whether choking manager should be
     * called gain.
     */
    private Date lastChokeUpdate;

    /**
     * The greatest number of peers which are allowed to be
     * unchoked at any given time.
     */
    private final int MAX_NUMBER_OF_UNCHOKED_PEERS = 4;

    /**
     * The last time (ms) an optimistic unchoke was performed. Is
     * used to keep track of whether optimistic unchoking should be
     * performed again.
     */
    private Date lastOptimisticChokingUpdate;

    /**
     * The greatest number of peers which are allowed to be
     * unchoked at any given time.
     */
    private final int MAX_NUMBER_OF_OPTIMISTICALLY_UNCHOKED_PEERS = 1;

    /**
     * The duration (ms) between each time choking state is
     * updated by calling the choking manager. The
     * standard value is 10s.
     */
    private final int CHOKING_MANAGEMENT_PERIODE = 10*1000;

    /**
     * The duration (ms) between each time optimistic unchoking is attempted.
     * The standard value is 30s.
     */
    private final int OPTIMISTIC_UNCHOKING_MANAGEMENT_PERIODE = 30*1000;

    /**
     * Used to generate random numbers, in particular for optimistic unchoking.
     */
    private Random random;

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

        this.random = new Random();

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

            // Run choking algorithm
            LinkedList<Connection> unchokedConnections = chokingAlgorithm();

            // Iterate unchoked connections, and send requests and pieces
            for(Connection c: unchokedConnections) {
                c.requestPieces();
                c.sendPieces();
            }

            // Do general extension processing
            processExtensions();

            // Maintain connectivity to swarm by sending keep-alive,
            // and connecting to new peers if we have to few connections.
            // Also drop peers not sending keep-alive.
            manageConnectivity();
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

    /**
     * Choking algorithm is performed by doing
     * two independent time contingent updates.
     * Standard choking management simply unchokes a certain
     * number of peers which have the greatest seed rate.
     * Optimistic unchoking unchokes some other random subset
     * of peers
     * @return list of presently unchoked connections.
     */
    private LinkedList<Connection> chokingAlgorithm() {

        // List of presently unchoked connections
        LinkedList<Connection> unchoked = new LinkedList<Connection>();

        // Get present time to compare against previous unchoking time stamp
        long presentTime = new Date().getTime();

        // Check if choking state should be updated
        if(presentTime - lastChokeUpdate.getTime() > CHOKING_MANAGEMENT_PERIODE) {

            // Make a shallow copy just for sorting
            LinkedList<Connection> copyForSorting = (LinkedList<Connection>)connections.clone();

            // If we have all pieces, we unchoke the fastest leechers,
            // otherwise we unchoke the fastest seeders
            if(weHaveAllPieces())
                Collections.sort(copyForSorting, new UploadRateDescendingComparator());
            else
                Collections.sort(copyForSorting, new DownloadRateDescendingComparator());

            // Pick top MAX_NUMBER_OF_UNCHOKED_PEERS and send unchoke
            // message to all that are not choked, and send
            // choked message to everyone else that are choked.
            int counter = 0;
            for(Connection c: copyForSorting) {

                // check if we should try to unchoke
                if(counter < MAX_NUMBER_OF_UNCHOKED_PEERS) {

                    // if connection is choked, then send unchoke
                    if(c.getClientState().isChoking())
                        c.enqueueMessageForSending(new UnChoke());

                } else { // or try to unchoke

                    // if connection is unchoked, then send choke
                    if(!c.getClientState().isChoking())
                        c.enqueueMessageForSending(new Choke());
                }

                counter++;
            }
        }

        // Check if optimistic unchoking should be performed
        if(presentTime - lastOptimisticChokingUpdate.getTime() > OPTIMISTIC_UNCHOKING_MANAGEMENT_PERIODE) {

            // Just find one which is choked and unchoke
            Connection connection = pickConnectionForOptimisticUnchoking();

            // check that there was at least one, and then send un choke
            if(connection != null)
                connection.enqueueMessageForSending(new UnChoke());
        }
    }

    /**
     * Comparator for finding good leechers
     */
    class UploadRateDescendingComparator implements Comparator<Connection> {

        /**
         * Compare upload rate in descending order
         * @param o1
         * @param o2
         * @return
         */
        @Override
        public int compare(Connection o1, Connection o2) {
            return o1.getUploadRate() - o2.getUploadRate();
        }

        /**
         * This equals contract is not identical to Object.equals,
         * read java api docs.
         * @param obj
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            return obj instanceof UploadRateDescendingComparator;
        }
    }

    /**
     * Comparator for finding good seeders
     */
    class DownloadRateDescendingComparator implements Comparator<Connection> {

        /**
         * Compare upload rate in descending order
         * @param o1
         * @param o2
         * @return
         */
        @Override
        public int compare(Connection o1, Connection o2) {
            return o1.getDownloadRate() - o2.getDownloadRate();
        }

        /**
         * This equals contract is not identical to Object.equals,
         * read java api docs.
         * @param obj
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            return obj instanceof DownloadRateDescendingComparator;
        }
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
     * @return
     */
    boolean weHaveAllPieces() {
        // check that we have metainfo
        // then check that we hvae it all
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

    /**
     * Returns one random presently choked connection.
     * @return Connection
     */
    private Connection pickConnectionForOptimisticUnchoking() {

        // Temporary container for all choked connections
        ArrayList<Connection> choked = new ArrayList<Connection>();

        // Iterate all connections, and
        for(Connection c: connections) {

            if(c.getPeerState().isChoking())
                choked.add(c);
        }

        // Return null if everything was choked
        int numberOfChokedConnections = choked.size();
        if(numberOfChokedConnections == 0)
            return null;
        else
            return choked.get(randInt(0, numberOfChokedConnections - 1));
    }

    /**
     * Returns random integer between min and max, both included.
     * @param min inlcusive lower bound
     * @param max inclusive upper bound
     * @return random integer
     */
    private int randInt(int min, int max) {

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return random.nextInt((max - min) + 1) + min;
    }


}