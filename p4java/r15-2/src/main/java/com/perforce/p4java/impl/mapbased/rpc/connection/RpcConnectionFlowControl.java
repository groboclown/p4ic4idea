/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.connection;

import java.util.HashMap;
import java.util.Map;

import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;

/**
 * Provides Perforce flow control services for the Perforce server connection.<p>
 * 
 *
 */

public class RpcConnectionFlowControl {

	public static final String TRACE_PREFIX = "RpcConnectionFlowControl";
	
	/**
	 * Length in bytes of a flush command when marshaled. This is actually
	 * a fiction -- it's more like 50 -- but the conservative sizing gives
	 * us a little leeway when things are tight (see the C++ API for a
	 * discussion on this -- we're just copying their behavior...).
	 */
	
	public static final int FLUSH_CMD_LENGTH = 60;
	
	protected RpcConnectionFlowControl() {
		
	}

	/**
	 * Construct a flush2 response to the passed-in flush1 packet.
	 */
	
	public RpcPacket respondToFlush1(RpcPacket flush1) {
		
		if (flush1 != null) {
			return respondToFlush1(flush1.getResultsMap());
		}
		
		return null;
	}
	
	/**
	 * Construct a flush2 response to the passed-in flush1 packet.
	 */
	
	public RpcPacket respondToFlush1(Map<String, Object> resultsMap) {
		
		if (resultsMap != null) {
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
			
			return flush2Packet;
		}
		
		return null;
	}
}
