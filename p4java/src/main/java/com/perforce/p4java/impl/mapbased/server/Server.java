/*
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.server;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.Metadata;
import com.perforce.p4java.admin.IDbSchema;
import com.perforce.p4java.admin.IDiskSpace;
import com.perforce.p4java.admin.ILogTail;
import com.perforce.p4java.admin.IProperty;
import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.admin.ITriggerEntry;
import com.perforce.p4java.admin.ServerConfigurationValue;
import com.perforce.p4java.charset.PerforceCharsetProvider;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelist.Type;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.core.IFileLineMatch;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.IRepo;
import com.perforce.p4java.core.IReviewChangelist;
import com.perforce.p4java.core.IServerProcess;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamIntegrationStatus;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSize;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.graph.ICommit;
import com.perforce.p4java.graph.IGraphListTree;
import com.perforce.p4java.graph.IGraphObject;
import com.perforce.p4java.graph.IGraphRef;
import com.perforce.p4java.graph.IRevListCommit;
import com.perforce.p4java.impl.generic.core.ListData;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.server.cmd.*;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.option.server.*;
import com.perforce.p4java.server.HelixCommandExecutor;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerAddress;
import com.perforce.p4java.server.IServerAddress.Protocol;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.server.ServerStatus;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.IProgressCallback;
import com.perforce.p4java.server.callback.ISSOCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.server.delegator.*;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.perforce.p4java.PropertyDefs.*;
import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwConnectionExceptionIfConditionFails;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwP4JavaErrorIfConditionFails;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.env.PerforceEnvironment.getP4Charset;
import static com.perforce.p4java.env.PerforceEnvironment.getP4Client;
import static com.perforce.p4java.env.PerforceEnvironment.getP4User;
import static com.perforce.p4java.server.PerforceCharsets.getJavaCharsetName;
import static com.perforce.p4java.server.PerforceCharsets.getP4CharsetName;
import static com.perforce.p4java.server.PerforceCharsets.isSupported;
import static com.perforce.p4java.util.PropertiesHelper.getPropertyByKeys;
import static com.perforce.p4java.util.PropertiesHelper.isExistProperty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Generic abstract superclass for implementation-specific server
 * implementations that use a command-style server interface implementation.
 * <p>
 * <p>
 * Normal users should not be creating this class or subclasses of this class
 * directly; you should use the ServerFactory server factory methods to get a
 * suitable server implementation class.
 * <p>
 */
public abstract class Server extends HelixCommandExecutor implements IServerControl, IOptionsServer {
	// The _FIELD_NAME names below MUST correspond to the names of the
	// static fields used in the individual server impl classes; those
	// fields MUST also be static...
	public static final String SCREEN_NAME_FIELD_NAME = "SCREEN_NAME";
	public static final String IMPL_COMMENTS_FIELD_NAME = "IMPL_COMMENTS";
	public static final String IMPL_TYPE_FIELD_NAME = "IMPL_TYPE";
	public static final String MINIMUM_SUPPORTED_SERVER_LEVEL_FIELD_NAME = "MINIMUM_SUPPORTED_SERVER_LEVEL";
	public static final String PROTOCOL_NAME_FIELD_NAME = "PROTOCOL_NAME";
	public static final String DEFAULT_STATUS_FIELD_NAME = "DEFAULT_STATUS";

	// Prefix used for the (anomalous) setFileAttributes stream map:
	public static final String P4TICKETS_ENV_VAR = "P4TICKETS";
	public static final String P4TICKETS_DEFAULT_WINDOWS = "p4tickets.txt";
	public static final String P4TICKETS_DEFAULT_OTHER = ".p4tickets";
	public static final String P4TRUST_ENV_VAR = "P4TRUST";
	public static final String P4TRUST_DEFAULT_WINDOWS = "p4trust.txt";
	public static final String P4TRUST_DEFAULT_OTHER = ".p4trust";
	public static final String P4IGNORE_ENV_VAR = "P4IGNORE";

	/**
	 * Signals access (login) needed
	 */
	protected static final String CORE_AUTH_FAIL_STRING_1 = "Perforce password (P4PASSWD)";

	/**
	 * Signals access (login) needed
	 */
	protected static final String CORE_AUTH_FAIL_STRING_2 = "Access for user";

	/**
	 * Signals ticket has expired
	 */
	protected static final String CORE_AUTH_FAIL_STRING_3 = "Your session has expired";

	/**
	 * Signals ticket has expired
	 */
	protected static final String CORE_AUTH_FAIL_STRING_4 = "Your session was logged out";

	protected static final int UNKNOWN_SERVER_VERSION = -1;
	protected static final String UNKNOWN_SERVER_HOST = null;
	protected static final int UNKNOWN_SERVER_PORT = -1;

	protected static boolean runningOnWindows = SystemInfo.isWindows();

	protected UsageOptions usageOptions = null;

	protected ServerStatus status = ServerStatus.UNKNOWN;
	protected Properties props = null;

	protected IServerInfo serverInfo = null;
	protected String serverAddress = null;

	protected boolean caseSensitive = true;
	protected int serverVersion = UNKNOWN_SERVER_VERSION;
	protected String serverHost = UNKNOWN_SERVER_HOST;
	protected int serverPort = UNKNOWN_SERVER_PORT;
	protected Protocol serverProtocol = null;

	protected String userName = null;
	protected String password = null;

	/**
	 * Storage for user auth tickets. What's returned from p4 login -p command,
	 * and what we can add to each command when non-null to authenticate it
	 */
	protected Map<String, String> authTickets = new HashMap<>();

	protected IClient client = null;
	protected String clientName = null;

	/**
	 * Used when we have no client set.
	 */
	protected String clientUnsetName = CLIENT_UNSET_NAME_DEFAULT;

	protected boolean setupOnConnect = false;
	protected boolean loginOnConnect = false;

	protected ICommandCallback commandCallback = null;
	protected IProgressCallback progressCallback = null;
	protected ISSOCallback ssoCallback = null;
	protected String ssoKey = null;

	protected String charsetName = null;
	protected Charset charset = null;

	protected boolean connected = false;

	protected int minimumSupportedServerVersion = Metadata.DEFAULT_MINIMUM_SUPPORTED_SERVER_VERSION;

	protected String tmpDirName = null;

	protected AtomicInteger nextCmdCallBackKey = new AtomicInteger();
	protected AtomicInteger nextProgressCallbackKey = new AtomicInteger();

	protected boolean nonCheckedSyncs = false;

	protected boolean enableStreams = true;

	protected boolean enableAndmaps = false;

	protected boolean enableGraph = false;

	protected boolean enableTracking = false;

	protected boolean enableProgress = false;

	protected boolean quietMode = false;

	protected boolean secure = false;

	protected boolean useAuthMemoryStore = false;

	protected String ignoreFileName = null;

	protected String rsh = null;

	// The delegators for running perforce commands
	private IAttributeDelegator attributeDelegator = null;
	private IBranchDelegator branchDelegator = null;
	private IBranchesDelegator branchesDelegator = null;
	private IChangeDelegator changeDelegator = null;
	private IChangesDelegator changesDelegator = null;
	private IClientDelegator clientDelegator = null;
	private IClientsDelegator clientsDelegator = null;
	private IConfigureDelegator configureDelegator = null;
	private ICounterDelegator counterDelegator = null;
	private ICountersDelegator countersDelegator = null;
	private IDBSchemaDelegator dbSchemaDelegator = null;
	private IDepotDelegator depotDelegator = null;
	private IDepotsDelegator depotsDelegator = null;
	private IReposDelegator reposDelegator = null;
	private DescribeDelegator describeDelegator = null;
	private IDiff2Delegator diff2Delegator = null;
	private IDirsDelegator dirsDelegator = null;
	private IDiskspaceDelegator diskspaceDelegator = null;
	private IDuplicateDelegator duplicateDelegator = null;
	private IExportDelegator exportDelegator = null;
	private IFileAnnotateDelegator fileAnnotateDelegator = null;
	private IFileLogDelegator fileLogDelegator = null;
	private IFilesDelegator filesDelegator = null;
	private IFixDelegator fixDelegator = null;
	private IFixesDelegator fixesDelegator = null;
	private IFstatDelegator fstatDelegator = null;
	private IGrepDelegator grepDelegator = null;
	private GroupDelegator groupDelegator = null;
	private GroupsDelegator groupsDelegator = null;
	private IInfoDelegator infoDelegator = null;
	private IIntegratedDelegator integratedDelegator = null;
	private InterchangesDelegator interchangesDelegator = null;
	private IJobDelegator jobDelegator = null;
	private IJobsDelegator jobsDelegator = null;
	private IJobSpecDelegator jobSpecDelegator = null;
	private IKeyDelegator keyDelegator = null;
	private IKeysDelegator keysDelegator = null;
	private ILabelDelegator labelDelegator = null;
	private ILabelsDelegator labelsDelegator = null;
	private IMonitorDelegator monitorDelegator = null;
	private IMoveDelegator moveDelegator = null;
	private IStatDelegator statDelegator = null;
	private IJournalWaitDelegator journalWaitDelegator = null;
	private ILoginDelegator loginDelegator;
	private ILogin2Delegator login2Delegator;
	private ILogoutDelegator logoutDelegator;
	private ILogTailDelegator logTailDelegator;
	private IObliterateDelegator obliterateDelegator;
	private IOpenedDelegator openedDelegator;
	private IPasswdDelegator passwdDelegator;
	private IPrintDelegator printDelegator;
	private IPropertyDelegator propertyDelegator;
	private IProtectDelegator protectDelegator;
	private IProtectsDelegator protectsDelegator;
	private IReloadDelegator reloadDelegator;
	private IRenameUserDelegator renameUserDelegator;
	private IReviewDelegator reviewDelegator;
	private IReviewsDelegator reviewsDelegator;
	private ISearchDelegator searchDelegator;
	private ISizesDelegator sizesDelegator;
	private IStreamDelegator streamDelegator;
	private IStreamsDelegator streamsDelegator;
	private ITagDelegator tagDelegator;
	private ITriggersDelegator triggersDelegator;
	private IUnloadDelegator unloadDelegator;
	private IUserDelegator userDelegator;
	private IUsersDelegator usersDelegator;
	private IVerifyDelegator verifyDelegator;
	private IGraphListTreeDelegator graphListTreeDelegator;
	private ICommitDelegator graphCommitDelegator;
	private IGraphRevListDelegator graphRevListDelegator;
	private IGraphReceivePackDelegator graphReceivePackDelegator;
	private IListDelegator listDelegator;
	private IGraphShowRefDelegator graphShowRefDelegator;

