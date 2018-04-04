/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.packet;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.ConnectionNotConnectedException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.RpcServer;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientFunctionDispatcher;
import com.perforce.p4java.impl.mapbased.rpc.func.proto.FlowControl;
import com.perforce.p4java.impl.mapbased.rpc.func.proto.ProtocolFunctionDispatcher;

/**
 * Top-level client-side packet dispatcher. Responsible for dispatching
 * packets (and their contents) to the correct processor classes or
 * methods as the packets come in from the server. Also responsible for
 * flow control and sundry other things as explained below. There is
 * one RpcPacketDispatcher object for each RpcServer object.
 * 
 *
 */

public class RpcPacketDispatcher {
	
	/**
	 * An enum used by subsidiary function and packet dispatchers to
	 * tell this level of dispatcher what to do. Individual values
	 * are hopefully somewhat self-explanatory.
	 */
	public enum RpcPacketDispatcherResult {
		NONE,
		CONTINUE,		// continue without changing dispatch mode
		CONTINUE_LOOP,	// continue in dispatch loop mode
		CONTINUE_DUPLEX,	// continue in dispatch duplex mode
		STOP_NORMAL,	// stop dispatch in normal way (no errors, etc.).
		STOP_ERROR;
	};
	
	/**
	 * Specifies the mode the dispatcher's currently running in.
	 * Names are taken from the informal README explanation of
	 * the C++ API's dispatch models, so don't blame me...
	 */
	public enum RpcPacketDispatcherMode {
		NONE,		// Strictly speaking, a grave error...
		PRIMAL,		// Client sends initial command
		CALLBACK,	// Server instructing client; continues until server sends release; used for
					// output-only commands (typically, anyway)
		LOOP,		// Server instructed client to send another message back to the server
					// to continue flow of control; typically used for client-side things like
					// logins, spec edits, etc. where the client should interact with the end user
					// (which we typically fake in the upper levels of P4Java)
		DUPLEX,		// Perforce server instructs client to send simple acks of operations.
		DUPLEXREV	// Not currently used here
	};
	
	public static final String TRACE_PREFIX = "RpcPacketDispatcher";
	private FlowControl flowController = null;
	
	private ProtocolFunctionDispatcher protocolDispatcher = null;
	private ClientFunctionDispatcher clientDispatcher = null;
	private Properties props = null;
	
	public RpcPacketDispatcher(Properties props, RpcServer server) {
		this.props = props;
		this.protocolDispatcher = new ProtocolFunctionDispatcher(this, this.props);
		this.clientDispatcher = new ClientFunctionDispatcher(this, this.props, server);
		this.flowController = new FlowControl(this, this.props);
	}
	
	/**
	 * Top-level dispatcher method.<p>
	 * 
	 * Dispatch (i.e. process) commands sent from the Perforce server in
	 * response to the original user command. This is an arbitrarily complex
	 * process, and may or may not involve traffic management, etc., so 
	 * the implementation here may be less than transparent...<p>
	 * 
	 * This method only returns when the entire original command was finished;
	 * basically, if it returns at all, things went well; otherwise it throws
	 * a suitable exception. Extensive work is actually passed off to the
	 * various sub dispatchers, which may do their own client / server messaging.
	 */
	
