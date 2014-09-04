package org.bittorrentj.event;

import org.bittorrentj.message.field.Hash;

/**
 * Created by bedeho on 30.08.2014.
 */
public abstract class TorrentEvent extends Event {

    /**
     * Info_hash for torrent which event is associated with.
     */
    Hash hash;

    TorrentEvent(Hash hash) {
        this.hash = hash;
    }
}
