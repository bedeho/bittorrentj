package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldInMessageException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class Have extends MessageWithLengthAndIdField {

    /**
     * The payload is the zero-based index of the piece
     */
    private int pieceIndex;

    /**
     * Constructor
     * @param pieceIndex zero based piece index
     */
    public Have(int pieceIndex) {
        super(MessageId.HAVE);

        this.pieceIndex = pieceIndex;
    }

    /**
     * Constructor based of raw wire representation in buffer
     * @param src buffer
     * @throws UnrecognizedMessageIdException
     * @throws NonMatchingIdFieldInMessageException when id in buffer does not match HAVE message id
     */
    public Have(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldInMessageException {

        // Read and process length and id fields
        super(MessageId.HAVE, src);

        // Read piece index
        this.pieceIndex = src.getInt();
    }

    /**
     * Checks when piece index is negative, or greater than number of pieces in torrent.
     * This routine is not part of constructor because its parameter may not be known at message
     * construction time, e.g. if client only knows info_hash and has to acquire
     * metainfo from peers through.
     * @param numberOfPiecesInTorrent the number of pieces in the torrent to which this message corresponds
     * @return true iff the piece index is non-negative and less than parameter
     */
    public boolean validatePieceIndex(int numberOfPiecesInTorrent) {
        return (this.pieceIndex < 0 || this.pieceIndex >= numberOfPiecesInTorrent);
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public void setPieceIndex(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) {
        dst.putInt(pieceIndex); // Write piece index
    }

    @Override
    int getRawPayloadLength() {
        return 4;
    }
}
