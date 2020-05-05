/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.packet;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.impl.mapbased.rpc.ExternalEnv;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldRule;
import com.perforce.p4java.server.callback.IFilterCallback;

/**
 * Describes the format of, and implements a bunch of methods for,
 * Perforce RPC packets as pushed across the wire between
 * Perforce clients and servers.<p>
 * 
 * Each RPC packet consists of the following elements (as cribbed from
 * the C++ API code and byte-level TCP/IP packet analysis done on p4
 * server network traffic):
 * <ul>
 * <li>a preamble that specifies the length of the rest of the packet
 *     (the payload) plus a simple sanity check checksum; the format and
 *     semantics of the preamble are given in the class RpcPacketPreamble
 *     below.
 * <li>an optional argument argument or results element containing zero
 *     or more packet fields (described in the RpcPacketField class
 *     Javadoc) used to provide arguments for server-side functions
 *     or to provide results from the server. These fields can be interpreted
 *     as either text fields or byte fields, according to the RPC function
 *     and context.
 * <li>an optional client environment specifier that spells out client
 *     state, etc.; this is only sent by a client, and only with the
 *     initial user command.
 * <li>a function field, specifying the function this packet is requesting.
 *     Functions in this usage can be user functions (initial requests
 *     that the Perforce server do something for the end user, e.g. "info"),
 *     or client functions (something the Perforce server is asking the
 *     client to do, e.g. "client message", a request to send the associated
 *     text to the end user), or one of an obscure menagerie of other
 *     functions spelled out elsewhere.
 * </ul>
 * All elements except the preamble are actually serialized arrays of text or byte
 * fields in the format described in the RpcPacketField class. This makes the
 * overall format fairly uniform and relatively straightforward to pick off
 * the wire in most cases.<p>
 * 
 * Note that the order of individual fields within the overall scheme
 * is theoretically unimportant (with the obvious exception of the preamble,
 * which has to be first), but there may be unknown server dependencies on
 * element order, so we try to closely follow the C++ API's ordering in our
 * implementation.<p>
 * 
 * Note that RPC packets are not in any way synonymous with TCP
 * packets or any underlying transport layer packet mechanism -- text
 * packets can span TCP packets or several RPC packets can be packed into a
 * single TCP packet, for example.<p>
 * 
 *
 */

public class RpcPacket {
	
	public static final int DEFAULT_RPC_PACKET_BUFFER_SIZE = 2048; // and why not?
	
	/**
	 * Length in bytes of the RPC packet length fields. This is a <i>very</i>
	 * fundamental constant; changing this will probably cause catastrophic
	 * P4Java misbehavior.
	 */
	public static final int RPC_LENGTH_FIELD_LENGTH = 4;
	
	public static final String TRACE_PREFIX = "RpcPacket";

	@SuppressWarnings("unused")
	private RpcFunctionSpec funcName = null;	// NOTE: do not use this field
												// unless you know what you're doing...
	private String funcNameString = null;
	private ExternalEnv env = null;
	private String[] strArgs = null;
	private Map<String, Object> mapArgs = null; // for named args going to the wire
	private Map<String, Object> resultsMap = null;	// for results off the wire
	private int packetLength = 0;
	
	/**
	 * Return a four byte array ready for sending across the wire that
	 * represents in Perforce standard wire form the integer passed in.<p>
	 * 
	 * Used extensively in the Perforce RPC protocol to encode ints before
	 * sending them across the wire.
	 */
	
	public static byte[] encodeInt4(int i) {
		byte[] bytes = new byte[RPC_LENGTH_FIELD_LENGTH];
	
		bytes[0] = (byte) ((i / 0x1 ) % 0x100);
		bytes[1] = (byte) ((i / 0x100 ) % 0x100);
		bytes[2] = (byte) ((i / 0x10000 ) % 0x100);
		bytes[3] = (byte) ((i / 0x1000000 ) % 0x100);
		
		return bytes;
	}
	
	/**
	 * Decode a Java int from the passed-in 4 byte Perforce encoded
	 * integer value.<p>
	 * 
	 * Used extensively in the Perforce RPC protocol to decode ints
	 * coming in off the wire. Note that we have to go to some length
	 * to convince Java that we want unsigned byte (it's amazing that
	 * Java still doesn't have unsigned integral types...); this
	 * helps explain the seemingly-redundant "& 0xFF"'s in the code
	 * below.
	 */
	
	public static int decodeInt4(byte[] bytes) {
		if (bytes == null) {
			throw new NullPointerError(
					"Null bytes passed to RpcPacket.decodeInt");
		}
		if (bytes.length != RPC_LENGTH_FIELD_LENGTH) {
			throw new ProtocolError(
					"Bad byte array size in RpcPacket.decodeInt: "
					+ bytes.length);
		}
		
		return ((bytes[0] & 0xFF) * 0x1)
						+ ((bytes[1] & 0xFF) * 0x100)
						+ ((bytes[2] & 0xFF) * 0x10000)
						+ ((bytes[3] & 0xFF) * 0x1000000);
	}
	
