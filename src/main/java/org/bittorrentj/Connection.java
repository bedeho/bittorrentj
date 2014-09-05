package org.bittorrentj;

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
    }

    //private ConnectionStatistics statistics;

    /*
    public enum State {
        BEFORE_HANDSHAKE(1),
    }

    public enum bufferState {

    }
    */

    /**
     * Attempts to read from channel when OP_READ is registered
     */
    public void readMessagesFromChannel() throws IOException {

        // Read from channel into network read buffer
        int numberOfBytesRead = channel.read(networkReadBuffer);

        // Each iteration reads one message from channel buffer
        while(true) {

            // Remaining space in buffer which has not been processed into a message
            int remainingBufferSize = networkReadBuffer.position() - currentReadBufferPosition;

            // Do we have enough space in buffer for length field of new message?,
            // if not we are done reading from buffer
            if(remainingBufferSize < MessageWithLengthField.LENGTH_FIELD_SIZE)
                break;

            // Read length of new message in as four byte big-endian integer
            int messageLength = networkReadBuffer.getInt(currentReadBufferPosition);

            // make some id, or not, depends on if length == 0

            // Check that buffer does contain a full new message,
            // if not we are done reading from buffer
            if(remainingBufferSize >= messageLength) // include id ?
               break;

            // Check that the id field after the length field is a valid bittorrent message id

            // Wrap this part of the read buffer, and generate a fresh message which is
            // inserted in readMessagesQueue

            // Advance position in buffer
            currentReadBufferPosition += messageLength;


        }

        // if there is no un procssed ata left: then reset it so that we postpone and eventual copy event
        // else: {
        //          if the unprocssed data touches the end of buffer, then copy the whole thing to the start of buffer to not waste space

    }

    /**
     * Attempts to write to channel when OP_WRITE is registered
     */
    public void writeMessagesToChannel() {

    }

    /**
     * Adds a message to writing queue
     * @param m
     * @return
     */
    public boolean enqueueMessageForSending(Message m) {

    }

    public boolean enqueueMessageRead(Message m) {

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