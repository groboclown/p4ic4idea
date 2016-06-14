/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcUnicodeInputStream;

/**
 * Provide MD5 digest methods for the rest of the RPC implementation. Basically
 * just a wrapper around the normal Java stuff, with a useful added method to
 * finalise the digest as a hex string.<p>
 * 
 *
 */

public class MD5Digester {
	
	public static final String DIGEST_TYPE = "MD5";
	
	private MessageDigest md = null;
	
	public MD5Digester() {
		try {
			md = MessageDigest.getInstance(DIGEST_TYPE);
			md.reset();
		} catch (NoSuchAlgorithmException exc) {
			throw new P4JavaError("Unable to create an MD5 digester for P4Java: "
									+ exc.getLocalizedMessage());
		}
	}
	
	public void reset() {
		md.reset();
	}
	
	public void update(String str) {
		if (str != null) {
			try {
				md.update(str.getBytes(CharsetDefs.UTF8.name()));
			} catch (UnsupportedEncodingException uee) {
				Log.exception(uee);
				throw new P4JavaError(uee);
			}
		}
	}
	
	public void update(byte[] bytes) {
		if (bytes != null) {
			md.update(bytes);
		}
	}
	
	public void update(byte[] bytes, int off, int len) {
		if (bytes != null) {
			md.update(bytes, off, len);
		}
	}
	
	/**
	 * NOTE: side effects!!
	 */
	public void update(ByteBuffer byteBuf) {
		if (byteBuf != null) {
			byte[] bytes = new byte[byteBuf.limit()];
			byteBuf.get(bytes);
			update(bytes);
		}
	}
	
	public byte[] digestAsBytes() {
		return md.digest();
	}
	
	/**
	 * Return the finalised digest as a 32 byte hex string. It's important
	 * elsewhere and in the server that the string be exactly 32 bytes long,
	 * so we stitch it up if possible to make it that long...
	 */
	
	public String digestAs32ByteHex() {
		String retStr = new BigInteger(1, md.digest()).toString(16).toUpperCase();
		
		if (retStr.length() != 32) {
			// Usually a sign that we need to add leading zeroes...
			
			if ((retStr.length() > 0) && (retStr.length() < 32)) {
				for (int i = retStr.length(); i < 32; i++) {
			
					// What a hack!
					
					retStr = "0" + retStr;
				}
			} else {
				// Panicable offense...
				
				throw new P4JavaError(
						"Bad 32 byte digest string size in MD5Digester.digestAs32ByteHex; string: "
						+ retStr + "; length: " + retStr.length());
			}
		}
		
		return retStr;
	}
	
	private ByteBuffer convertEndings(byte[] sourceBytes, int start, int length,
			ClientLineEnding clientLineEnding) {
		ByteBuffer converted = null;
		boolean convertText = (clientLineEnding == null ?
				ClientLineEnding.CONVERT_TEXT :
					ClientLineEnding.needsLineEndFiltering(clientLineEnding));
		if (convertText) {
			converted = ByteBuffer.allocate(length);
			byte replace = ClientLineEnding.FST_L_LF_BYTES[0];
			byte[] convert = ClientLineEnding.getLineEndBytes(clientLineEnding);
			for (int i = start; i < length; i++) {
				boolean fix = false;
				if (sourceBytes[i] == convert[0] && i + convert.length -1 < length) {
					fix = true;
					for (int c = 1; c < convert.length; c++) {
						if (sourceBytes[i + c] != convert[c]) {
							fix = false;
							break;
						}
					}
				}
				if (!fix) {
					converted.put(sourceBytes[i]);
				} else {
					converted.put(replace);
					i += convert.length - 1;
				}
			}
			converted.flip();
		} else {
			converted = ByteBuffer.wrap(sourceBytes, start, length);
		}
		return converted;
	}
	
	private int readMore(byte lastByte, ClientLineEnding clientLineEnding) {
		int more = -1;
		boolean convertText = (clientLineEnding == null ?
				ClientLineEnding.CONVERT_TEXT :
					ClientLineEnding.needsLineEndFiltering(clientLineEnding));
		if (convertText) {
			byte[] lineEndBytes = ClientLineEnding.getLineEndBytes(clientLineEnding);
			if (lastByte == lineEndBytes[0]) {
				return lineEndBytes.length - 1;
			}
		}
		return more;
	}
	
