package org.bittorrentj.message.exceptions;

/**
 * Created by bedeho on 07.09.2014.
 *
 * Exception thrown...
 */
public class InvalidMessageLengthFieldException extends MessageCreationException {

    /**
     * The invalid  length
     */
    private int messageLengthField;

    public InvalidMessageLengthFieldException(int messageLengthField) {
        this.messageLengthField = messageLengthField;
    }

    public int getMessageLengthField() {
        return messageLengthField;
    }
}