package org.bittorrentj.swarm.connection;

import java.util.Date;

/**
 * Created by bedeho on 21.09.2014.
 *
 * Manages the transmission rate and historical information for Input and Output message
 * streams, one such manager is assigned per stream, and it works with both
 * types of streams.
 */
public class MessageStreamManager {

    /**
     * The size of the window used to measure network io (up/down) speed
     * over, in milliseconds.
     */
    private final static int SPEED_MEASUREMENT_WINDOW_SIZE = 1000; // (ms)

    /**
     * Time when manager when first started.
     */
    private Date startTime;

    /**
     * The total number of bytes over full lifetime of the
     * manager.
     */
    private int totalAmountOfTransmittedData;

    /**
     * Counts the amount of data transmitted between data source
     * and message stream buffer read into the network read buffer
     * within present averaging window.
     */
    private int transmittedDataInPresentWindow;

    /**
     * Date and time when data was last transmitted.
     */
    private Date timeLastDataTransmitted;

    /**
     * Date and time at which present measuring window starts.
     */
    private Date currentWindowStart;

    /**
     * The rate (byte/ms) at which data is allowed
     * to be transmitted. The manager will attempt to get the rate
     * as close as possible, but not above.
     */
    private int desiredTransmissionRate;

    /**
     * Constructor
     * @param desiredTransmissionRate
     */
    MessageStreamManager(int desiredTransmissionRate) {

        this.startTime = new Date();
        this.totalAmountOfTransmittedData = 0;
        this.timeLastDataTransmitted = null;
        this.desiredTransmissionRate = desiredTransmissionRate;

        startNewWindowIfNecessary();
    }

    /**
     * 
     * @return
     */
    public int maximumTransmittableDataAtThisTime() {

    }

    /**
     *
     * @param numberOfBytes
     */
    public void transmittedData(int numberOfBytes) {

        // Start a new measuring window if necessary
        startNewWindowIfNecessary();

        // Count towards counter for present window
        transmittedDataInPresentWindow += numberOfBytes;

        // and net total transmission rate
        totalAmountOfTransmittedData += numberOfBytes;

        // and update the date of last transmitted data
        timeLastDataTransmitted = new Date();
    }

    /**
     *
     * @return
     */
    private Date startNewWindowIfNecessary() {

        Date now = new Date();

        if(currentWindowStart == null || now.getTime() - currentWindowStart.getTime() > SPEED_MEASUREMENT_WINDOW_SIZE) {
            this.transmittedDataInPresentWindow = 0;
            this.currentWindowStart = now;
        }

        return currentWindowStart;
    }

    public int getTotalAmountOfTransmittedData() {
        return totalAmountOfTransmittedData;
    }

    public int getDesiredTransmissionRate() {
        return desiredTransmissionRate;
    }

    public Date getTimeLastDataTransmitted() {
        return timeLastDataTransmitted;
    }

    public Date getStartTime() {
        return startTime;
    }
}
