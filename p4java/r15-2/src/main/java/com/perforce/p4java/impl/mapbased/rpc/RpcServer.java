/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.env.PerforceEnvironment;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientTrust;
import com.perforce.p4java.impl.mapbased.rpc.func.proto.PerformanceMonitor;
import com.perforce.p4java.impl.mapbased.rpc.func.proto.ProtocolCommand;
import com.perforce.p4java.impl.mapbased.rpc.helper.RpcUserAuthCounter;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldRule;
import com.perforce.p4java.impl.mapbased.rpc.stream.RpcStreamConnection;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.AbstractAuthHelper;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.Fingerprint;
import com.perforce.p4java.server.FingerprintsHelper;
import com.perforce.p4java.server.IServerAddress;
import com.perforce.p4java.server.IServerImplMetadata;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerStatus;
import com.perforce.p4java.util.PropertiesHelper;

/**
 * RPC-based Perforce server implementation superclass.
 */

public abstract class RpcServer extends Server {

        /**
         * The implementation type of this implementation.
         */
        public static final IServerImplMetadata.ImplType IMPL_TYPE
                                                                = IServerImplMetadata.ImplType.NATIVE_RPC;

        /**
         * The default string sent to the Perforce server in the protocol
         * command defining the client's program name. This can be set with the
         * IServer interface.
         */
        public static final String DEFAULT_PROG_NAME = "p4jrpc";

        /**
         * The default string sent to the Perforce server in the protocol
         * command defining the client's program version. This can be set with the
         * IServer interface.
         */
        public static final String DEFAULT_PROG_VERSION = "Beta 1.0";

        /**
         * Default Perforce client API level; 79 represents 2015.2 capabilities.
         * Don't change this unless you know what you're doing. Note that this
         * is a default for most commands; some commands dynamically bump up
         * the level for the command's duration.
         */
        public static final int DEFAULT_CLIENT_API_LEVEL = 79;	// 2015.2
        														// p4/msgs/p4tagl.cc
																// p4/client/client.cc
																// p4/server/rhservice.h

        /**
         * Default Perforce server API level; 99999 apparently means "whatever...".
         * Don't change this unless you know what you're doing.
         */
        public static final int DEFAULT_SERVER_API_LEVEL = 99999;	// As picked off the wire for C++ API
        															// p4/client/clientmain.cc
																	// p4/server/rhservice.cc

        /**
         * Signifies whether or not we use tagged output. Don't change this unless
         * you like weird incomprehensible errors and days of debugging.
         */
        public static final boolean RPC_TAGS_USED = true;

        /**
         * Signifies whether or not the client is capable of handling streams.
         */
        public static final boolean RPC_ENABLE_STREAMS = true;

        /**
         * The system properties key for the JVM's current directory.
         */
        public static final String RPC_ENV_CWD_KEY = "user.dir";

        /**
         * The system properties key for the OS name.
         */
        public static final String RPC_ENV_OS_NAME_KEY = "os.name";

        /**
         * RPC_ENV_OS_NAME_KEY property value prefix for Windows systems.
         */
        public static final String RPC_ENV_WINDOWS_PREFIX = "windows";

        /**
         * What we use in the RPC environment packet to signal to the
         * Perforce server that we're a Windows box.
         */
        public static final String RPC_ENV_WINDOWS_SPEC = "NT"; // sic

        /**
         * What we use in the RPC environment packet to signal to the
         * Perforce server that we're a NON-Windows box.
         */

        public static final String RPC_ENV_UNIX_SPEC = "UNIX";

        /**
         * What we use in the RPC environment packet to signal to the Perforce
         * server that we don't have a client set yet or don't know what it is.
         */
        public static final String RPC_ENV_NOCLIENT_SPEC = "unknownclient";

        /**
         * What we use in the RPC environment packet to signal to the Perforce
         * server that we don't have a hostname set yet or don't know what it is.
         */
        public static final String RPC_ENV_NOHOST_SPEC = "nohost"; // as cribbed from the C++ API...

        /**
         * What we use in the RPC environment packet to signal to the Perforce
         * server that we don't have a client set yet or don't know what it is.
         */
        public static final String RPC_ENV_NOUSER_SPEC = "nouser"; // as cribbed from the C++ API...

        /**
         * What we use as a P4JTracer trace prefix for methods here.
         */
        public static final String TRACE_PREFIX = "RpcServer";

        /**
         * Used to key temporary output streams in the command environment's
         * state map for things like getFileContents(), etc., using the execStreamCmd
         * method(s).
         */
        public static final String RPC_TMP_OUTFILE_STREAM_KEY = "";

        /**
         * Use to key converter to use out of state map
         */
        public static final String RPC_TMP_CONVERTER_KEY = "RPC_TMP_CONVERTER_KEY";

        protected String localHostName = null;	// intended to be just the unqualified name...
        protected int clientApiLevel = DEFAULT_CLIENT_API_LEVEL;
        protected int serverApiLevel = DEFAULT_SERVER_API_LEVEL;
        protected String applicationName = null;	// Application name

        private static final String AUTH_FAIL_STRING_1 = "Single sign-on on client failed"; // SSO failure
        private static final String AUTH_FAIL_STRING_2 = "Password invalid";

        private static final String[] accessErrMsgs = {
                        CORE_AUTH_FAIL_STRING_1,
                        CORE_AUTH_FAIL_STRING_2,
                        CORE_AUTH_FAIL_STRING_3,
                        CORE_AUTH_FAIL_STRING_4,
                        AUTH_FAIL_STRING_1,
                        AUTH_FAIL_STRING_2
        };

        private static final String PASSWORD_NOT_SET_STRING = "no password set for this user";
    	
        protected long connectionStart = 0;

        protected Map<String, Object> serverProtocolMap = new HashMap<String, Object>();

        private PerformanceMonitor perfMonitor = new PerformanceMonitor();

        protected ServerStats serverStats = null;

        protected String serverId = null;

    	protected Map<String, String> secretKeys = new HashMap<String, String>();

        protected ClientTrust clientTrust = null;
        
        protected String ticketsFilePath = null;
        
        protected String trustFilePath = null;

    	protected int authFileLockTry = 0;
    	protected long authFileLockDelay = 0;
    	protected long authFileLockWait = 0;
        
