/**
 * 
 */
package com.perforce.p4java;

/**
 * Defines keys and default values for common and useful P4Java-wide
 * properties. Particularly useful for initial properties passed in
 * through the server factory to define things like calling-program
 * name and version, but note that (as individually noted below) some
 * properties defined here must be defined at load time through the
 * system properties or they will have no effect.<p>
 * 
 * Unless otherwise noted in the individual definitions below,
 * properties defined here apply to all protocol implementations.<p>
 * 
 * Unless noted otherwise below, most properties can have an optional short
 * form which is typically just the full form without the cumbersome
 * com.perforce.p4java (etc.) prefix; use of the short form is particularly
 * convenient for passing in properties through the server factory url
 * mechanism, but you have to be careful that there are no system or
 * environment properties with a conflicting name.<p>
 */

public class PropertyDefs {
	
	/**
	 * The standard default P4Java server properties key prefix.
	 */
	public static final String P4JAVA_PROP_KEY_PREFIX = "com.perforce.p4java.";
	
	/**
	 * Short form program name key.
	 */
	public static final String PROG_NAME_KEY_SHORTFORM = "programName";
	
	/**
	 * Short form program version key.
	 */
	public static final String PROG_VERSION_KEY_SHORTFORM = "programVersion";

	/**
	 * Properties key for the calling-program version. Usage is intended to be similar
	 * to the p4 command lines -zversion flag. If no corresponding property is
	 * given, the value defined by PROG_VERSION_DEFAULT below is used.
	 */
	public static final String PROG_VERSION_KEY = P4JAVA_PROP_KEY_PREFIX + PROG_VERSION_KEY_SHORTFORM;
	
	/**
	 * Default calling-program version to use if no calling program property is
	 * set with the PROG_VERSION_KEY key, above.
	 */
	public static final String PROG_VERSION_DEFAULT = Metadata.getP4JVersionString();
	
	/**
	 * Properties key for the calling-program name. Usage is intended to be similar
	 * to the p4 command lines -zprog flag. If no corresponding property is
	 * given, the value defined by PROG_NAME_DEFAULT below is used.
	 */
	
	public static final String PROG_NAME_KEY = P4JAVA_PROP_KEY_PREFIX + PROG_NAME_KEY_SHORTFORM;
	
	/**
	 * Default calling-program name to use if no calling program property is
	 * set with the PROG_NAME_KEY key, above.
	 */
	
	public static final String PROG_NAME_DEFAULT = "Unknown P4Java program";
	
	/**
	 * Property key for passing in a suitable client name to be used when we
	 * don't actually have (or want) a Perforce client associated with a Perforce
	 * server connection. If no such property is given, the value used defaults
	 * to CLIENT_UNSET_NAME_DEFAULT, below.
	 */
	public static final String CLIENT_UNSET_NAME_KEY = P4JAVA_PROP_KEY_PREFIX + "unsetClientName";
	
	/**
	 * Default value to be used for the unset client name (see the comments for
	 * CLIENT_UNSET_NAME_KEY, above) when no associated property is set.
	 */
	// In order to prevent default clients from getting created the server team
	// is looking to make ____CLIENT_UNSET____ a reserved workspace name. That
	// way all of our clients can use the same fake client name and know that it
	// will never be created accidentally.
	// See job078085
	public static final String CLIENT_UNSET_NAME_DEFAULT = "_____CLIENT_UNSET_____";
	
	/**
	 * Property key for passing in a suitable user name to be used when we
	 * don't actually have (or want) a Perforce user associated with a Perforce
	 * server connection. If no such property is given, the value used defaults
	 * to USER_UNSET_NAME_DEFAULT, below.
	 */
	public static final String USER_UNSET_NAME_KEY = P4JAVA_PROP_KEY_PREFIX + "unsetUserName";
	
	/**
	 * Default value to be used for the unset user name (see the comments for
	 * USER_UNSET_NAME_KEY, above) when no associated property is set.
	 */
	public static final String USER_UNSET_NAME_DEFAULT = "nouser";
	
	/**
	 * Property name key for the P4Java API's temporary directory. Unless
	 * otherwise noted, this directory will be used for temporary files, and,
	 * unless set by this property, it will default to whatever's in the
	 * system java.io.tmpdir property.<p>
	 * 
	 * Note that sync operations will sync to temporary files in the enclosing
	 * directory of the target file rather than to the default tmp directory if
	 * this property is not set; this is so that cross-device copies are not
	 * needed at the end of each file sync from tmp to target. If set, however,
	 * this property's value will be used for sync ops as well.
	 */
	
	public static final String P4JAVA_TMP_DIR_KEY = P4JAVA_PROP_KEY_PREFIX + "tmpDir";
	
