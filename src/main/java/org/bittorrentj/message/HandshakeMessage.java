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
    public int pstrlen;

    /**
     * Protocol identifier, BitTorrent 1.0 uses "BitTorrent protocol", hence pstrlen=19
     */
    public String pstr;

    /**
     * Reserved bits
     */
    public Reserved reserved;

    /**
     * Info hash of torrent
     */
    public InfoHash info_hash;

    /**
     * BEP20 Peer id of peer
     */
    public PeerId peer_id;

    HandshakeMessage(int pstrlen, String pstr, Reserved reserved, InfoHash info_hash, PeerId peer_id) {

        this.pstrlen = pstrlen;
        this.pstr = pstr;
        this.reserved = reserved;
        this.info_hash = info_hash;
        this.peer_id = peer_id;
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
