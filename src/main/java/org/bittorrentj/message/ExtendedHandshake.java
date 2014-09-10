package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingExtendedIdFieldInMessageException;
import org.bittorrentj.message.exceptions.NonMatchingIdFieldInMessageException;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Created by bedeho on 09.09.2014.
 */
public class ExtendedHandshake extends Extended {

    /**
     * Size of extended id field in wire message
     */
    public final static int HANDSHAKE_ID = 0;

    /**
     * Dictionary of supported extension messages which maps names of extensions
     * to an extended message ID for each extension message.
     * The only requirement on these IDs is that no extension message share
     * the same one. Setting an extension number to zero means that the extension
     * is not supported/disabled. The client should ignore any
     * extension names it doesn't recognize.
     * The extension message IDs are the IDs used to send the
     * extension messages to the peer sending this handshake.
     * i.e. The IDs are local to this particular peer.
     */
    HashMap<Integer,String> m;

    /**
     * Constructor based on raw wire representation of message.
     * @param src buffer
     * @throws UnrecognizedMessageIdException when id field is not recognized
     * @throws NonMatchingIdFieldInMessageException when id does not match EXTENDED message id
     * @throws NonMatchingExtendedIdFieldInMessageException when extended message id is not HANDSHAKE_ID=0
     */
    public ExtendedHandshake(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldInMessageException, NonMatchingExtendedIdFieldInMessageException {
        super(HANDSHAKE_ID, src);

    }


    public ExtendedHandshake(BENCODE) {
        super(HANDSHAKE_ID);



    }

    @Override
    public void writeExtendedMessagePayloadToBuffer(ByteBuffer dst){

    }

    @Override
    protected int getExtendedMessagePayloadLength() {

    }
}
