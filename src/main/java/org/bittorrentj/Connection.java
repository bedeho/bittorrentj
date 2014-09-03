package org.bittorrentj;

/**
 * Created by bedeho on 30.08.2014.
 */
public class Connection {

    /**
     * State of client side of connection
     */
    private PeerState clientState;

    /**
     * State of peer side of connection
     */
    private PeerState peerState;

    /**
     * Class capturing state of connection
     */
    private class PeerState {

        /**
         * Is in choking state
         */
        private boolean choking;

        /**
         * Is in choking interested
         */
        private boolean interested;

        /**
         * Constructor
         * @param choking
         * @param interested
         */
        PeerState(boolean choking, boolean interested) {
            this.choking = choking;
            this.interested = interested;
        }

        public boolean isChoking() {
            return choking;
        }

        public void setChoking(boolean choking) {
            this.choking = choking;
        }

        public boolean isInterested() {
            return interested;
        }

        public void setInterested(boolean interested) {
            this.interested = interested;
        }
    }

    /*
    public enum State {
        BEFORE_HANDSHAKE(1),
    }

    public enum bufferState {

    }
    */
}

/*
fields
        disk buffer?
        network buffer?
        isConnected
        constructor(socket, peer_id, reserved, torrent)
        save fields
        isConnected = true
        TorrentSwarm()
        if peer supports BEP10, send handshake with torrent.createExtensionHandshakeDictionary()
        while(1) :
        classic message
        torrent.processMessage(this, msg)
        BEP10
        handshake (note: this may be refreshed)
        update reverse lookup table
        extended messages
        extract extended message ID x
        map x to local Extension i
        call torrent.Extension[i].processMessage(this,ByteBuffer)
        on disconnect exception
        isConnected = false
        call torrent.peerDisConnected(this)
        WHAT DOES THREAD DO, SLEEP ?
private methods for extensions to call
private sendExtendedMessage(ByteBuffer/ExtendedMessage) : allows extensions to send messages to peer
        socket.send(buffer): on disconnect do as in TorrentSwarm() and return false to Extension!! (extension can use to put peer in ban list or something)
private closeConnection()
        something?
private disableExtension() : called to disable itself, perhaps if it is not longer needed
private enableExtension() called to enable itself, however, how will this ever be called?
*/