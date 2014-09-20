package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class Request extends MessageWithLengthAndIdField {

    /**
     * The payload is the zero-based index of the piece
     */
    private int index;

    /**
     * Integer specifying the zero-based byte offset within the piece
     */
    private int begin;

    /**
     * Integer specifying the requested length
     */
    private int length;

    /**
     * Constructor
     * @param index piece index.
     * @param begin byte offset within the piece.
     * @param length requested length.
     * @throws IllegalArgumentException if either parameter is negative, or length is equal to zero.
     */
    public Request(int index, int begin, int length) {
        super(MessageId.REQUEST);

        this.index = index;
        this.begin = begin;
        this.length = length;

        // Check values
        if(index < 0 || begin < 0 || length <= 0)
            throw new IllegalArgumentException();
    }

    /**
     * Constructor based on wire representation of message.
     * @param src buffer
     * @throws UnrecognizedMessageIdException if id is not recognized.
     * @throws NonMatchingIdFieldException when id in buffer does not match REQUEST message id.
     * @throws IllegalArgumentException if either parameter is negative, or length is equal to zero.
     */
    public Request(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException {

        // Read and process length and id fields
        super(MessageId.REQUEST, src);

        // Read piece index
        this.index = src.getInt();
        this.begin = src.getInt();
        this.length = src.getInt();

        // Check values
        if(index < 0 || begin < 0 || length <= 0)
            throw new IllegalArgumentException();
    }

    /**
     * Checks when piece index is negative, or greater than number of pieces in torrent.
     *
     * This routine is not part of constructor because its parameter may not be known at message
     * construction time, e.g. if client only knows info_hash and has to acquire
     * metainfo from peers through.
     * @param numberOfPiecesInTorrent the number of pieces in the torrent to which this message corresponds.
     * @param pieceSize the byte size of each piece in torrent.
     * @return false iff the piece index is negative or less than numberOfPiecesInTorrent, or begin parameter is negative or greater than the number of pieces
     */
    public boolean validate(int numberOfPiecesInTorrent, int pieceSize) {
        return index >= 0 &&
               index < numberOfPiecesInTorrent &&
               begin >= 0 &&
               begin < pieceSize &&
               length >= 0;
               // length < pieceSize-begin; ???
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public int getLength() {
        return length;
    }

    public Cancel toCancelMessage() {
        return new Cancel(index, begin, length);
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) {
        dst.putInt(index);
        dst.putInt(begin);
        dst.putInt(length);
    }

    @Override
    int getRawPayloadLength() {
        return 3*4;
    }
}