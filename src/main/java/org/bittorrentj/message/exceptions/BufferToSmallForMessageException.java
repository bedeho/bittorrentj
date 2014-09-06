package org.bittorrentj.message.exceptions;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class BufferToSmallForMessageException extends MessageCreationException {

    /**
     * Size (bytes) of length field value which was to big.
     */
    int size;

    /**
     * Buffer which was to small
     */
    ByteBuffer buffer;

    public BufferToSmallForMessageException(int size, ByteBuffer buffer) {

        this.size = size;
        this.buffer = buffer;
    }

    public int getSize() {
        return size;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

}