	/**
	 * Short form user name key.
	 */
	public static final String USER_NAME_KEY_SHORTFORM = "userName";
	
	/**
	 * Property key for a Perforce user name set though the P4Java properties
	 * mechanism. If set, an IServer returned from the server factory will
	 * have its user name set to this value.
	 */
	public static final String USER_NAME_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ USER_NAME_KEY_SHORTFORM;
	
	/**
	 * Short form password key.
	 */
	public static final String PASSWORD_KEY_SHORTFORM = "password";
	
	/**
	 * Property key for a Perforce password set though the P4Java properties
	 * mechanism. If set, an IServer returned from the server factory will
	 * have its password set to this value.
	 */
	public static final String PASSWORD_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ PASSWORD_KEY_SHORTFORM;
	
	
	/**
	 * Short form client name key.
	 */
	public static final String CLIENT_NAME_KEY_SHORTFORM = "clientName";
	
	/**
	 * Property key for a Perforce client name set though the P4Java properties
	 * mechanism. If set, an IServer returned from the server factory will
	 * have its client name set to this value.
	 */
	public static final String CLIENT_NAME_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ CLIENT_NAME_KEY_SHORTFORM;
	
	/**
	 * Short form autoconnect key.
	 */
	public static final String AUTO_CONNECT_KEY_SHORTFORM = "autoConnect";
	
	/**
	 * If AUTO_CONNECT_KEY (or its short form) is set, attempt to
	 * connect with the client name, if the clientName is also set.
	 * Will be done after any auto logins (see below).
	 */
	public static final String AUTO_CONNECT_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ AUTO_CONNECT_KEY_SHORTFORM;
	
	/**
	 * Short form auto login key.
	 */
	public static final String AUTO_LOGIN_KEY_SHORTFORM = "autoLogin";
	
	/**
	 * If AUTO_LOGIN_KEY (or its short form) is set, attempt to perform
	 * a login on connect(). Will only actually try the login if
	 * userName and password are also set through the properties menachnism.
	 */
	
	public static final String AUTO_LOGIN_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ AUTO_LOGIN_KEY_SHORTFORM;
	
	/**
	 * If this property is set, attempt to use this path as the p4tickets file.
	 */
	public static final String TICKET_PATH_KEY_SHORT_FORM = "ticketPath";
	
	/**
	 * If this property is set, attempt to use this path as the p4tickets file.
	 */
	public static final String TICKET_PATH_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ TICKET_PATH_KEY_SHORT_FORM;
	
	/**
	 * If DEFAULT_CHARSET_KEY is set in the Java system properties, it defines the
	 * name of the charset used to convert strings to and from the Perforce server
	 * if that server is <i>NOT</i> in Unicode mode. Note that this property is
	 * fundamental and <i>must</i> be set early on, i.e. in the System properties
	 * at P4Java startup / load time.<p>
	 * 
	 * If this property is not set at P4Java load time, the default charset name
	 * is the current JVM default charset name if not null.<p>
	 * 
	 * Note that this value has no effect whatever when running against a non-Unicode
	 * Perforce server.<p>
	 * 
	 * Note also that this property has no short form.
	 */
	public static final String DEFAULT_CHARSET_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
																			+ "defaultCharset";
	
	/**
	 * Short form of the WRITE_IN_PLACE_KEY, below.
	 */
	public static final String WRITE_IN_PLACE_SHORT_FORM = "writeInPlace";
	
	/**
	 * If WRITE_IN_PLACE_KEY is true, certain operations listed below may
	 * write file contents from the Perforce server directly to the target
	 * client file rather than to a temporary file (which is then renamed to
	 * the target file).<p>
	 * 
	 * In general, this property should not be used unless you're
	 * on a Windows box and seeing performance issues with sync operations;
	 * even then it's probably best to consult with a Perforce support
	 * person before trying it, as this property has been introduced as
	 * a workaround to a known Java bug with file renaming on JDK 6 and
	 * earlier VMs on Windows boxes, and may not be an optimal solution
	 * for everyone. Known side-effects (besides performance) may include
	 * partial file content syncs in cases where network errors occur,
	 * but this is very rare and will not result in server-side data
	 * loss or corruption (in fact it's yet to be observed at all within
	 * Perforce).<p>
	 * 
	 * Currently only the sync command honors this property.
	 */
	public static final String WRITE_IN_PLACE_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ WRITE_IN_PLACE_SHORT_FORM;
	
	/**
	 * Short form of the NON_CHECKED_SYNC property (below).
	 * 
	 * @since 2011.1
	 */
	public static final String NON_CHECKED_SYNC_SHORT_FORM = "nonCheckedSync";
	
