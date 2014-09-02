package org.bittorrentj.message;

import org.bittorrentj.InfoHash;

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
     *
     */
    public Reserved reserved;

    /**
     *
     */
    public InfoHash info_hash;

    /**
     *
     */
    public PeerId peer_id;

}