        protected RpcUserAuthCounter authCounter = new RpcUserAuthCounter();
        
        protected IServerAddress rpcServerAddress = null;
        
    	/**
         * The RPC command args before the function name (i.e. "tag")
         */
        protected Map<String, Object> cmdMapArgs = null;

        /**
         * If true, relax the command name validation checks done
         * in the RPC layer. This is dangerous, and any use of this
         * should only be done if you know what you're doing and you're
         * able to deal with the consequences (which won't be spelled
         * out here).
         */
        protected boolean relaxCmdNameValidationChecks = false;

        /**
         * The default init sets up things like host names, etc., and fails if
         * we can't establish some pretty basic things at connect time. Does
         * <i>not</i> attempt to actually connect to the target Perforce
         * server -- this is left for the connect() call, below.
         *
         * @see com.perforce.p4java.impl.mapbased.server.Server#init(java.lang.String, int, java.util.Properties)
         */

        public ServerStatus init(String host, int port, Properties props)
                        throws ConfigException, ConnectionException {
                return init(host, port, props, null);
        }

        public ServerStatus init(String host, int port, Properties props, UsageOptions opts)
        				throws ConfigException, ConnectionException {
            return init(host, port, props, opts, false);
        }

        public ServerStatus init(String host, int port, Properties props, UsageOptions opts, boolean secure)
                                throws ConfigException, ConnectionException {
                super.init(host, port, props, opts, secure);
                try {
            			this.cmdMapArgs = new HashMap<String, Object>();
            			this.cmdMapArgs.put(ProtocolCommand.RPC_ARGNAME_PROTOCOL_ZTAGS, "");
                        this.relaxCmdNameValidationChecks = RpcPropertyDefs.getPropertyAsBoolean(props,
                                                                RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK, false);
                        this.applicationName = RpcPropertyDefs.getProperty(props,
                                								RpcPropertyDefs.RPC_APPLICATION_NAME_NICK, null);
                        if (this.getUsageOptions().getHostName() != null) {
                                // This had better reflect reality...
                                this.localHostName = this.getUsageOptions().getHostName();
                        } else {
                                this.localHostName = java.net.InetAddress.getLocalHost().getHostName();
                        }
                        if (this.localHostName == null) {
                                throw new NullPointerError("Null client host name in RPC connection init");
                        }
                        if (!this.useAuthMemoryStore) {
		                        // Search properties for ticket file path, fix for job035376
		                        this.ticketsFilePath = this.props.getProperty(PropertyDefs.TICKET_PATH_KEY_SHORT_FORM,
		                                                             this.props.getProperty(PropertyDefs.TICKET_PATH_KEY));
		                        // Search environment variable
		                        if (this.ticketsFilePath == null) {
		                        		this.ticketsFilePath = PerforceEnvironment.getP4Tickets();
		                        }
		                        // Search standard OS location
		                        if (this.ticketsFilePath == null) {
		                        		this.ticketsFilePath = getDefaultP4TicketsFile();
		                        }
		                        
		                        // Search properties for trust file path
		                        this.trustFilePath = this.props.getProperty(PropertyDefs.TRUST_PATH_KEY_SHORT_FORM,
		                                                           this.props.getProperty(PropertyDefs.TRUST_PATH_KEY));
		                        // Search environment variable
		                        if (this.trustFilePath == null) {
		                                this.trustFilePath = PerforceEnvironment.getP4Trust();

		                        }
		                        // Search standard OS location
		                        if (this.trustFilePath == null) {
		                                this.trustFilePath = getDefaultP4TrustFile();
		                        }
                        }
                        this.serverStats = new ServerStats();
                        // Auth file lock handling properties
                        this.authFileLockTry = PropertiesHelper.getPropertyAsInt(props,
                        		new String[] {PropertyDefs.AUTH_FILE_LOCK_TRY_KEY_SHORT_FORM,
                        				PropertyDefs.AUTH_FILE_LOCK_TRY_KEY},
                        		AbstractAuthHelper.DEFAULT_LOCK_TRY);
                        this.authFileLockDelay = PropertiesHelper.getPropertyAsLong(props,
                        		new String[] {PropertyDefs.AUTH_FILE_LOCK_DELAY_KEY_SHORT_FORM,
                        				PropertyDefs.AUTH_FILE_LOCK_DELAY_KEY},
                        		AbstractAuthHelper.DEFAULT_LOCK_DELAY);
                        this.authFileLockWait = PropertiesHelper.getPropertyAsLong(props,
                        		new String[] {PropertyDefs.AUTH_FILE_LOCK_WAIT_KEY_SHORT_FORM,
                        				PropertyDefs.AUTH_FILE_LOCK_WAIT_KEY},
                        		AbstractAuthHelper.DEFAULT_LOCK_WAIT);
                } catch (UnknownHostException uhe) {
                        throw new ConfigException("Unable to determine client host name: "
                                        + uhe.getLocalizedMessage());
                }

                // Initialize client trust
                clientTrust = new ClientTrust(this);
                
                return status;
        }

