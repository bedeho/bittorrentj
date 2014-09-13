package org.bittorrentj.exceptions;

/**
 * Created by bedeho on 13.09.2014.
 */
public class InvalidMessageRecievedException extends Exception {

    /**
     * The actual exception
     */
    private Exception actualException;

    public InvalidMessageRecievedException(Exception e) {
        this.actualException = e;
    }


}
