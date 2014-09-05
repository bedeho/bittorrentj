package org.bittorrentj.message.field;

import java.util.Arrays;

/**
 * Created by bedeho on 30.08.2014.
 */
public class Hash {

    /**
     * Byte array for 20 byte SHA1 hash field
     */
    private byte [] hash;

    /**
     * Constructor
     * @param hash in byte form
     */
    public Hash(byte[] hash) {

        if(hash.length != getLength())
            throw new IllegalArgumentException("Argument had incorrect length, expected " + getLength() + ", received " + hash.length);
        else
            this.hash = hash;
    }

    /**
     * Returns byte array for hash field,
     * altering array has no side effects on this object.
     * @return byte array
     */
    public byte[] getRaw() {
        return hash.clone();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param h object to compare with
     * @return true if this object and f have identical byte arrays backing them
     */
    public boolean equals(Hash h) {
        return Arrays.equals(hash, h.getRaw());
    }

    /**
     * Byte length of an info hash field
     * @return
     */
    public static int getLength() {
        return 20;
    }
}
