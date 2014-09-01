package org.bittorrentj.event;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by bedeho on 30.08.2014.
 */
public class StartServerErrorEvent implements Event {

    /**
     * Exception associated with server error
     */
    private IOException exception;

    /**
     * Local address for server
     */
    private InetAddress address;

    public StartServerErrorEvent(IOException exception, InetAddress address) {

        this.exception = exception;
        this.address = address;
    }

    /**
     *
     * @return
     */
    @Override
    public String message() {
        return "Starting server failed."; // add more later, e.g. language support
    }
}