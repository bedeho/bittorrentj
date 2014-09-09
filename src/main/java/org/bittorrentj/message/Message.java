package org.bittorrentj.message;

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
    final public ByteBuffer getRawMessage() {

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
     * Write wire representation of full message (<length><id><payload>) into buffer, while
     * trusting it has sufficient space. Position
     * of buffer is advanced to end of message written.
     * A buffer in big endian mode is expected, and position
     * of buffer is advanced to end of message written.
     * @param dst buffer written to
     */
    abstract public void writeMessageToBuffer(ByteBuffer dst);
}
