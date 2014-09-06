package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.IncorrectLengthFieldInMessageException;
import org.bittorrentj.message.field.exceptions.InvalidMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class KeepAlive extends MessageWithLengthField {

    /**
     * Constructor based on binary buffer, position
     * is left at the end of the message just read.
     * Not clear why anyone would ever use this constructor
     * directly, given that it requires peeking a head
     * in buffer, but we have it anyway.
     * @param src buffer containing raw wire form of message
     * @throws IncorrectLengthFieldInMessageException
     */
    public KeepAlive(ByteBuffer src) throws InvalidMessageIdException,IncorrectLengthFieldInMessageException {
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
    protected void writeIdAndPayloadToBuffer(ByteBuffer dst) { } // no payload
}