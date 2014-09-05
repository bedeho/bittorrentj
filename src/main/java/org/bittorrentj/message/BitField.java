package org.bittorrentj.message;

import org.bittorrentj.message.field.MessageId;

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
     * Constructor
     * @param bitfield bitfield for this message
     */
    public BitField(boolean[] bitfield) {

        this.id = MessageId.BITFIELD;
        this.bitfield = bitfield;
    }

    // Do I need both.....??
    public BitField(byte[] bitfield) {

    }


    public boolean getPieceAvailability(int pieceIndex) {
        return bitfield[pieceIndex];
    }

    /**
     * Computes number of bytes requried in raw
     * wire representation of bitfield property.
     * Notice: Does not give size of BitField message itself,
     * use getRawLength() for this.
     * @return
     */
    public int getLengthOfOnlyBitField() {
        return (int)Math.ceil(bitfield.length/8);
    }

    @Override
    public int getRawLength() {
        return LENGTH_FIELD_SIZE + ID_FIELD_SIZE + getLengthOfOnlyBitField(); // round up to nearest byte
    }

    @Override
    protected int writeMessageToBuffer(ByteBuffer dst) {

        // Write to buffer, and advance position
        dst.putInt(getRawLength()).put(id.getRaw());

        // Convert to binary bitfield
        byte [] binaryBitfield = new byte[getLengthOfOnlyBitField()];

        for(int i = 0;i < bitfield.length;i++) {

            int byteLocationOfThisBitInBinaryBitField = (int)Math.floor(i/8);




        }

        dst.put(....)

        // Return number of bytes
        return getRawLength();
    }
}
