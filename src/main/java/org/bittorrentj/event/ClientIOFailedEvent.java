package org.bittorrentj.event;

import java.io.IOException;

/**
 * Created by bedeho on 13.09.2014.
 */
public class ClientIOFailedEvent extends Event {

    /**
     * Input/Output exception in question.
     */
    private IOException e;

    public ClientIOFailedEvent(IOException e) {
        this.e = e;
    }

    public IOException getE() {
        return e;
    }
}
