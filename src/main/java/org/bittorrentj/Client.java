package org.bittorrentj;

import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.LinkedList;

import org.bittorrentj.command.Command;
import org.bittorrentj.event.Event;
import org.bittorrentj.event.StartServerErrorEvent;
import org.bittorrentj.event.ToManyConnectionsEvent;
import org.bittorrentj.message.HandshakeMessage;

public class Client extends Thread {

    /**
     * Reference to client manager
     */
    private BitTorrentj b;

    /**
     * Raw byte form of handshake message sent by this client to all peers
     */
    private ByteBuffer rawHandshakeMessage;

    /**
     * Number of accepted connections which are in the process of conducting handshake
     */
    private int numberOfHandshakingConnections;

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
     * Queue of commands issued by managing object b
     */
    private LinkedList<Command> commandQueue;

    /**
     * The number of milliseconds a call to the select routine on the selector,
     * should at most last. If there are no evens at all, which is unlikely,
     * then having no threshold would cause perpetual blocking.
     */
    private final static long SELECTOR_DELAY = 500;

    /**
     * Stages during handshake from the perspective of the receiver of a connection
     */
    private enum Stage { START , READ_PSTRLEN


        INFO_HASH_READ, HANDSHAKE_SENT};

    /**
     * Representation of full state of a connection during handshake stage
     */
    private class HandshakeReceiverState {

        /**
         * Stage representation
         */
        private Client.Stage s;

        /**
         * Buffer containing what has been read so far
         */
        private ByteBuffer b;
        private final static int MAX_HANDSHAKE_MESSAGE_SIZE = 304; // 49 + len(pstr) <= 49 + 255 = 304

        HandshakeReceiverState() {
            this.s = Stage.START;
            this.b = ByteBuffer.allocate(MAX_HANDSHAKE_MESSAGE_SIZE);
        }

        public Stage getS() {
            return s;
        }

        public void setS(Stage s) {
            this.s = s;
        }

        public ByteBuffer getB() {
            return b;
        }

        /**
         * How far we have read into handshake message
         * @return position
         */
        public int readSoFar() {
            return b.position();
        }
    }

    /**
     * Constructor
     * @param b managing object for this client
     */

    Client(BitTorrentj b) {

        this.b = b;
        this.numberOfHandshakingConnections = 0;
        /*
                                    BEP 5 DHT
                            BEP 6 Fast extension
                            BEP 10
         */
        this.rawHandshakeMessage = new HandshakeMessage().toByteBuffer();
        this.torrents = new HashMap<InfoHash, Torrent>();
        this.commandQueue = new LinkedList<Command>();
    }

    /**
     * Thread entry point where main client thread runs. It pools network selector and checks command queue
     */
    public void run() {

        // ALTER LATER TO SUPPORT INTERLEAVED BENINNING AND HALTING
        // ***idea, put server management as one of the commands***
        // Close server/selector as required by those commands
        if(!startServer())
            return; // ?

        // Main loop for processing new
        // 1. connections and conducting handshakes
        // 2. commands from client manager object b
        while(true) {

            // Get next channel event
            int numberOfUpdatedKeys = 0;

            try {
                numberOfUpdatedKeys = selector.select(SELECTOR_DELAY);
            } catch(IOException e) {
                System.out.println("what can causes us to come here????"); // <= logg later in log4j
            }

            // Process any potential channel events
            if(numberOfUpdatedKeys > 0) {

                // Iterate keys
                Iterator i = selector.selectedKeys().iterator();

                while (i.hasNext()) {

                    SelectionKey key = (SelectionKey) i.next();

                    // Remove from selected key set,
                    // otherwise it sticks around even after next select() call
                    i.remove();

                    // Ready to accept new connection
                    if (key.isAcceptable()) {

                        // Try to accept connection, and keep count if we succeed
                        if(accept(key))
                            numberOfHandshakingConnections++;
                    }

                    // Ready to be read
                    if(key.isReadable())
                        read(key);

                    // Ready to be written to
                    if(key.isWritable())
                        write(key);
                }
            }

            // Process at most one new command
            processOneCommand();
        }
    }

