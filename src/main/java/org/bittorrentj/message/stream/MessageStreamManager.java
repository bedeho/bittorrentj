package org.bittorrentj.message.stream;

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
    public final static int SPEED_MEASUREMENT_WINDOW_SIZE = 1000; // (ms)

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
     * The rate (b/ms) at which data is allowed
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
        this.transmittedDataInPresentWindow = 0;
        this.currentWindowStart = startTime;
    }

    /**
     * How much may be transmitted at present time
     * @return
     */
    //public int maximumTransmittableDataAtThisTime() {
    public int maximumTransmittableDataAtThisTime() {

        // Start a new measuring window if necessary
        startNewWindowIfNecessary();

        // Has the maximum amount been transmitted this window
        int maximumAmountPerWindow = desiredTransmissionRate * SPEED_MEASUREMENT_WINDOW_SIZE;

        // BOOLEAN: yes/no
        // if so then return true iff we have not used up quota
        //return transmittedDataInPresentWindow < maximumAmountPerWindow;

        // INTEGER CAPPED: assumes it is called at least once per window
        // if so then return true iff we have not used up quota
        int remaining = transmittedDataInPresentWindow - maximumAmountPerWindow;
        return remaining > 0 ? remaining : 0;
    }

    /**
     * Registers that data has been transmitted,
     * is called from streams.
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
     * Resets the starting time of the present window, and resets counter,
     * both only if current time has exceeded measuring window.
     * @return present starting date for window
     */
    private Date startNewWindowIfNecessary() {

        long now = new Date().getTime();

        // Time since start of the last measuring window
        long timeSinceLastWindowStart = now - currentWindowStart.getTime();

        // Has more than one interval passed, if so we must reset window start time and counter
        if(timeSinceLastWindowStart > SPEED_MEASUREMENT_WINDOW_SIZE) {

            // Get overflow of passed time
            long overflow = timeSinceLastWindowStart % SPEED_MEASUREMENT_WINDOW_SIZE;

            // Set time to be -overflow in the past
            this.currentWindowStart.setTime(now - overflow);

            // Reset transmission counter
            this.transmittedDataInPresentWindow = 0;
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
