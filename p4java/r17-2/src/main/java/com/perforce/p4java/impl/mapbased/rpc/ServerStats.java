/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc;

import java.util.concurrent.atomic.AtomicLong;

import com.perforce.p4java.Log;

/**
 * Simple class to centralize statistics gathering and reporting for
 * the RPC implementation server objects. Access to these is basically
 * thread-safe because we're using the Atomic series of classes,
 * but in any case the stats gathered here aren't intended to be
 * exact or precise....
 */

public class ServerStats {
	/**
	 * Number of actual socket connections made so far.
	 */
	public AtomicLong serverConnections = new AtomicLong();
	
	public AtomicLong streamSends = new AtomicLong();
	public AtomicLong streamRecvs = new AtomicLong();
	public AtomicLong totalBytesSent = new AtomicLong();
	public AtomicLong totalBytesRecv = new AtomicLong();
	public AtomicLong largestSend = new AtomicLong();
	public AtomicLong largestRecv = new AtomicLong();
	public AtomicLong packetsSent = new AtomicLong();
	public AtomicLong packetsRecv = new AtomicLong();
	public AtomicLong bufferCompacts = new AtomicLong();
	public AtomicLong connectStart = new AtomicLong();
	public AtomicLong largestRpcPacketSent = new AtomicLong();
	public AtomicLong largestRpcPacketRecv = new AtomicLong();
	public AtomicLong sendBufSize = new AtomicLong();
	public AtomicLong recvBufSize = new AtomicLong();
	public AtomicLong incompleteReads = new AtomicLong();
	
	public void clear() {
		this.serverConnections.set(0);
		this.streamSends.set(0);
		this.streamRecvs.set(0);
		this.totalBytesSent.set(0);
		this.totalBytesRecv.set(0);
		this.largestSend.set(0);
		this.largestRecv.set(0);
		this.packetsSent.set(0);
		this.packetsRecv.set(0);
		this.bufferCompacts.set(0);
		this.connectStart.set(0);
		this.largestRpcPacketSent.set(0);
		this.largestRpcPacketRecv.set(0);
		this.sendBufSize.set(0);
		this.recvBufSize.set(0);
		this.incompleteReads.set(0);
	}
	
	public void logStats() {
		Log.stats("RPC server connections made: " + this.serverConnections);
		Log.stats("RPC send ByteBuffer size: " + this.sendBufSize);
		Log.stats("RPC receive ByteBuffer size: " + this.recvBufSize);
		Log.stats("RPC packets sent: " + this.packetsSent
				+ "; RPC packets received: " + this.packetsRecv);
		Log.stats("stream sends: " + this.streamSends
				+ "; stream recvs: " + this.streamRecvs);
		Log.stats("bytes sent: " + this.totalBytesSent + "; bytes received: "
				+ this.totalBytesRecv + " bytes");
		Log.stats("largest RPC packet sent (bytes): " + this.largestRpcPacketSent
				+ "; largest RPC packet receieved (bytes): " + this.largestRpcPacketRecv);
		Log.stats("largest socket send (bytes): " + this.largestSend
				+ "; largest socket recv (bytes): " + this.largestRecv);
		Log.stats("RPC put buffer resizes: " + this.bufferCompacts);
		Log.stats("RPC read buffer incomplete reads: " + this.incompleteReads);
	}
}
