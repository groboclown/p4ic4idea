/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.proto;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher;

/**
 * Process and implement the various Perforce flow control commands,
 * in particular the flush1 / flush2 pair.<p>
 * 
 * In general, we keep a count of outstanding bytes and send
 * a flush1 when it gets above the high water mark (hwm); we
 * then wait for a corresponding flush2 before doing much else.
 * The server does the same thing in reverse. This will all be
 * explained in more detail later when the full implementation
 * is much clearer... (HR).
 * 
 *
 */

public class FlowControl {
	
	public static final String TRACE_PREFIX = "FlowControl";
	
	/**
	 * Length in bytes of a flush command when marshaled. This is actually
	 * a fiction -- it's more like 50 -- but the conservative sizing gives
	 * us a little leeway when things are tight (see the C++ API for a
	 * discussion on this -- we're just copying their behavior...).
	 */
	public static final int FLUSH_CMD_LENGTH = 60;

	/**
	 * Default maximum number of bytes allowed to be outstanding before we send
	 * a flush1 message on duplex commands. See the flow control documentation
	 * for an explanation of this and associated gubbins.
	 */
	public static final int DEFAULT_LO_MARK = 700;
	
	/**
	 * Default maximum number of bytes allowed to be outstanding before we start
	 * worrying that we haven't seen a suitable flush2 response.
	 * See the flow control documentation for an explanation of this and
	 * associated gubbins.
	 */
	public static final long DEFAULT_HI_MARK = 2000;
	
	private long loMark = DEFAULT_LO_MARK;	// flow control low mark as calculated at init time
	private long hiMark = DEFAULT_HI_MARK;	// flow control high mark as calculated at init time
	
	private long currentLoMark = 0;	// bytes outstanding going to server
	private long currentHiMark = 0;	// bytes outstanding coming from server without flush2
	
	@SuppressWarnings("unused") // used for debugging
	private RpcPacketDispatcher mainDispatcher = null;
	@SuppressWarnings("unused") // used for debugging
	private Properties props = null;
	
	public FlowControl(RpcPacketDispatcher mainDispatcher, Properties props) {
		this.mainDispatcher = mainDispatcher;
		this.props = props;
	}
	
	/**
	 * Given a map passed-in from the main dispatcher that represents
	 * a flush command received from the Perforce server, respond
	 * appropriately. What "appropriately" means is very context-dependent;
	 * in general, if we see a flush1 we immediately attempt to send a
	 * flush2 with the same sequence number. If we see a flush2 we try
	 * to match it up with any outstanding flush1's we've already sent.
	 * This can get arbitrarily complex...
	 */
	
	public void processFlushCommandFromServer(RpcConnection rpcConnection,
					RpcFunctionSpec funcSpec, Map<String, Object> resultsMap)
														throws ConnectionException {
		if (rpcConnection == null) {
			throw new NullPointerError(
				"Null rpc connection passed to FlowControl.processFlushCommandFromServer()");
		}
		if (resultsMap == null) {
			throw new NullPointerError(
				"Null results map passed to FlowControl.processFlushCommandFromServer()");
		}
		if (funcSpec == null) {
			throw new NullPointerError(
				"Null function name passed to FlowControl.processFlushCommandFromServer()");
		}

		if (funcSpec == RpcFunctionSpec.PROTOCOL_FLUSH1) {
			// In the simplest case all we do is copy the values
			// from inside the incoming flush1 and immediately send
			// out a flush2 with the same values.
			try {
				sendFlush2(rpcConnection, resultsMap);
			} catch (Exception exc) {
				// Bad format or cast error; either way, this is very wrong...
				Log.exception(exc);
				throw new ProtocolError(
					"Format error in FlowControl.processFlushCommandFromServer");
			}
		} else if (funcSpec == RpcFunctionSpec.PROTOCOL_FLUSH2) {
			throw new UnimplementedError("Unimplemented flush2 handler!");
		} else {
			
			// Can't really happen, but never mind -- it *will* happen one day...
			
			throw new ProtocolError(
					"Unexpected flow command in processFlushCommandFromServer: '"
					+ funcSpec + "'");
		}
	}

