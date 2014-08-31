package org.bittorrentj;

import java.util.HashMap;
import java.util.Iterator;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.io.IOException;

import org.bittorrentj.event.StartServerErrorEvent;

public class Client extends Thread {

    /**
     * Reference to main object
     */
    private BitTorrentj b;

    /**
     * Collection of torrents presently being serviced
     */
    private HashMap<InfoHash, Torrent> torrents;

    /**
     * Socket channel used for server
     */
    private ServerSocketChannel serverChannel;

    /**
     * Multiplexing selector for server
     */
    private Selector selector;

    /**
     * Map of channels which have not completed handshake step.
     * The key is InetSocketAddress.toString() which has format host:port.
     */
    private HashMap<String, SocketChannel> channelsBeforeHandshake;

    /**
     *
     * @param b
     */

    Client(BitTorrentj b) {

        this.b = b;
        this.torrents = new HashMap<InfoHash, Torrent>();

    }

    /**
     * Thread entry point where main client thread runs. It pools network selector and checks command queue
     */
    public void run() {

        // ALTER LATER TO SUPPORT INTERLEAVED BENINNING AND HALTING
        if(!startServer())
            return; // ?

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

                // Remove from selected key set <-- ? why needed
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
     */
    private boolean startServer() {

        // Create server side address
        InetSocketAddress address = new InetSocketAddress("localhost", b.getPort());

        // Create selector
        selector = Selector.open();

        // Start server
        try {

            // Create server channel
            serverChannel = ServerSocketChannel.open();

            // Try to listen to a given port
            serverChannel.bind(address);

            // Turn off blocking
            serverChannel.configureBlocking(false);

            // Recording server to selector (type OP_ACCEPT)
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            b.addEvent(new StartServerErrorEvent(e, address)); // should we even have the address here?
            return false;
        }

        return true;
    }

    /**
     *
     */
    private void accept() {

        // Check if we can accept one more connection
        if(numberOfConnections() + 1 <= b.getMaxNumberOfConnections()) {

            // Create event??
            b.addEvent(new ToManyConnectionsErrorEvent());

            // Continue processing next key
            return;
        }

        // Get socket channel
        SocketChannel client = serverChannel.accept();

        // Did we manage to actually accept connection? Why may this fail?
        if(client == null)
            return;

        // Set to non-blocking mode
        client.configureBlocking(false);

        // recording to the selector (reading)
        client.register(selector, SelectionKey.OP_READ);

        // Save channel
        InetSocketAddress a;

        try {
            a = client.getRemoteAddress();

            channelsBeforeHandshake.put(a.toString(), client);
        } catch (IOException e) {

        }

        // Continue processing next key

                    /*
                    wait for handshake message
                    consume message up to and including info_hash
                    do we serve info_hash it? if no: disconnect
                    */

                    /*


                        true:
                            then send our own handshake, set reserved bits to indicate we support
                            BEP 5 DHT
                            BEP 6 Fast extension
                            BEP 10
                            consume peer_id
                            call upon torrents(info_hash).addPeer(socket, peer_id, reserved)
                        false:
                            disconnect
                     */

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
     * Count the total number of accepted connections
     * @return
     */
    private int numberOfConnections() {

        int number = 0;

        // Count the number of peers for each torrent
        for(Torrent t: torrents)
            number += t.getNumberOfPeers();

        // Count the connections not yet having handshaked
        number += channelsBeforeHandshake.size();

        return number;
    }
}