package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.IncorrectLengthFieldInMessageException;
import org.bittorrentj.message.exceptions.NonMatchingExtendedIdFieldException;
import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;
import org.bittorrentj.util.extensionRegistration;

import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by bedeho on 09.09.2014.
 */
public class ExtendedHandshake extends Extended {

    /**
     * Extended id field of handshake message
     */
    public final static int HANDSHAKE_ID = 0;


    private Bencod dictionary;



    /**
     * Constructor based on raw wire representation of message.
     * @param src buffer
     * @throws UnrecognizedMessageIdException If id field is not recognized
     * @throws NonMatchingIdFieldException If id does not match EXTENDED message id
     * @throws NonMatchingExtendedIdFieldException If extended message id is not HANDSHAKE_ID=0
     */
    public ExtendedHandshake(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException, NonMatchingExtendedIdFieldException {
        super(HANDSHAKE_ID, src);

        // Get payload length
        int extendedHandshakePayloadLength = getMessageLengthField() - (LENGTH_FIELD_SIZE + ID_FIELD_SIZE + ID_FIELD_SIZE);

        if(extendedHandshakePayloadLength < 0)
            throw new IncorrectLengthFieldInMessageException()

        // Allocate space
        byte [] extendedHandshakePayload = new byte[extendedHandshakePayloadLength];

        // Read from buffer
        src.get(extendedHandshakePayload);

        // Parse bencoded dictionary




        // parse bencoding
        // get registrations
        // save bencoding

    }

    /**
     *
     * @param m
     */
    public ExtendedHandshake(LinkedList<extensionRegistration> m) {
        super(HANDSHAKE_ID);

        // Convert to bencoding

        // save bencoding

    }

    public ExtendedHandshake(Bencode) {

    }

    public bencode getDictionary() {

    }

    public LinkedList<extensionRegistration> getRegistrations() {

    }

    @Override
    public void writeExtendedMessagePayloadToBuffer(ByteBuffer dst){

    }

    @Override
    protected int getExtendedMessagePayloadLength() {

    }
}
