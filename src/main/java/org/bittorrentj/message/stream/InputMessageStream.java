package org.bittorrentj.message.stream;

import org.bittorrentj.exceptions.InvalidMessageReceivedException;
import org.bittorrentj.exceptions.MessageToLargeForNetworkBufferException;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.MessageWithLengthField;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by bedeho on 20.09.2014.
 *
 * Stream which buffers communication with an underlying SocketChannel,
 * so as to create read and write methods on the Message level.
 * Also, the stream has reading/writing bandwidth utilization management.
 */
public class InputMessageStream extends MessageStream {

    /**
     * All extensions registered.
     */
    HashMap<Integer, Extension> extensions;

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
     * @param manager
     * @param extensions
     */
    InputMessageStream(SocketChannel channel, MessageStreamManager manager, HashMap<Integer, Extension> extensions) {

        super(channel, manager);

        this.extensions = extensions;
        this.startPositionOfDataInReadBuffer = 0;
        this.copyAtEdgeEvent = 0;
    }

    /**
     * Read as many messages from socket as possible
     * @param queue
     * @return number of bytes
     * @throws IOException
     * @throws MessageToLargeForNetworkBufferException
     * @throws InvalidMessageReceivedException
     */
    public int read(LinkedList<MessageWithLengthField> queue) throws IOException, MessageToLargeForNetworkBufferException, InvalidMessageReceivedException  {

        // How much can be read at most at present
        int maxBytesToRead = manager.maximumTransmittableDataAtThisTime();

        // Set limit of buffer to respect this

        // The number of bytes read from socket into buffer
        int numberOfBytesRead = channel.read(buffer);

        // Remove limit


        // Note where in buffer data ends
        int endPositionOfDataInReadBuffer = buffer.position();

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
            int messageIdAndPayloadSize = buffer.getInt(startPositionOfDataInReadBuffer);

            // Get total size of message as claimed by length field
            int totalMessageSize = MessageWithLengthField.LENGTH_FIELD_SIZE + messageIdAndPayloadSize;

            /**
             * Check if peer is attempting to send a message we can never process,
             * that is a message greater than read buffer. If so we throw an exceptions.
             */
            if (totalMessageSize > BUFFER_SIZE)
                throw new MessageToLargeForNetworkBufferException(totalMessageSize, BUFFER_SIZE);

            /**
             * Check that buffer does contain a full new message,
             * that is: <length><id><payload>.
             * if not we are done reading from buffer, and we stop without
             * advancing buffer read position.
             */
            if (numberOfUnconsumedBytes >= totalMessageSize) {

                // Wrap in a temporary read only buffer
                ByteBuffer temporaryBuffer = buffer.duplicate().asReadOnlyBuffer();

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
                queue.add(m);

                // Advance position in buffer
                startPositionOfDataInReadBuffer += messageIdAndPayloadSize;
            } else
                break;
        }

        // If buffer is completely consumed, then we reset it
        // to postpone copy at edge event
        if (numberOfUnconsumedBytes == 0) {
            buffer.clear();
            startPositionOfDataInReadBuffer = 0;
        } else if (!buffer.hasRemaining()) {

            // If unconsumed data touches buffer limit,
            // then copy to the front of the buffer.

            // We use compact() to do this, and therefor we have to
            // first set position to data we want at start of buffer
            buffer.position(startPositionOfDataInReadBuffer);

            // and then limit is set to end of data.
            buffer.limit(endPositionOfDataInReadBuffer);

            // Do copying
            buffer.compact();

            // Note this compacting event, since its costly.
            copyAtEdgeEvent++;
        }

        // log bytes written
        // if(numberOfBytesRead > 0);

        // Notify manager
        manager.transmittedData(numberOfBytesRead);

        // Return how much was read
        return numberOfBytesRead;
    }
}
