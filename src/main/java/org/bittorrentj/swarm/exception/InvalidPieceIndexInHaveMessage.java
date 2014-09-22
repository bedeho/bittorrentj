package org.bittorrentj.swarm.exception;

/**
 * Created by bedeho on 15.09.2014.
 */
public class InvalidPieceIndexInHaveMessage extends Exception {

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
