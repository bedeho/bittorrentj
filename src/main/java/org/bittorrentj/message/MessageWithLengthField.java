package org.bittorrentj.message;

/**
 * Created by bedeho on 05.09.2014.
 */
public abstract class MessageWithLengthField extends Message {

    /**
     * Size of this field in wire message
     */
    public final static int LENGTH_FIELD_SIZE = 4;

    /**
     * Length of message, including
     * length field itself, and any potential id field
     * and payload
     * @return
     */
    abstract public int getMessageLength();
}