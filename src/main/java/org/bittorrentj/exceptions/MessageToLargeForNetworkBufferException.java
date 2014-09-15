package org.bittorrentj.exceptions;

/**
 * Created by bedeho on 05.09.2014.
 *
 * Thrown when a message length field is read which
 * is larger then the full network read buffer in the
 * Connection class.
 */
public class MessageToLargeForNetworkBufferException extends Exception {

    /**
     * Size (bytes) of length field value which was to big.
     */
    int size;

    /**
     * Size of buffer which was to small (bytes).
     * It is kept around since different Connections could
     * potentially have different size buffers.
     */
    int bufferSize;

    public MessageToLargeForNetworkBufferException(int size, int bufferSize) {

        this.size = size;
        this.bufferSize = bufferSize;
    }

    @Override
    public String toString() {
        return "Message length field contained a value " + size + "bytes, which is greater than network buffer of size " + bufferSize + ".";
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

}