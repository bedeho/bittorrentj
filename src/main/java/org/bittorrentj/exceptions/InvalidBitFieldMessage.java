package org.bittorrentj.exceptions;

import org.bittorrentj.message.BitField;

/**
 * Created by bedeho on 15.09.2014.
 */
public class InvalidBitFieldMessage extends Exception {

    /**
     * Invalid message
     */
    private BitField invalidMessage;

    public InvalidBitFieldMessage(BitField invalidMessage) {
        this.invalidMessage = invalidMessage;
    }
}
