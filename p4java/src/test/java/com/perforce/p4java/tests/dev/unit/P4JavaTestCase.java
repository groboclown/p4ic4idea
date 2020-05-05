/*
 *
 */
package com.perforce.p4java.tests.dev.unit;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary.ClientLineEnd;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.ILogCallback;
import com.perforce.p4java.server.callback.ILogCallback.LogTraceLevel;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.perforce.p4java.common.base.StringHelper.format;
import static org.apache.commons.lang3.StringUtils.endsWithAny;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// p4ic4idea: IServerMessage
import com.perforce.p4java.server.IServerMessage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.perforce.p4java.tests.MockCommandCallback;

import javax.annotation.Nonnull;

import static java.util.Objects.nonNull;

/**
 * Superclass for all normal p4jtest junit tests; should be subclassed further
 * for more specific tests.
 * <p>
 * <p>
 * Basically, this class and associated gubbins define three types of Perforce
 * users available to junit tests (superuser, normal user, and invalid user)
 * plus a bunch of other useful things such as standard client names, etc.
 * <p>
 * Most of these values are retrieved on startup from the System properties and
 * made available through methods and / or the various protected fields; most
 * also have sensible defaults if no corresponding System property was passed
 * in.
 * <p>
 * See the P4Java Test wiki page for a fuller explanation of the usage and
 * semantics of the various properties declared below. In general, these must
 * match the corresponding environment variables for setup and teardown scripts
 * if used. These properties are typically either defaulted (a fairly reasonable
 * approach in most cases) or set using the System properties mechanism by the
 * caller.
 * <p>
 * The logging level can be adjusted by providing
 * com.perforce.p4javatest.loglevel as a command line option corresponding to an
 * ILogCallback.LogTraceLevel value
 *
 * @deprecated Old and nasty. (p4ic4idea)
 */

// p4ic4idea: suppress warnings on access
@SuppressWarnings("WeakerAccess")
public class P4JavaTestCase extends AbstractP4JavaUnitTest {

	/**
	 * Line separator
	 */
	protected static String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

	/**
	 * The common properties prefix for all other System properties defined in
	 * or passed in to this test class. Always use this for P4Java test
	 * properties unless there are compelling reasons not to.
	 */
	protected static String P4JTEST_PROP_PREFIX = "com.perforce.p4javatest";

	/**
	 * The property name used to retrieve the test server's URI from the System
	 * properties.
	 */
	protected static String P4JTEST_SERVER_URL_PROPNAME = P4JTEST_PROP_PREFIX + ".serverUrl";

	/**
	 * The default test server's URI, used if no URI property was available
	 * using the P4JTEST_SERVER_URL_PROPNAME System property.
	 */
	// p4ic4idea: this is only valid from within the Perforce company private network...
	protected static String P4JTEST_SERVER_URL_DEFAULT = "p4java://eng-p4java-vm.das.perforce.com:20132";

	/**
	 * The property name used to retrieve a 10.1 test server's URI from the
	 * System properties.
	 */
	protected static String P4JTEST_101_SERVER_URL_PROPNAME = P4JTEST_PROP_PREFIX + ".serverUrl101";

	/**
	 * Default URI for a suitable 10.1 test server. See
	 * P4JTEST_101_SERVER_URL_PROPNAME above for associated property name.
	 */
    // p4ic4idea: this is only valid from within the Perforce company private network...
	protected static final String P4JTEST_101_SERVER_URL_DEFAULT = "p4java://eng-p4java-vm.das.perforce.com:20101";

    // p4ic4idea: this is only valid from within the Perforce company private network...
	protected static final String P4JTEST_REPLICA_SERVER_URL_DEFAULT = "p4jrpcnts://eng-p4java-vm.das.perforce.com:20132";

	/**
	 * The property name used to retrieve the URI of a suitable Unicode server.
	 */
	protected static String P4JTEST_UNICODE_SERVER_URL_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".unicodeServerUrl";

	/**
	 * The default unicode test server's URI, used if no URI property was
	 * available using the P4JTEST_UNICODE_SERVER_URL_PROPNAME System property.
	 */
    // p4ic4idea: this is only valid from within the Perforce company private network...
	protected static String P4JTEST_UNICODE_SERVER_URL_DEFAULT = "p4java://eng-p4java-vm.das.perforce.com:30161";

	/**
	 * The property name used to retrieve the Perforce superuser's name from the
	 * System properties. The super user name is used by and available for tests
	 * that need Perforce super user privileges.
	 */
	protected static String P4JTEST_SUPERUSERNAME_PROPNAME = P4JTEST_PROP_PREFIX + ".superUserName";

	/**
	 * The default Perforce super user's name, used if no corresponding property
	 * was available using the P4JTEST_SUPERUSERNAME_PROPNAME System property.
	 */
	protected static String P4JTEST_SUPERUSERNAME_DEFAULT = "p4jtestsuper";

	/**
	 * The property name used to retrieve the Perforce standard user's name from
	 * the System properties. The standard user name is used by and available
	 * for tests that need "normal" Perforce user privileges.
	 */
	protected static String P4JTEST_USERNAME_PROPNAME = P4JTEST_PROP_PREFIX + ".userName";

	/**
	 * The property name used to retrieve the Perforce non-logged-in user's name
	 * from the System properties.
	 */
	protected static String P4JTEST_NOLOGINNAME_PROPNAME = P4JTEST_PROP_PREFIX + ".nologinName";

	/**
	 * The default Perforce standard user's name, used if no corresponding
	 * property was available using the P4JTEST_USERNAME_PROPNAME System
	 * property.
	 */
	protected static String P4JTEST_USERNAME_DEFAULT = "p4jtestuser";

	/**
	 * The default Perforce no-login user's name, used if no corresponding
	 * property was available using the P4JTEST_NOLOGINNAME_PROPNAME System
	 * property.
	 */
	protected static String P4JTEST_NOLOGINNAME_DEFAULT = "p4jtestnologin";

	/**
	 * The property name used to retrieve the Perforce superuser's password from
	 * the System properties. This password is used to log in as the Perforce
	 * super user for tests that need to do so.
	 */
	protected static String P4JTEST_SUPERPASSWORD_PROPNAME = P4JTEST_PROP_PREFIX + ".superPassword";

	/**
	 * The default Perforce super user's password, used if no corresponding
	 * property was available using the P4JTEST_SUPERPASSWORD_PROPNAME System
	 * property.
	 */
	protected static String P4JTEST_SUPERPASSWORD_DEFAULT = "p4jtestsuper";

	/**
	 * The property name used to retrieve the Perforce standard user's name from
	 * the System properties. This password is used to log in as the Perforce
	 * standard user for tests that need to do so.
	 */
	protected static String P4JTEST_USERPASSWORD_PROPNAME = P4JTEST_PROP_PREFIX + ".userPassword";

	/**
	 * The default Perforce standard user's password, used if no corresponding
	 * property was available using the P4JTEST_USERPASSWORD_PROPNAME System
	 * property.
	 */
	protected static String P4JTEST_USERPASSWORD_DEFAULT = "p4jtestuser";

	/**
	 * The property name used to retrieve a Perforce invalid user name from the
	 * System properties. The invalid user name is intended to be used by tests
	 * that need to attempt to access something on a Perforce server with an
	 * invalid user name.
	 */
	protected static String P4JTEST_INVALIDUSER_PROPNAME = P4JTEST_PROP_PREFIX + ".invalidUserName";

	/**
	 * The default Perforce invalid user's name, used if no corresponding
	 * property was available using the P4JTEST_INVALIDUSER_PROPNAME System
	 * property.
	 */
	protected static String P4JTEST_INVALIDUSER_DEFAULT = "p4jtestinvaliduser";

	/**
	 * The property name used to retrieve a Perforce invalid user's password
	 * from the System properties. Intended to be an invalid Perforce password
	 * for use with the invalid user.
	 */
	protected static String P4JTEST_INVALIDPASSWORD_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".invalidPassword";

	/**
	 * The default Perforce invalid user's password, used if no corresponding
	 * property was available using the P4JTEST_INVALIPASWORD_PROPNAME System
	 * property.
	 */
	protected static String P4JTEST_INVALIDPASSWORD_DEFAULT = "p4jtestinvaliduser";

	/**
	 * The property name used to retrieve the global test name prefix from the
	 * System properties. This string is generally intended to be used as a
	 * handy name prefix for things like test clients, file names, directories,
	 * etc.
	 */
	protected static String P4JTEST_TESTPREFIX_PROPNAME = P4JTEST_PROP_PREFIX + ".testPrefix";

	/**
	 * The default global test name prefix, used if no corresponding property
	 * was available using the P4JTEST_TESTPREFIX_PROPNAME System property.
	 */
	protected static String P4JTEST_TESTPREFIX_DEFAULT = "p4javatest";

	/**
	 * The property name used to retrieve the Perforce client workspace root
	 * from the System properties. This root is intended for use by new clients,
	 * etc.
	 */
	protected static String P4JTEST_TESTWSROOT_PROPNAME = P4JTEST_PROP_PREFIX + ".testWsRoot";

	/**
	 * The default Perforce client workspace root, used if no corresponding
	 * property was available using the P4JTEST_TESTWSROOT_PROPNAME System
	 * property.
	 */
	protected static String P4JTEST_TESTWSROOT_DEFAULT = "/tmp/" + P4JTEST_TESTPREFIX_DEFAULT;

	/**
	 * The property name used to retrieve the standard Perforce client workspace
	 * name from the System properties. This name is intended to be used for new
	 * clients, etc., typically with a test-specific suffix and / or prefix.
	 */
	protected static final String P4JTEST_TESTCLIENTNAME_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".testClientName";

	/**
	 * The default Perforce client workspace name, used if no corresponding
	 * property was available using the P4JTEST_TESTCLIENTNAME_PROPNAME System
	 * property.
	 */
	protected static final String P4JTEST_TESTCLIENTNAME_DEFAULT = "p4TestUserWS";

	/**
	 * The property name used to retrieve the location of the p4 command line
	 * binary from the System properties. The p4 command is mostly used to set
	 * up tests, etc., or access results independently.
	 * <p>
	 * <p>
	 * The interpretation of this string is inherently platform-dependent, and
	 * can generally be either an absolute path or a relative one.
	 */
	protected static final String P4JTEST_P4CMD_LOCATION_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".p4CmdLocation";

	/**
	 * The default p4 command line location, used if no corresponding property
	 * was available using the P4JTEST_P4CMD_LOCATION_PROPNAME System property.
	 * The default here assumes that the test environment includes a proper path
	 * to the p4 command line interpreter.
	 */
	protected static final String P4JTEST_P4CMD_LOCATION_DEFAULT = "p4";

	/**
	 * The property name used to retrieve the server address for a local P4
	 * server
	 */
	protected static String P4JTEST_LOCALSERVERADDRESS_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".localServerAddress";

	/**
	 * Since we will be running cross platform, we have named one client each
	 * for Windows and Mac/Linux workspaces. We set defaultTestClientName to one
	 * of these values in the class constructor.
	 */

	protected static final String P4JTEST_TESTCLIENTNAME_WINDOWS_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".testClientNameWindows";
	protected static String P4JTEST_TESTCLIENTNAME_WINDOWS_DEFAULT = "p4TestUserWS-Windows";

	protected static final String P4JTEST_TESTCLIENTNAME_MACLINUX_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".testClientNameMacLinux";
	protected static String P4JTEST_TESTCLIENTNAME_MACLINUX_DEFAULT = "p4TestUserWS-MacLinux";

	/**
	 * The property name used to retrieve the client root for P4 server.
	 */
	protected static String P4JTEST_CLIENTROOT_PROPNAME = P4JTEST_PROP_PREFIX + ".testClientRoot";

	/**
	 * The default value for the client root for the P4 server. This is set
	 * before the test and verified at retrieval with getClientRoot() in
	 * IServerInfo interface.
	 */
	protected static String P4JTEST_CLIENTROOT_DEFAULT = "//p4testUserWS";

	/**
	 * The default value for the Test Client Host
	 */
	protected static final String P4JTEST_TESTHOST_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".testClientHost";
	protected static final String P4JTEST_TESTHOST_DEFAULT = "p4JavaTestHost";

