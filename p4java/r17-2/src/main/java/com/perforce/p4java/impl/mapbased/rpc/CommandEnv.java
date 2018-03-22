/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.proto.ProtocolCommand;
import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldRule;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.server.callback.IParallelCallback;
import com.perforce.p4java.server.callback.IProgressCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;

/**
 * Used to package up the Perforce function environment for a single
 * Perforce command across multiple RPC function calls.<p>
 * 
 * In particular, we need to keep things like file handles,
 * arbitrary RPC function arguments, etc., around for use during
 * complex long-running commands that span many dispatch calls
 * in loop or duplex mode, etc., in response to single
 * user commands like 'sync'.<p>
 * 
 * Note that this is in distinction to a) the command's external
 * environment (in the ExternalEnv class), and b) the command's
 * individual function environments,
 * 
 *
 */

public class CommandEnv {
	
	/**
	 * Max number of live handlers per command cycle.
	 * Value copied straight from the C++ API.
	 */
	public static final int MAX_HANDLERS = 10;

	/**
	 * Sequence used by operating system to separate lines in text files.
	 * Default to "\n" if it is not available.
	 */
	public static final String LINE_SEPARATOR = System.getProperty(
			"line.separator", "\n");
	
	/**
	 * P4Java's version of the notorious handler class
	 * in the C++ API. Basically used (and abused) in
	 * much the same way, mostly (at least) until I can
	 * work out a better way to do things, at which
	 * point it's likely to be factored out elsewhere.
	 */
	
	public class RpcHandler {
		
		private String name = null;
		private boolean error = false;
		private File file = null;
		private Map<String, Object> map = null;

		public RpcHandler(String name, boolean error, File file) {
			this.name = name;
			this.error = error;
			this.file = file;
			this.map = new HashMap<String, Object>();
		}
		
		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public boolean isError() {
			return this.error;
		}
		public void setError(boolean error) {
			this.error = error;
		}

		public File getFile() {
			return this.file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		public Map<String, Object> getMap() {
			return this.map;
		}

		public void setMap(Map<String, Object> map) {
			this.map = map;
		}
	}

	/**
	 * The parent server object
	 */
	private RpcServer server = null;
	
	/**
	 * The current user function that started this all...
	 */
	private RpcCmdSpec cmdSpec = null;
	
	/**
	 * The result maps that will ultimately be passed back to the user levels.
	 */
	private List<Map<String, Object>> resultMaps = null;
	
	/**
	 * State map for storing arbitrary state across RPC function
	 * calls.
	 */
	private Map<String, Object> stateMap = null;
	
	/** C++ API-like handlers. Will probably be refactored
	 * out later when I can work out what to do with them.
	 * NOTE: code below relies on the handlers array elements
	 * being initialized to null.
	 */
	private RpcHandler[] handlers = new RpcHandler[MAX_HANDLERS];
	
	/**
	 * Protocol specs (in command form). We carry this around
	 * for possible reference only; it may disappear in future
	 * refactorings if it's never used.
	 */
	private ProtocolCommand protocolSpecs = null;
	
	private Map<String, Object> serverProtocolSpecsMap = null;
	
	private IProgressCallback progressCallback = null;
	
	private int cmdCallBackKey = 0;
	
	private boolean syncInPlace = false;
	private boolean nonCheckedSyncs = false;
	private boolean dontWriteTicket = false;

	private boolean streamCmd = false;
	
	private RpcPacketFieldRule fieldRule = null;
	
	private IStreamingCallback streamingCallback = null;
	private int streamingCallbackKey = 0;
	
	private IFilterCallback filterCallback = null;
	
	private IParallelCallback parallelCallback = null;

	/**
	 * The Perforce RPC connection in use for this command.
	 */
	private RpcConnection rpcConnection = null;
	
	private boolean userCanceled = false; // true if the user tried to cancel the command
	
	public CommandEnv(RpcServer server, RpcCmdSpec cmdSpec, RpcConnection rpcConnection,
									ProtocolCommand protocolSpecs,
									Map<String, Object> serverProtocolSpecsMap,
									IProgressCallback progressCallback,
									int cmdCallBackKey,
									boolean syncInPlace,
									boolean nonCheckedSyncs) {
		this.server = server;
		this.cmdSpec = cmdSpec;
		this.rpcConnection = rpcConnection;
		this.protocolSpecs = protocolSpecs;
		this.stateMap = new HashMap<String, Object>();
		this.serverProtocolSpecsMap = serverProtocolSpecsMap;
		this.progressCallback = progressCallback;
		this.cmdCallBackKey = cmdCallBackKey;
		this.syncInPlace = syncInPlace;
		this.nonCheckedSyncs = nonCheckedSyncs;
	}
	
