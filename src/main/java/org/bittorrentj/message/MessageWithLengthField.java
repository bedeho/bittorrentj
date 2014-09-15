package org.bittorrentj.message;

import org.bittorrentj.bencodej.exception.DecodingBencodingException;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.exceptions.*;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;
import java.util.HashMap;

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
     * The value of the length field in the message header.
     * This variable is only set in the buffer based constructor in this
     * class, based on the length field, so that corresponding
     * constructors in extending classes can verify it against the
     * full message length read from buffer.
     */
    private int messageLengthField;

    /**
     * Indicates whether the buffer constructors was used
     */
    private boolean bufferConstructorUsed;

    /**
     * Constructor based on binary buffer, position
     * is left at the end of the message just read.
     * Since this class is abstract, it is only invoked
     * by subclass constructors.
     * @param src buffer containing raw wire form of message
     */
    protected MessageWithLengthField(ByteBuffer src) {

        // Recover length field in buffer
        this.messageLengthField = src.getInt();

        // Save that buffer constructor was used,
        // this allows getMessageLengthField() to be called without the
        // IllegalStateException exceptions
        this.bufferConstructorUsed = true;
    }

    /**
     * Default constructor, is needed by constructor of keep alive
     * (direct subclass) since it requires no arguments.
     */
    protected MessageWithLengthField() {

        // Save that buffer constructor was not used,
        // this causes getMessageLengthField() to throw the IllegalStateException exceptions
        this.bufferConstructorUsed = false;
    }

    /**
     * Factory method for producing a MessageWithLengthField
     * message based of the supplied byte buffer, whose
     * position is advanced to end of message read.
     * @param src buffer read from
     * @return created message
     * @throws MessageCreationException
     * @throws UnrecognizedMessageIdException
     */
    public static MessageWithLengthField create(ByteBuffer src, HashMap<Integer, Extension> activeExtensions) throws
            MessageCreationException,
            UnrecognizedMessageIdException,
            UnsupportedExtendedMessageFoundException,
            DecodingBencodingException
            {

        // Read length field
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
            return MessageWithLengthAndIdField.create(src, activeExtensions);
        }
    }

    /**
     * The length field of the byte buffer
     * parsed if the buffer constructor is used.
     * <b>If that constructor was not used, then this
     * value will be zero, hence it does not represent the
     * length of the raw message under all circumstances.
     * Use getRawMessageLength() for that purpose.</b>
     * The purpose of this message is to expose the
     * parsed length field value for constructors of extending messages.
     * @return byte length
     */
    public int getMessageLengthField() {

        if(!bufferConstructorUsed)
            throw new IllegalStateException();
        else
            return messageLengthField;
    }

    @Override
    final public int getRawMessageLength() {
        return LENGTH_FIELD_SIZE + getRawIdAndPayloadLength();
    }

    @Override
    final public void writeMessageToBuffer(ByteBuffer dst) {

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