package org.bittorrentj.message.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by bedeho on 21.09.2014.
 *
 * Abstract parent class for input and output message streams.
 */
public abstract class MessageStream {

    /**
     * Channel from/to which messages are read/written
     */
    protected SocketChannel channel;

    /**
     * Manages this message stream.
     */
    private MessageStreamManager manager;

    /**
     * Buffer
     */
    protected ByteBuffer buffer;

    /**
     * Size (bytes) of buffer
     */
    public final static int BUFFER_SIZE = 10 * 1024 * 1024;

    /**
     * Constructor
     * @param channel
     * @param manager
     */
    MessageStream(SocketChannel channel, MessageStreamManager manager) {

        this.channel = channel;
        this.manager = manager;
        this.buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    /**
     * Wraps transmittedData in manager.
     * This should be called as promptly as possible
     * after actual transmission has occurred, so as to fall within
     * the same window as the updateTransmissionLimit() call.
     * @param numberOfBytes
     * @return
     */
    protected void transmittedData(int numberOfBytes) {
        manager.transmittedData(numberOfBytes);
    }

    /**
     * Updates the limit on the buffer
     * to respect the transmission limit at present.
     * @return maximum transmittable at present
     */
    protected int updateTransmissionLimit() {

        // How much can be transmitted at most at present
        int maxTransmitted = manager.maximumTransmittableDataAtThisTime();

        // Set limit of buffer to respect this
        buffer.limit(buffer.position() + maxTransmitted);

        return maxTransmitted;
    }

    /**
     * Updates the limit on the buffer
     * w.r.t to transmission limit and explicit
     * limit argument, which typically represents
     * end of packet in buffer being prepared for writing
     * to socket.
     * @param limit
     * @return maximum transmittable at present
     */
    protected int updateTransmissionLimit(int limit) {

        // How much can be transmitted at most at present
        int maxTransmitted = manager.maximumTransmittableDataAtThisTime();

        // Pick the smaller of the two
        int actualLimit = maxTransmitted < limit ? maxTransmitted : limit;

        // Set limit of buffer to respect this
        buffer.limit(buffer.position() + actualLimit);

        return actualLimit;
    }
}
