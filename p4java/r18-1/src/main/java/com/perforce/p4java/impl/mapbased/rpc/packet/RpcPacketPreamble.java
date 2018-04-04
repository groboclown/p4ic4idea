/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.ProtocolError;

/**
 * The five byte preamble appended to every text packet payload.<p>
 * 
 * The format (as divined from the C++ API) is designed to provide
 * a very simple sanity check checksum and encode the length in bytes
 * of the accompanying RPC payload:
 * <pre>
 *      byte[1] = ( payload_length / 0x1 ) % 0x100;
 *      byte[2] = ( payload_length / 0x100 ) % 0x100;
 *      byte[3] = ( payload_length / 0x10000 ) % 0x100;
 *      byte[4] = ( payload_length / 0x1000000 ) % 0x100;
 *      byte[0] = byte[1] ^ byte[2] ^ byte[3] ^ byte[4];
 * </pre>
 * This can generally only be calculated after the other packet elements
 * have been serialized, which is irritating but not too annoying.
 * 
 *
 */

public class RpcPacketPreamble {
	
	/**
	 * Size in bytes of the preamble checksum. This is a very fundamental
	 * value; changing it will probably cause total destruction within
	 * P4Java...
	 */
	public static final int RPC_PREAMBLE_CHKSUM_SIZE = 1;
	
	/**
	 * The size in bytes of the standard text packet RPC packet preamble.
	 */
	public static final int RPC_PREAMBLE_SIZE = RpcPacket.RPC_LENGTH_FIELD_LENGTH
													+ RPC_PREAMBLE_CHKSUM_SIZE;
	
	private byte[] preLengthBytes = null;
	private byte[] preChksumBytes = null;
	
	/**
	 * Calculate and construct a suitable preamble for the passed-in
	 * payload buffer. Does not affect the incoming buffer at all (i.e.
	 * marks and limits, etc. are unaffected). Assumes the payload starts at
	 * buffer byte position zero and uses the buffer's limit as the length.
	 * 
	 * @param payload non-null byte buffer representing the payload
	 * @return new RpcPacketPreamble for the payload
	 */
	
	public static RpcPacketPreamble constructPreamble(ByteBuffer payload) {
		return new RpcPacketPreamble(payload.position());
	}
	
	/**
	 * Calculate and construct a suitable preamble for the passed-in
	 * payload buffer length.
	 */
	
	public static RpcPacketPreamble constructPreamble(int payloadLength) {
		return new RpcPacketPreamble(payloadLength);
	}
	
	/**
	 * Retrieve a preamble from the passed-in payload byte buffer. Will move
	 * the byte buffer pointer accordingly.
	 * 
	 * @param payload non-null payload
	 * @return new RpcPacketPreamble as retrieved from the payload buffer.
	 */
	
	public static RpcPacketPreamble retrievePreamble(ByteBuffer payload) {
		if (payload == null) {
			throw new NullPointerError(
					"Null payload buffer passed to RpcPacketPreamble.retrievePreamble()");
		}
		
		byte[] bytes = new byte[RPC_PREAMBLE_SIZE];
		
		try {
			payload.get(bytes);
		} catch (BufferUnderflowException bue) {
			throw new ProtocolError(bue.getLocalizedMessage(), bue);
		}
		
		return new RpcPacketPreamble(bytes);
	}
	
	/**
	 * Retrieve the preamble from raw bytes. Most sanity checking is done in
	 * the RpcPacketPreamble constructor.
	 */
	public static RpcPacketPreamble retrievePreamble(byte[] bytes) {
		return new RpcPacketPreamble(bytes);
	}
	
	/**
	 * Construct a suitable preamble for the passed-in payload
	 * buffer.
	 * 
	 * @param payload non-null byte buffer representing the payload
	 * 			to be preambleated.
	 */
	
	private RpcPacketPreamble(ByteBuffer payload) {
		if (payload == null) {
			throw new NullPointerError(
					"Null payload buffer passed to RpcPacketPreamble constructor");
		}
		
		preChksumBytes = new byte[RPC_PREAMBLE_CHKSUM_SIZE];
		preLengthBytes = RpcPacket.encodeInt4(payload.position());
		
		preChksumBytes[0] = (byte) (preLengthBytes[0]
                                       ^ preLengthBytes[1]
                                       ^ preLengthBytes[2]
                                       ^ preLengthBytes[3]);
	}
	
	/**
	 * Construct a preamble for the passed-in payload length.
	 */
	
	private RpcPacketPreamble(int length) {
		
		preChksumBytes = new byte[RPC_PREAMBLE_CHKSUM_SIZE];
		preLengthBytes = RpcPacket.encodeInt4(length);
		
		preChksumBytes[0] = (byte) (preLengthBytes[0]
                                       ^ preLengthBytes[1]
                                       ^ preLengthBytes[2]
                                       ^ preLengthBytes[3]);
	}
	
	/**
	 * Construct a suitable preamble object by reading the raw values
	 * from the passed-in byte array.
	 * 
	 * @param bytes non-null byte array exactly RPC_PREAMBLE_SIZE bytes long.
	 */
	
	private RpcPacketPreamble(byte[] bytes) {
		if (bytes == null) {
			throw new NullPointerError(
				"Null payload bytes passed to RpcPacketPreamble constructor");
		}
		
		if (bytes.length != RPC_PREAMBLE_SIZE) {
			throw new P4JavaError(
					"Incorrect byte array size passed to RpcPacketPreamble constructor: "
					+ bytes.length);
		}
		
		this.preChksumBytes = new byte[RPC_PREAMBLE_CHKSUM_SIZE];
		this.preChksumBytes[0] = bytes[0];
		
		this.preLengthBytes = new byte[RpcPacket.RPC_LENGTH_FIELD_LENGTH];
		
		for (int i = 0; i < RpcPacket.RPC_LENGTH_FIELD_LENGTH; i++) {
			this.preLengthBytes[i] = bytes[i + RPC_PREAMBLE_CHKSUM_SIZE];
		}
	}
	
	/**
	 * Return the payload size (in bytes) specified by this preamble.
	 * 
	 * @return the associated payload size in bytes.
	 */
	
	public int getPayloadSize() {
		return RpcPacket.decodeInt4(preLengthBytes);
	}
	
	/**
	 * Return true iff the simple checksum checks out.
	 */
	
	public boolean isValidChecksum() {
		return (preChksumBytes[0] ==
					(preLengthBytes[0] ^ preLengthBytes[1] ^ preLengthBytes[2] ^ preLengthBytes[3]));
	}
	
	/**
	 * Return a ByteBuffer representing the marshaled version
	 * of this preamble. Buffer will need to be flipped before sending...
	 * 
	 * @return non-null ByteBuffer ready for sending
	 */
	public ByteBuffer marshal() {
		return ByteBuffer.allocate(RPC_PREAMBLE_SIZE).put(this.preChksumBytes).put(this.preLengthBytes);
	}
	
	public byte[] marshalAsBytes() {
		byte[] retVal = new byte[RPC_PREAMBLE_SIZE];
		retVal[0] = this.preChksumBytes[0];
		for (int i = 0; i < RpcPacket.RPC_LENGTH_FIELD_LENGTH; i++) {
			retVal[i+1] = this.preLengthBytes[i];
		}
		
		return retVal;
	}
}
