package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.IncorrectIdFieldInMessageException;
import org.bittorrentj.message.exceptions.IncorrectLengthFieldInMessageException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.InvalidMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class UnChoke extends MessageWithLengthAndIdField {

    /**
     * Constructor based off raw wire representation in buffer
     * @param src buffer
     * @throws IncorrectLengthFieldInMessageException when length field does not match computed message length
     * @throws InvalidMessageIdException when id does not match UNCHOKE message id
     * @throws IncorrectIdFieldInMessageException when id field is invalid
     */
    public UnChoke(ByteBuffer src) throws IncorrectLengthFieldInMessageException, InvalidMessageIdException, IncorrectIdFieldInMessageException {
        // Read and process length and id fields
        super(MessageId.UNCHOKE, src);
    }

    /**
     * Constructor
     */
    public UnChoke() {
        super(MessageId.UNCHOKE);
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) { } // no payload

    @Override
    int getRawPayloadLength() {
        return 0;
    }
}
