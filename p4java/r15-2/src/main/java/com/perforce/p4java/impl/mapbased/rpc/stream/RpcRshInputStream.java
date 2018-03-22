/*
 * Copyright 2014 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import java.io.IOException;
import java.io.InputStream;

import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.impl.mapbased.rpc.ServerStats;

/**
 * Implements the lowest level of the P4Java RPC 'rsh' input stream architecture.<p>
 * 
 * This class does the most basic conversion from incoming bytes on the (TCP/IP)
 * wire to a Java IO input stream whose contents are further decoded upstream.<p>
 */

public class RpcRshInputStream extends InputStream {

	public static final String TRACE_PREFIX = "RpcRshInputStream";
	
	private InputStream rshStream = null;
	private ServerStats stats = null;
	
	/**
	 * Construct a suitable stream for the passed-in inputstream. No assumptions
	 * are made about the passed-in inputstream except that a) it's not null, and
	 * b) it's been initialized and set up for reading by the caller.
	 * 
	 * @param inputstream non-null InputStream
	 * @param stats non-null ServerStats
	 */
	public RpcRshInputStream(InputStream inputstream, ServerStats stats) {
		super();
		if (inputstream == null) {
			throw new NullPointerError(
					"null inputstream passed to RpcRshInputStream constructor");
		}
		this.stats = stats;
		this.rshStream = inputstream;
	}
	
	@Override
	public int read() throws IOException {
		if (this.rshStream == null) {
			throw new NullPointerError(
					"null socket stream in RpcRshInputStream.read()");
		}
		
		int retVal = this.rshStream.read();
		
		if ((stats != null) && (stats.largestRecv.get() < retVal)) {
			stats.largestRecv.set(retVal);
		}
		return retVal;
	}
	
	@Override
	public int read(byte[] bytes) throws IOException {
		if (this.rshStream == null) {
			throw new NullPointerError(
					"null socket stream in RpcRshInputStream.read()");
		}
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array in RpcRshInputStream.read()");
		}
		int retVal = this.rshStream.read(bytes);
		
		if ((stats != null) && (stats.largestRecv.get() < retVal)) {
			stats.largestRecv.set(retVal);
		}
		return retVal;
	}
	
	@Override
	public int read(byte[] bytes, int offset, int len) throws IOException {
		if (this.rshStream == null) {
			throw new NullPointerError(
					"null rsh stream in RpcRshInputStream.read()");
		}
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array in RpcRshInputStream.read()");
		}
		int retVal = this.rshStream.read(bytes, offset, len);
		
		if ((stats != null) && (stats.largestRecv.get() < retVal)) {
			stats.largestRecv.set(retVal);
		}
		return retVal;
	}

    @Override
    public long skip(long n) throws IOException {
        return this.rshStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return this.rshStream.available();
    }

    @Override
    public void close() throws IOException {
        this.rshStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
    	this.rshStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
    	this.rshStream.reset();
    }

    @Override
    public boolean markSupported() {
        return rshStream.markSupported();
    }

    @Override
    public String toString() {
        return "RpcRshInputStream[" + this.rshStream + ']';
    }	
	
	protected InputStream getRshStream() {
		return this.rshStream;
	}

	protected void setRshStream(InputStream rshStream) {
		this.rshStream = rshStream;
	}
}
