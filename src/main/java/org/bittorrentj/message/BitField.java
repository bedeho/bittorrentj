package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.exceptions.InvalidBitFieldLengthInBitFieldMessageException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class BitField extends MessageWithLengthAndIdField {

    /**
     * Binary representation of bit field.
     */
    private byte[] bitField;

    /**
     * Constructor based on boolean array representation of the
     * bit field, the length of which matches exactly the
     * number of pieces in the torrent which the field represents.
     * Notice: trailing overflow bits should not represented
     * @param bitField bit field
     */
    public BitField(boolean[] bitField) {
        super(MessageId.BITFIELD);

        // Save as binary bitField
        this.bitField = booleanToByteBitField(bitField);
    }

    /**
     * Constructor based on wire representation.
     * @param src buffer
     * @throws UnrecognizedMessageIdException when message id field in buffer is not recognized
     * @throws org.bittorrentj.message.exceptions.NonMatchingIdFieldException when message id field in buffer does not match BITFIELD message id
     * @throws InvalidBitFieldLengthInBitFieldMessageException when header length field implies non-positive byte length bitField
     */
    public BitField(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException, InvalidBitFieldLengthInBitFieldMessageException {

        // Read length and id fields
        super(MessageId.BITFIELD, src);

        // Get length field
        int byteLengthOfBitfield = getMessageLengthField() - 1;

        // Verify that length field is not malicious
        if(byteLengthOfBitfield <= 0)
            throw new InvalidBitFieldLengthInBitFieldMessageException(byteLengthOfBitfield);

        // Copy from byte buffer into a byte array,
        // and then convert to boolean bitField
        byte[] b = new byte[byteLengthOfBitfield];
        src.get(b);
    }

    /**
     * Converts a boolean array representation of a bit field
     * to a binary representation.
     * @param b boolean array representation
     * @return byte representation
     */
    public static byte[] booleanToByteBitField(boolean[] b) {

        // Size of new byte representation
        int size = bitFieldByteLength(b.length);

        // Allocate space for byte representation
        byte [] binaryBitfield = new byte[size];

        // Iterate boolean bit field and set corresponding bit in byte bit field
        for(int i = 0;i < b.length;i++) {

            // If given bit is set, then set it in binary field as well
            if(b[i])
                setPieceAvailability(binaryBitfield, i, true);
        }

        return binaryBitfield;
    }

    /**
     * Computes the byte length of the binary representation of a bit field
     * based on the number of bits that must be represented.
     * @param numberOfBits number of bits that must be represented
     * @return byte length of bit field
     */
    public static int bitFieldByteLength(int numberOfBits) {
        return (int)Math.ceil(numberOfBits/8);
    }

    /**
     * Returns the availability status of the given piece,
     * as specified by a zero-based index, based on this bit field.
     * @param pieceIndex zero-based piece index
     * @return true iff it is available
     */
    public boolean getPieceAvailability(int pieceIndex) {
        return getPieceAvailability(this.bitField, pieceIndex);
    }

    public static boolean getPieceAvailability(byte[] b, int index) {

        // To which byte in the binary bitField does this (i\th) bit correspond
        int byteLocation = (int) Math.floor((index + 1) / 8);

        // To which bit location within the given byteLocation does this (i\th) bit correspond
        int bitLocationWithinByte = index % 8;

        // Check if bit is set
        return (b[byteLocation] & (bitLocationWithinByte >> (byte)(0b10000000))) != 0;
    }

    /**
     * Alters the availability status of the given piece,
     * as specified by a zero-based index, based on this bit field.
     * @param pieceIndex zero-based piece index
     * @param availability new availability status of piece
     */
    public void setPieceAvailability(int pieceIndex, boolean availability) {
        setPieceAvailability(this.bitField, pieceIndex, availability);
    }

    /**
     * Worker routine for manipulating bit field
     * @param b bit field
     * @param index bit index
     * @param availability new truth value of bit
     */
    public static void setPieceAvailability(byte[] b, int index, boolean availability) {

        // To which byte in the binary bitField does this (i\th) bit correspond
        int byteLocation = (int) Math.floor((index + 1) / 8);

        // To which bit location within the given byteLocation does this (i\th) bit correspond
        int bitLocationWithinByte = index % 8;

        // Alter bit based on availability
        if(availability)
            b[byteLocation] |= (bitLocationWithinByte >> (byte)(0b10000000));
        else
            b[byteLocation] &= (bitLocationWithinByte >> (byte)(0b01111111));
    }

    /**
     * Check tha validity of the bit field message, given
     * the number of pieces in the torrent which the message
     * corresponds to. It first checks that the bit field
     * is of the correct size, then it checks
     * that the trailing bits in the last byte are all zero.
     * The last check is required by the bittorrent specification.
     * @param numberOfPiecesInTorrent number of pieces in the torrent
     * @return true if bit field passes both checks
     */
    public boolean validateBitField(int numberOfPiecesInTorrent) {

        // Does the bitfield have the number of bytes corresponding
        // to this number of pieces in the torrent? (not equal, but corresponding)
        if(bitField.length != bitFieldByteLength(numberOfPiecesInTorrent))
            return false;

        // If a trailing bit is set to true, then this bit field is invalid
        for(int i = numberOfPiecesInTorrent;i < 8*bitField.length;i++) {
            if (getPieceAvailability(i))
                return false;
        }

        // If not, then it is valid
        return true;
    }

    @Override
    public int getRawPayloadLength() {
        return bitField.length;
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) {
        dst.put(this.bitField);
    }
}