package org.bittorrentj.message.exceptions;

import org.bittorrentj.message.field.MessageId;

/**
 * Created by bedeho on 06.09.2014.
 *
 * Exception thrown when in MessageWithLengthAndIdField raw buffer constructor if id field of
 * provided in buffer is inconsistent with expected id which is separately passed to constructor.
 */
public class IncorrectIdFieldInMessageException extends MessageCreationException {

    /**
     * Id which was read from raw message
     */
    private MessageId readId;

    /**
     * Id which was expected and passed to constructor
     */
    private MessageId expectedId;

    public IncorrectIdFieldInMessageException(MessageId readId, MessageId expectedId) {

        this.readId = readId;
        this.expectedId = expectedId;
    }

    public MessageId getReadId() {
        return readId;
    }

    public MessageId getExpectedId() {
        return expectedId;
    }
}