        /**
         * Check the fingerprint of the Perforce server SSL connection
         * 
         * @throws ConnectionException 
         */
        protected void checkFingerprint(RpcConnection rpcConnection) throws ConnectionException {
        	if (rpcConnection != null && rpcConnection.isSecure() && !rpcConnection.isTrusted()) {
	        	if (rpcConnection.getFingerprint() == null) {
	        		throw new ConnectionException("Null fingerprint for this Perforce SSL connection");
	        	}
 
    			boolean fpExist = clientTrust.fingerprintExists(rpcConnection.getServerIpPort(),
    					ClientTrust.FINGERPRINT_USER_NAME);
    			boolean fpReplaceExist = clientTrust.fingerprintExists(rpcConnection.getServerIpPort(),
    					ClientTrust.FINGERPRINT_REPLACEMENT_USER_NAME);

    			boolean fpMatch = clientTrust.fingerprintMatches(rpcConnection.getServerIpPort(),
    					ClientTrust.FINGERPRINT_USER_NAME, rpcConnection.getFingerprint());
    			boolean fpReplaceMatch = clientTrust.fingerprintMatches(rpcConnection.getServerIpPort(),
    					ClientTrust.FINGERPRINT_REPLACEMENT_USER_NAME, rpcConnection.getFingerprint());
	        	
	        	// Not established
       			if ( (!fpExist && !fpReplaceExist) || (!fpExist && !fpReplaceMatch) ) {
       				throw new TrustException(TrustException.Type.NEW_CONNECTION,
       						getServerHostPort(),
       						rpcConnection.getServerIpPort(),
       						rpcConnection.getFingerprint(),
       						clientTrust.getMessages().getMessage(
       						ClientTrust.CLIENT_TRUST_WARNING_NOT_ESTABLISHED,
       						new Object[] { getServerHostPort(), rpcConnection.getFingerprint() }) +
       						clientTrust.getMessages().getMessage(
       						ClientTrust.CLIENT_TRUST_EXCEPTION_NEW_CONNECTION)     						
       						);
       			}
       			// New key
       			if (!fpMatch && !fpReplaceMatch) {
       				throw new TrustException(TrustException.Type.NEW_KEY,
       						getServerHostPort(),
       						rpcConnection.getServerIpPort(),
       						rpcConnection.getFingerprint(),
       						clientTrust.getMessages().getMessage(
       						ClientTrust.CLIENT_TRUST_WARNING_NEW_KEY,
       						new Object[] { getServerHostPort(), rpcConnection.getFingerprint() }) +
       						clientTrust.getMessages().getMessage(
       						ClientTrust.CLIENT_TRUST_EXCEPTION_NEW_KEY)     						
       						);
       			}

       			// Use replacement fingerprint
    			if ( (!fpExist || !fpMatch) && (fpReplaceExist && fpReplaceMatch) ) {
       				// Install/override fingerprint
    	    		clientTrust.installFingerprint(rpcConnection.getServerIpPort(), ClientTrust.FINGERPRINT_USER_NAME, rpcConnection.getFingerprint());
       				// Remove the replacement
    	    		clientTrust.removeFingerprint(rpcConnection.getServerIpPort(), ClientTrust.FINGERPRINT_REPLACEMENT_USER_NAME);
       			}
       			
       			// Trust this connection
       			rpcConnection.setTrusted(true);
        	}
        }
        
        /**
         * Try to establish an actual RPC connection to the target Perforce server.
         * Most of the actual setup work is done in the RpcConnection and
         * RpcPacketDispatcher constructors, but associated gubbins such as
         * auto login, etc., are done in the superclass.
         *
         * @see com.perforce.p4java.impl.mapbased.server.Server#connect()
         */

