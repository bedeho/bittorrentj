package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class Interested extends MessageWithLengthAndIdField {

    /**
     * Constructor based on wire representation of message.
     * @param src buffer
     * @throws UnrecognizedMessageIdException when id does not match INTERESTED message id.
     * @throws NonMatchingIdFieldException when id field is invalid.
     */
    public Interested(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException {
        super(MessageId.INTERESTED, src); // Read and process length and id fields
    }

    public Interested() {
        super(MessageId.INTERESTED);
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) { }

    @Override
    public int getRawPayloadLength() {
        return 0;
    }
}