	private void digestStream(InputStream inStream, boolean convertLineEndings, ClientLineEnding clientLineEnding,
			long fileSizeBytes) throws IOException {
		byte[] sourceBytes = new byte[(int) fileSizeBytes];
		int inBytesRead = 0;

		while ((inBytesRead = inStream.read(sourceBytes)) > 0) {
			int start = 0;
			int len = inBytesRead;
			if (convertLineEndings) {
				byte[] allBytes = sourceBytes;
				int more = readMore(allBytes[inBytesRead - 1],
						clientLineEnding);
				while (more > 0) {
					byte[] moreBytes = new byte[more];
					more = inStream.read(moreBytes);
					if (more > 0) {
						byte[] joinedBytes = new byte[allBytes.length + more];
						System.arraycopy(allBytes, 0, joinedBytes, 0,
								inBytesRead);
						System.arraycopy(moreBytes, 0, joinedBytes,
								inBytesRead, moreBytes.length);
						allBytes = joinedBytes;
						len = allBytes.length;
						more = readMore(allBytes[allBytes.length - 1],
								clientLineEnding);
					}
				}
				ByteBuffer convert = convertEndings(allBytes, start, len,
						clientLineEnding);
				sourceBytes = convert.array();
				start = convert.arrayOffset();
				len = convert.limit();
			}
			update(sourceBytes, start, len);
		}
	}
	
