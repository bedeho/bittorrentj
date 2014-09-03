package org.bittorrentj.message.field;

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
        this.reserved = new byte[getLength()];

        // Set positions

        dhtIsUsed
                extensionProtocolIsUsed
    }


    /**
     * Recovers DHT flag from reserved field
     * @return true iff DHT flag is set
     */
    public boolean getDhtIsUsed() {
        return reserved[] & ;
    }

    /**
     * Recovers Extension protocol flag from reserved field
     * @return true iff Extension protocol flag is set
     */
    public boolean getExtensionProtocolIsUsed() {
        return reserved[] & ;
    }

    /**
     * Get byte array for reserved field. Altering array changes
     * @return
     */
    public byte[] getRaw() {
        return reserved;
    }

    /**
     * Byte length of an info hash field
     * @return 8
     */
    public static int getLength() {
        return 8;
    }
}
