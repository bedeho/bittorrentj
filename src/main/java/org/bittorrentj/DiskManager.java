package org.bittorrentj;

import org.bittorrentj.torrent.Info;
import org.bittorrentj.torrent.SingleFileInfo;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Created by bedeho on 14.09.2014.
 */
public class DiskManager extends Thread {

    class Piece {

    }

    private HashMap<Integer, Piece> piecePool;

    public DiskManager(Info info) {

        piecePool = new HashMap<Integer, Piece>();

        if(info instanceof SingleFileInfo) {

        } else {

        }

    }

    synchronized public void writePiece(ByteBuffer src, int index) {

    }

    /**
     *
     * @param dst
     * @param index
     * @param connection
     */
    synchronized public void readPiece(ByteBuffer dst, int index, Connection connection) {

    }
}