    /**
     * Starts server by creating the selector and binding to given port
     */
    private boolean startServer() {

        // Create server side address
        InetSocketAddress address = new InetSocketAddress("localhost", b.getPort());

        // Start server
        try {

            // Create selector
            selector = Selector.open();

            // Create server channel
            serverChannel = ServerSocketChannel.open();

            // Try to listen to a given port
            serverChannel.bind(address);

            // Turn off blocking
            serverChannel.configureBlocking(false);

            // Recording server to selector (type OP_ACCEPT)
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            sendEvent(new StartServerErrorEvent(e, address)); // should we even have the address here?
            return false;
        }

        return true;
    }

    /**
     * Routine for handling new connection on a channel
     * @param key selection key with OP_ACCEPT set
     */
    private boolean accept(SelectionKey key) {

        // Check if we can accept one more connection
        if(numberOfConnections() + 1 > b.getMaxNumberOfConnections()) {

            // Send notification
            sendEvent(new ToManyConnectionsEvent());

            // Signal that connection was not accepted
            return false;
        }

        // Get socket channel
        SocketChannel client = serverChannel.accept();

        // Did we manage to actually accept connection? Why may this fail?
        // if so, signal that connection was not accepted
        if(client == null)
            return false;

        // Set to non-blocking mode
        client.configureBlocking(false);

        // recording to the selector (reading)
        client.register(selector, SelectionKey.OP_READ);

        // Attach state object with key
        key.attach(new HandshakeReceiverState());

        // Signal that connection as accepted
        return true;
    }

    /**
     * Routine for handling read opportunity on a channel
     * @param key selection key with OP_READ set
     */
    private void read(SelectionKey key) {

        // Recover channel state
        HandshakeReceiverState state = (HandshakeReceiverState)key.attachment();

        // Recover channel
        SocketChannel channel = (SocketChannel)key.channel();

        // Handle based on stage of handshake
        int readSoFar = state.readSoFar();
        ByteBuffer b = state.getB();

        switch(state.getS()) {

            case START:

                if(readSoFar == 0) {
                    channel.read(b, )
                } else {

                    byte pstrlen = b.get();

                }
                // read as much as we can up to info_hash, then switch state

                // do we serve info_hash it, if so // state.setS(Stage.INFO_HASH_READ);
                // if not: disconnect

                break;
            case INFO_HASH_READ:

                //consume peer_id, when done
                //state.setS(Stage.HANDSHAKE_SENT);
                break;
            case HANDSHAKE_SENT:

                   /*

                        // begin creation of new peer, or even new TORRENT - depending on the situation
                        // when the handshake content has been written and no io issue appeared
                        // pass along the relevant handshake information also
                        // upon torrents(info_hash).addPeer(socket, peer_id, reserved)
                    */

                break;
        }

    }

    /**
     * Routine for handling write opportunity on a channel
     * @param key selection key with OP_WRITE set
     */
    private void write(SelectionKey key) {

        // grab the output buffer of the relevant peer

        // write it out



    }

    /**
     * Takes the latest command in the commandQueue and processes it
     */
    private void processOneCommand() {

        // Get latest command
        Command c;

        synchronized (commandQueue) {

            if(commandQueue.isEmpty())
                return;
            else
                c = commandQueue.poll();
        }

        // Process
        // <! -- magic happens here -- !>
    }

    /**
     * Registers new command in queue, is called by client manager b
     * @param c command to be registered
     */
    public void registerCommand(Command c) {

        synchronized (commandQueue) {
            commandQueue.add(c);
        }
    }

    /**
     * Count the total number of accepted connections
     * @return number of connections
     */
    private int numberOfConnections() {

        int number = 0;

        // Count the number of peers for each torrent
        for(Torrent t: torrents.values())
            number += t.getNumberOfPeers();

        // Count the connections not yet having completed handshake
        number += numberOfHandshakingConnections;

        return number;
    }

    /**
     * Registers and event with the management object
     * @param e
     */
    private void sendEvent(Event e) {
        b.registerEvent(e);
    }
}