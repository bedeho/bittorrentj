package org.bittorrentj;

import org.bittorrentj.exceptions.MessageToLargeForNetworkBufferException;
import org.bittorrentj.message.Message;
import org.bittorrentj.message.MessageWithLengthField;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Date;

/**
 * Created by bedeho on 30.08.2014.
 */
public class Connection {

    /**
     * State of client side of connection
     */
    private PeerState clientState;

    /**
     * State of peer side of connection
     */
    private PeerState peerState;

    /**
     * Class capturing state of connection
     */
    private class PeerState {

        /**
         * Is in choking state
         */
        private boolean choking;

        /**
         * Is in choking interested
         */
        private boolean interested;

        /**
         * Constructor
         * @param choking
         * @param interested
         */
        PeerState(boolean choking, boolean interested) {
            this.choking = choking;
            this.interested = interested;
        }

        public boolean isChoking() {
            return choking;
        }

        public void setChoking(boolean choking) {
            this.choking = choking;
        }

        public boolean isInterested() {
            return interested;
        }

        public void setInterested(boolean interested) {
            this.interested = interested;
        }
    }

    /**
     * Date and time when data was written to channel for this connection
     */
    private Date timeLastDataSent;

    /**
     * Date and time when data was read from channel for this connection
     */
    private Date timeLastDataReceived;

    /**
     * Channel for this connection
     */
    private SocketChannel channel;

    /**
     * Write buffer for network
     */
    private ByteBuffer networkWriteBuffer;

    /**
     * Size (bytes) of networkWriteBuffer
     */
    private final static int NETWORK_WRITE_BUFFER_SIZE = 10 * 1024 * 1024;

    /**
     * Read buffer for network
     */
    private ByteBuffer networkReadBuffer;

    /**
     * Size (bytes) of networkReadBuffer
     */
    private final static int NETWORK_READ_BUFFER_SIZE = 10 * 1024 * 1024;

    /**
     * Position in position in networkReadBuffer where data
     * begins which has not been processed by readMessagesFromChannel() and
     * subsequently turned into a message in readMessagesQueue
     */
    private int currentReadBufferPosition;
    private int copyAtEdgeEvent; // purely a performance statistic to assess whether circluar buffer is needed

    /**
     * Queue of messages which have been read but not processed
     */
    private LinkedList<Message> readMessagesQueue;

    // MappedByteBuffer <-- memory mapped

    /**
     *
     */
    public Connection() {

        this.clientState = new PeerState(false, false);
        this.peerState = new PeerState(false, false);
        this.networkWriteBuffer = ByteBuffer.allocateDirect(NETWORK_WRITE_BUFFER_SIZE);
        this.networkReadBuffer = ByteBuffer.allocateDirect(NETWORK_READ_BUFFER_SIZE);
        this.readMessagesQueue = new LinkedList<Message>();

        this.currentReadBufferPosition = 0;
        this.copyAtEdgeEvent = 0;
    }

    /**
     * Attempts to read from channel when OP_READ is registered,
     * and put full messages in readMessagesQueue.
     */
    public void readMessagesFromChannel() throws IOException, MessageToLargeForNetworkBufferException {

        // Read from channel into network read buffer
        int numberOfBytesRead = channel.read(networkReadBuffer);

        // Each iteration attempts to read one message from channel buffer
        int remainingBufferSize;
        while(true) {

            // Remaining space in buffer which has not been processed into a message
            remainingBufferSize = networkReadBuffer.position() - currentReadBufferPosition;

            // Do we have enough space in buffer for length field of new message?,
            // if not we are done reading from buffer, and we stop without
            // advancing buffer read position.
            if(remainingBufferSize < MessageWithLengthField.LENGTH_FIELD_SIZE)
                break;

            // Read length field of new message in as four byte big-endian integer
            int messageIdAndPayloadSize = networkReadBuffer.getInt(currentReadBufferPosition);

            // Get total size of message as claimed by length field
            int totalMessageSize = MessageWithLengthField.LENGTH_FIELD_SIZE + messageIdAndPayloadSize;

            // Check if peer is attempting to send a message we can never process,
            // that is a message greater than read buffer. If so we throw an exception.
            if(totalMessageSize > NETWORK_READ_BUFFER_SIZE)
                throw new MessageToLargeForNetworkBufferException(totalMessageSize, NETWORK_READ_BUFFER_SIZE);

            // Check that buffer does contain a full new message,
            // that is: <length><id><payload>.
            // if not we are done reading from buffer, and we stop without
            // advancing buffer read position.
            if (remainingBufferSize >= totalMessageSize) {

                // Wrap in a temporary read only buffer
                ByteBuffer temporaryBuffer = networkReadBuffer.wrap....asReadOnlyBuffer();

                // Generate a new message
                MessageWithLengthField m = MessageWithLengthField.create(temporaryBuffer);

                // Save message in read queue
                readMessagesQueue.add(m);
            }

            // Advance position in buffer
            currentReadBufferPosition += messageIdAndPayloadSize;
        }

        // If buffer is completely consumed, then we reset it
        // to postpone copy at edge event
        if(remainingBufferSize == 0) {
            networkReadBuffer.reset();
            currentReadBufferPosition = 0;
        } else if(!networkReadBuffer.hasRemaining()) {

            // If unconsumed data touches buffer limit,
            // then copy to the front of the buffer

            do_copying_here

            copyAtEdgeEvent++;
        }
    }

    /**
     * Attempts to write to channel when OP_WRITE is registered
     */
    public void writeMessagesToChannel() {

    }

    public Date getTimeLastDataSent() {
        return timeLastDataSent;
    }

    public void setTimeLastDataSent(Date timeLastDataSent) {
        this.timeLastDataSent = timeLastDataSent;
    }

    public Date getTimeLastDataReceived() {
        return timeLastDataReceived;
    }

    public void setTimeLastDataReceived(Date timeLastDataReceived) {
        this.timeLastDataReceived = timeLastDataReceived;
    }
}

/*
fields
        disk buffer?
        network buffer?
        isConnected
        constructor(socket, peer_id, reserved, torrent)
        save fields
        isConnected = true
        TorrentSwarm()
        if peer supports BEP10, send handshake with torrent.createExtensionHandshakeDictionary()
        while(1) :
        classic message
        torrent.processMessage(this, msg)
        BEP10
        handshake (note: this may be refreshed)
        update reverse lookup table
        extended messages
        extract extended message ID x
        map x to local Extension i
        call torrent.Extension[i].processMessage(this,ByteBuffer)
        on disconnect exception
        isConnected = false
        call torrent.peerDisConnected(this)
        WHAT DOES THREAD DO, SLEEP ?
private methods for extensions to call
private sendExtendedMessage(ByteBuffer/ExtendedMessage) : allows extensions to send messages to peer
        socket.send(buffer): on disconnect do as in TorrentSwarm() and return false to Extension!! (extension can use to put peer in ban list or something)
private closeConnection()
        something?
private disableExtension() : called to disable itself, perhaps if it is not longer needed
private enableExtension() called to enable itself, however, how will this ever be called?
*/