	/**
	 * Local p4 server address and port default properties
	 */
	protected static final String P4JTEST_LOCALSERVERADDRESS_DEFAULT = "127.0.0.1";
	protected static final String P4JTEST_SERVER_PORT_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".serverPort";
	protected static final String P4JTEST_SERVER_PORT_DEFAULT = "1666";

	/**
	 * The property and default value for a test checkpoint that has valid
	 * changelist created with changelistID = 3
	 */
	protected static final String P4JTEST_VALID_CHANGELIST_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".changeListID";
	protected static final int P4JTEST_VALID_CHANGELIST_DEFAULT = 3;

	/**
	 * These are the filetypes used in P4
	 */
	protected static final String P4JTEST_FILETYPE_TEXT = "text";
	protected static final String P4JTEST_FILETYPE_BINARY = "binary";
	protected static final String P4JTEST_FILETYPE_SYMLINK = "symlink";
	protected static final String P4JTEST_FILETYPE_APPLE = "apple";
	protected static final String P4JTEST_FILETYPE_RESOURCE = "resource";
	protected static final String P4JTEST_FILETYPE_UNICODE = "unicode";
	protected static final String P4JTEST_FILETYPE_UTF16 = "utf16";

	/**
	 * These are the files used to make files unique. Don't need setters/getters
	 * for these.
	 */
	protected static String textBaseFile = "TestTextFile.txt";
	protected static String testRevsFile = "unixCLEFileRevs.txt";

	/**
	 * These values indicate which file specs we want to return.
	 */
	protected static final int P4JTEST_RETURNTYPE_VALIDONLY = 0;
	protected static final int P4JTEST_RETURNTYPE_INVALIDONLY = 1;
	protected static final int P4JTEST_RETURNTYPE_ALL = 2;

	/**
	 * These values indicate which verification methods to use for FileSpecs.
	 */
	protected static final int P4JTEST_VERIFYTYPE_BASIC = 0;
	protected static final int P4JTEST_VERIFYTYPE_EXTENDED = 1;
	protected static final int P4JTEST_VERIFYTYPE_ALL = 2;
	protected static final int P4JTEST_VERIFYTYPE_MESSAGE = 3;

	/**
	 * Turns debug printing on or off in the tests.
	 */
	protected static final String P4JTEST_DEBUG_PRINTING_PROPNAME = P4JTEST_PROP_PREFIX
			+ ".debugPrint"; // default
	protected static boolean debugPrintState = false; // defaults to false

	/**
	 * Sets the logging level for the Logging callback.
	 */
	protected static final String P4JTEST_LOG_LEVEL_PROPNAME = P4JTEST_PROP_PREFIX + ".loglevel";
	protected static ILogCallback.LogTraceLevel callBackLogLevel = ILogCallback.LogTraceLevel.COARSE;

	/**
	 * The current value of the server URI string; set on startup to the value
	 * of the P4JTEST_SERVER_URL_PROPNAME property or to
	 * P4JTEST_SERVER_URL_DEFAULT if no such property is set. Can be set or
	 * changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected static String serverUrlString = null;

	/**
	 * The current value of the Unicode server URI string; set on startup to the
	 * value of the P4JTEST_UNICODE_SERVER_URL_PROPNAME property or to
	 * P4JTEST_UNICODE_SERVER_URL_DEFAULT if no such property is set. Can be set
	 * or changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected String unicodeServerUrlString = null;

	/**
	 * The current value of the test prefix string; set on startup to the value
	 * of the P4JTEST_TESTPREFIX_PROPNAME property or to
	 * P4JTEST_TESTPREFIX_DEFAULT if no such property is set. Can be set or
	 * changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected String testPrefix = null;

	/**
	 * The current value of the test workspace root; set on startup to the value
	 * of the P4JTEST_TESTWSROOT_PROPNAME property or to
	 * P4JTEST_TESTWSROOT_DEFAULT if no such property is set. Can be set or
	 * changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected String defaultTestWsRoot = null;

	/**
	 * The current value of the standard test client name; set on startup to the
	 * value of the P4JTEST_TESTCLIENTNAME_PROPNAME property or to
	 * P4JTEST_TESTCLIENTNAME_DEFAULT if no such property is set. Can be set or
	 * changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected static String defaultTestClientName = null;

	/**
	 * A mapping database to allow specific platforms running tests to have
	 * their own workspace.
	 */
	private static final Map<String, Map<PlatformType, String>> CLIENT_NAME_TO_PLATFORM_MAPPINGS = new HashMap<String, Map<PlatformType, String>>();

	static {
		// Initialise the database of mappings
		Map<PlatformType, String> p4TestUserWS20112Mapping = new HashMap<PlatformType, String>();
		p4TestUserWS20112Mapping.put(PlatformType.WINDOWS, "p4TestUserWS20112NT");
		Map<PlatformType, String> p4TestUserWSMapping = new HashMap<PlatformType, String>();
		p4TestUserWSMapping.put(PlatformType.WINDOWS, "p4TestUserWSNT");
		Map<PlatformType, String> p4TestUserWSJob072688Mapping =
				new HashMap<PlatformType, String>();
		p4TestUserWSJob072688Mapping.put(PlatformType.WINDOWS, "Job072688NT");
		Map<PlatformType, String> p4TestDevStreamMapping =
				new HashMap<PlatformType, String>();
		p4TestDevStreamMapping.put(PlatformType.WINDOWS, "p4java_stream_devNT");
		Map<PlatformType, String> p4TestMainStreamMapping =
				new HashMap<PlatformType, String>();
		p4TestMainStreamMapping.put(PlatformType.WINDOWS, "p4java_stream_mainNT");
		CLIENT_NAME_TO_PLATFORM_MAPPINGS.put("p4TestUserWS20112", p4TestUserWS20112Mapping);
		CLIENT_NAME_TO_PLATFORM_MAPPINGS.put("p4TestUserWS", p4TestUserWSMapping);
		CLIENT_NAME_TO_PLATFORM_MAPPINGS.put("Job072688Client", p4TestUserWSJob072688Mapping);
		CLIENT_NAME_TO_PLATFORM_MAPPINGS.put("p4java_stream_dev", p4TestDevStreamMapping);
		CLIENT_NAME_TO_PLATFORM_MAPPINGS.put("p4java_stream_main", p4TestMainStreamMapping);
	}

	/**
	 * The current value of the standard test Perforce user name; set on startup
	 * to the value of the P4JTEST_USERNAME_PROPNAME property or to
	 * P4JTEST_USERNAME_DEFAULT if no such property is set. Can be set or
	 * changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected static String userName = null;

	/**
	 * The current value of the standard test Perforce user password; set on
	 * startup to the value of the P4JTEST_USERPASSWORD_PROPNAME property or to
	 * P4JTEST_USERPASSWORD_DEFAULT if no such property is set. Can be set or
	 * changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected static String password = null;

	/**
	 * The current value of the standard test Perforce super user name; set on
	 * startup to the value of the P4JTEST_SUPERUSERNAME_PROPNAME property or to
	 * P4JTEST_SUPERUSERNAME_DEFAULT if no such property is set. Can be set or
	 * changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected static String superUserName = null;

	/**
	 * The current value of the standard test Perforce super user password; set
	 * on startup to the value of the P4JTEST_SUPERPASSWORD_PROPNAME property or
	 * to P4JTEST_SUPERPASSWORD_DEFAULT if no such property is set. Can be set
	 * or changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected static String superUserPassword = null;

	/**
	 * The current value of an invalid test Perforce user name; set on startup
	 * to the value of the P4JTEST_INVALIDUSER_PROPNAME property or to
	 * P4JTEST_INVALIDUSER_DEFAULT if no such property is set. Can be set or
	 * changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected String invalidUserName = null;

	/**
	 * The current value of the invalid test Perforce user password; set on
	 * startup to the value of the P4JTEST_INVALIDPASSWORD_PROPNAME property or
	 * to P4JTEST_INVALIDPASSWORD_DEFAULT if no such property is set. Can be set
	 * or changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected String invalidUserPassword = null;

	/**
	 * The current name of the not-loggied-in test Perforce user; set on startup
	 * to the value of the P4JTEST_NOLOGINUSER_PROPNAME property or to
	 * P4JTEST_NOLOGINUSER_DEFAULT if no such property is set. Can be set or
	 * changed manually during testing with the relevant getter method or
	 * directly by subclasses.
	 */
	protected String noLoginUser = null;

	/**
	 * Properties available for test usage. Includes all System properties
	 * available on startup.
	 */
	protected static Properties props = null;

	/**
	 * Error printstream; defaults to System.err, and available for error
	 * messages (if you insist).
	 */
	protected PrintStream err = System.err;

	/**
	 * Standard printstream; defaults to System.out, and available for status
	 * messages, etc.
	 */
	protected PrintStream out = System.out;

	/**
	 * The (absolute or relative) path to the p4 command line interpreter.
	 * Inherently platform-dependent.
	 */
	protected String p4CmdLocation = null;

	/**
	 * The address of the local P4 Server. Set here for testing at the local
	 * level.
	 */
	protected String localServerAddress = null;

	/**
	 * The current value of the Client Root for our test Client.
	 */
	protected String testClientRoot = null;

	/**
	 * The current value of the Client Dir for our test Client. We set this
	 * value simply to have a starting place for testing the IServerInfo
	 * getClientCurrentDirectory().
	 */
	protected String testClientCurrDir = null;

	/**
	 * The current value of the Client Host for our test Client. Created to
	 * simply to have a starting place for testing the IServerInfo
	 * getClientHost().
	 */
	protected String testClientHost = null;

	protected String serverPort = null;

	/**
	 * Useful source of random integers, etc., for tests. Typically used to
	 * generate unique number suffixes for file or client names, etc.
	 */
	protected Random rand = new Random(System.currentTimeMillis());

	/**
	 * The current test's test ID, if set. This is typically set by the @TestId
	 * class annotation, but can also be set or reset manually. Note: can be
	 * null, but is TestId.NO_TESTID by default.
	 */
	protected static String testId = TestId.NO_TESTID;

	/**
	 * The jobs array, as determined through the @Jobs annotation or set
	 * manually.
	 */
	protected String[] jobs = null;

	static {
		initLoginInfo();
	}

	/**
	 * Default initializer. Sets up defaults from System properties where
	 * possible.
	 */
	public P4JavaTestCase() {
		initialP4JavaTestCase();
	}

	protected void initialP4JavaTestCase() {
		initLoginInfo();

		processAnnotations();

		this.unicodeServerUrlString = props.getProperty(P4JTEST_UNICODE_SERVER_URL_PROPNAME,
				P4JTEST_UNICODE_SERVER_URL_DEFAULT);

		String debugStr = props.getProperty(P4JTEST_DEBUG_PRINTING_PROPNAME, null);
		if (debugStr != null) {
			if (debugStr.equalsIgnoreCase("true") || debugStr.equalsIgnoreCase("yes")
					|| debugStr.startsWith("y") || debugStr.startsWith("Y")) {
				debugPrintState = true;
			}
		}
		try {
			String logLevel = props.getProperty(P4JTEST_LOG_LEVEL_PROPNAME, null);
			if (logLevel != null) {
				callBackLogLevel = ILogCallback.LogTraceLevel.valueOf(logLevel);
			}
		} catch (IllegalArgumentException iae) {
			outln(P4JTEST_LOG_LEVEL_PROPNAME + " was set to an unknown value <"
					+ iae.getLocalizedMessage() + ">.");
			StringBuffer levels = new StringBuffer();
			for (LogTraceLevel lv : ILogCallback.LogTraceLevel.values()) {
				if (levels.length() > 0) {
					levels.append(", ");
				}
				levels.append(lv.name());
			}
			outln("Supported values for log level are " + levels);
		}

		this.noLoginUser = props.getProperty(P4JTEST_NOLOGINNAME_PROPNAME,
				P4JTEST_NOLOGINNAME_DEFAULT);

		this.invalidUserName = props.getProperty(P4JTEST_INVALIDUSER_PROPNAME,
				P4JTEST_INVALIDUSER_DEFAULT);

		this.invalidUserPassword = props.getProperty(P4JTEST_INVALIDPASSWORD_PROPNAME,
				P4JTEST_INVALIDPASSWORD_DEFAULT);

		this.p4CmdLocation = props.getProperty(P4JTEST_P4CMD_LOCATION_PROPNAME,
				P4JTEST_P4CMD_LOCATION_DEFAULT);

		this.testPrefix = props.getProperty(P4JTEST_TESTPREFIX_PROPNAME,
				P4JTEST_TESTPREFIX_DEFAULT);

		this.defaultTestWsRoot = props.getProperty(P4JTEST_TESTWSROOT_PROPNAME,
				P4JTEST_TESTWSROOT_DEFAULT);

		this.localServerAddress = props.getProperty(P4JTEST_LOCALSERVERADDRESS_PROPNAME,
				P4JTEST_LOCALSERVERADDRESS_DEFAULT);

		this.testClientRoot = props.getProperty(P4JTEST_CLIENTROOT_PROPNAME,
				P4JTEST_CLIENTROOT_DEFAULT);

		this.testClientHost = props.getProperty(P4JTEST_TESTHOST_PROPNAME,
				P4JTEST_TESTHOST_DEFAULT);

		Log.setLogCallback(createTestLogCallBack(callBackLogLevel));
	}

