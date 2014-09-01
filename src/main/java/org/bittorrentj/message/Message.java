package org.bittorrentj.message;

/**
 * Created by bedeho on 30.08.2014.
 */
public abstract class Message {


    // All of the remaining messages in the protocol take the form of <length prefix><message ID><payload>. The length prefi
    // x is a four byte big-endian value. The message ID is a single decimal byte. The payload is message dependent.
}
