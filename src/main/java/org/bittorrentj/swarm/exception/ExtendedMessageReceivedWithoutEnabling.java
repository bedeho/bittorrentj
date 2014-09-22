package org.bittorrentj.swarm.exception;

/**
 * Created by bedeho on 22.09.2014.
 *
 * Thrown in message processing routine of Connection class
 * in response to a BEP10 message received, despite not
 * being enabled in handshake stage.s
 */
public class ExtendedMessageReceivedWithoutEnabling extends Exception {
}
