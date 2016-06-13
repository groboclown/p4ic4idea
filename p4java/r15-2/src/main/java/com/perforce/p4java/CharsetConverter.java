package com.perforce.p4java;

import com.perforce.p4java.exception.ClientError;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

/**
 * P4Java charset converter class
 */
public class CharsetConverter {

    // Encode this to get byte order mark
    final String bomChar = "\uFEFF";

	private CharsetDecoder decoder;
	private CharsetEncoder encoder;

	private byte[] underflow;
	private boolean checkBOM = false;
	private boolean ignoreBOM = false;

	/**
	 * Creates a new charset converted that decodes/encodes bytes in the
	 * specified non-null from/to charset objects specified.
	 * 
	 * @param fromCharset
	 * @param toCharset
	 * @param ignoreBOM
	 *            - true to ignore any byte order marks written by the UTF-16
	 *            charset and omit them from all return byte buffers
	 */
	public CharsetConverter(Charset fromCharset, Charset toCharset,
			boolean ignoreBOM) {
		// Create decoder that reports malformed/unmappable values
		this.decoder = fromCharset.newDecoder();
		this.decoder.onMalformedInput(CodingErrorAction.REPORT);
		this.decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

		// Create encoder that reports malformed/unmappable values
		this.encoder = toCharset.newEncoder();
		this.encoder.onMalformedInput(CodingErrorAction.REPORT);
		this.encoder.onUnmappableCharacter(CodingErrorAction.REPORT);

		// Check bom on UTF-16 since Java writes a BOM on each call to encode
		if ("UTF-16".equals(toCharset.name())) {
			checkBOM = true;
		}
		this.ignoreBOM = ignoreBOM;
	}

	/**
	 * Get charset name of from charset used to decode
	 * 
	 * @return - charset name
	 */
	public String getFromCharsetName() {
		return this.decoder.charset().name();
	}

	/**
	 * Get charset name of to charset used to encode
	 * 
	 * @return - charset name
	 */
	public String getToCharsetName() {
		return this.encoder.charset().name();
	}

	/**
	 * Creates a new charset converted that decodes/encodes bytes in the
	 * specified non-null from/to charset objects specified.
	 * 
	 * @param fromCharset
	 * @param toCharset
	 */
	public CharsetConverter(Charset fromCharset, Charset toCharset) {
		this(fromCharset, toCharset, false);
	}

	/**
	 * Get and clear the current converted underflow byte array. This results of
	 * this method should be wrapped in a {@link ByteBuffer} and specified as
	 * the from buffer on a call to {@link #convert(ByteBuffer)} to try convert
	 * any remaining bytes.
	 * 
	 * @return - byte array of underflow or null if the last call to
	 *         {@link #convert(ByteBuffer)} did not have underflow.
	 */
	public byte[] clearUnderflow() {
		byte[] cleared = this.underflow;
		this.underflow = null;
		return cleared;
	}

