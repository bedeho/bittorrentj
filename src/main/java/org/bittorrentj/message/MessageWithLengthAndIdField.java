package org.bittorrentj.message;

/**
 * Created by bedeho on 05.09.2014.
 */

import org.bittorrentj.message.exceptions.BufferToSmallForMessageException;
import org.bittorrentj.message.exceptions.IncorrectIdFieldInMessageException;
import org.bittorrentj.message.exceptions.IncorrectLengthFieldInMessageException;
import org.bittorrentj.message.exceptions.MessageCreationException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.InvalidMessageIdException;

import java.nio.ByteBuffer;

/**
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
     * Constructor based on binary buffer, position
     * is left at the end of the message just read.
     * Since this class is abstract, it is only invoked
     * by subclass constructors.
     * @param id message id
     * @param src source buffer containing raw wire form of message
     * @throws IncorrectLengthFieldInMessageException
     * @throws InvalidMessageIdException
     * @throws IncorrectIdFieldInMessageException
     */
    protected MessageWithLengthAndIdField(MessageId id, ByteBuffer src) throws IncorrectLengthFieldInMessageException, InvalidMessageIdException, IncorrectIdFieldInMessageException {

        // Read and process length field
        super(src);

        // Save id field
        this.id = id;

        // Recover raw id field
        byte rawId = src.get();
        MessageId readId = MessageId.getMessageIdFromRaw(rawId);

        // Throw exception if it does not match id provided to constructor
        if(!id.equals(readId))
            throw new IncorrectIdFieldInMessageException(readId, id);
    }

    /**
     * Constructor used by subclasses
     * @param id
     */
    protected MessageWithLengthAndIdField(MessageId id) {
        this.id = id;
    }

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
    public static MessageWithLengthAndIdField create(ByteBuffer src) throws MessageCreationException, InvalidMessageIdException {

        // Minimum size of messages of this class
        int minSize = LENGTH_FIELD_SIZE + ID_FIELD_SIZE;

        /**
         * Confirm that message has space for smallest possible message of this class.
         * When invoked by MessageWithLengthField.create(), this is guaranteed to be the case,
         * but we have to check in general.
         */

        if(src.remaining() < minSize)
            throw new BufferToSmallForMessageException(minSize, src);

        // Read raw id, without advancing position
        byte rawId = src.get(src.position() + LENGTH_FIELD_SIZE);

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
    public int getRawIdAndPayloadLength() {
        return ID_FIELD_SIZE + getRawPayloadLength();
    }

    @Override
    protected void writeIdAndPayloadToBuffer(ByteBuffer dst) {

        // Write id field
        dst.put(id.getRaw());

        // Write any potential payload field
        writePayloadToBuffer(dst);
    }

    /**
     * Write wire representation of payload field (if it exist) into buffer, while
     * trusting it has sufficient space. Position
     * of buffer is advanced to end of message written.
     * This is a helper routine, protected and thus not callable
     * outside class, which is used by the public writeRawMessage().
     * @param dst
     * @return number of bytes written to buffer
     */
    abstract void writePayloadToBuffer(ByteBuffer dst);

    /**
     * Byte length of wire representation of
     * payload field (if it exist).
     * @return byte length
     */
    abstract int getRawPayloadLength();
}