	/**
	 * Useful source of random integers, etc.
	 */
	protected Random rand = new Random(System.currentTimeMillis());

	public static String guardNull(String str) {
		final String nullStr = "<null>";

		return (str == null ? nullStr : str);
	}

	public static String[] getPreferredPathArray(final String[] preamble,
	                                             final List<IFileSpec> specList) {
		return getPreferredPathArray(preamble, specList, true);
	}

	public static String[] getPreferredPathArray(final String[] preamble,
	                                             final List<IFileSpec> specList, final boolean annotate) {
		int pathArraySize = (isNull(preamble) ? 0 : preamble.length)
				+ (isNull(specList) ? 0 : specList.size());
		String[] pathArray = new String[pathArraySize];
		int i = 0;
		if (nonNull(preamble)) {
			for (String str : preamble) {
				pathArray[i++] = str;
			}
		}

		if (nonNull(specList)) {
			for (IFileSpec fSpec : specList) {
				if (nonNull(fSpec) && (fSpec.getOpStatus() == VALID)) {
					if (annotate) {
						pathArray[i++] = fSpec.getAnnotatedPreferredPathString();
					} else {
						pathArray[i++] = fSpec.getPreferredPathString();
					}
				} else {
					pathArray[i++] = null;
				}
			}
		}

		return pathArray;
	}

	public static String[] populatePathArray(final String[] pathArray, final int start,
	                                         final List<IFileSpec> fileSpecList) {

		if (isNull(pathArray)) {
			return null;
		}
		if (isNull(fileSpecList)) {
			return pathArray;
		}

		throwP4JavaErrorIfConditionFails(start >= 0,
				"negative start index in populatePathArray: %s", start);
		throwP4JavaErrorIfConditionFails(
				start <= pathArray.length && (start + fileSpecList.size() <= pathArray.length),
				"pathArray too small in populatePathArray");

		int i = start;
		for (IFileSpec fSpec : fileSpecList) {
			if (nonNull(fSpec) && (fSpec.getOpStatus() == VALID)) {
				pathArray[i] = fSpec.getAnnotatedPreferredPathString();
			} else {
				pathArray[i] = null;
			}
			i++;
		}

		return pathArray;
	}

	/**
	 * Return true if the JVM indicates that we're running on a Windows
	 * platform. Not entirely reliable, but good enough for our purposes.
	 */
	public static boolean isRunningOnWindows() {
		return runningOnWindows;
	}

