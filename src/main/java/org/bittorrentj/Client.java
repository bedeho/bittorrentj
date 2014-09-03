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
import org.bittorrentj.message.field.InfoHash;
import org.bittorrentj.message.field.PeerId;
import org.bittorrentj.message.field.Reserved;

public class Client extends Thread {

    /**
     * Reference to client manager
     */
    private BitTorrentj b;

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
    private final static long SELECTOR_DELAY = 100;

    /**
     * Stages during handshake from the perspective of the receiver of a connection
     */
    private enum Stage {START , INFO_HASH_READ, HANDSHAKE_SENT};

    /**
     * Representation of full state of a connection during handshake stage
     */
    class HandshakeReceiverState {

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
         * Message received from peer, is filled continuously during handshake
         */
        public HandshakeMessage m;

        public HandshakeReceiverState() {

            this.s = Stage.START;
            this.b = ByteBuffer.allocate(MAX_HANDSHAKE_MESSAGE_SIZE);
            this.b.limit(1); // Initial read is of pstrlen field of one byte
            this.m = new HandshakeMessage(0, "", null, null, null);
        }
    }

    /**
     * Constructor
     * @param b managing object for this client
     */

    public Client(BitTorrentj b) {

        this.b = b;
        this.numberOfHandshakingConnections = 0;
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
                    if(key.isReadable()) {
                        try {
                            read(key);
                        } catch (IOException e) {
                            //
                            //
                            //
                            //
                        }
                    }


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
                        state.m.setPstrlen((int)b.get(0));

                        // Set new limit to read up to and including info_hash field
                        b.limit(1 + state.m.getPstrlen() + Reserved.getLength() + PeerId.getLength());
                    }

                } else { // Yes

                    // Try to read up to and including info_hash field
                    channel.read(b);

                    // Did we read it all?
                    if(b.remaining() == 0) {

                        // Extract fields from buffer
                        byte[] full = b.array();
                        byte[] pstr = Arrays.copyOfRange(full, 1, 1 + state.m.getPstrlen());
                        byte[] reserved = Arrays.copyOfRange(full, 1 + state.m.getPstrlen(), 1 + state.m.getPstrlen() + Reserved.getLength());
                        byte[] info_hash = Arrays.copyOfRange(full, 1 + state.m.getPstrlen() + Reserved.getLength(), 1 + state.m.getPstrlen() + Reserved.getLength() + InfoHash.getLength());

                        // Save variables in state
                        state.m.setPstr(pstr.toString());
                        state.m.setReserved(new Reserved(reserved));
                        state.m.setInfo_hash(new InfoHash(info_hash));

                        // Do we serve this torrent?
                        if(torrents.containsKey(state.m.getInfo_hash())) {

                            // Alter stage
                            state.s = Stage.INFO_HASH_READ;

                            // Set new buffer limit to read peer_id
                            b.limit(b.limit() + PeerId.getLength());

                            // Alter interest set so that we can ONLY read
                            key.interestOps(xxx)

                        } else {

                            // Send event
                            sendEvent(new UnrecognizedInfoHashEvent(channel.socket().getInetAddress(), state.m.getInfo_hash()));

                            // Disconnect
                            channel.close();

                            // Deregister channel
                            selector.re
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
                    state.m.setPeer_id();

                    // Add peer to given torrent
                    torrents.get(state.m.getInfo_hash()).addPeer(channel, state.m);
                }

                break;
        }

    }

    /**
     * Routine for handling write opportunity on a channel
     * @param key selection key with OP_WRITE set
     */
    private void write(SelectionKey key) {

        // Recover channel state
        HandshakeReceiverState state = (HandshakeReceiverState)key.attachment();

        // Recover channel
        SocketChannel channel = (SocketChannel)key.channel();

        // grab the output buffer of the relevant peer

        //HandshakeMessage m = new HandshakeMessage(19, "BitTorrent protocol", new Reserved(true, true), state.m.getInfoHash(), new PeerId(PeerId.PeerType.BitSwapr));
        //send m.toByteBuffer()

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
     * Count the total number of accepted connections. Keep in mind that channels
     * may be closed by peers during counting,
     * hence this routines gives an upper bound, which is fine.
     * @return number of connections
     */
    private int numberOfConnections() {

        int number = 0;

        // Count the number of peers for each torrent.
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