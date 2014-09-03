package org.bittorrentj.message.field;

/**
 * BEP 20 Peer ID Convention
 * http://www.bittorrent.org/beps/bep_0020.html
 **/

import org.bittorrentj.BitTorrentj;

/**
 * Created by bedeho on 02.09.2014.
 */
public class PeerId {

    /**
     * Representations of all known clients,
     * with corresponding name and peer_id prefix information
     */
    public enum PeerType {
        BITSWAPR("BitSwapr", "-BR"),
        LIBTORRENT("LibTorrent", "-LT"),
        UTORRENT("uTorrent", "-UT"),
        TRANSMISSON("Transmission", "-TR"),
        AZUREUS("Azureus", "-AZ"),
        UNKNOWN("Unknown", "");

        private final String name;
        private final String peerIdPrefix;

        /**
         * Constructor based of the name and fixed peer_id prefix, that is the
         * portion of the peer id this client which does not vary with version number, and is non random
         * @param name
         * @param peerIdPrefix
         */
        PeerType(String name, String peerIdPrefix) {

            this.name = name;
            this.peerIdPrefix = peerIdPrefix;
        }

        /**
         * Converts raw peer_id to the corresponding PeerType
         * by matching prefix of peer_id against table of prefixes.
         * If no match is found, then PeerType.UKNOWN is returned
         * @param peer_id matched PeerType
         * @return
         */
        static PeerType getPeerType(byte[] peer_id) {

            String peer_id_string = peer_id.toString();

            // Look for match among values
            for(PeerType t : PeerType.values()) {

                // Does prefix match
                int prefix_length = t.peerIdPrefix.length();
                if(peer_id_string.substring(0, prefix_length).equals(t.getPeerIdPrefix()))
                    return t;
            }

            // No match, lets return unknown
            return PeerType.UNKNOWN;
        }

        public String getPeerIdPrefix() {
            return peerIdPrefix;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Byte array for peer_id field
     */
    private byte[] peer_id;

    /**
     * Constructor based of bye array for peer_id
     * @param peer_id byte array for peer_id
     */
    public PeerId(byte[] peer_id) {
        this.peer_id = peer_id;
    }

    /**
     * Constructor based of peer type. Only PeerType.BITSWAPR is
     * fully constructed beyond prefix.
     * @param peer peer type
     */
    public PeerId(PeerType peer) {

        // Allocate space
        this.peer_id = new byte[getLength()];

        // Copy prefix
        String prefix = peer.getPeerIdPrefix();
        System.arraycopy(prefix.getBytes(), 0, peer_id, 0, prefix.length());

        // If it is PeerType.BITSWAPR, then fill in rest
        if(peer == PeerType.BITSWAPR) {

            // Is always less than 20 for sure !!!
            String rest = "" + BitTorrentj.majorVersion + BitTorrentj.minorVersion + BitTorrentj.tinyVersion + "-";
            System.arraycopy(prefix.getBytes(), prefix.length(), rest, 0, rest.length());
        }
    }

    /**
     * Get byte array for peer_id field. Altering array changes
     * @return
     */
    public byte[] getRaw() {
        return peer_id;
    }

    /**
     * Gives the peer type represented by this PeerId object
     * @return
     */
    public PeerType getPeerType() {
        return PeerType.getPeerType(peer_id);
    }

    /**
     * Byte length of a peer_id field
     * @return
     */
    public static int getLength() {
        return 20;
    }
}
