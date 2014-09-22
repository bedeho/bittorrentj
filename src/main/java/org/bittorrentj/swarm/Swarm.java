package org.bittorrentj.swarm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

import org.bittorrentj.Client;
import org.bittorrentj.event.ClientIOFailedEvent;
import org.bittorrentj.event.Event;
import org.bittorrentj.exceptions.*;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.*;
import org.bittorrentj.message.exceptions.UnsupportedExtendedMessageFoundException;
import org.bittorrentj.message.field.Hash;
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
     * Maps IP:Port string to corresponding connection
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

            // how to stop???

            // Think about the order of the processing below
            // Also perhaps refactor them some more to make them bemore testable and have more precise/stateless behaviour.

            //
            readAndWriteMessages();


            // choking algorithm, and optimistic unchoking
            updateChokingState();

            requestPieces();

            sendPieces();

            // extension processing called as well
            processExtensions();

            // if we have to few connections now, how do we get more peers?
            manageConnectivity(); // should this perhaps  be the first one, so we dont need to bother with all other stuff
        }
    }

    /**
     *
     */
    private void readAndWriteMessages() {

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

                // Read from channel if ready
                if (key.isReadable()) {
                    connection.readMessagesFromChannel();

                    // Process message queue just populated
                    try {
                        connection.processReadMessageQueue();
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
                        closeConnection(connection);
                        //return;
                        // or just ignore ?
                    }
                }

                // Write to channel if ready
                if (key.isWritable())
                    connection.writeMessagesToChannel();

            } catch (IOException e) {
                // Notify client
                sendEvent(new ClientIOFailedEvent(e));

                //// closeConnection(connection); ????

            } catch (MessageToLargeForNetworkBufferException e) {
                // ?
            } catch (InvalidMessageReceivedException e) {
                // ?
            } finally {
                // Close connection with this peer
                closeConnection(connection);
            }
        }
    }

    private void updateChokingState() {

        // stop downloading pieces from someone who is just super slow,or who did not respond to our request?

    }

    private void requestPieces() {

    }

    private void sendPieces() {

    }

    private void pickNextPiece() {

        // should hvae streamin and rarest first

    }

    private void processExtensions() {

    }

    private void manageConnectivity() {

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
    public boolean isMetaInformationKnown() { return metaInformation != null;}

    public int sizeOfBlockWeDoNotHave(int begin, int block, int length) {

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