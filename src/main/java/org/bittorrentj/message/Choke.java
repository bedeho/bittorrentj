package org.bittorrentj.message;

import org.bittorrentj.message.field.MessageId;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class Choke extends MessageWithLengthAndIdField {

    public Choke() {
        this.id = MessageId.CHOKE;
    }

    @Override
    public int getRawLength() {
        return LENGTH_FIELD_SIZE + ID_FIELD_SIZE;
    }

    @Override
    protected int writeMessageToBuffer(ByteBuffer dst) {

        // Write to buffer, and advance position
        dst.putInt(getRawLength()).put(id.getRaw());

        // Return number of bytes
        return getRawLength();
    }
}