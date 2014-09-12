package org.bittorrentj.message.exceptions;

/**
 * Created by bedeho on 06.09.2014.
 *
 * Exception thrown in .. when
 */
public class LengthFieldDifferentFromMessageLengthException extends MessageCreationException {

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
    public LengthFieldDifferentFromMessageLengthException(int messageFieldLength, int computedMessageLength) {

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