	public boolean addHandler(RpcHandler handler) {
		for (int i = 0; i < handlers.length; i++) {
			if ((handlers[i] != null) && (handlers[i].name != null)
					&& handlers[i].name.equalsIgnoreCase(handler.name)) {
				
			}
			if (handlers[i] == null) {
				handlers[i] = handler;
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Handle a result by either adding it to the resultsMapVec
	 * for later processing or passing it up to the streaming results
	 * callback handler.
	 * 
	 * @param resultMap
	 */
	
	public void handleResult(Map<String, Object> resultMap) {
		if (streamingCallback != null) {
			if (!userCanceled) {
				try {
					userCanceled = !streamingCallback.handleResult(resultMap, streamingCallbackKey);
				} catch (P4JavaException exc) {
					Log.error("caught exception from streaming callback handler (key: "
							+ streamingCallbackKey
							+ "): " + exc.getLocalizedMessage());
					Log.exception(exc);
				}
			}
		} else {
			resultMaps.add(resultMap);
		}
	}

	public RpcHandler getHandler(String handlerName) {
		for (RpcHandler handler : handlers) {
			if ((handler != null) && (handlerName != null)
								&& (handler.getName() != null)
								&& handler.getName().equalsIgnoreCase(handlerName)) {
				return handler;
			}
		}
		
		return null;
		
	}
	public RpcCmdSpec getCmdSpec() {
		return this.cmdSpec;
	}

	public void setCmdSpec(RpcCmdSpec cmdSpec) {
		this.cmdSpec = cmdSpec;
	}

	public List<Map<String, Object>> getResultMaps() {
		return this.resultMaps;
	}

	public void setResultMaps(List<Map<String, Object>> resultMaps) {
		this.resultMaps = resultMaps;
	}

	public Map<String, Object> getStateMap() {
		return this.stateMap;
	}

	public void setStateMap(Map<String, Object> stateMap) {
		this.stateMap = stateMap;
	}

	public ProtocolCommand getProtocolSpecs() {
		return this.protocolSpecs;
	}

	public void setProtocolSpecs(ProtocolCommand protocolSpecs) {
		this.protocolSpecs = protocolSpecs;
	}

	public RpcConnection getRpcConnection() {
		return this.rpcConnection;
	}

	public void setRpcConnection(RpcConnection rpcConnection) {
		this.rpcConnection = rpcConnection;
	}

	public RpcHandler[] getHandlers() {
		return this.handlers;
	}

	public void setHandlers(RpcHandler[] handlers) {
		this.handlers = handlers;
	}
	
	public void newHandler() {
		// Does nothing at the moment, as a standin for the C++ API version
		// FIXME -- HR.
	}

	public Map<String, Object> getServerProtocolSpecsMap() {
		return serverProtocolSpecsMap;
	}

	public void setServerProtocolSpecsMap(Map<String, Object> serverProtocolSpecsMap) {
		this.serverProtocolSpecsMap = serverProtocolSpecsMap;
	}

	public IProgressCallback getProgressCallback() {
		return progressCallback;
	}

	public void setProgressCallback(IProgressCallback progressCallback) {
		this.progressCallback = progressCallback;
	}

	public int getCmdCallBackKey() {
		return this.cmdCallBackKey;
	}

	public void setCmdCallBackKey(int cmdCallBackKey) {
		this.cmdCallBackKey = cmdCallBackKey;
	}

	public boolean isUserCanceled() {
		return this.userCanceled;
	}

	public void setUserCanceled(boolean userCanceled) {
		this.userCanceled = userCanceled;
	}

	public boolean isSyncInPlace() {
		return syncInPlace;
	}

	public void setSyncInPlace(boolean syncInPlace) {
		this.syncInPlace = syncInPlace;
	}

	public IStreamingCallback getStreamingCallback() {
		return streamingCallback;
	}

	public void setStreamingCallback(IStreamingCallback streamingCallback) {
		this.streamingCallback = streamingCallback;
	}

	public int getStreamingCallbackKey() {
		return streamingCallbackKey;
	}

	public void setStreamingCallbackKey(int streamingCallbackKey) {
		this.streamingCallbackKey = streamingCallbackKey;
	}

	public boolean isNonCheckedSyncs() {
		return nonCheckedSyncs;
	}

	public void setNonCheckedSyncs(boolean nonCheckedSyncs) {
		this.nonCheckedSyncs = nonCheckedSyncs;
	}

	public boolean isDontWriteTicket() {
		return dontWriteTicket;
	}

	public void setDontWriteTicket(boolean dontWriteTicket) {
		this.dontWriteTicket = dontWriteTicket;
	}

	public boolean isStreamCmd() {
		return streamCmd;
	}

	public void setStreamCmd(boolean streamCmd) {
		this.streamCmd = streamCmd;
	}

	public RpcPacketFieldRule getFieldRule() {
		return fieldRule;
	}

	public void setFieldRule(RpcPacketFieldRule fieldRule) {
		this.fieldRule = fieldRule;
	}

	public IFilterCallback getFilterCallback() {
		return filterCallback;
	}

	public void setFilterCallback(IFilterCallback filterCallback) {
		this.filterCallback = filterCallback;
	}

	public IParallelCallback getParallelCallback() {
		return parallelCallback;
	}
	
	public void setParallelCallback(IParallelCallback parallelCallback) {
		this.parallelCallback = parallelCallback;
	}

	public RpcServer getServer() {
		return server;
	}
}
