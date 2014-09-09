package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldInMessageException;
import org.bittorrentj.message.exceptions.IncorrectLengthFieldInMessageException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class NotInterested extends MessageWithLengthAndIdField {

    /**
     * Constructor based off raw wire representation in buffer
     * @param src
     * @throws IncorrectLengthFieldInMessageException when length field does not match computed message length
     * @throws org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException when id does not match NOT_INTERESTED message id
     * @throws org.bittorrentj.message.exceptions.NonMatchingIdFieldInMessageException when id field is invalid
     */
    public NotInterested(ByteBuffer src) throws IncorrectLengthFieldInMessageException, UnrecognizedMessageIdException, NonMatchingIdFieldInMessageException {
        super(MessageId.NOT_INTERESTED, src); // Read and process length and id fields
    }

    /**
     * Constructor
     */
    public NotInterested() {
        super(MessageId.NOT_INTERESTED);
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) { } // no payload

    @Override
    int getRawPayloadLength() {
        return 0;
    }
}
