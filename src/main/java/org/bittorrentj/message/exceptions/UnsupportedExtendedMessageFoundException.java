package org.bittorrentj.message.exceptions;

/**
 * Created by bedeho on 09.09.2014.
 */
public class UnsupportedExtendedMessageFoundException extends Exception {

    /**
     * Extended id of extended message found.
     */
    private byte extendedId;

    public UnsupportedExtendedMessageFoundException(byte extendedId) {
        this.extendedId = extendedId;
    }

    public byte getExtendedId() {
        return extendedId;
    }
}
