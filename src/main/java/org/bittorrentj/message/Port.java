package org.bittorrentj.message;

import org.bittorrentj.message.exceptions.NonMatchingIdFieldException;
import org.bittorrentj.message.field.MessageId;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 05.09.2014.
 */
public class Port extends MessageWithLengthAndIdField {

    /**
     * Port for DHT node of this client
     */
    private int port;

    /**
     * Constructor
     * @param port local listening port.
     */
    public Port(int port) {
        super(MessageId.PORT);

        this.port = port;

        if(port < 0)
            throw new IllegalArgumentException();
    }

    /**
     * Constructor based on wire representation of message.
     * @param src buffer
     * @throws UnrecognizedMessageIdException if id is not recognized.
     * @throws NonMatchingIdFieldException when id in buffer does not match PORT message id.
     */
    public Port(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException {

        // Read and process length and id fields
        super(MessageId.PORT, src);

        // Read piece index
        this.port = src.getInt();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    protected void writePayloadToBuffer(ByteBuffer dst) {
        dst.putInt(port);
    }

    @Override
    int getRawPayloadLength() {
        return 4;
    }
}