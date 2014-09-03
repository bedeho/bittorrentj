package org.bittorrentj.message.field;

import java.util.Arrays;

/**
 * Created by bedeho on 30.08.2014.
 */
public class InfoHash {

    /**
     * Byte array for info_hash field
     */
    private byte [] info_hash;

    /**
     * Constructor
     * @param info_hash in byte form
     */
    public InfoHash(byte[] info_hash) {

        if(info_hash.length != getLength())
            throw new IllegalArgumentException("Argument had incorrect length, expected " + getLength() + ", received " + info_hash.length);
        else
            this.info_hash = info_hash;
    }

    /**
     * Returns byte array for info_hash field
     * @return byte array
     */
    public byte[] getRaw() {
        return info_hash;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param h object to compare with
     * @return true if this object and f have identical byte arrays backing them
     */
    public boolean equals(InfoHash h) {
        return Arrays.equals(info_hash, h.getRaw());
    }

    /**
     * Byte length of an info hash field
     * @return
     */
    public static int getLength() {
        return 20;
    }
}
