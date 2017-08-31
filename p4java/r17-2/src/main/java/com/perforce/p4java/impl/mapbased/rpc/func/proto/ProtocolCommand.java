/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.proto;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to specify Perforce RPC protocol-related parameters
 * on the first call to a Perforce server on an RPC connection.<p>
 * 
 * Much of this has been cribbed from the corresponding C++ API
 * protocol specs, and will probably need revisiting with experience.
 * In general, don't change values here unless you know what you're
 * doing, as they can have considerable effects on client- and server-side
 * behaviours.
 * 
 *
 */

public class ProtocolCommand {
	
	public static final String RPC_ARGNAME_PROTOCOL_CMPFILE = "cmpfile";
	public static final String RPC_ARGNAME_PROTOCOL_CLIENT_API = "client";
	public static final String RPC_ARGNAME_PROTOCOL_APPLICATION_NAME = "app";
	public static final String RPC_ARGNAME_PROTOCOL_SERVER_API = "api";
	public static final String RPC_ARGNAME_PROTOCOL_SENDBUFSIZE = "sndbuf";
	public static final String RPC_ARGNAME_PROTOCOL_RECVBUFSIZE = "rcvbuf";
	public static final String RPC_ARGNAME_PROTOCOL_ZTAGS = "tag";
	public static final String RPC_ARGNAME_PROTOCOL_ENABLE_STREAMS = "enableStreams";
	public static final String RPC_ARGNAME_PROTOCOL_ENABLE_GRAPH = "enableGraph";
	public static final String RPC_ARGNAME_PROTOCOL_ENABLE_TRACKING = "track";
	public static final String RPC_ARGNAME_PROTOCOL_ENABLE_PROGRESS = "progress";
	public static final String RPC_ARGNAME_PROTOCOL_HOST = "host"; // P4HOST
	public static final String RPC_ARGNAME_PROTOCOL_PORT = "port"; // P4PORT
	public static final String RPC_ARGNAME_PROTOCOL_IPADDR = "ipaddr";

	private int clientApiLevel = -1;	// Client API level; determines client-side capabilities
	private boolean clientCmpFile = false;	// True if the client can do file compares
	private int serverApiLevel = -1;
	private String applicationName = null;  // Application name
	private int sendBufSize = 0;	// Socket send buf size (system-determined)
	private int recvBufSize = 0;	// Socket receive buf size (ditto)
	private String host = null;  // P4HOST
	private String port = null;  // P4PORT
	private String ipaddr = null;

	// These values are set elsewhere
	private boolean useTags = false;	// Use tagged output a la p4's -ztag option
	private boolean enableStreams = true;	// True if the client is capable of handling streams
	private boolean enableGraph = false; // True if the client is capable of handling graph
	private boolean enableTracking = false;	// True if enabling tracking for individual commands (-Ztrack option)
	private boolean enableProgress = false;	// True if enabling progress indicator report for a command (-I option)
	private boolean quietMode = false;	// True to suppress ALL info-level output. (-q option)
	
	public ProtocolCommand() {	
	}
	
	public ProtocolCommand(int clientApiLevel, boolean clientCmpFile,
							int serverApiLevel, int sendBufSize, int recvBufSize,
							boolean useTags) {
		this.clientApiLevel = clientApiLevel;
		this.clientCmpFile = clientCmpFile;
		this.serverApiLevel = serverApiLevel;
		this.sendBufSize = sendBufSize;
		this.recvBufSize = recvBufSize;
		this.useTags = useTags;
	}
	
	public ProtocolCommand(int clientApiLevel, boolean clientCmpFile,
							int serverApiLevel, int sendBufSize, int recvBufSize,
							boolean useTags, boolean enableStreams, boolean enableGraph) {
		this.clientApiLevel = clientApiLevel;
		this.clientCmpFile = clientCmpFile;
		this.serverApiLevel = serverApiLevel;
		this.sendBufSize = sendBufSize;
		this.recvBufSize = recvBufSize;
		this.useTags = useTags;
		this.enableStreams = enableStreams;
		this.enableGraph = enableGraph;
	}

