package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class UnChoke extends MessageWithLengthAndIdField {

    /**
     * Constructor based on wire representation of message.
     * @param src buffer
     * @throws UnrecognizedMessageIdException when id does not match UNCHOKE message id
     * @throws NonMatchingIdFieldException when id field is invalid
     */
    public UnChoke(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException {
        super(MessageId.UNCHOKE, src); // Read and process length and id fields
    }

    /**
     * Constructor
     */
    public UnChoke() {
        super(MessageId.UNCHOKE);
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) { }

    @Override
    int getRawPayloadLength() {
        return 0;
    }
}
