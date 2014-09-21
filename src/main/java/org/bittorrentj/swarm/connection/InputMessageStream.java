package org.bittorrentj.swarm.connection;

import org.bittorrentj.exceptions.InvalidMessageReceivedException;
import org.bittorrentj.exceptions.MessageToLargeForNetworkBufferException;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.MessageWithLengthField;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by bedeho on 20.09.2014.
 *
 * Stream which buffers communication with an underlying SocketChannel,
 * so as to create read and write methods on the Message level.
 * Also, the stream has reading/writing bandwidth utilization management.
 */
public class InputMessageStream {

    /**
     * Channel from/to which messages are read/written
     */
    SocketChannel channel;

    /**
     * All extensions registered.
     */
    HashMap<Integer, Extension> extensions;

    /**
     *
     */
    MessageStreamManager manager;

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
    private int copyAtEdgeEvent; // purely a performance statistic to assess whether circular buffer is needed

    /**
     * Constructor
     * @param channel
     * @param extensions
     * @param manager
     */
    InputMessageStream(SocketChannel channel, HashMap<Integer, Extension> extensions, MessageStreamManager manager) {

        this.channel = channel;
        this.extensions = extensions;
        this.manager = manager;
        this.networkReadBuffer = ByteBuffer.allocateDirect(NETWORK_READ_BUFFER_SIZE);
        this.startPositionOfDataInReadBuffer = 0;
        this.copyAtEdgeEvent = 0;
    }

    /**
     * Read as many messages from socket as possible
     * @param readMessagesQueue
     * @return number of bytes
     * @throws IOException
     * @throws MessageToLargeForNetworkBufferException
     * @throws InvalidMessageReceivedException
     */
    public int read(LinkedList<MessageWithLengthField> readMessagesQueue) throws IOException, MessageToLargeForNetworkBufferException, InvalidMessageReceivedException  {

        // The number of bytes read in total
        int totalNumberOfBytesRead = 0;

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
                        m = MessageWithLengthField.create(temporaryBuffer, extensions);
                    } catch (Exception e){
                        throw new InvalidMessageReceivedException(e);
                    }

                    // Save message in read queue
                    readMessagesQueue.add(m);

                    // Advance position in buffer
                    startPositionOfDataInReadBuffer += messageIdAndPayloadSize;
                } else
                    break;
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

            // Count number of bytes read as part of total for this call
            totalNumberOfBytesRead += numberOfBytesRead;
        }

        return totalNumberOfBytesRead;
    }
}
