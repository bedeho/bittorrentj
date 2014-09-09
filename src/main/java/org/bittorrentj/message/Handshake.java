package org.bittorrentj.message;

import org.bittorrentj.message.field.Hash;
import org.bittorrentj.message.field.PeerId;
import org.bittorrentj.message.field.Reserved;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 01.09.2014.
 */
public class Handshake extends Message {

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
    private Hash info_hash;

    /**
     * BEP20 Peer id of peer
     */
    private PeerId peer_id;

    /**
     * Constructor based on field objects. It is possible to pass null values for some or all fields,
     * but writeRawMessage() will then return exception.
     * @param pstrlen
     * @param pstr
     * @param reserved
     * @param info_hash
     * @param peer_id
     * @throws IllegalArgumentException when pstrlen and pstr are not compatible
     */
    public Handshake(int pstrlen, String pstr, Reserved reserved, Hash info_hash, PeerId peer_id) throws IllegalArgumentException {

        this.pstrlen = pstrlen;
        this.pstr = pstr;
        this.reserved = reserved;
        this.info_hash = info_hash;
        this.peer_id = peer_id;

        // Make sure pstr and pstrlen agree
        if(this.pstr != null && (this.pstr.length() != this.pstrlen))
            throw new IllegalArgumentException("pstrlen and pstr are not compatible");
    }

    /**
     * Gives the raw byte array form of the message,
     * altering has no effect on this object.
     * @return byte array of message
     * @throws IllegalStateException when all fields are not set, or pstrlen and pstr are not compatible
     */
    public void writeMessageToBuffer(ByteBuffer dst) {

        // Make sure message fields are set
        checkStateOrThrowException();

        // pstrlen
        dst.put(pstr.getBytes());

        // reserved
        dst.put(reserved.getRaw());

        // info_hash
        dst.put(info_hash.getRaw());

        // peer_id
        dst.put(peer_id.getRaw());
    }

    @Override
    public int getRawMessageLength() {

        // Make sure message fields are set
        checkStateOrThrowException();

        return 1 + pstrlen + Reserved.getLength() + Hash.getLength() + PeerId.getLength();
    }

    /**
     * Checks that all fields required for computing
     * raw message and length.
     * @throws IllegalStateException
     */
    private void checkStateOrThrowException() throws IllegalStateException {

        // If all fields are not set, which can be the case due to how handshake has a pause step,
        // then buffer form is not available
        if(pstrlen == 0 || pstr == null || reserved == null || info_hash == null || peer_id == null)
            throw new IllegalStateException("All fields are not set");
        else if(this.pstr.length() != this.pstrlen)
            throw new IllegalStateException("pstrlen and pstr are not compatible");
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

    public Hash getInfo_hash() {
        return info_hash;
    }

    public void setInfo_hash(Hash info_hash) {
        this.info_hash = info_hash;
    }

    public PeerId getPeer_id() {
        return peer_id;
    }

    public void setPeer_id(PeerId peer_id) {
        this.peer_id = peer_id;
    }
}
