package org.bittorrentj.message;

/**
 * Created by bedeho on 05.09.2014.
 */

import org.bittorrentj.message.exceptions.BufferToSmallForMessageException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.InvalidMessageIdException;

import java.nio.ByteBuffer;

/**
 *
 * Messages of this type take following form on wire: <length prefix><message ID><payload>.
 */
public abstract class MessageWithLengthAndIdField extends MessageWithLengthField {

    /**
     * Size of this field in wire message
     */
    public final static int ID_FIELD_SIZE = 1;

    /**
     * Id of message
     */
    MessageId id;

    /**
     * Id of message
     * @return id of message
     */
    public MessageId getId() {
        return id;
    }

    /**
     * Factory method for producing a MessageWithLengthField
     * object based of the supplied byte buffer, whose
     * position is advanced to end of message read.
     * @param src buffer read from
     * @return message created
     * @throws BufferToSmallForMessageException if buffer has no space to read length field
     */
    public static MessageWithLengthAndIdField create(ByteBuffer src) throws BufferToSmallForMessageException, InvalidMessageIdException {

        /**
         * We confirm that length field is not greater than buffer.
         * When invoked by MessageWithLengthField.create(), this is guaranteed not to be the case,
         * but we have to check in general.
         */

        if(src.remaining() < LENGTH_FIELD_SIZE + ID_FIELD_SIZE)
            throw new BufferToSmallForMessageException(LENGTH_FIELD_SIZE + ID_FIELD_SIZE, src);

        // Otherwise read length field
        int messageIdAndPayloadSize = src.getInt();

        // Read raw id
        byte rawId = src.get();

        // Convert to MessageId to extract message, may
        // case exception if not recognized
        MessageId id = MessageId.getMessageIdFromRaw(rawId);

        // Call upon correct constructor
        switch(id) {
            case CHOKE:

                break;
            case UNCHOKE:

                break;
            case INTERESTED:

                break;
            case NOT_INTERESTED:

                break;
            case HAVE:

                break;
            case BITFIELD:

                break;
            case REQUEST:

                break;
            case PIECE:

                break;
            case CANCEL:

                break;
            case PORT:
                break;

            case EXTENDED: // what to do here
                break;
        }
    }

    @Override
    abstract public int getRawLength();

    @Override
    abstract protected int writeMessageToBuffer(ByteBuffer dst);
}