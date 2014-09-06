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
     * Byte length of wire representation of full message (<length><id><payload>), including
     * length field itself, and any potential id field and payload
     * @return byte length
     */
    abstract public int getRawMessageLength();

    /**
     * Creates a rewinded (pos = 0, lim = cap, no mark) buffer filled with
     * raw wire representation of message. Altering buffer and/or content
     * has no effect on this object.
     * @return wire representation of message as ByteBuffer
     */
    public ByteBuffer getRawMessage() {

        // Allocate fresh buffer of correct size
        ByteBuffer b = ByteBuffer.allocate(getRawMessageLength());

        // Fill with message
        writeMessageToBuffer(b);

        // Rewind
        b.rewind();

        // Return
        return b;
    }

    /**
     * Writes raw wire representation of message
     * into provided buffer at given position.
     * A buffer in big endian mode is expected, and position
     * of buffer is advanced to end of message written.
     * @param dst
     * @throws BufferToSmallForMessageException buffer does not have sufficient space for message
     */
    public void writeRawMessage(ByteBuffer dst) throws BufferToSmallForMessageException {

        // Check that there is enough space
        if(dst.remaining() < getRawMessageLength())
            throw new BufferToSmallForMessageException(getRawMessageLength(), dst);

        // Fill buffer
        writeMessageToBuffer(dst);
    }

    /**
     * Write wire representation of full message (<length><id><payload>) into buffer, while
     * trusting it has sufficient space. Position
     * of buffer is advanced to end of message written.
     * This is a helper routine, protected and thus not callable
     * outside class, which is used by the public writeRawMessage().
     * @param dst
     */
    abstract protected void writeMessageToBuffer(ByteBuffer dst);
}
