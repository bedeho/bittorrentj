package org.bittorrentj.torrent;

/**
 * Created by bedeho on 03.09.2014.
 */

import java.util.LinkedList;

/**
 * For the case of the single-file mode,
 * the info dictionary contains the following structure:
 */
public class SingleFileInfo extends Info {

    /**
     * The filename. This is purely advisory. (string)
     */
    private String name;

    /**
     * Length of the file in bytes (integer)
     */
    private int length;

    /**
     * (optional) A 32-character hexadecimal string corresponding
     * to the MD5 sum of the file. This is not used by BitTorrent
     * at all, but it is included by some programs for greater compatibility.
     */
    private String md5sum;

    /**
     * Constuctor
     * @param pieceLength
     * @param pieces
     * @param private_
     * @param name filename (optional)
     * @param length length of file
     * @param md5sum MD5 sum of file (optional)
     */
    public SingleFileInfo(int pieceLength, LinkedList<String> pieces, int private_, String name, int length, String md5sum) {

        super(pieceLength, pieces, private_);

        this.name = name;
        this.length = length;
        this.md5sum = md5sum;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
