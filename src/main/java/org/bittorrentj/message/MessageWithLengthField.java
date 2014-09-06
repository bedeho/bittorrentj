package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.BufferToSmallForMessageException;
import org.bittorrentj.message.exceptions.IncorrectLengthFieldInMessageException;
import org.bittorrentj.message.exceptions.MessageCreationException;
import org.bittorrentj.message.field.exceptions.InvalidMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 *
 * Class for messages which have at least a length field,
 * and the only direct message subclass is the keep alive,
 * as it is the only message with only a length field.
 *
 */
public abstract class MessageWithLengthField extends Message {

    /**
     * Size of this field in wire message
     */
    public final static int LENGTH_FIELD_SIZE = 4;

    /**
     * Constructor based on binary buffer, position
     * is left at the end of the message just read.
     * Since this class is abstract, it is only invoked
     * by subclass constructors.
     * @param src buffer containing raw wire form of message
     * @throws IncorrectLengthFieldInMessageException when length field of buffer does not match length computed by getRawMessageLength()
     */
    protected MessageWithLengthField(ByteBuffer src) throws IncorrectLengthFieldInMessageException, InvalidMessageIdException {

        // Recover length field in buffer
        int messageLength = src.getInt();

        // Check that message actually has the claimed length
        int computedLength = getRawMessageLength();
        if(messageLength != computedLength)
            throw new IncorrectLengthFieldInMessageException(messageLength, computedLength);
    }

    /**
     * Default constructor, is needed by constructor of keep alive
     * (direct subclass) since it requires no arguments.
     */
    protected MessageWithLengthField() {}

    /**
     * Factory method for producing a MessageWithLengthField
     * message based of the supplied byte buffer, whose
     * position is advanced to end of message read.
     * @param src buffer read from
     * @return message created
     * @throws MessageCreationException
     * @throws InvalidMessageIdException
     */
    public static MessageWithLengthField create(ByteBuffer src) throws MessageCreationException, InvalidMessageIdException {

        // Minimum size of messages of this class
        int minSize = LENGTH_FIELD_SIZE;

        /**
        * Confirm that message has space for smallest possible message of this class.
        * When invoked by network read buffer, this is guaranteed to be the case,
        * but we have to check in general.
        */
        if(src.remaining() < minSize)
            throw new BufferToSmallForMessageException(minSize, src);

        // Otherwise read length field
        int messageIdAndPayloadSize = src.getInt();

        /**
        * messageIdAndPayloadSize == 0 implies that this must be a
        * keep alive message, if so, just return such a message.
        * If not, then message must have id field, and so call id message factory instead.
        */
        if(messageIdAndPayloadSize == 0)
            return new KeepAlive();
        else {

            // Rewind the buffer to start of length field, since this
            // is what next factory expects
            src.position(src.position() - LENGTH_FIELD_SIZE);

            // Call factory and return result
            return MessageWithLengthAndIdField.create(src);
        }
    }

    @Override
    public int getRawMessageLength() {
        return LENGTH_FIELD_SIZE + getRawIdAndPayloadLength();
    }

    @Override
    protected void writeMessageToBuffer(ByteBuffer dst) {

        // Write four byte big endian length field
        dst.putInt(getRawMessageLength());

        // Any potential id and payload fields are written in this call
        writeIdAndPayloadToBuffer(dst);
    }

    /**
     * Write wire representation of id and payload fields (if they exist) into buffer, while
     * trusting it has sufficient space. Position
     * of buffer is advanced to end of message written.
     * This is a helper routine, protected and thus not callable
     * outside class, which is used by the public writeRawMessage().
     * @param dst
     */
    abstract protected void writeIdAndPayloadToBuffer(ByteBuffer dst);

    /**
     * Byte length of wire representation of id and
     * payload fields of message (if they exist).
     * @return byte length
     */
    abstract public int getRawIdAndPayloadLength();
}