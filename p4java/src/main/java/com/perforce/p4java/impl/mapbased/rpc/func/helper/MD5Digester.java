/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.helper;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Provide MD5 digest methods for the rest of the RPC implementation. Basically
 * just a wrapper around the normal Java stuff, with a useful added method to
 * finalise the digest as a hex string.
 * <p>
 *
 * @author Sean Shou - clean code & unit test
 */

public class MD5Digester {
	private static final String DIGEST_TYPE = "MD5";
	private static final int LENGTH_OF_HEX_STRING = 32;
	private int bufferSize = 1024 * 8;

	private MessageDigest messageDigest = null;

	public MD5Digester() throws P4JavaError {
		try {
			messageDigest = MessageDigest.getInstance(DIGEST_TYPE);
			messageDigest.reset();
		} catch (NoSuchAlgorithmException exc) {
			throw new P4JavaError(
					"Unable to create an MD5 digester for P4Java: " + exc.getLocalizedMessage(),
					exc);
		}
	}

	public MD5Digester(@Nonnull int bufferSize) {
		this();
		this.bufferSize = requireNonNull(bufferSize);
	}

	void setMessageDigest(MessageDigest messageDigest) {
		this.messageDigest = messageDigest;
	}

	public byte[] digestAsBytes() {
		return messageDigest.digest();
	}

	/**
	 * Return the results of digesting an arbitrary file with this digester.
	 * <p>
	 * Returns null if it can't read or digest the file for whatever reason;
	 * otherwise the finalized digest is returned as a 32 byte hex string.
	 *
	 * @return - computed digest or null if computation failed
	 */
	@Nullable
	public String digestFileAs32ByteHex(@Nonnull File file) {
		requireNonNull(file, "Null file passed to MD5Digester.digestFileAs32ByteHex()");
		if (Files.isReadable(file.toPath())) {
			try (FileInputStream inStream = new FileInputStream(file)) {
				reset();
				byte[] inBytes = new byte[bufferSize];
				int inBytesRead;
				while ((inBytesRead = inStream.read(inBytes)) > 0) {
					update(inBytes, 0, inBytesRead);
				}

				return digestAs32ByteHex();
			} catch (final IOException ioexc) {
				Log.error("error digesting file: " + file.getPath() + "; exception follows...");
				Log.exception(ioexc);
			}
		}
		return null;
	}

	public void reset() {
		messageDigest.reset();
	}

	public void update(byte[] bytes, int off, int len) {
		if (nonNull(bytes)) {
			messageDigest.update(bytes, off, len);
		}
	}

	/**
	 * Return the finalised digest as a 32 byte hex string. It's important
	 * elsewhere and in the server that the string be exactly 32 bytes long, so
	 * we stitch it up if possible to make it that long...
	 */
	public String digestAs32ByteHex() {
		String retStr = new BigInteger(1, messageDigest.digest()).toString(16).toUpperCase();

		if (retStr.length() > 0 && (retStr.length() <= LENGTH_OF_HEX_STRING)) {
			return StringUtils.leftPad(retStr, LENGTH_OF_HEX_STRING, '0');
		} else {
			throw new P4JavaError("Bad 32 byte digest string size in MD5Digester.digestAs32ByteHex;"
					+ " string: " + retStr + ";" + " length: " + retStr.length());
		}
	}

	/**
	 * Return the results of digesting an arbitrary file with this digester.
	 * <p>
	 * Returns null if it can't read or digest the file for whatever reason;
	 * otherwise the finalized digest is returned as a 32 byte hex string.
	 *
	 * @return - computed digest or null if computation failed
	 */
	@Nullable
	public String digestFileAs32ByteHex(@Nonnull File file, @Nullable Charset charset) {
		return digestFileAs32ByteHex(file, charset, false);
	}

	/**
	 * Return the results of digesting an arbitrary file with this digester.
	 * <p>
	 * Returns null if it can't read or digest the file for whatever reason;
	 * otherwise the finalized digest is returned as a 32 byte hex string.
	 *
	 * @return - computed digest or null if computation failed
	 */
	@Nullable
	public String digestFileAs32ByteHex(@Nonnull File file, @Nullable Charset charset,
	                                    boolean doesNeedConvertLineEndings) {

		return digestFileAs32ByteHex(file, charset, doesNeedConvertLineEndings, null);
	}

