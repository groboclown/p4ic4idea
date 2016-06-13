package com.perforce.p4java.impl.mapbased.rpc.sys;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * InputStream wrapper that detects and skips the Unicode BOM (Byte Order Mark)
 * in Unicode encoded text files.
 * 
 * <pre>
 * EF BB BF    = UTF-8 BOM
 * FF FE       = UTF-16, little-endian BOM
 * FE FF       = UTF-16, big-endian BOM
 * FF FE 00 00 = UTF-32, little-endian BOM
 * 00 00 FE FF = UTF-32, big-endian BOM
 * </pre>
 */
public class RpcUnicodeInputStream extends InputStream {

	private final PushbackInputStream in;
	private final BOM bom;
	private boolean skipped = false;

	/**
	 * Types of Unicode BOMs.
	 */
	public static final class BOM {

		final byte bytes[];
		private final String description;

		private BOM(final byte bom[], final String description) {
			this.bytes = bom;
			this.description = description;
		}

		/**
		 * Default to no BOM.
		 */
		public static final BOM NONE = new BOM(new byte[] {}, "NONE");

		/**
		 * UTF-8 BOM (EF BB BF).
		 */
		public static final BOM UTF_8 = new BOM(new byte[] {
				(byte) 0xEF,
				(byte) 0xBB,
				(byte) 0xBF }, "UTF-8");

		/**
		 * UTF-16, little-endian (FF FE).
		 */
		public static final BOM UTF_16_LE = new BOM(new byte[] {
				(byte) 0xFF,
				(byte) 0xFE }, "UTF-16 little-endian");

		/**
		 * UTF-16, big-endian (FE FF).
		 */
		public static final BOM UTF_16_BE = new BOM(new byte[] {
				(byte) 0xFE,
				(byte) 0xFF }, "UTF-16 big-endian");

		/**
		 * UTF-32, little-endian (FF FE 00 00).
		 */
		public static final BOM UTF_32_LE = new BOM(new byte[] {
				(byte) 0xFF,
				(byte) 0xFE,
				(byte) 0x00,
				(byte) 0x00 }, "UTF-32 little-endian");

		/**
		 * UTF-32, big-endian (00 00 FE FF).
		 */
		public static final BOM UTF_32_BE = new BOM(new byte[] {
				(byte) 0x00,
				(byte) 0x00,
				(byte) 0xFE,
				(byte) 0xFF }, "UTF-32 big-endian");

		/**
		 * Returns the string representation of this BOM.
		 */
		public final String toString() {
			return description;
		}

		/**
		 * Returns the bytes of this BOM.
		 */
		public final byte[] getBytes() {
			final int length = bytes.length;
			final byte[] result = new byte[length];
			System.arraycopy(bytes, 0, result, 0, length);
			return result;
		}
	}

	/**
	 * Constructs a new RpcUnicodeInputStream that wraps the InputStream.
	 */
	public RpcUnicodeInputStream(final InputStream inputStream) throws NullPointerException, IOException {
		if (inputStream == null) {
			throw new NullPointerException("Null inputstream passed to the RpcUnicodeInputStream constructor.");
		}

		in = new PushbackInputStream(inputStream, 4);

		final byte bom[] = new byte[4];
		final int read = in.read(bom);

		switch (read) {
		
		case 4:
			if ((bom[0] == (byte) 0xFF)
					&& (bom[1] == (byte) 0xFE)
					&& (bom[2] == (byte) 0x00)
					&& (bom[3] == (byte) 0x00)) {
				this.bom = BOM.UTF_32_LE;
				break;
			} else if ((bom[0] == (byte) 0x00)
					&& (bom[1] == (byte) 0x00)
					&& (bom[2] == (byte) 0xFE)
					&& (bom[3] == (byte) 0xFF)) {
				this.bom = BOM.UTF_32_BE;
				break;
			}

		case 3:
			if ((bom[0] == (byte) 0xEF)
					&& (bom[1] == (byte) 0xBB)
					&& (bom[2] == (byte) 0xBF)) {
				this.bom = BOM.UTF_8;
				break;
			}

		case 2:
			if ((bom[0] == (byte) 0xFF)
					&& (bom[1] == (byte) 0xFE)) {
				this.bom = BOM.UTF_16_LE;
				break;
			} else if ((bom[0] == (byte) 0xFE)
					&& (bom[1] == (byte) 0xFF)) {
				this.bom = BOM.UTF_16_BE;
				break;
			}

		default:
			this.bom = BOM.NONE;
			break;
		}

		if (read > 0)
			in.unread(bom, 0, read);
	}

	/**
	 * Returns the BOM found in the InputStream.
	 */
	public final BOM getBOM() {
		return bom;
	}

	/**
	 * Skips the BOM found in the InputStream.
	 */
	public final synchronized RpcUnicodeInputStream skipBOM() throws IOException {
		if (!skipped) {
			in.skip(bom.bytes.length);
			skipped = true;
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public int read() throws IOException {
		return in.read();
	}

	/**
	 * {@inheritDoc}
	 */
	public int read(final byte b[]) throws IOException, NullPointerException {
		return in.read(b, 0, b.length);
	}

	/**
	 * {@inheritDoc}
	 */
	public int read(final byte b[], final int off, final int len)
			throws IOException, NullPointerException {
		return in.read(b, off, len);
	}

	/**
	 * {@inheritDoc}
	 */
	public long skip(final long n) throws IOException {
		return in.skip(n);
	}

	/**
	 * {@inheritDoc}
	 */
	public int available() throws IOException {
		return in.available();
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() throws IOException {
		in.close();
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void mark(final int readlimit) {
		in.mark(readlimit);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void reset() throws IOException {
		in.reset();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean markSupported() {
		return in.markSupported();
	}
}