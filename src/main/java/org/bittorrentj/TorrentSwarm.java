package org.bittorrentj;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

import org.bittorrentj.message.HandshakeMessage;
import org.bittorrentj.message.field.Hash;
import org.bittorrentj.torrent.Metainfo;

/**
 * Created by bedeho on 30.08.2014.
 */
public class TorrentSwarm extends Thread {

    /**
     * Info_hash for this torrent swarm
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
    private HashMap<String, Connection> connections;

    // diskworker?
    /*
    fields





        settings?


        constructor(int maxNumberOfConnections, int minimumNumberOfConnections, int maxNumberOfUploads, int max, ArrayList<Extension>,CallBackHandler, [magnet link or Metainfo]): typically called by BitTorrentj addTorrent()

        register torrent with extensions, for all i=1:Extension.length

        confirms magnet link validity if present

     */

    public TorrentSwarm(Hash info_hash) {

        this.info_hash = info_hash

                //Extension[i].addTorrent(this or info_hash, does it really need to know - can extension trust info, is it thread safe? ut_metdata would need to even modify!!!);
    }

    TorrentSwarm() {

        // Event loop
        while(true) {

            // Block until socket event is generated, or we are interreupted for some reason
            try {
                selector.select();
            } catch() {

            }

            // Iterate keys
            Iterator i = selector.selectedKeys().iterator();

            while(i.hasNext()) {

                SelectionKey key = (SelectionKey) i.next();

                // Remove from selected key set
                i.remove();

                // Ready to accept new connection
                if (key.isAcceptable())
                    accept();

                // Ready to be read
                if (key.isReadable())
                    read(key);

                // Ready to be written to
                if(key.isWritable())
                    write(key);
            }

            // LOOK AT MESSAGE WHICH MAY HAVE ARRIVED FROM BITTORRENTJ
            // concurrentlist.size() > 0 --> something to do

        }


    }


    /**
     *
     * @param key
     */
    private void read(SelectionKey key) {

        // get the correct peer

        // put data in input buffer

        // call processing routine for peer

        SocketChannel client = (SocketChannel) key.channel();

        // Read byte coming from the client
        int BUFFER_SIZE = 32;
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        try {
            client.read(buffer);
        }
        catch (Exception e) {
            // client is no longer active
            e.printStackTrace();
            continue;
        }
    }

    /**
     *
     * @param key
     */
    private void write(SelectionKey key) {

        // grab the output buffer of the relevant peer

        // write it out

    }

    /**
     * Number of connections in swarm at present
     * @return
     */
    int getNumberOfPeers() {

        // needs to be synched?

    }

    /**
     * Indicates whether this swarm accepts more connections
     * @return
     */
    boolean acceptsMoreConnections() {

    }

    /**
     *
     */
    public void addConnection(SocketChannel channel, HandshakeMessage m) {

        //register channel with selector with both opread and opwrite
    }

    /**
     *
     * @return
     */
    public list getConnections() {

    }

    /**
     *
     * @return
     */
    public Connection getConnection() {

    }

    /**
     *
     */
    public void removeConnection() {

    }
}
/*

synchronized begin() : called from BitTorrentj addTorrent in fresh thread, or begin() from BitTorrentj
        Check that we arent already running??

        MUST BE ABLE TO RECOVER FROM SAVED FILE, ALSO USING FAST FILE CHECKING ROUTINE
        CREATE file thread????


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