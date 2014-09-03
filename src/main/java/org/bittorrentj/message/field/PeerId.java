package org.bittorrentj.message.field;

/**
 * Created by bedeho on 02.09.2014.
 */
public class PeerId {

    /**
     * Table of known
     * https://wiki.theory.org/BitTorrentSpecification#Reserved_Bytes
     **/

    public enum PeerType {
        BitSwapr()
        Uknown()
    }

    /**
     *
     */
    private byte[] peer_id;

    /**
     *
     */
    private PeerType peer;

    /**
     *
     * @param peer_id
     */
    public PeerId(byte[] peer_id) {

    }

    /**
     *
     * @param peer
     */
    public PeerId(PeerType peer) {
        this.peer = peer;



    }

    /**
     * Get byte array for peer_id field. Altering array changes
     * @return
     */
    public byte[] getRaw() {
        return peer_id;
    }

    /**
     * Byte length of a peer_id field
     * @return
     */
    public static int getLength() {
        return 20;
    }

}
