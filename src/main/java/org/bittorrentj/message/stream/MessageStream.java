package org.bittorrentj.message.stream;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by bedeho on 21.09.2014.
 */
public abstract class MessageStream {

    /**
     * Channel from/to which messages are read/written
     */
    SocketChannel channel;

    /**
     * Manages this message stream.
     */
    MessageStreamManager manager;

    /**
     * Buffer
     */
    protected ByteBuffer buffer;

    /**
     * Size (bytes) of buffer
     */
    public final static int BUFFER_SIZE = 10 * 1024 * 1024;

    MessageStream(SocketChannel channel, MessageStreamManager manager) {

        this.channel = channel;
        this.manager = manager;
        this.buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

}
