package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.BufferToSmallForMessageException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class KeepAlive extends MessageWithLengthField {

    @Override
    public int getRawLength() {
        return LENGTH_FIELD_SIZE;
    }

    @Override
    protected int writeMessageToBuffer(ByteBuffer dst) {

        // Write length field as one four byte big-endian int to buffer,
        // and position is advanced
        dst.putInt(getRawLength());

        // Return number of bytes written
        return getRawLength();
    }
}