package org.bittorrentj.message;

import org.bittorrentj.bencodej.*;
import org.bittorrentj.bencodej.exception.DecodingBencodingException;
import org.bittorrentj.message.exceptions.*;
import org.bittorrentj.message.field.exceptions.UnrecognizedMessageIdException;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by bedeho on 09.09.2014.
 */
public class ExtendedHandshake extends Extended {

    /**
     * Extended id field of handshake message
     */
    public final static int HANDSHAKE_ID = 0;

    /**
     * Dictionary in extended handshake message
     */
    private BencodeableDictionary payload;

    /**
     * Constructor based on raw wire representation of message.
     * @param src buffer
     * @throws UnrecognizedMessageIdException If id field is not recognized.
     * @throws NonMatchingIdFieldException If id does not match EXTENDED message id.
     * @throws NonMatchingExtendedIdFieldException If extended message id is not HANDSHAKE_ID=0.
     * @throws DuplicateExtensionNameInMDictionaryException If an extension name was present more than once in the handshake m dictionary.
     * @throws PayloadDoesNotContainMDictionaryException If the m dictionary was not present in the handshake.
     * @throws MalformedMDictionaryException If the m dictionary did not have the expected structure.
     * @throws DecodingBencodingException If the payload did not have a welformed bencoding.
     * @throws ExtendedHandshakePayloadLengthToShortException If the length field was shorter than the minimum length of 6 = 4 + 1 + 1 = head length field (4b) + extension message id (1b) + handdshake id (1b).
     */
    public ExtendedHandshake(ByteBuffer src) throws UnrecognizedMessageIdException, NonMatchingIdFieldException, NonMatchingExtendedIdFieldException, DuplicateExtensionNameInMDictionaryException, PayloadDoesNotContainMDictionaryException, MalformedMDictionaryException, DecodingBencodingException, ExtendedHandshakePayloadLengthToShortException {
        super(HANDSHAKE_ID, src);

        // Get payload length
        int extendedHandshakePayloadLength = getMessageLengthField() - (LENGTH_FIELD_SIZE + ID_FIELD_SIZE + ID_FIELD_SIZE);

        if(extendedHandshakePayloadLength < 0)
            throw new ExtendedHandshakePayloadLengthToShortException();

        // Allocate space
        byte [] extendedHandshakePayload = new byte[extendedHandshakePayloadLength];

        // Read from buffer
        src.get(extendedHandshakePayload);

        // Parse bencoded dictionary
        Bencodable o = Bencodej.decode(src);

        if(!(o instanceof BencodeableDictionary))
            throw new MalformedMDictionaryException();
        else
            this.payload = (BencodeableDictionary) o;

        // Validate
        validateMDictionary(payload);
    }

    /**
     * Constructor based on wire representation of extended handshake.
     * @param payload dictionary sent with extended handshake message
     * @throws DuplicateExtensionNameInMDictionaryException If an extension name was present more than once in the handshake m dictionary.
     * @throws PayloadDoesNotContainMDictionaryException If the m dictionary was not present in the handshake.
     * @throws MalformedMDictionaryException If the m dictionary did not have the expected structure.
     */
    public ExtendedHandshake(BencodeableDictionary payload) throws DuplicateExtensionNameInMDictionaryException, PayloadDoesNotContainMDictionaryException, MalformedMDictionaryException {
        super(HANDSHAKE_ID);

        this.payload = payload;

        // Verify the payload
        validateMDictionary(payload);
    }

