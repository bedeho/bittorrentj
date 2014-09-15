package org.bittorrentj.torrent;

import org.bittorrentj.message.field.Hash;

import java.util.Date;
import java.util.LinkedList;

/**
 * Created by bedeho on 30.08.2014.
 */
public class MetaInfo {

    /**
     * Describes the file(s) of the torrent.
     * There are two possible forms: one for the case of a 'single-file'
     * torrent with no directory structure, and one for the case of
     * a 'multi-file' torrent (see below for details)
     */
    private Info info;

    /**
     * The announce URL of the tracker (string)
     */
    private String announce;

    /**
     * (optional) This is an extention to the official specification
     * offering backwards-compatibility. (list of lists of strings).
     */
    private LinkedList<String> announce_list;

    /**
     * (optional) The creation time of the torrent, in standard
     * UNIX epoch format (integer, seconds since 1-Jan-1970 00:00:00 UTC)
     */
    private Date creation_date;

    /**
     * (optional) Free-form textual comments of the author (string)
     */
    private String comment;

    /**
     * (optional) Name and version of the program used to create the .torrent (string)
     */
    private String created_by;

    /**
     * (optional) the string encoding format used to generate the pieces
     * part of the info dictionary in the .torrent metafile (string)
     */
    private String encoding;

    /**
     * Constructor loading metainfo from .torrent file
     * @param filename name of .torrent file
     */
    public MetaInfo(String filename) {

    }

    /**
     * Constructor
     * @param info
     * @param announce
     * @param announce_list
     * @param creation_date
     * @param comment
     * @param created_by
     * @param encoding
     */
    public MetaInfo(Info info, String announce, LinkedList<String> announce_list, Date creation_date, String comment, String created_by, String encoding) {

        this.info = info;
        this.announce = announce;
        this.announce_list = announce_list;
        this.creation_date = creation_date;
        this.comment = comment;
        this.created_by = created_by;
        this.encoding = encoding;

        // what short of coherency check is needed

    }

    /**
     * Number of pieces in the torrent file for this meta information.
     * @return number of pieces
     */
    public int getNumberOfPiecesInTorrent() {
        return info.getPieces().size();
    }
}