        public void connect() throws ConnectionException, AccessException, RequestException, ConfigException {
        	this.connectionStart = System.currentTimeMillis();
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
        public void disconnect() throws ConnectionException, AccessException {
                super.disconnect();

                if (this.connectionStart != 0) {
                        Log.stats("RPC connection connected for "
                                        + (System.currentTimeMillis() - this.connectionStart)
                                        + " msec elapsed time");
                }
                this.serverStats.logStats();

                // Clear up all counts for this RPC server
    		    this.authCounter.clearCount();
        }

        /**
         * @see com.perforce.p4java.server.IServer#supportsSmartMove()
         */
        public boolean supportsSmartMove()  throws ConnectionException,
                                RequestException, AccessException {
                // Return true iff server version >= 2009.1
        		// and move command is not disabled on server

                if (this.serverVersion < 20091)
                	return false;
                IServerInfo info = getServerInfo();
                if (info == null)
                	return false;
                return !info.isMoveDisabled();
        }

        /**
         * @see com.perforce.p4java.server.IOptionsServer#getErrorOrInfoStr(java.util.Map)
         */
        public String getErrorOrInfoStr(Map<String, Object> map) {
                return getString(map, MessageSeverityCode.E_INFO);
        }

        public boolean isInfoMessage(Map<String, Object> map) {
                if (map != null) {
                        return (RpcMessage.getSeverity((String) map.get("code0")) == MessageSeverityCode.E_INFO);
                }
                return false;
        }

        public int getSeverityCode(Map<String, Object> map) {
                // Note: only gets first severity, i.e. code0:

                if ((map != null) && map.containsKey("code0")) {
                        return RpcMessage.getSeverity((String) map.get("code0"));
                }

                return MessageSeverityCode.E_EMPTY;
        }

        protected int getGenericCode(Map<String, Object> map) {
                // Note: only gets first code, i.e. code0:

                if ((map != null) && map.containsKey("code0")) {
                        return RpcMessage.getGeneric((String) map.get("code0"));
                }

                return MessageGenericCode.EV_NONE;
        }

        private String getString(Map<String, Object> map, int minimumCode ) {
                if (map != null) {
                        int index = 0;
                        String code = (String) map.get(RpcMessage.CODE + index);

                        // Return if no code0 key found
                        if (code == null) {
                                return null;
                        }

                        boolean foundCode = false;
                        StringBuilder codeString = new StringBuilder();
                        while (code != null) {
                                int severity = RpcMessage.getSeverity(code);
                                if (severity >= minimumCode) {
                                        foundCode = true;
                                        String fmtStr = (String) map.get(RpcMessage.FMT + index);
                                        if (fmtStr != null) {
                                                if (fmtStr.indexOf('%') != -1) {
                                                        fmtStr = RpcMessage.interpolateArgs(fmtStr, map);
                                                }
                                                // Insert latest message at beginning of error string
                                                // since server structures them this way
                                                codeString.insert(0, fmtStr);
                                                codeString.insert(fmtStr.length(), '\n');
                                        }
                                }
                                index++;
                                code = (String) map.get(RpcMessage.CODE + index);
                        }

                        // Only return a string if at least one severity code was found
                        if (foundCode) {
                                return codeString.toString();
                        }
                }
                return null;
        }

        /**
         * RPC impl errors come across the wire as a map in the form usually like this:
         * <pre>
         * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
         * func=client-Message, user=nouser, code0=822483067
         * </pre>
         * With tags being used for non-error payloads, we can just basically
         * pick up the presence of the code0 entry; if it's there, use fmt0
         * as the format and the other args as appropriate...<p>
         *
         * FIXME: work with multiple code/fmt sets... -- HR.
         *
         * @see com.perforce.p4java.server.IOptionsServer#getErrorStr(java.util.Map)
         */
        public String getErrorStr(Map<String, Object> map) {
                return getString(map, MessageSeverityCode.E_FAILED);
        }

    	/**
    	 * @see com.perforce.p4java.server.IOptionsServer#getInfoStr(java.util.Map)
    	 */
        public String getInfoStr(Map<String, Object> map) {
                if (map != null) {
                        String code0 = (String) map.get("code0");

                        int severity = RpcMessage.getSeverity(code0);

                        if (severity == MessageSeverityCode.E_INFO) {
                                String fmtStr = (String) map.get("fmt0");

                                if (fmtStr == null) {
                                        return "";	// a la the p4 command line version
                                }

                                if (!fmtStr.contains("%")) {
                                        return fmtStr;
                                }

                                return RpcMessage.interpolateArgs(fmtStr, map);
                        }
                }

                return null;
        }

        /**
         * @see com.perforce.p4java.impl.mapbased.server.Server#isAuthFail(java.lang.String)
         */
        @Override
        public boolean isAuthFail(String errStr) {
                if (errStr != null) {
                        for (String str : accessErrMsgs) {
                                if (errStr.contains(str)) {
                                        return true;
                                }
                        }
                }

                return false;
        }

        /**
         * @see com.perforce.p4java.impl.mapbased.server.Server#isLoginNotRequired(java.lang.String)
         */
        @Override
        public boolean isLoginNotRequired(String msgStr) {
                if (msgStr != null) {
	                    if (msgStr.contains(PASSWORD_NOT_SET_STRING)) {
	                            return true;
	                    }
                }

                return false;
        }

        public int getClientApiLevel() {
                return this.clientApiLevel;
        }

        public void setClientApiLevel(int clientApiLevel) {
                this.clientApiLevel = clientApiLevel;
        }

        public String getApplicationName() {
                return this.applicationName;
        }

        public void setApplicationName(String applicationName) {
                this.applicationName = applicationName;
        }
        
        public PerformanceMonitor getPerfMonitor() {
                return this.perfMonitor;
        }

        public void setPerfMonitor(PerformanceMonitor perfMonitor) {
                this.perfMonitor = perfMonitor;
        }

        protected String getOsTypeForEnv() {
                String osName = System.getProperty(RPC_ENV_OS_NAME_KEY);

                if ((osName != null) && osName.toLowerCase(Locale.ENGLISH).contains(RPC_ENV_WINDOWS_PREFIX)){
                        return RPC_ENV_WINDOWS_SPEC;
                }

                return RPC_ENV_UNIX_SPEC; // sic -- as seen in the C++ API...
        }

        protected String getLanguageForEnv() {
                return this.getUsageOptions().getTextLanguage();
        }

        protected String getClientNameForEnv() {
                if (getClientName() != null) {
                        return getClientName();
                } else {
                        return this.getUsageOptions().getUnsetClientName();
                }
        }

        protected String getHostForEnv() {
                if (localHostName != null) {
                        return localHostName;
                }
                return RPC_ENV_NOHOST_SPEC;
        }

        protected String getUserForEnv() {
                if (getUserName() != null) {
                        return getUserName();
                }
                return this.getUsageOptions().getUnsetUserName();
        }


        protected void processCmdCallbacks(int cmdCallBackKey, long timeTaken, List<Map<String, Object>> resultMaps) {
                this.commandCallback.completedServerCommand(cmdCallBackKey, timeTaken);
                if (resultMaps != null) {
                        for (Map<String, Object> map : resultMaps) {
                                String str = getErrorOrInfoStr(map);
                                if (str != null) str = str.trim();
                                int severity = getSeverityCode(map);
                                int generic = getGenericCode(map);

                                if (severity != MessageSeverityCode.E_EMPTY) {
                                        this.commandCallback.receivedServerMessage(cmdCallBackKey, generic, severity,
                                                                                        str);
                                }

                                if (severity == MessageSeverityCode.E_INFO) {
                                        this.commandCallback.receivedServerInfoLine(cmdCallBackKey, str);
                                } else if (severity >= MessageSeverityCode.E_FAILED) {
                                        this.commandCallback.receivedServerErrorLine(cmdCallBackKey, str);
                                }
                        }
                }
        }

        /**
         * Save current ticket returned from {@link #getAuthTicket()}.
         *
         * @see #saveTicket(String)
         * @throws P4JavaException
         */
        public void saveCurrentTicket() throws P4JavaException {
                saveTicket(getAuthTicket());
        }

        /**
         * Save specified auth ticket value as associate with this server's address
         * and configured user returned from {@link #getUserName()}. This will
         * attempt to write an entry to the p4tickets file either specified as the
         * P4TICKETS environment variable or at the OS specific default location. If
         * the ticket value is null then the current entry in the will be cleared.
         *
         * @param ticketValue
         * @throws ConfigException
         */
        public void saveTicket(String ticketValue) throws ConfigException {
        	saveTicket(getUserName(), ticketValue);

        }

        /**
         * Save specified auth ticket value as associate with this server's address
         * and user name from the userName parameter. This will attempt to write
         * an entry to the p4tickets file either specified as the P4TICKETS environment
         * variable or at the OS specific default location. If the ticket value
         * is null then the current entry will be cleared.
         *
         * @param userName
         * @param ticketValue
         * @throws ConfigException
         */
        public void saveTicket(String userName, String ticketValue) throws ConfigException {
        		ConfigException exception = null;

        		// Must downcase the username to find or save a ticket when
        		// connected to a case insensitive server.
        		if (!isCaseSensitive() && userName != null) {
    				userName = userName.toLowerCase();
        		}
        		
        		String serverId = getServerId();
                // Try to save the ticket by server id first if set
                if (serverId != null) {
                        try {
                                AuthTicketsHelper.saveTicket(userName, serverId, ticketValue,
                                		this.ticketsFilePath, this.authFileLockTry,
                                		this.authFileLockDelay, this.authFileLockWait);
                        } catch (IOException e) {
                                exception = new ConfigException(e);
                        }
                }

                // If id is null try to use configured server address
                // If ticket value is null try to clear out any old values by the
                // configured server address
                if (ticketValue == null || serverId == null) {
                        // Try to save the ticket by server address
                        String server = null;
                        if (this.serverHost != null) {
                                server = this.serverHost;
                                if (this.serverPort != UNKNOWN_SERVER_PORT) {
                                        server += ":" + Integer.toString(this.serverPort);
                                }
                        } else if (this.serverPort != UNKNOWN_SERVER_PORT) {
                                server = Integer.toString(this.serverPort);
                        }
                        if (server != null) {
                                try {
                                        AuthTicketsHelper.saveTicket(userName, server, ticketValue,
                                        		this.ticketsFilePath, this.authFileLockTry,
                                        		this.authFileLockDelay, this.authFileLockWait);

                                } catch (IOException e) {
                                        // Use first exception if thrown
                                        if (exception == null) {
                                                exception = new ConfigException(e);
                                        }
                                }
                        }
                }

                //Throw the exception from either save attempt
                if (exception != null) {
                        throw exception;
                }
        }

        /**
         * Get the p4tickets entry value for the current user returned from
         * {@link #getUserName()} and server address based upon a search of either
         * the file found at {@link PropertyDefs#TICKET_PATH_KEY_SHORT_FORM},
         * {@link PropertyDefs#TICKET_PATH_KEY}, the P4TICKETS environment variable
         * or the standard p4tickets file location for the current OS. Will return
         * null if not found or if an error occurred attempt to lookup the value.
         *
         * @param serverId
         *
         * @return - ticket value to get used for {@link #setAuthTicket(String)} or
         *         null if not found.
         */
        public String loadTicket(String serverId) {
                String ticketValue = null;
                String name = getUserName();
                if (name != null) {
                        try {
                                ticketValue = AuthTicketsHelper.getTicketValue(name,
                                                serverId, this.ticketsFilePath);
                        } catch (IOException e) {
                                ticketValue = null;
                        }
                        if (ticketValue == null) {
                                String server = null;
                                if (this.serverHost != null) {
                                        server = this.serverHost;
                                        if (this.serverPort != UNKNOWN_SERVER_PORT) {
                                                server += ":" + Integer.toString(this.serverPort);
                                        }
                                } else if (this.serverPort != UNKNOWN_SERVER_PORT) {
                                        server = Integer.toString(this.serverPort);
                                }
                                try {
                                        ticketValue = AuthTicketsHelper.getTicketValue(name,
                                                        server, this.ticketsFilePath);
                                } catch (IOException e) {
                                        ticketValue = null;
                                }
                        }
                }
                return ticketValue;
        }

        /**
         * Save specified fingerprint value as associate with this server's address.
         * This will attempt to write an entry to the p4trust file either specified
         * as the P4TRUST environment variable or at the OS specific default location.
         * If the fingerprint value is null then the current entry will be cleared.
         *
         * @param rpcConnection
         * @param fingerprintValue
         * @throws ConfigException
         */
        public void saveFingerprint(String serverIpPort, String fingerprintUser, String fingerprintValue) throws ConfigException {
                if (serverIpPort == null || fingerprintUser == null) {
		              return;
                }
        	
                // Save the fingerprint by server IP and port
                try {
                        FingerprintsHelper.saveFingerprint(fingerprintUser,
                        		serverIpPort, fingerprintValue,
                        		this.trustFilePath, this.authFileLockTry,
                        		this.authFileLockDelay, this.authFileLockWait);
                } catch (IOException e) {
                        throw new ConfigException(e);
                }
        }

        /**
         * Get the p4trust entry value for the server IP and port based upon a search
         * of either the file found at {@link PropertyDefs#TRUST_PATH_KEY_SHORT_FORM},
         * {@link PropertyDefs#TRUST_PATH_KEY}, the P4TRUST environment variable
         * or the standard p4trust file location for the current OS. Will return
         * null if not found or if an error occurred attempt to lookup the value.
         *
         * @param serverIpPort
         *
         * @return - fingerprint or null if not found.
         */
        public Fingerprint loadFingerprint(String serverIpPort, String fingerprintUser) {
                if (serverIpPort == null || fingerprintUser == null) {
        		        return null;
                }
        	
                Fingerprint fingerprint = null;

                try {
                        fingerprint = FingerprintsHelper.getFingerprint(fingerprintUser,
                                        serverIpPort, this.trustFilePath);
                } catch (IOException e) {
                        fingerprint = null;
                }

                return fingerprint;
        }

        /**
         * Get the p4trust entries from the file found at {@link PropertyDefs#TRUST_PATH_KEY_SHORT_FORM},
         * {@link PropertyDefs#TRUST_PATH_KEY}, the P4TRUST environment variable
         * or the standard p4trust file location for the current OS. Will return
         * null if nothing found or if an error occurred attempt to lookup the entries.
         *
         * @return - list of fingerprints or null if nothing found.
         */
        public Fingerprint[] loadFingerprints() {
                Fingerprint[] fingerprints = null;
        	
                try {
                	fingerprints = FingerprintsHelper.getFingerprints(this.trustFilePath);
                } catch (IOException e) {
                	// Suppress error
                }

                return fingerprints;
        }

        /**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#getTrust()
    	 */
    	public String getTrust() throws P4JavaException {
    		RpcConnection rpcConnection = null;
    		 
    		try {
    			rpcConnection = new RpcStreamConnection(serverHost, serverPort, props,
    					this.serverStats, this.charset, this.secure);
	
    			return rpcConnection.getFingerprint();
    			
    		} finally {
    			if (rpcConnection != null) {
    				rpcConnection.disconnect(null);
    			}
    		}
    	}

    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#addTrust(com.perforce.p4java.option.server.TrustOptions)
    	 */
    	public String addTrust(TrustOptions opts) throws P4JavaException {

    		return addTrust(null, opts);
    	}
    	
    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#addTrust(java.lang.String)
    	 */
    	public String addTrust(String fingerprintValue) throws P4JavaException {
    		if (fingerprintValue == null) {
    			throw new NullPointerError("null fingerprintValue passed to addTrust");
    		}
    		
    		return addTrust(fingerprintValue, null);
    	}

    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#addTrust(com.perforce.p4java.option.server.TrustOptions, java.lang.String)
    	 */
    	public String addTrust(String fingerprintValue, TrustOptions opts) throws P4JavaException {
    		RpcConnection rpcConnection = null;
    		 
    		try {
    			rpcConnection = new RpcStreamConnection(serverHost, serverPort, props,
    					this.serverStats, this.charset, this.secure);
    			
    			if (opts == null) {
	    			opts = new TrustOptions();
	    		}

    			// Assume '-y' and '-f' options, if specified fingerprint value.
    			if (fingerprintValue != null) {
    				opts.setAutoAccept(true);
    				opts.setForce(true);
    			}

    			String fingerprintUser = opts.isReplacement() ? ClientTrust.FINGERPRINT_REPLACEMENT_USER_NAME : ClientTrust.FINGERPRINT_USER_NAME;
    			String fingerprint = (fingerprintValue != null) ? fingerprintValue : rpcConnection.getFingerprint();

    			String newConnectionWarning = clientTrust.getMessages().getMessage(
   						ClientTrust.CLIENT_TRUST_WARNING_NEW_CONNECTION,
   						new Object[] { getServerHostPort(), rpcConnection.getFingerprint() });
    			String newKeyWarning = clientTrust.getMessages().getMessage(
   						ClientTrust.CLIENT_TRUST_WARNING_NEW_KEY,
   						new Object[] { getServerHostPort(), rpcConnection.getFingerprint() });
    			
    			boolean fpExist = clientTrust.fingerprintExists(rpcConnection.getServerIpPort(),
    					ClientTrust.FINGERPRINT_USER_NAME);
    			boolean fpMatch = clientTrust.fingerprintMatches(rpcConnection.getServerIpPort(),
    					ClientTrust.FINGERPRINT_USER_NAME, rpcConnection.getFingerprint());

    			boolean fpReplaceExist = clientTrust.fingerprintExists(rpcConnection.getServerIpPort(),
    					ClientTrust.FINGERPRINT_REPLACEMENT_USER_NAME);
    			boolean fpReplaceMatch = clientTrust.fingerprintMatches(rpcConnection.getServerIpPort(),
    					ClientTrust.FINGERPRINT_REPLACEMENT_USER_NAME, rpcConnection.getFingerprint());
    			
    			String trustAddedInfo = clientTrust.getMessages().getMessage(ClientTrust.CLIENT_TRUST_ADDED,
						new Object[] { getServerHostPort(), rpcConnection.getServerIpPort() }); 
    			
    			// auto refuse
    			if (opts.isAutoRefuse()) {
    				// new connection
    				if (!fpExist) {
    					return newConnectionWarning;
    				}
    				// new key 
    				if (!fpMatch) {
    					return newKeyWarning;
    				}
    			}

    			// check and use replacement fingerprint
    			if ( (!fpExist || !fpMatch) && (fpReplaceExist && fpReplaceMatch) ) {
       				// Install/override fingerprint
    	    		clientTrust.installFingerprint(rpcConnection.getServerIpPort(), ClientTrust.FINGERPRINT_USER_NAME, rpcConnection.getFingerprint());
       				// Remove the replacement
    	    		clientTrust.removeFingerprint(rpcConnection.getServerIpPort(), ClientTrust.FINGERPRINT_REPLACEMENT_USER_NAME);
 
    	    		return clientTrust.getMessages().getMessage(ClientTrust.CLIENT_TRUST_ALREADY_ESTABLISHED);     						
    			}
    			
    			// new connection
    			if (!fpExist) {
    				// auto accept
    				if (opts.isAutoAccept()) {
        	    		// install fingerprint
        	    		clientTrust.installFingerprint(rpcConnection.getServerIpPort(),
        	    				fingerprintUser, fingerprint);
        	    		return newConnectionWarning + trustAddedInfo;    						
    				}
    				// didn't accept
       				throw new TrustException(TrustException.Type.NEW_CONNECTION,
       						getServerHostPort(),
       						rpcConnection.getServerIpPort(),
       						rpcConnection.getFingerprint(),
       						newConnectionWarning +
       						clientTrust.getMessages().getMessage(ClientTrust.CLIENT_TRUST_ADD_EXCEPTION_NEW_CONNECTION)     						
       						);
    			}
    			
    			// new key
    			if (!fpMatch) {
    				// force install
    				if (opts.isForce()) {
    					// auto accept
    					if (opts.isAutoAccept()) {
            	    		// install fingerprint
            	    		clientTrust.installFingerprint(rpcConnection.getServerIpPort(),
            	    				fingerprintUser, fingerprint);
            	    		return newKeyWarning + trustAddedInfo;    						
    					}
        				// didn't accept
           				throw new TrustException(TrustException.Type.NEW_KEY,
           						getServerHostPort(),
           						rpcConnection.getServerIpPort(),
           						rpcConnection.getFingerprint(),
           						newKeyWarning +
           						clientTrust.getMessages().getMessage(ClientTrust.CLIENT_TRUST_ADD_EXCEPTION_NEW_KEY)     						
           						);
    				}
    				// not force install
       				throw new TrustException(TrustException.Type.NEW_KEY,
       						getServerHostPort(),
       						rpcConnection.getServerIpPort(),
       						rpcConnection.getFingerprint(),
       						newKeyWarning +
       						clientTrust.getMessages().getMessage(ClientTrust.CLIENT_TRUST_ADD_EXCEPTION_NEW_KEY)     						
       						);
    			}

    			if (fpMatch && fingerprintValue != null) {
    	    		// install fingerprint
    	    		clientTrust.installFingerprint(rpcConnection.getServerIpPort(),
    	    				fingerprintUser, fingerprint);
    				return trustAddedInfo;
    			}
    			
    			// trust already established
	    		return clientTrust.getMessages().getMessage(ClientTrust.CLIENT_TRUST_ALREADY_ESTABLISHED);     						
    			
    		} finally {
    			if (rpcConnection != null) {
    				rpcConnection.disconnect(null);
    			}
    		}
    	}
    	
    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#removeTrust()
    	 */
    	public String removeTrust() throws P4JavaException {
    		
    		return removeTrust(null);
    	}

    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#removeTrust(com.perforce.p4java.option.server.TrustOptions)
    	 */
    	public String removeTrust(TrustOptions opts) throws P4JavaException {
    		RpcConnection rpcConnection = null;
    		
    		try {
    			rpcConnection = new RpcStreamConnection(serverHost, serverPort, props,
    					this.serverStats, this.charset, this.secure);

    			String fingerprintUser = (opts != null && opts.isReplacement()) ? ClientTrust.FINGERPRINT_REPLACEMENT_USER_NAME : ClientTrust.FINGERPRINT_USER_NAME;
    			
    			String message = "";

	    		// new connection
	    		if (!clientTrust.fingerprintExists(rpcConnection.getServerIpPort(),
	    				ClientTrust.FINGERPRINT_USER_NAME)) {
        			message = clientTrust.getMessages().getMessage(
       						ClientTrust.CLIENT_TRUST_WARNING_NEW_CONNECTION,
       						new Object[] { getServerHostPort(), rpcConnection.getFingerprint() }) + message;
	    		} else {
		    		// new key
	    			if (!clientTrust.fingerprintMatches(rpcConnection.getServerIpPort(),
	    					ClientTrust.FINGERPRINT_USER_NAME, rpcConnection.getFingerprint())) {
	    				message = clientTrust.getMessages().getMessage(
	       						ClientTrust.CLIENT_TRUST_WARNING_NEW_KEY,
	       						new Object[] { getServerHostPort(), rpcConnection.getFingerprint() }) + message;
	    			}
	    		}
	
    			// remove the fingerprint from the trust file
	    		clientTrust.removeFingerprint(rpcConnection.getServerIpPort(), fingerprintUser);
    			
    			return message + clientTrust.getMessages().getMessage(
    					ClientTrust.CLIENT_TRUST_REMOVED,
						new Object[] { getServerHostPort(), rpcConnection.getServerIpPort() });

    		} finally {
    			if (rpcConnection != null) {
    				rpcConnection.disconnect(null);
    			}
    		}
    	}

    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#getTrusts()
    	 */
    	public List<Fingerprint> getTrusts() throws P4JavaException {

    		return getTrusts(null);
    	}
        
    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#getTrusts(com.perforce.p4java.option.server.TrustOptions)
    	 */
    	public List<Fingerprint> getTrusts(TrustOptions opts) throws P4JavaException {
    		Fingerprint[] fingerprints = loadFingerprints();
    		if (fingerprints != null) {
    			List<Fingerprint> fingerprintsList = new ArrayList<Fingerprint>();
    			List<Fingerprint> replacementsList = new ArrayList<Fingerprint>();
    			for (Fingerprint fp : fingerprints) {
    				if (fp != null && fp.getUserName() != null) {
    					if (fp.getUserName().equalsIgnoreCase(
    							ClientTrust.FINGERPRINT_REPLACEMENT_USER_NAME)) {
    						replacementsList.add(fp);
    					} else {
    						fingerprintsList.add(fp);
    					}
    				}
    			}
    			if (opts != null && opts.isReplacement()) {
    				return replacementsList;
    			} else {
    				return fingerprintsList;
    			}
    		}
			return null;
    	}

    	/**
         * Set the server's id field used for storing authentication tickets. The id
         * specified here will be used when saving ticket values to a p4 tickets
         * file. This field should only be set to the server id returned as part of
         * a server message.
         *
         * @param serverId
         */
        public void setServerId(String serverId) {
                this.serverId = serverId;
        }

        /**
         * Get the server's id field used for storing authentication tickets. This
         * id should only be used as a server address portion for entries in a p4
         * tickets file.
         *
         * @return - possibly null server id
         */
        public String getServerId() {
                return this.serverId;
        }

        /**
         * Return true iff we should be performing server -> client
         * file write I/O operations in place for this command.<p>
         *
         * See PropertyDefs.WRITE_IN_PLACE_KEY javadoc for the semantics
         * of this.
         *
         * @param cmdName non-null command command name string
         * @return true iff we should do a sync in place
         */
        protected boolean writeInPlace(String cmdName) {
                return cmdName.equalsIgnoreCase(CmdSpec.SYNC.toString())
                                        &&	new Boolean(System.getProperty(
                                                        PropertyDefs.WRITE_IN_PLACE_KEY,
                                                                props.getProperty(
                                                                        PropertyDefs.WRITE_IN_PLACE_SHORT_FORM, "false")));
        }

        public String getSecretKey() {
            	return getSecretKey(this.userName);
        }

        public void setSecretKey(String secretKey) {
			setSecretKey(this.userName, secretKey);
	    }
    
        public String getSecretKey(String userName) {
        	if (userName != null) {
                return secretKeys.get(userName);
        	}
        	return null;
        }

        public void setSecretKey(String userName, String secretKey) {
        	if (userName != null) {
        		if (secretKey == null) {
        			this.secretKeys.remove(userName);
        		} else {
        			this.secretKeys.put(userName, secretKey);
        		}
        	}
        }

        protected boolean isRelaxCmdNameValidationChecks() {
                return relaxCmdNameValidationChecks;
        }

        protected void setRelaxCmdNameValidationChecks(
                        boolean relaxCmdNameValidationChecks) {
                this.relaxCmdNameValidationChecks = relaxCmdNameValidationChecks;
        }

        /**
    	 * Get the RPC packet field rule for skipping the charset conversion of
    	 * a range of RPC packet fields; leave the values as bytes. <p>
    	 * 
    	 * Note: currently only supporting the "export" command.
    	 */
        protected RpcPacketFieldRule getRpcPacketFieldRule(Map<String, Object> inMap, CmdSpec cmdSpec) {
    		if (inMap != null && cmdSpec != null) {
				if (cmdSpec == CmdSpec.EXPORT) {
	    			if (inMap.containsKey(cmdSpec.toString())) {
	    				// The map for this command spec
	    				if (inMap.get(cmdSpec.toString()) instanceof Map<?,?>) {
		    				@SuppressWarnings("unchecked")
							Map<String, Object> cmdMap = (Map<String, Object>)inMap.remove(cmdSpec.toString());
		    				if (cmdMap != null) {
								return RpcPacketFieldRule.getInstance(cmdMap);
							}
	    				}
	    			}
				}
    		}
    		return null;
    	}

    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#setAuthTicket(java.lang.String, java.lang.String)
    	 */
    	public void setAuthTicket(String userName, String authTicket) {
    		if (userName == null) {
    			throw new IllegalArgumentException("Null userName passed to the setAuthTicket method.");
    		}
    		// Must downcase the username to find or save a ticket when
    		// connected to a case insensitive server.
    		if (!isCaseSensitive() && userName != null) {
				userName = userName.toLowerCase();
    		}
    		String serverAddress = getServerId() != null ? getServerId() : getServerAddress();
            // Handling 'serverCluster'
			if (this.serverInfo != null && this.serverInfo.getServerCluster() != null) {
				serverAddress = serverInfo.getServerCluster();
			}
    		if (serverAddress == null) {
       			throw new IllegalStateException("Null serverAddress in the setAuthTicket method.");
            }
            if (authTicket == null) {
            	this.authTickets.remove(composeAuthTicketEntryKey(userName, serverAddress));
            } else {
            	this.authTickets.put(composeAuthTicketEntryKey(userName, serverAddress), authTicket);
            }
    	}
    	
    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#getAuthTicket(java.lang.String)
    	 */
    	public String getAuthTicket(String userName) {
    		// Must downcase the username to find or save a ticket when
    		// connected to a case insensitive server.
    		if (!isCaseSensitive() && userName != null) {
				userName = userName.toLowerCase();
    		}
            String serverAddress = getServerId() != null ? getServerId() : getServerAddress();
            // Handling 'serverCluster'
			if (this.serverInfo != null && this.serverInfo.getServerCluster() != null) {
				serverAddress = serverInfo.getServerCluster();
			}
            if (userName != null && serverAddress != null) {
        		return this.authTickets.get(composeAuthTicketEntryKey(userName, serverAddress));
        	}
    		return null;
    	}

    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#setTicketsFilePath(java.lang.String)
    	 */
    	public void setTicketsFilePath(String ticketsFilePath) {
    		if (ticketsFilePath == null) {
    			throw new IllegalArgumentException("Null ticketsFilePath passed to the setTicketsFilePath method.");
    		}
           	this.ticketsFilePath = ticketsFilePath;
    	}
    	
    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#getTicketsFilePath()
    	 */
    	public String getTicketsFilePath() {
            return this.ticketsFilePath;
    	}
    	
    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#setTrustFilePath(java.lang.String)
    	 */
    	public void setTrustFilePath(String trustFilePath) {
    		if (ticketsFilePath == null) {
    			throw new IllegalArgumentException("Null trustFilePath passed to the setTrustFilePath method.");
    		}
           	this.trustFilePath = trustFilePath;
    	}
    	
    	/**
    	 * @see com.perforce.p4java.impl.mapbased.server.Server#getTrustFilePath()
    	 */
    	public String getTrustFilePath() {
            return this.trustFilePath;
    	}
    	
    	/**
    	 * Compose the key for an auth ticket entry
    	 */
    	protected String composeAuthTicketEntryKey(String userName, String serverAddress) {
    		if (userName == null) {
    			throw new IllegalArgumentException("Null userName passed to the composeAuthTicketEntryKey method.");
    		}
            if (serverAddress == null) {
       			throw new IllegalArgumentException("Null serverAddress in the composeAuthTicketEntryKey method.");
            }
			if (serverAddress.indexOf(':') == -1) {
				serverAddress = "localhost:" + serverAddress;
			}
			return (serverAddress + "=" + userName);
    	}
    	
        /**
         * Get the server's address field used for storing authentication tickets.
         *
         * @return - possibly null server address
         */
    	public String getServerAddress() {
            String serverAddress = this.serverAddress;
            // If id is null try to use configured server address
            if (serverAddress == null) {
                    if (this.serverHost != null) {
                    	serverAddress = this.serverHost;
                        if (this.serverPort != UNKNOWN_SERVER_PORT) {
                        	serverAddress += ":" + Integer.toString(this.serverPort);
                        }
                    } else if (this.serverPort != UNKNOWN_SERVER_PORT) {
                    	serverAddress = Integer.toString(this.serverPort);
                    }
            }
            return serverAddress;
    	}

        /**
         * Get the server's host and port used for the RPC connection.
         *
         * @return - possibly null server host and port
         */
    	public String getServerHostPort() {
            String serverHostPort = null;
            if (this.serverHost != null) {
            	serverHostPort = this.serverHost;
                if (this.serverPort != UNKNOWN_SERVER_PORT) {
                	serverHostPort += ":" + Integer.toString(this.serverPort);
                }
            } else if (this.serverPort != UNKNOWN_SERVER_PORT) {
            	serverHostPort = Integer.toString(this.serverPort);
            }
            return serverHostPort;
    	}

    	/**
    	 * Allow for per-command use of tags or not. Currently has limited use
    	 * (only a few commands are anomalous as far as we can tell), but may
    	 * find more uses generally with experience.<p>.
    	 * 
    	 * This is normally used on a per-command (OneShot RPC server) basis.
    	 * In order to use this on a per-session (NTS RPC server) implementation
    	 * you must resend the RPC protocol, if the 'useTags' state has changed,
    	 * prior to sending the command.
    	 */
    	protected boolean useTags(String cmdName, String[] cmdArgs, Map<String, Object> inMap, boolean isStreamCmd) {
    		CmdSpec cmdSpec = CmdSpec.getValidP4JCmdSpec(cmdName);
    		if (cmdSpec != null) {
    			if (cmdSpec == CmdSpec.LOGIN) {
    				return false;
    			}
				if (isStreamCmd) {
    				switch (cmdSpec) {
					case DESCRIBE:
					case DIFF2:
					case PRINT:
					case PROTECT:
						return false;
					default:
						break;
					}
    			}
    			// Check the inMap for any tagged/non-tagged override
    			if (inMap != null) {
    				if (inMap.containsKey(IN_MAP_USE_TAGS_KEY)) {
    					return new Boolean((String)inMap.remove(IN_MAP_USE_TAGS_KEY));
    				}
    			}
    		}
    		return RPC_TAGS_USED;
    	}

        /**
         * Get the RPC user authentication counter.
         *
         * @return RPC user authentication counter
         */
    	public RpcUserAuthCounter getAuthCounter() {
    		return this.authCounter;
    	}

        /**
         * Get the server's address for the RPC connection.
         *
         * @return possibly-null RPC server address
         */
    	public IServerAddress getRpcServerAddress() {
            return this.rpcServerAddress;
    	}

        /**
         * Set the server's address for the RPC connection.
         *
         * @param rpcServerAddress RPC server address
         */
    	public void setRpcServerAddress(IServerAddress rpcServerAddress) {
            this.rpcServerAddress = rpcServerAddress;
    	}
}
