package org.bittorrentj.message.field;

import org.bittorrentj.BitTorrentj;

/**
 * Created by bedeho on 02.09.2014.
 *
 * Identifies known bittorrent clients,
 * and representation is based off BEP 20 Peer ID Convention
 * http://www.bittorrent.org/beps/bep_0020.html
 */
public enum PeerId {
    BITSWAPR("BitSwapr", "-BR"),
    LIBTORRENT("LibTorrent", "-LT"),
    UTORRENT("uTorrent", "-UT"),
    TRANSMISSON("Transmission", "-TR"),
    AZUREUS("Azureus", "-AZ"),
    UNKNOWN("Unknown", "");

    private final String name;
    private final String peerIdPrefix;

    /**
     * Constructor based of the name and fixed type prefix, that is the
     * portion of the peer id this client which does not
     * vary with version number, and is non random.
     * @param name
     * @param peerIdPrefix
     */
    PeerId(String name, String peerIdPrefix) {

        this.name = name;
        this.peerIdPrefix = peerIdPrefix;
    }

    /**
     * Converts raw type to the corresponding PeerType
     * by matching prefix of type against table of prefixes.
     * If no match is found, then PeerType.UNKNOWN is returned.
     * @param peer_id matched PeerType
     * @return
     */
    static PeerId getPeerType(byte[] peer_id) {

        String peer_id_string = peer_id.toString();

        // Look for match among values
        for(PeerId t : PeerId.values()) {

            // Does prefix match
            int prefix_length = t.peerIdPrefix.length();
            if(peer_id_string.substring(0, prefix_length).equals(t.getPeerIdPrefix()))
                return t;
        }

        // No match, lets return unknown
        return PeerId.UNKNOWN;
    }

    /**
     * Get raw wire representation of peer id. Only PeerId.BITSWAPR is
     * fully constructed beyond prefix. Altering array has
     * no side effect on this object
     * @return
     */
    public byte[] getRaw() {

        // Allocate space
        byte [] raw = new byte[getLength()];

        // Copy prefix
        String prefix = getPeerIdPrefix();
        System.arraycopy(prefix.getBytes(), 0, raw, 0, prefix.length());

        // If it is BITSWAPR, then fill in rest
        if(name.equals("BitSwapr")) {

            // Is always less than 20 for sure !!!
            String rest = "" + BitTorrentj.majorVersion + BitTorrentj.minorVersion + BitTorrentj.tinyVersion + "-";
            System.arraycopy(prefix.getBytes(), prefix.length(), rest, 0, rest.length());
        }

        return raw;
    }

    public String getPeerIdPrefix() {
        return peerIdPrefix;
    }

    public String getName() {
        return name;
    }

    /**
     * Byte length of a type field
     * @return
     */
    public static int getLength() {
        return 20;
    }
}
