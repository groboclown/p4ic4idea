/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

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
import com.perforce.p4java.impl.mapbased.rpc.stream.RpcStreamConnection;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcOutputStream;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IServerAddress.Protocol;
import com.perforce.p4java.server.ServerStatus;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;

/**
 * NTS (non-thread-safe) version of the P4Java RPC implementation.<p>
 * 
 * By "not thread safe" we really mean that it's up to the consumer to
 * synchronize calls to the server object; if you want a thread-safe
 * implementation see the OneShotServerImpl class (which is
 * marginally slower but thread safe). The intention here is that if
 * you can guarantee that only one thread at a time will access
 * the connect / disconnect and exec series of methods on this object,
 * you can use this class, which will typically have lower network
 * connection latency than the OneShotServerImpl implementation;
 * overll throughput, though, should be roughly comparable for both
 * implementations over time.
 */

public class NtsServerImpl extends RpcServer {
	/**
	 * The short-form name (display name) of this implementation.
	 */
	public static final String SCREEN_NAME = "Native RPC (Experimental)";
	
	/**
	 * Implementation-specific comments (dependencies, limitations, etc.).
	 */
	public static final String IMPL_COMMENTS
					= "Experimental Java-native RPC standalone P4Java implementation."
					+ " Requires JDK 6 or later, full Java NIO support, and "
					+ "external thread synchronization. Not for the faint-hearted.";
	
	/**
	 * The specific protocol name to be used in URIs for this implementation.
	 */
	public static final String PROTOCOL_NAME = Protocol.P4JRPCNTS.toString();
	
	/**
	 * The specific SSL protocol name to be used in URIs for this implementation.
	 */
	public static final String SSL_PROTOCOL_NAME = Protocol.P4JRPCNTSSSL.toString();

	/**
	 * The minimum Perforce server level required by this implementation.
	 */
	public static final int MINIMUM_SUPPORTED_SERVER_LEVEL = 20052;
	
	/**
	 * True IFF this is the default implementation. There must be only one of
	 * these...
	 */
	public static final boolean DEFAULT_STATUS = false;

	/**
	 * What we use as a P4JTracer trace prefix for methods here.
	 */
	public static final String TRACE_PREFIX = "NtsServerImpl";
	
	private boolean currentUseTags = true;
	private boolean haveSentProtocolSpecs = false;
	protected ProtocolCommand protocolSpecs = null;
	
	protected RpcPacketDispatcher dispatcher = null;
	protected RpcConnection rpcConnection = null;
	
	/**
	 * Initialize the server. Basically defers to the superclass after setting
	 * up the required server version.
	 * 
	 * @see com.perforce.p4java.impl.mapbased.rpc.RpcServer#init(java.lang.String, int, java.util.Properties, com.perforce.p4java.option.UsageOptions, boolean)
	 */

	public ServerStatus init(String host, int port, Properties props, UsageOptions opts, boolean secure)
				throws ConfigException, ConnectionException {
		super.init(host, port, props, opts, secure);
		super.minumumSupportedServerVersion = MINIMUM_SUPPORTED_SERVER_LEVEL;
		return status;
	}
	
	/**
	 * Shorthand for the options-based init() above, but with a fasle secure arg.
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

	/**
	 * Try to establish an actual RPC connection to the target Perforce server.
	 * Most of the actual setup work is done in the RpcConnection and
	 * RpcPacketDispatcher constructors, but associated gubbins such as
	 * auto login, etc., are done in the superclass.
	 * 
	 * @see com.perforce.p4java.impl.mapbased.server.Server#connect()
	 */
	
	public void connect() throws ConnectionException,
								AccessException, RequestException, ConfigException {
		this.rpcConnection = new RpcStreamConnection(serverHost, serverPort, props,
												this.serverStats, this.charset, this.secure);
		this.dispatcher = new RpcPacketDispatcher(props, this);
				
		Log.info("RPC connection to Perforce server "
				+ serverHost + ":" + serverPort + " established");
		
		super.connect();
	}
	
	/**
	 * Try to cleanly disconnect from the Perforce server at the other end
	 * of the current connection (with the emphasis on "cleanly"). This
	 * should theoretically include sending a release2 message, but we
	 * don't always get the chance to do that.
	 * 
	 * @see com.perforce.p4java.impl.mapbased.server.Server#disconnect()
	 */
	public void disconnect() throws ConnectionException,
											AccessException {
		Log.info("Disconnected RPC connection to Perforce server "
							+ this.serverHost + ":" + this.serverPort);
		
		this.dispatcher.shutdown(this.rpcConnection);
		this.rpcConnection.disconnect(this.dispatcher);
		this.haveSentProtocolSpecs = false;
		this.protocolSpecs = null;
		super.disconnect();
	}
	
