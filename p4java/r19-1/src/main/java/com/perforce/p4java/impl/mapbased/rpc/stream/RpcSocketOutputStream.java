/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.mapbased.rpc.ServerStats;

/**
 * Implements the lowest level of the P4Java RPC output stream architecture.<p>
 * 
 * This class does the most basic conversion of outgoing bytes to the (TCP/IP)
 * wire from a Java IO output stream whose contents have been encoded upstream.<p>
 */

public class RpcSocketOutputStream extends OutputStream {
	public static final String TRACE_PREFIX = "RpcSocketOutputStream";
	
	private Socket socket = null;
	private OutputStream socketStream = null;
	private ServerStats stats = null;
	
	/**
	 * Construct a suitable stream for the passed-in socket. No assumptions
	 * are made about the passed-in socket except that a) it's not null, and
	 * b) it's been initialized and set up for reading (or at least the successful
	 * retrieval of a suitable input stream) by the caller.
	 * 
	 * @param socket non-null socket
	 */
	public RpcSocketOutputStream(Socket socket, ServerStats stats) {
		super();
		if (socket == null) {
			throw new NullPointerError(
					"null RPC socket passed to RpcSocketInputStream constructor");
		}
		this.socket = socket;
		this.stats = stats;
		try {
			this.socketStream = socket.getOutputStream();
		} catch (IOException ioexc) {
			Log.error("Unexpected I/O exception thrown during output stream retrieval"
					+ " in RpcSocketInputStream constructor: " + ioexc.getLocalizedMessage());
			Log.exception(ioexc);
			throw new P4JavaError(
					"Unexpected I/O exception thrown during output stream retrieval"
					+ " in RpcSocketInputStream constructor: " + ioexc.getLocalizedMessage());
		}
	}

	@Override
	public void write(int b) throws IOException {
		if (this.socketStream == null) {
			throw new NullPointerError(
					"null socket stream in RpcSocketOutputStream.write()");
		}
		this.socketStream.write(b);
	}
	
	@Override
	public void write(byte[] bytes) throws IOException {
		if (this.socketStream == null) {
			throw new NullPointerError(
					"null socket stream in RpcSocketOutputStream.write()");
		}
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array in RpcSocketOutputStream.write()");
		}
		if ((stats != null) && (stats.largestSend.get() < bytes.length)) {
			stats.largestSend.set(bytes.length);
		}
		this.socketStream.write(bytes);
	}
	
	@Override
	public void write(byte[] bytes, int offset, int len) throws IOException {
		if (this.socketStream == null) {
			throw new NullPointerError(
					"null socket stream in RpcSocketOutputStream.write()");
		}
		if (bytes == null) {
			throw new NullPointerError(
					"null byte array in RpcSocketOutputStream.write()");
		}
		if ((stats != null) && (stats.largestSend.get() < len)) {
			stats.largestSend.set(len);
		}
		this.socketStream.write(bytes, offset, len);
	}
	
	protected Socket getSocket() {
		return this.socket;
	}
	protected void setSocket(Socket socket) {
		this.socket = socket;
	}
	protected OutputStream getSocketStream() {
		return this.socketStream;
	}
	protected void setSocketStream(OutputStream socketStream) {
		this.socketStream = socketStream;
	}

}
