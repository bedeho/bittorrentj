package org.bittorrentj.torrent;

/**
 * Created by bedeho on 03.09.2014.
 */

import org.bittorrentj.message.field.Hash;

import java.util.LinkedList;

/**
 *
 */
public abstract class Info {

    /**
     * Number of bytes in each piece (integer)
     */
    private int pieceLength;

    /**
     *  String consisting of the concatenation of all
     *  20-byte SHA1 hash values, one per piece (byte string, i.e. not urlencoded)
     */
    private LinkedList<Hash> pieces;

    /**
     *  (optional) this field is an integer.
     *  If it is set to "1", the client MUST publish
     *  its presence to get other peers ONLY via the
     *  trackers explicitly described in the metainfo file.
     *  If this field is set to "0" or is not present,
     *  the client may obtain peer from other means, e.g.
     *  PEX peer exchange, dht. Here, "private" may be read
     *  as "no external peer source".
     */
    private boolean private_;

    /**
     * Constructor
     * @param pieceLength bytes per each piece
     * @param pieces list of piece hashes
     * @param private_ privateness of torrent
     */
    public Info(int pieceLength, LinkedList<Hash> pieces, boolean private_) {

        this.pieceLength = pieceLength;
        this.pieces = pieces;
        this.private_ = private_;
    }

    public boolean getPrivate_() {
        return private_;
    }

    public void setPrivate_(boolean private_) {
        this.private_ = private_;
    }

    public int getPieceLength() {
        return pieceLength;
    }

    public void setPieceLength(int pieceLength) {
        this.pieceLength = pieceLength;
    }

    public LinkedList<Hash> getPieces() {
        return pieces;
    }

    public void setPieces(LinkedList<Hash> pieces) {
        this.pieces = pieces;
    }

    abstract public Hash computeInfoHash();
}