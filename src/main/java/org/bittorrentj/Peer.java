package org.bittorrentj;

/**
 * Created by bedeho on 30.08.2014.
 */
public class Peer {
}
/*
fields
        disk buffer?
        network buffer?
        isConnected
        constructor(socket, peer_id, reserved, torrent)
        save fields
        isConnected = true
        run()
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
        socket.send(buffer): on disconnect do as in run() and return false to Extension!! (extension can use to put peer in ban list or something)
private closeConnection()
        something?
private disableExtension() : called to disable itself, perhaps if it is not longer needed
private enableExtension() called to enable itself, however, how will this ever be called?
*/