package org.bittorrentj.message.exceptions;

/**
 * Created by bedeho on 07.09.2014.
 *
 * Exception thrown when in Have constructor raw buffer constructor if piece index
 * in buffer is negative or to large.
 */
public class InvalidPieceIndexInHaveMessage extends MessageCreationException {

    /**
     * The invalid piece index
     */
    private int invalidPieceIndex;

    /**
     * The correct number of pieces in the torrent to which
     * this have message corresponds
     */
    private int numberOfPiecesInTorrent;

    public InvalidPieceIndexInHaveMessage(int invalidPieceIndex, int numberOfPiecesInTorrent) {
        this.invalidPieceIndex = invalidPieceIndex;
        this.numberOfPiecesInTorrent = numberOfPiecesInTorrent;
    }

    public int getInvalidPieceIndex() {
        return invalidPieceIndex;
    }

    public int getNumberOfPiecesInTorrent() {
        return numberOfPiecesInTorrent;
    }
}
