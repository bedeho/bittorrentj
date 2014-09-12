package org.bittorrentj.message;

import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class KeepAlive extends MessageWithLengthField {

    /**
     * Constructor based on wire representation of message. Position
     * of buffer is left at the end of the message just read.
     * Not clear why anyone would ever use this constructor
     * directly, given that it requires peeking a head
     * in buffer, but we have it anyway.
     * @param src buffer containing raw wire form of message
     */
    public KeepAlive(ByteBuffer src) throws UnrecognizedMessageIdException {
        super(src);
    }

    /**
     * Default constructor, is required since creating a
     * keep alive message directly requires no arguments
     */
    public KeepAlive() {}

    @Override
    public int getRawIdAndPayloadLength() {
        return 0;
    }

    @Override
    public void writeIdAndPayloadToBuffer(ByteBuffer dst) { }
}