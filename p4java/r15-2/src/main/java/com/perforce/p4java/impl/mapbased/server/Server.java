/*
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */

package com.perforce.p4java.impl.mapbased.server;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.Metadata;
import com.perforce.p4java.PropertyDefs;
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
import com.perforce.p4java.core.IReviewChangelist;
import com.perforce.p4java.core.IServerProcess;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamIntegrationStatus;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.FileDiff;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSize;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.env.PerforceEnvironment;
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.generic.admin.DbSchema;
import com.perforce.p4java.impl.generic.admin.DiskSpace;
import com.perforce.p4java.impl.generic.admin.LogTail;
import com.perforce.p4java.impl.generic.admin.Property;
import com.perforce.p4java.impl.generic.admin.ProtectionEntry;
import com.perforce.p4java.impl.generic.admin.ProtectionsTable;
import com.perforce.p4java.impl.generic.admin.TriggerEntry;
import com.perforce.p4java.impl.generic.admin.TriggersTable;
import com.perforce.p4java.impl.generic.core.BranchSpec;
import com.perforce.p4java.impl.generic.core.BranchSpecSummary;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.ChangelistSummary;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.FileLineMatch;
import com.perforce.p4java.impl.generic.core.Fix;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.generic.core.Job;
import com.perforce.p4java.impl.generic.core.JobSpec;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.impl.generic.core.LabelSummary;
import com.perforce.p4java.impl.generic.core.ReviewChangelist;
import com.perforce.p4java.impl.generic.core.ServerProcess;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.generic.core.StreamIntegrationStatus;
import com.perforce.p4java.impl.generic.core.StreamSummary;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.impl.generic.core.UserSummary;
import com.perforce.p4java.impl.generic.core.file.ExtendedFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileAnnotation;
import com.perforce.p4java.impl.generic.core.file.FilePath.PathType;
import com.perforce.p4java.impl.generic.core.file.FileRevisionData;
import com.perforce.p4java.impl.generic.core.file.FileSize;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.generic.core.file.ObliterateResult;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.mapbased.client.ClientSummary;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.DeleteBranchSpecOptions;
import com.perforce.p4java.option.server.DeleteClientOptions;
import com.perforce.p4java.option.server.DeleteLabelOptions;
import com.perforce.p4java.option.server.DescribeOptions;
import com.perforce.p4java.option.server.DuplicateRevisionsOptions;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.option.server.FixJobsOptions;
import com.perforce.p4java.option.server.GetBranchSpecOptions;
import com.perforce.p4java.option.server.GetBranchSpecsOptions;
import com.perforce.p4java.option.server.GetChangelistDiffsOptions;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.GetClientTemplateOptions;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.option.server.GetCountersOptions;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.option.server.GetDirectoriesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.option.server.GetFileDiffsOptions;
import com.perforce.p4java.option.server.GetFileSizesOptions;
import com.perforce.p4java.option.server.GetFixesOptions;
import com.perforce.p4java.option.server.GetInterchangesOptions;
import com.perforce.p4java.option.server.GetJobsOptions;
import com.perforce.p4java.option.server.GetKeysOptions;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.option.server.GetPropertyOptions;
import com.perforce.p4java.option.server.GetProtectionEntriesOptions;
import com.perforce.p4java.option.server.GetReviewsOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.option.server.GetServerProcessesOptions;
import com.perforce.p4java.option.server.GetStreamOptions;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.option.server.GetSubmittedIntegrationsOptions;
import com.perforce.p4java.option.server.GetUserGroupsOptions;
import com.perforce.p4java.option.server.GetUsersOptions;
import com.perforce.p4java.option.server.JournalWaitOptions;
import com.perforce.p4java.option.server.KeyOptions;
import com.perforce.p4java.option.server.LogTailOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.MatchingLinesOptions;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.option.server.GetReviewChangelistsOptions;
import com.perforce.p4java.option.server.PropertyOptions;
import com.perforce.p4java.option.server.ReloadOptions;
import com.perforce.p4java.option.server.SearchJobsOptions;
import com.perforce.p4java.option.server.SetFileAttributesOptions;
import com.perforce.p4java.option.server.StreamIntegrationStatusOptions;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.option.server.TagFilesOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.option.server.UnloadOptions;
import com.perforce.p4java.option.server.UpdateClientOptions;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.option.server.VerifyFilesOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.Fingerprint;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.server.ServerStatus;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.server.callback.IProgressCallback;
import com.perforce.p4java.server.callback.ISSOCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;

/**
 * Generic abstract superclass for implementation-specific server implementations that use a 
 * command-style server interface implementation.<p>
 * 
 * Normal users should not be creating this class or subclasses of this class
 * directly; you should use the ServerFactory server factory methods to
 * get a suitable server implementation class.<p>
 */

public abstract class Server implements IServerControl, IOptionsServer {
	
	// The _FIELD_NAME names below MUST correspond to the names of the
	// static fields used in the individual server impl classes; those
	// fields MUST also be static...
	
	public static final String SCREEN_NAME_FIELD_NAME = "SCREEN_NAME";
	public static final String IMPL_COMMENTS_FIELD_NAME = "IMPL_COMMENTS";
	public static final String IMPL_TYPE_FIELD_NAME = "IMPL_TYPE";
	public static final String MINIMUM_SUPPORTED_SERVER_LEVEL_FIELD_NAME = "MINIMUM_SUPPORTED_SERVER_LEVEL";
	public static final String PROTOCOL_NAME_FIELD_NAME = "PROTOCOL_NAME";
	public static final String DEFAULT_STATUS_FIELD_NAME = "DEFAULT_STATUS";
	
	/**
	 * Property key for overriding the default tagged/non-tagged behavior of a
	 * command. This is a per-command property, set on a command's "inMap".
	 */
	public static final String IN_MAP_USE_TAGS_KEY = "useTags";

	// Prefix used for the (anomalous) setFileAttributes stream map:
	public static final String ATTRIBUTE_STREAM_MAP_KEY = "attributeInstream";
	
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

	/**
	 * P4TICKETS environment variable
	 */
	public static final String P4TICKETS_ENV_VAR = "P4TICKETS";

	/**
	 * P4TICKETS_DEFAULT_WINDOWS
	 */
	public static final String P4TICKETS_DEFAULT_WINDOWS = "p4tickets.txt";

	/**
	 * P4TICKETS_DEFAULT_OTHER
	 */
	public static final String P4TICKETS_DEFAULT_OTHER = ".p4tickets";
	
	/**
	 * P4TRUST environment variable
	 */
	public static final String P4TRUST_ENV_VAR = "P4TRUST";

	/**
	 * P4TRUST_DEFAULT_WINDOWS
	 */
	public static final String P4TRUST_DEFAULT_WINDOWS = "p4trust.txt";

	/**
	 * P4TRUST_DEFAULT_OTHER
	 */
	public static final String P4TRUST_DEFAULT_OTHER = ".p4trust";

	/**
	 * P4IGNORE environment variable
	 */
	public static final String P4IGNORE_ENV_VAR = "P4IGNORE";
	
	protected UsageOptions usageOptions = null;
	
	protected static final int UNKNOWN_SERVER_VERSION = -1;
	protected static final String UNKNOWN_SERVER_HOST = null;
	protected static final int UNKNOWN_SERVER_PORT = -1;
	
	protected ServerStatus status = ServerStatus.UNKNOWN;
	protected Properties props = null;
	
	protected IServerInfo serverInfo = null;
	protected String serverAddress = null;

	protected boolean caseSensitive = true;
	protected int serverVersion = UNKNOWN_SERVER_VERSION;
	protected String serverHost = UNKNOWN_SERVER_HOST;
	protected int serverPort = UNKNOWN_SERVER_PORT;
	
	protected String userName = null;
	protected String password = null;
	
    /**
     * Storage for user auth tickets. What's returned from p4 login -p command,
     * and what we can add to each command when non-null to authenticate it
     */
	protected Map<String, String> authTickets = new HashMap<String, String>();
    
	protected IClient client = null;
	protected String clientName = null;
	
	/**
	 * Used when we have no client set.
	 */
	protected String clientUnsetName = PropertyDefs.CLIENT_UNSET_NAME_DEFAULT;

	protected boolean setupOnConnect = false;
	protected boolean loginOnConnect = false;
	
	protected ICommandCallback commandCallback = null;
	protected IProgressCallback progressCallback = null;
	protected ISSOCallback ssoCallback = null;
	protected String ssoKey = null;
	
	protected String charsetName = null;
	protected Charset charset = null;

	protected boolean connected = false;
	
	protected int minumumSupportedServerVersion = Metadata.DEFAULT_MINIMUM_SUPPORTED_SERVER_VERSION;
	
	protected String tmpDirName = null;
	
	protected AtomicInteger nextCmdCallBackKey = new AtomicInteger();
	protected AtomicInteger nextProgressCallbackKey = new AtomicInteger();
	
	protected static boolean runningOnWindows = SystemInfo.isWindows();
	
	protected boolean nonCheckedSyncs = false;

	protected boolean enableTracking = false;
	
	protected boolean enableProgress = false;

	protected boolean quietMode = false;

	protected boolean secure = false;

	protected boolean useAuthMemoryStore = false;

	protected String ignoreFileName = null;

	protected String rsh = null;

    /**
     * Useful source of random integers, etc.
     */
    protected Random rand = new Random(System.currentTimeMillis());
	
