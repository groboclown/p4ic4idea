/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.packet;

import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldRule;

/**
 * Describes a value / name packet pair as marshaled
 * on to or off the RPC wire between Perforce clients and servers.<p>
 * 
 * Format on wire (from C++ API source), in bytes:
 * <pre>
 *         var<00><l1><l2><l3><l4>value<00>
 * </pre>
 * 
 * where either or both var and value can be empty (null),
 * and where value can be interpreted as text (utf-8 or ascii,
 * depending on settings) binary bytes, and where
 * the four byte value length spec does not include the terminating
 * null byte, and is encoded as per RpcPacket.encodeInt4().
 * 
 *
 */

public class RpcPacketField {
	
	public static final String TRACE_PREFIX = "RpcPacketField";
	
	/**
	 * How many elements in each packet field. Changing this will probably
	 * cause havoc.
	 */
	public static final int NUM_ELEMENTS = 2;
	
	/**
	 * Which element will contain (or not) the field's name.
	 */
	public static final int NAME_FIELD = 0;
	
	/**
	 * Which element will contain (or not) the field's value.
	 */
	public static final int VALUE_FIELD = 1;
		
	private String name = null;
	private Object value = null;

	/**
	 * Construct a packet field from the passed-n name / value pair.
	 * 
	 * @param name possibly-null name
	 * @param value possibly-null value
	 */
	
