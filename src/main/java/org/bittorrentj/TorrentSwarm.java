package org.bittorrentj;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.bittorrentj.event.Event;
import org.bittorrentj.message.HandshakeMessage;
import org.bittorrentj.message.KeepAliveMessage;
import org.bittorrentj.message.field.Hash;
import org.bittorrentj.torrent.Metainfo;

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
    private Hash info_hash;

    /**
     * Torrent file metainfo
     */
    private Metainfo metainfo;

    /**
     * Multiplexing selector for server
     */
    private Selector selector;

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

    private int maxNumberOfUploads;

    private int maxUploadSpeed;

    private MetaInfo mInfo;

    private Hash info_hash;

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

    public TorrentSwarm(Hash info_hash) {

        // who gets peers? Client or swarm?
        // dht must be in client I think????
        // PEX here?

        this.info_hash = info_hash;

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            System.out.println("How can we possibly end up here"); // log later or something
        }

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

                try {

                    // Get connection
                    Connection connection = (Connection)key.attachment();

                    // Ready to be read
                    if (key.isReadable())
                        connection.readMessagesFromChannel();

                    // Ready to be written to
                    if (key.isWritable())
                        connection.writeMessagesToChannel();

                } catch (IOException e) {
                    sendEvent(new ClientIOFailedEvent(e));

                    // remove from hashmap, do we even need hashmap on top of selecter key set?
                }
            }

            // Process each connection, and remove key from selected key set
            i = selector.selectedKeys().iterator();

            while (i.hasNext()) {

                SelectionKey key = (SelectionKey) i.next();

                // Remove from selected key set,
                // otherwise it sticks around even after next select() call
                i.remove();

                // Process channel
                processNetworkEvents(key);
            }
        }

        // Disconnect connections which have taken to long to talk to us,
        // send keep-alive if we have not written anything in a while
        long nowDateInMs = new Date().getTime();

        for(SelectionKey key : selector.keys()) {

            // Get connection
            Connection connection = (Connection)key.attachment();

            // Close if it has taken more time than upper limit,
            // otherwise send keep-alive if we have not written in a while
            if(nowDateInMs - connection.getTimeLastDataReceived().getTime() > MAX_SILENCE_DURATION)
                closeConnection(connection);
            else if(nowDateInMs - connection.getTimeLastDataSent().getTime() > KEEP_ALIVE_INTERVAL)
                connection.enqueueMessageForSending(new KeepAliveMessage());
        }
    }

    // Does something when there is some channel read/write event
    private void processNetworkEvents(SelectionKey key) {

    }

    /**
     * Number of connections in swarm at present
     * @return
     */
    int getNumberOfPeers() {

        // more complicated?
        /*
        synchronized (connections) {
            return connections.size();
        }
        */
    }

    /**
     * Indicates whether this swarm accepts more connections
     * @return
     */
    boolean acceptsMoreConnections() {

        // complicated, think more about later
        /*
        synchronized (connections) {
            return connections.size() > maxNumberOfConnections;
        }
        */
    }

    /**
     *
     */
    public void addConnection(SocketChannel channel, HandshakeMessage m) {

        //register channel with selector with both opread and opwrite
    }

    public void closeConnection(Connection connection) {

        // called from process*Netowrk, but perhaps also from Client
        // in response to command, figure this out, and whether we need
        // synching. this informs whetehr routine should be public or private
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
     */
    public ConnectionInformation getConnectionInformation() {

    }

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
}
/*




        do we have peers??? reconnect?
        findPeers()
        WHAT DOES THREAD DO, SLEEP ?
synchronized halt()
        chekc that we we are alredy running??
        call on peers and extensions?
private findPeers(): called either from begin() or from peerDisconnected(), perhaps this should be moved out
        What do we have available?
        Metainfo
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