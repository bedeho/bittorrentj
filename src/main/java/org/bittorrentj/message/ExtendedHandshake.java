package org.bittorrentj.message;

import java.util.HashMap;

/**
 * Created by bedeho on 09.09.2014.
 */
public class ExtendedHandshake extends Extended {

    /**
     * Dictionary of supported extension messages which maps names of extensions
     * to an extended message ID for each extension message.
     * The only requirement on these IDs is that no extension message share
     * the same one. Setting an extension number to zero means that the extension
     * is not supported/disabled. The client should ignore any
     * extension names it doesn't recognize.
     * The extension message IDs are the IDs used to send the
     * extension messages to the peer sending this handshake.
     * i.e. The IDs are local to this particular peer.
     */
    HashMap<Integer,String> m;


}
