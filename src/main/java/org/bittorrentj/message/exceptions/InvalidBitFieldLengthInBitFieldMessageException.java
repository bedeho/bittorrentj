package org.bittorrentj.message.exceptions;

/**
 * Created by bedeho on 07.09.2014.
 *
 * Exception thrown when in BitField constructor raw buffer constructor if
 * message length field, minus 1, becomes to small (<= 0 bytes).
 */
public class InvalidBitFieldLengthInBitFieldMessageException extends MessageCreationException {

    /**
     * The invalid bitfield length
     */
    private int invalidBitFieldLength;

    public InvalidBitFieldLengthInBitFieldMessageException(int invalidBitFieldLength) {
        this.invalidBitFieldLength = invalidBitFieldLength;
    }

    public int getInvalidBitFieldLength() {
        return invalidBitFieldLength;
    }
}