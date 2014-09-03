package org.bittorrentj;

/**
 * Created by bedeho on 30.08.2014.
 */
public class Extension {
}

/*
abstract class Extension : super class for BEP10 extensions (ut_metedata,ut_pex,br_mpc)
        fields
static String name : is used in BEP10 handshake
static HashMap<String, Bencoded>: fields added flat into the BEP10 handshake
        constructor( callbackObject?)

        getExtentionName()
        returns name of this extension
        getFlatKeys(TorrentSwarm)
        returns flat keys for this torrent
        addTorrent(TorrentSwarm)
        begin()
        halt()
        processMessage(peer, ByteBuffer) : does this need peer, or really just socket and TorrentSwarm: perhaps it needs to know what it is selling or buying from individual peers? would be important/cool to know
        Note!!!:
        for an extension to actually modify its peer/torrent, it must wait for lock on it!!!
*/
