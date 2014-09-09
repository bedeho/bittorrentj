package org.bittorrentj.message.field.exceptions;

/**
 * Created by bedeho on 05.09.2014.
 */
public class UnrecognizedMessageIdException extends Exception {

    /**
     * Raw wire representation of id which was not recognized
     */
    private byte rawId;

    public UnrecognizedMessageIdException(byte rawId) {
        this.rawId = rawId;
    }

    public byte getRawId() {
        return rawId;
    }
}
