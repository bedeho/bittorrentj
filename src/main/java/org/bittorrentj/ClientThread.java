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

import org.bittorrentj.event.Event;

public class ClientThread extends Thread {

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

    ClientThread(BitTorrentj b) {

        this.b = b;
        this.torrents = new HashMap<InfoHash, Torrent>();

    }

    public void run() {

        // Start server
        try {

            // Create server channel
            serverChannel = ServerSocketChannel.open();

            // Try to listen to a given port
            serverChannel.bind(new InetSocketAddress("localhost", b.getPort()));

            // Turn off blocking
            serverChannel.configureBlocking(false);

            // Create selector
            selector = Selector.open();

            // Recording server to selector (type OP_ACCEPT)
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {

            b.addEvent(new Event());
            // create some message which encodes that we could not start server
        }


        while(true) {

            /*

                block accept an incoming connection if it does not put us over limit
                wait for handshake message
                consume message up to and including info_hash
                do we serve info_hash it? if no: disconnect


                    can it have one more peer?

                    true:
                        then send our own  handshake, set reserved bits to indicate we support
                        BEP 5 DHT
                        BEP 6 Fast extension
                        BEP 10
                        consume peer_id
                        call upon torrents(info_hash).addPeer(socket, peer_id, reserved)
                    false:
                        disconnect

             */

            // LOOK AT MESSAGE WHICH MAY HAVE ARRIVED FROM BITTORRENTJ
            // concurrentlist.size() > 0 --> something to do

            // Block until socket event is generated
            try {
                selector.select();
            } catch() {

            }

            // Iterate keys
            Iterator i = selector.selectedKeys().iterator();

            while(i.hasNext()) {

                SelectionKey key = (SelectionKey) i.next();

                // Remove the current key
                i.remove();

                // if isAccetable = true
                // then a client required a connection
                if (key.isAcceptable()) {
                    // get client socket channel
                    SocketChannel client = serverChannel.accept();
                    // Non Blocking I/O
                    client.configureBlocking(false);
                    // recording to the selector (reading)
                    client.register(selector, SelectionKey.OP_READ);
                    continue;
                }

                // if isReadable = true
                // then the server is ready to read
                if (key.isReadable()) {

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
            }

        }

    }
}