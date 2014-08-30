package org.bittorrentj;

import java.util.ArrayList;
import java.util.LinkedList;

import org.bittorrentj.event.Event;
import org.bittorrentj.command.Command;
import org.bittorrentj.torrent.Metainfo;
import org.bittorrentj.torrent.TorrentInterface;

/**
 * Created by bedeho on 30.08.2014.
 */
public class BitTorrentj {

    /**
     * Port that will be used to listen for incoming connections
     */
    int port;

    /**
     * Greatest number of simultaneous connections allowed, regardless of who initiates them (incoming/outgoing)
     */
    int maxNumberOfConnections;

    /**
     * Extension managers for BEP10 extensions
     */
    ArrayList<Extension> extensions;

    /**
     * How do we interpret this again??????
     */
    boolean useDHT;

    /**
     * Thread managing client
     */
    private ClientThread client;

    /**
     * Queue containing events which client thread has recently generated. It is
     * directly queried by add/getEvent methods, called by client thread and API code respectively
     */
    private LinkedList<Event> eventQueue;

    /**
     * Queue com
     */
    private LinkedList<Command> commandQueue;

    /**
     * BitTorrentj constructor
     * @param port
     * @param maxNumberOfConnections
     * @param extensions
     * @param useDHT
     */

    BitTorrentj(int port, int maxNumberOfConnections, ArrayList<Extension> extensions, boolean useDHT) {

        this.port = port;
        this.maxNumberOfConnections = maxNumberOfConnections;
        this.extensions = extensions;
        this.useDHT = useDHT;

        // Setup
        this.eventQueue = new LinkedList<Event>();
        this.client = new ClientThread(this);

        // Start client
        client.start();
    }

    /**
     * Starts the server listening on the given port
     */
    public void begin() {
        // send a message for the thread to start again?
    }

    public void halt() {
        // send a message for the thread to stop
        // clear event queue?
    }

    public void addTorrent(String magnetLink) {

    }

    public void addTorrent(Metainfo info) {

    }

    /*
    some way of adding torrent which is partially downloaded, also support fast resume!!
    public void addTorrent(String directory) {

    }
    */

    public TorrentInterface getTorrent(InfoHash h) {

        // get session stats:  TorrentStatistics
        // change/view settings
        // pause, stop start
        // list,add,remove peers
        // etc

        return null;
    }

    /**
     * Adds an event to the event queue
     * Is called by the client thread (ClientThread) for various reasons (see where for exhaustive list)
     * @param e event object
     */
    void addEvent(Event e) {

        synchronized (eventQueue) {

            // Add event to queue
            eventQueue.add(e);

            // Notify any potentially waiting thread in getEvent()
            eventQueue.notify();
        }
    }

    /**
     * Retrieves any pending {@link org.bittorrentj.event.Event} object in event queue.
     * @param blocking
     * @return
     */
    public Event getEvent(boolean blocking) {

        synchronized (eventQueue) {

            while(eventQueue.isEmpty()) {

                try {

                    if (blocking)
                        eventQueue.wait();
                    else
                        return null;
                } catch (Exception e) {
                    // We were woken up for some reason
                }
            }

            // Retrieve and remove head of event queue
            return eventQueue.poll();
        }
    }

    public void removeTorrent(InfoHash h) {

    }

    public int getPort() {
        return port;
    }

    public int getMaxNumberOfConnections() {
        return maxNumberOfConnections;
    }

    public ArrayList<Extension> getExtensions() {
        return extensions;
    }

    public boolean isUseDHT() {
        return useDHT;
    }
}


