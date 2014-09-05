package org.bittorrentj.message;

/**
 * Created by bedeho on 05.09.2014.
 */

import org.bittorrentj.message.field.MessageId;

import java.nio.ByteBuffer;

/**
 *
 * Messages of this type take following form on wire: <length prefix><message ID><payload>.
 */
public abstract class MessageWithLengthAndIdField extends MessageWithLengthField {

    /**
     * Size of this field in wire message
     */
    public final static int ID_FIELD_SIZE = 1;

    /**
     * Id of message
     */
    MessageId id;

    /**
     * Id of message
     * @return id of message
     */
    public MessageId getId() {
        return id;
    }

    /**
     * Factory method for producing a MessageWithLengthField
     * object based of the supplied byte buffer. Supplied buffer
     * state (pos, lim, mark) is not altered.
     * @param src
     * @return
     * @throws dddddd
     */
    public static MessageWithLengthAndIdField create(ByteBuffer src){

        /**
         * We confirm that length field is not greater than buffer.
         * When invoked by MessageWithLengthField.create(), this is guaranteed not to be the case,
         * but we have to check in general.
         */

        // is the id recognized? if yes, then call constructor for that object, if not,
        // then throw exception

    }

    @Override
    abstract public int getRawLength();

    @Override
    abstract protected int writeMessageToBuffer(ByteBuffer dst);
}
