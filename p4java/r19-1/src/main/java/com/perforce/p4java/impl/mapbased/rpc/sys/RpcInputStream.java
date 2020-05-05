/**
 *
 */
package com.perforce.p4java.impl.mapbased.rpc.sys;

import com.perforce.p4java.CharsetConverter;
import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.exception.FileDecoderException;
import com.perforce.p4java.exception.FileEncoderException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import static org.apache.commons.io.ByteOrderMark.UTF_16BE;
import static org.apache.commons.io.ByteOrderMark.UTF_16LE;
import static org.apache.commons.io.ByteOrderMark.UTF_8;

/**
 * Provides a Perforce-specific extension to the basic Java
 * InputStream to allow us to intercept methods and implement
 * our own extensions.<p>
 * <p>
 * The current main use is for line-end processing with the
 * RpcLineEndFilterInputStream filter class; more uses
 * will probably follow with experience....
 */

public class RpcInputStream extends FileInputStream {

	private RpcPerforceFile file = null;
	private RpcPerforceFileType fileType = RpcPerforceFileType.FST_TEXT;
	private InputStream lineEndStream = null;
	private ClientLineEnding lineEnding = null;

	private static boolean isTextType(RpcPerforceFileType fileType) {
		switch (fileType) {
			case FST_TEXT:
			case FST_XTEXT:
			case FST_UNICODE:
			case FST_XUNICODE:
			case FST_UTF16:
			case FST_XUTF16:
			case FST_UTF8:
			case FST_XUTF8:
				return true;
			default:
				return false;
		}
	}

	/*
	 * So here's how this works:
	 *   Start with a file (FileInputStream == this)
	 *   if the file's binary, just super.*
	 *   if the file is text, get a
	 *      FileInputStream
	 *        -> BOMInputStream (optional)
	 *           -> BufferedInputStream
	 *
	 *   if we need to do charset conversion, set up a charset converted stream
	 *
	 *   if we need to do line ending conversion, create a RpcLineEndFilterInputStream
	 */
	public RpcInputStream(RpcPerforceFile file, Charset fromCharset) throws IOException, FileEncoderException {
		super(file);
		if (file == null) {
			throw new NullPointerError(
					"Null RpcPerforceFile passed to RpcInputStream constructor");
		}

		this.file = file;
		this.fileType = this.file.getFileType();
		this.lineEnding = this.file.getLineEnding();

		if (this.lineEnding == null) {
			this.lineEnding = ClientLineEnding.FST_L_LOCAL;
		}

		if (this.fileType == null) {
			this.fileType = RpcPerforceFileType.FST_TEXT;
		}

		if (isTextType(this.fileType)) {

			if (this.fileType == RpcPerforceFileType.FST_TEXT || this.fileType == RpcPerforceFileType.FST_XTEXT) {
				this.lineEndStream = new BufferedInputStream(new FileInputStream(file));
			} else {
				BOMInputStream bis = new BOMInputStream(new FileInputStream(file), UTF_8, UTF_16LE, UTF_16BE);
				if (fromCharset == CharsetDefs.UTF16) {
					fromCharset = bis.hasBOM() ? Charset.forName(bis.getBOMCharsetName())
							: ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ?
							Charset.forName("UTF-16BE") : Charset.forName("UTF-16LE");
				}
				this.lineEndStream = new BufferedInputStream(bis);
			}

			boolean doLineCvt = ClientLineEnding.needsLineEndFiltering(this.lineEnding);
			if (fromCharset != null && fromCharset != CharsetDefs.UTF8) {
				this.lineEndStream = new CharsetConverterStream(lineEndStream, fromCharset, doLineCvt);
			}

			if (doLineCvt) {
				this.lineEndStream = new RpcLineEndFilterInputStream(
						new BufferedInputStream(lineEndStream), this.lineEnding);
			}
		}
	}

	private class CharsetConverterStream extends InputStream {

		InputStream inStream = null;
		CharsetConverter converter = null;

		public CharsetConverterStream(InputStream inStream, Charset charset, boolean lineEndCvt) throws FileEncoderException {
			this.inStream = inStream;

			converter = new CharsetConverter(charset, CharsetDefs.UTF8);
		}

		@Override
		public int read() throws IOException {
			throw new UnimplementedError("RpcInputStream.read()");
		}

		@Override
		public int read(byte[] targetBytes) throws IOException {
			return this.read(targetBytes, 0, targetBytes.length);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {

			byte[] bytes = new byte[len / 2];
			int bytesRead = inStream.read(bytes);

			if (bytesRead <= 0)
				return bytesRead;

			ByteBuffer inBuffer = ByteBuffer.wrap(bytes, 0, bytesRead);
			try {
				ByteBuffer converted = converter.convert(inBuffer);
				byte[] sendBytes = converted.array();
				int pos = converted.position();
				int bytesCopied = converted.limit() - pos;

				System.arraycopy(sendBytes, pos, b, 0, bytesCopied);

				return bytesCopied;
			} catch (FileDecoderException e) {
				return -1;
			} catch (FileEncoderException e) {
				return -1;
			}
		}

		@Override
		public void close() throws IOException {
			if (this.inStream != null) {
				this.inStream.close();
			}
			super.close();
		}
	}

	@Override
	public void close() throws IOException {
		if (this.lineEndStream != null) {
			this.lineEndStream.close();
		}
		super.close();
	}

	@Override
	public int read() throws IOException {
		throw new UnimplementedError("RpcInputStream.read()");
	}

	@Override
	public int read(byte[] targetBytes, int targetOffset, int targetLen) throws IOException {
		if (targetBytes == null) {
			throw new NullPointerError("Null target byte array in RpcInputStream.read()");
		}
		if (targetOffset < 0) {
			throw new P4JavaError("Negative target offset in RpcInputStream.read()");
		}
		if (targetLen < 0) {
			throw new P4JavaError("Negative target length in RpcInputStream.read()");
		}
		if (targetBytes.length < targetLen) {
			throw new P4JavaError("Length of <targetBytes> must greater or "
					+ "equal than <targetLen> in RpcInputStream.read()");
		}

		if (this.lineEndStream != null) {
			return this.lineEndStream.read(targetBytes, targetOffset, targetLen);
		}
		return super.read(targetBytes, targetOffset, targetLen);
	}

	@Override
	public int read(byte[] targetBytes) throws IOException {
		return this.read(targetBytes, 0, targetBytes.length);
	}
}
