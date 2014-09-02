package org.bittorrentj.message.field;

/**
 * Created by bedeho on 02.09.2014.
 */
public class PeerId {

    /**
     * Table of known
     * https://wiki.theory.org/BitTorrentSpecification#Reserved_Bytes
     **/

    public enum id {

    }

    /**
     * Byte length of an info hash
     */
    private final int LENGTH = 20;

    /**
     *
     */
    private byte[] peer_id;

    /**
     *
     * @param peer_id
     */
    PeerId(byte[] peer_id) {

    }


}