	private void digestEncodedStream(InputStream inStream, Charset charset, boolean convertLineEndings,
			ClientLineEnding clientLineEnding, long fileSizeBytes) throws CharacterCodingException, IOException {
		// Unicode inputstream
		RpcUnicodeInputStream unicodeInputStream = new RpcUnicodeInputStream(inStream);

		// Read file content using P4CHARSET
		InputStreamReader reader = new InputStreamReader(unicodeInputStream, charset);

		try {
			char[] inBytes = new char[(int) fileSizeBytes];
			int inBytesRead = 0;

			// Create encoder that reports malformed/unmappable values
			CharsetEncoder encoder = CharsetDefs.UTF8.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);

			// Skip Unicode BOM, if any.
			unicodeInputStream.skipBOM();

			while ((inBytesRead = reader.read(inBytes)) > 0) {
				// Convert to UTF8 since server digest is UTF8
				ByteBuffer byteBuffer = encoder.encode(CharBuffer.wrap(inBytes,
						0, inBytesRead));

				if (convertLineEndings) {
					int more = readMore(byteBuffer.get(byteBuffer.limit() - 1),
							clientLineEnding);
					while (more > 0) {
						char[] moreChars = new char[more];
						more = reader.read(moreChars);
						if (more > 0) {
							ByteBuffer moreBuffer = encoder.encode(CharBuffer
									.wrap(moreChars, 0, more));
							byteBuffer.limit(byteBuffer.limit()
									+ moreBuffer.limit());
							byteBuffer.put(moreBuffer);
							more = readMore(byteBuffer
									.get(byteBuffer.limit() - 1),
									clientLineEnding);
						}
					}
				}

				byte[] sourceBytes = byteBuffer.array();
				int start = byteBuffer.arrayOffset();
				int len = byteBuffer.limit();

				if (convertLineEndings) {
					ByteBuffer convert = convertEndings(sourceBytes, start, len,
							clientLineEnding);
					sourceBytes = convert.array();
					start = convert.arrayOffset();
					len = convert.limit();
				}
				update(sourceBytes, start, len);
			}
		} finally {
			reader.close();
			unicodeInputStream.close();
		}
	}
	
	/**
	 * Return the results of digesting an arbitrary file with this digester.<p>
	 * 
	 * Returns null if it can't read or digest the file for whatever reason;
	 * otherwise the finalized digest is returned as a 32 byte hex string.
	 * 
	 * @param file 
	 * @param charset 
	 * @return - computed digest or null if computation failed
	 */
	public String digestFileAs32ByteHex(File file, Charset charset) {
		return digestFileAs32ByteHex(file, charset, false);
	}

	/**
	 * Return the results of digesting an arbitrary file with this digester.<p>
	 * 
	 * Returns null if it can't read or digest the file for whatever reason;
	 * otherwise the finalized digest is returned as a 32 byte hex string.
	 * 
	 * @param file 
	 * @param charset 
	 * @param convertLineEndings 
	 * @return - computed digest or null if computation failed
	 */
	public String digestFileAs32ByteHex(File file, Charset charset,
			boolean convertLineEndings) {
		return digestFileAs32ByteHex(file, charset, convertLineEndings, null);
	}

	/**
	 * Return the results of digesting an arbitrary file with this digester and
	 * a specific client line ending.<p>
	 * 
	 * Returns null if it can't read or digest the file for whatever reason;
	 * otherwise the finalized digest is returned as a 32 byte hex string.
	 * 
	 * @param file 
	 * @param charset 
	 * @param convertLineEndings 
	 * @param clientLineEnding 
	 * @return - computed digest or null if computation failed
	 */
	public String digestFileAs32ByteHex(File file, Charset charset,
			boolean convertLineEndings, ClientLineEnding clientLineEnding) {
		if (file == null) {
			throw new NullPointerError(
					"Null file passed to MD5Digester.digestFileAs32ByteHex()");
		}
		FileInputStream inStream = null;
		try {
			if (file.exists() && file.canRead()) {
				// Get file size in bytes
				long fileSizeInBytes = file.length();
				
				inStream = new FileInputStream(file);
				if (inStream != null) {
					this.reset();
					if (charset != null) {
						digestEncodedStream(inStream, charset,
								convertLineEndings, clientLineEnding, fileSizeInBytes);
					} else {
						digestStream(inStream, convertLineEndings, clientLineEnding, fileSizeInBytes);
					}

					return digestAs32ByteHex();
				}
			}
		} catch (CharacterCodingException mie) {
			Log.error("error digesting file: " + file.getPath()
					+ "; exception follows...");
			Log.exception(mie);
		} catch (IOException ioexc) {
			Log.error("error digesting file: " + file.getPath()
					+ "; exception follows...");
			Log.exception(ioexc);
		} finally {
			try {
				if (inStream != null) {
					inStream.close();
				}
			} catch (IOException ioexc) {
				Log.warn("file inputstream close error in MD5Digester.digestFileAs32ByteHex(): "
						+ ioexc.getLocalizedMessage());
			}
		}

		return null;
	}
	
	/**
	 * Return the results of digesting an arbitrary file with this digester.<p>
	 * 
	 * Returns null if it can't read or digest the file for whatever reason;
	 * otherwise the finalized digest is returned as a 32 byte hex string.
	 * 
	 * @param file 
	 * @return - computed digest or null if computation failed
	 */
	public String digestFileAs32ByteHex(File file) {
		if (file == null) {
			throw new NullPointerError(
					"Null file passed to MD5Digester.digestFileAs32ByteHex()");
		}
		FileInputStream inStream = null;
		try {
			if (file.exists() && file.canRead()) {
				// Get file size in bytes
				long fileSizeInBytes = file.length();

				inStream = new FileInputStream(file);
				if (inStream != null) {
					this.reset();
					byte[] inBytes = new byte[(int) fileSizeInBytes];
					int inBytesRead = 0;
					
					while ((inBytesRead = inStream.read(inBytes)) > 0) {
						update(inBytes, 0, inBytesRead);
					}
					
					return digestAs32ByteHex();
				}
			}
		} catch (IOException ioexc) {
			Log.error("error digesting file: " + file.getPath()
									+ "; exception follows...");
			Log.exception(ioexc);
		} finally {
			try {
				if (inStream != null) {
					inStream.close();
				}
			} catch (IOException ioexc) {
				Log.warn("file inputstream close error in MD5Digester.digestFileAs32ByteHex(): "
						+ ioexc.getLocalizedMessage());
			}
		}
		
		return null;
	}
}
