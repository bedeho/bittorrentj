package org.bittorrentj;

import java.util.ArrayList;

import org.bittorrentj.btjevent.BTjEventHandler;
import org.bittorrentj.torrent.Metainfo;
import org.bittorrentj.torrent.TorrentInterface;

import org.bittorrentj.util.InfoHash;

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
     * Event handler for BTjEvents
     */
    BTjEventHandler handler;

    /**
     * How do we interpret this again
     */
    boolean useDHT;

    private ClientThread client;

    //
    //ConcurrentLinkedQueue<> clientCommandQueue;

    BitTorrentj(int port, int maxNumberOfConnections, ArrayList<Extension> extensions, BTjEventHandler handler, boolean useDHT) {

        this.port = port;
        this.maxNumberOfConnections = maxNumberOfConnections;
        this.extensions = extensions;
        this.useDHT = useDHT;

        // Setup
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

    public BTjEventHandler getHandler() {
        return handler;
    }

    public boolean isUseDHT() {
        return useDHT;
    }
}