	public RpcPacketField(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Attempt to pick off a name / value field pair from the passed-in
	 * byte buffer. Will always return a two-element object array, but either
	 * or both of the objects may be null. Element zero is always the name (and
	 * will always be text (a string) if it's not null; element one is the corresponding
	 * value, and (if it's not null) will be a byte array whose interpretation must be
	 * done by the caller in the light of what's expected in context -- it might represent
	 * a normal string or it might actually be binary bytes.<p>
	 * 
	 * Updates the buffer's position accordingly. Have to be careful
	 * to use charsets with bytes and strings properly here and in the caller
	 * to keep internationalization and localization straight.
	 */
	
	public static Object[] retrievePacketField(ByteBuffer buf, boolean isUnicodeServer,
												Charset charset) {
		return retrievePacketField(buf, isUnicodeServer, charset, null);
	}

	/**
	 * Attempt to pick off a name / value field pair from the passed-in byte
	 * buffer with an optional rule to handle the RPC packet fields.
	 */
	public static Object[] retrievePacketField(ByteBuffer buf, boolean isUnicodeServer,
												Charset charset, RpcPacketFieldRule fieldRule) {
		
		if (buf == null) {
			throw new NullPointerError(
					"Null byte buffer passed to RpcPacketField.retrievePacketField()");
		}
				
		Object[] retVal;

		retVal = new Object[NUM_ELEMENTS];
		retVal[NAME_FIELD] = null;
		retVal[VALUE_FIELD] = null;
		
		// Get name string; may be empty (i.e. first byte is null); also have
		// to deal with the fact that the name string may be arbitrarily large...
		
		final int INITIAL_BYTEARRAY_SIZE = 128; // bytes
		
		byte[] bytes = new byte[INITIAL_BYTEARRAY_SIZE];
		
		int i = 0;
		while ((bytes[i] = buf.get()) != 0) {
			i++;
			if (i >= bytes.length) {
				// Resize needed...
				byte[] newBytes = new byte[bytes.length + INITIAL_BYTEARRAY_SIZE];
				System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
				bytes = newBytes;
			}
		}
		
		if (i > 0) {
			// Field names are currently always assumed to be in the default
			// charset, i.e. RpcConnection.NON_UNICODE_SERVER_CHARSET unless
			// the server is in Unicode mode.
			try {
				retVal[NAME_FIELD] = new String(bytes, 0, i, charset == null ?
						RpcConnection.NON_UNICODE_SERVER_CHARSET_NAME :
							(isUnicodeServer ? CharsetDefs.UTF8_NAME : charset.name()));
			} catch (UnsupportedEncodingException e) {
				// This should never be reached since we already have the Charset
				// object needed.
				Log.exception(e);
			}
		}
		
		// Get value string length which may be zero):
		
		int remaining = buf.remaining();
		if (remaining < RpcPacket.RPC_LENGTH_FIELD_LENGTH) {
			throw new ProtocolError(
				"Insufficient bytes in buffer to retrieve text value field length");
		}
		
		byte[] lengthBytes = new byte[RpcPacket.RPC_LENGTH_FIELD_LENGTH];
		buf.get(lengthBytes);
		
		int valLength = RpcPacket.decodeInt4(lengthBytes);
		
		if (valLength < 0) {
			throw new ProtocolError(
					"Negative text field value length in P4JRpcTextField initializer: "
					+ valLength);
		}
		
		// Get value (may be empty); at least here we know the
		// supposed number of the incoming bytes for the value:
		
		if (remaining < valLength) {
			throw new ProtocolError(
				"Insufficient bytes in buffer to retrieve text value field");
		}
		
		byte[] valBytes = new byte[valLength];
		buf.get(valBytes);

		String fieldName = (String) retVal[NAME_FIELD];

		// Check for field rule on data conversion

		boolean skipConversion = false;
		if (fieldRule != null) {
			fieldRule.update(fieldName);
			skipConversion = fieldRule.isSkipConversion();
		}
		
		if (!skipConversion &&
				RpcPacketFieldType.getFieldType((String) retVal[NAME_FIELD]) == RpcPacketFieldType.TEXT) {
			
			// Incoming string is encoded in UTF-8 if we're talking to a Unicode server;
			// otherwise it's in the specified charset, or maybe some 8 bit ASCI variant).
			
			try {
				retVal[VALUE_FIELD] = new String(valBytes, charset == null ?
						RpcConnection.NON_UNICODE_SERVER_CHARSET_NAME :
								(isUnicodeServer ? CharsetDefs.UTF8_NAME : charset.name()));
			} catch (UnsupportedEncodingException e) {
				// This should never be reached since we already have the Charset
				// object needed.
				Log.exception(e);
			}
		} else {
			retVal[VALUE_FIELD] = valBytes; // If unicode is involved here (as, e.g., file contents),
											// it'll be converted elsewhere...
		}
		buf.get();	// Step over the terminating null
		
		return retVal;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Object getValue() {
		return this.value;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	/**
	 * Marshal the passed-in packet fields onto a ByteBuffer. Affects
	 * the buffer's position, etc., accordingly.<p>
	 * @throws UnsupportedEncodingException 
	 */
	
	public static void marshal(ByteBuffer buf, String name, String value, Charset charset)
										throws BufferOverflowException, UnsupportedEncodingException {
		marshal(buf, name,
			(value == null ? null : value.getBytes(
					charset == null ? CharsetDefs.DEFAULT.name() : charset.name())));
	}
	
	/**
	 * Marshal the passed-in packet fields onto a ByteBuffer. Affects
	 * the buffer's position, etc., accordingly.<p>
	 * @throws UnsupportedEncodingException 
	 */
	
	public static void marshal(ByteBuffer buf, String name, StringBuffer value, Charset charset)
										throws BufferOverflowException, UnsupportedEncodingException {
		marshal(buf, name,
			(value == null ? null : value.toString().getBytes(
							charset == null ? CharsetDefs.DEFAULT.name() : charset.name())));
	}
	/**
	 * Marshal the passed-in packet fields onto a ByteBuffer. Affects
	 * the buffer's position, etc., accordingly.
	 */
	
	public static void marshal(ByteBuffer buf, String name, byte[] value)
										throws BufferOverflowException {
		if (buf == null) {
			throw new NullPointerError(
					"Null byte buffer passed to RpcPacketField.marshal()");
		}
		
		try {
			if (name != null) {
				buf.put(name.getBytes(CharsetDefs.DEFAULT.name()));
			}
			buf.put((byte) 0);
			
			int valLength = 0;
			
			if (value != null) {
				valLength = value.length;
			}

			buf.put(RpcPacket.encodeInt4(valLength));
			
			if (value != null) {
				buf.put(value);
			}
			buf.put((byte) 0);
		} catch (BufferOverflowException boe) {
			// Will attempt to increase the size of the buffer in the caller
			// (some time in the future, at any rate...)
			
			throw boe;
		} catch (Throwable thr) {
			// Should be buffer overflow errors only; in any case,
			// this is really a panic...
			
			Log.error("Unexpected exception: " + thr.getLocalizedMessage());
			Log.exception(thr);
			throw new P4JavaError(thr.getLocalizedMessage());
		}
	}
	
	/**
	 * Marshal the passed-in packet fields onto a ByteBuffer. Affects
	 * the buffer's position, etc., accordingly. Since this one uses
	 * special ByteBuffer to ByteBuffer methods, it gets its own
	 * version rather than using the byte[] method.<p>
	 * 
	 * Note: incoming value ByteBuffer must have been flipped ready for
	 * sending; value's position will be updated by this method, so 
	 * if the buffer is to be reused, you must flip or reset (or whatever)
	 * it yourself.<p>
	 */
	
	public static void marshal(ByteBuffer buf, String name, ByteBuffer value)
								throws BufferOverflowException {
		if (buf == null) {
			throw new NullPointerError(
					"Null byte buffer passed to RpcPacketField.marshal()");
		}

		try {
			if (name != null) {
				buf.put(name.getBytes(CharsetDefs.DEFAULT.name()));
			}
			buf.put((byte) 0);
			
			int valLength = 0;
			
			if (value != null) {
				valLength = value.limit();
			}

			buf.put(RpcPacket.encodeInt4(valLength));
			
			if (value != null) {
				buf.put(value);
			}
			buf.put((byte) 0);
		} catch (Throwable thr) {
			Log.error("Unexpected exception: " + thr.getLocalizedMessage());
			Log.exception(thr);
			throw new P4JavaError("Unexpected exception in RpcPacketField.marshal(ByteBuffer): "
										+ thr.getLocalizedMessage(), thr);
		}
	}
	
	public static void marshal(byte[] buf, String name, byte[] value)
						throws BufferOverflowException {
		throw new P4JavaError("Called unimplemented marshall method");
	}
}
