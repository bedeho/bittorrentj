package org.bittorrentj.event;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.io.IOException;

/**
 * Created by bedeho on 30.08.2014.
 */
public class StartServerErrorEvent implements Event {

    /**
     * Exception associated with server error
     */
    private IOException exception;

    /**
     *
     */
    private InetSocketAddress address;

    public StartServerErrorEvent(IOException exception, InetSocketAddress address) {

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