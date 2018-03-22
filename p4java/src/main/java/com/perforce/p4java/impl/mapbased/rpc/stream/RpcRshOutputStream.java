/*
 * Copyright 2014 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import java.io.IOException;
import java.io.OutputStream;

import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.impl.mapbased.rpc.ServerStats;

/**
 * Implements the lowest level of the P4Java RPC 'rsh' output stream architecture.<p>
 * 
 * This class does the most basic conversion of outgoing bytes to the (TCP/IP)
 * wire from a Java IO output stream whose contents have been encoded upstream.<p>
 */

public class RpcRshOutputStream extends OutputStream {
	public static final String TRACE_PREFIX = "RpcRshOutputStream";
	
	private OutputStream rshStream = null;
	private ServerStats stats = null;
	
	/**
	 * Construct a suitable stream for the passed-in inputstream. No assumptions
	 * are made about the passed-in inputstream except that a) it's not null, and
	 * b) it's been initialized and set up for reading by the caller.
	 * 
	 * @param outputstream non-null OutputStream
	 * @param stats non-null ServerStats
	 */
	public RpcRshOutputStream(OutputStream outputstream, ServerStats stats) {
		super();
		if (outputstream == null) {
			throw new NullPointerError(
					"null RPC outputstream passed to RpcRshOutputStream constructor");
		}
		this.stats = stats;
		this.rshStream = outputstream;
	}

	@Override
	public void write(int b) throws IOException {
		if (this.rshStream == null) {
			throw new NullPointerError(
					"null rsh stream in RpcRshOutputStream.write()");
		}
		this.rshStream.write(b);
	}
	
	@Override
	public void write(byte[] bytes) throws IOException {
		if (this.rshStream == null) {
			throw new NullPointerError(
					"null rsh stream in RpcRshOutputStream.write()");
		}
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array in RpcRshOutputStream.write()");
		}
		if ((stats != null) && (stats.largestSend.get() < bytes.length)) {
			stats.largestSend.set(bytes.length);
		}
		this.rshStream.write(bytes);
	}
	
	@Override
	public void write(byte[] bytes, int offset, int len) throws IOException {
		if (this.rshStream == null) {
			throw new NullPointerError(
					"null rsh stream in RpcRshOutputStream.write()");
		}
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array in RpcRshOutputStream.write()");
		}
		if ((stats != null) && (stats.largestSend.get() < len)) {
			stats.largestSend.set(len);
		}
		this.rshStream.write(bytes, offset, len);
	}

    @Override
    public void flush() throws IOException {
        this.rshStream.flush();
    }

    @Override
    public void close() throws IOException {
        this.rshStream.close();
    }

    @Override
    public String toString() {
        return "RpcRshOutputStream[" + this.rshStream + ']';
    }

	protected OutputStream getRshStream() {
		return this.rshStream;
	}

	protected void setRshStream(OutputStream rshStream) {
		this.rshStream = rshStream;
	}

}
