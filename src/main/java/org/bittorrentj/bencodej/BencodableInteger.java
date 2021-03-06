package org.bittorrentj.bencodej;


import org.bittorrentj.bencodej.exception.EmptyIntegerException;
import org.bittorrentj.bencodej.exception.InvalidDelimiterException;
import org.bittorrentj.bencodej.exception.InvalidIntegerDigits;
import org.bittorrentj.bencodej.exception.NegativeZeroException;

import java.nio.ByteBuffer;

/**
 * Created by bedeho on 10.09.2014.
 */
public class BencodableInteger implements Bencodable {

    /**
     * Integer value
     */
    private int value;

    public BencodableInteger(int value) {
        this.value = value;
    }

    public BencodableInteger(ByteBuffer src) throws InvalidDelimiterException, EmptyIntegerException, NegativeZeroException, InvalidIntegerDigits {

        // Get leading byte
        byte delimiter = src.get();

        // Check that we have correct delimiter
        if(delimiter != 'i')
            throw new InvalidDelimiterException(delimiter);

        // Get possible negative sign
        byte possiblyNegativeSign = src.get();
        boolean isNegativeInteger = false;

        if(possiblyNegativeSign == '-')
            isNegativeInteger = true;
        else
            src.position(src.position() - 1); // rewind buffer position one step, since we read leading digit

        // Find end of integer
        String digits = "";
        byte lastDigit;
        while((lastDigit = src.get()) != 'e')
            digits += (char)lastDigit;

        // Check that we read at least one digit
        if(digits.length() == 0)
            throw new EmptyIntegerException();

        int digitsValue;
        try {
            digitsValue = Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new InvalidIntegerDigits();
        }

        // Check that zero has no negative sign
        if(digitsValue == 0 && isNegativeInteger)
            throw new NegativeZeroException();

        // Convert to integer
        this.value = digitsValue * (isNegativeInteger ? -1 : 1);
    }

    @Override
    public byte [] bencode() {

        // Build bencoding representation of integer
        String bencoding = "i" + this.value + "e";

        // Put in buffer and return
        return bencoding.getBytes();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BencodableInteger)) return false;

        BencodableInteger that = (BencodableInteger) o;

        if (value != that.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
