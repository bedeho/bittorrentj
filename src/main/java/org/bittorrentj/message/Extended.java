package org.bittorrentj.message;

/**
 * Created by bedeho on 05.09.2014.
 */
public abstract class Extended extends MessageWithLengthAndIdField {

    /**
     * Size of extended id field in wire message
     */
    public final static int EXTENDED_ID_FIELD_SIZE = 1;

    /**
     * Id of extended message, ID = 0 is reserved for
     * handshake message.
     */
    private int extendedMessageId;
}
