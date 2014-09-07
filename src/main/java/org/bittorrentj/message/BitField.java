package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.IncorrectIdFieldInMessageException;
import org.bittorrentj.message.exceptions.IncorrectLengthFieldInMessageException;
import org.bittorrentj.message.exceptions.InvalidBitFieldLengthInBitFieldMessageException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.InvalidMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class BitField extends MessageWithLengthAndIdField {

    /**
     * The payload is the zero-based index of the piece
     */
    private boolean[] bitfield;

    /**
     * Constructor based on boolean array representation of
     * bitfield, the length of which matches exactly the
     * number of pieces the bitfield represents. I.e,
     * trailing overflow bits are not represented
     * @param bitfield bitfield for this message
     */
    public BitField(boolean[] bitfield) {
        super(MessageId.BITFIELD);

        this.bitfield = bitfield;
    }

    public BitField(ByteBuffer src, int numberOfPiecesInTorrent) throws IncorrectLengthFieldInMessageException, InvalidMessageIdException, IncorrectIdFieldInMessageException, InvalidBitFieldLengthInBitFieldMessageException {

        // Read and process length and id fields
        super(MessageId.BITFIELD, src);

        // Read piece index
        this.bitfield = ...zzzz(src.getInt());

        // Check that bitfield
        if(bitfield.length != numberOfPiecesInTorrent)
            throw new InvalidBitFieldLengthInBitFieldMessageException(bitfield.length, numberOfPiecesInTorrent);
    }

    public boolean getPieceAvailability(int pieceIndex) {
        return bitfield[pieceIndex];
    }

    public int getNumberOfPieces() {
        return bitfield.length;
    }

    /**
     * Computes number of bytes requried in raw
     * wire representation of bitfield property.
     * Notice: Does not give size of BitField message itself,
     * use getRawMessageLength() for this.
     * @return
     */
    public int getLengthOfOnlyBitField() {
        return (int)Math.ceil(bitfield.length/8);
    }

    @Override
    public int getRawMessageLength() {
        return LENGTH_FIELD_SIZE + ID_FIELD_SIZE + getLengthOfOnlyBitField(); // round up to nearest byte
    }

    @Override
    protected void writeMessageToBuffer(ByteBuffer dst) {

        // Write to buffer, and advance position
        dst.putInt(getRawMessageLength()).put(id.getRaw());

        // Convert to byte bitfield, first allocate space with all zeros
        byte [] binaryBitfield = new byte[getLengthOfOnlyBitField()];

        for(int i = 0;i < bitfield.length;i++) {

            // If given bit is set, then set it in binary field as well
            if(bitfield[i]) {

                // To which byte in the binary bitfield does this (i'th) bit correspond
                int byteLocation = (int) Math.floor((i + 1) / 8);

                // To which bit location within the given byteLocation does this (i'th) bit correspond
                int bitLocationWithinByte = i % 8;

                // Set bit
                binaryBitfield[byteLocation] |= (bitLocationWithinByte >> (byte)(0b10000000));
            }
        }

        // Write byte representation of bitfield
        dst.put(binaryBitfield);
    }
}
