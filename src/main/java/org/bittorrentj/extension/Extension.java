package org.bittorrentj.extension;

import org.bittorrentj.swarm.Connection;
import org.bittorrentj.bencodej.BencodeableDictionary;
import org.bittorrentj.message.Extended;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 30.08.2014.
 *
 * Parent class for BEP10 extensions (ut_metedata, ut_pex, br_mpc)
 */
public interface Extension {

    /**
     * Name of extension, as used in BEP20 handshake m dictionary
     * @return name
     */
    public String getName();

    /**
     * Literal description of extension.
     * @return description
     */
    public String getDescription();

    /**
     * Dictionary to add to extended handshake for this message.
     * @return
     */
    public BencodeableDictionary keysToAddToExtendedHandshake();

    /**
     * Process new extended message arriving on given connection
     * @param c Connection where message arrived
     * @param m message
     */
    public void processMessage(Connection c, Extended m);

    /**
     * Parses wire form of extended message for this extension.
     * After successful parsing the position of the buffer will be at the
     * end of the message in the buffer.
     * @param src buffer.
     * @return message parsed.
     */
    public Extended parseMessage(ByteBuffer src);

    /**
     * Is called when (guaranteed only once) an extended handshake message is
     * received over given connection.
     * @param c connection
     */
    public void init(Connection c);

    /**
     * This is a general processing callback which
     * is called to allow the extension to do general
     * processing unrelated to the arrivial of
     * an extension message.
     */
    public void processing();

    /**
     * Indicates whether this extension should be called
     * regularly to do general processing, unrelated
     * to advent of new messages assigned to it. If this
     * method returns false, then the process method will
     * not be called, otherwise it will. Changing
     * return value will change behaviour accordingly
     * during a session.
     * @return true iff processing should be done.
     */
    public boolean needsProcessing();
}