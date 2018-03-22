package com.perforce.p4java;

import com.perforce.p4java.exception.ClientError;
import com.perforce.p4java.exception.FileDecoderException;
import com.perforce.p4java.exception.FileEncoderException;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.Utf8ByteHelper;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * P4Java charset converter class
 */
public class CharsetConverter {

	// Encode this to get byte order mark
	final String bomChar = "\uFEFF";

	private CharsetDecoder decoder;
	private CharsetEncoder encoder;

	private boolean checkBOM = false;
	private boolean ignoreBOM = false;

	// UTF8 remainder buffer
	private ByteBuffer remainder = null;

	/**
	 * Creates a new charset converted that decodes/encodes bytes in the
	 * specified non-null from/to charset objects specified.
	 *
	 * @param fromCharset
	 * @param toCharset
	 * @param ignoreBOM   - true to ignore any byte order marks written by the UTF-16
	 *                    charset and omit them from all return byte buffers
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
	 * Converts a char buffer to a byte buffer using the toCharset. This ignores
	 * any existing underflow since the characters to convert are already
	 * complete and known.
	 *
	 * @param from
	 * @return - byte buffer, use {@link ByteBuffer#position()} for starting
	 * array offset, {@link ByteBuffer#limit()} for number of bytes to
	 * read, and {@link ByteBuffer#array()} for the byte[] itself.
	 * @throws FileEncoderException
	 */
	public ByteBuffer convert(CharBuffer from) throws FileEncoderException {
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
			throw new FileEncoderException("Translation of file content failed", cce);
		}
		return converted;
	}

	/**
	 * Convert a byte buffer by decoding using the fromCharset and encoding
	 * using the toCharset. The byte buffer returned will have its position be
	 * the array offset to use and the limit be the length of bytes to read from
	 * the byte buffer's backing array.
	 * <p>
	 * Any remaining bytes that couldn't be converted are stored locally until
	 * the next call to {@link #convert(ByteBuffer)}. The from buffer specified
	 * will be joined with the underflow from a previous call on subsequent
	 * calls to {@link #convert(ByteBuffer)}.
	 *
	 * @param from - byte buffer to convert
	 * @return - byte buffer, use {@link ByteBuffer#position()} for starting
	 * array offset, {@link ByteBuffer#limit()} for number of bytes to
	 * read, and {@link ByteBuffer#array()} for the byte[] itself.
	 * @throws FileEncoderException
	 * @throws FileDecoderException
	 */
	public ByteBuffer convert(ByteBuffer from) throws FileDecoderException, FileEncoderException {
		if (CharsetDefs.UTF8.equals(decoder.charset())) {
			from = getUtf8BufferWindow(from);
		}

		int size = from.limit() * 2;
		CharBuffer sourceChars = CharBuffer.allocate(size);

		CoderResult res = decoder.decode(from, sourceChars, true);
		if (res.isError()) {
			throw new FileDecoderException();
		}

		sourceChars.flip();

		// Encode back to byte buffer
		ByteBuffer converted = convert(sourceChars);
		return converted;
	}

	/**
	 * Returns a window into the buffer containing whole UTF8 words.  Split UTF8 multi byte words
	 * falling over the buffer boundary are added to the remainder.
	 *
	 * @param buffer utf8 byte stream
	 * @return whole utf8 words to convert.
	 * @throws FileDecoderException
	 */
	private ByteBuffer getUtf8BufferWindow(ByteBuffer buffer) throws FileDecoderException {
		// Add remainder from previous calculation
		buffer = addRemainder(buffer);

		// exit if buffer is empty.
		int r = buffer.remaining();
		if (r < 1) {
			buffer.rewind();
			return buffer;
		}

		// check last byte; exit with full buffer if SINGLE UTF8 byte
		byte lastByte = buffer.get(r - 1);
		Utf8ByteHelper type = Utf8ByteHelper.parse(lastByte);
		if (Utf8ByteHelper.SINGLE.equals(type)) {
			buffer.rewind();
			return buffer;
		}

		// get end of buffer window and set limit
		int end = Utf8ByteHelper.findBufferLimit(buffer);

		// set remainder; set old limit and move to end
		buffer.position(end);
		r = buffer.remaining();
		if (r > 0) {
			remainder = ByteBuffer.allocate(r);
			remainder.put(buffer);
		}

		// rewind and set new limit
		buffer.rewind();
		buffer.limit(end);
		return buffer;
	}

	/**
	 * Add the remaining bytes from the split UTF8 word to the start of the buffer
	 *
	 * @param buffer to be converted
	 * @return new buffer with the remainder.
	 */
	private ByteBuffer addRemainder(ByteBuffer buffer) {
		ByteBuffer combined;
		if (remainder != null) {
			remainder.clear();
			int last = remainder.remaining();
			combined = ByteBuffer.allocate(last + buffer.remaining());
			combined.put(remainder);
			combined.put(buffer);
			remainder = null;
			combined.rewind();
			return combined;
		} else {
			return buffer;
		}
	}
}
