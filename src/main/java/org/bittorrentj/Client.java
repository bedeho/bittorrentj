package org.bittorrentj;

import java.nio.channels.*;
import java.util.*;

import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.io.IOException;

import org.bittorrentj.command.Command;
import org.bittorrentj.event.Event;
import org.bittorrentj.event.StartServerErrorEvent;
import org.bittorrentj.event.ToManyConnectionsEvent;
import org.bittorrentj.message.HandshakeMessage;
import org.bittorrentj.message.field.Hash;
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
     * Collection of torrentSwarms presently being serviced
     */
    private HashMap<Hash, TorrentSwarm> torrentSwarms;

    /**
     * Socket channel used for server
     */
    private ServerSocketChannel serverChannel;

    /**
     * Multiplexing selector for server
     */
    private Selector selector;

    /**
     * Queue of commands issued by managing object readBuffer
     */
    private LinkedList<Command> commandQueue;

    /**
     * The number of milliseconds a call to the select routine on the selector,
     * should at most last. If there are no evens at all, which is unlikely,
     * then having no threshold would cause perpetual blocking.
     */
    public final static long MAX_SELECTOR_DELAY = 100;

    /**
     * The number of milliseconds in total a handshake with a new peer may take,
     * any longer results in a disconnect.
     */
    private final static long MAX_HANDSHAKE_DELAY = 2000;

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
        public ByteBuffer readBuffer;
        private final int MAX_HANDSHAKE_MESSAGE_SIZE = 1 + 255 + Reserved.getLength() + Hash.getLength() + PeerId.getLength(); // 1 + len(pstr) + 8 + 20 + 20 <= 49 + 255 = 304

        /**
         * Message received from peer, is filled continuously during handshake
         */
        public HandshakeMessage messageReceived;

        /**
         * Contains message sent from from client to peer, is built just after reading info_hash.
         * There is no need to keep the HandshakeMessage object around itself, since the
         * raw form is put into this buffer right away.
         */
        public ByteBuffer writeBuffer;

        /**
         * Date and time when handshake was initiated, is used to discharge
         * connections which do not handshake properly within a given period of time
         */
        public Date hanshakeBegan;

        /**
         * Constructor
         */
        public HandshakeReceiverState() {

            this.s = Stage.START;
            this.readBuffer = ByteBuffer.allocate(MAX_HANDSHAKE_MESSAGE_SIZE);
            this.readBuffer.limit(1); // Initial read is of pstrlen field of one byte
            this.messageReceived = new HandshakeMessage(0, "", null, null, null);
            this.writeBuffer = null;
            this.hanshakeBegan = new Date();
        }
    }

    /**
     * Constructor
     * @param b managing object for this client
     */
    public Client(BitTorrentj b) {

        this.b = b;
        this.numberOfHandshakingConnections = 0;
        this.torrentSwarms = new HashMap<Hash, TorrentSwarm>();
    }

    /**
     * Thread entry point where main client thread runs.
     * It pools network selector and checks command queue
     */
   @Override
    public void run() {

        // ALTER LATER TO SUPPORT INTERLEAVED BEGINNING AND HALTING
        // ***idea, put server management as one of the commands***
        // Close server/selector as required by those commands
        if(!startServer())
            return; // ?

        // Main loop for processing new
        // 1. connections and conducting handshakes
        // 2. commands from client manager object readBuffer
        while(true) {

            // Process network channel events
            processNetwork();

            // Process at most one new command, if available
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

       try {

           // Get socket channel
           SocketChannel client = serverChannel.accept();

            // Did we manage to actually accept connection? Why may this fail?
            // if so, signal that connection was not accepted
            if (client == null)
                return false;

            // Set to non-blocking mode
            client.configureBlocking(false);

            // Register with with selector ONLY for reading
            client.register(selector, SelectionKey.OP_READ);

       } catch (IOException e) {
           sendEvent(new AcceptingClientFailedEvent(e));
           return false;
       }

        // Attach state object with key
        key.attach(new HandshakeReceiverState());

        // Signal that connection as accepted
        return true;
    }

    /**
     * Processes the latest channel events, and manages channels
     */
    private void processNetwork() {

        // Get next channel event
        int numberOfUpdatedKeys = 0;

        try {
            numberOfUpdatedKeys = selector.select(MAX_SELECTOR_DELAY);
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

                try {

                    // Ready to be read
                    if (key.isReadable())
                        read(key);

                    // Ready to be written to
                    if (key.isWritable())
                        write(key);

                } catch (IOException e) {
                    sendEvent(new AcceptingClientFailedEvent(e));
                }
            }
        }

        // Disconnect channels which have taken to long
        long nowDateInMs = new Date().getTime();

        for(SelectionKey key : selector.keys()) {

            // Recover state of handshake
            HandshakeReceiverState state = (HandshakeReceiverState)key.attachment();

            // Close if it has taken more time than upper limit
            if(nowDateInMs - state.hanshakeBegan.getTime() > MAX_HANDSHAKE_DELAY) {
                try {
                    key.channel().close();
                } catch(IOException e) {
                    // We may end up here if channel was some how already close,
                    // but in that case who cares.
                }
            }
        }
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
        ByteBuffer b = state.readBuffer;

        switch(state.s) {

            case START:

                // Has pstrlen been read?
                if(b.position() == 0) { // No

                    // Try to read pstrlen field
                    channel.read(b);

                    // Did we read it all?
                    if(b.remaining() == 0) {

                        // Read pstrlen
                        int pstrlen = (int)b.get(0);

                        // Check that it a positive integer
                        if(pstrlen < 1) {

                            // Otherwise close channel
                            channel.close();

                            break;
                        }

                        // Save pstrlen in state
                        state.messageReceived.setPstrlen(pstrlen);

                        // Set new limit to read up to and including info_hash field
                        b.limit(1 + state.messageReceived.getPstrlen() + Reserved.getLength() + Hash.getLength());
                    }

                } else { // Yes

                    // Try to read up to and including info_hash field
                    channel.read(b);

                    // Did we read it all?
                    if(b.remaining() == 0) {

                        // Extract fields from buffer and save in state
                        byte[] full = b.array();

                        // pstr
                        int from = 1;
                        int to = from + state.messageReceived.getPstrlen();
                        byte[] pstr = Arrays.copyOfRange(full, from, to);
                        state.messageReceived.setPstr(pstr.toString());

                        // reserved
                        from = to;
                        to += Reserved.getLength();
                        byte[] reserved = Arrays.copyOfRange(full, from, to);
                        state.messageReceived.setReserved(new Reserved(reserved));

                        //  info_hash
                        from = to;
                        to += Hash.getLength();
                        byte[] info_hash = Arrays.copyOfRange(full, from, to);
                        state.messageReceived.setInfo_hash(new Hash(info_hash));

                        // Do we serve this torrent?
                        TorrentSwarm t = torrentSwarms.get(state.messageReceived.getInfo_hash());
                        if(t != null) {

                            // Can this swarm handle one more client
                            if(t.acceptsMoreConnections()) {

                                // Alter stage
                                state.s = Stage.INFO_HASH_READ;

                                // Save message we intend to save
                                HandshakeMessage m = new HandshakeMessage(19, "BitTorrent protocol", new Reserved(true, true), state.messageReceived.getInfoHash(), new PeerId(PeerId.PeerType.BitSwapr));
                                state.writeBuffer = ByteBuffer.wrap(m.toRaw());

                                // Alter interest set so that we can ONLY write our handshake
                                key.interestOps(SelectionKey.OP_WRITE);
                            } else {

                                // Send event
                                sendEvent(new TorrentSwarmFullEvent(channel.socket().getInetAddress(), state.messageReceived.getInfo_hash()));

                                // Disconnect, and therefor also automatically unregister with selector
                                channel.close();
                            }
                        } else {

                            // Send event
                            sendEvent(new UnrecognizedInfoHashEvent(channel.socket().getInetAddress(), state.messageReceived.getInfo_hash()));

                            // Disconnect, and therefor also automatically unregister with selector
                            channel.close();
                        }
                    }
                }

                break;
            case INFO_HASH_READ:
                // We should not end up here, given that we set OP_WRITE as only channel event we care about
                // because it is time to write our handshake in write() routine, it will set HANDSHAKE_SENT
                // when done
                break;
            case HANDSHAKE_SENT:

                // Try to read peer_id
                channel.read(b);

                // Did we read it all?
                if(b.remaining() == 0) {

                    // Save peer_id
                    int from = 1 + state.messageReceived.getPstrlen() + Reserved.getLength() + Hash.getLength();
                    int to = from + PeerId.getLength();
                    byte[] peer_id = Arrays.copyOfRange(b.array(), from, to);

                    // Save peer_id in state
                    state.messageReceived.setPeer_id(new PeerId(peer_id));

                    // Unregister channel with this selector, will be registered in TorrentSwarm object
                    key.cancel();

                    // Add peer to given torrent
                    torrentSwarms.get(state.messageReceived.getInfo_hash()).addConnection(channel, state.messageReceived);
                }

                break;
        }

    }

    /**
     * Routine for handling write opportunity on a channel
     * @param key selection key with OP_WRITE set
     */
    private void write(SelectionKey key) throws IOException {

        // Recover channel state
        HandshakeReceiverState state = (HandshakeReceiverState)key.attachment();

        // Recover channel
        SocketChannel channel = (SocketChannel)key.channel();

        // Handle based on stage of handshake
        ByteBuffer b = state.writeBuffer;

        // Try to write our handshake
        channel.write(b);

        // Have we written it all?
        if(b.remaining() == 0) {

            // Set new buffer limit to read peer_id
            state.readBuffer.limit(state.readBuffer.limit() + PeerId.getLength());

            // Alter stage
            state.s = Stage.HANDSHAKE_SENT;

            // Alter state so that we can only read, in order to get peer_id
            key.interestOps(SelectionKey.OP_READ);
        }
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
     * Registers new command in queue, is called by client manager readBuffer
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
        for(TorrentSwarm t: torrentSwarms.values())
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

    /**
     * Getter routine used by TorrentSwarm objects to send message
     * to manager
     * @return manager
     */
    public BitTorrentj getB() {
        return b;
    }
}