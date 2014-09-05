package org.bittorrentj.message;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 *
 * Class for messages which have at least a length field,
 * and the only direct message subclass is the keep alive,
 * as it is the only message with only a length field.
 */
public abstract class MessageWithLengthField extends Message {

    /**
     * Size of this field in wire message
     */
    public final static int LENGTH_FIELD_SIZE = 4;

    /**
     * Factory method for producing a MessageWithLengthField
     * message based of the supplied byte buffer, whose
     * state (pos, lim, mark) is not altered.
     * @param src
     * @return
     * @throws dddddd
     */
    public static MessageWithLengthField create(ByteBuffer src) {

        /**
        * We confirm that length field is not greater than buffer.
        * When invoked by network read buffer, this is guaranteed not to be the case,
        * but we have to check in general.
        */



        // if length field not have space in buffer, if not, then throw exception

        //

        /**
        * if keep alive message (len==0), if so, just return it and we are good
        * Read length field, and check if its 0,
        * and therefor the keep-alive message. If not,
        * then message must have id field, and so call id message factory instead.
        */
        if(src.getInt(0) == 0)
            return new KeepAlive();
        else
            return MessageWithLengthAndIdField.create(src);// Message must be with id field
    }

    @Override
    abstract public int getRawLength();

    @Override
    abstract protected int writeMessageToBuffer(ByteBuffer dst);

}