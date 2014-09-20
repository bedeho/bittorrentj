package org.bittorrentj.message;

import org.bittorrentj.exceptions.InvalidMessageReceivedException;
import org.bittorrentj.exceptions.MessageToLargeForNetworkBufferException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * Created by bedeho on 20.09.2014.
 *
 * 
 */
public class MessageStream {

    /**
     * Channel from/to which messages are read/written
     */
    SocketChannel channel;

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
     * The size of the window used to measure network io (up/down) speed
     * over, in milliseconds.
     */
    private final static int NETWORK_IO_SPEED_MEASUREMENT_WINDOW_SIZE = 1000; // (ms)

    /**
     * Counts the amount of raw data read into the network read buffer
     * within present averaging window.
     */
    private int rawDownloadCounter;


    MessageStream(SocketChannel channel) {

        this.networkReadBuffer = ByteBuffer.allocateDirect(NETWORK_READ_BUFFER_SIZE);
        this.startPositionOfDataInReadBuffer = 0;
        this.copyAtEdgeEvent = 0;

    }

    int read(LinkedList<MessageWithLengthField> readMessagesQueue ) throws IOException, MessageToLargeForNetworkBufferException, InvalidMessageReceivedException  {


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
                 * that is a message greater than read buffer. If so we throw an exceptions.
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
                        m = MessageWithLengthField.create(temporaryBuffer, activeClientExtensions);
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

        return numberOfBytesRead;
    }

}
