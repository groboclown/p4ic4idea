/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import com.jcraft.jzlib.ZInputStream;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.UnimplementedError;

/**
 * A lightweight wrapper around the JZlib GZIP input stream for
 * processing compressed streams being sent from Perforce servers
 * when the client "client compress" mode is enabled.<p>
 * 
 * Note that the Perforce version of the GZIP stream dispenses with
 * headers and trailers, but is otherwise fairly standard (which is why
 * this works).
 */

public class RpcGZIPInputStream extends InflaterInputStream {
	
	private ZInputStream jzInStream = null;
	
	public RpcGZIPInputStream(InputStream in) throws IOException {
		super(in);
		this.jzInStream = new ZInputStream(in, true);
	}
	
	@Override
	public int read(byte[] bytes) throws IOException {
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array passed to RpcGZIPInputStream.read()");
		}
		return read(bytes, 0, bytes.length);
	}
	
	@Override
	public int read(byte[] bytes, int offset, int len) throws IOException {
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array passed to RpcGZIPInputStream.read()");
		}
		if ((len <= 0) || (offset < 0) || (offset >= bytes.length) || (len > (bytes.length - offset))) {
			throw new P4JavaError(
					"bad length or offset in RpcGZIPInputStream.read()");
		}
		return this.jzInStream.read(bytes, offset, len);
	}
	
	@Override
	public int read() throws IOException {
		// NOTE: exception is here out of curiosity -- this shouldn't be
		// called from anywhere -- HR.
		
		throw new UnimplementedError("single-byte RpcGZIPInputStream.read()");
	}
	
	@Override
	public void close() throws IOException {
		this.jzInStream.close();
	}
}
