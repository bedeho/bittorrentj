package org.bittorrentj.message.exceptions;

import org.bittorrentj.message.field.MessageId;

/**
 * Created by bedeho on 06.09.2014.
 *
 * Exception thrown when in Extended raw buffer constructor if extended id field of
 * provided in buffer is inconsistent with expected extended id which is separately passed to constructor.
 */
public class NonMatchingExtendedIdFieldException extends MessageCreationException {

    /**
     * Id which was read from raw message
     */
    private int readId;

    /**
     * Id which was expected and passed to constructor
     */
    private int expectedId;

    public NonMatchingExtendedIdFieldException(int readId, int expectedId) {

        this.readId = readId;
        this.expectedId = expectedId;
    }

    public int getReadId() {
        return readId;
    }

    public int getExpectedId() {
        return expectedId;
    }
}
