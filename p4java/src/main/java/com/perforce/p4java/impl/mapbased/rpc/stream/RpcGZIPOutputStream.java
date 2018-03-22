/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.UnimplementedError;


/**
 * A fairly lightweight filter output stream that implements Perforce's
 * GZIP-based connection stream compression for Perforce clients that have
 * the Perforce "client compression" option set.<p>
 * 
 * The implementation here uses the JZlib package because the standard
 * JDK GZIP packages in java.util.zip are not able to cope cleanly with
 * the various flush options needed on a streaming connection like this.
 * Our use of the JZlib package is pretty boring, and is basically just a
 * transliteration of the original C++ API code that dates from about 1999
 * or so. The implementation here is not thread safe in that there's a
 * buffer in each instantiation of this stream; this could probably be
 * tightened up if there's a need for it.<p>
 * 
 * Note that this implementation requires the upper levels to ensure that
 * the stream is flushed properly with the flush() method whenever an RPC
 * packet is ready for sending; this ensures that GZIP block processing,
 * etc., is done properly and the server sees correct block and stream
 * boundaries. If this isn't done, the server may hang or you may see some
 * very odd client-side errors. The stream should also be closed properly, but that's
 * less of an issue.<p> 
 * 
 * Note that there's quite a performance penalty for using connection (a.k.a.
 * client) compression (especially on the server), but if that's what the customer
 * wants, that what the customer gets.
 */

public class RpcGZIPOutputStream extends FilterOutputStream {

	private static final int ZBITS = 15;	// Don't change this, it's fundamental...
	private static final int ZBUF_SIZE = 10240;	// Might want to play with this a bit...
	
	private ZStream jzOutputSream = null;
	private byte[] jzBytes = null;
	
	public RpcGZIPOutputStream(OutputStream out) throws IOException {
		super(out);
		this.jzOutputSream = new ZStream();
		this.jzOutputSream.deflateInit(JZlib.Z_DEFAULT_COMPRESSION, ZBITS, true);
		this.jzBytes = new byte[ZBUF_SIZE];
		this.jzOutputSream.next_out = this.jzBytes;
		this.jzOutputSream.next_out_index = 0;
		this.jzOutputSream.avail_out = this.jzBytes.length;
	}
	
	/**
	 * A convenience method for write(bytes, 0, bytes.length).
	 * 
	 * @see java.io.FilterOutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] bytes) throws IOException {
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array passed to RpcGZIPOutputStream.write()");
		}
		write(bytes, 0, bytes.length);
	}
	
	/**
	 * Deflate (compress) the passed-in bytes and -- if appropriate --
	 * send the compressed bytes downstream to the filter's output stream.<p>
	 * 
	 * This write method does not necessarily cause a write to the
	 * server -- a write will only occur when the jzBytes buffer
	 * is full, or on a later flush. This is a consequence of the
	 * way GZIP streaming works here, and means you must ensure that
	 * a suitable flush is done at a suitable (packet) boundary. See
	 * the comments for flush() below.
	 * 
	 * @see java.io.FilterOutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] bytes, int offset, int len) throws IOException {
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array passed to RpcGZIPOutputStream.write()");
		}
		if ((len <= 0) || (offset < 0) || (offset >= bytes.length) || (len > (bytes.length - offset))) {
			throw new P4JavaError(
					"bad length or offset in RpcGZIPOutputStream.write()");
		}
		
		this.jzOutputSream.next_in = bytes;
		this.jzOutputSream.avail_in = len;
		this.jzOutputSream.next_in_index = offset;

		while (this.jzOutputSream.avail_in != 0) {
			if (this.jzOutputSream.avail_out == 0) {
				this.out.write(this.jzBytes);
				this.jzOutputSream.next_out = this.jzBytes; // redundant, but safe...
				this.jzOutputSream.avail_out = this.jzBytes.length;
				this.jzOutputSream.next_out_index = 0;
			}
			
			int jzErr = this.jzOutputSream.deflate(JZlib.Z_NO_FLUSH);
			
			if (jzErr != JZlib.Z_OK) {
				throw new IOException("connection compression error: "
						+ getJZlibErrorStr(jzErr));
			}
		}
	}
	
	/**
	 * Not used. Will cause a UnimplementedError to be thrown
	 * if called.
	 * 
	 * @see java.io.FilterOutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		// NOTE: exception is here out of curiosity -- this shouldn't be
		// called from anywhere -- HR.
		
		throw new UnimplementedError("single-byte RpcGZIPOutputStream.write()");
	}
	
	/**
	 * Flush the results of previous byte deflation (compression) downstream.<p>
	 * 
	 * As a consequence of the way GZIP streaming works, this flush is often the only
	 * place where bytes are actually written downstream towards the server (the earlier
	 * writes may only write to the internal buffer here). Using flush causes a compression
	 * boundary, so it should only be used after a complete packet has been put onto
	 * this stream -- i.e. users of this stream must call flush appropriately, or the
	 * server may not see packets at all.
	 * 
	 * @see java.io.FilterOutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
		this.jzOutputSream.avail_in = 0;
		
		boolean done = false;
		while (true) {
			if ((this.jzOutputSream.avail_out == 0) || done) {
				out.write(this.jzBytes, 0, this.jzBytes.length - this.jzOutputSream.avail_out);
				this.jzOutputSream.next_out = this.jzBytes;
				this.jzOutputSream.avail_out = this.jzBytes.length;
				this.jzOutputSream.next_out_index = 0;
			}
			
			if (done) {
				break;
			}
			
			int jzErr = this.jzOutputSream.deflate(JZlib.Z_FULL_FLUSH);
			if (jzErr != JZlib.Z_OK) {
				throw new IOException("Perforce connection compression error: "
						+ getJZlibErrorStr(jzErr));
			}
			
			if (this.jzOutputSream.avail_out != 0) {
				done = true;
			}
		}
	}
	
	/**
	 * Cleanly close the stream and finalize deflation (compression). No one dies
	 * if you don't call this properly, but it certainly helps to close the
	 * stream cleanly.
	 * 
	 * @see java.io.FilterOutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		this.jzOutputSream.deflateEnd();
	}
	
	/**
	 * Provide a more human-readable form of the underlying JZlib compression errors.<p>
	 * 
	 * Should be made even more human-readable sometime later -- HR.
	 */
	protected String getJZlibErrorStr(int errNum) {
		switch (errNum) {
			case JZlib.Z_STREAM_ERROR:
				return "stream error";
			case JZlib.Z_DATA_ERROR:
				return "data error";
			case JZlib.Z_MEM_ERROR:
				return "memory error";
			case JZlib.Z_BUF_ERROR:
				return "buffer error";
			case JZlib.Z_VERSION_ERROR:
				return "version error";
			default:
				return "unknown error";
		}
	}
}
