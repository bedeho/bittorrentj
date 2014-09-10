package org.bittorrentj.message;

/**
 * Created by bedeho on 05.09.2014.
 */

import org.bittorrentj.message.exceptions.ExtendedMessageFoundException;
import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.exceptions.MessageCreationException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

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
     * Constructor based on raw wire representation of message in a buffer, position
     * is left at the end of the message just read.
     * Since this class is abstract, it is only invoked
     * by subclass constructors.
     * @param id message id
     * @param src buffer
     * @throws UnrecognizedMessageIdException when id field in buffer is not recognized
     * @throws org.bittorrentj.message.exceptions.NonMatchingIdFieldException when id parameter does not match message id field in buffer
     */
    protected MessageWithLengthAndIdField(MessageId id, ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException {

        // Read length field
        super(src);

        // Save id field
        this.id = id;

        // Recover raw id field
        byte rawId = src.get();
        MessageId readId = MessageId.getMessageIdFromRaw(rawId);

        // Throw exception if it does not match id provided to constructor
        if(!id.equals(readId))
            throw new NonMatchingIdFieldException(readId, id);
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
     * object based of the supplied buffer. The buffer position
     * is left at the end of message.
     * @param src buffer
     * @return message
     * @throws MessageCreationException when message was malformed in buffer
     * @throws UnrecognizedMessageIdException when message id field in buffer was not recognized
     * @throws ExtendedMessageFoundException when message id field in buffer indicated a BEP20 extension message which is not handshake message
     */
    public static MessageWithLengthAndIdField create(ByteBuffer src) throws MessageCreationException, UnrecognizedMessageIdException, ExtendedMessageFoundException {

        // Read raw id, without advancing position
        byte rawId = src.get(src.position() + LENGTH_FIELD_SIZE);

        // Convert to MessageId to extract message, may
        // case exception if not recognized
        MessageId id = MessageId.getMessageIdFromRaw(rawId);

        // Call upon correct constructor
        switch(id) {
            case CHOKE:
                return new Choke(src);
            case UNCHOKE:
                return new UnChoke(src);
            case INTERESTED:
                return new Interested(src);
            case NOT_INTERESTED:
                return new NotInterested(src);
            case HAVE:
                return new Have(src);
            case BITFIELD:
                return new BitField(src);
            case REQUEST:
                return new Request(src);
            case PIECE:
                return new Piece(src);
            case CANCEL:
                return new Cancel(src);
            case PORT:
                return new Port(src);
            case EXTENDED:

                // Read raw id of extended message, without advancing position
                byte eRawId = src.get(src.position() + LENGTH_FIELD_SIZE + Extended.EXTENDED_ID_FIELD_SIZE);

                // Process the extended handshake message directly,
                // otherwise raise exception so caller can process message.
                if(eRawId == 0)
                    return new ExtendedHandshake(src);
                else
                    throw new ExtendedMessageFoundException();
            default:
                // We cannot come here so long as all enum cases are covered, since
                // getMessageIdFromRaw() throws this exception if
                // the id is not among the known cases.
                throw new UnrecognizedMessageIdException(rawId);
        }
    }

    public MessageId getMessageId(){
        return id;
    }

    @Override
    final public int getRawIdAndPayloadLength() {
        return ID_FIELD_SIZE + getRawPayloadLength();
    }

    @Override
    final protected void writeIdAndPayloadToBuffer(ByteBuffer dst) {

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