    /**
     * Verify payload dictionary, and return registrations for extensions in message.
     * @param payload dictionary sent with extended handshake message.
     * @throws DuplicateExtensionNameInMDictionaryException If an extension name was present more than once in the handshake m dictionary.
     * @throws PayloadDoesNotContainMDictionaryException If the m dictionary was not present in the handshake.
     * @throws MalformedMDictionaryException If the m dictionary did not have the expected structure.
     */
    private static void validateMDictionary(BencodeableDictionary payload) throws DuplicateExtensionNameInMDictionaryException, MalformedMDictionaryException, PayloadDoesNotContainMDictionaryException {

        // Check that it has m kay
        if(!payload.containsKey("m"))
            throw new PayloadDoesNotContainMDictionaryException();

        // Get extension dictionary
        BencodeableDictionary extensionsDictionary;

        try {
            extensionsDictionary = (BencodeableDictionary) payload.get("m");
        } catch(Exception e) {
            throw new MalformedMDictionaryException(); // bencodable string key
        }

        // Iterate m keys
        LinkedList<Integer> observedExtensionIds = new LinkedList<Integer>();
        LinkedList<String> observedExtensionNames = new LinkedList<String>();

        for(BencodableByteString s: extensionsDictionary.keySet()) {

            // Confirm that the same extension is not registered multiple times
            String name = s.getByteString().toString();
            if(observedExtensionNames.contains(name))
                throw new DuplicateExtensionNameInMDictionaryException();
            else
                observedExtensionNames.add(name);

            try {
                int id = ((BencodableInteger) extensionsDictionary.get(s)).getValue();

                if(id != 0 && observedExtensionIds.contains(id))
                    throw new DuplicateIdInMDictionaryException();
                else
                    observedExtensionIds.add(id);

            } catch (Exception e) {
                throw new MalformedMDictionaryException(); // bencodablestring key did not correspond to bencodable integer
            }
        }
    }

    /**
     * Get maping of extension id to extension names for all enabled
     * extensions in message, that is all extensions with a non-zero id.
     * @return hash map from extension ids (int) to names (string)
     * @throws DuplicateExtensionNameInMDictionaryException If an extension name was present more than once in the handshake m dictionary.
     * @throws PayloadDoesNotContainMDictionaryException If the m dictionary was not present in the handshake.
     * @throws MalformedMDictionaryException If the m dictionary did not have the expected structure.
     */
    public HashMap<Integer, String> getEnabledExtensions() throws DuplicateExtensionNameInMDictionaryException, PayloadDoesNotContainMDictionaryException, MalformedMDictionaryException {

        // Check that all is well
        validateMDictionary(payload);

        // Allocate list
        HashMap<Integer, String> map = new HashMap<Integer, String>();

        // Get m dictionary
        BencodeableDictionary extensionsDictionary = (BencodeableDictionary) payload.get("m");

        // Iterate m keys
        for(BencodableByteString s: extensionsDictionary.keySet()) {

            // Get extension id
            int id = ((BencodableInteger) extensionsDictionary.get(s)).getValue();

            // If id is zero, then extension is disabled
            if(id != 0)
                map.put(id, s.getByteString().toString());
        }

        return map;
    }

    /**
     * Get list of extension names which are disabled,
     * that is all extensions with a zero id.
     * @return linked list of extension names
     * @throws DuplicateExtensionNameInMDictionaryException If an extension name was present more than once in the handshake m dictionary.
     * @throws PayloadDoesNotContainMDictionaryException If the m dictionary was not present in the handshake.
     * @throws MalformedMDictionaryException If the m dictionary did not have the expected structure.
     */
    public LinkedList<String> getDisabledExtensions() throws DuplicateExtensionNameInMDictionaryException, PayloadDoesNotContainMDictionaryException, MalformedMDictionaryException {

        // Check that all is well
        validateMDictionary(payload);

        // Allocate list
        LinkedList<String> list = new LinkedList<String>();

        // Get m dictionary
        BencodeableDictionary extensionsDictionary = (BencodeableDictionary) payload.get("m");

        // Iterate m keys
        for(BencodableByteString s: extensionsDictionary.keySet()) {

            // Get extension id
            int id = ((BencodableInteger) extensionsDictionary.get(s)).getValue();

            // If id is zero, then extension is disabled
            if(id == 0)
                list.add(s.getByteString().toString());
        }

        return list;
    }

    public BencodeableDictionary getPayload() {
        return payload;
    }

    @Override
    public void writeExtendedMessagePayloadToBuffer(ByteBuffer dst){

        // Write bencoding of payload to buffer
        dst.put(payload.bencode());
    }

    @Override
    protected int getExtendedMessagePayloadLength() {
        return payload.bencode().length;
    }
}
