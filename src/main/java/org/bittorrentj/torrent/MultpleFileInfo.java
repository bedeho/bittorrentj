package org.bittorrentj.torrent;

/**
 * Created by bedeho on 03.09.2014.
 */

import org.bittorrentj.message.field.Hash;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * For the case of the multi-file mode,
 * the info dictionary contains the following structure:
 */
public class MultpleFileInfo extends Info {

    /**
     *  The file path of the directory in
     *  which to store all the files. This is purely advisory. (string)
     */
    private String name;

    /**
     * Information about each file in
     */
    private ArrayList<SingleFileAmongManyInfo> files;

    public class SingleFileAmongManyInfo {

        /**
         * Length of the file in bytes (integer)
         */
        public int length;

        /**
         *  (optional) A 32-character hexadecimal string corresponding to the MD5 sum of the file. This is not used by
         *  BitTorrent at all, but it is included by some programs for greater compatibility.
         */
        public String md5;

        /**
         * A list containing one or more string elements that together represent
         * the path and filename. Each element in the list corresponds to either
         * a directory name or (in the case of the final element) the filename.
         * For example, a the file "dir1/dir2/file.ext" would consist of three
         * string elements: "dir1", "dir2", and "file.ext".
         * This is encoded as a bencoded list of strings
         * such as l4:dir14:dir28:file.exte
         */
        public String path;

        /**
         * Constructor
         * @param length
         * @param md5
         * @param path
         */
        public SingleFileAmongManyInfo(int length, String md5, String path) {

            this.length = length;
            this.md5 = md5;
            this.path = path;
        }
    }

    /**
     * Constructor
     * @param pieceLength bytes per each piece
     * @param pieces list of piece hashes
     * @param private_ privateness of torrent
     * @param name
     * @param files
     */
    public MultpleFileInfo(int pieceLength, LinkedList<Hash> pieces, boolean private_, String name, ArrayList<SingleFileAmongManyInfo> files) {

        super(pieceLength, pieces, private_);

        this.name = name;
        this.files = files;
    }

    @Override
    public Info computeInfoHash() {
            // Generate info hash
            return null;
    }
}
