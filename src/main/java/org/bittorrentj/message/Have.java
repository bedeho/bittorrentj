package org.bittorrentj.message;

import org.bittorrentj.message.field.MessageId;

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

        this.id = MessageId.HAVE;
        this.pieceIndex = pieceIndex;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public void setPieceIndex(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    @Override
    public int getRawLength() {
        return LENGTH_FIELD_SIZE + ID_FIELD_SIZE + 4;
    }

    @Override
    protected int writeMessageToBuffer(ByteBuffer dst) {

        // Write to buffer, and advance position
        dst.putInt(getRawLength()).put(id.getRaw()).putInt(pieceIndex);

        // Return number of bytes
        return getRawLength();
    }
}
