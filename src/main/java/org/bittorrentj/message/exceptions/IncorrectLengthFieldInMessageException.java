package org.bittorrentj.message.exceptions;

/**
 * Created by bedeho on 06.09.2014.
 *
 * Exception thrown when in MessageWithLengthField raw buffer constructor if length field of
 * provided in buffer is inconsistent with length computed by getRawMessageLength().
 */
public class IncorrectLengthFieldInMessageException extends MessageCreationException {

    /**
     * Length field from message
     */
    int messageFieldLength;

    /**
     * Length computed by getRawMessageLength()
     */
    int computedMessageLength;

    /**
     * Constructor
     * @param messageFieldLength
     * @param computedMessageLength
     */
    public IncorrectLengthFieldInMessageException(int messageFieldLength, int computedMessageLength) {

        this.messageFieldLength = messageFieldLength;
        this.computedMessageLength = computedMessageLength;
    }

    public int getComputedMessageLength() {
        return computedMessageLength;
    }

    public int getMessageFieldLength() {
        return messageFieldLength;
    }
}
