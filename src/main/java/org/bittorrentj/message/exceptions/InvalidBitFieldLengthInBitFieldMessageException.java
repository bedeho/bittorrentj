package org.bittorrentj.message.exceptions;

/**
 * Created by bedeho on 07.09.2014.
 *
 * Exception thrown when in BitField constructor raw buffer constructor if length of bitfield
 * in buffer is does not match number of pieces in torrent.
 */
public class InvalidBitFieldLengthInBitFieldMessageException extends MessageCreationException {

    /**
     * The invalid bitfield length
     */
    private int invalidBitFieldLength;

    /**
     * The correct number of pieces in the torrent to which
     * this have message corresponds
     */
    private int numberOfPiecesInTorrent;

    public InvalidBitFieldLengthInBitFieldMessageException(int invalidBitFieldLength, int numberOfPiecesInTorrent) {
        this.invalidBitFieldLength = invalidBitFieldLength;
        this.numberOfPiecesInTorrent = numberOfPiecesInTorrent;
    }

    public int getInvalidBitFieldLength() {
        return invalidBitFieldLength;
    }

    public int getNumberOfPiecesInTorrent() {
        return numberOfPiecesInTorrent;
    }
}
