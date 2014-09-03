package org.bittorrentj.message;

import org.bittorrentj.message.field.InfoHash;
import org.bittorrentj.message.field.PeerId;
import org.bittorrentj.message.field.Reserved;

/**
 * Created by bedeho on 01.09.2014.
 */
public class HandshakeMessage {

    /**
     * Length of pstr field
     */
    private int pstrlen;

    /**
     * Protocol identifier, BitTorrent 1.0 uses "BitTorrent protocol", hence pstrlen=19
     */
    private String pstr;

    /**
     * Reserved bits
     */
    private Reserved reserved;

    /**
     * Info hash of torrent
     */
    private InfoHash info_hash;

    /**
     * BEP20 Peer id of peer
     */
    private PeerId peer_id;

    /**
     * Constructor based on field objects. It is possible to pass null values for some or all fields,
     * but toRaw() will then return exception.
     * @param pstrlen
     * @param pstr
     * @param reserved
     * @param info_hash
     * @param peer_id
     * @throws IllegalArgumentException when pstrlen and pstr are not compatible
     */
    public HandshakeMessage(int pstrlen, String pstr, Reserved reserved, InfoHash info_hash, PeerId peer_id) throws IllegalArgumentException {

        this.pstrlen = pstrlen;
        this.pstr = pstr;
        this.reserved = reserved;
        this.info_hash = info_hash;
        this.peer_id = peer_id;

        // Make sure pstr and pstrelen agree
        if(this.pstr != null && (this.pstr.length() != this.pstrlen))
            throw new IllegalArgumentException("pstrlen and pstr are not compatible");
    }

    /**
     *
     * @return
     * @throws IllegalStateException when all fields are not set
     */
    private byte [] toRaw() throws IllegalStateException {

        // If all fields are not set, which can be the case due to how handshake has a pause step,
        // then buffer form is not available
        if(pstrlen == 0 || pstr == null || reserved == null || info_hash == null || peer_id == null)
            throw new IllegalStateException("All fields are not set");

        // Size of new package
        int total_size = 1 + pstrlen + Reserved.getLength() + InfoHash.getLength() + PeerId.getLength();

        // Allocate space
        byte [] raw = new byte[total_size];

        // Fill
        int to = 0;





        // Return
        return raw;
    }

    public int getPstrlen() {
        return pstrlen;
    }

    public void setPstrlen(int pstrlen) {
        this.pstrlen = pstrlen;
    }

    public String getPstr() {
        return pstr;
    }

    public void setPstr(String pstr) {
        this.pstr = pstr;
    }

    public Reserved getReserved() {
        return reserved;
    }

    public void setReserved(Reserved reserved) {
        this.reserved = reserved;
    }

    public InfoHash getInfo_hash() {
        return info_hash;
    }

    public void setInfo_hash(InfoHash info_hash) {
        this.info_hash = info_hash;
    }

    public PeerId getPeer_id() {
        return peer_id;
    }

    public void setPeer_id(PeerId peer_id) {
        this.peer_id = peer_id;
    }
}
