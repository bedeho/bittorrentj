package org.bittorrentj.message.stream;

import org.bittorrentj.exceptions.MessageToLargeForNetworkBufferException;
import org.bittorrentj.message.MessageWithLengthField;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * Created by bedeho on 21.09.2014.
 */
public class OutputMessageStream extends MessageStream {

    /**
     * The number of bytes in the write buffer.
     * We cannot use hasRemaining on buffer, since that
     * can also be state when buffer is perfectly empty.
     */
    private int bytesInWriteBuffer;

    public OutputMessageStream(SocketChannel channel, MessageStreamManager manager) {

        super(channel, manager);
        this.bytesInWriteBuffer = 0;
    }

    /**
     *
     * @param queue
     * @return
     * @throws IOException
     * @throws MessageToLargeForNetworkBufferException
     */
    public int write(LinkedList<MessageWithLengthField> queue) throws IOException, MessageToLargeForNetworkBufferException {

        // The number of bytes written into socket from write buffer in last iteration of next loop
        int numberOfBytesWritten = 0;

        do {

            // Does buffer have anything to be written
            if(bytesInWriteBuffer == 0) {

                // No, then if we still have messages to write, put in buffer
                if(!queue.isEmpty()) {

                    // Get message
                    MessageWithLengthField m = queue.poll();

                    // Is there space in buffer?
                    int messageLength = m.getRawMessageLength();

                    // otherwise throw exception
                    if(messageLength > BUFFER_SIZE)
                        throw new MessageToLargeForNetworkBufferException(messageLength, BUFFER_SIZE);

                    // Write raw message into network buffer
                    m.writeMessageToBuffer(buffer);

                    // Buffer was filled with full message
                    bytesInWriteBuffer = messageLength;

                    // Write buffer to socket
                    // removed from first line after if: buffer.clear();  // Clear buffer: pos = 0, lim = cap, mark discarded
                    //buffer.flip(); // make limit= position and position = 0, to facilitate writing into socket
                    //numberOfBytesWritten = channel.write(buffer);
                    buffer.clear(); // pos = 0, lim=cap
                    numberOfBytesWritten = write(bytesInWriteBuffer);

                } else // Otherwise we are done
                    numberOfBytesWritten = 0;

            } else // Write, and note number of bytes written from buffer to socket
                numberOfBytesWritten = write(bytesInWriteBuffer);

            if(numberOfBytesWritten > 0);
            // log data sent

            // Update number of bytes in buffer
            bytesInWriteBuffer -= numberOfBytesWritten;

        } while(numberOfBytesWritten > 0); // We are done for now if we can't write to socket right now

        return numberOfBytesWritten;
    }

    /**
     * Writes from buffer into socket channel
     * starting at present position
     * while respecting present transmission limit,
     * and noting how much was written.
     * @return number of bytes written
     * @throws IOException
     */
    protected int write(int upperLimit) throws IOException {

        // Prepare for transmission by updating limit on buffer
        updateTransmissionLimit(upperLimit);

        // Perform writing
        int numberOfBytesWritten = channel.write(buffer);

        // Notify manager of quantity written
        transmittedData(numberOfBytesWritten);

        // and return quantity to caller
        return numberOfBytesWritten;
    }
}