	public Map<String, Object> asMap() {
		Map<String, Object> valMap = new HashMap<String, Object>();
		
		valMap.put(RPC_ARGNAME_PROTOCOL_CMPFILE, clientCmpFile ? null : "");
		valMap.put(RPC_ARGNAME_PROTOCOL_CLIENT_API, "" + clientApiLevel);
		valMap.put(RPC_ARGNAME_PROTOCOL_SERVER_API, "" + serverApiLevel);
		valMap.put(RPC_ARGNAME_PROTOCOL_SENDBUFSIZE,  "" + sendBufSize);
		valMap.put(RPC_ARGNAME_PROTOCOL_RECVBUFSIZE, "" + recvBufSize);
		
		if (this.applicationName != null) {
			valMap.put(RPC_ARGNAME_PROTOCOL_APPLICATION_NAME, applicationName);
		}
		if (this.useTags) {
			valMap.put(RPC_ARGNAME_PROTOCOL_ZTAGS, "");
		}

		valMap.put(RPC_ARGNAME_PROTOCOL_ENABLE_STREAMS, (this.enableStreams) ? "" : "no");

		valMap.put(RPC_ARGNAME_PROTOCOL_ENABLE_GRAPH, (this.enableGraph) ? "" : "no");

		if (this.enableTracking) {
			valMap.put(RPC_ARGNAME_PROTOCOL_ENABLE_TRACKING, "");
		}
		if (this.enableProgress) {
			// On each command message sent to the server (i.e. "user-foo")
	        // a variable "progress" should be set to 1 to indicate that
	        // the server should send progress messages to the client if they
	        // are available for that command.
			valMap.put(RPC_ARGNAME_PROTOCOL_ENABLE_PROGRESS, "1");
		}
		if (this.host != null) {
			valMap.put(RPC_ARGNAME_PROTOCOL_HOST, host);
		}
		if (this.port != null) {
			valMap.put(RPC_ARGNAME_PROTOCOL_PORT, port);
		}
		if (this.ipaddr != null) {
			valMap.put(RPC_ARGNAME_PROTOCOL_IPADDR, ipaddr);
		}
		return valMap;
	}
	
	public int getClientApiLevel() {
		return this.clientApiLevel;
	}

	public void setClientApiLevel(int clientApiLevel) {
		this.clientApiLevel = clientApiLevel;
	}

	public boolean isClientCmpFile() {
		return this.clientCmpFile;
	}

	public void setClientCmpFile(boolean clientCmpFile) {
		this.clientCmpFile = clientCmpFile;
	}

	public int getServerApiLevel() {
		return this.serverApiLevel;
	}

	public void setServerApiLevel(int serverApiLevel) {
		this.serverApiLevel = serverApiLevel;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public int getSendBufSize() {
		return this.sendBufSize;
	}

	public void setSendBufSize(int sendBufSize) {
		this.sendBufSize = sendBufSize;
	}

	public int getRecvBufSize() {
		return this.recvBufSize;
	}

	public void setRecvBufSize(int recvBufSize) {
		this.recvBufSize = recvBufSize;
	}

	public boolean isUseTags() {
		return this.useTags;
	}

	public void setUseTags(boolean useTags) {
		this.useTags = useTags;
	}

	public boolean isEnableStreams() {
		return this.enableStreams;
	}

	public void setEnableStreams(boolean enableStreams) {
		this.enableStreams = enableStreams;
	}

	public boolean isEnableGraph() {
		return this.enableGraph;
	}

	public void setEnableGraph(boolean enableGraph) {
		this.enableGraph = enableGraph;
	}

	public boolean isEnableTracking() {
		return this.enableTracking;
	}

	public void setEnableTracking(boolean enableTracking) {
		this.enableTracking = enableTracking;
	}

	public boolean isEnableProgress() {
		return enableProgress;
	}

	public void setEnableProgress(boolean enableProgress) {
		this.enableProgress = enableProgress;
	}

	public boolean isQuietMode() {
		return quietMode;
	}

	public void setQuietMode(boolean quietMode) {
		this.quietMode = quietMode;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return this.port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getIpAddr() {
		return this.ipaddr;
	}

	public void setIpAddr(String ipaddr) {
		this.ipaddr = ipaddr;
	}
}
