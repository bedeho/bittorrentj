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

                // Clear buffer: pos = 0, lim = cap, mark discarded
                buffer.clear();

                // If we still have messages to write, then fill buffer
                if(!queue.isEmpty()) {

                    // Get message
                    MessageWithLengthField m = queue.poll();

                    // Is there space in buffer? otherwise throw exceptions
                    int messageLength = m.getRawMessageLength();
                    if(messageLength > BUFFER_SIZE)
                        throw new MessageToLargeForNetworkBufferException(messageLength, BUFFER_SIZE);
                    else
                        bytesInWriteBuffer = messageLength; // buffer will be filled with message

                    // Write raw message into network buffer
                    m.writeMessageToBuffer(buffer);

                    // Write buffer to socket
                    buffer.flip(); // make limit= position and position = 0, to facilitate writing into socket
                    numberOfBytesWritten = channel.write(buffer);

                } else
                    numberOfBytesWritten = 0; // otherwise we are done

            } else
                numberOfBytesWritten = channel.write(buffer); // write what is in buffer to socket

            if(numberOfBytesWritten > 0);
            // log data sent

            // Update number of bytes in buffer
            bytesInWriteBuffer -= numberOfBytesWritten;

        } while(numberOfBytesWritten > 0); // We are done for now if we can't write to socket right now

        return numberOfBytesWritten;
    }
}