	/**
	 * If the NON_CHECKED_SYNC property is set (to any value), syncs and certain
	 * other operations (see below) against 2010.2 or later Perforce servers will not
	 * have integrity checks performed during the operations. The default is to
	 * use integrity checks on these operations with 2010.2 or later servers (these
	 * additional checks are not done for earlier server versions).<p>
	 * 
	 * By default, for 2010.2 and later servers, the integrity checks are performed on
	 * the client for sync, revert, unshelve, and integ operations, and on the server
	 * for submit and shelve. Note that this feature can also be disabled on the server
	 * side (see the Perforce admin command for details).
	 * 
	 * @since 2011.1
	 */
	public static final String NON_CHECKED_SYNC = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ NON_CHECKED_SYNC_SHORT_FORM;

	/**
	 * Short form of the ENABLE_TRACKING property (below).
	 * 
	 * @since 2012.1
	 */
	public static final String ENABLE_TRACKING_SHORT_FORM = "enableTracking";
	
	/**
	 * If the ENABLE_TRACKING property is set (to any value), the server
	 * performance tracking information will be returned as part of the result
	 * for applicable commands. You can see what tables a command is accessing,
	 * and implicitly locking. In addition to the command's usual output, the
	 * "track" information includes the table name and the type of locks
	 * obtained on that table. <p>
	 * 
	 * Note that using "-Ztrack" users will have to handle tracking information
	 * return from the lower level "raw" IServer.exec* methods.
	 * 
	 * @since 2012.1
	 */
	public static final String ENABLE_TRACKING = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ ENABLE_TRACKING_SHORT_FORM;

	/**
	 * Short form of the ENABLE_STREAMS property (below).
	 *
	 * @since 2017.1
	 */
	public static final String ENABLE_STREAMS_SHORT_FORM = "enableStreams";

	/**
	 * If the ENABLE_STREAMS property is set (to any value), the server
	 * enable streams commands.
	 *
	 * @since 2017.1
	 */
	public static final String ENABLE_STREAMS = Metadata.P4JAVA_PROPS_KEY_PREFIX
			+ ENABLE_STREAMS_SHORT_FORM;

	/**
	 * Short form of the ENABLE_ANDMAPS property (below).
	 *
	 * @since 2017.1
	 */
	public static final String ENABLE_ANDMAPS_SHORT_FORM = "expandAndmaps";

	/**
	 * If the ENABLE_ANDMAPS property is set (to any value), the server
	 * expands and maps.
	 *
	 * @since 2017.1
	 */
	public static final String ENABLE_ANDMAPS = Metadata.P4JAVA_PROPS_KEY_PREFIX
			+ ENABLE_ANDMAPS_SHORT_FORM;

	/**
	 * Short form of the ENABLE_GRAPH property (below).
	 *
	 * @since 2017.1
	 */
	public static final String ENABLE_GRAPH_SHORT_FORM = "enableGraph";

	/**
	 * If the ENABLE_GRAPH property is set (to any value), the server
	 * will reply with graph data as required.
	 *
	 * @since 2017.1
	 */
	public static final String ENABLE_GRAPH = Metadata.P4JAVA_PROPS_KEY_PREFIX
			+ ENABLE_GRAPH_SHORT_FORM;

	/**
	 * Short form of the FILESYS_UTF8BOM property (below).
	 *
	 * @since 2017.2
	 */
	public static final String FILESYS_UTF8BOM_SHORT_FORM = "filesys.utf8bom";

	/**
	 * FILESYS_UTF8BOM Set to 0 to prevent writing utf8 files BOM, Set to 1 to
	 * write utf8 files with a BOM, Set to 2 to write utf8 BOM only on Windows.
	 *
	 * @since 2017.2
	 */
	public static final String FILESYS_UTF8BOM = Metadata.P4JAVA_PROPS_KEY_PREFIX
			+ FILESYS_UTF8BOM_SHORT_FORM;

	/**
	 * Short form of the ENABLE_PROGRESS property (below).
	 * 
	 * @since 2012.3
	 */
	public static final String ENABLE_PROGRESS_SHORT_FORM = "enableProgress";
	
	/**
	 * If the ENABLE_PROGRESS property is set (to any value), a variable
	 * "progress" will be set to 1 to indicate that the server should send
	 * progress messages to the client if they are available for that command.
	 * <p>
	 * 
	 * Note that the progress indicator (p4 -I <command>) flag makes sense to be
	 * used with P4Java's lower level "raw" IServer.execStreamingMapCommand()
	 * method. This streaming method takes a callback handler and continuously
	 * report progress (in a result map) during the lifetime of a command.
	 * 
	 * @since 2012.3
	 */
	public static final String ENABLE_PROGRESS = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ ENABLE_PROGRESS_SHORT_FORM;
	