	/**
	 * Need to override this method at this level as we keep the connection open here...
	 * 
	 * @see com.perforce.p4java.impl.mapbased.server.Server#setCharsetName(java.lang.String)
	 */
	public boolean setCharsetName(String charsetName) throws UnsupportedCharsetException {
		boolean retVal = super.setCharsetName(charsetName);
		this.rpcConnection.setClientCharset(this.charset);
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execMapCmd(java.lang.String, java.lang.String[], java.util.Map)
	 */
	@Override
	public Map<String, Object>[] execMapCmd(String cmdName, String[] cmdArgs,
			Map<String, Object> inMap) throws ConnectionException,
			AccessException, RequestException {
		return this.execMapCmd(cmdName, cmdArgs, inMap, null, false, null, 0, null);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execMapCmdList(java.lang.String, java.lang.String[], java.util.Map)
	 */
	@Override
	public List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs,
			Map<String, Object> inMap) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, inMap, null, false, null, 0, null);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execMapCmdList(java.lang.String, java.lang.String[], java.util.Map, com.perforce.p4java.server.callback.IFilterCallback)
	 */
	@Override
	public List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs,
			Map<String, Object> inMap, IFilterCallback filterCallback) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, inMap, null, false, null, 0, filterCallback);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execQuietMapCmd(java.lang.String, java.lang.String[], java.util.Map)
	 */
	@Override
	public Map<String, Object>[] execQuietMapCmd(String cmdName,
			String[] cmdArgs, Map<String, Object> inMap)
			throws ConnectionException, RequestException,
			AccessException {
		return this.execMapCmd(cmdName, cmdArgs, inMap, null, true, null, 0, null);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execQuietMapCmdList(java.lang.String, java.lang.String[], java.util.Map)
	 */
	@Override
	public List<Map<String, Object>> execQuietMapCmdList(String cmdName,
			String[] cmdArgs, Map<String, Object> inMap) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, inMap, null, true, null, 0, null);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringMapCmd(java.lang.String, java.lang.String[], java.lang.String)
	 */
	public Map<String, Object>[] execInputStringMapCmd(String cmdName,
			String[] cmdArgs, String inString) throws P4JavaException {
		return this.execMapCmd(cmdName, cmdArgs, null, inString, true, null, 0, null);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringMapCmdList(java.lang.String, java.lang.String[], java.lang.String)
	 */
	public List<Map<String, Object>> execInputStringMapCmdList(String cmdName,
			String[] cmdArgs, String inString) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, null, inString, true, null, 0, null);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringMapCmdList(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IFilterCallback)
	 */
	public List<Map<String, Object>> execInputStringMapCmdList(String cmdName,
			String[] cmdArgs, String inString, IFilterCallback filterCallback) throws P4JavaException {
		return this.execMapCmdList(cmdName, cmdArgs, null, inString, true, null, 0, filterCallback);
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
		execMapCmd(cmdName, cmdArgs, null, inString, false, callback, key, null);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringStreamingMapCmd(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IStreamingCallback, int)
 	 */
	public void execInputStringStreamingMapCmd(String cmdName,
			String[] cmdArgs, String inString, IStreamingCallback callback,
			int key) throws P4JavaException {
		execMapCmd(cmdName, cmdArgs, null, inString, false, callback, key, null);
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object>[] execMapCmd(String cmdName,
			String[] cmdArgs, Map<String, Object> inMap, String inString, boolean ignoreCallbacks,
			IStreamingCallback callback, int callbackKey, IFilterCallback filterCallback)
				throws ConnectionException, AccessException, RequestException {
		List<Map<String, Object>> results = execMapCmdList(cmdName, cmdArgs, inMap, inString, ignoreCallbacks, callback, callbackKey, filterCallback);
		if (results != null) {
			return results.toArray(new HashMap[results.size()]);
		}
		
		return null;
	}

	protected List<Map<String, Object>> execMapCmdList(String cmdName,
			String[] cmdArgs, Map<String, Object> inMap, String inString, boolean ignoreCallbacks,
			IStreamingCallback callback, int callbackKey, IFilterCallback filterCallback)
				throws ConnectionException, AccessException, RequestException {
		
		CommandEnv cmdEnv = null;
		
		try {
			int cmdCallBackKey = this.nextCmdCallBackKey.incrementAndGet();
			long startTime = System.currentTimeMillis();
			if (inMap != null && ClientLineEnding.CONVERT_TEXT) {
				ClientLineEnding.convertMap(inMap);
			}
			ExternalEnv env = setupCmd(cmdName.toLowerCase(Locale.ENGLISH), cmdArgs, inMap, ignoreCallbacks, cmdCallBackKey, false);
			cmdEnv = new CommandEnv(
					new RpcCmdSpec(
							cmdName,
							cmdArgs,
							getAuthTicket(),
							inMap,
							inString,
							env),
					this.rpcConnection,
					this.protocolSpecs,
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
			if (callback != null) {
				try {
					callback.startResults(callbackKey);
				} catch (P4JavaException exc) {
					Log.error("streaming callback startResults method threw exception: " + exc.getLocalizedMessage());
					Log.exception(exc);
				}
			}
			List<Map<String, Object>> resultMaps = this.dispatcher.dispatch(cmdEnv);
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
				this.processCmdCallbacks(cmdCallBackKey, endTime - startTime, resultMaps);
			}
			
			// Close RPC output stream
			RpcOutputStream outStream = (RpcOutputStream) cmdEnv.getStateMap().get(
					RpcServer.RPC_TMP_OUTFILE_STREAM_KEY);
			if (outStream != null) {
				outStream.close();
			}
			
			return resultMaps;
			
		} catch (BufferOverflowException exc) {
			Log.error("RPC Buffer overflow: " + exc.getLocalizedMessage());
			Log.exception(exc);
			throw new P4JavaError("RPC Buffer overflow: " + exc.getLocalizedMessage(), exc);
		} catch (ConnectionNotConnectedException cnce) {
			this.connected = false;
			this.status = ServerStatus.ERROR;
			throw cnce;
		} catch (IOException ioexc) {
			Log.error("I/O error encountered in stream command: " + ioexc.getLocalizedMessage());
			Log.exception(ioexc);
			throw new RequestException(
					"I/O error encountered in stream command: "
					+ ioexc.getLocalizedMessage(), ioexc);
		} finally {
			// Handle user cancelled command
			if (cmdEnv != null && cmdEnv.isUserCanceled()) {
				if (rpcConnection != null) {
					rpcConnection.disconnect(dispatcher);
					try {
						connect();
					} catch (ConfigException cfe) {
						this.connected = false;
						this.status = ServerStatus.ERROR;
						throw new ConnectionNotConnectedException(cfe);
					}
				}
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
		
		execMapCmdList(cmdName, cmdArgs, inMap, null, false, callback, key, null);
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
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execStreamCmd(java.lang.String, java.lang.String[], java.util.Map)
	 */
	@Override
	public InputStream execStreamCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
			throws P4JavaException {
		return this.execStreamCmd(cmdName, cmdArgs, inMap, null, false);
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.Server#execInputStringStreamCmd(java.lang.String, java.lang.String)
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
		if (cmdName == null) {
			throw new NullPointerError(
					"Null command name passed to execStreamCmd");
		}
		
		if (!this.connected) {
			throw new ConnectionNotConnectedException(
					"Not currently connected to a Perforce server");
		}
		
		CommandEnv cmdEnv = null;
		
		try {
			int cmdCallBackKey = this.nextCmdCallBackKey.incrementAndGet();
			long startTime = System.currentTimeMillis();
			if (inMap != null && ClientLineEnding.CONVERT_TEXT) {
				ClientLineEnding.convertMap(inMap);
			}
			ExternalEnv env = setupCmd(cmdName,cmdArgs, inMap, ignoreCallbacks, cmdCallBackKey, true);
			cmdEnv = new CommandEnv(
									new RpcCmdSpec(
											cmdName,
											cmdArgs,
											getAuthTicket(),
											inMap,
											inString,
											env),
									this.rpcConnection,
									this.protocolSpecs,
									this.serverProtocolMap,
									this.progressCallback,
									cmdCallBackKey,
									writeInPlace(cmdName),
									this.isNonCheckedSyncs());
			cmdEnv.setDontWriteTicket(isDontWriteTicket(cmdName.toLowerCase(Locale.ENGLISH), cmdArgs));
			cmdEnv.setFieldRule(getRpcPacketFieldRule(inMap, CmdSpec.getValidP4JCmdSpec(cmdName)));
			cmdEnv.setStreamCmd(true);

			List<Map<String, Object>> resultMaps = this.dispatcher.dispatch(cmdEnv);
			
			long endTime = System.currentTimeMillis();
			
			if (!ignoreCallbacks && (this.commandCallback != null)) {
				this.processCmdCallbacks(cmdCallBackKey, endTime - startTime, resultMaps);
			}
			
			if ((resultMaps != null) && (resultMaps.size() != 0)) {
				for (Map<String, Object> map : resultMaps) {
					if (map != null) {
						String errStr = this.getErrorStr(map);
						if (errStr != null) {
							if (isAuthFail(errStr)) {
								throw new AccessException(errStr);
							} else {
								throw new RequestException(errStr, (String) map.get("code0"));
							}
						}
					}
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
			throw new P4JavaError("RPC Buffer overflow: " + exc.getLocalizedMessage(), exc);
		} catch (ConnectionNotConnectedException cnce) {
			this.connected = false;
			this.status = ServerStatus.ERROR;
			throw cnce;
		} catch (IOException ioexc) {
			Log.error("I/O error encountered in stream command: " + ioexc.getLocalizedMessage());
			Log.exception(ioexc);
			throw new RequestException(
					"I/O error encountered in stream command: "
					+ ioexc.getLocalizedMessage(), ioexc);
		} finally {
			// Handle user cancelled command
			if (cmdEnv != null && cmdEnv.isUserCanceled()) {
				if (rpcConnection != null) {
					rpcConnection.disconnect(dispatcher);
					try {
						connect();
					} catch (ConfigException cfe) {
						this.connected = false;
						this.status = ServerStatus.ERROR;
						throw new ConnectionNotConnectedException(cfe);
					}
				}
			}
		}
	}
	
	/**
	 * Factors out the command setup that's common to stream and map commands.
	 */

	protected ExternalEnv setupCmd(String cmdName, String[] cmdArgs,
			Map<String, Object> inMap, boolean ignoreCallbacks, int cmdCallBackKey,
			boolean isStream) throws ConnectionException, AccessException, RequestException {

		if (this.rpcConnection == null) {
			throw new NullPointerError("Null RPC connection in execMapCmd call");
		}
		if (this.dispatcher == null) {
			throw new NullPointerError("Null RPC dispatcher in execMapCmd call");
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
		
		// If the "useTags" state had changed from the previous command we must
		// send the protocol again.
		if (!this.haveSentProtocolSpecs || (this.currentUseTags != useTags)) {

			// Piggy-back the sending of the protocol setup command
			this.protocolSpecs = new ProtocolCommand();
			this.protocolSpecs.setClientApiLevel(this.clientApiLevel);
			this.protocolSpecs.setClientCmpFile(false);
			this.protocolSpecs.setServerApiLevel(this.serverApiLevel);
			this.protocolSpecs.setApplicationName(this.applicationName);
			this.protocolSpecs.setSendBufSize(rpcConnection.getSystemSendBufferSize());
			this.protocolSpecs.setRecvBufSize(rpcConnection.getSystemRecvBufferSize());
			this.protocolSpecs.setUseTags(useTags);
			this.protocolSpecs.setEnableStreams(true);
			this.protocolSpecs.setEnableTracking(this.enableTracking);
			this.protocolSpecs.setEnableProgress(this.enableProgress);
			this.protocolSpecs.setQuietMode(this.quietMode);
			// Set the 'host' (P4HOST) and 'port' (P4PORT) protocol parameters
			protocolSpecs.setHost(env.getHost());
			protocolSpecs.setPort(env.getPort());

			protPacket = RpcPacket.constructRpcPacket(
									RpcFunctionSpec.PROTOCOL_PROTOCOL,
									this.protocolSpecs.asMap(),
									null);

			this.currentUseTags = useTags;
			this.haveSentProtocolSpecs = true;
		}
		
		RpcFunctionSpec name = RpcFunctionSpec.decodeFromEndUserCmd(cmdName,
									this.isRelaxCmdNameValidationChecks());
		
		// For historical reasons, we need to special-case the login command
		//if (name == RpcFunctionSpec.USER_LOGIN) {
		//	cmdArgs = new String[] {"-p"};
		//}	

		RpcPacket cmdPacket = RpcPacket.constructRpcPacket(
										name,
										cmdName,
										cmdArgs,
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
			this.rpcConnection.putRpcPacket(cmdPacket);
		} else {
			this.rpcConnection.putRpcPackets(new RpcPacket[] {protPacket, cmdPacket});
		}
		
		return env;
	}
	
	public RpcConnection getRpcConnection() {
		return this.rpcConnection;
	}

	public void setRpcConnection(RpcConnection rpcConnection) {
		this.rpcConnection = rpcConnection;
	}
}
