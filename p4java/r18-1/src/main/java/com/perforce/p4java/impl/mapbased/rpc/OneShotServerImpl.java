/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.ConnectionNotConnectedException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.generic.core.TempFileInputStream;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.func.proto.ProtocolCommand;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher;
import com.perforce.p4java.impl.mapbased.rpc.stream.RpcSocketPool;
import com.perforce.p4java.impl.mapbased.rpc.stream.RpcSocketPool.ShutdownHandler;
import com.perforce.p4java.impl.mapbased.rpc.stream.RpcStreamConnection;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcOutputStream;
import com.perforce.p4java.impl.mapbased.server.ServerAddressBuilder;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IServerAddress;
import com.perforce.p4java.server.IServerAddress.Protocol;
import com.perforce.p4java.server.ServerStatus;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.server.callback.IParallelCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.BufferOverflowException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * A one-shot (connection-per-command) version of the RPC protocol
 * implementation. This version is intended for use by multiple threads
 * using the same IServer object simultaneously, and is therefore
 * thread-safe. The tradeoff here is connection latency -- each time
 * a command is executed a new connection to the Perforce server is
 * created, used, and closed.<p>
 * 
 * Note that performance monitors and command callback objects are shared
 * by all threads using this server object and are not synchronized by
 * this class -- you must sort out any conflicting output yourself (such
 * conflicting output is not a sign of underlying issues, and, as long
 * as you keep callbacks thread-safe, there are no problems with this at
 * this level).<p>
 */

public class OneShotServerImpl extends RpcServer {
	
	/**
	 * The short-form name (display name) of this implementation.
	 */
	public static final String SCREEN_NAME = "Native RPC";
	
	/**
	 * Implementation-specific comments (dependencies, limitations, etc.).
	 */
	public static final String IMPL_COMMENTS = "Java-native RPC standalone P4Java implementation."
							+ " Requires JDK 6 or later and full Java NIO support.";
	
	/**
	 * The specific protocol name to be used in URIs for this implementation.
	 */
	public static final String PROTOCOL_NAME = Protocol.P4JRPC.toString();
	
	/**
	 * The specific SSL protocol name to be used in URIs for this implementation.
	 */
	public static final String SSL_PROTOCOL_NAME = Protocol.P4JRPCSSL.toString();

	/**
	 * True IFF this is the default implementation. There must be only one of
	 * these...
	 */
	public static final boolean DEFAULT_STATUS = false;
	
	/**
	 * The minimum Perforce server level required by this implementation.
	 */
	public static final int MINIMUM_SUPPORTED_SERVER_LEVEL = 20052;
	
	/**
	 * What we use as a P4JTracer trace prefix for methods here.
	 */
	public static final String TRACE_PREFIX = "OneShotServerImpl";
	
	/**
	 * Socket pool for this server
	 */
	protected RpcSocketPool socketPool = null;
	
	/**
	 * Initialize the server. Basically defers to the superclass after setting
	 * up the required server version and any optional socket pools.
	 * 
	 * @see com.perforce.p4java.impl.mapbased.rpc.RpcServer#init(java.lang.String, int, java.util.Properties, com.perforce.p4java.option.UsageOptions, boolean)
	 */
	public ServerStatus init(String host, int port, Properties props, UsageOptions opts,
					boolean secure, String rsh) throws ConfigException, ConnectionException {
		super.init(host, port, props, opts, secure);
		super.minimumSupportedServerVersion = MINIMUM_SUPPORTED_SERVER_LEVEL;
		this.rsh = rsh;
		int poolSize = RpcPropertyDefs.getPropertyAsInt(this.props,
				RpcPropertyDefs.RPC_SOCKET_POOL_SIZE_NICK,
				RpcPropertyDefs.RPC_SOCKET_POOL_DEFAULT_SIZE);
		if (poolSize > 0) {
			ShutdownHandler handler = new ShutdownHandler() {

				public void shutdown(Socket socket) {
					try {
						RpcPacketDispatcher dispatcher = new RpcPacketDispatcher(
								OneShotServerImpl.this.props,
								OneShotServerImpl.this);
						RpcStreamConnection rpcConnection = new RpcStreamConnection(
								serverHost, serverPort,
								OneShotServerImpl.this.props,
								OneShotServerImpl.this.serverStats,
								OneShotServerImpl.this.charset,
								socket, null,
								OneShotServerImpl.this.secure,
								OneShotServerImpl.this.rsh);
						dispatcher.shutdown(rpcConnection);
					} catch (ConnectionException e) {
						Log.exception(e);
					}
				}
			};
			this.socketPool = new RpcSocketPool(poolSize, this.serverHost,
					this.serverPort, this.props, handler, this.secure);
		}
		
		return status;
	}
	