	/**
	 * Short form of the QUIET_MODE property (below).
	 * 
	 * @since 2013.1
	 */
	public static final String QUIET_MODE_SHORT_FORM = "quietMode";
	
	/**
	 * If the QUIET_MODE property is set (to any value), suppress ALL info-level
	 * output.
	 * 
	 * @since 2013.1
	 */
	public static final String QUIET_MODE = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ QUIET_MODE_SHORT_FORM;

	/**
	 * If this property is set, attempt to use this ignore file name.
	 * 
	 * @since 2012.1
	 */
	public static final String IGNORE_FILE_NAME_KEY_SHORT_FORM = "ignoreFileName";

	/**
	 * If this property is set, attempt to use this ignore file name.
	 * 
	 * @since 2012.1
	 */
	public static final String IGNORE_FILE_NAME_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ IGNORE_FILE_NAME_KEY_SHORT_FORM;

	/**
	 * If this property is set, attempt to use this path as the p4trust file.
	 * 
	 * @since 2012.1
	 */
	public static final String TRUST_PATH_KEY_SHORT_FORM = "trustPath";
	
	/**
	 * If this property is set, attempt to use this path as the p4trust file.
	 * 
	 * @since 2012.1
	 */
	public static final String TRUST_PATH_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ TRUST_PATH_KEY_SHORT_FORM;

	/**
	 * If this property is set (to any value), attempt to use memory instead of
	 * file to store auth tickets and fingerprints.
	 * 
	 * @since 2012.3
	 */
	public static final String USE_AUTH_MEMORY_STORE_KEY_SHORT_FORM = "useAuthMemoryStore";
	
	/**
	 * If this property is set (to any value), attempt to use memory instead of
	 * file to store auth tickets and fingerprints.
	 * 
	 * @since 2012.3
	 */
	public static final String USE_AUTH_MEMORY_STORE_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ USE_AUTH_MEMORY_STORE_KEY_SHORT_FORM;
	
	/**
	 * Short form of the UNICODE_MAPPING property (below).
	 * 
	 * @since 2012.3
	 */
	public static final String UNICODE_MAPPING_SHORT_FORM = "unicodeMapping";
	
	/**
	 * If the UNICODE_MAPPING property is set (to any value), attempt to apply
	 * Perforce specific updates to character mappings in the P4ShiftJIS charset
	 * implementation.
	 * 
	 * @since 2012.3
	 */
	public static final String UNICODE_MAPPING = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ UNICODE_MAPPING_SHORT_FORM;

	/**
	 * If this property is set, attempt to use this value as the number of tries
	 * for creating a auth lock file.
	 * 
	 * @since 2015.2
	 */
	public static final String AUTH_FILE_LOCK_TRY_KEY_SHORT_FORM = "authFileLockTry";
	
	/**
	 * If this property is set, attempt to use this value as the number of tries
	 * for creating a auth lock file.
	 * 
	 * @since 2015.2
	 */
	public static final String AUTH_FILE_LOCK_TRY_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ AUTH_FILE_LOCK_TRY_KEY_SHORT_FORM;

	/**
	 * If this property is set, attempt to use this value as the number of milliseconds
	 * delay for deciding if the auth lock file is new or old based on file time stamp.
	 * 
	 * @since 2015.2
	 */
	public static final String AUTH_FILE_LOCK_DELAY_KEY_SHORT_FORM = "authFileLockDelay";
	
	/**
	 * If this property is set, attempt to use this value as the number of milliseconds
	 * delay for deciding if the auth lock file is new or old based on file time stamp.
	 * 
	 * @since 2015.2
	 */
	public static final String AUTH_FILE_LOCK_DELAY_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ AUTH_FILE_LOCK_DELAY_KEY_SHORT_FORM;

	/**
	 * If this property is set, attempt to use this value as the number of milliseconds
	 * the current thread should wait (pause execution) for the other thread/process
	 * to finish handling the auth lock file.
	 * 
	 * @since 2015.2
	 */
	public static final String AUTH_FILE_LOCK_WAIT_KEY_SHORT_FORM = "authFileLockWait";
	
	/**
	 * If this property is set, attempt to use this value as the number of milliseconds
	 * the current thread should wait (pause execution) for the other thread/process
	 * to finish handling the auth lock file.
	 * 
	 * @since 2015.2
	 */
	public static final String AUTH_FILE_LOCK_WAIT_KEY = Metadata.P4JAVA_PROPS_KEY_PREFIX
													+ AUTH_FILE_LOCK_WAIT_KEY_SHORT_FORM;
}
