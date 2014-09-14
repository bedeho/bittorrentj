package org.bittorrentj.exceptions;

/**
 * Created by bedeho on 13.09.2014.
 */
public class InvalidMessageReceivedException extends Exception {

    /**
     * The actual exception
     */
    private Exception actualException;

    public InvalidMessageReceivedException(Exception e) {
        this.actualException = e;
    }


}
