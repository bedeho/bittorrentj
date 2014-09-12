package org.bittorrentj.message.exceptions;

/**
 * Created by bedeho on 12.09.2014.
 *
 * Thrown by Piece constructor for buffer when value of block length field is greater than the read buffer of the connection class.
 */
public class BlockLengthToLargeException extends MessageCreationException {
}
