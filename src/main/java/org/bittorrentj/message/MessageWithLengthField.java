package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.BufferToSmallForMessageException;
import org.bittorrentj.message.field.exceptions.InvalidMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 *
 * Class for messages which have at least a length field,
 * and the only direct message subclass is the keep alive,
 * as it is the only message with only a length field.
 */
public abstract class MessageWithLengthField extends Message {

    /**
     * Size of this field in wire message
     */
    public final static int LENGTH_FIELD_SIZE = 4;

    /**
     * Factory method for producing a MessageWithLengthField
     * message based of the supplied byte buffer, whose
     * position is advanced to end of message read.
     * @param src buffer read from
     * @return message created
     * @throws BufferToSmallForMessageException if buffer has no space to read length field
     */
    public static MessageWithLengthField create(ByteBuffer src) throws BufferToSmallForMessageException, InvalidMessageIdException {

        /**
        * We confirm that length field is not greater than buffer.
        * When invoked by network read buffer, this is guaranteed not to be the case,
        * but we have to check in general.
        */
        if(src.remaining() < LENGTH_FIELD_SIZE)
            throw new BufferToSmallForMessageException(LENGTH_FIELD_SIZE, src);

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
    abstract public int getRawLength();

    @Override
    abstract protected int writeMessageToBuffer(ByteBuffer dst);

}