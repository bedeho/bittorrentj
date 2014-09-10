package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingExtendedIdFieldException;
import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public abstract class Extended extends MessageWithLengthAndIdField {

    /**
     * Size of extended id field in wire message
     */
    public final static int EXTENDED_ID_FIELD_SIZE = 1;

    /**
     * Id of extended message, ID = 0 is reserved for
     * handshake message.
     */
    private int extendedMessageId;

    /**
     * Constructor based on raw wire representation.
     * @param extendedMessageId id of extended message
     * @param src buffer
     * @throws UnrecognizedMessageIdException when id field is not recognized
     * @throws org.bittorrentj.message.exceptions.NonMatchingIdFieldException when id does not match EXTENDED message id
     * @throws org.bittorrentj.message.exceptions.NonMatchingExtendedIdFieldException when extended message id does not match the expected extended message id
     */
    public Extended(int extendedMessageId, ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException, NonMatchingExtendedIdFieldException {
        super(MessageId.EXTENDED, src);

        // Save extended message id
        this.extendedMessageId = extendedMessageId;

        // Read extended message id from buffer
        int readExtendedMessageId = src.getInt();

        // Confirm that they are identical
        if(this.extendedMessageId != readExtendedMessageId)
            throw new NonMatchingExtendedIdFieldException(readExtendedMessageId, extendedMessageId);
    }

    public Extended(int extendedMessageId) {
        super(MessageId.EXTENDED);

        this.extendedMessageId = extendedMessageId;
    }

    public int getExtendedMessageId() {
        return extendedMessageId;
    }

    @Override
    final protected void writePayloadToBuffer(ByteBuffer dst) {

        // Write extended message id
        dst.putInt(extendedMessageId);

        // Write rest of extended message.
        writeExtendedMessagePayloadToBuffer(dst);
    }

    /**
     * Writes wire representation of payload in extended message to buffer.
     * @param dst buffer
     */
    abstract protected void writeExtendedMessagePayloadToBuffer(ByteBuffer dst);

    @Override
    public final int getRawPayloadLength() {
        return EXTENDED_ID_FIELD_SIZE + getExtendedMessagePayloadLength();
    }

    /**
     * Byte length of wire representation of payload in extended message.
     * @return
     */
    abstract protected int getExtendedMessagePayloadLength();
}