	/**
	 * Construct an RPC packet for a user command.
	 * 
	 * @param funcName non-null function name
	 * @param args potentially-null function arguments as a string array
	 * @param env potentially-null command environment 
	 * @return non-null text packet ready for marshaling (or whatever)
	 */
	
	public static RpcPacket constructRpcPacket(
							RpcFunctionSpec funcName,
							String realName,
							String[] args,
							ExternalEnv env) {
		return new RpcPacket(funcName, realName, args, env);
	}
	
	/**
	 * Construct an RPC packet for a user command.
	 * 
	 * @param funcName non-null function name
	 * @param args potentially-null function arguments in map form
	 * @param env potentially-null command environment 
	 * @return non-null text packet ready for marshaling (or whatever)
	 */
	
	public static RpcPacket constructRpcPacket(
							RpcFunctionSpec funcName,
							Map<String, Object> args,
							ExternalEnv env) {
		return new RpcPacket(funcName, args, env);
	}
	
	/**
	 * Construct an RPC packet for a user command.
	 * 
	 * @param funcName non-null function name
	 * @param args potentially-null function arguments in map form
	 * @param env potentially-null command environment 
	 * @return non-null text packet ready for marshaling (or whatever)
	 */
	
	public static RpcPacket constructRpcPacket(
							String funcName,
							Map<String, Object> args,
							ExternalEnv env) {
		return new RpcPacket(funcName, args, env);
	}
	
	/**
	 * Construct an RPC packet from the passed-in preamble, bytes, and charset.
	 */
	public static RpcPacket constructRpcPacket(RpcPacketPreamble preamble,
												byte[] bytes, boolean isUnicodeServer,
												Charset charset) {
		return constructRpcPacket(preamble, bytes, isUnicodeServer, charset, null, null);
	}

	/**
	 * Construct an RPC packet from the passed-in preamble, bytes, charset and fieldRule.
	 */
	public static RpcPacket constructRpcPacket(RpcPacketPreamble preamble,
												byte[] bytes, boolean isUnicodeServer,
												Charset charset, RpcPacketFieldRule fieldRule,
												IFilterCallback filterCallback) {
		return new RpcPacket(preamble, bytes, isUnicodeServer, charset, fieldRule, filterCallback);
	}

	private RpcPacket(RpcFunctionSpec funcName, String realName, String[] args,
											ExternalEnv env) {
		if (funcName == null) {
			throw new NullPointerError(
				"Null function name passed to RpcPacket constructor");
		}
				
		this.funcName = funcName;
		if (funcName == RpcFunctionSpec.USER_SPECIFIED) {
			// Special case for relaxed user command checking...
			this.funcNameString = "user-" + realName;
		} else {
			this.funcNameString = funcName.getEncoding();
		}
		this.strArgs = args;
		this.env = env;
	}
	
	private RpcPacket(RpcFunctionSpec funcName, Map<String, Object> args, ExternalEnv env) {
		if (funcName == null) {
			throw new NullPointerError(
				"Null function name passed to RpcPacket constructor");
		}
				
		this.funcName = funcName;
		this.funcNameString = funcName.getEncoding();
		this.mapArgs = args;
		this.env = env;
	}
	
	private RpcPacket(String funcName, Map<String, Object> args, ExternalEnv env) {
		if (funcName == null) {
			throw new NullPointerError(
				"Null function name passed to RpcPacket constructor");
		}
				
		this.funcNameString = funcName;
		this.mapArgs = args;
		this.env = env;
	}
	
	private RpcPacket(Map<String, Object> resultsMap, int packetLength) {
		this.resultsMap = resultsMap;
		this.packetLength = packetLength;
		if (resultsMap != null) {
			this.funcNameString = (String) resultsMap.get(RpcFunctionMapKey.FUNCTION);
		}
	}
	
