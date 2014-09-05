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
    protected MessageId id;

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

        // Minimum size of messages of this class
        int minSize = LENGTH_FIELD_SIZE + ID_FIELD_SIZE;

        /**
         * We confirm that length field is not greater than buffer.
         * When invoked by MessageWithLengthField.create(), this is guaranteed not to be the case,
         * but we have to check in general.
         */

        if(src.remaining() < minSize)
            throw new BufferToSmallForMessageException(minSize, src);

        // Otherwise read length field
        int messageIdAndPayloadSize = src.getInt();

        // Read raw id
        byte rawId = src.get();

        // Rewind buffer to start of message as
        // promised to constructors of message objects
        src.position(src.position() - minSize);

        // Convert to MessageId to extract message, may
        // case exception if not recognized
        MessageId id = MessageId.getMessageIdFromRaw(rawId);

        // Call upon correct constructor
        switch(id) {
            case CHOKE:
                return new Choke(src);
                break;
            case UNCHOKE:
                return new UnChoke(src);
                break;
            case INTERESTED:
                return new Interested(src);
                break;
            case NOT_INTERESTED:
                return new NotInterested(src);
                break;
            case HAVE:
                return new Have(src);
                break;
            case BITFIELD:
                return new BitField(src);
                break;
            case REQUEST:
                return new Request(src);
                break;
            case PIECE:
                return new Piece(src);
                break;
            case CANCEL:
                return new Cancel(src);
                break;
            case PORT:
                break;
                return new Port(src);
            case EXTENDED: // what to do here
                break;
        }
    }

    public MessageId getMessageId(){
        return id;
    }

    @Override
    abstract public int getRawLength();

    @Override
    abstract protected int writeMessageToBuffer(ByteBuffer dst);
}