	/**
	 * Return the results of digesting an arbitrary file with this digester and
	 * a specific client line ending.
	 * <p>
	 * Returns null if it can't read or digest the file for whatever reason;
	 * otherwise the finalized digest is returned as a 32 byte hex string.
	 *
	 * @return - computed digest or null if computation failed
	 */
	@Nullable
	public String digestFileAs32ByteHex(@Nonnull File file, @Nullable Charset charset,
	                                    boolean isRequireLineEndingConvert, @Nullable ClientLineEnding clientLineEnding) {

		requireNonNull(file, "Null file passed to MD5Digester.digestFileAs32ByteHex()");
		if (Files.isReadable(file.toPath())) {
			try (FileInputStream inStream = new FileInputStream(file)) {
				reset();
				if (nonNull(charset)) {
					digestEncodedStreamToUtf8(inStream, charset, isRequireLineEndingConvert,
							clientLineEnding);
				} else {
					digestStream(inStream, isRequireLineEndingConvert, clientLineEnding);
				}
				return digestAs32ByteHex();
			} catch (IOException ioexc) {
				Log.error("error digesting file: " + file.getPath() + "; exception follows...");
				Log.exception(ioexc);
			}
		}

		return null;
	}

	private void digestEncodedStreamToUtf8(@Nonnull InputStream inStream, @Nonnull Charset charset,
	                                       boolean isRequireLineEndingConvert, @Nullable ClientLineEnding clientLineEnding)
			throws IOException {

		try (BOMInputStream unicodeInputStream = new BOMInputStream(inStream, false,
				ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE,
				ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);

		    InputStreamReader encodedStreamReader = new InputStreamReader(unicodeInputStream,
				    charset)) {
			CharsetEncoder utf8CharsetEncoder = CharsetDefs.UTF8.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);

			char[] buffer = new char[bufferSize];
			int read;
			while ((read = encodedStreamReader.read(buffer)) > 0) {
				// Convert encoded stream to UTF8 since server digest is UTF8
				ByteBuffer utf8ByteBuffer = utf8CharsetEncoder
						.encode(CharBuffer.wrap(buffer, 0, read));

				if (isRequireLineEndingConvert) {
					ByteBuffer convert = findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert(
							encodedStreamReader, utf8CharsetEncoder, utf8ByteBuffer,
							clientLineEnding);

					update(convert.array(), convert.arrayOffset(), convert.limit());
				} else {
					update(utf8ByteBuffer.array(), utf8ByteBuffer.arrayOffset(),
							utf8ByteBuffer.limit());
				}
			}
		}
	}

	private ByteBuffer findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert(
			@Nonnull InputStreamReader encodedStreamReader,
			@Nonnull CharsetEncoder utf8CharsetEncoder, @Nonnull ByteBuffer initialUtf8ByteBuffer,
			@Nullable ClientLineEnding clientLineEnding) throws IOException {

		int limit = initialUtf8ByteBuffer.limit();
		byte[] allUtf8Bytes = Arrays.copyOfRange(initialUtf8ByteBuffer.array(),
				initialUtf8ByteBuffer.arrayOffset(), limit);
		byte lastByte = initialUtf8ByteBuffer.get(limit - 1);
		int offset = findOffsetOfNextClientLineEndingIfReadBytesEndWithFirstByteOfClientLineEnding(
				lastByte, clientLineEnding);

		while (offset > 0) {
			char[] followingPotentialClientLineEndingChars = new char[offset];
			offset = encodedStreamReader.read(followingPotentialClientLineEndingChars);
			if (offset > 0) {
				ByteBuffer moreBuffer = utf8CharsetEncoder.encode(
						CharBuffer.wrap(followingPotentialClientLineEndingChars, 0, offset));
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				outputStream.write(allUtf8Bytes);
				outputStream.write(Arrays.copyOfRange(moreBuffer.array(),
						moreBuffer.arrayOffset(), moreBuffer.limit()));
				allUtf8Bytes = outputStream.toByteArray();

				lastByte = allUtf8Bytes[allUtf8Bytes.length - 1];
				offset = findOffsetOfNextClientLineEndingIfReadBytesEndWithFirstByteOfClientLineEnding(
						lastByte, clientLineEnding);
			}
		}

		return convertToP4dServerEndingsIfRequired(allUtf8Bytes, 0, allUtf8Bytes.length,
				clientLineEnding);
	}

	private int findOffsetOfNextClientLineEndingIfReadBytesEndWithFirstByteOfClientLineEnding(
			byte lastByte, @Nullable ClientLineEnding clientLineEnding) {

		int more = -1;
		if (isRequireConvertClientOrLocalLineEndingToServerFormat(clientLineEnding)) {
			byte[] lineEndBytes = ClientLineEnding.getLineEndBytes(clientLineEnding);
			if (lastByte == lineEndBytes[0]) {
				return lineEndBytes.length - 1;
			}
		}
		return more;
	}

	private boolean isRequireConvertClientOrLocalLineEndingToServerFormat(
			@Nullable ClientLineEnding clientLineEnding) {
		boolean isLocalLineEndingSameAsServerFormat = ClientLineEnding.CONVERT_TEXT;
		if (nonNull(clientLineEnding)) {
			isLocalLineEndingSameAsServerFormat = ClientLineEnding
					.needsLineEndFiltering(clientLineEnding);
		}

		return isLocalLineEndingSameAsServerFormat;
	}

	private ByteBuffer convertToP4dServerEndingsIfRequired(@Nonnull byte[] sourceBytes,
	                                                       final int start, final int length, @Nullable ClientLineEnding clientLineEnding) {
		ByteBuffer convertedByteBuffer;
		if (isRequireConvertClientOrLocalLineEndingToServerFormat(clientLineEnding)) {
			convertedByteBuffer = ByteBuffer.allocate(length);
			byte p4dServerLineEnding = ClientLineEnding.FST_L_LF_BYTES[0];
			byte[] clientLineEndBytes = ClientLineEnding.getLineEndBytes(clientLineEnding);
			for (int i = start; i < length; i++) {
				if (doesSourceBytesUseSameClientLineEnding(sourceBytes, i, length,
						clientLineEndBytes)) {
					convertedByteBuffer.put(p4dServerLineEnding);
					i += clientLineEndBytes.length - 1;
				} else {
					convertedByteBuffer.put(sourceBytes[i]);
				}
			}
			convertedByteBuffer.flip();
		} else {
			convertedByteBuffer = ByteBuffer.wrap(sourceBytes, start, length);
		}
		return convertedByteBuffer;
	}

	private boolean doesSourceBytesUseSameClientLineEnding(@Nonnull byte[] sourceBytes,
	                                                       final int indexOfSourceBytes, final int length, byte[] clientLineEndBytes) {

		boolean isSame = false;
		int potentialLastIndex = indexOfSourceBytes + clientLineEndBytes.length - 1;
		if (potentialLastIndex < length) {
			byte[] subSourceBytes = Arrays.copyOfRange(sourceBytes, indexOfSourceBytes,
					potentialLastIndex + 1);
			isSame = Arrays.equals(subSourceBytes, clientLineEndBytes);
		}

		return isSame;
	}

	private void digestStream(@Nonnull InputStream inStream, boolean isRequireLineEndingConvert,
	                          @Nullable ClientLineEnding clientLineEnding) throws IOException {

		byte[] buffer = new byte[bufferSize];
		int read;
		while ((read = inStream.read(buffer)) > 0) {
			int start = 0;
			if (isRequireLineEndingConvert) {
				ByteBuffer convert = findAndReplaceNonEncodedClientLineEndingIfRequireLineEndingConvert(
						inStream, buffer, read, clientLineEnding);
				update(convert.array(), convert.arrayOffset(), convert.limit());
			} else {
				update(buffer, start, read);
			}
		}
	}

	private ByteBuffer findAndReplaceNonEncodedClientLineEndingIfRequireLineEndingConvert(
			@Nonnull InputStream inStream, @Nonnull final byte[] readBuffer,
			final int totalBytesReadIntoBuffer, @Nullable ClientLineEnding clientLineEnding)
			throws IOException {

		byte lastReadByte = readBuffer[totalBytesReadIntoBuffer - 1];
		byte[] allBytes = Arrays.copyOfRange(readBuffer, 0, totalBytesReadIntoBuffer);
		int length = totalBytesReadIntoBuffer;
		int offset = findOffsetOfNextClientLineEndingIfReadBytesEndWithFirstByteOfClientLineEnding(
				lastReadByte, clientLineEnding);
		while (offset > 0) {
			byte[] potentialClientLineEndingBytes = new byte[offset];
			offset = inStream.read(potentialClientLineEndingBytes);
			if (offset > 0) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				outputStream.write(allBytes);
				outputStream.write(Arrays.copyOfRange(potentialClientLineEndingBytes, 0, offset));
				allBytes = outputStream.toByteArray();
				length = allBytes.length;
				offset = findOffsetOfNextClientLineEndingIfReadBytesEndWithFirstByteOfClientLineEnding(
						allBytes[allBytes.length - 1], clientLineEnding);
			}
		}
		return convertToP4dServerEndingsIfRequired(allBytes, 0, length, clientLineEnding);
	}

	public void update(String str) {
		if (nonNull(str)) {
			try {
				messageDigest.update(str.getBytes(CharsetDefs.UTF8.name()));
			} catch (UnsupportedEncodingException uee) {
				Log.exception(uee);
				throw new P4JavaError(uee);
			}
		}
	}

	public void update(byte[] bytes) {
		if (nonNull(bytes)) {
			messageDigest.update(bytes);
		}
	}

	/**
	 * NOTE: side effects!! It will be removed from next release
	 */
	@Deprecated
	public void update(ByteBuffer byteBuf) {
		if (byteBuf != null) {
			byte[] bytes = new byte[byteBuf.limit()];
			byteBuf.get(bytes);
			update(bytes);
		}
	}
}