	private RpcPacket(RpcPacketPreamble preamble, byte[] payloadBytes,
						boolean isUnicodeServer, Charset charset,
						RpcPacketFieldRule fieldRule, IFilterCallback filterCallback) {
		if (preamble == null) {
			throw new NullPointerError("null RPC preamble passed to RpcPacket constructor");
		}
		if (!preamble.isValidChecksum()) {
			throw new ProtocolError(
					"Bad checksum in RPC preamble passed to RpcPacket constructor");
		}
		if (payloadBytes == null) {
			throw new NullPointerError("null payload byte array passed to RpcPacket constructor");
		}
		
		int payloadLength = preamble.getPayloadSize();
		if (payloadLength != payloadBytes.length) {
			throw new P4JavaError("bad byte array size in RpcPacket constructor; byte array length: "
					+ payloadBytes.length + "; from preamble: " + payloadLength);
		}
		
		try {
			resultsMap = new HashMap<String, Object>();
			ByteBuffer packetBuf = ByteBuffer.wrap(payloadBytes);
			
			final Map<String, String> doNotSkipKeysMap = filterCallback != null ? filterCallback.getDoNotSkipKeysMap() : null;
			final AtomicBoolean skipSubsequent = new AtomicBoolean(false);
			
			while (packetBuf.position() < packetBuf.limit()) {
				Object[] fields = RpcPacketField.retrievePacketField(packetBuf, isUnicodeServer, charset, fieldRule);

				// Filter callback
				if (filterCallback != null) {
					// Check for RPC protocol related keys which cannot be skipped.
					// Also, check for do-not-skip keys passed-in by the callback.
					if (!RpcFunctionMapKey.RPC_KEYS_MAP.containsKey((String)fields[RpcPacketField.NAME_FIELD]) &&
							!(doNotSkipKeysMap != null && doNotSkipKeysMap.containsKey((String)fields[RpcPacketField.NAME_FIELD]))) {
						if (skipSubsequent != null &&  skipSubsequent.get()) {
							// Skip
							continue;
						} else {
							if (filterCallback.skip((String)fields[RpcPacketField.NAME_FIELD],
									fields[RpcPacketField.VALUE_FIELD], skipSubsequent)) {
								// Skip
								continue;
							}
						}
					}
				}
				
				if (fields[RpcPacketField.NAME_FIELD] == null) {
					resultsMap.put(null, fields[RpcPacketField.VALUE_FIELD]);
				} else {
					// Assumes zeroth field is always a string; this assumption has yet
					// to be broken, but keep a good look out in any case... -- HR.
					
					String fieldName = (String) fields[RpcPacketField.NAME_FIELD];

					// func2 detection below is related to job037970 -- the server is incorrectly
					// sending *two* func2 fields in certain proxy-related circumstances; only the
					// first value is correct. This should be fixed in the server, but this quick
					// fix should help out in the field and with older servers -- HR.
					
					if (fieldName.equalsIgnoreCase(RpcFunctionMapKey.FUNC2)) {
						if (!resultsMap.containsKey(fieldName)) {
							resultsMap.put(fieldName, fields[RpcPacketField.VALUE_FIELD]);
						}
					} else {
						// Handle multiple fields with the same field name by
						// appending an incremental number to the end. This
						// happens in the case of the "istat" command with the
						// "-a -s" flags. The repeating field names: "change",
						// "parentChange", "copyParent", "mergeParent",
						// "mergeHighVal", "branchHash" and "status".
						if (resultsMap.containsKey(fieldName)) {
							int suffixCounter = 0;
							while (true) {
								if (!resultsMap.containsKey(fieldName + suffixCounter)) {
									break;
								}
								suffixCounter++;
							}
							resultsMap.put(fieldName + suffixCounter, fields[RpcPacketField.VALUE_FIELD]);
						} else {
							resultsMap.put(fieldName, fields[RpcPacketField.VALUE_FIELD]);
						}
					}
				}
			}
			
			this.packetLength = payloadLength;
			if (resultsMap != null) {
				this.funcNameString = (String) resultsMap.get(RpcFunctionMapKey.FUNCTION);
			}			

			// Callback reset()
			if (filterCallback != null) {
				filterCallback.reset();
			}

		} catch (ProtocolError pe) {
			throw pe;
		} catch (Throwable thr) {
			Log.error("Unexpected exception: " + thr.getLocalizedMessage());
			Log.exception(thr);
			throw new ProtocolError(thr.getLocalizedMessage(), thr);
		}
	}

	public Map<String, Object> getResultsMap() {
		return this.resultsMap;
	}

	public void setResultsMap(Map<String, Object> resultsMap) {
		this.resultsMap = resultsMap;
	}

	public String getFuncNameString() {
		return this.funcNameString;
	}

	public void setFuncNameString(String funcNameString) {
		this.funcNameString = funcNameString;
	}

	public int getPacketLength() {
		return this.packetLength;
	}

	public void setPacketLength(int packetLength) {
		this.packetLength = packetLength;
	}

	public String[] getStrArgs() {
		return this.strArgs;
	}

	public void setStrArgs(String[] strArgs) {
		this.strArgs = strArgs;
	}

	public Map<String, Object> getMapArgs() {
		return this.mapArgs;
	}

	public void setMapArgs(Map<String, Object> mapArgs) {
		this.mapArgs = mapArgs;
	}

	public ExternalEnv getEnv() {
		return this.env;
	}

	public void setEnv(ExternalEnv env) {
		this.env = env;
	}
}
