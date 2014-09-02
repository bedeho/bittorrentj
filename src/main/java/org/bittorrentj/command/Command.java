package org.bittorrentj.command;

/**
 * Created by bedeho on 30.08.2014.
 */

/**
 * Represents commands issued by external code. Objects of this class are added to commandQueue.
 */
public abstract class Command {

    // add handle so that callee can easily recover to which command a given command was issued, sincethis is included on the
    // revent
}