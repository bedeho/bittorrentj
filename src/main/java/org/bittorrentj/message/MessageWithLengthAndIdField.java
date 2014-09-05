package org.bittorrentj.message;

/**
 * Created by bedeho on 05.09.2014.
 */

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
     * Id of supported messages, which includes
     * BEP 5 (dht) - PORT
     * BEP 10 (ext) - EXTENDED
     */
    public enum MESSAGE_ID {
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

        byte id;

        MESSAGE_ID(int id) {
            this.id = (byte)id;
        }

        MESSAGE_ID(byte id) {
            this.id = id;
        }

        /**
         * Get id wire representation of id
         * @return
         */
        byte getRaw() {
            return id;
        }
    }

    /**
     * Id of message
     */
    MESSAGE_ID id;

    /**
     * Id of message
     * @return id of message
     */
    public MESSAGE_ID getId() {
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