	public void sendFlush1(RpcConnection rpcConnection, Map<String, Object> resultsMap) {
		if (rpcConnection == null) {
			throw new NullPointerError(
					"Null rpc connection passed to FlowControl.sendFlush1()");
		}
		if (resultsMap == null) {
			throw new NullPointerError(
					"Null rpc result map passed to FlowControl.sendFlush1()");
		}
		
		String hwmStr = (String) resultsMap.get(RpcFunctionMapKey.FLUSH_HWM);
		String seqNumStr = (String) resultsMap.get(RpcFunctionMapKey.FLUSH_SEQ);
		String rseqNumStr = (String) resultsMap.get(RpcFunctionMapKey.FLUSH_RSEQ);
		
		Map<String, Object> flushMap = new HashMap<String, Object>();
		if (hwmStr != null) {
			flushMap.put(RpcFunctionMapKey.FLUSH_HWM, "" + new Long(hwmStr));
		}
		if (seqNumStr != null) {
			flushMap.put(RpcFunctionMapKey.FLUSH_SEQ, "" + new Long(seqNumStr));
		}
		if (rseqNumStr != null) {
			flushMap.put(RpcFunctionMapKey.FLUSH_RSEQ, "" + new Long(rseqNumStr));
		}
		
		RpcPacket.constructRpcPacket(
				RpcFunctionSpec.PROTOCOL_FLUSH1, flushMap, null);
		
	}
	
	public void sendFlush2(RpcConnection rpcConnection, Map<String, Object> resultsMap)
														throws ConnectionException {
		if (rpcConnection == null) {
			throw new NullPointerError(
					"Null rpc connection passed to FlowControl.sendFlush2()");
		}
		if (resultsMap == null) {
			throw new NullPointerError(
					"Null rpc result map passed to FlowControl.sendFlush2()");
		}

		String hwmStr = (String) resultsMap.get(RpcFunctionMapKey.FLUSH_HWM);
		String seqNumStr = (String) resultsMap.get(RpcFunctionMapKey.FLUSH_SEQ);
		String rseqNumStr = (String) resultsMap.get(RpcFunctionMapKey.FLUSH_RSEQ);
		
		Map<String, Object> flushMap = new HashMap<String, Object>();
		if (hwmStr != null) {
			flushMap.put(RpcFunctionMapKey.FLUSH_HWM, "" + new Long(hwmStr));
		}
		if (seqNumStr != null) {
			flushMap.put(RpcFunctionMapKey.FLUSH_SEQ, "" + new Long(seqNumStr));
		}
		if (rseqNumStr != null) {
			flushMap.put(RpcFunctionMapKey.FLUSH_RSEQ, "" + new Long(rseqNumStr));
		}
		
		RpcPacket flush2Packet = RpcPacket.constructRpcPacket(
				RpcFunctionSpec.PROTOCOL_FLUSH2, flushMap, null);
		
		rpcConnection.putRpcPacket(flush2Packet);
	}
	
	public void incrementCurrentHiMark(int incr) {
		this.currentHiMark += incr;
	}
	
	public void decrementCurrentHiMark(int decr) {
		this.currentHiMark -= decr;
	}

	public long getLoMark() {
		return this.loMark;
	}

	public void setLoMark(long loMark) {
		this.loMark = loMark;
	}

	public long getHiMark() {
		return this.hiMark;
	}

	public void setHiMark(long hiMark) {
		this.hiMark = hiMark;
	}

	public long getCurrentLoMark() {
		return this.currentLoMark;
	}

	public void setCurrentLoMark(long currentLoMark) {
		this.currentLoMark = currentLoMark;
	}

	public long getCurrentHiMark() {
		return this.currentHiMark;
	}

	public void setCurrentHiMark(long currentHiMark) {
		this.currentHiMark = currentHiMark;
	}
}
