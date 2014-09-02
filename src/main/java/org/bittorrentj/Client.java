package org.bittorrentj;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.io.IOException;

import org.bittorrentj.command.Command;
import org.bittorrentj.event.Event;
import org.bittorrentj.event.StartServerErrorEvent;
import org.bittorrentj.event.ToManyConnectionsEvent;
import org.bittorrentj.message.HandshakeMessage;
import org.bittorrentj.message.PeerId;
import org.bittorrentj.message.Reserved;

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
    private enum Stage {START , INFO_HASH_READ, HANDSHAKE_SENT};

    /**
     * Representation of full state of a connection during handshake stage
     */
    private class HandshakeReceiverState {

        /**
         * Stage representation
         */
        public Client.Stage s;

        /**
         * Buffer containing what has been read so far
         */
        public ByteBuffer b;
        public final static int MAX_HANDSHAKE_MESSAGE_SIZE = 304; // 1 + len(pstr) + 8 + 20 + 20 <= 49 + 255 = 304

        /**
         *
         */
        public int pstrlen;

        /**
         *
         */
        public String pstr;

        /**
         *
         */
        public Reserved reserved;

        /**
         *
         */
        public InfoHash info_hash;

        /**
         *
         */
        public PeerId peer_id;

        HandshakeReceiverState() {

            this.s = Stage.START;
            this.b = ByteBuffer.allocate(MAX_HANDSHAKE_MESSAGE_SIZE);
            this.b.limit(1); // Initial read is of pstrlen field of one byte

            this.pstrlen = 0;
            this.pstr = null;
            this.reserved = null;
            this.info_hash = null;
            this.peer_id = null;
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
    private void read(SelectionKey key) throws IOException {

        // Recover channel state
        HandshakeReceiverState state = (HandshakeReceiverState)key.attachment();

        // Recover channel
        SocketChannel channel = (SocketChannel)key.channel();

        // Handle based on stage of handshake
        ByteBuffer b = state.b;

        switch(state.s) {

            case START:

                // Has pstrlen been read?
                if(b.position() == 0) { // No

                    // Try to read pstrlen field
                    channel.read(b);

                    // Did we read it all?
                    if(b.remaining() == 0) {

                        // Save pstrlen in state
                        state.pstrlen = (int) b.get(0);

                        // Set new limit to read up to and including info_hash field
                        b.limit(1 + state.pstrlen + 8 + 20);
                    }

                } else { // Yes

                    // Try to read up to and including info_hash field
                    channel.read(b);

                    // Did we read it all?
                    if(b.remaining() == 0) {

                        // Save variables in state
                        byte[] raw = b.array();

                        state.pstr = Arrays.copyOf(raw, 1, 1 + state.pstrlen).toString();
                        state.reserved = new Reserved(Arrays.copyOf(raw, 1 + state.pstrlen + 1, 1 + state.pstrlen + 1 + 8));
                        state.info_hash = new InfoHash(Arrays.copyOf(raw, 1 + state.pstrlen + 1, 1 + state.pstrlen + 1 + 8));

                        // Do we serve this torrent?
                        if(torrents.containsKey(state.info_hash)) {

                            // Alter stage
                            state.s = Stage.INFO_HASH_READ;

                            // Set new buffer limit to read peer_id
                            b.limit(b.limit() + 20);

                        } else {

                            // Send event
                            sendEvent(new UnrecognizedInfoHashEvent(channel.socket().getInetAddress(), state.info_hash));

                            // Disconnect
                            channel.close();
                        }
                    }
                }

                break;
            case INFO_HASH_READ:
                // do nothing, its time to write our handshake in write() routine, it will set HANDSHAKE_SENT
                break;
            case HANDSHAKE_SENT:

                // Try to read peer_id
                channel.read(b);

                // Did we read it all?
                if(b.remaining() == 0) {

                    // Save peer_id
                    state.peer_id = ;

                    // Add peer to given torrent
                    torrents.get(state.info_hash).addPeer(channel, state.pstr, state.reserved, state, state.info_hash, state.peer_id);
                }

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

        // on info hash read_, send handshake, when done set //state.setS(Stage.HANDSHAKE_SENT);


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