	/**
	 * Shorthand for the options-based init() above, but with a false secure arg.
	 * 
	 * @see com.perforce.p4java.impl.mapbased.rpc.RpcServer#init(java.lang.String, int, java.util.Properties, com.perforce.p4java.option.UsageOptions)
	 */
	public ServerStatus init(String host, int port, Properties props, UsageOptions opts)
								throws ConfigException, ConnectionException {
		return this.init(host, port, props, opts, false);
	}

	/**
	 * Shorthand for the options-based init() above, but with a null opts arg.
	 * 
	 * @see com.perforce.p4java.impl.mapbased.rpc.RpcServer#init(java.lang.String, int, java.util.Properties)
	 */
	public ServerStatus init(String host, int port, Properties props)
				throws ConfigException, ConnectionException {
		return this.init(host, port, props, null);
	}
	
	public void connect() throws ConnectionException, AccessException, 
							RequestException, ConfigException {
		this.serverStats.clear();
		super.connect();
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.rpc.RpcServer#disconnect()
	 */
	public void disconnect() throws ConnectionException, AccessException {
		if( this.socketPool != null) {
			this.socketPool.disconnect();
		}
		super.disconnect();
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execMapCmd(java.lang.String, java.lang.String[], java.util.Map)
	 */
	@Override
	public Map<String, Object>[] execMapCmd(String cmdName, String[] cmdArgs,
			Map<String, Object> inMap) throws ConnectionException,
			AccessException, RequestException {
		return this.execMapCmd(cmdName, cmdArgs, inMap, null, false, null, 0);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execMapCmdList(java.lang.String, java.lang.String[], java.util.Map)
	 */
	@Override
	public List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs,
			Map<String, Object> inMap) throws ConnectionException, AccessException, RequestException {
		return this.execMapCmdList(cmdName, cmdArgs, inMap, null, false, null, 0, null, null);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execMapCmdList(java.lang.String, java.lang.String[], java.util.Map, com.perforce.p4java.server.callback.IFilterCallback)
	 */
	@Override
	public List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs,
			Map<String, Object> inMap, IFilterCallback filterCallback) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, inMap, null, false, null, 0, filterCallback, null);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execQuietMapCmd(java.lang.String, java.lang.String[], java.util.Map)
	 */
	@Override
	public Map<String, Object>[] execQuietMapCmd(String cmdName,
			String[] cmdArgs, Map<String, Object> inMap)
			throws ConnectionException, RequestException,
			AccessException {
		return this.execMapCmd(cmdName, cmdArgs, inMap, null, true, null, 0);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execQuietMapCmdList(java.lang.String, java.lang.String[], java.util.Map)
	 */
	@Override
	public List<Map<String, Object>> execQuietMapCmdList(String cmdName,
			String[] cmdArgs, Map<String, Object> inMap) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, inMap, null, true, null, 0, null, null);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringMapCmd(java.lang.String, java.lang.String[], java.lang.String)
	 */
	public Map<String, Object>[] execInputStringMapCmd(String cmdName,
			String[] cmdArgs, String inString) throws P4JavaException {
		return this.execMapCmd(cmdName, cmdArgs, null, inString, true, null, 0);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringMapCmdList(java.lang.String, java.lang.String[], java.lang.String)
	 */
	public List<Map<String, Object>> execInputStringMapCmdList(String cmdName,
			String[] cmdArgs, String inString) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, null, inString, true, null, 0, null, null);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringMapCmdList(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IFilterCallback)
	 */
	public List<Map<String, Object>> execInputStringMapCmdList(String cmdName,
			String[] cmdArgs, String inString, IFilterCallback filterCallback) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, null, inString, true, null, 0, filterCallback, null);
	}
	
	@Override
	public List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs, IFilterCallback filterCallback,
			IParallelCallback parallelCallback) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, null, null, true, null, 0, filterCallback, parallelCallback);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringStreamingMapComd(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IStreamingCallback, int)
	 * 
	 * @deprecated As of release 2013.1, replaced by {@link #execInputStringStreamingMapCmd(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IStreamingCallback, int)}
 	 */
	@Deprecated
	public void execInputStringStreamingMapComd(String cmdName,
			String[] cmdArgs, String inString, IStreamingCallback callback,
			int key) throws P4JavaException {
		execMapCmd(cmdName, cmdArgs, null, inString, false, callback, key);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringStreamingMapCmd(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IStreamingCallback, int)
 	 */
	public void execInputStringStreamingMapCmd(String cmdName,
			String[] cmdArgs, String inString, IStreamingCallback callback,
			int key) throws P4JavaException {
		execMapCmd(cmdName, cmdArgs, null, inString, false, callback, key);
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object>[] execMapCmd(String cmdName,
			String[] cmdArgs, Map<String, Object> inMap, String inString, boolean ignoreCallbacks,
							IStreamingCallback callback, int callbackKey)
								throws ConnectionException, AccessException, RequestException {
		
		List<Map<String, Object>> results = execMapCmdList(cmdName, cmdArgs, inMap, inString, ignoreCallbacks, callback, callbackKey, null, null);
		if (results != null) {
			return results.toArray(new HashMap[results.size()]);
		}
		
		return null;
	}

	protected List<Map<String, Object>> execMapCmdList(String cmdName,
			String[] cmdArgs, Map<String, Object> inMap, String inString, boolean ignoreCallbacks,
							IStreamingCallback callback, int callbackKey, IFilterCallback filterCallback,
							IParallelCallback parallelCallback)
								throws ConnectionException, AccessException, RequestException {
		RpcPacketDispatcher dispatcher = null;
		RpcConnection rpcConnection = null;
		
		if (cmdName == null) {
			throw new NullPointerError(
					"Null command name passed to execMapCmd");
		}
		
		if (!this.connected) {
			throw new ConnectionNotConnectedException(
					"Not currently connected to a Perforce server");
		}
		
		try {
			int cmdCallBackKey = this.nextCmdCallBackKey.incrementAndGet();
			long startTime = System.currentTimeMillis();
			dispatcher = new RpcPacketDispatcher(props, this);
			rpcConnection = new RpcStreamConnection(serverHost, serverPort,
					props, this.serverStats, this.charset, null, this.socketPool,
					this.secure, this.rsh);
			ProtocolCommand protocolSpecs = new ProtocolCommand();
			
			if (inMap != null && ClientLineEnding.CONVERT_TEXT) {
				ClientLineEnding.convertMap(inMap);
			}

			ExternalEnv env = setupCmd(dispatcher, rpcConnection, protocolSpecs,
									cmdName.toLowerCase(Locale.ENGLISH), cmdArgs, inMap, ignoreCallbacks, cmdCallBackKey, false);
			CommandEnv cmdEnv = new CommandEnv(
					this,
					new RpcCmdSpec(
							cmdName.toLowerCase(Locale.ENGLISH),
							cmdArgs,
							getAuthTicket(),
							inMap,
							inString,
							env),
					rpcConnection, 
					protocolSpecs,
					this.serverProtocolMap,
					this.progressCallback,
					cmdCallBackKey,
					writeInPlace(cmdName),
					this.isNonCheckedSyncs());
			cmdEnv.setDontWriteTicket(isDontWriteTicket(cmdName.toLowerCase(Locale.ENGLISH), cmdArgs));
			cmdEnv.setFieldRule(getRpcPacketFieldRule(inMap, CmdSpec.getValidP4JCmdSpec(cmdName)));
			cmdEnv.setStreamingCallback(callback);
			cmdEnv.setStreamingCallbackKey(callbackKey);
			cmdEnv.setFilterCallback(filterCallback);
			cmdEnv.setParallelCallback(parallelCallback);
			if (callback != null) {
				try {
					callback.startResults(callbackKey);
				} catch (P4JavaException exc) {
					Log.error("streaming callback startResults method threw exception: " + exc.getLocalizedMessage());
					Log.exception(exc);
				}
			}
			List<Map<String, Object>> retMapList = dispatcher.dispatch(cmdEnv);
			long endTime = System.currentTimeMillis();
			if (callback != null) {
				try {
					callback.endResults(callbackKey);
				} catch (P4JavaException exc) {
					Log.error("streaming callback endResults method threw exception: " + exc.getLocalizedMessage());
					Log.exception(exc);
				}
			}
			
			// Check if currently case sensitive so the map search for the no
			// case key is only performed when necessary. Once a server is
			// marked as case insensitive this check will never look at the
			// server protocol specs map.
			if (this.caseSensitive
					&& cmdEnv.getServerProtocolSpecsMap().containsKey(
							RpcFunctionMapKey.NOCASE)) {
				this.caseSensitive = false;
			}
			
			if (!ignoreCallbacks && (this.commandCallback != null)) {
				this.processCmdCallbacks(cmdCallBackKey, endTime - startTime, retMapList);
			}

			// Close RPC output stream
			RpcOutputStream outStream = (RpcOutputStream) cmdEnv.getStateMap().get(
					RpcServer.RPC_TMP_OUTFILE_STREAM_KEY);
			if (outStream != null) {
				outStream.close();
			}

			return retMapList;
			
		} catch (BufferOverflowException exc) {
			Log.error("RPC Buffer overflow: " + exc.getLocalizedMessage());
			Log.exception(exc);
			throw new P4JavaError("RPC Buffer overflow: " + exc.getLocalizedMessage());
		} catch (ConnectionNotConnectedException cnce) {
			this.connected = false;
			this.status = ServerStatus.ERROR;
			throw cnce;
		} catch (IOException ioexc) {
			Log.error("RPC I/O error: " + ioexc.getLocalizedMessage());
			Log.exception(ioexc);
			throw new RequestException(
					"I/O error encountered in stream command: "
					+ ioexc.getLocalizedMessage(), ioexc);
		} finally {			
			if (rpcConnection != null) {
				rpcConnection.disconnect(dispatcher);
			}
		}
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execStreamingMapCommand(java.lang.String, java.lang.String[], java.util.Map, com.perforce.p4java.server.callback.IStreamingCallback, int)
	 */
	public void execStreamingMapCommand(String cmdName, String[] cmdArgs, Map<String, Object> inMap,
			IStreamingCallback callback, int key) throws P4JavaException {
		if (callback == null) {
			throw new NullPointerError(
							"null streaming callback passed to execStreamingMapCommand method");
		}
		
		execMapCmdList(cmdName, cmdArgs, inMap, null, false, callback, key, null, null);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execMapCmdList(String, String[], IFilterCallback, IParallelCallback)
	 */
	public void execStreamingMapCommand(String cmdName, String[] cmdArgs, Map<String, Object> inMap,
	                                    IStreamingCallback callback, int key, IParallelCallback parallelCallback) throws P4JavaException {
		if (callback == null) {
			throw new NullPointerError(
					"null streaming callback passed to execStreamingMapCommand method");
		}

		execMapCmdList(cmdName, cmdArgs, inMap, null, false, callback, key, null, parallelCallback);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execQuietStreamCmd(java.lang.String, java.lang.String[])
	 */
	@Override
	public InputStream execQuietStreamCmd(String cmdName, String[] cmdArgs)
			throws ConnectionException, RequestException,
			AccessException {
		return this.execStreamCmd(cmdName, cmdArgs, null, null, true);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execStreamCmd(java.lang.String, java.lang.String[])
	 */
	@Override
	public InputStream execStreamCmd(String cmdName, String[] cmdArgs)
			throws ConnectionException, RequestException,
			AccessException {
		return this.execStreamCmd(cmdName, cmdArgs, null, null, false);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execStreamCmd(String, String[], Map)
	 */
	@Override
	public InputStream execStreamCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
			throws P4JavaException {
		return this.execStreamCmd(cmdName, cmdArgs, inMap, null, false);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringStreamCmd(String, String[], String)
	 */
	@Override
	public InputStream execInputStringStreamCmd(String cmdName, String[] cmdArgs, String inString)
			throws P4JavaException {
		return this.execStreamCmd(cmdName, cmdArgs, null, inString, false);
	}
	
	/**
	 * Note that this method does the access / request exception processing here rather
	 * than passing things up the stack; we may introduce an extended version of this
	 * method to take the map array as an output parameter in later releases.
	 */
	protected InputStream execStreamCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap, String inString, boolean ignoreCallbacks)
						throws ConnectionException, RequestException, AccessException {
		RpcPacketDispatcher dispatcher = null;
		RpcConnection rpcConnection = null;
		if (cmdName == null) {
			throw new NullPointerError(
					"Null command name passed to execStreamCmd");
		}
		
		if (!this.connected) {
			throw new ConnectionNotConnectedException(
					"Not currently connected to a Perforce server");
		}
		
		try {	
			int cmdCallBackKey = this.nextCmdCallBackKey.incrementAndGet();
			long startTime = System.currentTimeMillis();
			dispatcher = new RpcPacketDispatcher(props, this);
			rpcConnection = new RpcStreamConnection(serverHost, serverPort,
					props, this.serverStats, this.charset, null, this.socketPool,
					this.secure, this.rsh);
			ProtocolCommand protocolSpecs = new ProtocolCommand();
			if (inMap != null && ClientLineEnding.CONVERT_TEXT) {
				ClientLineEnding.convertMap(inMap);
			}
			ExternalEnv env = setupCmd(dispatcher, rpcConnection, protocolSpecs,
											cmdName.toLowerCase(Locale.ENGLISH),cmdArgs, inMap, ignoreCallbacks, cmdCallBackKey, true);
			CommandEnv cmdEnv = new CommandEnv(
											this,
											new RpcCmdSpec(
													cmdName.toLowerCase(Locale.ENGLISH),
													cmdArgs,
													getAuthTicket(),
													inMap,
													inString,
													env),
											rpcConnection,
											protocolSpecs,
											this.serverProtocolMap,
											this.progressCallback,
											cmdCallBackKey,
											writeInPlace(cmdName),
											this.isNonCheckedSyncs());
			cmdEnv.setDontWriteTicket(isDontWriteTicket(cmdName.toLowerCase(Locale.ENGLISH), cmdArgs));
			cmdEnv.setFieldRule(getRpcPacketFieldRule(inMap, CmdSpec.getValidP4JCmdSpec(cmdName)));
			cmdEnv.setStreamCmd(true);

			List<Map<String, Object>> retMapList = dispatcher.dispatch(cmdEnv);
			
			long endTime = System.currentTimeMillis();
			
			if (!ignoreCallbacks && (this.commandCallback != null)) {
				this.processCmdCallbacks(cmdCallBackKey, endTime - startTime, retMapList);
			}

			if ((retMapList != null) && (retMapList.size() != 0)) {
				for (Map<String, Object> map : retMapList) {
					ResultMapParser.handleErrorStr(map);
					ResultMapParser.handleWarningStr(map);
				}
			}

			RpcOutputStream outStream = (RpcOutputStream) cmdEnv.getStateMap().get(
					RpcServer.RPC_TMP_OUTFILE_STREAM_KEY);
			
			if (outStream != null) {
				outStream.close();
				TempFileInputStream inStream
								= new TempFileInputStream(outStream.getFile());
				return inStream;
			}
			
			return null;
			
		} catch (BufferOverflowException exc) {
			Log.error("RPC Buffer overflow: " + exc.getLocalizedMessage());
			Log.exception(exc);
			throw new P4JavaError("RPC Buffer overflow: " + exc.getLocalizedMessage());
		} catch (ConnectionNotConnectedException cnce) {
			this.connected = false;
			this.status = ServerStatus.ERROR;
			throw cnce;
		} catch (IOException ioexc) {
			Log.error("RPC I/O error: " + ioexc.getLocalizedMessage());
			Log.exception(ioexc);
			throw new RequestException(
					"I/O error encountered in stream command: "
					+ ioexc.getLocalizedMessage(), ioexc);
		} finally {
			if (rpcConnection != null) {
				rpcConnection.disconnect(dispatcher);
			}
		}
	}

	/**
	 * Factors out the command setup that's common to stream and map commands.
	 */

	protected ExternalEnv setupCmd(RpcPacketDispatcher dispatcher,
				RpcConnection rpcConnection, ProtocolCommand protocolSpecs,
				String cmdName, String[] cmdArgs, Map<String, Object> inMap,
				boolean ignoreCallbacks, int cmdCallBackKey, boolean isStream)
									throws ConnectionException, AccessException, RequestException {
		if (rpcConnection == null) {
			throw new NullPointerError("Null RPC connection in execMapCmd call");
		}
		if (dispatcher == null) {
			throw new NullPointerError("Null RPC dispatcher in execMapCmd call");
		}
		if (protocolSpecs == null) {
			throw new NullPointerError("Null RPC protocol specs in execMapCmd call");
		}
		if (!this.isRelaxCmdNameValidationChecks() && !CmdSpec.isValidP4JCmdSpec(cmdName)) {
			throw new RequestException("command name '"
					+ cmdName + "' unimplemented or unrecognized by p4java");
		}
		
		// Should use tags?
		boolean useTags = useTags(cmdName, cmdArgs, inMap, isStream);
		
		// Check fingerprint
		checkFingerprint(rpcConnection);
		
		ExternalEnv env = new ExternalEnv(
					this.getUsageOptions().getProgramName(),
					this.getUsageOptions().getProgramVersion(),
					this.getClientNameForEnv(),
					this.getUsageOptions().getWorkingDirectory(),
					this.getHostForEnv(),
					this.getServerHostPort(),
					this.getUsageOptions().getTextLanguage(),
					this.getOsTypeForEnv(),
					this.getUserForEnv(),
					this.charsetName != null,
					this.charset
				);
		
		if (!ignoreCallbacks && (this.commandCallback != null)) {
			StringBuilder cmd = new StringBuilder(cmdName);
			for (String argStr : cmdArgs) {
				if (argStr != null) {
					cmd.append(" ");
					cmd.append(argStr);
				}
			}
			this.commandCallback.issuingServerCommand(cmdCallBackKey, cmd.toString());
		}
	
		RpcPacket protPacket = null;
		
		protocolSpecs.setClientApiLevel(this.clientApiLevel);
		protocolSpecs.setClientCmpFile(false);
		protocolSpecs.setServerApiLevel(this.serverApiLevel);
		protocolSpecs.setApplicationName(this.applicationName);
		protocolSpecs.setSendBufSize(rpcConnection.getSystemSendBufferSize());
		protocolSpecs.setRecvBufSize(rpcConnection.getSystemRecvBufferSize());
		protocolSpecs.setUseTags(useTags);
		protocolSpecs.setEnableStreams(this.enableStreams);
		protocolSpecs.setEnableGraph(this.enableGraph);
		protocolSpecs.setEnableTracking(this.enableTracking);
		protocolSpecs.setEnableProgress(this.enableProgress);
		protocolSpecs.setQuietMode(this.quietMode);
		// Set the 'host' (P4HOST) and 'port' (P4PORT) protocol parameters
		protocolSpecs.setHost(env.getHost());
		protocolSpecs.setPort(env.getPort());
		
		if (props.containsKey(RpcFunctionMapKey.IPADDR)
		        && props.containsKey(RpcFunctionMapKey.SVRNAME)
		        && props.containsKey(RpcFunctionMapKey.PORT)) {
			protocolSpecs.setIpAddr(props.getProperty(RpcFunctionMapKey.IPADDR));
		}

		protPacket = RpcPacket.constructRpcPacket(
								RpcFunctionSpec.PROTOCOL_PROTOCOL,
								protocolSpecs.asMap(),
								null);
		
		RpcFunctionSpec name = RpcFunctionSpec.decodeFromEndUserCmd(cmdName,
													this.isRelaxCmdNameValidationChecks());
		
		RpcPacket cmdPacket = RpcPacket.constructRpcPacket(name, cmdName, cmdArgs,
														env);
		// Append the "tag" argument before the function name
		if (useTags) {
			cmdPacket.setMapArgs(this.cmdMapArgs);
		}

		// On each command message sent to the server (i.e. "user-foo")
        // a variable "progress" should be set to 1 to indicate that
        // the server should send progress messages to the client if they
        // are available for that command.
		if (this.enableProgress) {
			Map<String, Object> valMap = new HashMap<String, Object>();
			if (cmdPacket.getMapArgs() != null) {
				valMap.putAll(cmdPacket.getMapArgs());
			}
			valMap.put(ProtocolCommand.RPC_ARGNAME_PROTOCOL_ENABLE_PROGRESS, "1");
			cmdPacket.setMapArgs(valMap);
		}
		
		if (protPacket == null) {
			rpcConnection.putRpcPacket(cmdPacket);
		} else {
			rpcConnection.putRpcPackets(new RpcPacket[] {protPacket, cmdPacket});
		}
		
		return env;
	}

	/**
	 * Get server address object
	 * 
	 * @return server address object
	 */
	public IServerAddress getServerAddressDetails() {
		ServerAddressBuilder builder = new ServerAddressBuilder();
		builder.setRsh(rsh);
		builder.setHost(serverHost);
		builder.setPort(serverPort);
		if (rsh != null) {
			builder.setProtocol(Protocol.P4JRSH);
		} else if (secure) {
			builder.setProtocol(Protocol.P4JRPCSSL);
		} else {
			builder.setProtocol(Protocol.P4JRPC);
		}
		
		return builder.build();
	}
}
