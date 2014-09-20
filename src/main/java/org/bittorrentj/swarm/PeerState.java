package org.bittorrentj.swarm;

/**
 * Created by bedeho on 15.09.2014.
 */

import org.bittorrentj.message.ExtendedHandshake;
import org.bittorrentj.message.Handshake;
import org.bittorrentj.message.Request;

import java.util.LinkedList;

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
     * Request messages sent by this peer which have not been satisfied.
     */
    private LinkedList<Request> unsatisfiedRequests;

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

    public void setExtendedHandshake(ExtendedHandshake extendedHandshake) {
        this.extendedHandshake = extendedHandshake;
    }

    public ExtendedHandshake getExtendedHandshake() {
        return extendedHandshake;
    }

    /**
     * Returns array representing piece availability.
     * Altering array will not influence availability
     * in this object.
     * @return
     */
    public boolean[] getPieceAvailability() {
        return pieceAvailability.clone();
    }

    public void setPieceAvailability(boolean[] pieceAvailability) {
        this.pieceAvailability = pieceAvailability;
    }

    public boolean isPieceAvailabilityKnown() {
        return pieceAvailability != null;
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

    /**
     * Checks whether BEP10 extension protocol is used by this peer.
     * @return
     */
    public boolean extensionProtocolIsUsed() {
        return handshake.getReserved().getExtensionProtocolIsUsed();
    }

    public void registerRequest(Request m) { unsatisfiedRequests.add(m);}

    public boolean unregisterRequest(Request m) { return unsatisfiedRequests.remove(m); }
}
