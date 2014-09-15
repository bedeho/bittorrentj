package org.bittorrentj;

/**
 * Created by bedeho on 15.09.2014.
 */

import org.bittorrentj.message.ExtendedHandshake;
import org.bittorrentj.message.Handshake;

/**
 * Class capturing state of connection
 */
public class PeerState {

    /**
     * Is in choking state
     */
    private boolean choking;

    /**
     * Is in choking interested
     */
    private boolean interested;

    /**
     * Most recent knowledge of piece availability.
     */
    private boolean [] pieceAvailability;

    /**
     * Handshake transmitted by peer during connection setup.
     */
    private Handshake handshake;

    /**
     * Most recent extended handshake (BEP10) transmitted by peer.
     */
    private ExtendedHandshake extendedHandshake;

    /**
     * Constructor
     * @param choking
     * @param interested
     * @param pieceAvailability
     */
    PeerState(boolean choking, boolean interested, boolean [] pieceAvailability, Handshake handshake, ExtendedHandshake extendedHandshake) {
        this.choking = choking;
        this.interested = interested;
        this.pieceAvailability = pieceAvailability;

        this.handshake = handshake;
        this.extendedHandshake = extendedHandshake;
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

    public void setPieceAvailability(boolean[] pieceAvailability) {
        this.pieceAvailability = pieceAvailability;
    }

    public boolean isPieceAvailable(int index) {

        if(pieceAvailability == null)
            throw new IllegalStateException();
        else
            return pieceAvailability[index];
    }

    public void alterPieceAvailability(int index, boolean availability) {

        if(pieceAvailability == null)
            throw new IllegalStateException();
        else
            pieceAvailability[index] = availability;
    }
}
