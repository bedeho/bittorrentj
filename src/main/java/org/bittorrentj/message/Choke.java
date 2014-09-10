package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class Choke extends MessageWithLengthAndIdField  {

    /**
     * Constructor based off raw wire representation
     * @param src buffer
     * @throws UnrecognizedMessageIdException when id does not match CHOKE message id
     * @throws org.bittorrentj.message.exceptions.NonMatchingIdFieldException when id field is invalid
     */
    public Choke(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException {
        super(MessageId.CHOKE, src);
    }

    public Choke() {
        super(MessageId.CHOKE);
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) { } // no payload

    @Override
    public int getRawPayloadLength() {
        return 0;
    }
}