	public List<Map<String, Object>> dispatch(CommandEnv cmdEnv)
							throws ConnectionException, AccessException {
		
		// Basic idea is to sit in the receive loop processing commands until we see
		// a release command come back from the Perforce server.
						
		if (cmdEnv == null) {
			throw new NullPointerError("Null command environment passed to main dispatcher");
		}
		
		if (cmdEnv.getRpcConnection() == null) {
			throw new NullPointerError("Null rpc connection passed to main dispatcher");
		}
		
		int cmdCallBackKey = cmdEnv.getCmdCallBackKey();
		RpcConnection rpcConnection = cmdEnv.getRpcConnection();
		
		List<Map<String, Object>> resultMaps = new CopyOnWriteArrayList<>(new LinkedList<Map<String, Object>>());
		cmdEnv.setResultMaps(resultMaps);
		
		try {
			RpcPacketDispatcherMode dispatchMode = RpcPacketDispatcherMode.PRIMAL;
			RpcPacket packet = null;
			
			if (cmdEnv.getProgressCallback() != null) {
				cmdEnv.getProgressCallback().start(cmdCallBackKey);
			}
			
			while ((packet = rpcConnection.getRpcPacket(cmdEnv.getFieldRule(), cmdEnv.getFilterCallback())) != null) {

				// User cancelled command
				if (cmdEnv.isUserCanceled()) {
					return resultMaps;
				}
				
				Map<String, Object> paramMap = null;	// contains the incoming packet's parameter map,
														// if any.
				String funcNameStr = null;
				RpcFunctionSpec func = RpcFunctionSpec.NONE;

				paramMap = packet.getResultsMap();
				if (paramMap == null) {
					throw new ProtocolError("Null results map in P4JRpcTextPacket");
				}
				
				funcNameStr = packet.getFuncNameString();	
				
				if (funcNameStr == null) {
					throw new ProtocolError("Null function value string in dispatch text packet");
				}
				
				func  = RpcFunctionSpec.decode(funcNameStr);
				
				if (func == RpcFunctionSpec.NONE) {
					throw new ProtocolError("Unable to decode function in RpcPacket;"
							+ " func string: " + funcNameStr);
				}
									
				switch (func.getType()) {
				
					case CLIENT:
						switch (clientDispatcher.dispatch(dispatchMode, 
										func, cmdEnv, paramMap)) {
							case CONTINUE:
								break;
							case CONTINUE_LOOP:
								// We're in (or starting) an interactive exchange...
								dispatchMode = RpcPacketDispatcherMode.LOOP;
								break;
							case STOP_NORMAL:
								return resultMaps;
							default:
								break;
						}

						break;
						
					case USER:
						throw new ProtocolError(
								"Unexpected user function in dispatch: '"
								+ funcNameStr + "'");
						
					case SERVER:
						throw new UnimplementedError(
									"Unexpected server function '"
									+ funcNameStr
									+ "' encountered in RPC dispatch"
								);

					case PROTOCOL:	
						switch (protocolDispatcher.dispatch(dispatchMode, 
													func, cmdEnv, paramMap)) {
							case CONTINUE:
								break;
							case CONTINUE_LOOP:
								// We're in (or starting) an interactive exchange...
								dispatchMode = RpcPacketDispatcherMode.LOOP;
								break;
							case STOP_NORMAL:
								return resultMaps;
							default:
								break;
						}
						
						break;
						
					default:
						throw new ProtocolError(
								"Unrecognized function string type in RPC packet: '"
								+ funcNameStr + "'");
				}
				
			}
			
			// If we get here, we got a non-positive return from the recv, which almost
			// always means the other end unexpectedly shut down the connection (timed out or
			// whatever).

			throw new ConnectionNotConnectedException(
						"Perforce server disconnected at server end; unknown cause.");
			
		} catch (ConnectionNotConnectedException cnce) {
			throw cnce;
		} catch (Throwable thr) {
			Log.error("Unexpected exception: " + thr.getLocalizedMessage());
			Log.exception(thr);
			throw new ConnectionException(thr.getLocalizedMessage(), thr);
		}
	}
	
	/**
	 * Attempt to cleanly shut down the dispatcher; this should involve
	 * sending a release2 packet, but this is not always possible, and
	 * we suppress any resulting errors.
	 */
	
	public void shutdown(RpcConnection rpcConnection)  throws ConnectionException {
		if ((this.protocolDispatcher != null) && (rpcConnection != null)) {
			try {
				this.protocolDispatcher.sendRelease2(rpcConnection);
			} catch (Exception exc) {
				Log.warn("Unexpected exception in RPC packet dispatch shutdown: "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}
		}
	}

	public FlowControl getFlowController() {
		return this.flowController;
	}

	protected void setFlowController(FlowControl flowController) {
		this.flowController = flowController;
	}
}
