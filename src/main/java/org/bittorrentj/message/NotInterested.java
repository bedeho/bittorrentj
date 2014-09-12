package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class NotInterested extends MessageWithLengthAndIdField {

    /**
     * Constructor based on wire representation of message.
     * @param src
     * @throws UnrecognizedMessageIdException when id does not match NOT_INTERESTED message id.
     * @throws org.bittorrentj.message.exceptions.NonMatchingIdFieldException when id field is invalid.
     */
    public NotInterested(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException {
        super(MessageId.NOT_INTERESTED, src); // Read and process length and id fields
    }

    /**
     * Constructor
     */
    public NotInterested() {
        super(MessageId.NOT_INTERESTED);
    }

    @Override
    public void writePayloadToBuffer(ByteBuffer dst) { }

    @Override
    int getRawPayloadLength() {
        return 0;
    }
}
