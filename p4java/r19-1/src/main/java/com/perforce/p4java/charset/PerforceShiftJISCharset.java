/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import com.perforce.p4java.PropertyDefs;

/**
 * Charset implementation which performs P4ShiftJIS encoding. P4-ShiftJIS
 * encoding uses the MS932 (Microsoft code page 932) encoding, a superset of
 * Shift-JIS. Additionally, P4-ShiftJIS applies Perforce specific updates to
 * characters
 */
public class PerforceShiftJISCharset extends Charset {
	// Environment variable for enabling Perforce specific updates
	public static final boolean UNICODE_MAPPING = (System.getProperty(PropertyDefs.UNICODE_MAPPING_SHORT_FORM,
			System.getProperty(PropertyDefs.UNICODE_MAPPING, null))) == null ? false : true;

	// The name of the real charset encoding delegate.
	// Microsoft code page 932 (windows-31j, csWindows31J, windows-932, MS932)
	private static final String CHARSET_NAME = "MS932";

	// Handle to the real charset for transcoding between characters and bytes.
	Charset charset;

	/**
	 * Constructor for the P4ShiftJIS charset. Call the superclass constructor
	 * to pass along the name and aliases.
	 */
	protected PerforceShiftJISCharset(String canonical, String[] aliases) {
		super(canonical, aliases);
		charset = Charset.forName(CHARSET_NAME);
	}

	// ----------------------------------------------------------

	/**
	 * Called by users of this Charset to obtain an encoder.
	 */
	public CharsetEncoder newEncoder() {
		return new Encoder(this, charset.newEncoder());
	}

	/**
	 * Called by users of this Charset to obtain a decoder.
	 */
	public CharsetDecoder newDecoder() {
		return new Decoder(this, charset.newDecoder());
	}

	/**
     * Tells whether or not this charset contains the given charset.
	 */
	public boolean contains(Charset cs) {
        return this.getClass().isInstance(cs);
	}

	/**
	 * Apply Perforce specific updates.
	 */
	private void update(CharBuffer cb) {
		if (UNICODE_MAPPING) {
			for (int pos = cb.position(); pos < cb.limit(); pos++) {
				char c = cb.get(pos);
				switch (c) {
				case '\\':
					cb.put(pos, '\u00A5');
					break;
				case '\u007E':
					cb.put(pos, '\u203E');
					break;
				default:
				}
			}
		}
	}

	/**
	 * The encoder implementation for the P4ShiftJIS Charset.
	 */
	private class Encoder extends CharsetEncoder {
		private CharsetEncoder encoder;

		/**
		 * Call the superclass constructor with the Charset object and the
		 * encodings sizes from the encoder.
		 */
		Encoder(Charset cs, CharsetEncoder encoder) {
			super(cs, encoder.averageBytesPerChar(), encoder.maxBytesPerChar());
			this.encoder = encoder;
		}

		/**
		 * Implementation of the encoding loop. Apply Perforce specific updates,
		 * then reset the encoder and encode the characters to bytes.
		 */
		protected CoderResult encodeLoop(CharBuffer cb, ByteBuffer bb) {
			CharBuffer tmpcb = CharBuffer.allocate(cb.remaining());
			while (cb.hasRemaining()) {
				tmpcb.put(cb.get());
			}
			tmpcb.rewind();
			update(tmpcb);
			encoder.reset();
			CoderResult cr = encoder.encode(tmpcb, bb, true);
			cb.position(cb.position() - tmpcb.remaining());
			return (cr);
		}
	}

	/**
	 * The decoder implementation for the P4ShiftJIS Charset.
	 */
	private class Decoder extends CharsetDecoder {
		private CharsetDecoder decoder;

		/**
		 * Call the superclass constructor with the Charset object and pass
		 * along the chars/byte values from the decoder.
		 */
		Decoder(Charset cs, CharsetDecoder decoder) {
			super(cs, decoder.averageCharsPerByte(), decoder.maxCharsPerByte());
			this.decoder = decoder;
		}

		/**
		 * Implementation of the decoding loop. Reset the decoder and decode the
		 * bytes into characters, then apply Perforce specific updates.
		 */
		protected CoderResult decodeLoop(ByteBuffer bb, CharBuffer cb) {
			decoder.reset();
			CoderResult result = decoder.decode(bb, cb, true);
			update(cb);
			return (result);
		}
	}
}
