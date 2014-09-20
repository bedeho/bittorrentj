package org.bittorrentj;

import java.util.LinkedList;

import org.bittorrentj.event.Event;
import org.bittorrentj.extension.Extension;
import org.bittorrentj.message.field.Hash;
import org.bittorrentj.torrent.MetaInfo;

/**
 * Created by bedeho on 30.08.2014.
 */
public class BitTorrentj {

    /**
     * Version information for this edition of BitTorrentj
     */
    public final static int majorVersion = 0;
    public final static int minorVersion = 0;
    public final static int tinyVersion = 0;

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
    LinkedList<Extension> extensions;

    /**
     * How do we interpret this again??????
     */
    boolean useDHT;

    /**
     * Thread managing client
     */
    private Client client;

    /**
     * Queue containing events which client thread has recently generated. It is
     * directly queried by add/getEvent methods, called by client thread and API code respectively
     */
    private LinkedList<Event> eventQueue;

    /**
     * BitTorrentj constructor
     * @param port
     * @param maxNumberOfConnections
     * @param extensions
     * @param useDHT
     */

    BitTorrentj(int port, int maxNumberOfConnections, LinkedList<Extension> extensions, boolean useDHT) {

        this.port = port;
        this.maxNumberOfConnections = maxNumberOfConnections;
        this.extensions = extensions;
        this.useDHT = useDHT;

        // Setup
        this.eventQueue = new LinkedList<Event>();
        this.client = new Client(this);

        // Start client
        client.start();
    }

    /**
     * Starts the server listening on the given port
     */
    public void beginClient() {
        //client.registerCommand(new BeginCommand())
    }

    public void haltClient() {
        //client.registerCommand(new HaltCommand())
    }

    public void addTorrent(String magnetLink) {
        //client.registerCommand(new AddTorrentCommand())
    }

    public void addTorrent(MetaInfo info) {
        //client.registerCommand(new AddTorrentCommand())
    }

    public void removeTorrent(Hash h) {
        //client.registerCommand(new RemoveTorrentCommand())
    }

    public void getTorrentState(Hash h) {
        //client.registerCommand(new GetTorrentSwarmStateCommand())


        // get session stats:  TorrentStatistics
        // change/view settings
        // pause, stop start
        // list,add,remove peers
        // etc
    }

    public void alterTorrentSettings(Hash h) {

    }

    /**
     * Adds an event to the event queue
     * Is called by the client thread (ClientThread) for various reasons (see where for exhaustive list)
     * @param e event object
     */
    public void registerEvent(Event e) {

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

    int getPort() {
        return port;
    }

    int getMaxNumberOfConnections() {
        return maxNumberOfConnections;
    }

    // what if this list is messed with??
    /*
    LinkedList<Extension> getExtensions() {
        return extensions;
    }
    */

    boolean isUseDHT() {
        return useDHT;
    }
}


