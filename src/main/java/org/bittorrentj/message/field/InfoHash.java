package org.bittorrentj.message.field;

import java.util.Arrays;

/**
 * Created by bedeho on 30.08.2014.
 */
public class InfoHash {

    /**
     * Byte length of an info hash
     */
    private final int LENGTH = 20;

    /**
     * Byte array for actual hash
     */
    private byte[] hash;

    InfoHash(byte[] hash) {

        if(hash.length != LENGTH)
            throw new IllegalArgumentException("Byte array had incorrect length, expected " + LENGTH + ", received " + hash.length);
        else
            this.hash = hash;
    }

    public byte[] getHash() {
        return hash;
    }

    boolean equals(InfoHash h) {
        return Arrays.equals(hash, h.getHash());
    }
}