	/**
	 * @see com.perforce.p4java.server.IServer#registerCallback(com.perforce.p4java.server.callback.ICommandCallback)
	 */
	public ICommandCallback registerCallback(ICommandCallback callback) {
		ICommandCallback oldCallback = this.commandCallback;
		this.commandCallback = callback;
		return oldCallback;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#registerProgressCallback(com.perforce.p4java.server.callback.IProgressCallback)
	 */
	
	public IProgressCallback registerProgressCallback(IProgressCallback progressCallback) {
		IProgressCallback oldCallback = this.progressCallback;
		this.progressCallback = progressCallback;
		return oldCallback;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#registerSSOCallback(com.perforce.p4java.server.callback.ISSOCallback, java.lang.String)
	 */
	public ISSOCallback registerSSOCallback(ISSOCallback callback, String ssoKey) {
		ISSOCallback oldCallback = this.ssoCallback;
		this.ssoCallback = callback;
		this.ssoKey = ssoKey;
		return oldCallback;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getStatus()
	 */
	public ServerStatus getStatus() {
		return status;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getServerVersionNumber()
	 */
	public int getServerVersionNumber() {
		return this.serverVersion;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#isCaseSensitive()
	 */
	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#setCharsetName(java.lang.String)
	 */
	public boolean setCharsetName(String charsetName) throws UnsupportedCharsetException {
		if (charsetName != null) {
			// Check if it is a supported Perforce charset
			if (!PerforceCharsets.isSupported(charsetName)) {	 
				throw new UnsupportedCharsetException(charsetName);
			}
			// Get the Java equivalent charset for this Perforce charset
			String javaCharsetName = PerforceCharsets.getJavaCharsetName(charsetName);
			if (javaCharsetName != null) {
				try {
					this.charset = Charset.forName(javaCharsetName);
				} catch (UnsupportedCharsetException uce) {
					// In case P4Java's Perforce extended charsets are not
					// loaded in the VM's bootstrap classpath (i.e. P4Java JAR
					// file is inside a WAR deployed in a web app container like
					// Jetty, Tomcat, etc.), we'll instantiate it and lookup the
					// Perforce extended charsets.
					PerforceCharsetProvider p4CharsetProvider = new PerforceCharsetProvider();
					this.charset = p4CharsetProvider.charsetForName(javaCharsetName);
		
					// Throw the unsupported charset exception that was catched.
					if (this.charset == null) {
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
			this.charset = null;
		}
		
		return (this.charset != null);
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getCharsetName()
	 */
	public String getCharsetName() {
		return this.charsetName;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#supportsUnicode()
	 */
	public boolean supportsUnicode()
				throws ConnectionException, RequestException, AccessException {
		if (this.serverInfo == null) {
			this.serverInfo = getServerInfo();
		}

		if (this.serverInfo != null) {
			return this.serverInfo.isUnicodeEnabled();
		}
		
		return false;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getKnownCharsets()
	 */
	public String[] getKnownCharsets() {
		return PerforceCharsets.getKnownCharsets();
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getProperties()
	 */
	public Properties getProperties() {
		return props;
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.server.IServerControl#init(java.lang.String, int, java.util.Properties)
	 */
	public ServerStatus init(String host, int port, Properties props)
							throws ConfigException, ConnectionException {
		return init(host, port, props, null);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.IServerControl#init(java.lang.String, int, java.util.Properties, com.perforce.p4java.option.UsageOptions)
	 */
	public ServerStatus init(String host, int port, Properties props, UsageOptions opts)
			throws ConfigException, ConnectionException {
		return init(host, port, props, opts, false);
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.server.IServerControl#init(java.lang.String, int, java.util.Properties, com.perforce.p4java.option.UsageOptions, boolean)
	 */
	public ServerStatus init(String host, int port, Properties props, UsageOptions opts, boolean secure)
			throws ConfigException, ConnectionException {

		this.serverHost = host;
		this.serverPort = port;
		this.secure = secure;
		
		// Ensure that props is never null:
		
		if (props != null) {
			this.props = props;
		} else {
			this.props = new Properties();
		}
		
		// Try to ensure that usageOptions isn't null...
		
		if (opts == null) {
			this.usageOptions = new UsageOptions(this.props);
		} else {
			this.usageOptions = opts;
		}

		// Retrieve some fairly generic properties; note the use of the short form keys for
		// program name and version (done as a favour to testers and users everywhere...).
		
		this.tmpDirName = RpcPropertyDefs.getProperty(this.props,
				PropertyDefs.P4JAVA_TMP_DIR_KEY,
						System.getProperty("java.io.tmpdir"));

		if (this.tmpDirName == null) {
			// This can really only happen if someone has nuked or played with
			// the JVM's system props before we get here... the default will
			// work for most non-Windows boxes in most cases, and may not be
			// needed in many cases anyway.
			
			this.tmpDirName = "/tmp";
			
			Log.warn("Unable to get tmp name from P4 props or System; using "
			+ this.tmpDirName + " instead");
			
		}
		
		Log.info("Using program name: '" + this.getUsageOptions().getProgramName() +
								"'; program version: '" + this.getUsageOptions().getProgramVersion() + "'");
		Log.info("Using tmp file directory: " + this.tmpDirName);
		
		setUserName(this.props.getProperty(PropertyDefs.USER_NAME_KEY_SHORTFORM,
							this.props.getProperty(PropertyDefs.USER_NAME_KEY,
									PerforceEnvironment.getP4User())));
		this.password = this.props.getProperty(PropertyDefs.PASSWORD_KEY_SHORTFORM,
									this.props.getProperty(PropertyDefs.PASSWORD_KEY, null));
		this.clientName = this.props.getProperty(PropertyDefs.CLIENT_NAME_KEY_SHORTFORM,
							this.props.getProperty(PropertyDefs.CLIENT_NAME_KEY,
									PerforceEnvironment.getP4Client()));
		this.setupOnConnect = (this.props.getProperty(PropertyDefs.AUTO_CONNECT_KEY_SHORTFORM,
							this.props.getProperty(PropertyDefs.AUTO_CONNECT_KEY, null))) == null ? false : true;
		this.loginOnConnect = (this.props.getProperty(PropertyDefs.AUTO_LOGIN_KEY_SHORTFORM,
							this.props.getProperty(PropertyDefs.AUTO_LOGIN_KEY, null))) == null ? false : true;
		this.nonCheckedSyncs = (this.props.getProperty(PropertyDefs.NON_CHECKED_SYNC_SHORT_FORM,
							this.props.getProperty(PropertyDefs.NON_CHECKED_SYNC, null))) == null ? false : true;
		this.enableTracking = (this.props.getProperty(PropertyDefs.ENABLE_TRACKING_SHORT_FORM,
							this.props.getProperty(PropertyDefs.ENABLE_TRACKING, null))) == null ? false : true;
		this.enableProgress = (this.props.getProperty(PropertyDefs.ENABLE_PROGRESS_SHORT_FORM,
				this.props.getProperty(PropertyDefs.ENABLE_PROGRESS, null))) == null ? false : true;
		this.quietMode = (this.props.getProperty(PropertyDefs.QUIET_MODE_SHORT_FORM,
				this.props.getProperty(PropertyDefs.QUIET_MODE, null))) == null ? false : true;
		this.useAuthMemoryStore = (this.props.getProperty(PropertyDefs.USE_AUTH_MEMORY_STORE_KEY_SHORT_FORM,
				this.props.getProperty(PropertyDefs.USE_AUTH_MEMORY_STORE_KEY, null))) == null ? false : true;

		// Attempt to get the P4IGNORE file name from the passed-in properties
		// or the system environment variable 'P4IGNORE'
		this.ignoreFileName = this.props.getProperty(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM,
							this.props.getProperty(PropertyDefs.IGNORE_FILE_NAME_KEY,
									System.getenv(P4IGNORE_ENV_VAR) != null ?
											System.getenv(P4IGNORE_ENV_VAR) : null));

		return this.status; // Which is UNKNOWN at this point...
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#connect()
	 */
	
	public void connect() throws ConnectionException, AccessException, RequestException, ConfigException {
		this.connected = true;
		this.status = ServerStatus.READY;
		
		Log.info("connected to Perforce server at " + this.serverHost + ":" + this.serverPort);
		
		// Try to get and then verify the server version:
		
		int serverVersion = getServerVersion();
		if (serverVersion == UNKNOWN_SERVER_VERSION) {
			throw new ConnectionException(
					"Unable to determine Perforce server version for connection; "
					+ "check network connection, connection character set setting, "
					+ "and / or server status");
		} else if (serverVersion < this.minumumSupportedServerVersion) {
			throw new ConnectionException(
					"Attempted to connect to an unsupported Perforce server version; "
					+ "target server version: " + serverVersion
					+ "; minimum supported version: "
					+ this.minumumSupportedServerVersion);
		}
		
		if (this.loginOnConnect && (this.userName != null) && (this.password != null)) {
			this.login(this.password);
		}
		
		if (this.setupOnConnect && (this.clientName != null)) {
			// Attempt to get the client set up, etc.; subclasses will 
			// probably do much more than this, or nothing at all...
			
			this.client = this.getClient(this.clientName);
		}
		
		// If the charset is not set and P4CHARSET is null/none/auto (unset),
		// automatically sets it to the Java default charset.
		if (this.serverInfo.isUnicodeEnabled() && this.charset == null) {
			if (PerforceEnvironment.getP4Charset() == null ||
					PerforceEnvironment.getP4Charset().equalsIgnoreCase("none") ||
					PerforceEnvironment.getP4Charset().equalsIgnoreCase("auto")) {
				// Get the first matching Perforce charset for the Java default charset
				String p4CharsetName = PerforceCharsets.getP4CharsetName(CharsetDefs.DEFAULT_NAME);
				if (p4CharsetName != null) {
					this.charsetName = p4CharsetName;
					this.charset = CharsetDefs.DEFAULT;
				} else { // Default to Perforce "utf8" equivalent to Java "UTF-8"
					this.charsetName = "utf8";
					this.charset = CharsetDefs.UTF8;
				}
			} else {
				setCharsetName(PerforceEnvironment.getP4Charset());
			}
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#isConnected()
	 */
	public boolean isConnected() {
		return this.connected;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#disconnect()
	 */
	public void disconnect() throws ConnectionException, AccessException {
		this.connected = false;
		this.status = ServerStatus.DISCONNECTED;
		Log.info("disconnected from Perforce server at " + this.serverHost + ":" + this.serverPort);
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#setUserName(java.lang.String)
	 */
	public void setUserName(String userName) {
		this.userName = userName;
		setAuthTicket(getAuthTicket(userName));
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getUserName()
	 */
	public String getUserName() {
		return this.userName;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#setAuthTicket(java.lang.String)
	 */
	public void setAuthTicket(String authTicket) {
		if (this.userName != null) {
			setAuthTicket(this.userName, authTicket);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getAuthTicket()
	 */
	public String getAuthTicket() {
		return getAuthTicket(this.userName);
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getServerInfo()
	 */
	@SuppressWarnings("unchecked")
	public IServerInfo getServerInfo()
				throws ConnectionException, RequestException, AccessException {
				
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.INFO, new String[0], null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorStr(map);
				if (errStr != null) {
					throw new RequestException(errStr, (String) map.get("code0"));
				}
			}
			return new ServerInfo(resultMaps.toArray(new HashMap[resultMaps.size()]));
		} else {
			return new ServerInfo();
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#login(java.lang.String)
	 */
	public void login(String password) throws ConnectionException,
							RequestException, AccessException, ConfigException {
		login(password, false);
	}
	
	/**
	 * Works by retrieving the auth ticket and storing it away for use on all future
	 * commands.
	 *
	 * @see com.perforce.p4java.server.IServer#login(java.lang.String)
	 */
	public void login(String password, boolean allHosts) throws ConnectionException,
							RequestException, AccessException, ConfigException {

		try {
			login(password, new LoginOptions().setAllHosts(allHosts));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getLoginStatus()
	 */
	public String getLoginStatus() throws P4JavaException {
		String statusStr = null;
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.LOGIN,
												new String[] {"-s"}, null);
		if ((resultMaps != null) && (resultMaps.size() > 0)) {
			statusStr = getInfoStr(resultMaps.get(0));
			if (statusStr == null) {
				// it's probably an error message:
				
				statusStr = getErrorStr(resultMaps.get(0));
			}
		}
		
		return statusStr == null ? "" : statusStr; // guaranteed non-null return
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#login(java.lang.String, com.perforce.p4java.option.server.LoginOptions)
	 */
	public void login(String password, LoginOptions opts) throws P4JavaException {
		login(password, null, opts);
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#login(java.lang.String, java.lang.StringBuffer, com.perforce.p4java.option.server.LoginOptions)
	 */
	public void login(String password, StringBuffer ticket, LoginOptions opts) throws P4JavaException {
		if (password != null) {
			password = password + "\n";
		}

		if (opts == null) {
			opts = new LoginOptions();
		}
		
		HashMap<String, Object> pwdMap = new HashMap<String, Object>();
		pwdMap.put("password", password);
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.LOGIN,
														Parameters.processParameters(opts, this), pwdMap);
		
		// We expect a success message in the first (and hopefully only) map returned;
		// all else is not (currently) processed.
		
		String retVal = null;
		//String retCode = null;
		if (resultMaps != null && resultMaps.size() > 0) {
			if (resultMaps.get(0) != null) {
				handleErrorStr(resultMaps.get(0));
				if (isInfoMessage(resultMaps.get(0))) {
					retVal = getInfoStr(resultMaps.get(0));
					//retCode = (String) resultMaps.get(0).get("code0");
				}
			}
		}
		
		// At this point, either login is successful or no login is necessary.
		// Handle login with password not set on the server (code0 = 268442937)
		// If the passed-in 'password' parameter is not null/empty and
		// the return message indicates login not required ("'login' not
		// necessary, no password set for this user."), throw access exception.
		if (password != null && password.length() > 0 && retVal != null) {
			if (isLoginNotRequired(retVal)) {
				throw new AccessException(retVal);
			}
		}
		
		// Note: if the ticket StringBuffer is non-null the auth ticket will
		// be appended to the end the of the buffer. If the buffer originally
		// has content it will remain there.
		
		if (ticket != null) {
			if (this.getAuthTicket() != null) {
				ticket.append(this.getAuthTicket());
			}
		}
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#login(com.perforce.p4java.core.IUser, java.lang.StringBuffer, com.perforce.p4java.option.server.LoginOptions)
	 */
	public void login(IUser user, StringBuffer ticket, LoginOptions opts) throws P4JavaException {
		if (user == null) {
			throw new NullPointerError("null user passed to IOptionsServer.login method");
		}
		if (user.getLoginName() == null) {
			throw new NullPointerError("null user.getLoginName() passed to IOptionsServer.login method");
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.LOGIN,
														Parameters.processParameters(opts, null, user.getLoginName(), this), null);
		
		// We expect a success message in the first (and hopefully only) map returned;
		// all else is not (currently) processed.
		
		if (resultMaps != null && resultMaps.size() > 0) {
			if (resultMaps.get(0) != null) {
				handleErrorStr(resultMaps.get(0));
			}
		}

		// Note: if the ticket StringBuffer is non-null the auth ticket will
		// be appended to the end the of the buffer. If the buffer originally
		// has content it will remain there.
		
		if (ticket != null) {
			if (this.getAuthTicket(user.getLoginName()) != null) {
				ticket.append(this.getAuthTicket(user.getLoginName()));
			}
		}
	}

	/**
	 * @see com.perforce.p4java.server.IServer#logout()
	 */
	public void logout() throws ConnectionException, RequestException, 
									AccessException, ConfigException {
		try {
			logout(new LoginOptions());
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#logout(com.perforce.p4java.option.server.LoginOptions)
	 */
	public void logout(LoginOptions opts) throws P4JavaException {
		if (getAuthTicket() == null) {
			// We're not logged in. Should probably make this an error, but never mind...
			return;
		}
		
		@SuppressWarnings("unused") // used for debugging
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.LOGOUT, new String[0], null);
		
		// We basically don't really care about the results (any errors have already been
		// thrown up the exception ladder); we just need to null out the ticket:
		
		setAuthTicket(null);
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#changePassword(java.lang.String, java.lang.String, java.lang.String)
	 */
	public String changePassword(String oldPassword, String newPassword, String userName) throws P4JavaException {

		if (oldPassword != null) {
			oldPassword += "\n";
		}

		newPassword = newPassword == null ? "\n" : newPassword + "\n";

		HashMap<String, Object> pwdMap = new HashMap<String, Object>();
		pwdMap.put(RpcFunctionMapKey.OLD_PASSWORD, oldPassword);
		pwdMap.put(RpcFunctionMapKey.NEW_PASSWORD, newPassword);
		pwdMap.put(RpcFunctionMapKey.NEW_PASSWORD2, newPassword);
		
		List<String> args = new ArrayList<String>();
		
		// Set the userName, if it is not null and not empty
		if (userName != null && userName.trim().length() > 0) {
			args.add(userName);
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.PASSWD,
													args.toArray(new String[args.size()]),
													pwdMap);
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getClients(java.lang.String, java.lang.String, int)
	 */
	public List<IClientSummary> getClients(String userName, String queryString, int maxResults)
					throws ConnectionException, RequestException, AccessException {
		if (userName != null) {
			if (this.getServerVersion() < 20062) {
				throw new RequestException(
					"user restrictions for client lists are not supported by this version of the Perforce server",
						MessageGenericCode.EV_UPGRADE,
						MessageSeverityCode.E_FAILED);
			}
		}
		if (queryString != null) {
			if (this.getServerVersion() < 20081) {
				throw new RequestException(
					"query expressions for client lists are not supported by this version of the Perforce server",
					MessageGenericCode.EV_UPGRADE,
					MessageSeverityCode.E_FAILED);
			}
		}
		
		if (maxResults > 0) {
			if (this.getServerVersion() < 20061) {
				throw new RequestException(
					"user restrictions for client lists are not supported by this version of the Perforce server",
						MessageGenericCode.EV_UPGRADE,
						MessageSeverityCode.E_FAILED);
			}
		}
		
		
		try {
			return getClients(new GetClientsOptions()
									.setMaxResults(maxResults)
									.setUserName(userName)
									.setNameFilter(queryString));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getClients(com.perforce.p4java.option.server.GetClientsOptions)
	 */
	public List<IClientSummary> getClients(GetClientsOptions opts)
									throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CLIENTS,
												Parameters.processParameters(opts, this),
												null);
		
		if (resultMaps == null) {
			throw new P4JavaError("Null resultMaps in getClientList call");
		}
		
		List<IClientSummary> specList = new ArrayList<IClientSummary>();
		
		for (Map<String, Object> map : resultMaps) {		
			String errStr = getErrorStr(map);
			
			if (errStr != null) {
				throw new RequestException(errStr, (String) map.get("code0"));
			} else {
				specList.add(new ClientSummary(map, true));
			}
		}
		
		return specList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getLabels(java.lang.String, int, java.lang.String, java.util.List)
	 */
	public List<ILabelSummary> getLabels(String user, int maxLabels, String nameFilter,
			List<IFileSpec> fileList)
			throws ConnectionException, RequestException, AccessException {
		if (user != null) {
			if (this.getServerVersion() < 20062) {
				throw new RequestException(
					"user restrictions for label lists are not supported by this version of the Perforce server",
						MessageGenericCode.EV_UPGRADE,
						MessageSeverityCode.E_FAILED);
			}
		}
		if (maxLabels > 0) {
			if (this.getServerVersion() < 20061) {
				throw new RequestException(
						"max limit for label lists are not supported by this version of the Perforce server",
						MessageGenericCode.EV_UPGRADE,
						MessageSeverityCode.E_FAILED);
			}
		}
		if (nameFilter != null) {
			if (this.getServerVersion() < 20081) {
				throw new RequestException(
					"query expressions for label lists are not supported by this version of the Perforce server",
					MessageGenericCode.EV_UPGRADE,
					MessageSeverityCode.E_FAILED);
			}
		}

		try {
			return getLabels(fileList, new GetLabelsOptions()
											.setMaxResults(maxLabels)
											.setUserName(user)
											.setNameFilter(nameFilter));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getLabels(com.perforce.p4java.option.server.GetLabelsOptions)
	 */
	public List<ILabelSummary> getLabels(List<IFileSpec> fileList, GetLabelsOptions opts)
									throws P4JavaException {
		List<ILabelSummary> labelList = new ArrayList<ILabelSummary>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.LABELS,
												Parameters.processParameters(opts, fileList, this),
												null);								
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					if (!handleErrorStr(map)) {
						labelList.add(new LabelSummary(map));
					}
				}
			}
		}
		return labelList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getLabel(java.lang.String)
	 */
	public ILabel getLabel(String labelName)
					throws ConnectionException, RequestException, AccessException {
		if (labelName == null) {
			throw new NullPointerError("Null label name passed to ServerImpl.labelName");
		}
		
		final String OFLAG = "-o";
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.LABEL,
													new String[] { OFLAG, labelName},
													null);
		ILabel label = null;
		
		if (resultMaps == null) {
			Log.warn("Unexpected null map array returned to ServerImpl.getLabel()");
		} else {
			// Note that the only way
			// to tell whether the requested label existed or not is to look for the
			// returned Access and Update fields -- if they're both missing, it's probably
			// not a real label, just the default new label template coming back from
			// the server.
			
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					handleErrorStr(map);
					if (!isInfoMessage(map)
							&& (map.containsKey(MapKeys.UPDATE_KEY) || map.containsKey(MapKeys.ACCESS_KEY))) {
						label = new Label(map, this);
					}
				}
			}
		}
		
		return label;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#createLabel(com.perforce.p4java.core.ILabel)
	 */
	public String createLabel(ILabel label)
				throws ConnectionException, RequestException, AccessException {
		if (label == null) {
			throw new NullPointerError("null label passed to ServerImpl.newLabel()");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.LABEL, new String[] {"-i"},
				InputMapper.map(label));

		String retStr = null;
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retStr == null) {
						retStr = getInfoStr(map);
					} else {
						retStr += "\n" + getInfoStr(map);
					}
				}
			}
		} else {
			Log.warn("null return map array in ServerImpl.newLabel");
		}

		return retStr;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#deleteLabel(java.lang.String, boolean)
	 */
	public String deleteLabel(String labelName, boolean force)
					throws ConnectionException, RequestException, AccessException {
		try {
			return deleteLabel(labelName, new DeleteLabelOptions().setForce(force));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deleteLabel(java.lang.String, com.perforce.p4java.option.server.DeleteLabelOptions)
	 */
	public String deleteLabel(String labelName, DeleteLabelOptions opts)
								throws P4JavaException {
		if (labelName == null) {
			throw new NullPointerError("null label name passed to Server.deleteLabel()");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.LABEL,
												Parameters.processParameters(
														opts, null, new String[] {"-d", labelName}, this),
												null);
		String retStr = null;
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retStr == null) {
						retStr = getInfoStr(map);
					} else {
						retStr += "\n" + getInfoStr(map);
					}
				}
			}
		} else {
			Log.warn("null return map array in Server.deleteLabel");
		}

		return retStr;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#updateLabel(com.perforce.p4java.core.ILabel)
	 */
	public String updateLabel(ILabel label)
					throws ConnectionException, RequestException, AccessException {
		if (label == null) {
			throw new NullPointerError("null label passed to ServerImpl.updateLabel()");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.LABEL, new String[] {"-i"},
											InputMapper.map(label));
		String retVal = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in ServerImpl.updateLabel");
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getDepotFiles(java.util.List, boolean)
	 */
	public List<IFileSpec> getDepotFiles(List<IFileSpec> fileSpecs, boolean allRevs)
									throws ConnectionException, AccessException {
		try {
			return getDepotFiles(fileSpecs, new GetDepotFilesOptions().setAllRevs(allRevs));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IServer.getDepotFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getDepotFiles(java.util.List, com.perforce.p4java.option.server.GetDepotFilesOptions)
	 */
	public List<IFileSpec> getDepotFiles(List<IFileSpec> fileSpecs, GetDepotFilesOptions opts)
								throws P4JavaException {
		if (fileSpecs == null) {
			throw new NullPointerError("Null file specification list passed to getDepotFiles");
		}
		
		List<IFileSpec> fileList = new ArrayList<IFileSpec>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
								CmdSpec.FILES, Parameters.processParameters(
													opts, fileSpecs, this), null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				fileList.add(handleFileReturn(map));
			}
		}
		return fileList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getFileAnnotations(java.util.List, com.perforce.p4java.core.file.DiffType, boolean, boolean, boolean)
	 */
	public List<IFileAnnotation> getFileAnnotations(List<IFileSpec> fileSpecs, DiffType wsOpts,
									boolean allResults, boolean useChangeNumbers, boolean followBranches)
						throws ConnectionException, RequestException, AccessException {
		
		if ((wsOpts != null) && (!wsOpts.isWsOption())) {
			throw new RequestException("Bad whitespace option in getFileAnnotations");
		}
		
		try {
			return getFileAnnotations(fileSpecs, new GetFileAnnotationsOptions()
														.setAllResults(allResults)
														.setUseChangeNumbers(useChangeNumbers)
														.setFollowBranches(followBranches)
														.setWsOpts(wsOpts));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getFileAnnotations(java.util.List, com.perforce.p4java.option.server.GetFileAnnotationsOptions)
	 */
	public List<IFileAnnotation> getFileAnnotations(List<IFileSpec> fileSpecs, GetFileAnnotationsOptions opts)
															throws P4JavaException {
		List<IFileAnnotation> returnList = new ArrayList<IFileAnnotation>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.ANNOTATE,
											Parameters.processParameters(opts, fileSpecs, this),
											null);
		if (resultMaps != null) {
			String depotFile = null;
			for (Map<String, Object> map : resultMaps) {
				FileAnnotation dataAnnotation = null;
				if (map != null) {
					// RPC version returns info, cmd version returns error... we throw
					// an exception in either case.
					
					String errStr = getErrorStr(map);
					if (errStr != null) {
						throw new RequestException(errStr, (String) map.get("code0"));
					} else {
						// Note that this processing depends a bit on the current
						// ordering of tagged results back from the server; if this
						// changes, we may need to change things here as well...
						
						if (map.containsKey("depotFile")) {
							depotFile = (String) map.get("depotFile");	// marks the start of annotations for
																		// a new depot file.
						} else {
							// Pick up the "data" annotation:
							dataAnnotation = new FileAnnotation(
									map,
									depotFile,
									this.client == null? null : this.client.getLineEnd());
							returnList.add(dataAnnotation);
						
							// Look for any associated contributing integrations:
							
							for (int order = 0; map.containsKey("depotFile" + order); order++) {
								try {
									dataAnnotation.addIntegrationAnnotation(
														new FileAnnotation(
																order,
																(String) map.get("depotFile" + order),
																new Integer((String) map.get("upper" + order)),
																new Integer((String) map.get("lower" + order))
															));
								} catch (Throwable thr) {
									Log.error("bad conversion in getFileAnnotations");
									Log.exception(thr);
								}
							}
						}
					}
				}
			}
		}
		
		return returnList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#tagFiles(java.util.List, java.lang.String, boolean, boolean)
	 */
	public List<IFileSpec> tagFiles(List<IFileSpec> fileSpecs, String labelName,
			boolean listOnly, boolean delete)
				throws ConnectionException, RequestException, AccessException {		
		try {
			return tagFiles(fileSpecs, labelName, new TagFilesOptions().setDelete(delete).setListOnly(listOnly));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IServer.getDepotFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#tagFiles(java.util.List, java.lang.String, com.perforce.p4java.option.server.TagFilesOptions)
	 */
	public List<IFileSpec> tagFiles(List<IFileSpec> fileSpecs, String labelName,
								TagFilesOptions opts) throws P4JavaException {
		String labelOpt = (labelName == null ? null : ("-l" + labelName));
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.TAG,
											Parameters.processParameters(opts, fileSpecs, labelOpt, this),
											null);
		List<IFileSpec> fileList = new ArrayList<IFileSpec>();

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
			fileList.add(handleFileReturn(map));
			}
		} else {
			Log.warn("null return map array in ServerImpl.tagFiles");
		}
		
		return fileList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getReviews(int, java.util.List)
	 */
	public List<IUserSummary> getReviews(int changelistId, List<IFileSpec> fileSpecs)
					throws ConnectionException, RequestException, AccessException {
		try {
			return getReviews(fileSpecs, new GetReviewsOptions(changelistId));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getReviews(java.util.List, com.perforce.p4java.option.server.GetReviewsOptions)
	 */
	public List<IUserSummary> getReviews(List<IFileSpec> fileSpecs, GetReviewsOptions opts)
						throws P4JavaException {
		List<IUserSummary> userList = new ArrayList<IUserSummary>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.REVIEWS,
								Parameters.processParameters(opts, fileSpecs, this),
								null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				try {
					userList.add(new UserSummary(
										(String) map.get("user"),
										(String) map.get("email"),
										(String) map.get("name"),
										null,	// access
										null	// update
						));
				} catch (Throwable thr) {
					Log.error("Unexpected exception in getReviews: "
							+ thr.getLocalizedMessage());
					Log.exception(thr);
				}
			}
		}
		return userList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getReviewChangelists(com.perforce.p4java.option.server.GetReviewChangelistsOptions)
	 */
	public List<IReviewChangelist> getReviewChangelists(GetReviewChangelistsOptions opts)
						throws P4JavaException {
		List<IReviewChangelist> reviewList = new ArrayList<IReviewChangelist>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.REVIEW,
								Parameters.processParameters(opts, this),
								null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				try {
					reviewList.add(new ReviewChangelist(
										new Integer((String) map.get("change")),
										(String) map.get("user"),
										(String) map.get("email"),
										(String) map.get("name")
						));
				} catch (Throwable thr) {
					Log.error("Unexpected exception in getReviews: "
							+ thr.getLocalizedMessage());
					Log.exception(thr);
				}
			}
		}
		return reviewList;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#moveFile(int, boolean, boolean, java.lang.String, com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec)
	 */
	public List<IFileSpec> moveFile(int changeListId, boolean listOnly, boolean noClientMove,
							String fileType, IFileSpec fromFile, IFileSpec toFile)
			throws ConnectionException, RequestException, AccessException {
		
		/*
		 * Minimum level of server that supports this command.
		 */
		final int MIN_SUPPORTED_SERVER = 20091;
		final int MIN_SUPPORTED_SERVER_OPTION_K = 20092;
		
		if (this.serverVersion < MIN_SUPPORTED_SERVER) {
			throw new RequestException(
					"command requires a Perforce server version 2009.1 or later"
					);
		}
		
		if ((this.serverVersion < MIN_SUPPORTED_SERVER_OPTION_K) && noClientMove) {
			throw new RequestException(
					"command option noClientMove requires a Perforce server version 2009.2 or later");
		}
		
		try {
			return moveFile(fromFile, toFile, new MoveFileOptions()
													.setChangelistId(changeListId)
													.setFileType(fileType)
													.setForce(false)
													.setListOnly(listOnly)
													.setNoClientMove(noClientMove));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IServer.moveFile: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#moveFile(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.option.server.MoveFileOptions)
	 */
	public List<IFileSpec> moveFile(IFileSpec fromFile, IFileSpec toFile, MoveFileOptions opts)
										throws P4JavaException {
		if ((fromFile == null || fromFile.getPreferredPath() == null) ||
				(toFile == null || toFile.getPreferredPath() == null)) {
			throw new RequestException(
					"command requires both to and from files to be specified");
		}
		
		List<IFileSpec> fileList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.MOVE,
								Parameters.processParameters(opts, null,
										new String[] {
											fromFile.getPreferredPath().toString(),
											toFile.getPreferredPath().toString()
										},
										this),
								null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				fileList.add(handleFileReturn(map));
			}
		}
		
		return fileList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getUser(java.lang.String)
	 */
	public IUser getUser(String userName)
					throws ConnectionException, RequestException, AccessException {
		String[] args = null;
		if (userName == null) {
			args = new String[] { "-o" };
		} else {
			args = new String[] { "-o", userName };
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.USER, args, null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (!isInfoMessage(map)
						&& (map.containsKey(MapKeys.UPDATE_KEY) || map.containsKey(MapKeys.ACCESS_KEY))) {
					return new User(map, this);
				}
			}
		}
		
		return null;
	}
	
	
	/**
	 * @see com.perforce.p4java.server.IServer#createUser(com.perforce.p4java.core.IUser, boolean)
	 */
	public String createUser(IUser user, boolean force)
					throws ConnectionException, RequestException, AccessException {
		if (user == null) {
			throw new NullPointerError("Null user passed to IServer.createUser");
		}
		
		return updateUser(user, force);
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#createUser(com.perforce.p4java.core.IUser, com.perforce.p4java.option.server.UpdateUserOptions)
	 */
	public String createUser(IUser user, UpdateUserOptions opts) throws P4JavaException {
		return updateUser(user, opts);
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#updateUser(com.perforce.p4java.core.IUser, boolean)
	 */
	public String updateUser(IUser user, boolean force)
						throws ConnectionException, RequestException, AccessException {
		try {
			return updateUser(user, new UpdateUserOptions(force));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#updateUser(com.perforce.p4java.core.IUser, com.perforce.p4java.option.server.UpdateUserOptions)
	 */
	public String updateUser(IUser user, UpdateUserOptions opts) throws P4JavaException {
		if (user == null) {
			throw new NullPointerError("Null user passed to IServer.updateUser");
		}
		if (user.getLoginName() == null) {
			throw new NullPointerError("Null user name in user passed to IServer.updateUser");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.USER,
												Parameters.processParameters(opts, null, "-i", this),
												InputMapper.map(user));
		String retVal = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in Server.updateUser");
		}
	
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#deleteUser(java.lang.String, boolean)
	 */
	public String deleteUser(String userName, boolean force)
						throws ConnectionException, RequestException, AccessException {
		try {
			return deleteUser(userName, new UpdateUserOptions(force));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deleteUser(java.lang.String, com.perforce.p4java.option.server.UpdateUserOptions)
	 */
	public String deleteUser(String userName, UpdateUserOptions opts) throws P4JavaException {
		if (userName == null) {
			throw new NullPointerError("Null user name passed to IServer.deleteUser");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.USER,
											Parameters.processParameters(opts, null,
													new String[] {"-d", userName}, this),
											null);
		String retStr = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retStr == null) {
						retStr = getInfoStr(map);
					} else {
						retStr += "\n" + getInfoStr(map);
					}
				}
			}
		} else {
			Log.warn("null return map array in Server.deleteUser");
		}
		
		return retStr;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#renameUser(java.lang.String, java.lang.String)
	 */
	public String renameUser(String oldUserName, String newUserName) throws P4JavaException {
		if (oldUserName == null) {
			throw new NullPointerError("Null old user name passed to IOptionsServer.renameUser");
		}
		if (newUserName == null) {
			throw new NullPointerError("Null new user name passed to IOptionsServer.renameUser");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.RENAMEUSER,
											new String[] {"--from=" + oldUserName, "--to=" + newUserName},
											null);
		
		String retStr = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retStr == null) {
						retStr = getInfoStr(map);
					} else {
						retStr += "\n" + getInfoStr(map);
					}
				}
			}
		} else {
			Log.warn("null return map array in Server.renameUser");
		}
		
		return retStr;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getUsers(java.util.List, int)
	 */
	public List<IUserSummary> getUsers(List<String> userList, int maxUsers)
						throws ConnectionException, RequestException, AccessException {
		try {
			return getUsers(userList, new GetUsersOptions().setMaxUsers(maxUsers));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getUsers(java.util.List, com.perforce.p4java.option.server.GetUsersOptions)
	 */
	public List<IUserSummary> getUsers(List<String> userList, GetUsersOptions opts)
								throws P4JavaException {
		List<IUserSummary> resultsList = new ArrayList<IUserSummary>();
		
		String[] users = null;
		
		if (userList != null) {
			users = userList.toArray(new String[userList.size()]);
		}
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.USERS,
												Parameters.processParameters(opts, null, users, this),
												null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				resultsList.add(new UserSummary(map, true));
			}
		}
		
		return resultsList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getUserGroups(java.lang.String, boolean, boolean, int)
	 */
	public List<IUserGroup> getUserGroups(String userOrGroupName, boolean indirect,
												boolean displayValues, int maxGroups)
								throws ConnectionException, RequestException, AccessException {
		try {
			return getUserGroups(userOrGroupName, new GetUserGroupsOptions()
														.setIndirect(indirect)
														.setDisplayValues(displayValues)
														.setMaxGroups(maxGroups));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getUserGroups(java.lang.String, com.perforce.p4java.option.server.GetUserGroupsOptions)
	 */
	public List<IUserGroup> getUserGroups(String userOrGroupName, GetUserGroupsOptions opts)
							throws P4JavaException {
		List<IUserGroup> resultsList = new ArrayList<IUserGroup>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.GROUPS,
												Parameters.processParameters(
														opts, null, userOrGroupName, this),
												null);
		
		if (resultMaps != null) {
			UserGroup ugImpl = null;
			List<String> userList = null;
			List<String> subgroupList = null;
			for (Map<String, Object> map : resultMaps) {
				
				handleErrorStr(map);
				
				// The server returns the results not as a series of rows, each row
				// representing a single group, but as a series of rows, each row representing
				// a single *user*, meaning we need to do reverse correlation. At least they come
				// back sorted by group name from the server, but even still, this makes for
				// less than optimal decoding...

				String groupName = (String) map.get(MapKeys.GROUP_LC_KEY);
				
				if (groupName != null) {
					if (ugImpl == null) {
						ugImpl = new UserGroup();
						userList = new ArrayList<String>();
						ugImpl.setUsers(userList);
						subgroupList = new ArrayList<String>();
						ugImpl.setSubgroups(subgroupList);
						ugImpl.setName(groupName);
					} else if (!ugImpl.getName().equals(groupName)) {
						resultsList.add(ugImpl);
						ugImpl = new UserGroup();
						ugImpl.setName(groupName);
						userList = new ArrayList<String>();
						ugImpl.setUsers(userList);
						ugImpl.setSubgroups(subgroupList);
					}
					
					try {
						String userName = (String) map.get(MapKeys.USER_LC_KEY);
						String maxScanRows = (String) map.get(MapKeys.MAXSCANROWS_LC_KEY);
						String maxLockTime = (String) map.get(MapKeys.MAXLOCKTIME_LC_KEY);
						String timeout = (String) map.get(MapKeys.TIMEOUT_LC_KEY);
						String passwordTimeout = (String) map.get(MapKeys.PASSWORD_TIMEOUT_LC_KEY);
						String maxResults = (String) map.get(MapKeys.MAXRESULTS_LC_KEY);
						String isOwner = (String) map.get(MapKeys.ISOWNER_LC_KEY);
						if ((isOwner != null) && (isOwner.equalsIgnoreCase("1"))) {
							if (ugImpl.getOwners() == null) {
								ugImpl.setOwners(new ArrayList<String>());
								ugImpl.getOwners().add(userName);
							}
						}
						String isSubGroup = (String) map.get(MapKeys.ISSUBGROUP_LC_KEY);
						if ((isSubGroup != null) && isSubGroup.equals("1")) {
							subgroupList.add(userName);
						}
						else {
							userList.add(userName);
						}
						if (maxScanRows != null) {
							ugImpl.setMaxScanRows(new Integer(maxScanRows));
						}
						if (maxLockTime != null) {
							ugImpl.setMaxLockTime(new Integer(maxLockTime));
						}
						if (timeout != null) {
							ugImpl.setTimeout(new Integer(timeout));
						}
						if (maxResults != null) {
							ugImpl.setMaxResults(new Integer(maxResults));
						}
						if (passwordTimeout != null) {
							ugImpl.setPasswordTimeout(new Integer(passwordTimeout));
						}
					} catch (Throwable thr) {
						Log.warn("Unexpected exception in ServerImpl.getUserGroups: "
									+ thr.getMessage());
						Log.exception(thr);
					}
				}
			}
			
			if (ugImpl != null) {
				resultsList.add(ugImpl);
			}
		}
		
		return resultsList;
	}
	/**
	 * @see com.perforce.p4java.server.IServer#getUserGroup(java.lang.String)
	 */
	public IUserGroup getUserGroup(String name) 
							throws ConnectionException, RequestException, AccessException {
		if (name == null) {
			throw new NullPointerError("null group name passed to Server.getUserGroup");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.GROUP, new String[] {"-o", name}, null);
		
		UserGroup ugImpl = null;
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);

				ugImpl = new UserGroup(map);
				break;
			}
		}
		
		return ugImpl;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#createUserGroup(com.perforce.p4java.core.IUserGroup)
	 */
	public String createUserGroup(IUserGroup group)
							throws ConnectionException, RequestException, AccessException {
		if (group == null) {
			throw new NullPointerError("Null group passed to IServer.createUserGroup method");
		}
		
		return updateUserGroup(group, false);
	}

	/**
	 * @see com.perforce.p4java.server.IServer#updateUserGroup(com.perforce.p4java.core.IUserGroup, boolean)
	 */
	public String updateUserGroup(IUserGroup group, boolean updateIfOwner)
							throws ConnectionException, RequestException, AccessException {
		try {
			return updateUserGroup(group,
					new UpdateUserGroupOptions().setUpdateIfOwner(updateIfOwner));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.server.IServer#deleteUserGroup(com.perforce.p4java.core.IUserGroup)
	 */
	public String deleteUserGroup(IUserGroup group)
							throws ConnectionException, RequestException, AccessException {
		try {
			return deleteUserGroup(group, new UpdateUserGroupOptions());
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#createUserGroup(com.perforce.p4java.core.IUserGroup, com.perforce.p4java.option.server.UpdateUserGroupOptions)
	 */
	public String createUserGroup(IUserGroup group, UpdateUserGroupOptions opts)
								throws P4JavaException {
		return updateUserGroup(group, opts);
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#updateUserGroup(com.perforce.p4java.core.IUserGroup, com.perforce.p4java.option.server.UpdateUserGroupOptions)
	 */
	public String updateUserGroup(IUserGroup group, UpdateUserGroupOptions opts)
								throws P4JavaException {
		if (group == null) {
			throw new NullPointerError("Null group passed to IServer.updateUserGroup method");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
									CmdSpec.GROUP,
									Parameters.processParameters(opts, null, "-i", this),
									InputMapper.map(group));
		String retVal = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in Server.updateUserGroup");
		}
	
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deleteUserGroup(com.perforce.p4java.core.IUserGroup, com.perforce.p4java.option.server.UpdateUserGroupOptions)
	 */
	public String deleteUserGroup(IUserGroup group, UpdateUserGroupOptions opts)
								throws P4JavaException {
		if (group == null) {
			throw new NullPointerError("Null group passed to IServer.deleteUserGroup method");
		}
		if (group.getName() == null) {
			throw new NullPointerError(
					"Null group name in user group passed to IServer.deleteUserGroup method");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
										CmdSpec.GROUP,
										Parameters.processParameters(
												opts, null, new String[] {"-d", group.getName()}, this),
										null);
		String retVal = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in Server.deleteUserGroup");
		}
	
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getProtectionsTable()
	 */
	public InputStream getProtectionsTable() throws P4JavaException {

		return this.execStreamCmd(CmdSpec.PROTECT, new String[] {"-o"});
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getProtectionEntries(boolean, java.lang.String, java.lang.String, java.lang.String, java.util.List)
	 */
	public List<IProtectionEntry> getProtectionEntries(boolean allUsers, String hostName,
									String userName, String groupName,
									List<IFileSpec> fileList)
						throws ConnectionException, RequestException, AccessException {
		try {
			return getProtectionEntries(fileList,
						new GetProtectionEntriesOptions()
								.setAllUsers(allUsers)
								.setHostName(hostName)
								.setUserName(userName)
								.setGroupName(groupName));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getProtectionEntries(java.util.List, com.perforce.p4java.option.server.GetProtectionEntriesOptions)
	 */
	public List<IProtectionEntry> getProtectionEntries(List<IFileSpec> fileList,
							GetProtectionEntriesOptions opts) throws P4JavaException {
		List<IProtectionEntry> protectsList = new ArrayList<IProtectionEntry>();
		
		// Get preferred path array without annotations. The reason is the
		// Perforce server 'protects' command requires a file list devoid of
		// annotated revision specificity.
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.PROTECTS,
										Parameters.processParameters(opts, fileList, null, false, this),
										null);
		
		if (resultMaps != null) {
			int order = 0;
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					handleErrorStr(map);
					protectsList.add(new ProtectionEntry(map, order++));
				}
			}
		}
		
		return protectsList;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#createProtectionEntries(java.util.List)
	 */
	public String createProtectionEntries(List<IProtectionEntry> entryList) throws P4JavaException {
		if (entryList == null) {
			throw new NullPointerError("Null new protection entry list in createProtectionEntries method");
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.PROTECT, new String[] {"-i"},
														InputMapper.map(new ProtectionsTable(entryList)));
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#updateProtectionEntries(java.util.List)
	 */
	public String updateProtectionEntries(List<IProtectionEntry> entryList)
										throws P4JavaException {
		if (entryList == null) {
			throw new NullPointerError("Null new protection entry list in updateProtectionEntries method");
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.PROTECT, new String[] {"-i"},
														InputMapper.map(new ProtectionsTable(entryList)));
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getBranchSpecs(java.lang.String, java.lang.String, int)
	 */
	public List<IBranchSpecSummary> getBranchSpecs(String userName, String nameFilter, int maxReturns)
						throws ConnectionException, RequestException, AccessException {

		if (userName != null) {
			if (this.getServerVersion() < 20062) {
				throw new RequestException(
					"user restrictions for branch lists are not supported by this version of the Perforce server",
						MessageGenericCode.EV_UPGRADE,
						MessageSeverityCode.E_FAILED);
			}
		}
		if (nameFilter != null) {
			if (this.getServerVersion() < 20081) {
				throw new RequestException(
					"query expressions for branch lists are not supported by this version of the Perforce server",
					MessageGenericCode.EV_UPGRADE,
					MessageSeverityCode.E_FAILED);
			}
		}
		if (maxReturns > 0) {
			if (this.getServerVersion() < 20061) {
				throw new RequestException(
						"max limit for branch lists are not supported by this version of the Perforce server",
						MessageGenericCode.EV_UPGRADE,
						MessageSeverityCode.E_FAILED);
			}
		}

		try {
			return getBranchSpecs(new GetBranchSpecsOptions()
											.setMaxResults(maxReturns)
											.setNameFilter(nameFilter)
											.setUserName(userName));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getBranchSpecs(com.perforce.p4java.option.server.GetBranchSpecsOptions)
	 */
	public List<IBranchSpecSummary> getBranchSpecs(GetBranchSpecsOptions opts)
										throws P4JavaException {
		List<IBranchSpecSummary> branchList = new ArrayList<IBranchSpecSummary>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.BRANCHES,
														Parameters.processParameters(opts, this),
														null);
		if (resultMaps != null) {
			for (Map<String, Object> branchMap : resultMaps) {
				handleErrorStr(branchMap);
				branchList.add(new BranchSpecSummary(branchMap, true));
			}
		}
		
		return branchList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getBranchSpec(java.lang.String)
	 */
	public IBranchSpec getBranchSpec(String name) 
							throws ConnectionException, RequestException, AccessException {
		try {
			return getBranchSpec(name, new GetBranchSpecOptions());
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getBranchSpec(java.lang.String, com.perforce.p4java.option.server.GetBranchSpecOptions)
	 */
	public IBranchSpec getBranchSpec(String name, GetBranchSpecOptions opts)
								throws P4JavaException {
		if (name == null) {
			throw new NullPointerError("Null branch spec name passed to getBranchSpec");
		}
		IBranchSpec branchSpec = null;

		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.BRANCH,
											Parameters.processParameters(
													opts,
													null,
													new String[] {"-o", name},
													this),
											null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					handleErrorStr(map);
					if (!isInfoMessage(map)) {
						branchSpec = new BranchSpec(map, this);
					}
				}
			}
		}
		
		return branchSpec;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#createBranchSpec(com.perforce.p4java.core.IBranchSpec)
	 */
	public String createBranchSpec(IBranchSpec branchSpec)
						throws ConnectionException, RequestException, AccessException {
		if (branchSpec == null) {
			throw new NullPointerError(
					"null branch spec passed to ServerImpl.newBranchSpec()");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.BRANCH, new String[] {"-i"},
				InputMapper.map(branchSpec));

		String retStr = null;
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retStr == null) {
						retStr = getInfoStr(map);
					} else {
						retStr += "\n" + getInfoStr(map);
					}
				}
			}
		} else {
			Log.warn("null return map array in ServerImpl.newBranchSpec");
		}

		return retStr;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#updateBranchSpec(com.perforce.p4java.core.IBranchSpec)
	 */
	public String updateBranchSpec(IBranchSpec branchSpec)
						throws ConnectionException, RequestException, AccessException {
		if (branchSpec == null) {
			throw new NullPointerError("null label passed to ServerImpl.updateBranchSpec()");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.BRANCH, new String[] {"-i"},
											InputMapper.map(branchSpec));
		String retVal = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in ServerImpl.updateBranchSpec");
		}
		
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#deleteBranchSpec(java.lang.String, boolean)
	 */
	public String deleteBranchSpec(String branchSpecName, boolean force)
						throws ConnectionException, RequestException, AccessException {
		try {
			return deleteBranchSpec(branchSpecName, new DeleteBranchSpecOptions(force));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deleteBranchSpec(java.lang.String, com.perforce.p4java.option.server.DeleteBranchSpecOptions)
	 */
	public String deleteBranchSpec(String branchSpecName, DeleteBranchSpecOptions opts)
								throws  P4JavaException {
		if (branchSpecName == null) {
			throw new NullPointerError(
					"Null branch spec name passed to IServer.deleteBranchSpec method");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.BRANCH,
												Parameters.processParameters(
													opts, null, new String[] {"-d", branchSpecName}, this),
												null);
		
		String retVal = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in ServerImpl.deleteBranchSpec");
		}
		
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getCurrentClient()
	 */
	public IClient getCurrentClient() {
		return this.client;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#setCurrentClient(com.perforce.p4java.client.IClient)
	 */
	public void setCurrentClient(IClient client) {
		this.client = client;
		
		if ((this.client != null) && (client != null)) {
			this.clientName = this.client.getName();
		} else {
			this.clientName = null;
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getClient(java.lang.String)
	 */
	public IClient getClient(String clientName)
				throws ConnectionException, RequestException, AccessException {
		
		if (clientName == null) {
			throw new NullPointerError("Null client name passed to IServer.getClient()");
		}
		
		String[] args = {"-o", clientName};
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CLIENT, args, null);

		IClient client = null;
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				
				if (map != null) {
					handleErrorStr(map);
					
					if (!isInfoMessage(map)) {						
						// Unfortunately, the p4 command version returns a valid map
						// for non-existent clients; the only way we can detect that
						// the client doesn't exist is to see if the Update or Access
						// map entries exist -- if they do, the client is (most likely)
						// a valid client on this server. This seems less than optimal to
						// me... -- HR.
						
						if (map.containsKey("Update") || map.containsKey("Access")) {	
							client = new Client(this, map);
						}
					}
				}
			}
		}

		return client;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getClient(com.perforce.p4java.client.IClientSummary)
	 */
	public IClient getClient(IClientSummary clientSummary)
						throws ConnectionException, RequestException, AccessException {
		if (clientSummary == null) {
			throw new NullPointerError("Null client summary passed to IServer.getClient()");
		}
		return getClient(clientSummary.getName());
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getClientTemplate(java.lang.String, boolean)
	 */
	public IClient getClientTemplate(String clientName, boolean allowExistent)
			throws ConnectionException, RequestException,
			AccessException {
		try {
			return getClientTemplate(clientName, new GetClientTemplateOptions(allowExistent));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getClientTemplate(java.lang.String, com.perforce.p4java.option.server.GetClientTemplateOptions)
	 */
	public IClient getClientTemplate(String clientName, GetClientTemplateOptions opts)
								throws P4JavaException {
		if (clientName == null) {
			throw new NullPointerError(
					"Null client name passed to IServer.getClientTemplate()");
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.CLIENT,
											Parameters.processParameters(
													opts,
													null,
													new String[] {"-o", clientName},
													this),
											null);
		
		IClient client = null;

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {

				if (map != null) {
					handleErrorStr(map);
					if (!isInfoMessage(map)) {
	
						// Unfortunately, the p4 command version returns a valid map
						// for non-existent clients; the only way we can detect that
						// the client doesn't exist is to see if the Update or
						// Access
						// map entries exist -- if they do, the client is (most
						// likely)
						// a valid client on this server. This seems less than
						// optimal to
						// me... -- HR.
	
						boolean nonExistent = !map.containsKey("Update")
								&& !map.containsKey("Access");
						if (nonExistent || ((opts != null) && opts.isAllowExistent())) {
							client = new Client(this, map);
						}
					}
				}
			}
		}

		return client;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getClientTemplate(java.lang.String)
	 */
	public IClient getClientTemplate(String clientName)
			throws ConnectionException, RequestException,
			AccessException {
		return getClientTemplate(clientName, false);
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#createClient(com.perforce.p4java.client.IClient)
	 */
	public String createClient(IClient newClient)
				throws ConnectionException, RequestException, AccessException {
		if (newClient == null) {
			throw new NullPointerError("Null new client spec in newClient method");
		}

		// Check if client name has whitespace
		// Replace any whitespace with "_"
		// See job073878
		if (newClient.getName() != null) {
			if (newClient.getName().contains(" ") || newClient.getName().contains("\t")) {
				String newClientName = newClient.getName().replaceAll("\\s", "_");
				newClient.setName(newClientName);
			}
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CLIENT, new String[] {"-i"},
														InputMapper.map(newClient));
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#updateClient(com.perforce.p4java.client.IClient)
	 */
	public String updateClient(IClient client)
					throws ConnectionException, RequestException, AccessException {
		if (client == null) {
			throw new NullPointerError("Null client in IServer.updateClient method");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CLIENT, new String[] {"-i"},
				InputMapper.map(client));
		
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#updateClient(com.perforce.p4java.client.IClient, boolean)
	 */
	public String updateClient(IClient client, boolean force)
						throws ConnectionException, RequestException, AccessException {
		try {
			return updateClient(client, new UpdateClientOptions(force));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#updateClient(com.perforce.p4java.client.IClient, com.perforce.p4java.option.server.UpdateClientOptions)
	 */
	public String updateClient(IClient client, UpdateClientOptions opts) throws P4JavaException {
		if (client == null) {
			throw new NullPointerError("Null client passed to IOptionsServer.updateClient");
		}
		if (client.getName() == null) {
			throw new NullPointerError("Null client name in client passed to IOptionsServer.updateClient");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.CLIENT,
												Parameters.processParameters(opts, null, "-i", this),
												InputMapper.map(client));
		String retVal = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in Server.updateClient");
		}
	
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#deleteClient(java.lang.String, boolean)
	 */
	public String deleteClient(String clientName, boolean force) 
							throws ConnectionException, RequestException, AccessException {	
		try {
			return deleteClient(clientName, new DeleteClientOptions(force));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deleteClient(java.lang.String, com.perforce.p4java.option.server.DeleteClientOptions)
	 */
	public String deleteClient(String clientName, DeleteClientOptions opts)
													throws P4JavaException {
		if (clientName == null) {
			throw new NullPointerError("Null client name passed to ServerImpl.deleteClient");
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CLIENT,
												Parameters.processParameters(
														opts, null, new String[] {"-d", clientName}, this),
												null);
		
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#switchClientView(java.lang.String, java.lang.String, com.perforce.p4java.option.server.SwitchClientViewOptions)
	 */
	public String switchClientView(String templateClientName, String targetClientName, SwitchClientViewOptions opts) 
														throws P4JavaException {	
		if (templateClientName == null) {
			throw new NullPointerError("Null template client name passed to ServerImpl.switchClientView");
		}

		List<String> args = new ArrayList<String>();
		args.add("-s");
		args.add("-t");
		args.add(templateClientName);
		if (targetClientName != null) {
			args.add(targetClientName);
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CLIENT,
												Parameters.processParameters(
														opts, null, args.toArray(new String[args.size()]), this),
												null);
		
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#switchStreamView(java.lang.String, java.lang.String, com.perforce.p4java.option.server.SwitchClientViewOptions)
	 */
	public String switchStreamView(String streamPath, String targetClientName, SwitchClientViewOptions opts) 
														throws P4JavaException {	
		if (streamPath == null) {
			throw new NullPointerError("Null stream path passed to ServerImpl.switchStreamView");
		}

		List<String> args = new ArrayList<String>();
		args.add("-s");
		args.add("-S");
		args.add(streamPath);
		if (targetClientName != null) {
			args.add(targetClientName);
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CLIENT,
												Parameters.processParameters(
														opts, null, args.toArray(new String[args.size()]), this),
												null);
		
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getDepots()
	 */
	public List<IDepot> getDepots()
				throws ConnectionException, RequestException, AccessException {
		ArrayList<IDepot> metadataArray = new ArrayList<IDepot>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.DEPOTS, new String[0], null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (!handleErrorStr(map)) {
					try {
						metadataArray.add(new Depot(map));
					} catch (Exception exc) {
						// May happen if the server changes a type format, etc., in which case
						// we basically panic.
						
						Log.exception(exc);
						throw new P4JavaError("Unexpected conversion error in getDepotList: "
								+ exc.getLocalizedMessage(), exc);
					}
				}
			}
		}
		
		return metadataArray;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getDepot(java.lang.String)
	 */
	public IDepot getDepot(String name) throws P4JavaException {
		if (name == null) {
			throw new NullPointerError("null depot name to getDepot method");
		}
		IDepot depot = null;
		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.DEPOT,
													new String[] {"-o", name},
													null);
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {					
					handleErrorStr(map);
					if (!isInfoMessage(map)) {
						depot = new Depot(map);
					}
				}
			}
		}
		return depot;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#createDepot(com.perforce.p4java.core.IDepot)
	 */
	public String createDepot(IDepot newDepot) throws P4JavaException {
		if (newDepot == null) {
			throw new NullPointerError("null depot passed to createDepot method");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.DEPOT,
												new String[] {"-i"},
												InputMapper.map(newDepot));
		String retVal = null;
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deleteDepot(com.perforce.p4java.core.IDepot)
	 */
	public String deleteDepot(String name) throws P4JavaException {
		if (name == null) {
			throw new NullPointerError("null depot name passed to deletDepot method");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.DEPOT,
											new String[] {"-d", name},
											null);
		
		String retVal = null;
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}
	
	
	/**
	 * @see com.perforce.p4java.server.IServer#getChangelists(int, java.util.List, java.lang.String, java.lang.String, boolean, com.perforce.p4java.core.IChangelist.Type, boolean)
	 */
	public List<IChangelistSummary> getChangelists(int maxMostRecent,
			List<IFileSpec> fileSpecs, String clientName, String userName,
			boolean includeIntegrated, Type type, boolean longDesc)
			throws ConnectionException, RequestException, AccessException {
		try {
			return getChangelists(fileSpecs, new GetChangelistsOptions()
														.setClientName(clientName)
														.setIncludeIntegrated(includeIntegrated)
														.setLongDesc(longDesc)
														.setMaxMostRecent(maxMostRecent)
														.setType(type)
														.setUserName(userName));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getChangelists(int, java.util.List, java.lang.String, java.lang.String, boolean, boolean, boolean, boolean)
	 */
	public List<IChangelistSummary> getChangelists(int maxMostRecent, List<IFileSpec> fileSpecs, String clientName,
			String userName, boolean includeIntegrated, boolean submittedOnly, boolean pendingOnly,
							boolean longDesc) 
					throws ConnectionException, RequestException, AccessException {
		IChangelist.Type type = null;
		if( submittedOnly ) {
			type = Type.SUBMITTED;
		} else if( pendingOnly) {
			type = Type.PENDING;
		}
		return getChangelists(maxMostRecent, fileSpecs, clientName, userName, includeIntegrated, type, longDesc);
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getChangelists(java.util.List, com.perforce.p4java.option.server.GetChangelistsOptions)
	 */
	public List<IChangelistSummary> getChangelists(List<IFileSpec> fileSpecs, GetChangelistsOptions opts)
														throws P4JavaException {
		List<IChangelistSummary> changeListList = new ArrayList<IChangelistSummary>();

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CHANGES,
														Parameters.processParameters(opts, fileSpecs, this),
														null);
		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {				
				if (!handleErrorStr(result)) {
					try {
						changeListList.add(new ChangelistSummary(result, true));
					} catch (Exception exc) {
						Log.exception(exc);
						throw new P4JavaError("Unexpected conversion error in getChangelist: "
								+ exc.getLocalizedMessage(), exc);
					}
				}
			}
		}

		return changeListList;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getChangelist(int)
	 */
	public IChangelist getChangelist(int id)
				throws ConnectionException, RequestException, AccessException {
		
		try {
			return getChangelist(id, null);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getChangelist(int, com.perforce.p4java.option.server.ChangelistOptions)
	 */
	public IChangelist getChangelist(int id, ChangelistOptions opts) throws P4JavaException {
		
		// We just pick up the change metadata here, as users can get the file list and diffs
		// separately through the various IChangelist methods and misc. methods below.
		
		IChangelist changeList = null;
		String[] args = null;
		if (id == IChangelist.DEFAULT) {
			args = new String[] { "-o" };
		} else {
			args = new String[] { "-o", "" + id };
		}
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CHANGE,
													Parameters.processParameters(
															opts, null, args, this),
													null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {					
					handleErrorStr(map);
					if (!isInfoMessage(map)) {
						changeList = new Changelist(map, this);
					}
				}
			}
		}
		
		return changeList;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#deletePendingChangelist(int)
	 */
	public String deletePendingChangelist(int id)
					throws ConnectionException, RequestException, AccessException {
		try {
			return deletePendingChangelist(id, null);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deletePendingChangelist(int, com.perforce.p4java.option.server.ChangelistOptions)
	 */
	public String deletePendingChangelist(int id, ChangelistOptions opts) throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.CHANGE,
											Parameters.processParameters(
													opts, null, new String[] { "-d", "" + id }, this),
											null);
		String retVal = null;
		if (resultMaps != null) {	
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
			
		}
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getChangelistFiles(int)
	 */
	public List<IFileSpec> getChangelistFiles(int id)
						throws ConnectionException, RequestException, AccessException {

		// NOTE: do NOT change the location or order of the "-s" flag below, as its
		// existence is used to signal to the underlying RPC implementation(s) that tagged
		// output must (or must not) be used with this particular "describe" command. See
		// OneShotServerImpl.useTags() for a canonical example of this...

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.DESCRIBE,
												new String[] { "-s", "" + id }, null);
		
		List<IFileSpec> fileList = new ArrayList<IFileSpec>();
		
		if (resultMaps != null) {
			// NOTE: all the results are returned in *one* map, not an array of them...
			
			if ((resultMaps.size() > 0) && (resultMaps.get(0) != null)) {
				
				Map<String, Object> map = resultMaps.get(0);
				
				for (int i = 0; map.get("rev" + i) != null; i++) {
					FileSpec fSpec = new FileSpec(map, this, i);
					fSpec.setChangelistId(id);
					fileList.add(fSpec);
				}
			}
		}
		return fileList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getShelvedFiles(int)
	 */
	public List<IFileSpec> getShelvedFiles(int id) throws P4JavaException {

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.DESCRIBE,
												new String[] { "-s", "-S", "" + id }, null);
		
		List<IFileSpec> fileList = new ArrayList<IFileSpec>();
		
		if (resultMaps != null) {

			if ((resultMaps.size() > 0) && (resultMaps.get(0) != null)) {
				
				Map<String, Object> map = resultMaps.get(0);
				
				for (int i = 0; map.get("rev" + i) != null; i++) {
					FileSpec fSpec = new FileSpec(map, this, i);
					fSpec.setChangelistId(id);
					fileList.add(fSpec);
				}
			}
		}
		return fileList;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getChangelistDiffs(int, com.perforce.p4java.core.file.DiffType)
	 */
	public InputStream getChangelistDiffs(int id, DiffType diffType)
					throws ConnectionException, RequestException, AccessException {
		return getChangelistDiffsStream(id, new DescribeOptions(diffType));
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getChangelistDiffs(int, com.perforce.p4java.option.server.GetChangelistDiffsOptions)
	 */
	public InputStream getChangelistDiffs(int id, GetChangelistDiffsOptions opts)
					throws P4JavaException {
		return this.execStreamCmd(CmdSpec.DESCRIBE,
										Parameters.processParameters(opts, null, "" + id, this));
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getChangelistDiffsStream(int, com.perforce.p4java.option.server.DescribeOptions)
	 */
	public InputStream getChangelistDiffsStream(int id, DescribeOptions options) throws ConnectionException, RequestException,
			AccessException {
		
		DiffType diffType = null;
		boolean shelvedDiffs = false;
		if( options != null) {
			diffType = options.getType();
			shelvedDiffs = options.isOutputShelvedDiffs();
		}
		
		// Shelved file diffs are only support in server version 2009.2+
		if (shelvedDiffs && this.getServerVersion() < 20092) {
			throw new RequestException(
					"Shelved file diffs are not supported by this version of the Perforce server",
					MessageGenericCode.EV_UPGRADE, MessageSeverityCode.E_FAILED);
		}
		
		try {
			GetChangelistDiffsOptions opts = new GetChangelistDiffsOptions();
			opts.setOutputShelvedDiffs(shelvedDiffs);
			
			if (diffType != null) {
				switch (diffType) {
				case RCS_DIFF:
					opts.setRcsDiffs(true);
					break;
				case CONTEXT_DIFF:
					opts.setDiffContext(0);
					break;
				case SUMMARY_DIFF:
					opts.setSummaryDiff(true);
					break;
				case UNIFIED_DIFF:
					opts.setUnifiedDiff(0);
					break;
				case IGNORE_WS_CHANGES:
					opts.setIgnoreWhitespaceChanges(true);
					break;
				case IGNORE_WS:
					opts.setIgnoreWhitespace(true);
					break;
				case IGNORE_LINE_ENDINGS:
					opts.setIgnoreLineEndings(true);
					break;
				}
			}
			
			return getChangelistDiffs(id, opts);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getRevisionHistory(java.util.List, int,
	 * 				boolean, boolean, boolean, boolean)
	 */
	public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(List<IFileSpec> fileSpecs,
			int maxRevs, boolean contentHistory, boolean includeInherited, boolean longOutput,
					boolean truncatedLongOutput) throws ConnectionException, AccessException {
		try {
			return getRevisionHistory(fileSpecs, new GetRevisionHistoryOptions()
														.setContentHistory(contentHistory)
														.setIncludeInherited(includeInherited)
														.setLongOutput(longOutput)
														.setTruncatedLongOutput(truncatedLongOutput)
														.setMaxRevs(maxRevs));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IServer.getRevisionHistory: " + exc);
			return new HashMap<IFileSpec, List<IFileRevisionData>>();
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getRevisionHistory(java.util.List, com.perforce.p4java.option.server.GetRevisionHistoryOptions)
	 */
	public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(List<IFileSpec> fileSpecs,
			GetRevisionHistoryOptions opts) throws P4JavaException {
		Map<IFileSpec, List<IFileRevisionData>> revMap
								= new HashMap<IFileSpec, List<IFileRevisionData>>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
							CmdSpec.FILELOG, Parameters.processParameters(opts, fileSpecs, this),
							null);

		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				String errStr = this.handleFileErrorStr(result);
				if (errStr != null) {
					FileSpec fSpec = new FileSpec(FileSpecOpStatus.ERROR, errStr,
														(String) result.get("code0"));
					String depotPath = (String) result.get("depotFile");
					fSpec.setDepotPath(depotPath);
					revMap.put(fSpec, null);
				} else {
					int revNum = 0;
					List<IFileRevisionData> revList = new ArrayList<IFileRevisionData>();
					String depotFilePath = (String) result.get("depotFile");
					FileSpec fSpec = new FileSpec();
					fSpec.setDepotPath(depotFilePath);
					revMap.put(fSpec, revList);
					while ((result.get("rev" + revNum)) != null) {
						revList.add(new FileRevisionData(result, revNum));
						revNum++;
					}
				}
			}
		}
		return revMap;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getOpenedFiles(java.util.List, boolean, java.lang.String, int, int)
	 */
	public List<IFileSpec> getOpenedFiles(List<IFileSpec> fileSpecs, boolean allClients, String clientName,
								int maxFiles, int changeListId)
					throws ConnectionException, AccessException {

		try {
			return this.getOpenedFiles(fileSpecs,
					new OpenedFilesOptions(
								allClients, clientName, maxFiles, null, changeListId
					));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IServer.openedFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getOpenedFiles(java.util.List, com.perforce.p4java.option.server.OpenedFilesOptions)
	 */
	public List<IFileSpec> getOpenedFiles(List<IFileSpec> fileSpecs, OpenedFilesOptions opts)
									throws P4JavaException {
		List<IFileSpec> openedList = new ArrayList<IFileSpec>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
								CmdSpec.OPENED, Parameters.processParameters(
													opts, fileSpecs, this), null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				openedList.add(handleFileReturn(map));
			}
		}
		return openedList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getFileContents(java.util.List, boolean, boolean)
	 */
	public InputStream getFileContents(List<IFileSpec> fileSpecs, boolean allRevs,
														boolean noHeaderLine)
						throws ConnectionException, RequestException, AccessException {
		
		try {
			return getFileContents(fileSpecs, new GetFileContentsOptions(allRevs, noHeaderLine));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getFileContents(java.util.List, com.perforce.p4java.option.server.GetFileContentsOptions)
	 */
	public InputStream getFileContents(List<IFileSpec> fileSpecs, GetFileContentsOptions opts)
										throws P4JavaException {

		boolean annotateFiles = !(opts != null ?  opts.isDontAnnotateFiles() : false);
		
		return this.execStreamCmd(CmdSpec.PRINT, Parameters.processParameters(opts, fileSpecs, null, annotateFiles, this));
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getDirectories(java.util.List, boolean, boolean, boolean)
	 */
	public List<IFileSpec> getDirectories(List<IFileSpec> fileSpecs, boolean clientOnly,
							boolean deletedOnly, boolean haveListOnly)
										throws ConnectionException, AccessException {
		
		if (fileSpecs == null) {
			throw new NullPointerError("Null fileSpecs in getDirectories");
		}

		try {
			return getDirectories(fileSpecs, new GetDirectoriesOptions()
													.setClientOnly(clientOnly)
													.setDeletedOnly(deletedOnly)
													.setHaveListOnly(haveListOnly));
		}  catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IServer.getDirectories: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getDirectories(java.util.List, com.perforce.p4java.option.server.GetDirectoriesOptions)
	 */
	public List<IFileSpec> getDirectories(List<IFileSpec> fileSpecs, GetDirectoriesOptions opts)
													throws P4JavaException {

		// It seems the tagged result doesn't return an error message
		// for non-existing dirs. See job050447 for details.
		// We're turning off tagged output for the "dirs" command
		HashMap<String, Object> inMap = new HashMap<String, Object>();
		inMap.put(IN_MAP_USE_TAGS_KEY, "no");
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.DIRS,
												Parameters.processParameters(opts, fileSpecs, this),
												inMap);
		List<IFileSpec> specList = new ArrayList<IFileSpec>();

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				// Special cases for directories, unfortunately -- they're not
				// really first-class parts of the Perforce file menagerie...
	
				if (map != null) {
					String errStr = handleFileErrorStr(map);
					if (errStr == null) {
						if (map.get("dirName") != null) {
							specList.add(new FileSpec((String) map.get("dirName")));
						} else if (map.get("dir") != null) {
							specList.add(new FileSpec((String) map.get("dir")));
						}
					} else {
						if (isInfoMessage(map)) {
							if (map.get("dirName") != null) {
								specList.add(new FileSpec((String) map.get("dirName")));
							} else if (map.get("dir") != null) {
								specList.add(new FileSpec((String) map.get("dir")));
							} else {
								specList.add(new FileSpec(FileSpecOpStatus.INFO, errStr,
															(String) map.get("code0")));
							}
						} else {
							specList.add(new FileSpec(FileSpecOpStatus.ERROR, errStr,
													(String) map.get("code0")));
						}
					}
				}
			}
		}
	
		return specList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getSubmittedIntegrations(java.util.List, java.lang.String, boolean)
	 */
	public List<IFileSpec> getSubmittedIntegrations(List<IFileSpec> fileSpecs,
												String branchSpec, boolean reverseMappings)
					throws ConnectionException, RequestException, AccessException {
		try {
			return getSubmittedIntegrations(fileSpecs,
									new GetSubmittedIntegrationsOptions(branchSpec, reverseMappings));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getSubmittedIntegrations(java.util.List, com.perforce.p4java.option.server.GetSubmittedIntegrationsOptions)
	 */
	public List<IFileSpec> getSubmittedIntegrations(List<IFileSpec> fileSpecs, GetSubmittedIntegrationsOptions opts)
								throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.INTEGRATED,
											Parameters.processParameters(opts, fileSpecs, this),
											null);
		
		List<IFileSpec> integList = new ArrayList<IFileSpec>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				integList.add(handleIntegrationFileReturn(map, null));
			}
		}
		
		return integList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getInterchanges(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, boolean, boolean, int)
	 */
	public List<IChangelist> getInterchanges(IFileSpec fromFile, IFileSpec toFile, boolean showFiles,
												boolean longDesc, int maxChangelistId)
						throws ConnectionException, RequestException, AccessException {
		try {
			return getInterchanges(fromFile, toFile,
													new GetInterchangesOptions()
														.setShowFiles(showFiles)
														.setLongDesc(longDesc)
														.setMaxChangelistId(maxChangelistId));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getInterchanges(java.lang.String, java.util.List, java.util.List, boolean, boolean, int, boolean, boolean)
	 */
	public List<IChangelist> getInterchanges(String branchSpecName,
							List<IFileSpec> fromFileList, List<IFileSpec> toFileList,
							boolean showFiles, boolean longDesc, int maxChangelistId,
							boolean reverseMapping, boolean biDirectional)
				throws ConnectionException, RequestException, AccessException {
		try {
			return getInterchanges(branchSpecName, fromFileList, toFileList,
											new GetInterchangesOptions()
														.setShowFiles(showFiles)
														.setLongDesc(longDesc)
														.setMaxChangelistId(maxChangelistId)
														.setReverseMapping(reverseMapping)
														.setBiDirectional(biDirectional));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getInterchanges(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.option.server.GetInterchangesOptions)
	 */
	public List<IChangelist> getInterchanges(IFileSpec fromFile, IFileSpec toFile, GetInterchangesOptions opts)
								throws P4JavaException {
		List<IFileSpec> files = new ArrayList<IFileSpec>();
		files.add(fromFile);
		files.add(toFile);
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.INTERCHANGES,
											Parameters.processParameters(opts, files, this),
											null);
		return processInterchangeMaps(resultMaps, opts == null ? false : opts.isShowFiles());
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getInterchanges(java.lang.String, java.util.List, java.util.List, com.perforce.p4java.option.server.GetInterchangesOptions)
	 */
	public List<IChangelist> getInterchanges(String branchSpecName,
			List<IFileSpec> fromFileList, List<IFileSpec> toFileList, GetInterchangesOptions opts)
								throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.INTERCHANGES,
				Parameters.processParameters(opts, fromFileList, toFileList, branchSpecName, this),
				null);
		return processInterchangeMaps(resultMaps, opts == null ? false : opts.isShowFiles());
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getExtendedFiles(java.util.List, int, int, int, com.perforce.p4java.core.file.FileStatOutputOptions, com.perforce.p4java.core.file.FileStatAncilliaryOptions)
	 */
	public List<IExtendedFileSpec> getExtendedFiles(List<IFileSpec> fileSpecs, int maxFiles,
			int sinceChangelist, int affectedByChangelist,
			FileStatOutputOptions outputOptions, FileStatAncilliaryOptions ancilliaryOptions)
					throws ConnectionException, AccessException {
		try {
			return getExtendedFiles(fileSpecs, new GetExtendedFilesOptions()
														.setAncilliaryOptions(ancilliaryOptions)
														.setMaxResults(maxFiles)
														.setOutputOptions(outputOptions)
														.setSinceChangelist(sinceChangelist)
														.setAffectedByChangelist(affectedByChangelist)
													);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IServer.getExtendedFiles: " + exc);
			return new ArrayList<IExtendedFileSpec>();
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getExtendedFiles(java.util.List, com.perforce.p4java.option.server.GetExtendedFilesOptions)
	 */
	public List<IExtendedFileSpec> getExtendedFiles(List<IFileSpec> fileSpecs,
						GetExtendedFilesOptions opts) throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.FSTAT,
					Parameters.processParameters(opts, fileSpecs, this), null);

		List<IExtendedFileSpec> specList = new ArrayList<IExtendedFileSpec>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				// We do this by hand for the statFiles case; this may be
				// included in the generic handler later -- HR.
				
				// Note: as of 10.1 or so, fstats on shelved files may return
				// a "special" fstat info message (usually the last message) that
				// contains only the description  field of the associated changelist
				// (see fstat -e documentation for this); therefore we carefully weed
				// out any return map here that has no depot path and a "desc" field
				// -- HR (see also job040680).
				
				String errStr = handleFileErrorStr(map);
				ExtendedFileSpec eSpec = null;
				if (errStr == null) {
					if (map.containsKey("depotFile") && !map.containsKey("desc")) {
						eSpec = new ExtendedFileSpec(map, this, -1);
					}
				} else {
					if (isInfoMessage(map)) {
						eSpec = new ExtendedFileSpec(FileSpecOpStatus.INFO, errStr);
					} else {
						eSpec = new ExtendedFileSpec(FileSpecOpStatus.ERROR, errStr);
					}
				}
				if (eSpec != null) {
					specList.add(eSpec);
				}
			}
		}
		return specList;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#searchJobs(java.lang.String, com.perforce.p4java.option.server.SearchJobsOptions)
	 */
	public 	List<String> searchJobs(String words, SearchJobsOptions opts) throws P4JavaException {

		if (words == null) {
			throw new NullPointerError("Null words passed to searchJobs method");
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.SEARCH,
													Parameters.processParameters(
															opts, null, new String[] {words}, this),
													null);
		
		List<String> jobIdList = new ArrayList<String>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					String errStr = getErrorStr(map);
					
					if (errStr != null) {
						throw new RequestException(errStr, (String) map.get("code0"));
					} else {
						jobIdList.add(getInfoStr(map));
					}
				}
			}
		}
		
		return jobIdList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getJobs(java.util.List, int, boolean, boolean, boolean, java.lang.String)
	 */
	public List<IJob> getJobs(List<IFileSpec> fileSpecs, int maxJobs, boolean longDescriptions,
											boolean reverseOrder, boolean includeIntegrated,
											String jobView)
						throws ConnectionException, RequestException, AccessException {
		try {
			return getJobs(fileSpecs, new GetJobsOptions()
											.setIncludeIntegrated(includeIntegrated)
											.setLongDescriptions(longDescriptions)
											.setMaxJobs(maxJobs)
											.setReverseOrder(reverseOrder)
											.setJobView(jobView));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getJobs(java.util.List, com.perforce.p4java.option.server.GetJobsOptions)
	 */
	public List<IJob> getJobs(List<IFileSpec> fileSpecs, GetJobsOptions opts) throws P4JavaException {
		List<IJob> jobList = new ArrayList<IJob>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
								CmdSpec.JOBS,
								Parameters.processParameters(opts, fileSpecs, this),
								null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					String errStr = getErrorStr(map);
					
					if (errStr != null) {
						throw new RequestException(errStr, (String) map.get("code0"));
					} else {
						jobList.add(new Job(this, map, opts == null ? false : opts.isLongDescriptions()));
					}
				}
			}
		}
		
		return jobList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getJob(java.lang.String)
	 */
	
	public IJob getJob(String jobId)
				throws ConnectionException, RequestException, AccessException {
		
		if (jobId == null) {
			throw new P4JavaError("Null jobId in server.getJob()");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
							CmdSpec.JOB,
							new String[] { "-o", jobId },
							null);
		IJob job = null;
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (!isInfoMessage(map)) {
					job = new Job(this, map);
				}
			}
		}
		
		return job;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#createJob(java.util.Map)
	 */
	public IJob createJob(Map<String, Object> fieldMap)
				throws ConnectionException, RequestException, AccessException {
		if (fieldMap == null) {
			throw new NullPointerError(
				"Null field map passed to ServerImpl.createJob");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.JOB,
											new String[] { "-i" },
											fieldMap);
		
		if (resultMaps != null) {
			// What comes back is a simple info message that contains the
			// job ID, a trigger output info message, or an error message; in the first instance we retrieve
			// the new ID then get the job; otherwise we throw the error.
			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				String infoStr = getInfoStr(map);
				
				if ((infoStr != null) && infoStr.contains("Job ") && infoStr.contains(" saved")) {
					// usually in format "Job jobid saved"
					
					String[] strs = infoStr.split(" ");
					if (strs.length == 3) {
						if (strs[1] != null) {
							return this.getJob(strs[1]);
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#updateJob(com.perforce.p4java.core.IJob)
	 */
	public String updateJob(IJob job)
					throws ConnectionException, RequestException, AccessException {
		if (job == null) {
			throw new NullPointerError(
				"Null job passed to Server.updateJob");
		}

		String retVal = null;
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.JOB,
												new String[] { "-i" },
												job.getRawFields());
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = this.getInfoStr(map);
					} else {
						retVal += "\n" + this.getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#deleteJob(java.lang.String)
	 */
	public String deleteJob(String jobId)
					throws ConnectionException, RequestException, AccessException {
		if (jobId == null) {
			throw new NullPointerError(
				"Null job ID passed to Server.deleteJob");
		}
		
		String retVal = null;
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
				CmdSpec.JOB,
				new String[] { "-d", jobId },
				null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = this.getInfoStr(map);
					} else {
						retVal += "\n" + this.getInfoStr(map);
					}
				}
			}
		}

		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getJobSpec()
	 */
	
	public IJobSpec getJobSpec()
					throws ConnectionException, RequestException, AccessException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
				CmdSpec.JOBSPEC,
				new String[] {"-o"},
				null);
		IJobSpec jobSpec = null;
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					handleErrorStr(map);
					if (!isInfoMessage(map)) {
						jobSpec = new JobSpec(map, this);
					}
				}
			}
		}
		return jobSpec;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getFixList(java.util.List, int, java.lang.String, boolean, int)
	 */
	public List<IFix> getFixList(List<IFileSpec> fileSpecs, int changeListId, String jobId,
							boolean includeIntegrations, int maxFixes)
					throws ConnectionException, RequestException, AccessException {
		try {
			// Note the hack below to get backwards compatibility with the
			// need to let IChangelist.DEFAULT be similar to saying *no*
			// changelist, which was arguably wrong and is fixed in the
			// new version where DEFAULT is OK. See job 040703.
			
			return getFixes(fileSpecs, new GetFixesOptions()
												.setChangelistId(
														changeListId == IChangelist.DEFAULT ?
																IChangelist.UNKNOWN : changeListId)
												.setIncludeIntegrations(includeIntegrations)
												.setJobId(jobId)
												.setMaxFixes(maxFixes));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getFixList(java.util.List, com.perforce.p4java.option.server.GetFixesOptions)
	 */
	public List<IFix> getFixes(List<IFileSpec> fileSpecs, GetFixesOptions opts)
											throws P4JavaException {
		List<IFix> fixList = new ArrayList<IFix>();
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.FIXES,
											Parameters.processParameters(opts, fileSpecs, this),
											null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					handleErrorStr(map);
					fixList.add(new Fix(map));
				}
			}
		}
		
		return fixList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#fixJobs(java.util.List, int, java.lang.String, boolean)
	 */
	public List<IFix> fixJobs(List<String> jobIdList, int changeListId, String status, boolean delete)
					throws ConnectionException, RequestException, AccessException {
		try {
			return fixJobs(jobIdList, changeListId,
									new FixJobsOptions().setDelete(delete).setStatus(status));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#fixJobs(java.util.List, com.perforce.p4java.option.server.FixJobsOptions)
	 */
	public List<IFix> fixJobs(List<String> jobIds, int changelistId, FixJobsOptions opts)
							throws P4JavaException {
		if (jobIds == null) {
			throw new P4JavaError("Null jobIds list in fixJobs");
		}
		
		List<String> args = new ArrayList<String>();
		args.add("-c" + (changelistId == IChangelist.DEFAULT ? "default" : changelistId));
		args.addAll(jobIds);
		List<IFix> fixList = new ArrayList<IFix>();
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.FIX,
											Parameters.processParameters(
													opts,
													null,
													args.toArray(new String[args.size()]),
													this),
											null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorStr(map);

				if (errStr != null) {
					throw new RequestException(errStr, (String) map.get("code0"));
				}

				fixList.add(new Fix(map));
			}
		}
		
		return fixList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getCounter(java.lang.String)
	 */
	public String getCounter(String counterName) 
					throws ConnectionException, RequestException, AccessException {
		try {
			return getCounter(counterName, null);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getCounter(java.lang.String, com.perforce.p4java.option.server.CounterOptions)
	 */
	public String getCounter(String counterName, CounterOptions opts) throws P4JavaException {
		if (counterName == null) {
			throw new NullPointerError("null counter name passed to getCounter method");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.COUNTER,
													Parameters.processParameters(
															opts, null, new String[] {counterName}, this),
													null);
		String retVal = "";
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					if (map.containsKey("value")) {
						return (String) map.get("value");
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#setCounter(java.lang.String, java.lang.String, boolean)
	 */
	public void setCounter(String counterName, String value, boolean perforceCounter)
					throws ConnectionException, RequestException, AccessException
	{
		if (counterName == null) {
			throw new NullPointerError("null counter name passed to setCounter method");
		}
		if (value == null) {
			throw new NullPointerError("null counter value passed to setCounter method");
		}

		try {
			setCounter(counterName, value, new CounterOptions().setPerforceCounter(perforceCounter));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#setCounter(java.lang.String, java.lang.String, com.perforce.p4java.option.server.CounterOptions)
	 */
	public String setCounter(String counterName, String value, CounterOptions opts)
									throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.COUNTER,
											Parameters.processParameters(
													opts, null, new String[] {counterName, value}, this),
											null);
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (map.containsKey("value")) {
					return (String) map.get("value");
				}
			}
		}
		return null;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#deleteCounter(java.lang.String, boolean)
	 */
	public void deleteCounter(String counterName, boolean perforceCounter)
					throws ConnectionException, RequestException, AccessException {
		if (counterName == null) {
			throw new NullPointerError("null counter name passed to setCounter method");
		}
		try {
			setCounter(counterName, null, new CounterOptions(perforceCounter, true, false));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getCounters()
	 */
	
	public 	Map<String, String> getCounters()
				throws ConnectionException, RequestException, AccessException {
		
		try {
			return getCounters((GetCountersOptions)null);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getCounters(com.perforce.p4java.option.server.CounterOptions)
	 * 
	 * @deprecated As of release 2013.1, replaced by {@link #getCounters(com.perforce.p4java.option.server.GetCountersOptions)}
 	 */
	@Deprecated
	public 	Map<String, String> getCounters(CounterOptions opts) throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.COUNTERS,
													Parameters.processParameters(opts, this),
													null);
		
		Map<String, String> counterMap = new HashMap<String, String>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorOrInfoStr(map);
				
				if (errStr != null) {
					if (isAuthFail(errStr)) {
						throw new AccessException(errStr);
					} else {
						throw new RequestException(errStr, (String) map.get("code0"));
					}
				} else {
					try {
						counterMap.put((String) map.get("counter"), (String) map.get("value"));
					} catch (Exception exc) {
						Log.error("getCounter conversion error: " + exc.getLocalizedMessage());
						Log.exception(exc);
					}
				}
			}
		}
		
		return counterMap;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getCounters(com.perforce.p4java.option.server.GetCountersOptions)
	 */
	public 	Map<String, String> getCounters(GetCountersOptions opts) throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.COUNTERS,
													Parameters.processParameters(opts, this),
													null);
		
		Map<String, String> counterMap = new HashMap<String, String>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorOrInfoStr(map);
				
				if (errStr != null) {
					if (isAuthFail(errStr)) {
						throw new AccessException(errStr);
					} else {
						throw new RequestException(errStr, (String) map.get("code0"));
					}
				} else {
					try {
						counterMap.put((String) map.get("counter"), (String) map.get("value"));
					} catch (Exception exc) {
						Log.error("getCounter conversion error: " + exc.getLocalizedMessage());
						Log.exception(exc);
					}
				}
			}
		}
		
		return counterMap;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getKey(java.lang.String)
	 */
	public String getKey(String keyName) throws P4JavaException {
		if (keyName == null) {
			throw new NullPointerError("null counter name passed to getKey method");
		}
		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.KEY,
													new String[] {keyName},
													null);
		String retVal = "";
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					if (map.containsKey("value")) {
						return (String) map.get("value");
					}
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#setKey(java.lang.String, java.lang.String, com.perforce.p4java.option.server.KeyOptions)
	 */
	public String setKey(String KeyName, String value, KeyOptions opts)
									throws P4JavaException {
		if (KeyName == null) {
			throw new NullPointerError("null key name passed to setKey method");
		}
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.KEY,
											Parameters.processParameters(
													opts, null, new String[] {KeyName, value}, this),
											null);
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (map.containsKey("value")) {
					return (String) map.get("value");
				}
			}
		}
		return null;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deleteKey(java.lang.String)
	 */
	public String deleteKey(String KeyName) throws P4JavaException {
		if (KeyName == null) {
			throw new NullPointerError("null key name passed to deleteKey method");
		}
		return setKey(KeyName, null, new KeyOptions(true, false));
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getKeys(com.perforce.p4java.option.server.GetKeysOptions)
	 */
	public Map<String, String> getKeys(GetKeysOptions opts) throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.KEYS,
													Parameters.processParameters(opts, this),
													null);
		
		Map<String, String> keyMap = new HashMap<String, String>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorOrInfoStr(map);
				
				if (errStr != null) {
					if (isAuthFail(errStr)) {
						throw new AccessException(errStr);
					} else {
						throw new RequestException(errStr, (String) map.get("code0"));
					}
				} else {
					try {
						keyMap.put((String) map.get("key"), (String) map.get("value"));
					} catch (Exception exc) {
						Log.error("getKeys conversion error: " + exc.getLocalizedMessage());
						Log.exception(exc);
					}
				}
			}
		}
		
		return keyMap;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getProperty(com.perforce.p4java.option.server.GetPropertyOptions)
	 */
	public List<IProperty> getProperty(GetPropertyOptions opts) throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.PROPERTY,
												Parameters.processParameters(
														opts, null, new String[] {"-l"}, this),
												null);
		
		List<IProperty> propertyList = new ArrayList<IProperty>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorOrInfoStr(map);
				
				if (errStr != null) {
					if (isAuthFail(errStr)) {
						throw new AccessException(errStr);
					} else {
						throw new RequestException(errStr, (String) map.get("code0"));
					}
				} else {
					try {
						propertyList.add(new Property(map));
					} catch (Exception exc) {
						Log.error("getProperty conversion error: " + exc.getLocalizedMessage());
						Log.exception(exc);
					}
				}
			}
		}
		
		return propertyList;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#setProperty(java.lang.String, java.lang.String, com.perforce.p4java.option.server.PropertyOptions)
	 */
	public String setProperty(String name, String value, PropertyOptions opts)
									throws P4JavaException {
		if (name == null && (opts == null || opts.getName() == null)) {
			throw new NullPointerError("null property name passed to setProperty method");
		}
		if (value == null && (opts == null || opts.getValue() == null)) {
			throw new NullPointerError("null property value passed to setProperty method");
		}
		if (opts == null) {
			opts = new PropertyOptions();
		}
		if (name != null) {
			opts.setName(name);
		}
		if (value != null) {
			opts.setValue(value);
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.PROPERTY,
											Parameters.processParameters(
													opts, null, new String[] {"-a"}, this),
											null);
		String retVal = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in ServerImpl.setProperty");
		}
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deleteProperty(java.lang.String, com.perforce.p4java.option.server.PropertyOptions)
	 */
	public String deleteProperty(String name, PropertyOptions opts) throws P4JavaException {

		if (name == null && (opts == null || opts.getName() == null)) {
			throw new NullPointerError("null property name passed to deleteProperty method");
		}
		if (opts == null) {
			opts = new PropertyOptions();
		}
		if (name != null) {
			opts.setName(name);
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.PROPERTY,
											Parameters.processParameters(
													opts, null, new String[] {"-d"}, this),
											null);
		String retVal = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in ServerImpl.deleteProperty");
		}
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getServerProcesses()
	 */
	public List<IServerProcess> getServerProcesses()
						throws ConnectionException, RequestException, AccessException {
		
		try {
			return getServerProcesses(null);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getServerProcesses(com.perforce.p4java.option.server.GetServerProcessesOptions)
	 */
	public 	List<IServerProcess> getServerProcesses(GetServerProcessesOptions opts) throws P4JavaException {
		List<IServerProcess> processList = new ArrayList<IServerProcess>();

		List<String> args = new ArrayList<String>();
		args.add("show");

		String[] options = Parameters.processParameters(opts, this);
		if (options != null) {
			args.addAll(Arrays.asList(options));
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.MONITOR, args.toArray(new String[args.size()]), null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorStr(map);

				if (errStr != null) {
					throw new RequestException(errStr, (String) map.get("code0"));
				}

				processList.add(new ServerProcess(map));
			}
		}
		return processList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getServerFileDiffs(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, java.lang.String, com.perforce.p4java.core.file.DiffType, boolean, boolean, boolean)
	 */
	public InputStream getServerFileDiffs(IFileSpec file1, IFileSpec file2,
							String branchSpecName, DiffType diffType, boolean quiet,
							boolean includeNonTextDiffs, boolean gnuDiffs)
									throws ConnectionException, RequestException, AccessException {
		try {
			GetFileDiffsOptions opts = new GetFileDiffsOptions()
											.setQuiet(quiet)
											.setIncludeNonTextDiffs(includeNonTextDiffs)
											.setGnuDiffs(gnuDiffs);
			if (diffType != null) {
				switch (diffType) {
					case RCS_DIFF:
						opts.setRcsDiffs(true);
						break;
					case CONTEXT_DIFF:
						opts.setDiffContext(0);
						break;
					case SUMMARY_DIFF:
						opts.setSummaryDiff(true);
						break;
					case UNIFIED_DIFF:
						opts.setUnifiedDiff(0);
						break;
					case IGNORE_WS_CHANGES:
						opts.setIgnoreWhitespaceChanges(true);
						break;
					case IGNORE_WS:
						opts.setIgnoreWhitespace(true);
						break;
					case IGNORE_LINE_ENDINGS:
						opts.setIgnoreLineEndings(true);
						break;
				}
			}
			return getFileDiffsStream(file1, file2, branchSpecName, opts);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getFileDiffs(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, java.lang.String, com.perforce.p4java.core.file.DiffType, boolean, boolean, boolean)
	 */
	public List<IFileDiff> getFileDiffs(IFileSpec file1, IFileSpec file2,
			String branchSpecName, DiffType diffType, boolean quiet,
			boolean includeNonTextDiffs, boolean gnuDiffs)
			throws ConnectionException, RequestException, AccessException {
		try {
			GetFileDiffsOptions opts = new GetFileDiffsOptions()
											.setQuiet(quiet)
											.setIncludeNonTextDiffs(includeNonTextDiffs)
											.setGnuDiffs(gnuDiffs);
			if (diffType != null) {
				switch (diffType) {
					case RCS_DIFF:
						opts.setRcsDiffs(true);
						break;
					case CONTEXT_DIFF:
						opts.setDiffContext(0);
						break;
					case SUMMARY_DIFF:
						opts.setSummaryDiff(true);
						break;
					case UNIFIED_DIFF:
						opts.setUnifiedDiff(0);
						break;
					case IGNORE_WS_CHANGES:
						opts.setIgnoreWhitespaceChanges(true);
						break;
					case IGNORE_WS:
						opts.setIgnoreWhitespace(true);
						break;
					case IGNORE_LINE_ENDINGS:
						opts.setIgnoreLineEndings(true);
						break;
				}
			}
			return getFileDiffs(file1, file2, branchSpecName, opts);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getFileDiffsStream(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, java.lang.String, com.perforce.p4java.option.server.GetFileDiffsOptions)
	 */
	public InputStream getFileDiffsStream(IFileSpec file1, IFileSpec file2, String branchSpecName,
							GetFileDiffsOptions opts) throws P4JavaException {
		return this.execStreamCmd(CmdSpec.DIFF2, Parameters.processParameters(
										opts, file1, file2, branchSpecName, this));
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getFileDiffs(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, java.lang.String, com.perforce.p4java.option.server.GetFileDiffsOptions)
	 */
	public List<IFileDiff> getFileDiffs(IFileSpec file1, IFileSpec file2, String branchSpecName,
							GetFileDiffsOptions opts) throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.DIFF2,
											Parameters.processParameters(
													opts, file1, file2, branchSpecName, this),
											null);
		List<IFileDiff> diffs = new ArrayList<IFileDiff>();
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					String errStr = getErrorStr(map);
					if (errStr != null) {
						throw new RequestException(errStr, (String) map.get("code0"));
					}
					// Check for info/warn message
					errStr = getErrorOrInfoStr(map);
					if (errStr != null) {
						Log.info(errStr);
					} else {
						diffs.add(new FileDiff(map));
					}
				}
			}
		}
		
		return diffs;
	}

	/**
	 * @see com.perforce.p4java.server.IServer#getDbSchema(java.util.List)
	 */
	public List<IDbSchema> getDbSchema(List<String> tableSpecs)
						throws ConnectionException, RequestException, AccessException {
		
		String[] args = null;
		if (tableSpecs != null) {
			args = new String[tableSpecs.size()];
			int i = 0;
			for (String table: tableSpecs) {
				args[i++] = table;
			}
		}
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.DBSCHEMA, args, null);
		
		List<IDbSchema> schemaList = new ArrayList<IDbSchema>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorStr(map);
	
				if (errStr != null) {
					throw new RequestException(errStr, (String) map.get("code0"));
				}
				schemaList.add(new DbSchema(map));
			}
		}
		return schemaList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IServer#getExportRecords(boolean, long, int, long, boolean, java.lang.String, java.lang.String)
	 */
	public List<Map<String, Object>> getExportRecords(boolean useJournal, long maxRecs,
									int sourceNum, long offset, boolean format, String journalPrefix, String filter)
						throws ConnectionException, RequestException, AccessException {
		try {
			return getExportRecords(new ExportRecordsOptions()
											.setFormat(format)
											.setFilter(filter)
											.setJournalPrefix(journalPrefix)
											.setMaxRecs(maxRecs)
											.setOffset(offset)
											.setSourceNum(sourceNum)
											.setUseJournal(useJournal));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getExportRecords(com.perforce.p4java.option.server.ExportRecordsOptions)
	 */
	public List<Map<String, Object>> getExportRecords(ExportRecordsOptions opts)
								throws P4JavaException {
		Map<String, Object> inMap = new HashMap<String, Object>();
		if (opts != null) {
			inMap = opts.processFieldRules();
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.EXPORT,
												Parameters.processParameters(opts, this),
												inMap);
		
		List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorStr(map);
				if (errStr != null) {
					throw new RequestException(errStr, (String) map.get("code0"));
				}
				if (map.containsKey("func")) {
					map.remove("func");
				}
				mapList.add(map);
			}
		}
		return mapList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getStreamingExportRecords(com.perforce.p4java.option.server.ExportRecordsOptions, com.perforce.p4java.server.callback.IStreamingCallback, int)
	 */
	public void getStreamingExportRecords(ExportRecordsOptions opts, IStreamingCallback callback, int key)
								throws P4JavaException {
		if (callback == null) {
			throw new NullPointerError(
							"null streaming callback passed to getStreamingExportRecords method");
		}
		
		Map<String, Object> inMap = new HashMap<String, Object>();
		if (opts != null) {
			inMap = opts.processFieldRules();
		}
		
		this.execStreamingMapCommand(CmdSpec.EXPORT.toString(),
				Parameters.processParameters(opts, this), inMap, callback, key);
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getDiskSpace(java.util.List)
	 */
	public List<IDiskSpace> getDiskSpace(List<String> filesystems)
								throws P4JavaException {
		List<IDiskSpace> diskSpaceList = new ArrayList<IDiskSpace>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.DISKSPACE,
												filesystems == null ? null : filesystems.toArray(new String[filesystems.size()]),
												null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					handleErrorStr(map);
					diskSpaceList.add(new DiskSpace(map));
				}
			}
		}

		return diskSpaceList;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#setFileAttributes(com.perforce.p4java.option.server.SetFileAttributesOptions, java.util.Map, java.util.List)
	 */
	public List<IFileSpec> setFileAttributes(List<IFileSpec> files, Map<String, String> attributes,
							SetFileAttributesOptions opts) throws P4JavaException {
		if (attributes == null) {
			throw new NullPointerError("null attributes map passed to setFileAttributes");
		}
		
		/* Note the rather odd parameter processing below, required due to the rather odd way
		 * attributes are passed to the server (or not) -- each name must have a -n flag attached,
		 * and each value a corresponding -v flag; it's unclear what happens when these don't match up, but
		 * never mind... in any case, after some experimentation, it seems safest to bunch all
		 * the names first, then the values. This may change with further experimentation.
		 */
		List<String> args = new ArrayList<String>();
		
		if (attributes != null) {
			for (String name : attributes.keySet()) {
				args.add("-n" + name);
			}
			for (Map.Entry<String, String> entry : attributes.entrySet()) {
				if (entry.getValue() != null) {
					args.add("-v" + entry.getValue());
				}
			}
		}
		
		/*
		 * Note that this is the one command that doesn't adhere to the (admittedly
		 * loose) rules about multiple returns and tag names, etc, meaning that we
		 * anomalously return a list of result strings rather than file specs; the
		 * reason underlying this is that each attribute set causes a result row,
		 * meaning we may get multiple results back from the server for the same file,
		 * and several error messages, etc. -- all from the single request. This seems
		 * less than optimal to me...
		 */
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.ATTRIBUTE,
											Parameters.processParameters(
													opts,
													files,
													args == null ? null : args.toArray(new String[args.size()]),
													true,
													this),
											null);
		
		List<IFileSpec> resultList = new ArrayList<IFileSpec>();
		
		if (resultMaps != null) {
			List<String> filesSeen = new ArrayList<String>();
			for (Map<String, Object> map : resultMaps) {
				IFileSpec spec = handleFileReturn(map);
				if (spec.getOpStatus() == FileSpecOpStatus.VALID) {
					//resultList.add(spec);
					String file = spec.getAnnotatedPathString(PathType.DEPOT);
					if (file != null) {
						if (!filesSeen.contains(file)) {
							filesSeen.add(file);
							resultList.add(spec);
						}
					}
				} else {
					resultList.add(spec);
				}
			}
		}
		
		return resultList;

	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#setFileAttributes(com.perforce.p4java.option.server.SetFileAttributesOptions, java.io.InputStream, java.util.List)
	 */
	public List<IFileSpec> setFileAttributes(List<IFileSpec> files, String attributeName,
					InputStream inStream, SetFileAttributesOptions opts) throws P4JavaException {
		if (inStream == null) {
			throw new NullPointerError("null input stream passed to setFileAttributes");
		}
		if (attributeName == null) {
			throw new NullPointerError("null attribute name passed to setFileAttributes");
		}
		/*
		 * Note that we use the map argument here to pass in a single stream; this can
		 * be expanded later if the server introduces multiple streams for attributes (not
		 * likely for the attributes command, but other commands may do this in the distant
		 * future, and anyway, it's the thought that counts...).
		 */
		Map<String, Object> inputMap = new HashMap<String, Object>();
		inputMap.put(ATTRIBUTE_STREAM_MAP_KEY, inStream);
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.ATTRIBUTE,
													Parameters.processParameters(
															opts,
															files,
															new String[] {"-i", "-n" + attributeName},
															true,
															this),
													inputMap);

		
		List<IFileSpec> resultList = new ArrayList<IFileSpec>();
		
		if (resultMaps != null) {
			List<String> filesSeen = new ArrayList<String>();
			for (Map<String, Object> map : resultMaps) {
				IFileSpec spec = handleFileReturn(map);
				if (spec.getOpStatus() == FileSpecOpStatus.VALID) {
					//resultList.add(spec);
					String file = spec.getAnnotatedPathString(PathType.DEPOT);
					if (file != null) {
						if (!filesSeen.contains(file)) {
							filesSeen.add(file);
							resultList.add(spec);
						}
					}
				} else {
					resultList.add(spec);
				}
			}
		}
		
		return resultList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#showServerConfiguration(java.lang.String, java.lang.String)
	 */
	public List<ServerConfigurationValue> showServerConfiguration(String serverName,
						String variableName) throws P4JavaException {
		final String SHOW_CMD = "show";
		String[] args = null;
		
		// Only one of serverName or variableName should be set:
		
		if (serverName != null) {
			args = new String[] {SHOW_CMD, serverName};
		} else if (variableName != null) {
			args = new String[] {SHOW_CMD, variableName};
		} else {
			args = new String[] {SHOW_CMD};
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
									CmdSpec.CONFIGURE,
									args,
									null);
		
		List<ServerConfigurationValue> configList = new ArrayList<ServerConfigurationValue>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorStr(map);
				if (errStr != null) {
					throw new RequestException(errStr, (String) map.get("code0"));
				}
				configList.add(new ServerConfigurationValue(map));
			}
		}
		
		return configList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#setServerConfigurationValue(java.lang.String, java.lang.String)
	 */
	public String setServerConfigurationValue(String name, String value) throws P4JavaException {
		if (name == null) {
			throw new NullPointerError("null config name passed to setServerConfigurationValue");
		}
		
		String args[] = null;
		
		if (value == null) {
			args = new String[] {"unset", name};
		} else {
			args = new String[] {"set", name + "=" + value};
		}
		
		String retVal = null;
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
				CmdSpec.CONFIGURE,
				args,
				null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {				
				if (map != null) {
					String str = this.getErrorOrInfoStr(map);
					if (str == null) {
						// Handling the new message format for Perforce server
						// version 2011.1; also maintain backward compatibility.
						if (map.containsKey("Name")) {
							if (map.containsKey("Action") && map.get("Action") != null) {
								String action = (String)map.get("Action");
								if (action.equalsIgnoreCase("set")) {
									str = "For server '%serverName%', configuration variable '%variableName%' set to '%variableValue%'";
								} else if (action.equalsIgnoreCase("unset")) {
									str = "For server '%serverName%', configuration variable '%variableName%' removed.";
								}
								if (str != null) {
									str = str.replaceAll("%serverName%", (String)map.get("ServerName"));
									str = str.replaceAll("%variableName%", (String)map.get("Name"));
									str = str.replaceAll("%variableValue%", (String)map.get("Value"));
									str += "\n";
								}
							}
						}
					}
					if (str != null) {
						retVal = str;
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getFileSizes(java.util.List, com.perforce.p4java.option.server.GetFileSizesOptions)
	 */
	public List<IFileSize> getFileSizes(List<IFileSpec> fileSpecs, GetFileSizesOptions opts) throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.SIZES,
													Parameters.processParameters(opts, fileSpecs, this),
													null);

		List<IFileSize> fileSizesList = new ArrayList<IFileSize>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorOrInfoStr(map);
				
				if (errStr != null) {
					if (isAuthFail(errStr)) {
						throw new AccessException(errStr);
					} else {
						throw new RequestException(errStr, (String) map.get("code0"));
					}
				} else {
					try {
						fileSizesList.add(new FileSize(map));
					} catch (Exception exc) {
						Log.error("getFileSizes conversion error: " + exc.getLocalizedMessage());
						Log.exception(exc);
					}
				}
			}
		}
		
		return fileSizesList;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#journalWait(com.perforce.p4java.option.server.JournalWaitOptions)
	 */
	public void journalWait(JournalWaitOptions opts) throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.JOURNALWAIT,
													Parameters.processParameters(opts, this),
													null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				String errStr = getErrorStr(map);
				if (errStr != null) {
					throw new RequestException(errStr, (String) map.get("code0"));
				}
			}
		}
	}

	public boolean handleErrorStr(Map<String, Object> map)
			throws RequestException, AccessException {
		
		String errStr = getErrorStr(map);
		
		if (errStr != null) {
			if (isAuthFail(errStr)) {
				throw new AccessException(errStr);
			} else {
				throw new RequestException(errStr, (String) map.get("code0"));
			}
		}
		return false;
	}
	
	public IFileSpec handleFileReturn(Map<String, Object> map)
									throws AccessException, ConnectionException {
		return handleFileReturn(map, this.client);
	}
	
	public IFileSpec handleFileReturn(Map<String, Object> map, IClient client)
					throws AccessException, ConnectionException {
		if (map != null) {
			String errStr = handleFileErrorStr(map);
			if (errStr == null) {
				return new FileSpec(map, this, -1);
			} else {
				if (isInfoMessage(map)) {
					return new FileSpec(FileSpecOpStatus.INFO, errStr,
										(String) map.get("code0"));
				} else {
					return new FileSpec(FileSpecOpStatus.ERROR, errStr,
										(String) map.get("code0"));
				}
			}
		}
		return null;
	}
	
	public IFileSpec handleIntegrationFileReturn(Map<String, Object> map, IClient client)
							throws AccessException, ConnectionException {
		return handleIntegrationFileReturn(map, false);
	}
	
	public IFileSpec handleIntegrationFileReturn(Map<String, Object> map, boolean ignoreInfo)
							throws AccessException, ConnectionException {
		if (map != null) {
			String errStr = handleFileErrorStr(map);
			if (errStr == null) {
				return new FileSpec(map, this, -1);
			} else {
				if (isInfoMessage(map)) {
					if (ignoreInfo) {
						return new FileSpec(map, this, -1);
					} else {
						return new FileSpec(FileSpecOpStatus.INFO, errStr, (String) map.get("code0"));
					}
				} else {
					return new FileSpec(FileSpecOpStatus.ERROR, errStr,
													(String) map.get("code0"));
				}
			}
		}
		return null;
	}
	
	public String handleFileErrorStr(Map<String, Object> map)
			throws ConnectionException, AccessException {
		String errStr = getErrorOrInfoStr(map);
		
		if (errStr != null) {
			if (isAuthFail(errStr)) {
				throw new AccessException(errStr);
			} else {
				return errStr.trim();
			}
		}
		
		return null;
	}
	
	public static String guardNull(String str) {
		final String nullStr = "<null>";
		
		return (str == null ? nullStr : str);
	}
	
	public static String[] getPreferredPathArray(String[] preamble, List<IFileSpec> specList,
																		boolean annotate) {
		String[] pathArray = new String[
		                                (preamble == null ? 0 : preamble.length) +
		                                (specList == null ? 0 : specList.size())
		                               ];
		int i = 0;
		if (preamble != null) {
			for (String str : preamble) {
				pathArray[i++] = str;
			}
		}
		
		if (specList != null) {
			for (IFileSpec fSpec : specList) {
				if ((fSpec != null) && (fSpec.getOpStatus() == FileSpecOpStatus.VALID)) {
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
	
	public static String[] getPreferredPathArray(String[] preamble, List<IFileSpec> specList) {
		return getPreferredPathArray(preamble, specList, true);
	}
	
	public static String[] populatePathArray(String[] pathArray, int start,
							List<IFileSpec> fileSpecList) {
		if (pathArray == null) {
			return null;
		}
		if (fileSpecList == null) {
			return pathArray;
		}
		if (start < 0) {
			throw new P4JavaError("negative start index in populatePathArray: " + start);
		}
		if ((start > pathArray.length) || (pathArray.length < (start + fileSpecList.size()))) {
			throw new P4JavaError("pathArray too small in populatePathArray");
		}
		
		int i = start;
		for (IFileSpec fSpec : fileSpecList) {
			if ((fSpec != null) && (fSpec.getOpStatus() == FileSpecOpStatus.VALID)) {
				pathArray[i] = fSpec.getAnnotatedPreferredPathString();
			} else {
				pathArray[i] = null;
			}
			i++;
		}
		
		return pathArray;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object>[] execMapCmd(CmdSpec cmdSpec, String[] cmdArgs, Map<String, Object> inMap)
					throws ConnectionException, AccessException {
		List<Map<String, Object>> resultMaps = execMapCmdList(cmdSpec, cmdArgs, inMap);
		if (resultMaps != null) {
			return resultMaps.toArray(new HashMap[resultMaps.size()]);
		}

		return null;
	}

	public List<Map<String, Object>> execMapCmdList(CmdSpec cmdSpec, String[] cmdArgs, Map<String, Object> inMap)
			throws ConnectionException, AccessException {
		if (cmdSpec == null) {
			throw new NullPointerError("Null command spec in execMapCmd");
		}
		
		try {
			return this.execMapCmdList(cmdSpec.toString(), cmdArgs, inMap);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			// Currently "can't happen" or "shouldn't happen" -- thrown only by 
			return null;
		} catch (P4JavaException exc) {
			// Currently "can't happen" or "shouldn't happen" -- thrown only by 
			return null;
		}
	}

	public InputStream execStreamCmd(CmdSpec cmdSpec, String[] cmdArgs)
					throws ConnectionException, RequestException, AccessException {
		if (cmdSpec == null) {
			throw new NullPointerError("Null command spec in execMapCmd");
		}
		
		return this.execStreamCmd(cmdSpec.toString(), cmdArgs);
	}
	
	protected boolean isUnicode() {
		return (this.charsetName != null); // good enough for Government work...
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getErrorStr(java.util.Map)
	 */
	public String getErrorStr(Map<String, Object> map) {
		throw new UnimplementedError("called IOptionsServer.getErrorStr(map)");
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getErrorOrInfoStr(java.util.Map)
	 */
	public String getErrorOrInfoStr(Map<String, Object> map) {
		throw new UnimplementedError("called IOptionsServer.getErrorOrInfoStr(map)");
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getInfoStr(java.util.Map)
	 */
	public String getInfoStr(Map<String, Object> map) {
		throw new UnimplementedError("called IOptionsServer.getInfoStr(map)");
	}

	abstract public boolean isAuthFail(String errStr);
	abstract public boolean isLoginNotRequired(String msgStr);
	abstract public boolean isInfoMessage(Map<String, Object> map);
	protected abstract int getGenericCode(Map<String, Object> map);
	protected abstract int getSeverityCode(Map<String, Object> map);
	
	/**
	 * @see com.perforce.p4java.server.IServer#execMapCmd(java.lang.String, java.lang.String[], java.util.Map)
	 */
	abstract public Map<String, Object>[] execMapCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
									throws ConnectionException, AccessException, RequestException;
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#execMapCmdList(java.lang.String, java.lang.String[], java.util.Map)
	 */
	abstract public List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
									throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#execMapCmdList(java.lang.String, java.lang.String[], java.util.Map, com.perforce.p4java.server.callback.IFilterCallback)
	 */
	abstract public List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs, Map<String, Object> inMap, IFilterCallback filterCallback)
									throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IServer#execInputStringMapCmd(java.lang.String, java.lang.String[], java.lang.String)
	 */
	abstract public Map<String, Object>[] execInputStringMapCmd(String cmdName, String[] cmdArgs, String inString)
									throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#execInputStringMapCmdList(java.lang.String, java.lang.String[], java.lang.String)
	 */
	abstract public List<Map<String, Object>> execInputStringMapCmdList(String cmdName, String[] cmdArgs, String inString)
									throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#execInputStringMapCmdList(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IFilterCallback)
 	 */
	abstract public List<Map<String, Object>> execInputStringMapCmdList(String cmdName, String[] cmdArgs, String inString, IFilterCallback filterCallback)
									throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IServer#execQuietMapCmd(java.lang.String, java.lang.String[], java.util.Map)
	 */
	abstract public Map<String, Object>[] execQuietMapCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
									throws ConnectionException, RequestException, AccessException;
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#execQuietMapCmdList(java.lang.String, java.lang.String[], java.util.Map)
	 */
	abstract public List<Map<String, Object>> execQuietMapCmdList(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
									throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IServer#execStreamCmd(java.lang.String, java.lang.String[])
	 */
	abstract public InputStream execStreamCmd(String cmdName, String[] cmdArgs)
									throws ConnectionException, RequestException, AccessException;
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#execStreamCmd(java.lang.String, java.lang.String[], java.util.Map)
	 */
	abstract public InputStream execStreamCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
									throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#execInputStringStreamCmd(java.lang.String, java.lang.String)
	 */
	abstract public InputStream execInputStringStreamCmd(String cmdName, String[] cmdArgs, String inString)
									throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IServer#execQuietStreamCmd(java.lang.String, java.lang.String[])
	 */
	abstract public InputStream execQuietStreamCmd(String cmdName, String[] cmdArgs)
									throws ConnectionException, RequestException, AccessException;
	
	/**
	 * @see com.perforce.p4java.server.IServer#execStreamingMapCommand(java.lang.String, java.lang.String[], java.util.Map, com.perforce.p4java.server.callback.IStreamingCallback, int)
	 */
	abstract public void execStreamingMapCommand(String cmdName, String[] cmdArgs, Map<String, Object> inMap,
									IStreamingCallback callback, int key) throws P4JavaException;
	
	/**
	 * @see com.perforce.p4java.server.IServer#execInputStringStreamingMapComd(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IStreamingCallback, int)
	 * 
	 * @deprecated As of release 2013.1, replaced by {@link #execInputStringStreamingMapCmd(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IStreamingCallback, int)}
 	 */
	@Deprecated
	abstract public void execInputStringStreamingMapComd(String cmdName, String[] cmdArgs, String inString,
									IStreamingCallback callback, int key) throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#execInputStringStreamingMapCmd(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IStreamingCallback, int)
 	 */
	abstract public void execInputStringStreamingMapCmd(String cmdName, String[] cmdArgs, String inString,
									IStreamingCallback callback, int key) throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#setAuthTicket(java.lang.String, java.lang.String)
	 */
	abstract public void setAuthTicket(String userName, String authTicket);
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getAuthTicket(java.lang.String)
	 */
	abstract public String getAuthTicket(String userName);

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#setTicketsFilePath(java.lang.String)
	 */
	abstract public void setTicketsFilePath(String ticketsFilePath);
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getTicketsFilePath()
	 */
	abstract public String getTicketsFilePath();

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#setTrustFilePath(java.lang.String)
	 */
	abstract public void setTrustFilePath(String trustFilePath);
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getTrustFilePath()
	 */
	abstract public String getTrustFilePath();

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getTrust()
	 */
	abstract public String getTrust() throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#addTrust(com.perforce.p4java.option.server.TrustOptions)
	 */
	abstract public String addTrust(TrustOptions opts) throws P4JavaException;
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#addTrust(java.lang.String)
	 */
	abstract public String addTrust(String fingerprintValue) throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#addTrust(java.lang.String, com.perforce.p4java.option.server.TrustOptions)
	 */
	abstract public String addTrust(String fingerprintValue, TrustOptions opts) throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#removeTrust()
	 */
	abstract public String removeTrust() throws P4JavaException;
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#removeTrust(com.perforce.p4java.option.server.TrustOptions)
	 */
	abstract public String removeTrust(TrustOptions opts) throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getTrusts()
	 */
	abstract public List<Fingerprint> getTrusts() throws P4JavaException;

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getTrusts(com.perforce.p4java.option.server.TrustOptions)
	 */
	abstract public List<Fingerprint> getTrusts(TrustOptions opts) throws P4JavaException;

	/**
	 * Check if the server is secure (SSL) or not.
	 */
	protected boolean isSecure() {
		return this.secure;
	}
	
	/**
	 * Sets the server to secure (SSL) or non-secure mode.
	 */
	protected void setSecure(boolean secure) {
		this.secure = secure;
	}
	
	public String getClientName() {
		return this.clientName;
	}
	
	public String getWorkingDirectory() {
		if (this.usageOptions != null) {
			return this.usageOptions.getWorkingDirectory();
		} else {
			return null;
		}
	}

	public void setWorkingDirectory(String dirPath) {
		if (this.usageOptions != null) {
			this.usageOptions.setWorkingDirectory(dirPath);
		}
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	
	// Try to get the Perforce server version. This is likely to be the first time
	// actual connectivity is tested for the server...
	// Since this is called before we know much about the state or type of the
	// Perforce server, we do virtually no real error checking or recovery -- we
	// either get a suitable response and dig out the server version, or we just
	// leave things alone.
	//
	// NOTE: has the side effect of setting the server impl's serverVersion field.
	protected int getServerVersion() throws ConnectionException {
		
		if (this.serverVersion != UNKNOWN_SERVER_VERSION) {
			// We've already got the server version. This will fail
			// if the server changes underneath us, but that's life...
			return this.serverVersion;
		}

		try {
			this.serverInfo = getServerInfo();
			if (this.serverInfo != null) {
				if (this.serverInfo.getServerAddress() != null) {
					this.serverAddress = this.serverInfo.getServerAddress();
				}
				if (this.serverInfo.getServerVersion() != null) {
					this.serverVersion = parseVersionString(this.serverInfo.getServerVersion());
					return this.serverVersion;
				}
			}
		} catch (Exception exc) {
			Log.exception(exc);
			throw new ConnectionException(exc.getLocalizedMessage(), exc);
		}
		return UNKNOWN_SERVER_VERSION;
	}
	
	/**
	 * Get the server address entry from the p4 info.
	 * 
	 * @return - server address or null if error
	 */
	protected String getInfoServerAddress() {
		if (this.serverAddress != null) {
			// We've already got the server version. This will fail
			// if the server changes underneath us, but that's life...
			return this.serverAddress;
		}

		try {
			this.serverInfo = getServerInfo();
			if (this.serverInfo != null) {
				if (this.serverInfo.getServerAddress() != null) {
					this.serverAddress = this.serverInfo.getServerAddress();
				}
				if (this.serverInfo.getServerVersion() != null) {
					this.serverVersion = parseVersionString(this.serverInfo.getServerVersion());
				}
			}
		} catch (Exception exc) {
			Log.exception(exc);
		}
		
		return this.serverAddress;
	}
	
	/**
	 * Return the major version number (e.g. 20081) from the passed-in
	 * complete version string. Instead of using regex or anything too complex
	 * we just keep splitting the string and recombining; this could be optimised
	 * or flexibilised fairly easily on one of those long rainy days... (HR).
	 */
	
	protected int parseVersionString(String versionString) {
		
		// Format: P4D/LINUX26X86/2007.3/142194 (2007/12/17),
		// but with minor variants possible due to internal server builds,
		// e.g. 2005.2.r05.2_nightly. But all we want is the 2007.3 turned
		// into an int like 20073...
		
		if (versionString != null) {
			String[] subStrings = versionString.split("/");
			
			if (subStrings.length >= 3) {
				String candidate = subStrings[2];
				String[] candParts = candidate.split("\\.");
				if (candParts.length >= 2) {
					try {
						return new Integer(candParts[0] + candParts[1]);
					} catch (NumberFormatException nfe) {
						Log.error(
								"Unexpected exception in P4CmdServerImpl.parseVersionString: " + nfe);
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
        return Math.abs(this.rand.nextInt(Integer.MAX_VALUE));
    }
	
	/**
	 * Return true if the JVM indicates that we're running
	 * on a Windows platform. Not entirely reliable, but good
	 * enough for our purposes.
	 */
	
	public static boolean isRunningOnWindows() {
		return runningOnWindows;
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

	protected List<IChangelist> processInterchangeMaps(List<Map<String, Object>> resultMaps, boolean showFiles)
									throws ConnectionException, AccessException, RequestException {
		List<IChangelist> interchangeList = new ArrayList<IChangelist>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					// map is either an error (in which case we do what we'd
					// normally do with an error), or it's an "error" -- i.e. it's
					// telling us there's no interchanges -- in which case we return
					// an empty list,
					// or it's a change summary (in which case we return a full changelist
					// constructed from the summary, even though we don't get the full
					// changelist info back),
					// or (if the showFiles option was set) it's a change summary with a
					// nested set of depot file specs inside the same map, in
					// which case we pick the files off as best we can and then associate
					// them with the changelist constructed as above.
					
					String errStr = handleFileErrorStr(map);
					
					if (errStr != null) {
						// What we're doing here is weeding out the "all revision(s)
						// already integrated" non-error error...
						// Note that the code here may be fragile in the face of
						// server-side changes to error messages and code changes.
						
						if ((getGenericCode(map) != 17) && (getSeverityCode(map) != 2) &&
								!errStr.contains("all revision(s) already integrated")) {
							throw new RequestException(errStr, (String) map.get("code0"));
						}
					} else {
						Changelist changelist = new Changelist(new ChangelistSummary(map, true, this), this, false);
						interchangeList.add(changelist);
						if (showFiles) {
							List<IFileSpec> fileSpecs = new ArrayList<IFileSpec>();
							
							int i = 0;
							final String depotKey = "depotFile";
							while (map.get(depotKey + i) != null) {
								FileSpec fileSpec = new FileSpec(map, this, i);
								fileSpec.setChangelistId(changelist.getId());
								fileSpecs.add(fileSpec);
								i++;
							}
							
							changelist.setFileSpecs(fileSpecs);
						}
					}
				}
			}
		}
		return interchangeList;
	}
	
	/**
	 * Special case handling of the "-p" flag for the "p4 login" command.
	 * The -p flag displays the ticket, but does not store it on the client
	 * machine.
	 */
	protected boolean isDontWriteTicket(String cmd, String[] cmdArgs) {
		if (cmd != null) {
			if (cmd.equalsIgnoreCase(CmdSpec.LOGIN.toString())) {
				if (cmdArgs != null) {
					for (String arg : cmdArgs) {
						if (arg != null) {
							if (arg.equals("-p")) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getMatchingLines(java.util.List, java.lang.String, com.perforce.p4java.option.server.MatchingLinesOptions)
	 */
	public List<IFileLineMatch> getMatchingLines(List<IFileSpec> fileSpecs, String pattern,
			MatchingLinesOptions options) throws P4JavaException {
		return getMatchingLines(fileSpecs, pattern, null, options);
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getMatchingLines(java.util.List, java.lang.String, java.util.List, com.perforce.p4java.option.server.MatchingLinesOptions)
	 */
	public List<IFileLineMatch> getMatchingLines(List<IFileSpec> fileSpecs, String pattern,
			List<String> infoLines, MatchingLinesOptions options) throws P4JavaException {
		if (fileSpecs == null) {
			throw new NullPointerError(
				"Null file specification list passed to IOptionsServer.getMatchingLines");
		}
		
		if (pattern == null) {
			throw new NullPointerError(
				"Null pattern string passed to IOptionsServer.getMatchingLines");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
								CmdSpec.GREP,
								Parameters.processParameters(options, fileSpecs, "-e" + pattern, this),
								null);

		if (resultMaps == null) {
			throw new P4JavaError("Null resultMaps in Server.getMatchingLines call");
		}

		List<IFileLineMatch> specList = new ArrayList<IFileLineMatch>();

		for (Map<String, Object> map : resultMaps) {
			String message = getErrorStr(map);

			if (message != null) {
				throw new RequestException(message, (String) map.get("code0"));
			} else {
				message = getErrorOrInfoStr(map);
				if (message == null) {
					specList.add(new FileLineMatch(map));
				} else if (infoLines != null) {
					infoLines.add(message);
				}
			}
		}

		return specList;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#obliterateFiles(java.util.List, com.perforce.p4java.option.server.ObliterateFilesOptions)
	 */
	public List<IObliterateResult> obliterateFiles(List<IFileSpec> fileSpecs,
						ObliterateFilesOptions opts) throws P4JavaException {
		if (fileSpecs == null) {
			throw new NullPointerError("null fileSpecs passed to Server.obliterateFiles()");
		}

		List<IObliterateResult> obliterateResults = new ArrayList<IObliterateResult>();

		List<Map<String, Object>> resultMaps = execMapCmdList(
									CmdSpec.OBLITERATE,
									Parameters.processParameters(opts, fileSpecs, this),
									null);

		// The "obliterate" command can take multiple filespecs.
		// Each filespec has its own result in the results map.
		// Each result has summary keys: "revisionRecDeleted", etc.
		// The summary keys indicate boundary between results. 
		// Additionally, there might be a "reportOnly" key at the end.
		// Note: some results might not have "purgeFile" and "purgeRev" values.
		if (resultMaps != null) {
			// Check for the "reportOnly" key in the last map entry.
			// We only check if there are two or more elements.
			boolean reportOnly = false;
			if (resultMaps.size() > 1) {
				Map<String, Object> lastMap = resultMaps.get(resultMaps.size() -1);
				if (lastMap != null) {
					if (lastMap.containsKey("reportOnly")) {
						reportOnly = true;
					}
				}
			}
			try {
				IObliterateResult result = null;
				List<IFileSpec> fsList = new ArrayList<IFileSpec>();
				for (Map<String, Object> map : resultMaps) {
					String errStr = handleFileErrorStr(map);
					FileSpec fs = null;
					if (errStr == null) {
						if (map.containsKey("purgeFile")) {
							fs = new FileSpec();
							fs.setDepotPath((String) map.get("purgeFile"));
							fs.setEndRevision(new Integer((String) map.get("purgeRev")));
							fsList.add(fs);
						} else if (map.containsKey("revisionRecDeleted")) {
							result = new ObliterateResult(
									fsList,
									new Integer((String) map.get("integrationRecAdded")),
									new Integer((String) map.get("labelRecDeleted")),
									new Integer((String) map.get("clientRecDeleted")),
									new Integer((String) map.get("integrationRecDeleted")),
									new Integer((String) map.get("workingRecDeleted")),
									new Integer((String) map.get("revisionRecDeleted")),
									reportOnly);
							obliterateResults.add(result);
							// Create a new list for the next result
							fsList = new ArrayList<IFileSpec>();
						}
					} else {
						if (isInfoMessage(map)) {
							fs = new FileSpec(FileSpecOpStatus.INFO, errStr);
						} else {
							fs = new FileSpec(FileSpecOpStatus.ERROR,	errStr);
						}
						fsList.add(fs);
						result = new ObliterateResult(fsList, 0, 0, 0, 0, 0, 0, reportOnly);
						obliterateResults.add(result);
					}
				}
			} catch (Exception exc) {
				Log.error("Unexpected exception in ObliterateFileSpec constructor"
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}
		}

		return obliterateResults;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getStreams(java.util.List streamPaths, com.perforce.p4java.option.server.GetStreamsOptions)
	 */
	public List<IStreamSummary> getStreams(List<String> streamPaths, GetStreamsOptions opts)
										throws P4JavaException {
		List<IStreamSummary> streamList = new ArrayList<IStreamSummary>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.STREAMS,
													Parameters.processParameters(
															opts,
															null,
															streamPaths != null ? streamPaths.toArray(new String[streamPaths.size()]) : null,
															this),
														null);
		if (resultMaps != null) {
			for (Map<String, Object> streamMap : resultMaps) {
				String errStr = handleFileErrorStr(streamMap);
				if (errStr != null) {
					Log.error(errStr);
				} else {
					streamList.add(new StreamSummary(streamMap, true));
				}
			}
		}
		
		return streamList;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#createStream(com.perforce.p4java.core.IStream)
	 */
	public String createStream(IStream stream) throws P4JavaException {
		if (stream == null) {
			throw new NullPointerError("null stream passed to createStream method");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.STREAM,
												new String[] {"-i"},
												InputMapper.map(stream));
		String retVal = null;
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getStream(java.lang.String)
	 */
	public IStream getStream(String streamPath) throws P4JavaException {
		return getStream(streamPath, new GetStreamOptions());
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getStream(java.lang.String, com.perforce.p4java.option.server.GetStreamOptions)
	 */
	public IStream getStream(String streamPath, GetStreamOptions opts) throws P4JavaException {
		if (streamPath == null) {
			throw new NullPointerError("null stream name to getStream method");
		}
		IStream stream = null;
		List<Map<String, Object>> resultMaps = execMapCmdList(
													CmdSpec.STREAM,
													Parameters.processParameters(
															opts,
															null,
															new String[] {"-o", streamPath},
															this),
													null);
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {					
					handleErrorStr(map);
					if (!isInfoMessage(map)) {
						stream = new Stream(map, this);
					}
				}
			}
		}
		return stream;
	}

	/**
	 * @see com.perforce.p4java.server.IOptioinsServer#updateStream(com.perforce.p4java.core.IStream, com.perforce.p4java.option.server.StreamOptions)
	 */
	public String updateStream(IStream stream, StreamOptions opts) throws P4JavaException {
		if (stream == null) {
			throw new NullPointerError("Null stream in IOptioinsServer.updateStream method");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
												CmdSpec.STREAM,
												Parameters.processParameters(opts, null, "-i", this),
												InputMapper.map(stream));
	
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#deleteStream(java.lang.String, com.perforce.p4java.option.server.StreamOptions)
	 */
	public String deleteStream(String streamPath, StreamOptions opts) throws P4JavaException {
		if (streamPath == null) {
			throw new NullPointerError("Null stream path passed to IOptionsServer.deleteStream");
		}
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.STREAM,
											Parameters.processParameters(opts, null,
													new String[] {"-d", streamPath}, this),
											null);
		String retStr = null;
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retStr == null) {
						retStr = getInfoStr(map);
					} else {
						retStr += "\n" + getInfoStr(map);
					}
				}
			}
		} else {
			Log.warn("null return map array in Server.deleteStream");
		}
		
		return retStr;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getStreamIntegrationStatus(java.lang.String, com.perforce.p4java.option.server.StreamIntegrationStatusOptions)
	 */
	public IStreamIntegrationStatus getStreamIntegrationStatus(String stream, StreamIntegrationStatusOptions opts)
								throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.ISTAT,
											Parameters.processParameters(opts, null,
													new String[] {stream}, this),
											null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					handleErrorStr(map);
					return new StreamIntegrationStatus(map);
				}
			}
		}

		return null;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getLogTail(com.perforce.p4java.option.server.LogTailOptions)
	 */
	public ILogTail getLogTail(LogTailOptions opts) throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(
											CmdSpec.LOGTAIL,
											Parameters.processParameters(
													opts, this),
											null);
		String logFile = null;
		long offset = -1;
		List<String> data = new ArrayList<String>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					String errStr = getErrorStr(map);
					if (errStr != null) {
						throw new RequestException(errStr, (String) map.get("code0"));
					} else {
						try {
							if (map.containsKey("file")) {
								logFile = (String) map.get("file");
							}
							if (map.containsKey("data")) {
								data.add((String) map.get("data"));
							}
							if (map.containsKey("offset")) {
								offset = new Long((String) map.get("offset"));
							}
						} catch (Throwable thr) {
							Log.exception(thr);
						}
					}
				}
			}
		}

		if (logFile != null && data.size() > 0 && offset > -1) {
			return new LogTail(logFile, offset, data);
		}
		
		return null;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#duplicateRevisions(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.option.DuplicateRevisionsOptions)
	 */
	public List<IFileSpec> duplicateRevisions(IFileSpec fromFile, IFileSpec toFile,
			DuplicateRevisionsOptions opts) throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(
								CmdSpec.DUPLICATE,
								Parameters.processParameters(
										opts, fromFile, toFile, null, this),
								null);
		
		List<IFileSpec> integList = new ArrayList<IFileSpec>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				integList.add(handleIntegrationFileReturn(map, null));
			}
		}
		
		return integList;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#unload(com.perforce.p4java.option.server.UnloadOptions)
	 */
	public String unload(UnloadOptions opts) throws P4JavaException {

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.UNLOAD,
				Parameters.processParameters(opts, this), null);

		String retVal = null;

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in Server.unload");
		}

		return retVal;

	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#reload(com.perforce.p4java.option.server.ReloadOptions)
	 */
	public String reload(ReloadOptions opts) throws P4JavaException {

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.RELOAD,
				Parameters.processParameters(opts, this), null);

		String retVal = null;

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (retVal == null) {
					retVal = getInfoStr(map);
				} else {
					retVal += "\n" + getInfoStr(map);
				}
			}
		} else {
			Log.warn("null return map array in Server.unload");
		}

		return retVal;

	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getTriggersTable()
	 */
	public InputStream getTriggersTable() throws P4JavaException {

		return this.execStreamCmd(CmdSpec.TRIGGERS, new String[] {"-o"});
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#getTriggerEntries()
	 */
	public List<ITriggerEntry> getTriggerEntries() throws P4JavaException {
		List<ITriggerEntry> triggersList = new ArrayList<ITriggerEntry>();
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.TRIGGERS,
										new String[] {"-o"},
										null);
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				if (map != null) {
					handleErrorStr(map);
					for (int i = 0; ; i++) {
						String entry = (String) map.get(MapKeys.TRIGGERS_KEY + i);
						if (entry != null) {
							triggersList.add(new TriggerEntry((String)entry, i));
						} else {
							break;
						}
					}
				}
			}
		}
		
		return triggersList;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#createTriggerEntries(java.util.List)
	 */
	public String createTriggerEntries(List<ITriggerEntry> entryList) throws P4JavaException {
		if (entryList == null) {
			throw new NullPointerError("Null new protection entry list in createTriggerEntries method");
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.TRIGGERS, new String[] {"-i"},
														InputMapper.map(new TriggersTable(entryList)));
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}

	/**
	 * @see com.perforce.p4java.server.IOptionsServer#updateTriggerEntries(java.util.List)
	 */
	public String updateTriggerEntries(List<ITriggerEntry> entryList) throws P4JavaException {
		if (entryList == null) {
			throw new NullPointerError("Null new trigger entry list in updateTriggerEntries method");
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.TRIGGERS, new String[] {"-i"},
														InputMapper.map(new TriggersTable(entryList)));
		String retVal = null;
		
		if (resultMaps != null) {			
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal == null) {
						retVal = getInfoStr(map);
					} else {
						retVal += " \n" + getInfoStr(map);
					}
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.server.IOptionsServer#verifyFiles(java.util.List, com.perforce.p4java.option.server.VerifyFilesOptions)
	 */
	public List<IExtendedFileSpec> verifyFiles(List<IFileSpec> fileSpecs, VerifyFilesOptions opts)
									throws P4JavaException {
		
		List<Map<String, Object>> resultMaps = execMapCmdList(CmdSpec.VERIFY,
				Parameters.processParameters(opts, fileSpecs, this), null);

		List<IExtendedFileSpec> specList = new ArrayList<IExtendedFileSpec>();
		
		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				
				String errStr = handleFileErrorStr(map);
				ExtendedFileSpec eSpec = null;
				if (errStr == null) {
					if (map.containsKey("depotFile") && !map.containsKey("desc")) {
						eSpec = new ExtendedFileSpec(map, this, -1);
					}
				} else {
					if (isInfoMessage(map)) {
						eSpec = new ExtendedFileSpec(FileSpecOpStatus.INFO, errStr);
					} else {
						eSpec = new ExtendedFileSpec(FileSpecOpStatus.ERROR, errStr);
					}
				}
				if (eSpec != null) {
					specList.add(eSpec);
				}
			}
		}
		return specList;
	}
	
	public ISSOCallback getSSOCallback() {
		return this.ssoCallback;
	}
	
	public String getSSOKey() {
		return this.ssoKey;
	}

	public UsageOptions getUsageOptions() {
		return usageOptions;
	}

	public Server setUsageOptions(UsageOptions usageOptions) {
		this.usageOptions = usageOptions;
		return this;
	}

	public boolean isNonCheckedSyncs() {
		return nonCheckedSyncs;
	}

	public void setNonCheckedSyncs(boolean nonCheckedSyncs) {
		this.nonCheckedSyncs = nonCheckedSyncs;
	}
	
	public boolean isEnableTracking() {
		return enableTracking;
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

	public String getIgnoreFileName() {
		return ignoreFileName;
	}
	
	public void setIgnoreFileName(String ignoreFileName) {
		this.ignoreFileName = ignoreFileName;
	}
}
