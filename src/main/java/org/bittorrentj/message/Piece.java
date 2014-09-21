package org.bittorrentj.message;

import org.bittorrentj.swarm.connection.Connection;
import org.bittorrentj.message.exceptions.BlockLengthIsToShortException;
import org.bittorrentj.message.exceptions.BlockLengthToLargeException;
import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class Piece extends MessageWithLengthAndIdField {

    /**
     * The payload is the zero-based index of the piece
     */
    private int index;

    /**
     * Integer specifying the zero-based byte offset within the piece
     */
    private int begin;

    /**
     * block of data, which is a subset of the piece specified by index.
     */
    private byte [] block;

    /**
     * Constructor
     * @param index piece index.
     * @param begin byte offset within the piece.
     * @param block buffer.
     * @throws IllegalArgumentException if either parameter is negative, or length is equal to zero.
     */
    public Piece(int index, int begin, byte [] block) {
        super(MessageId.PIECE);

        this.index = index;
        this.begin = begin;
        this.block = block;

        // Check values
        if(index < 0 || begin < 0)
            throw new IllegalArgumentException();
    }

    /**
     * Constructor based on wire representation of message.
     * @param src buffer
     * @throws UnrecognizedMessageIdException if id is not recognized.
     * @throws NonMatchingIdFieldException when id in buffer does not match PIECE message id.
     * @throws IllegalArgumentException if either parameter is negative, or length is equal to zero.
     * @throws BlockLengthIsToShortException if block length field is to short.
     * @throws BlockLengthToLargeException if block length field is larger than the read buffer limit of Connection class.
     */
    public Piece(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException, BlockLengthIsToShortException, BlockLengthToLargeException {

        // Read and process length and id fields
        super(MessageId.PIECE, src);

        // Get payload length
        int blockLength = getMessageLengthField() - (LENGTH_FIELD_SIZE + ID_FIELD_SIZE + 4 + 4); // the two 4 bytes are for index and begin field respectively

        // Check that block is long enough
        if(blockLength <= 0)
            throw new BlockLengthIsToShortException();
        else if(blockLength > Connection.NETWORK_READ_BUFFER_SIZE)
            throw new BlockLengthToLargeException();

        // Read piece index
        this.index = src.getInt();
        this.begin = src.getInt();

        this.block = new byte[blockLength];
        src.put(this.block);

        // Check values
        if(index < 0 || begin < 0)
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
                begin < pieceSize;
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public byte [] getBlock () { return block; }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) {
        dst.putInt(index);
        dst.putInt(begin);
        dst.put(block);
    }

    @Override
    int getRawPayloadLength() {
        return 4 + 4 + block.length;
    }
}
