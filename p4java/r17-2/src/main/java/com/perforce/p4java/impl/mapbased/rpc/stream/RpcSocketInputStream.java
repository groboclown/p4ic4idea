/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.mapbased.rpc.ServerStats;

/**
 * Implements the lowest level of the P4Java RPC input socket stream architecture.<p>
 * 
 * This class does the most basic conversion from incoming bytes on the (TCP/IP)
 * wire to a Java IO input stream whose contents are further decoded upstream.<p>
 */

public class RpcSocketInputStream extends InputStream {

	public static final String TRACE_PREFIX = "RpcSocketInputStream";
	
	private Socket socket = null;
	private InputStream socketStream = null;
	private ServerStats stats = null;
	
	/**
	 * Construct a suitable stream for the passed-in socket. No assumptions
	 * are made about the passed-in socket except that a) it's not null, and
	 * b) it's been initialized and set up for reading (or at least the successful
	 * retrieval of a suitable input stream) by the caller.
	 * 
	 * @param socket non-null socket
	 */
	public RpcSocketInputStream(Socket socket, ServerStats stats) {
		super();
		if (socket == null) {
			throw new NullPointerError(
					"null RPC socket passed to RpcSocketInputStream constructor");
		}
		this.socket = socket;
		this.stats = stats;
		try {
			this.socketStream = socket.getInputStream();
		} catch (IOException ioexc) {
			Log.error("Unexpected I/O exception thrown during input stream retrieval"
					+ " in RpcSocketInputStream constructor: " + ioexc.getLocalizedMessage());
			Log.exception(ioexc);
			throw new P4JavaError(
					"Unexpected I/O exception thrown during input stream retrieval"
					+ " in RpcSocketInputStream constructor: " + ioexc.getLocalizedMessage());
		}
	}
	
	@Override
	public int read() throws IOException {
		if (this.socketStream == null) {
			throw new NullPointerError(
					"null socket stream in RpcSocketInputStream.read()");
		}
		
		int retVal = this.socketStream.read();
		
		if ((stats != null) && (stats.largestRecv.get() < retVal)) {
			stats.largestRecv.set(retVal);
		}
		return retVal;
	}
	
	@Override
	public int read(byte[] bytes) throws IOException {
		if (this.socketStream == null) {
			throw new NullPointerError(
					"null socket stream in RpcSocketInputStream.read()");
		}
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array in RpcSocketInputStream.read()");
		}
		int retVal = this.socketStream.read(bytes);
		
		if ((stats != null) && (stats.largestRecv.get() < retVal)) {
			stats.largestRecv.set(retVal);
		}
		return retVal;
	}
	
	@Override
	public int read(byte[] bytes, int offset, int len) throws IOException {
		if (this.socketStream == null) {
			throw new NullPointerError(
					"null socket stream in RpcSocketInputStream.read()");
		}
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array in RpcSocketInputStream.read()");
		}
		int retVal = this.socketStream.read(bytes, offset, len);
		
		if ((stats != null) && (stats.largestRecv.get() < retVal)) {
			stats.largestRecv.set(retVal);
		}
		return retVal;
	}

	protected Socket getSocket() {
		return this.socket;
	}

	protected void setSocket(Socket socket) {
		this.socket = socket;
	}

	protected InputStream getSockStream() {
		return this.socketStream;
	}

	protected void setSockStream(InputStream sockStream) {
		this.socketStream = sockStream;
	}
}
