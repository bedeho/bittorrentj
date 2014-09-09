package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldInMessageException;
import org.bittorrentj.message.exceptions.IncorrectLengthFieldInMessageException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class Choke extends MessageWithLengthAndIdField  {

    /**
     * Constructor based off raw wire representation in buffer
     * @param src buffer
     * @throws IncorrectLengthFieldInMessageException when length field does not match computed message length
     * @throws org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException when id does not match CHOKE message id
     * @throws org.bittorrentj.message.exceptions.NonMatchingIdFieldInMessageException when id field is invalid
     */
    public Choke(ByteBuffer src) throws IncorrectLengthFieldInMessageException, UnrecognizedMessageIdException, NonMatchingIdFieldInMessageException {
        super(MessageId.CHOKE, src); // Read and process length and id fields
    }

    public Choke() {
        super(MessageId.CHOKE);
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) { } // no payload

    @Override
    int getRawPayloadLength() {
        return 0;
    }
}