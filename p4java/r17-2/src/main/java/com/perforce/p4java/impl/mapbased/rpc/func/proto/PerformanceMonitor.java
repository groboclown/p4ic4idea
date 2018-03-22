/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.proto;

/**
 * Utility class for general RPC performance measuring, monitoring,
 * and reporting. Currently just embryonic... and not ever intended
 * to be precise or exact.
 * 
 *
 */

public class PerformanceMonitor {

	/**
	 * Total number of bytes sent for this connection.
	 */
	public long totalBytesSent = 0;
	
	/**
	 * Number of bytes sent this dispatch
	 */
	public long bytesSentDispatch = 0;
}
