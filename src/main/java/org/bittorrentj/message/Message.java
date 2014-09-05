package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.BufferToSmallForMessageException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 30.08.2014.
 *
 * Base class for all wire messages,
 * the only direct subclass is HandshakeMessage,
 * as it is the only message without id and length fields
 */
public abstract class Message {

    /**
     * Byte length of wire representation of message, including
     * length field itself, and any potential id field and payload
     * @return byte length of wire representation
     */
    abstract public int getRawLength();

    /**
     * Creates a rewinded (pos = 0, lim = cap, no mark) buffer filled with
     * raw wire representation of message. Altering buffer and/or content
     * has no effect on this object.
     * @return wire representation of message as ByteBuffer
     */
    public ByteBuffer getRaw() {

        // Allocate fresh buffer of correct size
        ByteBuffer b = ByteBuffer.allocate(getRawLength());

        // Fill with message
        writeMessageToBuffer(b);

        // Rewind
        b.rewind();

        // Return
        return b;
    }

    /**
     * Writes raw wire representation of message
     * into provided buffer at given position. Position
     * of buffer is advanced to end of message written.
     * @param dst
     * @returns number of bytes written to buffer
     * @throws BufferToSmallForMessageException buffer does not have sufficient space for message
     */
    public int writeRaw(ByteBuffer dst) throws BufferToSmallForMessageException {

        // Check that there is enough space
        if(dst.remaining() < getRawLength())
            throw new BufferToSmallForMessageException(getRawLength(), dst);

        // Fill buffer and return it
        return writeMessageToBuffer(dst);
    }

    /**
     * Write wire representation of message into buffer, while
     * trusting it has sufficient space. Position
     * of buffer is advanced to end of message written.
     * @param dst
     * @return number of bytes written to buffer
     */
    abstract protected int writeMessageToBuffer(ByteBuffer dst);
}
