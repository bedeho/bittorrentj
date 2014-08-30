package org.bittorrentj.event;

import org.bittorrentj.InfoHash;

/**
 * Created by bedeho on 30.08.2014.
 */
public abstract class TorrentEvent extends Event {

    /**
     * Info_hash for torrent which event is associated with.
     */
    InfoHash hash;

    TorrentEvent(InfoHash hash) {
        this.hash = hash;
    }
}
