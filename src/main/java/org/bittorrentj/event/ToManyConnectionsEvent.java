package org.bittorrentj.event;

import java.net.InetSocketAddress;

/**
 * Created by bedeho on 31.08.2014.
 */
public class ToManyConnectionsEvent implements Event {

    /**
     *
     * @return
     */
    @Override
    public String message() {
        return "Rejec."; // add more later, e.g. language support
    }
}