	@Override
	public String getCharsetName() {
		return charsetName;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getIgnoreFileName() {
		return ignoreFileName;
	}

	public void setIgnoreFileName(String ignoreFileName) {
		this.ignoreFileName = ignoreFileName;
	}

	@Override
	public ServerStatus getStatus() {
		return status;
	}

	public UsageOptions getUsageOptions() {
		return usageOptions;
	}

	public Server setUsageOptions(UsageOptions usageOptions) {
		this.usageOptions = usageOptions;
		return this;
	}

	/**
	 * Returns a list of revisions given the options
	 *
	 * @param options
	 * @return
	 * @throws P4JavaException
	 */
	@Override
	public List<IRevListCommit> getGraphRevList(GraphRevListOptions options) throws P4JavaException {
		return graphRevListDelegator.getGraphRevList(options);
	}

	@Override
	public List<ICommit> getGraphCommitLogList(GraphCommitLogOptions options) throws P4JavaException {
		return graphCommitDelegator.getGraphCommitLogList(options);
	}

	@Override
	public void doGraphReceivePack(GraphReceivePackOptions options) throws P4JavaException {
		graphReceivePackDelegator.doGraphReceivePack(options);
	}

	/**
	 * Usage: list [-l label [-d]] [-C] [-M] files..
	 *
	 * @param fileSpecs List of file specs
	 * @param options   Command options
	 * @return ListData
	 * @throws P4JavaException
	 */
	@Override
	public ListData getListData(List<IFileSpec> fileSpecs, ListOptions options) throws P4JavaException {
		return listDelegator.getListData(fileSpecs, options);
	}

	/**
	 * Internal use only.
	 * <p>
	 * Use IClient.getListData to restrict list to client view.
	 *
	 * @param fileSpecs List of file specs
	 * @param options   Command options
	 * @return ListData
	 * @throws P4JavaException
	 */
	@Override
	public ListData getListData(List<IFileSpec> fileSpecs, ListOptions options, String clientName) throws P4JavaException {
		return listDelegator.getListData(fileSpecs, options, clientName);
	}

	@Override
	public String getUserName() {
		return userName;
	}

	@Override
	public void setUserName(String userName) {
		this.userName = userName;
		setAuthTicket(getAuthTicket(userName));
	}

	@Override
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	public boolean isEnableProgress() {
		return enableProgress;
	}

	public void setEnableProgress(boolean enableProgress) {
		this.enableProgress = enableProgress;
	}

	public boolean isEnableTracking() {
		return enableTracking;
	}

	public void setEnableTracking(boolean enableTracking) {
		this.enableTracking = enableTracking;
	}

	public boolean isNonCheckedSyncs() {
		return nonCheckedSyncs;
	}

	public void setNonCheckedSyncs(boolean nonCheckedSyncs) {
		this.nonCheckedSyncs = nonCheckedSyncs;
	}

	public boolean isQuietMode() {
		return quietMode;
	}

	public void setQuietMode(boolean quietMode) {
		this.quietMode = quietMode;
	}

	/**
	 * Check if the server is secure (SSL) or not.
	 */
	protected boolean isSecure() {
		return secure;
	}

	/**
	 * Sets the server to secure (SSL) or non-secure mode.
	 */
	protected void setSecure(boolean secure) {
		this.secure = secure;
	}

	// Try to get the Perforce server version. This is likely to be the first
	// time
	// actual connectivity is tested for the server...
	// Since this is called before we know much about the state or type of the
	// Perforce server, we do virtually no real error checking or recovery -- we
	// either get a suitable response and dig out the server version, or we just
	// leave things alone.
	//
	// NOTE: has the side effect of setting the server impl's serverVersion
	// field.
	@Override
	public int getServerVersion() throws ConnectionException {
		if (serverVersion != UNKNOWN_SERVER_VERSION) {
			// We've already got the server version. This will fail
			// if the server changes underneath us, but that's life...
			return serverVersion;
		}

		try {
			serverInfo = getServerInfo();
			if (nonNull(serverInfo)) {
				if (isNotBlank(serverInfo.getServerAddress())) {
					serverAddress = serverInfo.getServerAddress();
				}
				String currentServerVersion = serverInfo.getServerVersion();
				if (isNotBlank(currentServerVersion)) {
					serverVersion = parseVersionString(currentServerVersion);
					return serverVersion;
				}
			}
		// p4ic4idea: don't re-wrap the problem.
		} catch (ConnectionException e) {
			Log.exception(e);
			throw e;
		} catch (Exception exc) {
			Log.exception(exc);
			throw new ConnectionException(exc.getLocalizedMessage(), exc);
		}
		return UNKNOWN_SERVER_VERSION;
	}

	@Override
	public void setCurrentServerInfo(IServerInfo info) {
		this.serverInfo = info;
	}

	@Override
	public IServerInfo getCurrentServerInfo() {
		return this.serverInfo;
	}

	@Override
	public void connect()
			throws ConnectionException, AccessException, RequestException, ConfigException {
		connected = true;
		status = ServerStatus.READY;

		Log.info("connected to Perforce server at %s:%s", serverHost, serverPort);

		// Try to get and then verify the server version:
		int serverVersion = getServerVersion();
		throwConnectionExceptionIfConditionFails(serverVersion != UNKNOWN_SERVER_VERSION,
				"Unable to determine Perforce server version for connection; "
						+ "check network connection, connection character set setting, "
						+ "and / or server status");

		throwConnectionExceptionIfConditionFails(serverVersion >= minimumSupportedServerVersion,
				"Attempted to connect to an unsupported Perforce server version; "
						+ "target server version: %s; minimum supported version: %s",
				serverVersion, minimumSupportedServerVersion);

		if (loginOnConnect && isNotBlank(userName) && isNotBlank(password)) {
			login(password);
		}

		if (setupOnConnect && isNotBlank(clientName)) {
			// Attempt to get the client set up, etc.; subclasses will
			// probably do much more than this, or nothing at all...
			client = getClient(clientName);
		}

		// If the charset is not set and P4CHARSET is null/none/auto (unset),
		// automatically sets it to the Java default charset.
		// The following are special cases.
		// "auto" (Guess a P4CHARSET based on client OS params)
		// "none" (same as unsetting P4CHARSET)
		if (serverInfo.isUnicodeEnabled() && isNull(charset)) {
			String p4Charset = getP4Charset();
			if (isBlank(p4Charset) || "none".equalsIgnoreCase(p4Charset)
					|| "auto".equalsIgnoreCase(p4Charset)) {
				// Get the first matching Perforce charset for the Java default
				// charset
				String p4CharsetName = getP4CharsetName(CharsetDefs.DEFAULT_NAME);
				if (isNotBlank(p4CharsetName)) {
					charsetName = p4CharsetName;
					charset = CharsetDefs.DEFAULT;
				} else { // Default to Perforce "utf8" equivalent to Java
					// "UTF-8"
					charsetName = "utf8";
					charset = CharsetDefs.UTF8;
				}
			} else {
				setCharsetName(p4Charset);
			}
		}
	}

	@Override
	public void disconnect() throws ConnectionException, AccessException {
		connected = false;
		status = ServerStatus.DISCONNECTED;
		Log.info("disconnected from Perforce server at %s:%s", serverHost, serverPort);
	}

	@Override
	public String getAuthTicket() {
		return getAuthTicket(userName);
	}

	@Override
	public void setAuthTicket(String authTicket) {
		if (isNotBlank(userName)) {
			setAuthTicket(userName, authTicket);
		}
	}

	@Override
	public String[] getKnownCharsets() {
		return PerforceCharsets.getKnownCharsets();
	}

	@Override
	public Properties getProperties() {
		return props;
	}

	@Override
	public int getServerVersionNumber() {
		return serverVersion;
	}

	public String getWorkingDirectory() {
		if (nonNull(usageOptions)) {
			return usageOptions.getWorkingDirectory();
		} else {
			return null;
		}
	}

	public void setWorkingDirectory(final String dirPath) {
		if (nonNull(usageOptions)) {
			usageOptions.setWorkingDirectory(dirPath);
		}
	}

	@Override
	public IClient getCurrentClient() {
		return client;
	}

	@Override
	public void setCurrentClient(final IClient client) {
		this.client = client;

		if (nonNull(client)) {
			clientName = client.getName();
		} else {
			clientName = null;
		}
	}

	@Override
	public ICommandCallback registerCallback(ICommandCallback callback) {
		ICommandCallback oldCallback = commandCallback;
		commandCallback = callback;
		return oldCallback;
	}

	@Override
	public IProgressCallback registerProgressCallback(IProgressCallback progressCallback) {
		IProgressCallback oldCallback = this.progressCallback;
		this.progressCallback = progressCallback;
		return oldCallback;
	}

	@Override
	public ISSOCallback registerSSOCallback(ISSOCallback callback, String ssoKey) {
		ISSOCallback oldCallback = ssoCallback;
		ssoCallback = callback;
		this.ssoKey = ssoKey;
		return oldCallback;
	}

	@Override
	public boolean setCharsetName(final String charsetName) throws UnsupportedCharsetException {
		// "auto" (Guess a P4CHARSET based on client OS params)
		// "none" (same as unsetting P4CHARSET)
		if (isNotBlank(charsetName)
				&& !("none".equals(charsetName) || "auto".equals(charsetName))) {
			// Check if it is a supported Perforce charset
			if (!isSupported(charsetName)) {
				throw new UnsupportedCharsetException(charsetName);
			}
			// Get the Java equivalent charset for this Perforce charset
			String javaCharsetName = getJavaCharsetName(charsetName);
			if (isNotBlank(javaCharsetName)) {
				try {
					charset = Charset.forName(javaCharsetName);
				} catch (UnsupportedCharsetException uce) {
					// In case P4Java's Perforce extended charsets are not
					// loaded in the VM's bootstrap classpath (i.e. P4Java JAR
					// file is inside a WAR deployed in a web app container like
					// Jetty, Tomcat, etc.), we'll instantiate it and lookup the
					// Perforce extended charsets.
					PerforceCharsetProvider p4CharsetProvider = new PerforceCharsetProvider();
					charset = p4CharsetProvider.charsetForName(javaCharsetName);

					// Throw the unsupported charset exception that was catched.
					if (isNull(charset)) {
						throw uce;
					}
				} catch (IllegalCharsetNameException icne) {
					// Throw a unsupported charset exception wrapped around
					// the illegal charset name exception.
					throw new UnsupportedCharsetException(icne.getLocalizedMessage());
				}
				// Set the new charset name
				this.charsetName = charsetName;
			}
		} else { // Reset the charset to "no charset"
			this.charsetName = null;
			charset = null;
		}

		return nonNull(charset);
	}

	@Override
	public boolean supportsUnicode() throws ConnectionException, RequestException, AccessException {
		if (isNull(serverInfo)) {
			serverInfo = getServerInfo();
		}

		return nonNull(serverInfo) && serverInfo.isUnicodeEnabled();
	}

	@Override
	public ServerStatus init(final String host, final int port, final Properties properties,
	                         final UsageOptions opts, final boolean secure)
			throws ConfigException, ConnectionException {
		serverHost = host;
		serverPort = port;
		this.secure = secure;

		props = ObjectUtils.firstNonNull(properties, new Properties());
		usageOptions = ObjectUtils.firstNonNull(opts, new UsageOptions(this.props));

		// Retrieve some fairly generic properties; note the use of the short
		// form keys for
		// program name and version (done as a favour to testers and users
		// everywhere...).
		tmpDirName = RpcPropertyDefs.getProperty(props, P4JAVA_TMP_DIR_KEY,
				System.getProperty("java.io.tmpdir"));

		if (isBlank(tmpDirName)) {
			// This can really only happen if someone has nuked or played with
			// the JVM's system properties before we get here... the default
			// will
			// work for most non-Windows boxes in most cases, and may not be
			// needed in many cases anyway.
			tmpDirName = "/tmp";
			Log.warn("Unable to get tmp name from P4 properties or System; using %s instead",
					tmpDirName);
		}

		Log.info("Using program name: '%s'; program version: '%s'", usageOptions.getProgramName(),
				usageOptions.getProgramVersion());
		Log.info("Using tmp file directory: %s", tmpDirName);

		setUserName(getPropertyByKeys(props, USER_NAME_KEY_SHORTFORM, USER_NAME_KEY, getP4User()));
		password = getPropertyByKeys(props, PASSWORD_KEY_SHORTFORM, PASSWORD_KEY, null);
		clientName = getPropertyByKeys(props, CLIENT_NAME_KEY_SHORTFORM, CLIENT_NAME_KEY,
				getP4Client());

		setupOnConnect = isExistProperty(props, AUTO_CONNECT_KEY_SHORTFORM, AUTO_CONNECT_KEY, setupOnConnect);
		loginOnConnect = isExistProperty(props, AUTO_LOGIN_KEY_SHORTFORM, AUTO_LOGIN_KEY, loginOnConnect);
		nonCheckedSyncs = isExistProperty(props, NON_CHECKED_SYNC_SHORT_FORM, NON_CHECKED_SYNC, nonCheckedSyncs);
		enableStreams = isExistProperty(props, ENABLE_STREAMS_SHORT_FORM, ENABLE_STREAMS, enableStreams);
		enableAndmaps = isExistProperty(props, ENABLE_ANDMAPS_SHORT_FORM, ENABLE_ANDMAPS, enableAndmaps);
		enableGraph = isExistProperty(props, ENABLE_GRAPH_SHORT_FORM, ENABLE_GRAPH, enableGraph);
		enableTracking = isExistProperty(props, ENABLE_TRACKING_SHORT_FORM, ENABLE_TRACKING, enableTracking);
		enableProgress = isExistProperty(props, ENABLE_PROGRESS_SHORT_FORM, ENABLE_PROGRESS, enableProgress);
		quietMode = isExistProperty(props, QUIET_MODE_SHORT_FORM, QUIET_MODE, quietMode);
		useAuthMemoryStore = isExistProperty(props, USE_AUTH_MEMORY_STORE_KEY_SHORT_FORM, USE_AUTH_MEMORY_STORE_KEY, useAuthMemoryStore);

		// Attempt to get the P4IGNORE file name from the passed-in properties
		// or the system environment variable 'P4IGNORE'
		ignoreFileName = getPropertyByKeys(props, IGNORE_FILE_NAME_KEY_SHORT_FORM,
				IGNORE_FILE_NAME_KEY, System.getenv(P4IGNORE_ENV_VAR));
		// Instantiate the delegators
		attributeDelegator = new AttributeDelegator(this);
		branchDelegator = new BranchDelegator(this);
		branchesDelegator = new BranchesDelegator(this);
		changeDelegator = new ChangeDelegator(this);
		changesDelegator = new ChangesDelegator(this);
		clientDelegator = new ClientDelegator(this);
		clientsDelegator = new ClientsDelegator(this);
		configureDelegator = new ConfigureDelegator(this);
		counterDelegator = new CounterDelegator(this);
		countersDelegator = new CountersDelegator(this);
		dbSchemaDelegator = new DBSchemaDelegator(this);
		depotDelegator = new DepotDelegator(this);
		depotsDelegator = new DepotsDelegator(this);
		reposDelegator = new ReposDelegator(this);
		describeDelegator = new DescribeDelegator(this);
		diff2Delegator = new Diff2Delegator(this);
		dirsDelegator = new DirsDelegator(this);
		diskspaceDelegator = new DiskspaceDelegator(this);
		duplicateDelegator = new DuplicateDelegator(this);
		exportDelegator = new ExportDelegator(this);
		fileAnnotateDelegator = new FileAnnotateDelegator(this);
		fileLogDelegator = new FileLogDelegator(this);
		filesDelegator = new FilesDelegator(this);
		fixDelegator = new FixDelegator(this);
		fixesDelegator = new FixesDelegator(this);
		fstatDelegator = new FstatDelegator(this);
		grepDelegator = new GrepDelegator(this);
		groupDelegator = new GroupDelegator(this);
		groupsDelegator = new GroupsDelegator(this);
		infoDelegator = new InfoDelegator(this);
		integratedDelegator = new IntegratedDelegator(this);
		interchangesDelegator = new InterchangesDelegator(this);
		jobDelegator = new JobDelegator(this);
		jobsDelegator = new JobsDelegator(this);
		jobSpecDelegator = new JobSpecDelegator(this);
		keyDelegator = new KeyDelegator(this);
		keysDelegator = new KeysDelegator(this);
		labelDelegator = new LabelDelegator(this);
		labelsDelegator = new LabelsDelegator(this);
		monitorDelegator = new MonitorDelegator(this);
		moveDelegator = new MoveDelegator(this);
		statDelegator = new StatDelegator(this);
		journalWaitDelegator = new JournalWaitDelegator(this);
		loginDelegator = new LoginDelegator(this);
		login2Delegator = new Login2Delegator(this);
		logoutDelegator = new LogoutDelegator(this);
		logTailDelegator = new LogTailDelegator(this);
		obliterateDelegator = new ObliterateDelegator(this);
		openedDelegator = new OpenedDelegator(this);
		passwdDelegator = new PasswdDelegator(this);
		printDelegator = new PrintDelegator(this);
		propertyDelegator = new PropertyDelegator(this);
		protectDelegator = new ProtectDelegator(this);
		protectsDelegator = new ProtectsDelegator(this);
		reloadDelegator = new ReloadDelegator(this);
		renameUserDelegator = new RenameUserDelegator(this);
		reviewDelegator = new ReviewDelegator(this);
		reviewsDelegator = new ReviewsDelegator(this);
		searchDelegator = new SearchDelegator(this);
		sizesDelegator = new SizesDelegator(this);
		streamDelegator = new StreamDelegator(this);
		streamsDelegator = new StreamsDelegator(this);
		tagDelegator = new TagDelegator(this);
		triggersDelegator = new TriggersDelegator(this);
		unloadDelegator = new UnloadDelegator(this);
		userDelegator = new UserDelegator(this);
		usersDelegator = new UsersDelegator(this);
		verifyDelegator = new VerifyDelegator(this);
		graphListTreeDelegator = new GraphListTreeDelegator(this);
		graphCommitDelegator = new CommitDelegator(this);
		graphRevListDelegator = new GraphRevListDelegator(this);
		graphReceivePackDelegator = new GraphReceivePackDelegator(this);
		listDelegator = new ListDelegator(this);
		graphShowRefDelegator = new GraphShowRefDelegator(this);
		return status; // Which is UNKNOWN at this point...
	}

	@Override
	public ServerStatus init(final String host, final int port, final Properties props,
	                         final UsageOptions opts) throws ConfigException, ConnectionException {

		return init(host, port, props, opts, false);
	}

	@Override
	public ServerStatus init(final String host, final int port, final Properties props)
			throws ConfigException, ConnectionException {

		return init(host, port, props, null);
	}

	/**
	 * Get default p4tickets file for the running OS
	 *
	 * @return - default p4tickets file
	 */
	protected String getDefaultP4TicketsFile() {
		StringBuilder sb = new StringBuilder();
		sb.append(SystemInfo.getUserHome()).append(SystemInfo.getFileSeparator());
		if (SystemInfo.isWindows()) {
			sb.append(P4TICKETS_DEFAULT_WINDOWS);
		} else {
			sb.append(P4TICKETS_DEFAULT_OTHER);
		}
		return sb.toString();
	}

	/**
	 * Get default p4trust file for the running OS
	 *
	 * @return - default p4trust file
	 */
	protected String getDefaultP4TrustFile() {
		StringBuilder sb = new StringBuilder();
		sb.append(SystemInfo.getUserHome()).append(SystemInfo.getFileSeparator());
		if (SystemInfo.isWindows()) {
			sb.append(P4TRUST_DEFAULT_WINDOWS);
		} else {
			sb.append(P4TRUST_DEFAULT_OTHER);
		}
		return sb.toString();
	}

	/**
	 * Get the server address entry from the p4 info.
	 *
	 * @return - server address or null if error
	 */
	protected String getInfoServerAddress() {
		if (isNotBlank(serverAddress)) {
			// We've already got the server version. This will fail
			// if the server changes underneath us, but that's life...
			return serverAddress;
		}

		try {
			serverInfo = getServerInfo();
			if (nonNull(serverInfo)) {
				String serverInfoServerAddress = serverInfo.getServerAddress();
				if (isNotBlank(serverInfoServerAddress)) {
					serverAddress = serverInfoServerAddress;
				}

				String serverInfoServerVersion = serverInfo.getServerVersion();
				if (isNotBlank(serverInfoServerVersion)) {
					serverVersion = parseVersionString(serverInfoServerVersion);
				}
			}
		} catch (Exception exc) {
			Log.exception(exc);
		}

		return serverAddress;
	}

	/**
	 * Return the major version number (e.g. 20081) from the passed-in complete
	 * version string. Instead of using regex or anything too complex we just
	 * keep splitting the string and recombining; this could be optimised or
	 * flexibilised fairly easily on one of those long rainy days... (HR).
	 */
	protected int parseVersionString(final String versionString) {
		// Format: P4D/LINUX26X86/2007.3/142194 (2007/12/17),
		// but with minor variants possible due to internal server builds,
		// e.g. 2005.2.r05.2_nightly. But all we want is the 2007.3 turned
		// into an int like 20073...

		if (isNotBlank(versionString)) {
			String[] subStrings = versionString.split("/");
			if (subStrings.length >= 3) {
				String candidate = subStrings[2];
				String[] candidateParts = candidate.split("\\.");
				if (candidateParts.length >= 2) {
					try {
						return Integer.parseInt(candidateParts[0] + candidateParts[1]);
					} catch (NumberFormatException nfe) {
						Log.error("Unexpected exception in P4CmdServerImpl.parseVersionString: %s",
								nfe);
					}
				}
			}
		}

		return UNKNOWN_SERVER_VERSION;
	}

	/**
	 * Returns the next positive pseudo random int.
	 */
	protected int getRandomInt() {
		return Math.abs(rand.nextInt(Integer.MAX_VALUE));
	}

	public ISSOCallback getSSOCallback() {
		return ssoCallback;
	}

	public String getSSOKey() {
		return ssoKey;
	}

	protected boolean isUnicode() {
		return isNotBlank(charsetName);
	}

	// Command delegators
	/*
	 * (non-Javadoc)
     * 
     * @see com.perforce.p4java.server.delegator.IAttributeDelegator#
     * setFileAttributes(java.util.List, java.util.Map,
     * com.perforce.p4java.option.server.SetFileAttributesOptions)
     */
	@Override
	public List<IFileSpec> setFileAttributes(List<IFileSpec> files, Map<String, String> attributes,
	                                         SetFileAttributesOptions opts) throws P4JavaException {
		return attributeDelegator.setFileAttributes(files, attributes, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.delegator.IAttributeDelegator#
	 * setFileAttributes(java.util.List, java.lang.String, java.io.InputStream,
	 * com.perforce.p4java.option.server.SetFileAttributesOptions)
	 */
	@Override
	public List<IFileSpec> setFileAttributes(
			List<IFileSpec> files,
			@Nonnull String attributeName,
			@Nonnull InputStream inStream,
			SetFileAttributesOptions opts) throws P4JavaException {
		return attributeDelegator.setFileAttributes(files, attributeName, inStream, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.delegator.IBranchesDelegator#getBranchSpecs(
	 * com.perforce.p4java.option.server.GetBranchSpecsOptions)
	 */
	@Override
	public List<IBranchSpecSummary> getBranchSpecs(GetBranchSpecsOptions opts)
			throws P4JavaException {
		return branchesDelegator.getBranchSpecs(opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getBranchSpecs(java.lang.String,
	 * java.lang.String, int)
	 */
	@Override
	public List<IBranchSpecSummary> getBranchSpecs(String userName, String nameFilter,
	                                               int maxReturns) throws ConnectionException, RequestException, AccessException {
		return branchesDelegator.getBranchSpecs(userName, nameFilter, maxReturns);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IBranchDelegator#getBranchSpec(java.lang.
	 * String, com.perforce.p4java.option.server.GetBranchSpecOptions)
	 */
	@Override
	public IBranchSpec getBranchSpec(String name, GetBranchSpecOptions opts)
			throws P4JavaException {
		return branchDelegator.getBranchSpec(name, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.IBranchDelegator#deleteBranchSpec(java.lang.
	 * String, com.perforce.p4java.option.server.DeleteBranchSpecOptions)
	 */
	@Override
	public String deleteBranchSpec(String branchSpecName, DeleteBranchSpecOptions opts)
			throws P4JavaException {
		return branchDelegator.deleteBranchSpec(branchSpecName, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IBranchDelegator#getBranchSpec(java.lang.
	 * String)
	 */
	@Override
	public IBranchSpec getBranchSpec(String name)
			throws ConnectionException, RequestException, AccessException {
		return branchDelegator.getBranchSpec(name);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.IBranchDelegator#createBranchSpec(com.perforce
	 * .p4java.core.IBranchSpec)
	 */
	@Override
	public String createBranchSpec(@Nonnull IBranchSpec branchSpec)
			throws ConnectionException, RequestException, AccessException {
		return branchDelegator.createBranchSpec(branchSpec);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.IBranchDelegator#updateBranchSpec(com.perforce
	 * .p4java.core.IBranchSpec)
	 */
	@Override
	public String updateBranchSpec(@Nonnull IBranchSpec branchSpec)
			throws ConnectionException, RequestException, AccessException {
		return branchDelegator.updateBranchSpec(branchSpec);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.IBranchDelegator#deleteBranchSpec(java.lang.
	 * String, boolean)
	 */
	@Override
	public String deleteBranchSpec(String branchSpecName, boolean force)
			throws ConnectionException, RequestException, AccessException {
		try {
			return branchDelegator.deleteBranchSpec(branchSpecName,
					new DeleteBranchSpecOptions(force));
		} catch (P4JavaException p4je) {
			throw new RequestException(p4je);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#deletePendingChangelist(int)
	 */
	@Override
	public String deletePendingChangelist(int id)
			throws ConnectionException, RequestException, AccessException {
		return changeDelegator.deletePendingChangelist(id);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getChangelist(int)
	 */
	@Override
	public IChangelist getChangelist(int id)
			throws ConnectionException, RequestException, AccessException {
		return changeDelegator.getChangelist(id);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.delegator.IChangeDelegator#
	 * deletePendingChangelist(int,
	 * com.perforce.p4java.option.server.ChangelistOptions)
	 */
	@Override
	public String deletePendingChangelist(int id, ChangelistOptions opts) throws P4JavaException {
		return changeDelegator.deletePendingChangelist(id, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.delegator.IChangeDelegator#getChangelist(int,
	 * com.perforce.p4java.option.server.ChangelistOptions)
	 */
	@Override
	public IChangelist getChangelist(int id, ChangelistOptions opts) throws P4JavaException {
		return changeDelegator.getChangelist(id, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getChangelists(int,
	 * java.util.List, java.lang.String, java.lang.String, boolean,
	 * com.perforce.p4java.core.IChangelist.Type, boolean)
	 */
	@Override
	public List<IChangelistSummary> getChangelists(final int maxMostRecent,
	                                               final List<IFileSpec> fileSpecs, final String clientName, final String userName,
	                                               final boolean includeIntegrated, final Type type, final boolean longDesc)
			throws ConnectionException, RequestException, AccessException {
		return changesDelegator.getChangelists(maxMostRecent, fileSpecs, clientName, userName,
				includeIntegrated, type, longDesc);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getChangelists(int,
	 * java.util.List, java.lang.String, java.lang.String, boolean, boolean,
	 * boolean, boolean)
	 */
	@Override
	public List<IChangelistSummary> getChangelists(final int maxMostRecent,
	                                               final List<IFileSpec> fileSpecs, final String clientName, final String userName,
	                                               final boolean includeIntegrated, final boolean submittedOnly, final boolean pendingOnly,
	                                               final boolean longDesc) throws ConnectionException, RequestException, AccessException {

		return changesDelegator.getChangelists(maxMostRecent, fileSpecs, clientName, userName,
				includeIntegrated, submittedOnly, pendingOnly, longDesc);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.delegator.IChangesDelegator#getChangelists(
	 * java.util.List, com.perforce.p4java.option.server.GetChangelistsOptions)
	 */
	public List<IChangelistSummary> getChangelists(final List<IFileSpec> fileSpecs,
	                                               final GetChangelistsOptions opts) throws P4JavaException {
		return changesDelegator.getChangelists(fileSpecs, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.delegator.IDescribeDelegator#
	 * getChangelistDiffs(int,
	 * com.perforce.p4java.option.server.GetChangelistDiffsOptions)
	 */
	@Override
	public InputStream getChangelistDiffs(int id, GetChangelistDiffsOptions opts)
			throws P4JavaException {
		return describeDelegator.getChangelistDiffs(id, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.delegator.IDescribeDelegator#
	 * getChangelistDiffsStream(int,
	 * com.perforce.p4java.option.server.DescribeOptions)
	 */
	@Override
	public InputStream getChangelistDiffsStream(int id, DescribeOptions options)
			throws ConnectionException, RequestException, AccessException {
		return describeDelegator.getChangelistDiffsStream(id, options);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.delegator.IDescribeDelegator#getShelvedFiles(
	 * int)
	 */
	@Override
	public List<IFileSpec> getShelvedFiles(int changelistId) throws P4JavaException {
		return describeDelegator.getChangelistFiles(changelistId);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.delegator.IDescribeDelegator#getShelvedFiles(
	 * int, int)
	 */
	@Override
	public List<IFileSpec> getShelvedFiles(int changelistId, int max) throws P4JavaException {
		return describeDelegator.getChangelistFiles(changelistId, max);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getChangelistDiffs(int,
	 * com.perforce.p4java.core.file.DiffType)
	 */
	@Override
	public InputStream getChangelistDiffs(int id, DiffType diffType)
			throws ConnectionException, RequestException, AccessException {
		return describeDelegator.getChangelistDiffs(id, diffType);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getChangelistFiles(int)
	 */
	@Override
	public List<IFileSpec> getChangelistFiles(int id)
			throws ConnectionException, RequestException, AccessException {
		return describeDelegator.getChangelistFiles(id);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getChangelistFiles(int, int)
	 */
	@Override
	public List<IFileSpec> getChangelistFiles(int id, int max)
			throws ConnectionException, RequestException, AccessException {
		return describeDelegator.getChangelistFiles(id, max);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getCommitFiles(String, String)
	 */
	@Override
	public List<IFileSpec> getCommitFiles(final String repo, final String commit)
			throws ConnectionException, RequestException, AccessException {
		return describeDelegator.getCommitFiles(repo, commit);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.delegator.IConfigureDelegator#
	 * setOrUnsetServerConfigurationValue(java.lang.String, java.lang.String)
	 */
	@Override
	public String setOrUnsetServerConfigurationValue(@Nonnull final String name,
	                                                 @Nullable final String value) throws P4JavaException {
		return configureDelegator.setOrUnsetServerConfigurationValue(name, value);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.delegator.IConfigureDelegator#
	 * showServerConfiguration(java.lang.String, java.lang.String)
	 */
	@Override
	public List<ServerConfigurationValue> showServerConfiguration(final String serverName,
	                                                              final String variableName) throws P4JavaException {
		return configureDelegator.showServerConfiguration(serverName, variableName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getDbSchema(java.util.List)
	 */
	@Override
	public List<IDbSchema> getDbSchema(List<String> tableSpecs) throws P4JavaException {
		return dbSchemaDelegator.getDbSchema(tableSpecs);
	}

	@Override
	public IClient getClient(String clientName)
			throws ConnectionException, RequestException, AccessException {
		return clientDelegator.getClient(clientName);
	}

	@Override
	public IClient getClient(@Nonnull IClientSummary clientSummary)
			throws ConnectionException, RequestException, AccessException {
		return clientDelegator.getClient(clientSummary);
	}

	@Override
	public IClient getClientTemplate(String clientName)
			throws ConnectionException, RequestException, AccessException {
		return clientDelegator.getClientTemplate(clientName);
	}

	@Override
	public IClient getClientTemplate(String clientName, boolean allowExistent)
			throws ConnectionException, RequestException, AccessException {
		return clientDelegator.getClientTemplate(clientName, allowExistent);
	}

	@Override
	public IClient getClientTemplate(String clientName, GetClientTemplateOptions opts)
			throws P4JavaException {
		return clientDelegator.getClientTemplate(clientName, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.IInterchangesDelegator#getInterchanges(com.
	 * perforce.p4java.core.file.IFileSpec,
	 * com.perforce.p4java.core.file.IFileSpec,
	 * com.perforce.p4java.option.server.GetInterchangesOptions)
	 */
	@Override
	public List<IChangelist> getInterchanges(IFileSpec fromFile, IFileSpec toFile,
	                                         GetInterchangesOptions opts) throws P4JavaException {
		return interchangesDelegator.getInterchanges(fromFile, toFile, opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.IInterchangesDelegator#getInterchanges(java.
	 * lang.String, java.util.List, java.util.List,
	 * com.perforce.p4java.option.server.GetInterchangesOptions)
	 */
	@Override
	public List<IChangelist> getInterchanges(String branchSpecName, List<IFileSpec> fromFileList,
	                                         List<IFileSpec> toFileList, GetInterchangesOptions opts) throws P4JavaException {
		return interchangesDelegator.getInterchanges(branchSpecName, fromFileList, toFileList,
				opts);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.perforce.p4java.server.IServer#getInterchanges(com.perforce.p4java.
	 * core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, boolean,
	 * boolean, int)
	 */
	@Override
	public List<IChangelist> getInterchanges(IFileSpec fromFile, IFileSpec toFile,
	                                         boolean showFiles, boolean longDesc, int maxChangelistId)
			throws ConnectionException, RequestException, AccessException {
		return interchangesDelegator.getInterchanges(fromFile, toFile, showFiles, longDesc,
				maxChangelistId);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.perforce.p4java.server.IServer#getInterchanges(java.lang.String,
	 * java.util.List, java.util.List, boolean, boolean, int, boolean, boolean)
	 */
	@Override
	public List<IChangelist> getInterchanges(String branchSpecName, List<IFileSpec> fromFileList,
	                                         List<IFileSpec> toFileList, boolean showFiles, boolean longDesc, int maxChangelistId,
	                                         boolean reverseMapping, boolean biDirectional)
			throws ConnectionException, RequestException, AccessException {
		return interchangesDelegator.getInterchanges(branchSpecName, fromFileList, toFileList,
				showFiles, longDesc, maxChangelistId, reverseMapping, biDirectional);
	}

	@Override
	public String createClient(@Nonnull IClient newClient)
			throws ConnectionException, RequestException, AccessException {
		return clientDelegator.createClient(newClient);
	}

	@Override
	public String updateClient(@Nonnull IClient client)
			throws ConnectionException, RequestException, AccessException {
		return clientDelegator.updateClient(client);
	}

	@Override
	public String updateClient(IClient client, boolean force)
			throws ConnectionException, RequestException, AccessException {
		return clientDelegator.updateClient(client, force);
	}

	@Override
	public String updateClient(IClient client, UpdateClientOptions opts) throws P4JavaException {
		return clientDelegator.updateClient(client, opts);
	}

	@Override
	public String deleteClient(String clientName, boolean force)
			throws ConnectionException, RequestException, AccessException {

		return clientDelegator.deleteClient(clientName, force);
	}

	@Override
	public String deleteClient(String clientName, DeleteClientOptions opts) throws P4JavaException {
		return clientDelegator.deleteClient(clientName, opts);
	}

	@Override
	public String switchClientView(String templateClientName, String targetClientName,
	                               SwitchClientViewOptions opts) throws P4JavaException {
		return clientDelegator.switchClientView(templateClientName, targetClientName, opts);
	}

	@Override
	public String switchStreamView(String streamPath, String targetClientName,
	                               SwitchClientViewOptions opts) throws P4JavaException {
		return clientDelegator.switchStreamView(streamPath, targetClientName, opts);
	}

	@Override
	public List<IClientSummary> getClients(final GetClientsOptions opts) throws P4JavaException {
		return clientsDelegator.getClients(opts);
	}

	@Override
	public List<IClientSummary> getClients(
			final String userName,
			final String nameFilter,
			final int maxResults) throws ConnectionException, RequestException, AccessException {

		return clientsDelegator.getClients(userName, nameFilter, maxResults);
	}

	@Override
	public String getCounter(final String counterName)
			throws ConnectionException, RequestException, AccessException {
		return counterDelegator.getCounter(counterName);
	}

	@Override
	public String getCounter(final String counterName, final CounterOptions opts)
			throws P4JavaException {
		return counterDelegator.getCounter(counterName, opts);
	}

	@Override
	public void setCounter(final String counterName, final String value,
	                       final boolean perforceCounter)
			throws ConnectionException, RequestException, AccessException {
		counterDelegator.setCounter(counterName, value, perforceCounter);
	}

	@Override
	public String setCounter(final String counterName, final String value,
	                         final CounterOptions opts) throws P4JavaException {
		return counterDelegator.setCounter(counterName, value, opts);
	}

	@Override
	public void deleteCounter(final String counterName, final boolean perforceCounter)
			throws ConnectionException, RequestException, AccessException {
		counterDelegator.deleteCounter(counterName, perforceCounter);
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.delegator.ICountersDelegator#getCounters(com.perforce.p4java.option.server.GetCountersOptions)
	 */
	@Override
	public Map<String, String> getCounters(GetCountersOptions opts) throws P4JavaException {
		return countersDelegator.getCounters(opts);
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.delegator.ICountersDelegator#getCounters(com.perforce.p4java.option.server.CounterOptions)
	 */
	@Override
	public Map<String, String> getCounters(CounterOptions opts) throws P4JavaException {
		return countersDelegator.getCounters(opts);
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.IServer#getCounters()
	 */
	@Override
	public Map<String, String> getCounters()
			throws ConnectionException, RequestException, AccessException {
		return countersDelegator.getCounters();
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.impl.mapbased.server.IServerControl#init(java.lang.String, int, java.util.Properties, com.perforce.p4java.option.UsageOptions, boolean, java.lang.String)
	 */
	@Override
	public ServerStatus init(String host, int port, Properties props, UsageOptions opts,
	                         boolean secure, String rsh) throws ConfigException, ConnectionException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.delegator.IDepotDelegator#createDepot(com.perforce.p4java.core.IDepot)
	 */
	@Override
	public String createDepot(@Nonnull IDepot newDepot) throws P4JavaException {
		return depotDelegator.createDepot(newDepot);
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.delegator.IDepotDelegator#deleteDepot(java.lang.String)
	 */
	@Override
	public String deleteDepot(String name) throws P4JavaException {
		return depotDelegator.deleteDepot(name);
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.delegator.IDepotDelegator#getDepot(java.lang.String)
	 */
	@Override
	public IDepot getDepot(String name) throws P4JavaException {
		return depotDelegator.getDepot(name);
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.IServer#getDepots()
	 */
	@Override
	public List<IDepot> getDepots() throws ConnectionException, RequestException, AccessException {
		return depotsDelegator.getDepots();
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.IServer#getRepos()
	 */
	@Override
	public List<IRepo> getRepos() throws ConnectionException, RequestException, AccessException {
		return reposDelegator.getRepos();
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.IServer#getRepos(ReposOptions)
	 */
	@Override
	public List<IRepo> getRepos(ReposOptions options) throws P4JavaException {
		return reposDelegator.getRepos(options);
	}

	/* (non-Javadoc)
	 * @see com.perforce.p4java.server.IServer#getRepos()
	 */
	@Override
	public List<IRepo> getRepos(String clientName) throws ConnectionException, RequestException, AccessException {
		return reposDelegator.getRepos(clientName);
	}

	@Override
	public List<IFileDiff> getFileDiffs(
			final IFileSpec file1,
			final IFileSpec file2,
			final String branchSpecName,
			final GetFileDiffsOptions opts) throws P4JavaException {

		return diff2Delegator.getFileDiffs(file1, file2, branchSpecName, opts);
	}

	@Override
	public InputStream getFileDiffsStream(
			final IFileSpec file1,
			final IFileSpec file2,
			final String branchSpecName,
			final GetFileDiffsOptions opts) throws P4JavaException {

		return diff2Delegator.getFileDiffsStream(file1, file2, branchSpecName, opts);
	}

	@Override
	public List<IFileDiff> getFileDiffs(
			final IFileSpec file1,
			final IFileSpec file2,
			final String branchSpecName,
			final DiffType diffType,
			final boolean quiet,
			final boolean includeNonTextDiffs,
			final boolean gnuDiffs) throws ConnectionException, RequestException, AccessException {

		return diff2Delegator.getFileDiffs(
				file1,
				file2,
				branchSpecName,
				diffType,
				quiet,
				includeNonTextDiffs,
				gnuDiffs);
	}

	@Override
	public InputStream getServerFileDiffs(
			final IFileSpec file1,
			final IFileSpec file2,
			final String branchSpecName,
			final DiffType diffType,
			final boolean quiet,
			final boolean includeNonTextDiffs,
			final boolean gnuDiffs) throws ConnectionException, RequestException, AccessException {

		return diff2Delegator.getServerFileDiffs(
				file1,
				file2,
				branchSpecName,
				diffType,
				quiet,
				includeNonTextDiffs,
				gnuDiffs);
	}

	@Override
	public List<IDiskSpace> getDiskSpace(final List<String> filesystems) throws P4JavaException {
		return diskspaceDelegator.getDiskSpace(filesystems);
	}

	@Override
	public List<IFileSpec> duplicateRevisions(
			final IFileSpec fromFile,
			final IFileSpec toFile,
			final DuplicateRevisionsOptions opts) throws P4JavaException {

		return duplicateDelegator.duplicateRevisions(fromFile, toFile, opts);
	}

	@Override
	public List<Map<String, Object>> getExportRecords(final ExportRecordsOptions opts)
			throws P4JavaException {
		return exportDelegator.getExportRecords(opts);
	}

	@Override
	public void getStreamingExportRecords(
			final ExportRecordsOptions opts,
			@Nonnull final IStreamingCallback callback,
			final int key) throws P4JavaException {

		exportDelegator.getStreamingExportRecords(opts, callback, key);
	}

	@Override
	public List<Map<String, Object>> getExportRecords(
			final boolean useJournal,
			final long maxRecs,
			final int sourceNum,
			final long offset,
			final boolean format,
			final String journalPrefix,
			final String filter) throws ConnectionException, RequestException, AccessException {

		return exportDelegator.getExportRecords(
				useJournal,
				maxRecs,
				sourceNum,
				offset,
				format,
				journalPrefix,
				filter);
	}

	@Override
	public List<IFileAnnotation> getFileAnnotations(
			final List<IFileSpec> fileSpecs,
			@Nonnull final DiffType wsOpts,
			final boolean allResults,
			final boolean useChangeNumbers,
			final boolean followBranches) throws ConnectionException, RequestException, AccessException {

		return fileAnnotateDelegator.getFileAnnotations(
				fileSpecs,
				wsOpts,
				allResults,
				useChangeNumbers,
				followBranches);
	}

	@Override
	public List<IFileAnnotation> getFileAnnotations(
			final List<IFileSpec> fileSpecs,
			final GetFileAnnotationsOptions opts) throws P4JavaException {

		return fileAnnotateDelegator.getFileAnnotations(fileSpecs, opts);
	}

	@Override
	public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(
			final List<IFileSpec> fileSpecs,
			final GetRevisionHistoryOptions opts) throws P4JavaException {

		return fileLogDelegator.getRevisionHistory(fileSpecs, opts);
	}

	@Override
	public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(
			final List<IFileSpec> fileSpecs,
			final int maxRevs,
			final boolean contentHistory,
			final boolean includeInherited,
			final boolean longOutput,
			final boolean truncatedLongOutput) throws ConnectionException, AccessException {

		return fileLogDelegator.getRevisionHistory(
				fileSpecs,
				maxRevs,
				contentHistory,
				includeInherited,
				longOutput,
				truncatedLongOutput);
	}

	@Override
	public List<IFileSpec> getDirectories(@Nonnull final List<IFileSpec> fileSpecs,
	                                      final boolean clientOnly, final boolean deletedOnly, final boolean haveListOnly)
			throws ConnectionException, AccessException {
		return dirsDelegator.getDirectories(fileSpecs, clientOnly, deletedOnly, haveListOnly);
	}

	@Override
	public List<IFileSpec> getDirectories(final List<IFileSpec> fileSpecs,
	                                      final GetDirectoriesOptions opts) throws P4JavaException {
		return dirsDelegator.getDirectories(fileSpecs, opts);
	}

	@Override
	public List<IFileSpec> getDepotFiles(@Nonnull final List<IFileSpec> fileSpecs,
	                                     final boolean allRevs) throws ConnectionException, AccessException {

		return filesDelegator.getDepotFiles(fileSpecs, allRevs);
	}

	@Override
	public List<IFileSpec> getDepotFiles(@Nonnull final List<IFileSpec> fileSpecs,
	                                     final GetDepotFilesOptions opts) throws P4JavaException {

		return filesDelegator.getDepotFiles(fileSpecs, opts);
	}

	@Override
	public List<IFix> fixJobs(final List<String> jobIds, final int changeListId,
	                          final String status, final boolean delete)
			throws ConnectionException, RequestException, AccessException {
		return fixDelegator.fixJobs(jobIds, changeListId, status, delete);
	}

	@Override
	public List<IFix> fixJobs(@Nonnull final List<String> jobIds, final int changeListId,
	                          final FixJobsOptions opts) throws P4JavaException {
		return fixDelegator.fixJobs(jobIds, changeListId, opts);
	}

	@Override
	public List<IFix> getFixList(final List<IFileSpec> fileSpecs, final int changeListId,
	                             final String jobId, final boolean includeIntegrations, final int maxFixes)
			throws ConnectionException, RequestException, AccessException {
		return fixesDelegator.getFixList(fileSpecs, changeListId, jobId,
				includeIntegrations, maxFixes);
	}

	@Override
	public List<IFix> getFixes(final List<IFileSpec> fileSpecs, final GetFixesOptions opts)
			throws P4JavaException {
		return fixesDelegator.getFixes(fileSpecs, opts);
	}

	@Override
	public List<IExtendedFileSpec> getExtendedFiles(final List<IFileSpec> fileSpecs,
	                                                final int maxFiles, final int sinceChangelist, final int affectedByChangelist,
	                                                final FileStatOutputOptions outputOptions,
	                                                final FileStatAncilliaryOptions ancilliaryOptions)
			throws ConnectionException, AccessException {
		return fstatDelegator.getExtendedFiles(fileSpecs, maxFiles, sinceChangelist,
				affectedByChangelist, outputOptions, ancilliaryOptions);
	}

	@Override
	public List<IExtendedFileSpec> getExtendedFiles(final List<IFileSpec> fileSpecs,
	                                                final GetExtendedFilesOptions opts) throws P4JavaException {
		return fstatDelegator.getExtendedFiles(fileSpecs, opts);
	}

	@Override
	public List<IFileLineMatch> getMatchingLines(List<IFileSpec> fileSpecs,
	                                             String pattern, MatchingLinesOptions options) throws P4JavaException {
		return grepDelegator.getMatchingLines(fileSpecs, pattern, options);
	}

	@Override
	public List<IFileLineMatch> getMatchingLines(@Nonnull List<IFileSpec> fileSpecs,
	                                             // p4ic4idea: use IServerMessage for info lines
	                                             @Nonnull String pattern, @Nullable List<IServerMessage> infoLines,
	                                             MatchingLinesOptions options) throws P4JavaException {
		return grepDelegator.getMatchingLines(fileSpecs, pattern, infoLines, options);
	}

	@Override
	public IServerInfo getServerInfo() throws ConnectionException, RequestException, AccessException {
		return infoDelegator.getServerInfo();
	}

	@Override
	public String createUserGroup(IUserGroup group)
			throws ConnectionException, RequestException, AccessException {
		return groupDelegator.createUserGroup(group);
	}

	@Override
	public String createUserGroup(IUserGroup group, UpdateUserGroupOptions opts)
			throws P4JavaException {
		return groupDelegator.createUserGroup(group, opts);
	}

	@Override
	public String deleteUserGroup(IUserGroup group)
			throws ConnectionException, RequestException, AccessException {
		return groupDelegator.deleteUserGroup(group);
	}

	@Override
	public String deleteUserGroup(IUserGroup group, UpdateUserGroupOptions opts)
			throws P4JavaException {
		return groupDelegator.deleteUserGroup(group, opts);
	}

	@Override
	public IUserGroup getUserGroup(String name)
			throws ConnectionException, RequestException, AccessException {
		return groupDelegator.getUserGroup(name);
	}

	@Override
	public String updateUserGroup(IUserGroup group, boolean updateIfOwner)
			throws ConnectionException, RequestException, AccessException {
		return groupDelegator.updateUserGroup(group, updateIfOwner);
	}

	@Override
	public String updateUserGroup(IUserGroup group, UpdateUserGroupOptions opts)
			throws P4JavaException {
		return groupDelegator.updateUserGroup(group, opts);
	}

	@Override
	public List<IUserGroup> getUserGroups(String userOrGroupName, GetUserGroupsOptions opts)
			throws P4JavaException {
		return groupsDelegator.getUserGroups(userOrGroupName, opts);
	}

	@Override
	public List<IUserGroup> getUserGroups(String userOrGroupName, boolean indirect,
	                                      boolean displayValues, int maxGroups)
			throws ConnectionException, RequestException, AccessException {
		return groupsDelegator.getUserGroups(userOrGroupName, indirect, displayValues, maxGroups);
	}

	@Override
	public List<IFileSpec> getSubmittedIntegrations(List<IFileSpec> fileSpecs, String branchSpec,
	                                                boolean reverseMappings) throws ConnectionException, RequestException, AccessException {
		return integratedDelegator.getSubmittedIntegrations(fileSpecs, branchSpec, reverseMappings);
	}

	@Override
	public List<IFileSpec> getSubmittedIntegrations(List<IFileSpec> fileSpecs,
	                                                GetSubmittedIntegrationsOptions opts) throws P4JavaException {
		return integratedDelegator.getSubmittedIntegrations(fileSpecs, opts);
	}

	@Override
	public IStreamIntegrationStatus getStreamIntegrationStatus(final String stream,
	                                                           final StreamIntegrationStatusOptions opts) throws P4JavaException {
		return statDelegator.getStreamIntegrationStatus(stream, opts);
	}

	@Override
	public IJob createJob(@Nonnull Map<String, Object> fieldMap)
			throws ConnectionException, RequestException, AccessException {
		return jobDelegator.createJob(fieldMap);
	}

	@Override
	public String deleteJob(String jobId) throws ConnectionException, RequestException, AccessException {
		return jobDelegator.deleteJob(jobId);
	}

	@Override
	public IJob getJob(String jobId) throws ConnectionException, RequestException, AccessException {
		return jobDelegator.getJob(jobId);
	}

	@Override
	public String updateJob(@Nonnull IJob job)
			throws ConnectionException, RequestException, AccessException {
		return jobDelegator.updateJob(job);
	}

	@Override
	public List<IJob> getJobs(final List<IFileSpec> fileSpecs, final int maxJobs,
	                          final boolean longDescriptions, final boolean reverseOrder,
	                          final boolean includeIntegrated, final String jobView)
			throws ConnectionException, RequestException, AccessException {
		return jobsDelegator.getJobs(fileSpecs, maxJobs, longDescriptions, reverseOrder, includeIntegrated, jobView);
	}

	@Override
	public List<IJob> getJobs(final List<IFileSpec> fileSpecs, final GetJobsOptions opts)
			throws P4JavaException {
		return jobsDelegator.getJobs(fileSpecs, opts);
	}

	@Override
	public IJobSpec getJobSpec() throws ConnectionException, RequestException, AccessException {
		return jobSpecDelegator.getJobSpec();
	}

	@Override
	public String deleteKey(final String keyName) throws P4JavaException {
		return keyDelegator.deleteKey(keyName);
	}

	@Override
	public String setKey(final String keyName, final String value, final KeyOptions opts)
			throws P4JavaException {

		return keyDelegator.setKey(keyName, value, opts);
	}

	@Override
	public String getKey(final String keyName) throws P4JavaException {
		return keyDelegator.getKey(keyName);
	}

	@Override
	public Map<String, String> getKeys(final GetKeysOptions opts) throws P4JavaException {
		return keysDelegator.getKeys(opts);
	}

	@Override
	public String createLabel(@Nonnull final ILabel label)
			throws ConnectionException, RequestException, AccessException {

		return labelDelegator.createLabel(label);
	}

	@Override
	public String deleteLabel(final String labelName, final boolean force)
			throws ConnectionException, RequestException, AccessException {

		return labelDelegator.deleteLabel(labelName, force);
	}

	@Override
	public String deleteLabel(final String labelName, final DeleteLabelOptions opts)
			throws P4JavaException {

		return labelDelegator.deleteLabel(labelName, opts);
	}

	@Override
	public ILabel getLabel(final String labelName)
			throws ConnectionException, RequestException, AccessException {

		return labelDelegator.getLabel(labelName);
	}

	@Override
	public String updateLabel(@Nonnull final ILabel label)
			throws ConnectionException, RequestException, AccessException {

		return labelDelegator.updateLabel(label);
	}

	@Override
	public List<ILabelSummary> getLabels(
			final String user,
			final int maxLabels,
			final String nameFilter,
			final List<IFileSpec> fileList)
			throws ConnectionException, RequestException, AccessException {

		return labelsDelegator.getLabels(user, maxLabels, nameFilter, fileList);
	}

	@Override
	public List<ILabelSummary> getLabels(
			final List<IFileSpec> fileList,
			final GetLabelsOptions opts) throws P4JavaException {

		return labelsDelegator.getLabels(fileList, opts);
	}

	@Override
	public void journalWait(final JournalWaitOptions opts) throws P4JavaException {
		journalWaitDelegator.journalWait(opts);
	}

	@Override
	public String getLoginStatus() throws P4JavaException {
		return loginDelegator.getLoginStatus();
	}

	@Override
	public void login(final String password)
			throws ConnectionException, RequestException, AccessException, ConfigException {
		loginDelegator.login(password);
	}

	@Override
	public void login(final String password, final boolean allHosts)
			throws ConnectionException, RequestException, AccessException, ConfigException {
		loginDelegator.login(password, allHosts);
	}

	@Override
	public void login(final String password, final LoginOptions opts) throws P4JavaException {
		loginDelegator.login(password, opts);
	}

	@Override
	public void login(final String password, final StringBuffer ticket, final LoginOptions opts)
			throws P4JavaException {
		loginDelegator.login(password, ticket, opts);
	}

	@Override
	public void login(@Nonnull final IUser user, final StringBuffer ticket, final LoginOptions opts)
			throws P4JavaException {
		loginDelegator.login(user, ticket, opts);
	}

	@Override
	public boolean isDontWriteTicket(final String cmd, final String[] cmdArgs) {
		return loginDelegator.isDontWriteTicket(cmd, cmdArgs);
	}

	@Override
	public List<Map<String, Object>> login2(Login2Options opts, String user) throws P4JavaException {
		return login2Delegator.login2(opts, user);
	}

	@Override
	// p4ic4idea: use iServerMessage
	public IServerMessage getLogin2Status() throws P4JavaException {
		return login2Delegator.getLogin2Status();
	}

	@Override
	// p4ic4idea: use iServerMessage
	public IServerMessage getLogin2Status(IUser user) throws P4JavaException {
		return login2Delegator.getLogin2Status(user);
	}

	@Override
	public Map<String, String> login2ListMethods() throws P4JavaException {
		return login2Delegator.login2ListMethods();
	}

	@Override
	public String login2InitAuth(String method) throws P4JavaException {
		return login2Delegator.login2InitAuth(method);
	}

	@Override
	public String login2CheckAuth(String auth, boolean persist) throws P4JavaException {
		return login2Delegator.login2CheckAuth(auth, persist);
	}

	@Override
	public String login2(IUser user, Login2Options opts) throws P4JavaException {
		return login2Delegator.login2(user, opts);
	}

	@Override
	public void logout()
			throws ConnectionException, RequestException, AccessException, ConfigException {
		logoutDelegator.logout();
	}

	@Override
	public void logout(final LoginOptions opts) throws P4JavaException {
		logoutDelegator.logout(opts);
	}

	@Override
	public List<IServerProcess> getServerProcesses()
			throws ConnectionException, RequestException, AccessException {

		return monitorDelegator.getServerProcesses();
	}

	@Override
	public List<IServerProcess> getServerProcesses(final GetServerProcessesOptions opts)
			throws P4JavaException {

		return monitorDelegator.getServerProcesses(opts);
	}

	@Override
	public ILogTail getLogTail(final LogTailOptions opts) throws P4JavaException {
		return logTailDelegator.getLogTail(opts);
	}

	@Override
	public List<IFileSpec> getOpenedFiles(final List<IFileSpec> fileSpecs, final boolean allClients,
	                                      final String clientName, final int maxFiles, final int changeListId)
			throws ConnectionException, AccessException {
		return openedDelegator.getOpenedFiles(fileSpecs, allClients, clientName, maxFiles, changeListId);
	}

	@Override
	public List<IFileSpec> getOpenedFiles(final List<IFileSpec> fileSpecs,
	                                      final OpenedFilesOptions opts) throws P4JavaException {
		return openedDelegator.getOpenedFiles(fileSpecs, opts);
	}

	@Override
	public List<IFileSpec> moveFile(
			final int changelistId,
			final boolean listOnly,
			final boolean noClientMove,
			final String fileType,
			@Nonnull final IFileSpec fromFile,
			@Nonnull final IFileSpec toFile)
			throws ConnectionException, RequestException, AccessException {

		return moveDelegator.moveFile(
				changelistId,
				listOnly,
				noClientMove,
				fileType,
				fromFile,
				toFile);
	}

	@Override
	public List<IFileSpec> moveFile(
			@Nonnull IFileSpec fromFile,
			@Nonnull IFileSpec toFile,
			@Nullable MoveFileOptions opts) throws P4JavaException {

		return moveDelegator.moveFile(fromFile, toFile, opts);
	}

	@Override
	public List<IObliterateResult> obliterateFiles(
			@Nonnull final List<IFileSpec> fileSpecs,
			final ObliterateFilesOptions opts) throws P4JavaException {

		return obliterateDelegator.obliterateFiles(fileSpecs, opts);
	}

	@Override
	public String changePassword(
			final String oldPassword,
			final String newPassword,
			final String userName) throws P4JavaException {
		return passwdDelegator.changePassword(oldPassword, newPassword, userName);
	}

	@Override
	public InputStream getFileContents(
			final List<IFileSpec> fileSpecs,
			final GetFileContentsOptions opts) throws P4JavaException {

		return printDelegator.getFileContents(fileSpecs, opts);
	}

	@Override
	public InputStream getFileContents(
			final List<IFileSpec> fileSpecs,
			final boolean allrevs,
			final boolean noHeaderLine)
			throws ConnectionException, RequestException, AccessException {

		return printDelegator.getFileContents(fileSpecs, allrevs, noHeaderLine);
	}

	@Override
	public String setProperty(
			final String name,
			final String value,
			final PropertyOptions opts) throws P4JavaException {

		return propertyDelegator.setProperty(name, value, opts);
	}

	@Override
	public List<IProperty> getProperty(final GetPropertyOptions opts)
			throws P4JavaException {

		return propertyDelegator.getProperty(opts);
	}

	@Override
	public String deleteProperty(
			final String name,
			final PropertyOptions opts) throws P4JavaException {

		return propertyDelegator.deleteProperty(name, opts);
	}

	@Override
	public String createProtectionEntries(@Nonnull final List<IProtectionEntry> entryList)
			throws P4JavaException {

		return protectDelegator.createProtectionEntries(entryList);
	}

	@Override
	public String updateProtectionEntries(@Nonnull final List<IProtectionEntry> entryList)
			throws P4JavaException {

		return protectDelegator.updateProtectionEntries(entryList);
	}

	@Override
	public InputStream getProtectionsTable() throws P4JavaException {
		return protectDelegator.getProtectionsTable();
	}

	@Override
	public List<IProtectionEntry> getProtectionEntries(
			final List<IFileSpec> fileList,
			final GetProtectionEntriesOptions opts) throws P4JavaException {

		return protectsDelegator.getProtectionEntries(fileList, opts);
	}

	@Override
	public List<IProtectionEntry> getProtectionEntries(
			final boolean allUsers,
			final String hostName,
			final String userName,
			final String groupName,
			final List<IFileSpec> fileList)
			throws ConnectionException, RequestException, AccessException {

		return protectsDelegator.getProtectionEntries(
				allUsers,
				hostName,
				userName,
				groupName,
				fileList);
	}

	@Override
	public String reload(final ReloadOptions opts) throws P4JavaException {
		return reloadDelegator.reload(opts);
	}

	@Override
	public String renameUser(
			final String oldUserName,
			final String newUserName) throws P4JavaException {

		return renameUserDelegator.renameUser(oldUserName, newUserName);
	}

	@Override
	public List<IReviewChangelist> getReviewChangelists(final GetReviewChangelistsOptions opts)
			throws P4JavaException {

		return reviewDelegator.getReviewChangelists(opts);
	}

	@Override
	public List<IUserSummary> getReviews(
			final int changelistId,
			final List<IFileSpec> fileSpecs)
			throws ConnectionException, RequestException, AccessException {

		return reviewsDelegator.getReviews(changelistId, fileSpecs);
	}

	@Override
	public List<IUserSummary> getReviews(
			final List<IFileSpec> fileSpecs,
			final GetReviewsOptions opts) throws P4JavaException {

		return reviewsDelegator.getReviews(fileSpecs, opts);
	}

	@Override
	public List<String> searchJobs(
			final String words,
			final SearchJobsOptions opts) throws P4JavaException {

		return searchDelegator.searchJobs(words, opts);
	}

	@Override
	public List<IFileSize> getFileSizes(
			final List<IFileSpec> fileSpecs,
			final GetFileSizesOptions opts) throws P4JavaException {

		return sizesDelegator.getFileSizes(fileSpecs, opts);
	}

	@Override
	public String createStream(@Nonnull final IStream stream) throws P4JavaException {
		return streamDelegator.createStream(stream);
	}

	@Override
	public IStream getStream(@Nonnull final String streamPath) throws P4JavaException {
		return streamDelegator.getStream(streamPath);
	}

	@Override
	public IStream getStream(
			final String streamPath,
			final GetStreamOptions opts) throws P4JavaException {

		return streamDelegator.getStream(streamPath, opts);
	}

	@Override
	public String updateStream(
			final IStream stream,
			final StreamOptions opts) throws P4JavaException {

		return streamDelegator.updateStream(stream, opts);
	}

	@Override
	public String deleteStream(
			final String streamPath,
			final StreamOptions opts) throws P4JavaException {

		return streamDelegator.deleteStream(streamPath, opts);
	}

	@Override
	public List<IStreamSummary> getStreams(
			final List<String> streamPaths,
			final GetStreamsOptions opts) throws P4JavaException {

		return streamsDelegator.getStreams(streamPaths, opts);
	}

	@Override
	public List<IFileSpec> tagFiles(
			List<IFileSpec> fileSpecs,
			String labelName,
			boolean listOnly,
			boolean delete) throws ConnectionException, RequestException, AccessException {

		return tagDelegator.tagFiles(fileSpecs, labelName, listOnly, delete);
	}

	@Override
	public List<IFileSpec> tagFiles(
			final List<IFileSpec> fileSpecs,
			final String labelName,
			final TagFilesOptions opts) throws P4JavaException {

		return tagDelegator.tagFiles(fileSpecs, labelName, opts);
	}

	@Override
	public String createTriggerEntries(@Nonnull final List<ITriggerEntry> entryList)
			throws P4JavaException {

		return triggersDelegator.createTriggerEntries(entryList);
	}

	@Override
	public List<ITriggerEntry> getTriggerEntries() throws P4JavaException {
		return triggersDelegator.getTriggerEntries();
	}

	@Override
	public String updateTriggerEntries(@Nonnull final List<ITriggerEntry> entryList)
			throws P4JavaException {

		return triggersDelegator.updateTriggerEntries(entryList);
	}

	@Override
	public InputStream getTriggersTable() throws P4JavaException {
		return triggersDelegator.getTriggersTable();
	}

	@Override
	public String createUser(
			@Nonnull final IUser user,
			final boolean force) throws ConnectionException, RequestException, AccessException {

		return userDelegator.createUser(user, force);
	}

	@Override
	public String createUser(
			@Nonnull final IUser user,
			final UpdateUserOptions opts) throws P4JavaException {

		return userDelegator.createUser(user, opts);
	}

	@Override
	public String updateUser(
			@Nonnull final IUser user,
			final UpdateUserOptions opts) throws P4JavaException {

		return userDelegator.updateUser(user, opts);
	}

	@Override
	public String updateUser(
			@Nonnull final IUser user,
			final boolean force)
			throws ConnectionException, RequestException, AccessException {

		return userDelegator.updateUser(user, force);
	}

	@Override
	public String deleteUser(
			final String userName,
			final boolean force) throws ConnectionException, RequestException, AccessException {

		return userDelegator.deleteUser(userName, force);
	}

	@Override
	public String deleteUser(
			String userName,
			UpdateUserOptions opts) throws P4JavaException {

		return userDelegator.deleteUser(userName, opts);
	}

	@Override
	public IUser getUser(final String userName)
			throws ConnectionException, RequestException, AccessException {

		return userDelegator.getUser(userName);
	}

	@Override
	public List<IUserSummary> getUsers(
			final List<String> userList,
			final int maxUsers)
			throws ConnectionException, RequestException, AccessException {

		return usersDelegator.getUsers(userList, maxUsers);
	}

	@Override
	public List<IUserSummary> getUsers(
			final List<String> userList,
			final GetUsersOptions opts) throws P4JavaException {

		return usersDelegator.getUsers(userList, opts);
	}

	@Override
	public String unload(final UnloadOptions opts) throws P4JavaException {
		return unloadDelegator.unload(opts);
	}

	@Override
	public List<IExtendedFileSpec> verifyFiles(
			final List<IFileSpec> fileSpecs,
			final VerifyFilesOptions opts) throws P4JavaException {

		return verifyDelegator.verifyFiles(fileSpecs, opts);
	}

	/**
	 * Usage: ls-tree {tree-sha}
	 *
	 * @param sha
	 * @return
	 */
	@Override
	public List<IGraphListTree> getGraphListTree(String sha) throws P4JavaException {

		return graphListTreeDelegator.getGraphListTree(sha);
	}

	/**
	 * Usage: show-ref [ -a -n {repo} -u {user} -t {type} -m {max} ]
	 *
	 * @param opts
	 * @return
	 */
	@Override
	public List<IGraphRef> getGraphShowRefs(GraphShowRefOptions opts) throws P4JavaException {

		return graphShowRefDelegator.getGraphShowRefs(opts);
	}

	/**
	 * Usage: cat-file commit {object-sha}
	 *
	 * @param sha
	 * @return
	 */
	@Override
	public ICommit getCommitObject(String sha) throws P4JavaException {
		return graphCommitDelegator.getCommitObject(sha);
	}

	/**
	 * Usage: cat-file -n {repo} commit {object-sha}
	 *
	 * @param sha
	 * @return
	 */
	@Override
	public ICommit getCommitObject(String sha, String repo) throws P4JavaException {
		return graphCommitDelegator.getCommitObject(sha, repo);
	}

	/**
	 * Usage: cat-file -n {repo} blob {object-sha}
	 *
	 * @param repo
	 * @param sha
	 * @return
	 */
	@Override
	public InputStream getBlobObject(String repo, String sha) throws P4JavaException {
		return graphCommitDelegator.getBlobObject(repo, sha);
	}

	/**
	 * Usage: cat-file -t {object-sha}
	 *
	 * @param sha
	 * @return
	 */
	@Override
	public IGraphObject getGraphObject(String sha) throws P4JavaException {
		return graphCommitDelegator.getGraphObject(sha);
	}

	/**
	 * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder}
	 */
	@Deprecated
	public IFileSpec handleFileReturn(Map<String, Object> map)
			throws AccessException, ConnectionException {
		return ResultListBuilder.handleFileReturn(map, this);
	}

	/**
	 * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder}
	 */
	@Deprecated
	public IFileSpec handleFileReturn(Map<String, Object> map, IClient client)
			throws AccessException, ConnectionException {
		return ResultListBuilder.handleFileReturn(map, this);
	}

	public abstract IServerAddress getServerAddressDetails();
}
