package org.bittorrentj.message.field;

/**
 * Table of known uses for reserved bits
 * https://wiki.theory.org/BitTorrentSpecification#Reserved_Bytes
 **/

/**
 * Created by bedeho on 02.09.2014.
 */
public class Reserved {

    /**
     * Byte array for info_hash field
     */
    private byte [] reserved;

    /**
     * Constructor based on byte array
     * @param reserved field
     */
    public Reserved(byte[] reserved) {

        if(reserved.length != getLength())
            throw new IllegalArgumentException("Argument had incorrect length, expected " + getLength() + ", received " + reserved.length);
        else
            this.reserved = reserved;
    }

    /**
     * Constructor based on flags
     * @param dhtIsUsed BEP 5 DHT protocol flag
     * @param extensionProtocolIsUsed BEP 6 Fast Extension protocol flag
     */
    public Reserved(boolean dhtIsUsed, boolean extensionProtocolIsUsed){

        // Allocate space for field
        reserved = new byte[getLength()];

        // DHT uses very last bit, set it if needed
        if(dhtIsUsed)
            reserved[8] |= (byte)0x80;

        // Extension protocol uses bit 20 from the right, set it if needed
        if(extensionProtocolIsUsed)
            reserved[5] |= (byte)0x10;
    }

    /**
     * Recovers DHT flag from reserved field
     * @return true iff DHT flag is set
     */
    public boolean getDhtIsUsed() {
        return (reserved[8] & (byte)0x80) == 1;
    }

    /**
     * Recovers Extension protocol flag from reserved field
     * @return true iff Extension protocol flag is set
     */
    public boolean getExtensionProtocolIsUsed() {
        return (reserved[5] & (byte)0x10) == 1;
    }

    /**
     * Get byte array for reserved field,
     * altering array has no side effect on this object
     * @return
     */
    public byte[] getRaw() {
        return reserved.clone();
    }

    /**
     * Byte length of an info hash field
     * @return 8
     */
    public static int getLength() {
        return 8;
    }
}
