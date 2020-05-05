/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.proto;

import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherMode;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherResult;
import com.perforce.p4java.server.callback.IProgressCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * General dispatcher for incoming protocol functions.
 * 
 *
 */

public class ProtocolFunctionDispatcher {
	
	public static final String TRACE_PREFIX = "ProtocolFunctionDispatcher";
	
	@SuppressWarnings("unused") // used for debugging
	private RpcPacketDispatcher mainDispatcher = null;
	@SuppressWarnings("unused") // used for debugging
	private Properties props = null;
	
	public ProtocolFunctionDispatcher(RpcPacketDispatcher mainDispatcher,
									Properties props) {
		if (mainDispatcher == null) {
			throw new NullPointerError(
				"Null main dispatcher passed to ProtocolFunctionDispatcher constructor");
		}
		
		this.props = props;
		
		this.mainDispatcher = mainDispatcher;
	}

	public RpcPacketDispatcherResult dispatch(RpcPacketDispatcherMode dispatchMode,
			RpcFunctionSpec funcSpec, CommandEnv cmdEnv,
							Map<String, Object> resultsMap) throws ConnectionException {
		
		if (funcSpec == null) {
			throw new NullPointerError(
				"Null function spec passed to ProtocolFunctionDispatcher.dispatch()");
		}
		
		if (cmdEnv == null) {
			throw new NullPointerError(
				"Null command environment passed to ProtocolFunctionDispatcher.dispatch()");
		}
		
		int cmdCallBackKey = cmdEnv.getCmdCallBackKey();
		RpcConnection rpcConnection = cmdEnv.getRpcConnection();
				
		RpcPacketDispatcherResult result = RpcPacketDispatcherResult.NONE;
		
		switch (funcSpec) {
			case PROTOCOL_PROTOCOL:
				// We copy the resulting map values because they'll be used all over the place,
				// long after the resultsMap itself can be deleted or have its values changed
				
				for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
					cmdEnv.getServerProtocolSpecsMap().put(entry.getKey(), entry.getValue());
				}
				result = RpcPacketDispatcherResult.CONTINUE;
				break;
				
			case PROTOCOL_FLUSH1:

				// Basically all we're supposed to do is send out a corresponding flush2
				// packet, post haste. This may not always be possible, but we do our best...
				
				RpcPacket flush2Packet = rpcConnection.getFlowController().respondToFlush1(resultsMap);
				rpcConnection.putRpcPacket(flush2Packet);
				result = RpcPacketDispatcherResult.CONTINUE;
				
				break;
				
			case PROTOCOL_FLUSH2:
				// We shouldn't be seeing this (we should be *sending* it, not 
				// receiving it...).
				
				throw new ProtocolError("Unexpected flush2 message in protocol dispatcher");

			case PROTOCOL_RELEASE:				
				IProgressCallback progressCallback = cmdEnv.getProgressCallback();
				
				if (progressCallback != null) {
					progressCallback.stop(cmdCallBackKey);
				}
				result = RpcPacketDispatcherResult.STOP_NORMAL;
				break;
				
			case PROTOCOL_RELEASE2:
				// We shouldn't be seeing this (we should be *sending* it, not 
				// receiving it...).
				
				throw new ProtocolError("Unexpected release2 message in protocol dispatcher");

			case PROTOCOL_COMPRESS1:
				// Arrange to have the connection bytes sent across a zlib-compressed
				// connection at the lower levels; if successful, send a compress2
				// message back to the server. Note that once the connection is set to
				// using a compressed stream, the only way to set it uncompressed is to
				// close down the connection.
				// All the real work is done in useConnectionCompression();
				
				rpcConnection.useConnectionCompression();
				result = RpcPacketDispatcherResult.CONTINUE;
				break;
				
			case PROTOCOL_COMPRESS2:
				// We shouldn't be seeing this (we should be *sending* it, not 
				// receiving it...).
				
				throw new ProtocolError("Unexpected compress2 message in protocol dispatcher");

			case PROTOCOL_ECHO:
				// TODO PTA

			case PROTOCOL_CRYPTO:
				// TODO PTA

			case PROTOCOL_ERRORHANDLER:
				// TODO PTA

			case PROTOCOL_FUNCHANDLER:
				// TODO PTA

			default:
				throw new P4JavaError(
					"Unimplemented function spec in ProtocolFunctionDispatcher.dispatch(): '"
					+ funcSpec.toString() + "'");
		}

		return result;
	}
	
	public void sendRelease2(RpcConnection rpcConnection)
											throws ConnectionException {
		if (rpcConnection == null) {
			throw new NullPointerError(
				"Null rpc connection passed to ProtocolFunctionDispatcher.sendRelease2()");
		}
				
		Map<String, Object> flushMap = new HashMap<String, Object>(2);
		
		RpcPacket flush2Packet = RpcPacket.constructRpcPacket(
				RpcFunctionSpec.PROTOCOL_RELEASE2, flushMap, null);
		
		rpcConnection.putRpcPacket(flush2Packet);
	}
}
