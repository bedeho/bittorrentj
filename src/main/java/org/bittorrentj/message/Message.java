package org.bittorrentj.message;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 30.08.2014.
 *
 * Base class for all wire messages
 */
public abstract class Message {

    /**
     * Creates a reset (pos = 0, lim = cap) ByteBuffer filled with
     * raw wire representation of message.
     * @return wire representation of message as ByteBuffer
     */
    abstract ByteBuffer getRaw();

    /**
     * Writes raw wire representation of message
     * into provided buffer at given position.
     * @param dst
     * @returns true iff dst has sufficient space remaining
     */
    abstract boolean getRaw(ByteBuffer dst);
}