	private static void initLoginInfo() {
		props = new Properties(System.getProperties());

		userName = props.getProperty(P4JTEST_USERNAME_PROPNAME, P4JTEST_USERNAME_DEFAULT);
		password = props.getProperty(P4JTEST_USERPASSWORD_PROPNAME, P4JTEST_USERPASSWORD_DEFAULT);
		serverUrlString = props.getProperty(P4JTEST_SERVER_URL_PROPNAME,
				P4JTEST_SERVER_URL_DEFAULT);
		defaultTestClientName = props.getProperty(P4JTEST_TESTCLIENTNAME_PROPNAME,
				P4JTEST_TESTCLIENTNAME_DEFAULT);

		superUserName = props.getProperty(P4JTEST_SUPERUSERNAME_PROPNAME,
				P4JTEST_SUPERUSERNAME_DEFAULT);

		superUserPassword = props.getProperty(P4JTEST_SUPERPASSWORD_PROPNAME,
				P4JTEST_SUPERPASSWORD_DEFAULT);
	}

	/**
	 * Return the local host name, or die trying...
	 */

	protected static String getLocalHostName() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			String hostName = addr.getHostName();
			assertNotNull("null hostname returned in getHostName()", hostName);
			if (hostName.contains(".")) {
				int i = hostName.indexOf(".");
				if (i > 0) {
					return hostName.substring(0, i);
				}
			}
			return hostName;
		} catch (UnknownHostException uhe) {
			fail("Unexpected exception: " + uhe.getLocalizedMessage());
		}

		return null; // should never be reached...
	}

	/**
	 * Return a best guess at the current local host's platform type as
	 * determined from the os.name System property.
	 * <p>
	 * If this method can't determine the local host platform type, it will fail
	 * with a JUnit fail() call. We can expand coverage as soon as we add test
	 * platforms...
	 */

	protected static PlatformType getHostPlatformType() {
		String osName = System.getProperty("os.name");

		assertNotNull("No os.name system property!", osName);

		if (osName.contains("Windows") || osName.contains("windows")) {
			return PlatformType.WINDOWS;
		} else if (osName.contains("Mac") || osName.contains("Mac")) {
			return PlatformType.MACOSX;
		} else if (osName.contains("Linux") || osName.contains("Linux")) {
			return PlatformType.LINUX;
		}

		fail("Unable to determine host platform type");

		return null; // Not reached
	}

	/**
	 * Look for a mapping for the given client name in a database of platform
	 * specific workspaces. If none can be found, stick with the original. Note
	 * that this is an incomplete list intended to be expanded when we come
	 * across specific situations that need help.
	 *
	 * @param clientName - the original client name
	 * @return clientNameToUse - the real client name for this test
	 */
	protected static String getPlatformClientName(final String clientName) {
		Map<PlatformType, String> platformSpecificNames = CLIENT_NAME_TO_PLATFORM_MAPPINGS
				.get(clientName);
		if (platformSpecificNames != null) {
			String clientNameToUse = platformSpecificNames.get(getHostPlatformType());
			if (clientNameToUse != null) {
				return clientNameToUse;
			}
		}
		return clientName;
	}

	protected static IOptionsServer getServer(Properties props, String userName,
			String password) throws URISyntaxException, P4JavaException {
		return getServer(serverUrlString, props, userName, password);
	}

	/**
	 * Get a new IServer object for test usage and attempt to login before
	 * returning the server interface. Equivalent to a getServer(String
	 * uriString, Properties props) call immediately followed by a
	 * setUserName(userName) followed by a login(password).
	 *
	 * @param uriString if non-null, use this as the target server's URI; if null, use
	 *                  the current value of this.serverUrlString, usually set by the
	 *                  P4JTEST_SERVER_URL_PROPNAME system property or defaulted to
	 *                  P4JTEST_SERVER_URL_DEFAULT.
	 * @param props     optional properties to be passed to the server factory to be
	 *                  further passed on to the specific server implementation. Can
	 *                  be null.
	 * @param userName  user name to be used to log in with; if null, the current
	 *                  value of this.userName will be used.
	 * @param password  password to be used to log in with; if null the current value
	 *                  of this.password will be used.
	 * @return IServer as returned from the server factory. Not guaranteed to be
	 * non-null.
	 * @throws ConnectionException   passed on from the server factory
	 * @throws NoSuchObjectException passed on from the server factory
	 * @throws ConfigException       passed on from the server factory
	 * @throws ResourceException     passed on from the server factory
	 * @throws URISyntaxException    you passed in an invalid server URI...
	 * @throws RequestException      login failed for some reason
	 * @throws AccessException       access denied
	 */
	protected static IOptionsServer getServer(String uriString, Properties props, String userName,
											  String password) throws URISyntaxException, P4JavaException {
		IOptionsServer server = getServer(uriString, props);
		if (server != null) {
			server.setUserName(userName == null ? P4JavaTestCase.userName : userName);
			if (!(password != null && password.length() == 0)) {
				try {
					server.login(password == null ? P4JavaTestCase.password : password, null);
				} catch (AccessException ae) {
					if (ae.getMessage().contains("'login' not necessary, no password set for this user.")) {
						return server;
					}
				}
			}
		}
		return server;
	}

	/**
	 * Get a new IServer object for test usage and attempt to login before
	 * returning the server interface. Equivalent to a getServer(String
	 * uriString, Properties props) call immediately followed by a
	 * setUserName(userName) followed by a login(password).
	 *
	 * @param uriString if non-null, use this as the target server's URI; if null, use
	 *                  the current value of this.serverUrlString, usually set by the
	 *                  P4JTEST_SERVER_URL_PROPNAME system property or defaulted to
	 *                  P4JTEST_SERVER_URL_DEFAULT.
	 * @param props     optional properties to be passed to the server factory to be
	 *                  further passed on to the specific server implementation. Can
	 *                  be null.
	 * @param userName  user name to be used to log in with; if null, the current
	 *                  value of this.userName will be used.
	 * @param password  password to be used to log in with; if null the current value
	 *                  of this.password will be used.
	 * @return IServer as returned from the server factory. Not guaranteed to be
	 * non-null.
	 * @throws P4JavaException if anything went wrong...
	 */
	protected IOptionsServer getOptionsServer(String uriString, Properties props, String userName,
											  String password) throws P4JavaException, URISyntaxException {
		IOptionsServer server = this.getOptionsServer(uriString, props);

		if (server != null) {
			server.setUserName(userName == null ? this.userName : userName);
			server.login(password == null ? this.password : password, null);
		}
		return server;
	}

	protected void mangleTextFile(int severity, InputStream inFile, PrintStream outFile) {
		if ((inFile == null) || (outFile == null)) {
			fail("null input or output stream passed to mangleTextFile");
		}

		byte[] inBuf = new byte[2048];
		int bytesRead = 0;
		try {
			while ((bytesRead = inFile.read(inBuf)) > 0) {
				if (severity > 0) {
					for (int i = 0; i < bytesRead; i++) {
						if ((i % severity) == 0) {
							if ((inBuf[i] >= ' ') && (inBuf[i] <= '~')) {
								inBuf[i] = getRandomCharByte();
							}
						}
					}
				}
				outFile.write(inBuf, 0, bytesRead);
			}
		} catch (IOException ioexc) {
			fail("I/O exception in mangleTextFile: " + ioexc.getLocalizedMessage());
		}
	}

	protected byte getRandomCharByte() {
		int i = 0;

		while (i < ' ') {
			i = this.rand.nextInt('~' + 1);
		}

		return (byte) i;
	}

	public String getTestId() {
		return testId;
	}

	public void setTestId(String testId) {
		P4JavaTestCase.testId = testId;
	}

	private void processAnnotations() {

		if (this.getClass().isAnnotationPresent(TestId.class)) {
			TestId id = this.getClass().getAnnotation(TestId.class);

			if (id != null) {
				testId = id.value();
			} else {
				testId = "AnonymousTest";
			}
		}

		if (this.getClass().isAnnotationPresent(Jobs.class)) {
			Jobs jobs = this.getClass().getAnnotation(Jobs.class);

			if (jobs != null) {
				this.jobs = jobs.value();
			}
		}
	}

	/**
	 * Get a new IServer object for test usage without logging in.
	 * <p>
	 * <p>
	 * Note that in some circumstances (most, actually), if you're using the
	 * underlying p4 command line implementation if you don't log in explicitly,
	 * you end up doing things in the user `whoami` or the Windows equivalent,
	 * which is probably exactly wrong....
	 *
	 * @param uriString if non-null, use this as the target server's URI; if null, use
	 *                  the current value of this.serverUrlString, usually set by the
	 *                  P4JTEST_SERVER_URL_PROPNAME system property or defaulted to
	 *                  P4JTEST_SERVER_URL_DEFAULT.
	 * @param props     optional properties to be passed to the server factory to be
	 *                  further passed on to the specific server implementation. Can
	 *                  be null.
	 * @return IServer as returned from the server factory. Not guaranteed to be
	 * non-null.
	 * @throws ConnectionException   passed on from the server factory
	 * @throws NoSuchObjectException passed on from the server factory
	 * @throws ConfigException       passed on from the server factory
	 * @throws ResourceException     passed on from the server factory
	 * @throws URISyntaxException    you passed in an invalid server URI...
	 */
	protected static IOptionsServer getServer(String uriString, Properties props)
			throws ConnectionException, NoSuchObjectException, ConfigException, ResourceException,
			URISyntaxException, AccessException, RequestException {
		IOptionsServer server = null;
		if (props == null) {
			props = new Properties();
			props.put(RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_NICK, "300000");
		} else {
			props.put(RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_NICK, "300000");
		}
		if (uriString == null) {
			server = ServerFactory.getOptionsServer(serverUrlString, props);
		} else {
			server = ServerFactory.getOptionsServer(uriString, props);
		}
		if (server != null) {
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}
		}
		return server;
	}

	protected IOptionsServer getOptionsServer(String uriString, Properties props)
			throws P4JavaException, URISyntaxException {
		IOptionsServer server = ServerFactory.getOptionsServer(uriString, props);
		if (server != null) {
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}
		}
		return server;
	}

	/**
	 * Get a new IServer object for test usage using the default or current
	 * server URI. Equivalent to getServer(null, Properties props).
	 * <p>
	 *
	 * See the caveats (etc.) for getServer(null, Properties props); this is
	 * probably not a very useful convenience method...
	 */

	/**
	 * Get a new IServer object for test usage using the default or current
	 * server URI, no properties, and the default user logins.
	 * <p>
	 * <p>
	 * Equivalent to getServer(null, null, null, null).
	 */
	protected static IOptionsServer getServer() throws P4JavaException, URISyntaxException {
		// p4ic4idea: ensure this is done in a non-Perforce server...
		assertFalse("Invalid server: " + serverUrlString, serverUrlString.contains(".perforce.com"));
		IOptionsServer server = getServer(serverUrlString, null, null, null);
		assertNotNull("Null server returned by server factory in P4JavaTestCase.getServer", server);
		return server;
	}

	/**
	 * Get a new IServer object for test usage using the default or current
	 * server URI, no properties, and the super user logins.
	 * <p>
	 * <p>
	 * Convenience method for getServer(null, null, getSuperUserName(),
	 * getSuperUserPassword).
	 */
	@Deprecated
	protected static IOptionsServer getServerAsSuper() throws P4JavaException, URISyntaxException {
		return getServer(serverUrlString, null, getSuperUserName(),
				getSuperUserPassword());
	}

	/**
	 * Get a new IServer object for test usage using the default or current
	 * server URI, no properties, and the super user logins.
	 * <p>
	 * <p>
	 * Convenience method for getServer(null, null, getSuperUserName(),
	 * getSuperUserPassword).
	 */
	@Deprecated
	protected static IOptionsServer getServerAsSuper(String serverURL) throws P4JavaException, URISyntaxException {
		return getServer(serverURL, null, getSuperUserName(),
				getSuperUserPassword());
	}

	/**
	 * Get a new IServer object for test usage using the default or current
	 * server URI, properties, and the super user logins.
	 * <p>
	 * <p>
	 * Convenience method for getServer(null, null, getSuperUserName(),
	 * getSuperUserPassword).
	 */
	protected static IOptionsServer getServerAsSuper(Properties properties, String serverURL) throws P4JavaException, URISyntaxException {
		return getServer(serverURL, properties, getSuperUserName(),
				getSuperUserPassword());
	}

	/**
	 * End the server session by disconnecting.
	 */
	protected static void endServerSession(IServer server) {
		assertNotNull("null server passed to P4JavaTestCase.endServerSession", server);
		try {
			server.disconnect();
		} catch (Exception exc) {
			exc.printStackTrace();
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

	/**
	 * Gets a super user connection to server
	 *
	 * @param serverURL
	 * @return server object with super user connection
	 * @throws Exception
	 */
	protected static IOptionsServer getSuperConnection(String serverURL) throws Exception {
		IOptionsServer Server = getServerAsSuper(serverURL);
		MatcherAssert.assertThat(Server, notNullValue());
		Server.connect();
		IClient superClient = getDefaultClient(Server);
		MatcherAssert.assertThat(superClient, notNullValue());
		Server.setCurrentClient(superClient);
		return Server;
	}

	/**
	 * Return the current default test client as an IClient object. The name of
	 * the client is normally "p4java-" prepended to the local host name as
	 * determined by getLocalHostName(), e.g. "p4java-hreid-ubuntu104". This can
	 * be overridden by setting the P4JTEST_TESTCLIENTNAME_PROPNAME system
	 * property.
	 *
	 * @param server IServer to use as the server
	 * @return null if no such client, or IClient representing the default test
	 * client named by the defaultTestClientName field.
	 */

	@Nonnull
	protected static IClient getDefaultClient(IServer server)
			throws ConnectionException, RequestException, AccessException {
		assertNotNull("null server in getDefaultClient()", server);

		if (defaultTestClientName == null) {
			defaultTestClientName = "p4jtest-" + getLocalHostName();
		}

		// p4ic4idea: report the real client used.
		String clientName = getPlatformClientName(defaultTestClientName);
		IClient ret = server.getClient(clientName);
		assertNotNull(
				"could not find client named " + clientName + " (default client " + defaultTestClientName + ")",
				ret);
		return ret;
	}

	/**
	 * Return a new changelist implementation with default field values.
	 */
	protected static Changelist getNewChangelist(IServer server, IClient client, String description) {
		assertNotNull("Null client passed to getNewChangelist()", client);
		return new Changelist(IChangelist.UNKNOWN, client.getName(), getUserName(),
				ChangelistStatus.NEW, null, description, false, (Server) server);
	}

	/**
	 * Return a new changelist implementation belonging to a specific user with
	 * default field values.
	 */
	protected Changelist getNewChangelist(IServer server, IClient client, IUser user,
										  String description) {
		assertNotNull("Null client passed to getNewChangelist()", client);
		return new Changelist(IChangelist.UNKNOWN, client.getName(), user.getLoginName(),
				ChangelistStatus.NEW, null, description, false, (Server) server);
	}

	/**
	 * Get the file type of a specific file. Will usually fail if wildcards are
	 * used. Can validly return a null type.
	 */
	protected String getFileType(IOptionsServer server, String filePath) {
		assertNotNull("null server in getFileType", server);
		assertNotNull("null file path in getFileType", filePath);
		try {
			List<IFileSpec> files = server.getDepotFiles(FileSpecBuilder.makeFileSpecList(filePath),
					null);
			assertNotNull("null file list returned from getDepotFiles", files);
			assertEquals("too many file results returned in getFileType", 1, files.size());
			assertEquals("file operation status not valid in getFileType", FileSpecOpStatus.VALID,
					files.get(0).getOpStatus());
			return files.get(0).getFileType();
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
		return null;
	}

	/**
	 * Do a general byte-level copy of source file to target file. Will fail if
	 * the targetfile and / or its parent can't be created, or if an I/O
	 * exception occurs, or if either argument is null.
	 */
	protected void copyFile(String source, String target) {
		this.copyFile(source, target, true);
	}

	/**
	 * Do a general byte-level copy of source file to target file. Will fail if
	 * the targetfile and / or its parent can't be created, or if an I/O
	 * exception occurs, or if either argument is null. If overwrite is true,
	 * ignore
	 */
	protected void copyFile(String source, String target, boolean overwrite) {
		assertNotNull("null source file in copyFile", source);
		assertNotNull("null target file in copyFile", target);

		File sourceFile = new File(source);
		assertTrue("copyFile source file does not exist", sourceFile.exists());
		File targetFile = new File(target);
		File targetParent = targetFile.getParentFile();
		assertNotNull("Unable to get target parent path in copyFile", targetParent);
		if (!targetParent.exists() && !targetParent.mkdirs()) {
			fail("Unable to create parent directory for target file '" + targetFile.getPath());
		}
		if (!overwrite && targetFile.exists()) {
			fail("tried overwriting on copy without setting overwrite true");
		}
		try {
			if (!targetFile.exists() && !targetFile.createNewFile()) {
				fail("Can't create target file in copyFile after 4 tries");
			}
		} catch (IOException e) {
			fail("can't create target file in copyFile: " + e.getLocalizedMessage());
		}
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		try {
			inStream = new FileInputStream(sourceFile);
			outStream = new FileOutputStream(targetFile);
			byte[] inBytes = new byte[1024];
			int bytesRead = 0;

			while ((bytesRead = inStream.read(inBytes)) > 0) {
				outStream.write(inBytes, 0, bytesRead);
			}
		} catch (Exception exc) {
			fail("copy error in copyFile: " + exc.getLocalizedMessage());
		} finally {
			try {
				if (inStream != null)
					inStream.close();
				if (outStream != null)
					outStream.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	/**
	 * Use IClient.where to get the system (local) path of the passed-in path.
	 * Intended for use on single file paths only, i.e. it fails if you pass in
	 * wildcard strings like //depot/xyz/... which return more than one filespec
	 * result.
	 * <p>
	 * <p>
	 * Fails if it can't get a local path; this can be for a number of different
	 * reasons...
	 */
	protected String getSystemPath(IClient client, String filePath) {
		assertNotNull("null client passed to getSystemPath", client);
		assertNotNull("null file path passed to getSystemPath", filePath);
		assertNotNull("null client root in getSystemPath", client.getRoot());

		try {
			List<IFileSpec> whereList = client.where(FileSpecBuilder.makeFileSpecList(filePath));
			assertNotNull("null where list returned from client.where", whereList);
			assertEquals("getSystemPath used on more than one file", 1, whereList.size());
			IFileSpec fSpec = whereList.get(0);
			assertNotNull("null filespec in where list", fSpec);
			if (fSpec.getOpStatus() == FileSpecOpStatus.VALID) {
				return fSpec.getLocalPathString();
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}

		fail("unable to determine local path for file path '" + filePath + "'");
		return null; // not reached
	}

	/**
	 * Use IClient.where to get the depot path of the passed-in path. Intended
	 * for use on single file paths only, i.e. it fails if you pass in wildcard
	 * strings like //depot/xyz/... which return more than one filespec result.
	 * <p>
	 * <p>
	 * Fails if it can't get a depot path; this can be for a number of different
	 * reasons...
	 */
	protected String getDepotPath(IClient client, String filePath) {
		assertNotNull("null client passed to getSystemPath", client);
		assertNotNull("null file path passed to getSystemPath", filePath);
		assertNotNull("null client root in getSystemPath", client.getRoot());

		try {
			List<IFileSpec> whereList = client.where(FileSpecBuilder.makeFileSpecList(filePath));
			assertNotNull("null where list returned from client.where", whereList);
			assertEquals("getSystemPath used on more than one file", 1, whereList.size());
			IFileSpec fSpec = whereList.get(0);
			assertNotNull("null filespec in where list", fSpec);
			if (fSpec.getOpStatus() == FileSpecOpStatus.VALID) {
				return fSpec.getDepotPathString();
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}

		fail("unable to determine depot path for file path '" + filePath + "'");
		return null; // not reached
	}

	/**
	 * Use IClient.where to get the client path of the passed-in path. Intended
	 * for use on single file paths only, i.e. it fails if you pass in wildcard
	 * strings like //depot/xyz/... which return more than one filespec result.
	 * <p>
	 * <p>
	 * Fails if it can't get a client path; this can be for a number of
	 * different reasons...
	 */
	protected String getClientPath(IClient client, String filePath) {
		assertNotNull("null client passed to getSystemPath", client);
		assertNotNull("null file path passed to getSystemPath", filePath);
		assertNotNull("null client root in getSystemPath", client.getRoot());

		try {
			List<IFileSpec> whereList = client.where(FileSpecBuilder.makeFileSpecList(filePath));
			assertNotNull("null where list returned from client.where", whereList);
			assertEquals("getSystemPath used on more than one file", 1, whereList.size());
			IFileSpec fSpec = whereList.get(0);
			assertNotNull("null filespec in where list", fSpec);
			if (fSpec.getOpStatus() == FileSpecOpStatus.VALID) {
				return fSpec.getClientPathString();
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}

		fail("unable to determine client path for file path '" + filePath + "'");
		return null; // not reached
	}

	/**
	 * Do an unchecked forced sync of the passed-in root / client pair. No
	 * checking is done to see whether it actually worked or not...
	 */
	protected List<IFileSpec> forceSyncFiles(IClient client, String syncRoot)
			throws P4JavaException {

		return forceSyncFiles(client, FileSpecBuilder.makeFileSpecList(syncRoot));
	}

	protected List<IFileSpec> forceSyncFiles(IClient client, List<IFileSpec> fileSpecs)
			throws P4JavaException {
		assertNotNull(client);
		assertNotNull(fileSpecs);
		List<IFileSpec> refFiles = client.sync(fileSpecs, new SyncOptions().setForceUpdate(true));
		assertNotNull(refFiles);
		return refFiles;
	}

	/**
	 * Given a (possibly null, possibly empty) list of file specs, concatenate
	 * any non-valid filespec status messages into a single (possibly very
	 * large...) string and return it. Returns null if there's nothing to
	 * report.
	 */
	protected String reportInvalidSpecs(List<IFileSpec> files) {
		String retVal = null;
		if (files != null) {
			for (IFileSpec file : files) {
				if (file != null) {
					if ((file.getOpStatus() != null)
							&& (file.getOpStatus() != FileSpecOpStatus.VALID)) {
						if (retVal == null) {
							retVal = "";
						}
						retVal += file.getOpStatus() + ": ";
						if (file.getStatusMessage() != null) {
							retVal += file.getStatusMessage() + "; ";
						}
					}
				}
			}
		}

		return retVal;
	}

	/**
	 * Return a suitable changelist given a client name, a server, and a
	 * description. Minimal error checking is done.
	 */
	protected IChangelist createChangelist(String clientName, IServer server, String description) {
		assertNotNull("null client name passed to createChangelist", clientName);
		assertNotNull("null server passed to createChangelist", server);
		assertNotNull("null description passed to createChangelist", description);

		return new Changelist(IChangelist.UNKNOWN, clientName, this.getUserName(),
				ChangelistStatus.NEW, null, description, false, (Server) server);
	}

	/**
	 * Create a suitable pro-forma changelist for a given client.
	 */
	protected IChangelist createChangelist(IClient client) {
		assertNotNull("null client passed to createChangelist", client);

		String desc = this.testId == null ? "Unknown test changelist" : "Changelist for " + testId;
		return new Changelist(IChangelist.UNKNOWN, client.getName(), this.getUserName(),
				ChangelistStatus.NEW, null, desc, false, (Server) client.getServer());
	}

	/**
	 * Quick and dirty check to see if any client-side files are missing or
	 * they're different from the depot versions. Includes no-text diffs if
	 * nonText is true.
	 * <p>
	 * <p>
	 * Not exactly fool-proof...
	 *
	 * @return true diff the tree appears to have the same contents as the depot
	 */
	protected boolean diffTree(IClient client, List<IFileSpec> files, boolean nonText)
			throws P4JavaException {
		List<IFileSpec> diffFiles = client.getDiffFiles(
				files,
				0,
				nonText,
				false,
				false,
				true,
				false,
				false,
				false);
		assertNotNull("null file list returned from diffTree", diffFiles);
		if (diffFiles.size() != 0) {
			return false;
		}

		diffFiles = client.getDiffFiles(
				files,
				0,
				nonText,
				false,
				false,
				false,
				true,
				false,
				false);
		assertNotNull("null file list returned from diffTree", diffFiles);
		assertThat("Expected dirty check of 'diff files' return 0", diffFiles.size(), is(0));
		return diffFiles.size() == 0;
	}

	/**
	 * Check whether a given path contains the passed-in filePath in an entry in
	 * the list with the passed-in status. Somewhat impressionistic in
	 * implementation, but probably good enough for our uses...
	 * <p>
	 * <p>
	 * Note the cross-platform issues to do with local file paths vs. depot
	 * paths, etc.; you're generally on your own when using this method.
	 */

	protected boolean checkFileList(List<IFileSpec> list, String filePath,
									FileSpecOpStatus opStatus) {
		assertNotNull("null file list passed to checkFileList", list);
		assertNotNull("null file path passed to checkFileList", filePath);
		assertNotNull("null op status passed to checkFileList", opStatus);
		for (IFileSpec fSpec : list) {
			assertNotNull("null filespec in filespec list", fSpec);
			if (fSpec.getOpStatus() == opStatus) {
				if (opStatus == FileSpecOpStatus.VALID) {
					String candidate = fSpec.getDepotPathString();
					if ((candidate != null) && candidate.contains(filePath))
						return true;

					candidate = fSpec.getClientPathString();
					if ((candidate != null) && candidate.contains(filePath))
						return true;

					candidate = fSpec.getLocalPathString();
					if ((candidate != null) && candidate.contains(filePath))
						return true;

					candidate = fSpec.getPreferredPathString();
					if ((candidate != null) && candidate.contains(filePath))
						return true;
				} else {
					// look for it in the associated message:

					// p4ic4idea: iServerMessage
					IServerMessage candidate = fSpec.getStatusMessage();
					assertNotNull("null status message in non-valid file spec", candidate);
					if (candidate.getErrorOrInfoStr().contains(filePath))
						return true;
				}
			}
		}

		return false;
	}

	/**
	 * Convert a string representing a local path that has local separators
	 * (usually either "/" or "\" into one with canonicalized Perforce
	 * separators ("/").
	 */
	protected String canonicalize(String path) {
		return null;
	}

	/**
	 * Return true iff diff2 on the depot files says that the files are
	 * identical. Will do binary diffs where necessary.
	 */

	protected boolean diffDepotFiles(IOptionsServer server, String path1, String path2) {
		assertNotNull("null path1 passed to diffDepotFiles", path1);
		assertNotNull("null path2 passed to diffDepotFiles", path2);
		assertNotNull("null server passed to diffDepotFiles", server);
		try {
			List<IFileDiff> diffs = server.getFileDiffs(new FileSpec(path1), new FileSpec(path2),
					null, DiffType.CONTEXT_DIFF, false, true, false);

			assertNotNull("null diff list returned from server.getFileDiffs", diffs);
			for (IFileDiff diff : diffs) {
				assertNotNull("null diff in diff list", diff);
				if (diff.getStatus() != IFileDiff.Status.IDENTICAL) {
					return false;
				}
			}
			return true;
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}

		return false;
	}

	/**
	 * Return true iff the two passed-in local files are byte-for-byte the same.
	 * Fails is either file doesn't exist or is unreadable, or any I/O errors
	 * occur. Not perhaps the smartest way to do this, but it allows for some
	 * useful on-the-fly debugging when using junit as scaffolding...
	 */
	protected boolean cmpFiles(String path1, String path2) {
		assertNotNull("null path1 passed to diffDepotFiles", path1);
		assertNotNull("null path2 passed to diffDepotFiles", path2);

		File file1 = new File(path1);
		assertTrue("cmpFiles path1 '" + path1 + "' either not readable or doesn't exist locally",
				file1.exists() && file1.canRead());
		File file2 = new File(path2);
		assertTrue("cmpFiles path2 '" + path2 + "' either not readable or doesn't exist locally",
				file2.exists() && file2.canRead());
		if (file1.length() != file2.length()) {
			return false;
		}
		FileInputStream inStream1 = null;
		FileInputStream inStream2 = null;
		try {
			inStream1 = new FileInputStream(file1);
			inStream2 = new FileInputStream(file2);
			byte[] inBytes1 = new byte[1024];
			byte[] inBytes2 = new byte[1024];

			Arrays.fill(inBytes1, (byte) 0);
			Arrays.fill(inBytes2, (byte) 0);
			int bytesRead1 = 0;
			int bytesRead2 = 0;
			while ((bytesRead1 = inStream1.read(inBytes1)) > 0) {
				bytesRead2 = inStream2.read(inBytes2);
				// I/O error, really, since we already know they're the same
				// length, but
				// we just return false for now.
				if (bytesRead1 != bytesRead2) {
					return false;
				}

				// Note that we assume read does not touch byte arrays beyond
				// the last
				// read point, otherwise we have to clear the byte array each
				// time...
				if (!Arrays.equals(inBytes1, inBytes2)) {
					return false;
				}
			}
		} catch (Exception exc) {
			fail("Unexpected exception in cmpFiles: " + exc.getLocalizedMessage());
		} finally {
			try {
				if (inStream1 != null)
					inStream1.close();
				if (inStream2 != null)
					inStream2.close();
			} catch (Exception exc) {
				// ignore
			}
		}

		return true;
	}

	protected List<Map<String, Object>> doTaggedP4Cmd(String args[], String inString,
													  String charsetName, boolean asSuper) throws Exception {
		// Assumes tagged output...

		StringBuilder errBuf = new StringBuilder();

		List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();

		Process proc = null;
		OutputStream output = null;
		int exitCode = -1;
		BufferedReader inReader = null;
		BufferedReader errReader = null;

		assertNotNull("Passed null args array to doTaggedP4Cmd", args);
		assertNotNull("Super user name not set in doTaggedP4Cmd", this.superUserName);
		assertNotNull("Super user password not set in doTaggedP4Cmd", this.superUserPassword);

		// Ensure that the array here is just big enough to take all the
		// incoming
		// args and the setup params below:

		int cmdArgsLength = args.length + 5;
		String[] cmdArgs = new String[cmdArgsLength];

		int i = 0;
		cmdArgs[i++] = this.p4CmdLocation;
		URI uri = new URI(this.serverUrlString);
		assertNotNull("Bad server URI format in doTaggedP4Cmd", uri);
		cmdArgs[i++] = "-p" + uri.getHost() + ":" + uri.getPort();
		cmdArgs[i++] = "-u" + (asSuper ? this.superUserName : this.userName);
		cmdArgs[i++] = "-P" + (asSuper ? this.superUserPassword : this.password);
		cmdArgs[i++] = "-ztag";

		for (String str : args) {
			cmdArgs[i++] = str;
			if (i > cmdArgsLength) {
				fail("Array out of bounds in doTaggedP4Cmd");
			}
		}

		try {
			proc = Runtime.getRuntime().exec(cmdArgs);

			if (proc != null) {
				output = proc.getOutputStream();
				if (inString != null) {
					output.write(inString.getBytes());
					output.flush();
					output.close();
				}

				if (charsetName == null) {
					inReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				} else {
					inReader = new BufferedReader(
							new InputStreamReader(proc.getInputStream(), charsetName));
				}

				String inLine = null;

				Map<String, Object> map = new HashMap<String, Object>();
				while ((inLine = inReader.readLine()) != null) {
					if (inLine.startsWith("...")) {

						if (map == null) {
							map = new HashMap<String, Object>();
						}
						// Skip "... " to get field name; skip to second space
						// to get field value (if any)
						// FIXME: not a lot of error-checking goes on here yet
						// -- HR.

						String fieldName = null;
						String fieldValue = null;
						int firstSpace = inLine.indexOf(" ");
						int secondSpace = inLine.indexOf(" ",
								firstSpace < 0 ? 0 : (firstSpace + 1));
						if ((inLine.length() > firstSpace) && (secondSpace > firstSpace)) {
							fieldName = inLine.substring(firstSpace + 1, secondSpace);
							fieldValue = inLine.substring(secondSpace + 1, inLine.length());
							if (fieldName != null) {
								map.put(fieldName, fieldValue);
							}
						}
					} else {
						if (map != null) {
							mapList.add(map);
							map = null;
						}
					}
				}

				String errLine = null;

				errReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				while ((errLine = errReader.readLine()) != null) {
					errBuf.append(errLine).append("\n");
				}

				exitCode = proc.waitFor();
			}
		} finally {
			try {
				if (output != null) {
					output.close();
				}
				if (errReader != null) {
					errReader.close();
				}
				if (inReader != null) {
					inReader.close();
				}
				if (proc != null) {
					proc.destroy();
				}
			} catch (Exception exc) {
				// not much we can do here, is there?!
			}
		}

		if ((exitCode != 0) || (errBuf.length() > 0)) {
			throw new Exception(errBuf.toString());
		}

		return mapList;
	}

	/**
	 * Get a property from the test properties (which include the System
	 * properties by default).
	 */
	protected String getProperty(String propName) {
		if (propName != null) {
			return props.getProperty(propName);
		}
		return null;
	}

	/**
	 * Get a property from the test properties (which include the System
	 * properties by default); if no such property exist, return defaultValue
	 * instead.
	 */
	protected String getProperty(String propName, String defaultValue) {
		if (propName != null) {
			return props.getProperty(propName, defaultValue);
		}
		return defaultValue;
	}

	/**
	 * Set a test property.
	 */
	protected Object setProperty(String key, String value) {
		return this.props.setProperty(key, value);
	}

	/**
	 * Return a nominally-unique client name for test-specific purposes. Client
	 * name returned is in the format this.testId + passed-in-string + "Client"
	 * + a random integer between 0 and 99999, where the passed-in-string is
	 * typically the name of the specific test method called by the test, e.g.:
	 * ServerNewClientBasicTestClienttest450906
	 * <p>
	 * Note that there's a 1024 character limit on client names....
	 *
	 * @param str test-specific string; if null, ignored
	 * @return suitable client name string
	 */
	protected String getRandomClientName(String str) {
		return this.testId + (str == null ? "" : str) + "Client"
				+ Math.abs(this.rand.nextInt(99999));
	}

	/**
	 * Return the name of a suitable directory in which temporary files and
	 * workspaces (etc.) can be placed. Typically /tmp for Unix and Linux boxes,
	 * and typically taken from the JVM's java.io.tmpdir property, but none of
	 * this is guaranteed.
	 * <p>
	 * <p>
	 * Throws an exception if it can't work out where to put things.
	 */
	protected String getTempDirName() throws IOException {
		String dirName = System.getProperty("java.io.tmpdir", null);

		if (dirName == null) {
			throw new IOException("Can't determine a suitable temp directory name");
		}
		return dirName;
	}

	/**
	 * Return a nominally-unique string name for test-specific purposes. The
	 * name returned is in the format this.testId + passed-in-string + a random
	 * integer between 0 and 99999.
	 *
	 * @param str test-specific string; if null, ignored
	 * @return suitable general name string
	 */
	protected String getRandomName(String str) {
		return getRandomName(true, str);
	}

	protected String getRandomName(boolean useTestId, String str) {
		return (useTestId ? this.testId : "") + (str == null ? "" : str)
				+ Math.abs(this.rand.nextInt(99999));
	}

	protected int getRandomInt() {
		return Math.abs(this.rand.nextInt());
	}

	/**
	 * Try "tries" times to get an unused (random) file in dirPath; returns null
	 * if we can't get one that doesn't already exist. The directory named by
	 * dirPath must already exist...
	 */
	protected File getUnusedFile(int tries, String dirPath, String suffix) {
		assertNotNull("null dirPath passed to getUnusedFile", dirPath);
		assertTrue("negative 'tries' parameter passed to getUnusedFile", tries >= 0);

		File dir = new File(dirPath);
		assertTrue("dirPath does not exist on client", dir.exists());
		assertTrue("dirPath is not a directory on client", dir.isDirectory());
		File file = null;
		for (int i = 0; i < 5; i++) {
			String name = this.getRandomName(null);
			assertNotNull(name);
			file = new File(dirPath + (dirPath.endsWith("/") ? "" : "/") + name
					+ (suffix == null ? "" : "." + suffix));
			try {
				if (file.createNewFile()) {
					return file;
				}
			} catch (IOException e) {
				fail("unexpected exception: " + e.getLocalizedMessage());
			}
		}
		return file;
	}

	/**
	 * Return a client suitable for temporary use with the given name. If name
	 * is null, use getRandomClientName(null) to get a new name. The client view
	 * is left null to be stitched up later; the client's root will be at
	 * getTempDirName + "/" + testId.
	 */
	protected Client makeTempClient(String name, IServer server) throws Exception {
		String tempDirName = getTempDirName();
		if (!endsWithAny(tempDirName, "/", "\\")) {
			tempDirName = tempDirName + "/";
		}
		return new Client(name == null ? getRandomClientName(null) : name, null, // accessed
				null, // updated
				testId + " temporary test client", null, getUserName(), tempDirName + testId,
				ClientLineEnd.LOCAL, null, // client
				// options
				null, // submit options
				null, // alt roots
				server, null);
	}

	/**
	 * debugPrint prints out the values to the output window. There is a flag
	 * 'debugPrintState' in the class to turn off this feature.
	 */
	protected static void debugPrint(String expStr) {

		if (debugPrintState) {
			System.err.println("*DEBUG*: " + expStr);
		}
	}

	/**
	 * debugPrint prints out the values to the output window. There is a flag
	 * 'debugPrintState' in the class to turn off this feature.
	 */
	protected static void debugPrint(String expStr, String actStr) {

		if (debugPrintState) {
			System.err.println("*DEBUG*\n\tExp: " + expStr + "\n\tAct: " + actStr);
		}
	}

	/**
	 * debugPrint prints out the values to the output window. There is a flag
	 * 'debugPrintState' in the class to turn off this feature.
	 */
	protected static void debugPrint(String testVal, String expStr, String actStr) {

		if (debugPrintState) {
			System.err.println(
					"*DEBUG* TestVal: " + testVal + "\n\tExp: " + expStr + "\n\tAct: " + actStr);
		}
	}

	/**
	 * debugPrint prints out the values to the output window. There is a flag
	 * 'debugPrintState' in the class to turn off this feature.
	 */
	protected static void debugPrint(boolean isInfo, String val1, String val2, String val3) {

		if (debugPrintState) {
			if (isInfo) {
				System.err.println(
						"*DEBUG*\n\tInfo: " + val1 + "\n\tInfo: " + val2 + "\n\tInfo: " + val3);
			} else {
				System.err.println(
						"*DEBUG*\n\tVal1: " + val1 + "\n\tVal2: " + val2 + "\n\tVal3: " + val3);
			}
		}
	}

	/**
	 * debugPrintTestName prints out the testName to the output window. There is
	 * a flag 'debugPrintState' in the class to turn off this feature.
	 */
	protected void debugPrintTestName() {

		if (debugPrintState) {
			System.err.println("\n### DEBUG " + getName() + " ###");
		}
	}

	protected void debugPrintTestName(String testName) {

		if (debugPrintState) {
			System.err.println("\n### DEBUG " + testName + " ###");
		}
	}

	/**
	 * Print a throwable's stack trace to System.err.
	 *
	 * @param thr if not null, will have its printStackTrace method called with
	 *            System.err as the argument.
	 */
	protected void debugStackTrace(Throwable thr) {
		if (debugPrintState && (thr != null)) {
			thr.printStackTrace(System.err);
		}
	}

	/**
	 * Returns the name of this test class.
	 */
	protected String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Output a String to the test's standard out, i.e. this.out.
	 */
	protected void out(String str) {
		if (out != null) {
			out.print(str);
		}
	}

	/**
	 * Output a String to the test's standard out, i.e. this.out, and append a
	 * newline.
	 */
	protected void outln(String str) {
		if (out != null) {
			out.println(str);
		}
	}

	/**
	 * Output a String to the test's error stream, i.e. this.err.
	 */
	protected void err(String str) {
		if (err != null) {
			err.print(str);
		}
	}

	/**
	 * Output a String to the test's error stream, i.e. this.err, and append a
	 * newline.
	 */
	protected void errln(String str) {
		if (err != null) {
			err.println(str);
		}
	}

	// Sundry setters and getters...
	public static boolean getDebugPrint() {
		return debugPrintState;
	}

	public static void setDebugPrint(boolean debugPrint) {
		debugPrintState = debugPrint;
	}

    // p4ic4idea: should never be used.
    @Deprecated
	public static String getServerUrlString() {
        // return serverUrlString;
        if (serverUrlString != null && serverUrlString.startsWith("rsh:")) {
            return serverUrlString;
        }
        throw new IllegalStateException("Don't use this");
	}

	public void setServerUrlString(String serverUrlString) {
		this.serverUrlString = serverUrlString;
	}

	public String getTestPrefix() {
		return this.testPrefix;
	}

	public void setTestPrefix(String testPrefix) {
		this.testPrefix = testPrefix;
	}

	public String getDefaultTestWsRoot() {
		return this.defaultTestWsRoot;
	}

	public void setDefaultTestWsRoot(String defaultTestWsRoot) {
		this.defaultTestWsRoot = defaultTestWsRoot;
	}

	public String getDefaultTestClientName() {
		return this.defaultTestClientName;
	}

	public void setDefaultTestClientName(String defaultTestClientName) {
		this.defaultTestClientName = defaultTestClientName;
	}

	public static String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public static String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public static String getSuperUserName() {
		return superUserName;
	}

	public void setSuperUserName(String superUserName) {
		this.superUserName = superUserName;
	}

	public static String getSuperUserPassword() {
		return superUserPassword;
	}

	public void setSuperUserPassword(String superUserPassword) {
		this.superUserPassword = superUserPassword;
	}

	public String getInvalidUserName() {
		return this.invalidUserName;
	}

	public void setInvalidUserName(String invalidUserName) {
		this.invalidUserName = invalidUserName;
	}

	public String getInvalidUserPassword() {
		return this.invalidUserPassword;
	}

	public void setInvalidUserPassword(String invalidUserPassword) {
		this.invalidUserPassword = invalidUserPassword;
	}

	public Properties getProps() {
		return this.props;
	}

	public void setProps(Properties props) {
		this.props = props;
	}

	public PrintStream getErr() {
		return this.err;
	}

	public void setErr(PrintStream err) {
		this.err = err;
	}

	public PrintStream getOut() {
		return this.out;
	}

	public void setOut(PrintStream out) {
		this.out = out;
	}

	/**
	 * Dump all the info about the passed in fileSpecs.
	 */
	protected void dumpFileSpecInfo(List<IFileSpec> fSpecs) {

		dumpFileSpecInfo(fSpecs, "");
	}

	/**
	 * Dump all the info about the passed in fileSpecs.
	 */
	public void dumpFileSpecInfo(List<IFileSpec> fSpecs, String comments) {

		int validFSpecCount = 0;
		int errorFSpecCount = 0;
		int infoFSpecCount = 0;
		FileSpecOpStatus opStatus = null;

		// p4ic4idea: IServerMessage
		IServerMessage msg = null;

		debugPrint("\n\n** dumpFileSpecInfo **\n" + comments + "\n");

		if (fSpecs != null) {
			for (IFileSpec fileSpec : fSpecs) {
				if (fileSpec != null) {
					opStatus = fileSpec.getOpStatus();
					msg = fileSpec.getStatusMessage();
					if (opStatus == FileSpecOpStatus.VALID) {
						debugPrint("DumpInfo on fileSpec: " + fileSpec, "OpStatus: " + opStatus,
								"Action: " + fileSpec.getAction());
						validFSpecCount++;
					} else if (opStatus == FileSpecOpStatus.INFO) {
						debugPrint("DumpInfo on fileSpec: " + fileSpec, "OpStatus: " + opStatus,
								"StatusMsg: " + msg);
						infoFSpecCount++;
					} else if (opStatus == FileSpecOpStatus.ERROR) {
						debugPrint("DumpInfo on fileSpec: " + fileSpec, "OpStatus: " + opStatus,
								"StatusMsg: " + msg);
						errorFSpecCount++;
					}
				}
			}
		}
		debugPrint("Valid FileSpecs: " + validFSpecCount, "Info FileSpecs: " + infoFSpecCount,
				"Error FileSpecs: " + errorFSpecCount);
	}

	/**
	 * Creates a client path given the client name. Returned ClientPath ends
	 * with /
	 */
	protected String createClientPathSyntax(String clientName) {

		String newClientPathString = "//" + clientName + "/";
		debugPrint("newClientPathString: " + newClientPathString);

		return (newClientPathString);
	}

	/**
	 * Creates a client path given the client name, filename, optional
	 * fileExtension, and a file token.
	 */
	protected String createClientPathSyntax(String clientName, String fileName, String fileToken,
											String fileExt) {

		String newClientPathString = "//" + clientName + "/" + fileName + fileToken + "." + fileExt;
		debugPrint("newClientPathString: " + newClientPathString);

		return (newClientPathString);
	}

	/**
	 * Creates a client path given the client name and fileName or filePath.
	 * Replaces filePath File.separator with '/'.
	 */
	protected String createClientPathSyntax(String clientName, String filePath) {

		filePath = filePath.replace('\\', '/');

		String newClientPathString = "//" + getPlatformClientName(clientName) + "/" + filePath;
		debugPrint("newClientPathString: " + newClientPathString);

		return (newClientPathString);
	}

	/**
	 * Creates a depot path given the fileName or filePath. Replaces filePath
	 * File.separator with '/'.
	 */
	protected String createDepotPathSyntax(String filePath) {

		filePath = filePath.replace('\\', '/');

		String newDepotPathString = "//depot/" + filePath;
		debugPrint("newDepotPathString: " + newDepotPathString);

		return (newDepotPathString);
	}

	/*
	 * Opens the file that keeps local version numbers, reads the new version
	 * then returns the value as a string.
	 */
	protected String getTestFileVer() throws IOException {
		Random rnd = new Random();
		return String.valueOf(defaultTestClientName + rnd.nextInt(9999999));
	}

	protected String prepareTestFile(String sourceFile, String newFileBase, boolean fileNameOnly)
			throws IOException {
		return (prepareTestFile(sourceFile, newFileBase, "", fileNameOnly));
	}

	/**
	 * Creates an arbitrary test file with default content if it does not
	 * exist..
	 *
	 * @param sourceFileName the source file name
	 * @param isBinary       the is binary
	 * @throws FileNotFoundException the file not found exception
	 * @throws IOException           Signals that an I/O exception has occurred.
	 */
	protected static void createTestSourceFile(String sourceFileName, boolean isBinary)
			throws FileNotFoundException, IOException {
		Path path = Paths.get(sourceFileName);
		if (!Files.exists(path)) {
			File sourceFile = new File(sourceFileName);
			File parent = sourceFile.getParentFile();
			if (!parent.exists() && !parent.mkdirs()) {
				throw new IllegalStateException("Couldn't create dir: " + parent);
			}
			if (isBinary) {
				try (DataOutputStream os = new DataOutputStream(
						new FileOutputStream(sourceFileName))) {
					os.writeInt(42);
				}
			} else {
				try (FileOutputStream fs = new FileOutputStream(sourceFileName)) {
					fs.write("Test text".getBytes());
				}
			}
		}
	}

	/**
	 * This creates the file by copying the sourceFile to newFileName
	 * (newFileBase + currTestVer) If fileNameOnly is true, returns the
	 * newFileName only; else returns the entire path originally passed in.
	 */
	protected String prepareTestFile(String sourceFile, String newFileBase, String appendText,
									 boolean fileNameOnly) throws IOException {

		String fSuffix = "";
		String newFileName = null;
		String[] fileParts = null;

		String currTestVer = getTestFileVer();
		currTestVer = currTestVer.replaceAll("\\r|\\n", "");

		fileParts = newFileBase.split("\\.");
		if (fileParts.length > 1) {
			fSuffix = "." + fileParts[1];
		}

		newFileName = fileParts[0] + currTestVer + fSuffix;

		// now make any dirs needed for the file
		File fileBaseSource = new File(newFileBase);
		if (fileBaseSource.getParentFile().exists() == false) {
			boolean madeDir = fileBaseSource.getParentFile().mkdirs();
			debugPrint(fileBaseSource.getParentFile() + " does not exist: ",
					"MadeDir?: " + madeDir);
		}

		boolean copyWorked = fileCopy(sourceFile, newFileName, "");

		if (fileNameOnly) {
			File tmpFile = new File(newFileName);
			newFileName = tmpFile.getName();
		}
		debugPrint("prepareTestFile returned: ", newFileName);

		return newFileName;
	}

	protected boolean fileCopy(String sourcePath, String newFileName) throws IOException {
		return (fileCopy(sourcePath, newFileName, ""));
	}

	/**
	 * Do a direct byte-for-byte copy of an entire file.
	 */
	protected boolean fileCopy(String sourcePath, String newFileName, String appendText)
			throws IOException {

		FileOutputStream fs = null;
		boolean copyWorked = false;
		File sourceFile = new File(sourcePath);
		byte[] appendBytes = null;

		try {
			debugPrint("Copying: " + sourceFile + " to " + newFileName);

			byte[] bytes = readFileBytes(sourcePath);

			// open the file and write the bytes to the new file.
			fs = new FileOutputStream(newFileName);
			fs.write(bytes);
			if (appendText.length() > 0) {
				appendBytes = appendText.getBytes();
				fs.write(appendBytes);
				debugPrint("Copied file and appended this text: " + appendText);
			}

			copyWorked = true;
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} finally {
			if (fs != null) {
				fs.close();
			}
		}

		return copyWorked;
	}

	protected byte[] readFileBytes(String sourcePath) throws IOException {

		InputStream is = null;
		File sourceFile = new File(sourcePath);
		byte[] bytes = null;

		try {
			is = new FileInputStream(sourceFile);

			long length = sourceFile.length();
			if (length > Integer.MAX_VALUE) {
				System.err.println("File exceeded the maximum size.");
			}

			bytes = new byte[(int) length];

			// Read the entire file into the byte array
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}
			if (offset < bytes.length) {
				throw new IOException("Could not read entire file: " + sourceFile);
			}
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} finally {
			if (is != null) {
				is.close();
			}
		}

		return bytes;
	}

	protected boolean writeFileBytes(String filePath, String fileText, boolean appendToFile)
			throws IOException {
		FileOutputStream fs = null;
		boolean opWorked = false;
		File sourceFile = new File(filePath);
		byte[] bytes = null;

		try {
			debugPrint("Writing File: " + sourceFile);
			// write the bytes to the new file. May want to move this somewhere
			// else
			fs = new FileOutputStream(filePath, appendToFile);
			bytes = fileText.getBytes();

			fs.write(bytes);

			fs.close();

			opWorked = true;
		} catch (FileNotFoundException exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		} catch (IOException ioe) {
			fail("Unexpected Exception: " + ioe + " - " + ioe.getLocalizedMessage());
		} finally {
			if (fs != null) {
				fs.close();
			}
		}

		return opWorked;
	}

	/**
	 * Recursively delete all files and sub directories in a directory
	 */
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			dir.setWritable(true);
			String[] subs = dir.list();
			for (int i = 0; i < subs.length; i++) {
				boolean success = deleteDir(new File(dir, subs[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

	/**
	 * Scan a list of filespecs for info messages and return them in a list.
	 *
	 * @param fileSpecs - the output of a p4java client command
	 * @return errorMessages - a, possibly empty, list of messages
	 */
	// p4ic4idea: IServerMessage
	public static List<IServerMessage> getInfoFromFileSpecList(final List<IFileSpec> fileSpecs) {
		return getMessagesFromFileSpecList(FileSpecOpStatus.INFO, fileSpecs);
	}

	/**
	 * Scan a list of filespecs for errors and return them in a list.
	 *
	 * @param fileSpecs - the output of a p4java client command
	 * @return errorMessages - a, possibly empty, list of messages
	 */
	// p4ic4idea: IServerMessage
	public static List<IServerMessage> getErrorsFromFileSpecList(final List<IFileSpec> fileSpecs) {
		return getMessagesFromFileSpecList(FileSpecOpStatus.ERROR, fileSpecs);
	}

	/**
	 * Scan a list of filespecs for messages and return them in a list.
	 *
	 * @param fileSpecs - the output of a p4java client command
	 * @return errorMessages - a, possibly empty, list of messages
	 */
    // p4ic4idea: IServerMessage
	public static List<IServerMessage> getMessagesFromFileSpecList(final FileSpecOpStatus type,
														   final List<IFileSpec> fileSpecs) {
		List<IServerMessage> errorMessages = new ArrayList<>();
		for (IFileSpec fileSpec : fileSpecs) {
			if (type.equals(fileSpec.getOpStatus())) {
				errorMessages.add(fileSpec.getStatusMessage());
			}
		}
		return errorMessages;
	}

	/**
	 * Recursively get all files in a directory.
	 * <p>
	 * Note: must pass in a non-null 'files' list as a parameter.
	 */
	public static void getFiles(File dir, FilenameFilter filter, List<File> files) {
		if (files == null) {
			throw new IllegalArgumentException(
					"Must pass in a non-null 'files' list as a parameter.");
		}
		if (dir.isDirectory()) {
			String[] children = dir.list(filter);
			for (int i = 0; i < children.length; i++) {
				getFiles(new File(dir, children[i]), filter, files);
			}
		} else {
			files.add(dir);
		}
	}

	/**
	 * Unpack an archive file's content onto a destination folder.
	 */
	public static void unpack(File zipFile, File destDir) throws IOException {
		ZipFile zip = new ZipFile(zipFile);
		Enumeration<? extends ZipEntry> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			File f = new File(destDir, entry.getName());
			if (entry.isDirectory()) {
				continue;
			}
			if (!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}
			InputStream is = zip.getInputStream(entry);
			OutputStream os = new FileOutputStream(f);
			byte[] buf = new byte[4096];
			int r;
			while ((r = is.read(buf)) != -1) {
				os.write(buf, 0, r);
			}
			os.close();
			is.close();
		}
	}

	// p4ic4idea: really crappy stuff that should be removed.
	protected static IServerMessage serverMessage = null;
	protected static String serverCommand = null;
	protected static long completedTime;

	protected static ICommandCallback createCommandCallback() {
		return new ICommandCallback() {
			public void issuingServerCommand(int key, String command) {
				serverCommand = command;
			}

			public void completedServerCommand(int key, long millisecondsTaken) {
				completedTime = millisecondsTaken;
			}

			// p4ic4idea: IServerMessage implementations
			@Override
			public void receivedServerInfoLine(int key, IServerMessage infoLine) {
				serverMessage = infoLine;
			}

			@Override
			public void receivedServerErrorLine(int key, IServerMessage errorLine) {
				serverMessage = errorLine;
			}

			@Override
			public void receivedServerMessage(int key, IServerMessage message) {
				serverMessage = message;
			}
		};
	}

	protected static void setUtf8CharsetIfServerSupportUnicode(final IOptionsServer server)
			throws ConnectionException, AccessException, RequestException {
		setClientCharsetIfServerSupportUnicode(server, "utf8");
	}

	protected static void setClientCharsetIfServerSupportUnicode(final IOptionsServer server,
																 String clientCharset) throws ConnectionException, AccessException, RequestException {
		if (clientCharset != null && !clientCharset.isEmpty() && server.isConnected()
				&& server.supportsUnicode()) {
			server.setCharsetName(clientCharset);
		}
	}

	protected static void failIfKeyNotEqualsExpected(final int key, final int expectedKey) {

		if (key != expectedKey) {
			fail(format("key mismatch; expected: %s; observed: %s", expectedKey, key));
		}
	}

	protected static void failIfConditionFails(boolean expression, String message, Object... args) {
		if (!expression) {
			fail(format(message, args));
		}
	}

	protected static void afterEach(IOptionsServer server) {
		if (server != null) {
			endServerSession(server);
		}
	}

	protected static void afterEach(IOptionsServer server, IOptionsServer superServer) {
		if (server != null) {
			endServerSession(server);
		}
		if (superServer != null) {
			endServerSession(superServer);
		}
	}

	protected ILogCallback createTestLogCallBack() {
		return this.createTestLogCallBack(ILogCallback.LogTraceLevel.COARSE);
	}

	protected ILogCallback createTestLogCallBack(final ILogCallback.LogTraceLevel level) {
		return new ILogCallback() {

			public LogTraceLevel getTraceLevel() {
				return level;
			}

			public void internalError(String errorString) {
				internalTrace(ILogCallback.LogTraceLevel.COARSE, errorString);
			}

			public void internalException(Throwable thr) {
				if (level != ILogCallback.LogTraceLevel.NONE) {
					System.err.println(thr);
				}
			}

			public void internalInfo(String infoString) {
				internalTrace(ILogCallback.LogTraceLevel.FINE, infoString);
			}

			public void internalStats(String statsString) {
				internalTrace(ILogCallback.LogTraceLevel.SUPERFINE, statsString);
			}

			public void internalTrace(LogTraceLevel traceLevel, String traceMessage) {
				// Only log if the current level allows
				switch (level) {
					case NONE: // Never log
						break;
					case COARSE:
						if ("SUPERFINE".contains(traceLevel.toString())) {
							break;
						}
					case FINE:
					default:
						if (ILogCallback.LogTraceLevel.SUPERFINE.equals(traceLevel)) {
							break;
						}
					case SUPERFINE: // Log all levels
					case ALL: // Always log
						System.out.println(traceMessage);
				}
			}

			public void internalWarn(String warnString) {
				internalTrace(ILogCallback.LogTraceLevel.COARSE, warnString);
			}
		};
	}

	protected static Properties configRpcTimeOut(String programName, int timeOutInSeconds) {
		Properties rpcTimeOutProperties = new Properties();
		rpcTimeOutProperties.put(PropertyDefs.PROG_NAME_KEY, programName);
		rpcTimeOutProperties.put(PropertyDefs.PROG_VERSION_KEY, "tv_1.0");
		rpcTimeOutProperties.put(
				RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_NICK,
				TimeUnit.SECONDS.toMillis(timeOutInSeconds));

		return rpcTimeOutProperties;
	}

	protected static void defaultBeforeAll() throws Exception {
		// p4ic4idea: delegate...
		defaultBeforeAll(createCommandCallback());
	}

	// p4ic4idea: custom callback...
	protected static void defaultBeforeAll(ICommandCallback callback) throws Exception {
		server = getServer();
		MatcherAssert.assertThat(server, notNullValue());

		// p4ic4idea: use argument
		server.registerCallback(callback);
		server.connect();
		setUtf8CharsetIfServerSupportUnicode(server);
		server.setUserName(getUserName());

		server.login(getPassword(), new LoginOptions());
	}

	protected static void defaultAfterAll() throws Exception {
		afterEach(server);
	}

	protected static IClient getClient(IOptionsServer server) {
		IClient client = server.getCurrentClient();
		assertNotNull(client);
		return client;
	}

	protected static IClient createClient(IOptionsServer server, String clientName, String clientDescription, String clientRoot, String[] clientViews) throws Exception {
		IClient client = Client.newClient(server, clientName, clientDescription, clientRoot, clientViews);
		assertNotNull(client);
		server.createClient(client);
		server.setCurrentClient(client);
		return client;
	}

	protected static IClient createClient(IOptionsServer server, String clientName) throws Exception {
		final String clientDescription = "temp client for test";
		final String clientRoot = Paths.get(server.getServerInfo().getServerRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString();
		final String[] clientViews = {"//depot/... //" + clientName + "/..."};

		IClient client = createClient(server, clientName, clientDescription, clientRoot, clientViews);
		return client;
	}

	protected static IClient createStreamsClient(IOptionsServer server, String clientName, String streamName) throws Exception {
		final String clientDescription = "temp client for test";
		final String clientRoot = Paths.get("tmp" + File.separator + clientName).toAbsolutePath().toString();

		IClient client = Client.newClient(server, clientName, clientDescription, clientRoot, null);
		client.setStream(streamName);
		assertNotNull(client);
		server.createClient(client);
		server.setCurrentClient(client);
		client.refresh();
		return client;
	}

	protected static IDepot createDepot(String depotName, IServer server, IDepot.DepotType depottype, String streamDepth, String depotMap, String depotDescription, String suffix, Date moddate, String address, ViewMap specMap) throws P4JavaException {
		final String expectedCreationResultString = "Depot " + depotName + " saved.";
		// Create depot for the test
		IDepot depot = new Depot(
				depotName,
				server.getUserName(),
				moddate,
				depotDescription,
				depottype,
				address,
				suffix,
				streamDepth,
				depotMap,
				specMap
		);
		// Set the server user
		server.setUserName("p4jtestsuper");
		// Login using the normal method
		try {
			server.login(superUserPassword, new LoginOptions());
		} catch (AccessException ae) {

		}
		String resultStr = server.createDepot(depot);
		assertNotNull("null result string from createDepot()", resultStr);
		assertEquals(expectedCreationResultString, resultStr);
		return depot;
	}

	protected static IDepot createDepot(String depotName, IServer server, IDepot.DepotType depottype, String streamDepth) throws P4JavaException {
		final String depotMap = depotName + "/...";
		final String depotDescription = "temp stream depot for test " + testId;
		return createDepot(depotName, server, depottype, streamDepth, depotMap, depotDescription, null, null, null, null);
	}

	protected static IDepot createLocalDepot(String depotName, IServer server) throws P4JavaException {
		return createDepot(depotName, server, IDepot.DepotType.LOCAL, null);
	}

	protected static IDepot createStreamsDepot(String depotName, IServer server, String streamDepth) throws P4JavaException {
		return createDepot(depotName, server, IDepot.DepotType.STREAM, streamDepth);
	}

	protected static IDepot createSpecDepot(String depotName, IServer server) throws P4JavaException {
		return createDepot(depotName, server, IDepot.DepotType.SPEC, null);
	}

	protected static IDepot createUnloadDepot(String depotName, IServer server) throws Exception {
		return createDepot(depotName, server, IDepot.DepotType.UNLOAD, null);
	}

	protected static void createFileOnDisk(String pathname) throws Exception {
		File testFile = new File(pathname);
		testFile.getParentFile().mkdirs();
		testFile.createNewFile();
		List<String> testdata = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			testdata.add("test data");
		}
		FileUtils.writeLines(testFile, "UTF-8", testdata);
	}

	protected static void createFilesOnDisk(String[] pathnames) throws Exception {
		for (String pathname : pathnames) {
			createFileOnDisk(pathname);
		}
	}

	protected static File createFileObject(String pathname) {
		File file = new File(pathname);
		assertNotNull(file);
		return file;
	}

	protected static IChangelist createNewChangelist(IClient client, String changelistDescription) throws Exception {
		IChangelist change = new Changelist();
		change.setDescription(changelistDescription);
		change = client.createChangelist(change);
		assertNotNull(change);
		return change;
	}

	protected static void AddFileToChangelist(File testFile, IChangelist change, IClient client) throws Exception {
		List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(testFile.getAbsolutePath());
		AddFilesOptions addOpts = new AddFilesOptions();
		addOpts.setChangelistId(change.getId());
		List<IFileSpec> msg = client.addFiles(fileSpecs, addOpts);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
	}

	protected static void submitChangelist(IChangelist change) throws Exception {
		List<IFileSpec> msg = change.submit(false);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
	}

	protected static void cleanupFiles(IClient client) throws Exception {
		String clientRoot = client.getRoot();
		assertTrue("client root does not have \"tmp\" in it, will not delete cleanup to prevent accidental deletion of unintendid files of", clientRoot.contains("tmp") || clientRoot.contains("p4javatest"));
		FileUtils.deleteDirectory(new File(clientRoot));
	}

	protected static boolean attemptCleanupFiles(IClient client) {
		int count = 10;
		while (count > 0) {
			try {
				cleanupFiles(client);
				return true;
			} catch (Exception e) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			count--;
		}
		return false;
	}

	protected static void createTextFileOnServer(IClient client, String fileName, String changelistDescription) throws Exception {
		try {
			String pathname = client.getRoot() + File.separator + fileName;
			createFileOnDisk(pathname);
			File testfile = createFileObject(pathname);
			IChangelist change = createNewChangelist(client, changelistDescription);
			AddFileToChangelist(testfile, change, client);
			submitChangelist(change);
		} finally {
			if (!attemptCleanupFiles(client)) {
				System.out.println("Cannot delete client root");
			}
		}
	}

	protected static void createTextFilesOnServer(IClient client, String[] fileNames, String changelistDescription) throws Exception {
		try {
			IChangelist change = createNewChangelist(client, changelistDescription);
			for (String fileName : fileNames) {
				String pathname = client.getRoot() + File.separator + fileName;
				createFileOnDisk(pathname);
				File testfile = createFileObject(pathname);
				AddFileToChangelist(testfile, change, client);
			}
			submitChangelist(change);
		} finally {
			cleanupFiles(client);
		}
	}

	protected static void setupUtf8(IServer server) throws Exception {
		Assert.assertTrue("Server is not connected", server.isConnected());
		Assert.assertTrue("server does not support unicode", server.supportsUnicode());
		server.setCharsetName("utf8");
	}

	protected static UserGroup createSuperUserGroup(IServer server, String superUserGroupName, String superUserToAdd, String userToAdd) throws Exception {
		UserGroup userGroup = new UserGroup();
		userGroup.setName(superUserGroupName);
		userGroup.addOwner(superUserToAdd);
		userGroup.addUser(userToAdd);
		server.createUserGroup(userGroup);
		return userGroup;
	}

	protected void createUser(IOptionsServer superServer, String newUserName, String password) throws Exception {
		String email = newUserName + "@localhost.localdomain";
		String fullName = "New P4Java Test User " + newUserName;
		IUser newUser = new User(newUserName, email, fullName, null, null, null,
				null, UserType.STANDARD, null);
		newUser.setPassword(password);
		String message = superServer.createUser(newUser, true);
		assertNotNull(message);
		assertTrue(message.contentEquals("User " + newUserName + " saved."));
		newUser = server.getUser(newUserName);
		assertNotNull(newUser);
	}

	protected void deleteFile(String filePath, IOptionsServer server, IClient client, String desc) throws P4JavaException {

		// Delete submitted test files
		IChangelist deleteChangelist = getNewChangelist(server, client, desc);
		deleteChangelist = client.createChangelist(deleteChangelist);
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(filePath);
		client.deleteFiles(fileSpec,
				new DeleteFilesOptions()
						.setChangelistId(deleteChangelist.getId()));
		deleteChangelist.refresh();
		List<IFileSpec> deleteFilesList = deleteChangelist.submit(null);
		assertNotNull(deleteFilesList);
	}

	protected void copyFile(IOptionsServer server, IClient client, String description, String sourceFile, String targetFile) throws Exception {

		// Create a changelist for copy
		IChangelist changelist = getNewChangelist(server, client, description);
		assertNotNull(changelist);
		changelist = client.createChangelist(changelist);

		// Make a new copy of a file
		List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
						sourceFile), new FileSpec(targetFile), null,
				new CopyFilesOptions().setChangelistId(changelist.getId()));
		assertNotNull(copyFiles);

		// Submit the file in the copy changelist
		changelist.refresh();
		List<IFileSpec> submitCopyList = changelist.submit(null);
		assertNotNull(submitCopyList);
	}

	protected void addFile(IOptionsServer server, IClient client, String description, String filePath) throws Exception {

		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(filePath);

		// Create a changelist for add
		IChangelist changelist = getNewChangelist(server, client, description);
		assertNotNull(changelist);
		changelist = client.createChangelist(changelist);

		fileSpec = client.localWhere(fileSpec);

		createFileOnDisk(fileSpec.get(0).getLocalPathString());

		// Open file for add
		AddFilesOptions opts = new AddFilesOptions();
		opts.setChangelistId(changelist.getId());
		List<IFileSpec> addFiles = client.addFiles(fileSpec, opts);
		assertNotNull(addFiles);

		// Submit the file in the edit changelist
		changelist.refresh();
		List<IFileSpec> addFilesList = changelist.submit(null);
		assertNotNull(addFilesList);
		assertTrue(addFilesList.get(1).getStatusMessage().hasMessageFragment("Submitted"));
	}

	protected void editFile(IOptionsServer server, IClient client, String description, String filePath) throws Exception {

		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(filePath);
		client.sync(fileSpec, new SyncOptions().setForceUpdate(true));

		// Create a changelist for edit
		IChangelist changelist = getNewChangelist(server, client, description);
		assertNotNull(changelist);
		changelist = client.createChangelist(changelist);

		fileSpec = client.localWhere(fileSpec);


		// Open file for edit
		EditFilesOptions opts = new EditFilesOptions();
		opts.setChangelistId(changelist.getId());
		List<IFileSpec> editFiles = client.editFiles(fileSpec, opts);
		assertNotNull(editFiles);
		String content = "edit" + System.lineSeparator();
		Files.write(Paths.get(fileSpec.get(0).getLocalPathString()), content.getBytes(), StandardOpenOption.APPEND);

		// Submit the file in the edit changelist
		changelist.refresh();
		List<IFileSpec> editFilesList = changelist.submit(null);
		assertNotNull(editFilesList);
		// p4ic4idea: IServerMessage
		assertTrue(editFilesList.get(1).getStatusMessage().hasMessageFragment("Submitted"));
	}

	public void validateFileSpecs(List<IFileSpec> fileSpecs) {
		MatcherAssert.assertThat(fileSpecs, notNullValue());
		for (IFileSpec fileSpec : fileSpecs) {
			if (fileSpec.getOpStatus() != FileSpecOpStatus.INFO) {
				// p4ic4idea: IServerMessage
				MatcherAssert.assertThat(fileSpec.getStatusString(), fileSpec.getOpStatus(),
						is(FileSpecOpStatus.VALID));
			}
		}
	}

	public void assertFileSpecError(List<IFileSpec> fileSpecs) {
		MatcherAssert.assertThat(fileSpecs, notNullValue());
		for (IFileSpec fileSpec : fileSpecs) {
			// p4ic4idea: IServerMessage
			assertFalse(fileSpec.getStatusString(), fileSpec.getOpStatus() == FileSpecOpStatus.ERROR);
		}
	}

	public static class SimpleCallbackHandler implements IStreamingCallback {
		int expectedKey = 0;
		P4JavaTestCase testCase = null;

		public SimpleCallbackHandler(P4JavaTestCase testCase,
									 int key) {
			if (testCase == null) {
				throw new NullPointerException(
						"null testCase passed to CallbackHandler constructor");
			}
			this.expectedKey = key;
			this.testCase = testCase;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			if (resultMap == null) {
				fail("null result map in handleResult");
			}
			return true;
		}
	}

	public static class ListCallbackHandler implements IStreamingCallback {

		int expectedKey = 0;
		P4JavaTestCase testCase = null;
		List<Map<String, Object>> resultsList = null;

		public ListCallbackHandler(P4JavaTestCase testCase,
								   int key, List<Map<String, Object>> resultsList) {
			this.expectedKey = key;
			this.testCase = testCase;
			this.resultsList = resultsList;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			if (resultMap == null) {
				fail("null resultMap passed to handleResult callback");
			}
			this.resultsList.add(resultMap);
			return true;
		}

		public List<Map<String, Object>> getResultsList() {
			return this.resultsList;
		}
	}

	public static class ParallelStreamingHandler implements IStreamingCallback {

		int expectedKey = 0;

		List<Map<String, Object>> resultsList = null;

		public ParallelStreamingHandler(int key, List<Map<String, Object>> resultsList) {
			this.expectedKey = key;
			this.resultsList = resultsList;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			if (resultMap == null) {
				fail("null resultMap passed to handleResult callback");
			}
			this.resultsList.add(resultMap);
			return true;
		}

		public List<Map<String, Object>> getResultsList() {
			return this.resultsList;
		}
	}

}
