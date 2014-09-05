package org.bittorrentj.message.field;

import org.bittorrentj.message.field.exceptions.InvalidMessageIdException;

/**
 * Created by bedeho on 05.09.2014.
 *
 * Id of supported messages, which includes
 * BEP 5 (dht) - PORT
 * BEP 10 (ext) - EXTENDED
 */
public enum MessageId {
    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    NOT_INTERESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7),
    CANCEL(8),
    PORT(9),
    EXTENDED(20);

    int id;

    MessageId(int id) {
        this.id = id;
    }

    /**
     * Get wire representation of message id
     * @return
     */
    byte getRaw() {
        return (byte)id;
    }

    /**
     * Converts raw wire representation of message rawId to
     * corresponding MessageId object.
     * @param rawId raw wire representation of rawId
     * @return the MessageId
     * @throws InvalidMessageIdException if rawId is not recognized
     */
    public static MessageId getMessageIdFromRaw(byte rawId) throws InvalidMessageIdException {

        // Look for match among values
        for(MessageId i : MessageId.values()) {

            if(i.getRaw() == rawId)
                return i;
        }

        throw new InvalidMessageIdException(rawId);
    }
}
