package org.bittorrentj;

import org.bittorrentj.exceptions.InvalidMessageReceivedException;
import org.bittorrentj.exceptions.MessageToLargeForNetworkBufferException;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.Message;
import org.bittorrentj.message.MessageWithLengthField;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
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
     * Read buffer for network
     */
    private ByteBuffer networkReadBuffer;

    /**
     * Size (bytes) of networkReadBuffer
     */
    public final static int NETWORK_READ_BUFFER_SIZE = 10 * 1024 * 1024;

    /**
     * Position in networkReadBuffer where data
     * begins which has not been processed by readMessagesFromChannel() and
     * subsequently turned into a message in readMessagesQueue
     */
    private int startPositionOfDataInReadBuffer;
    private int copyAtEdgeEvent; // purely a performance statistic to assess whether circluar buffer is needed

    /**
     * Write buffer for network
     */
    private ByteBuffer networkWriteBuffer;

    /**
     * Size (bytes) of networkWriteBuffer
     */
    private final static int NETWORK_WRITE_BUFFER_SIZE = 10 * 1024 * 1024;

    /**
     * The number of bytes in the write buffer.
     * We cannot use hasRemaining on buffer, since that
     * can also be state when buffer is perfectly empty.
     */
    private int bytesInWriteBuffer;

    /**
     * Queue of messages which have been read but not processed
     */
    private LinkedList<Message> readMessagesQueue;

    /**
     * Queue of messages which have been read but not processed
     */
    private LinkedList<Message> writeMessagesQueue;

    /**
     * Maps extension id to corresponding handler so that
     * messages
     */
    private HashMap<Integer, Extension> activeExtensions;

    // MappedByteBuffer <-- memory mapped

    /**
     * Constructor
     */
    public Connection(HashMap<Integer, Extension> activeExtensions) {

        this.activeExtensions = activeExtensions;

        this.clientState = new PeerState(false, false);
        this.peerState = new PeerState(false, false);
        this.networkWriteBuffer = ByteBuffer.allocateDirect(NETWORK_WRITE_BUFFER_SIZE);
        this.networkReadBuffer = ByteBuffer.allocateDirect(NETWORK_READ_BUFFER_SIZE);
        this.readMessagesQueue = new LinkedList<Message>();

        this.bytesInWriteBuffer = 0;
        this.startPositionOfDataInReadBuffer = 0;
        this.copyAtEdgeEvent = 0;
    }

    /**
     * Attempts to read from channel when OP_READ is registered,
     * and put full messages in readMessagesQueue.
     */
    public void readMessagesFromChannel() throws IOException, MessageToLargeForNetworkBufferException, InvalidMessageReceivedException {

        // The number of bytes read from socket into read buffer in last iteration of next loop
        int numberOfBytesRead;

        /**
         * Read from channel into network read buffer so long as we can,
         * this may be possible multiple times if read buffer is comparatively
         * smaller compared to kernel socket buffer and incoming traffic/bandwidth.
         */
        while((numberOfBytesRead = channel.read(networkReadBuffer)) > 0) {

            // Note where in buffer data ends
            int endPositionOfDataInReadBuffer = networkReadBuffer.position();

            // Each iteration attempts to read one message from channel buffer
            int numberOfUnconsumedBytes;
            while (true) {

                // Remaining space in buffer which has not been processed into a message
                numberOfUnconsumedBytes = endPositionOfDataInReadBuffer - startPositionOfDataInReadBuffer;

                /**
                * Do we have enough space in buffer for length field of new message?,
                * if not we are done reading from buffer, and we stop without
                * advancing buffer read position.
                */
                if (numberOfUnconsumedBytes < MessageWithLengthField.LENGTH_FIELD_SIZE)
                    break;

                // Read length field of new message in as four byte big-endian integer, without altering buffer position
                int messageIdAndPayloadSize = networkReadBuffer.getInt(startPositionOfDataInReadBuffer);

                // Get total size of message as claimed by length field
                int totalMessageSize = MessageWithLengthField.LENGTH_FIELD_SIZE + messageIdAndPayloadSize;

                /**
                * Check if peer is attempting to send a message we can never process,
                * that is a message greater than read buffer. If so we throw an exception.
                */
                if (totalMessageSize > NETWORK_READ_BUFFER_SIZE)
                    throw new MessageToLargeForNetworkBufferException(totalMessageSize, NETWORK_READ_BUFFER_SIZE);

                /**
                 * Check that buffer does contain a full new message,
                 * that is: <length><id><payload>.
                 * if not we are done reading from buffer, and we stop without
                 * advancing buffer read position.
                 */
                if (numberOfUnconsumedBytes >= totalMessageSize) {

                    // Wrap in a temporary read only buffer
                    ByteBuffer temporaryBuffer = networkReadBuffer.duplicate().asReadOnlyBuffer();

                    // and make sure this buffer starts where message starts
                    temporaryBuffer.position(startPositionOfDataInReadBuffer);

                    // and ends where message ends
                    temporaryBuffer.limit(startPositionOfDataInReadBuffer + totalMessageSize);

                    // Process buffer and generate new message
                    MessageWithLengthField m;

                    try {
                        m = MessageWithLengthField.create(temporaryBuffer, activeExtensions);
                    } catch (Exception e){
                        throw new InvalidMessageReceivedException(e);
                    }

                    // Save message in read queue
                    readMessagesQueue.add(m);

                    // Advance position in buffer
                    startPositionOfDataInReadBuffer += messageIdAndPayloadSize;
                }
            }

            // If buffer is completely consumed, then we reset it
            // to postpone copy at edge event
            if (numberOfUnconsumedBytes == 0) {
                networkReadBuffer.clear();
                startPositionOfDataInReadBuffer = 0;
            } else if (!networkReadBuffer.hasRemaining()) {

                // If unconsumed data touches buffer limit,
                // then copy to the front of the buffer.

                // We use compact() to do this, and therefor we have to
                // first set position to data we want at start of buffer
                networkReadBuffer.position(startPositionOfDataInReadBuffer);

                // and then limit is set to end of data.
                networkReadBuffer.limit(endPositionOfDataInReadBuffer);

                // Do copying
                networkReadBuffer.compact();

                // Note this compacting event, since its costly.
                copyAtEdgeEvent++;
            }

            if(numberOfBytesRead > 0);
                // log bytes written

        }
    }

    /**
     * Attempts to write to channel when OP_WRITE is registered,
     * by writing messages from writeMessagesQueue.
     */
    public void writeMessagesToChannel() throws IOException, MessageToLargeForNetworkBufferException {

        // The number of bytes written into socket from write buffer in last iteration of next loop
        int numberOfBytesWritten = 0;

        do {

            // Does buffer have anything to be written
            if(bytesInWriteBuffer == 0) { // <--------------- this does not work!!!!

                // Clear buffer: pos = 0, lim = cap, mark discarded
                networkWriteBuffer.clear();

                // If we still have messages to write, then fill buffer
                if(!writeMessagesQueue.isEmpty()) {

                    // Get message
                    Message m = writeMessagesQueue.poll();

                    // Is there space in buffer? otherwise throw exception
                    int messageLength = m.getRawMessageLength();
                    if(messageLength > NETWORK_WRITE_BUFFER_SIZE)
                        throw new MessageToLargeForNetworkBufferException(messageLength, NETWORK_WRITE_BUFFER_SIZE);
                    else
                        bytesInWriteBuffer = messageLength; // buffer will be filled with message

                    // Write raw message into network buffer
                    m.writeMessageToBuffer(networkWriteBuffer);

                    // Write buffer to socket
                    networkWriteBuffer.flip(); // make limit= position and position = 0, to facilitate writing into socket
                    numberOfBytesWritten = channel.write(networkWriteBuffer);

                } else
                    numberOfBytesWritten = 0; // otherwise we are done

            } else
                numberOfBytesWritten = channel.write(networkWriteBuffer); // write what is in buffer to socket

            if(numberOfBytesWritten > 0);
                // log data sent

            // Update number of bytes in buffer
            bytesInWriteBuffer -= numberOfBytesWritten;

        } while(numberOfBytesWritten > 0); // We are done for now if we can't write to socket right now

    }

    /**
     *
     * @param m
     */
    public void enqueueMessageForSending(Message m) {
        writeMessagesQueue.add(m);
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