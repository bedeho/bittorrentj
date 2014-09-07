package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.IncorrectIdFieldInMessageException;
import org.bittorrentj.message.exceptions.IncorrectLengthFieldInMessageException;
import org.bittorrentj.message.exceptions.InvalidPieceIndexInHaveMessage;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.InvalidMessageIdException;

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
     * @param numberOfPiecesInTorrent the number of pieces in the torrent to which this message corresponds, it is used to verify piece index read from buffer
     * @throws IncorrectLengthFieldInMessageException when length field does not match computed message length
     * @throws InvalidMessageIdException when id does not match have message id
     * @throws IncorrectIdFieldInMessageException when id field is invalid
     * @throws InvalidPieceIndexInHaveMessage when piece index is negative, or greater than number of pieces in torrent
     */
    public Have(ByteBuffer src, int numberOfPiecesInTorrent) throws IncorrectLengthFieldInMessageException, InvalidMessageIdException, IncorrectIdFieldInMessageException, InvalidPieceIndexInHaveMessage {

        // Read and process length and id fields
        super(MessageId.HAVE, src);

        // Read piece index
        this.pieceIndex = src.getInt();

        // Check that it is non-negative and also not to large
        if(this.pieceIndex < 0 || this.pieceIndex >= numberOfPiecesInTorrent)
            throw new InvalidPieceIndexInHaveMessage(this.pieceIndex, numberOfPiecesInTorrent);
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