	/**
	 * Converts a char buffer to a byte buffer using the toCharset. This ignores
	 * any existing underflow since the characters to convert are already
	 * complete and known.
	 * 
	 * @param from
	 * @return - byte buffer, use {@link ByteBuffer#position()} for starting
	 *         array offset, {@link ByteBuffer#limit()} for number of bytes to
	 *         read, and {@link ByteBuffer#array()} for the byte[] itself.
	 */
	public ByteBuffer convert(CharBuffer from) {
		ByteBuffer converted = null;
		try {

			// Encode back to byte buffer
			converted = encoder.encode(from);

			// UTF-16
			if (checkBOM) {
				// Endianness (byte order) of client OS
				// Java default to BE byte order, so we need to handle if native byte order is LE
				if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
					try {
					    // Append BOM char and get the bytes in UTF-16LE encoding (manually add BOM)
						byte[] bytes = (bomChar + from.rewind().toString()).getBytes("UTF-16LE");
						converted = ByteBuffer.wrap(bytes);
						converted.order(ByteOrder.LITTLE_ENDIAN);
					} catch (UnsupportedEncodingException uee) {
						Log.exception(uee);
						throw new ClientError("Translation of file content failed", uee);
					}
				}
				
				// Ignore BOM if UTF-16 and not first call to convert
				if (ignoreBOM) {
					int limit = converted.limit();
					if (limit > 2) {
						byte[] bom = new byte[2];
						converted.get(bom);
						// byte value of -2 == 0xFE
						// byte value of -1 == 0xFF
						// Big Endian BOM = FEFF
						// Little Endiam BOM = FFFE
						if ((bom[0] == -2 && bom[1] == -1)
								|| (bom[0] == -1 && bom[1] == -2)) {
							// Advance past BOM if detected
							converted.position(2);
							converted.limit(limit - 2);
						} else {
							// Rewind buffer if BOM not found
							converted.rewind();
						}
					}
				} else {
					ignoreBOM = true;
				}
			} else {
				converted.position(0);
			}
		} catch (CharacterCodingException cce) {
			Log.exception(cce);
			throw new ClientError("Translation of file content failed", cce);
		}
		return converted;
	}

	/**
	 * Convert a byte buffer by decoding using the fromCharset and encoding
	 * using the toCharset. The byte buffer returned will have its position be
	 * the array offset to use and the limit be the lenght of bytes to read from
	 * the byte buffer's backing array.
	 * 
	 * Any remaining bytes that couldn't be converted are stored locally until
	 * the next call to {@link #convert(ByteBuffer)}. The from buffer specified
	 * will be joined with the underflow from a previous call on subsequent
	 * calls to {@link #convert(ByteBuffer)}.
	 * 
	 * @param from
	 *            - byte buffer to convert
	 * @param lookahead
	 *            - lookahead callback
	 * @return - byte buffer, use {@link ByteBuffer#position()} for starting
	 *         array offset, {@link ByteBuffer#limit()} for number of bytes to
	 *         read, and {@link ByteBuffer#array()} for the byte[] itself.
	 */
	public ByteBuffer convert(ByteBuffer from, ILookahead lookahead) {
		ByteBuffer converted = null;

		// Check if there are any left over bytes that weren't converted from
		// the last chunk
		if (underflow != null) {
			ByteBuffer joinedBuffer = ByteBuffer.allocate(from.array().length
					+ underflow.length);
			joinedBuffer.put(underflow);
			joinedBuffer.put(from);
			from = joinedBuffer;
			from.rewind();
			this.underflow = null;
		}

		CharBuffer sourceChars = CharBuffer.allocate(Math.round(decoder
				.maxCharsPerByte()
				* from.limit()) + 1);

		decoder.decode(from, sourceChars, true);
		sourceChars.flip();

		if (lookahead != null && sourceChars.limit() > 0) {

			// Get an array of bytes to attempt to convert
			byte[] ahead = lookahead.bytesToAdd(sourceChars.charAt(sourceChars
					.limit() - 1));

			// Look until no more lookahead is triggered by callback
			while (ahead != null && ahead.length > 0) {
				byte[] next = null;

				// Join lookahead with previous underflow from last call to
				// decode if present
				if (from.hasRemaining()) {
					int remaining = from.remaining();
					next = new byte[ahead.length + remaining];
					from.get(next, 0, remaining);
					System.arraycopy(ahead, 0, next, remaining, ahead.length);
				} else {
					next = ahead;
				}

				from = ByteBuffer.wrap(next);
				// Create new char buffer with underflow + lookahead
				CharBuffer aheadChars = CharBuffer.allocate(Math.round(decoder
						.maxCharsPerByte()
						* from.limit()) + 1);

				// Decode underflow + lookahead
				decoder.decode(from, aheadChars, true);
				aheadChars.flip();

				// If decoding produced at least one usable character than join
				// with main char buffer and query for more lookahead based on
				// new ending char in buffer
				if (aheadChars.limit() > 0) {
					CharBuffer joinedChars = CharBuffer.allocate(aheadChars
							.limit()
							+ sourceChars.limit());
					joinedChars.put(sourceChars);
					joinedChars.put(aheadChars);
					sourceChars = joinedChars;
					sourceChars.rewind();
					ahead = lookahead.bytesToAdd(sourceChars.charAt(sourceChars
							.limit() - 1));
				} else {
					// If no chars were decoded then break out of loop
					ahead = null;
				}
			}
		}

		// Store any left over bytes for the next write chunk
		if (from.hasRemaining()) {
			byte[] leftOver = new byte[from.remaining()];
			from.get(leftOver, 0, from.remaining());
			this.underflow = leftOver;
		}

		// Encode back to byte buffer
		converted = convert(sourceChars);
		return converted;
	}

	/**
	 * Convert a byte buffer by decoding using the fromCharset and encoding
	 * using the toCharset. The byte buffer returned will have its position be
	 * the array offset to use and the limit be the length of bytes to read from
	 * the byte buffer's backing array.
	 * 
	 * Any remaining bytes that couldn't be converted are stored locally until
	 * the next call to {@link #convert(ByteBuffer)}. The from buffer specified
	 * will be joined with the underflow from a previous call on subsequent
	 * calls to {@link #convert(ByteBuffer)}.
	 * 
	 * @param from
	 *            - byte buffer to convert
	 * @return - byte buffer, use {@link ByteBuffer#position()} for starting
	 *         array offset, {@link ByteBuffer#limit()} for number of bytes to
	 *         read, and {@link ByteBuffer#array()} for the byte[] itself.
	 */
	public ByteBuffer convert(ByteBuffer from) {
		return convert(from, null);
	}

}
