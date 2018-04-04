/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perforce.p4java;

import com.perforce.p4java.client.IClient;
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
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.unit.PlatformType;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class P4JavaUtil {
    public static final String DEFAULT_USER = "luser";
    public static final String DEFAULT_USER_PASSWORD = "password1";

    public static final String DEFAULT_SUPER = "lsuper";
    public static final String DEFAULT_SUPER_PASSWORD = "password2";




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
    public static IOptionsServer getServer(String uriString, Properties props, String userName,
            String password) throws P4JavaException, URISyntaxException {
        IOptionsServer server = getServer(uriString, props);

        if (server != null) {
            server.setUserName(userName);
            if (password != null && password.length() > 0) {
                server.login(password, null);
            }
        }
        return server;
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
    public static IOptionsServer getServer(String uriString, Properties props)
            throws ConnectionException, NoSuchObjectException, ConfigException, ResourceException,
            URISyntaxException, AccessException, RequestException {
        IOptionsServer server;

        System.err.println("Connecting to server " + uriString);
        server = ServerFactory.getOptionsServer(uriString, props);
        if (server != null) {
            server.connect();
            if (server.isConnected()) {
                if (server.supportsUnicode()) {
                    server.setCharsetName("utf8");
                }
                String password = props.getProperty(PropertyDefs.PASSWORD_KEY);
                if (isBlank(password)) {
                    password = props.getProperty(PropertyDefs.PASSWORD_KEY_SHORTFORM);
                }
                if (isNotBlank(password)) {
                    server.login(password);
                }
            }
        }
        return server;
    }

    public static IOptionsServer getOptionsServer(String uriString, Properties props)
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
     * Return the current default test client as an IClient object.  The client
     * cannot be encoded in the basic server root setup, due to the execution
     * environment being different from test execution to test execution.
     *
     * @param server IServer to use as the server
     * @return null if no such client, or IClient representing the default test
     * client named by the defaultTestClientName field.
     */
    public static IClient getClient(@Nonnull IServer server, String clientName, File clientRoot,
            boolean createIfNotExists)
            throws ConnectionException, RequestException, AccessException {
        assertNotNull("null server in getDefaultClient()", server);

        IClient client = server.getClient(clientName);
        if (client == null && createIfNotExists) {
            client = server.getClientTemplate(clientName, false);
            assertNotNull(client);
            // all the default mappings should be fine.
            client.setRoot(clientRoot.getAbsolutePath());
            server.createClient(client);
        } else if (clientRoot != null && client != null &&
                (client.getRoot() == null || !clientRoot.getAbsolutePath().equals(new File(client.getRoot()).getAbsolutePath()))) {
            client.setRoot(clientRoot.getAbsolutePath());
        }
        return client;
    }

    public static IClient getDefaultClient(@Nonnull IServer server, File clientRoot)
            throws ConnectionException, RequestException, AccessException {
        return getClient(server, "p4jtest-localhost-" + server.getUserName(), clientRoot, true);
    }

    /**
     * Return a best guess at the current local host's platform type as
     * determined from the os.name System property.
     * <p>
     * If this method can't determine the local host platform type, it will fail
     * with a JUnit fail() call. We can expand coverage as soon as we add test
     * platforms...
     */
    public static PlatformType getHostPlatformType() {
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

    public static List<IFileSpec> checkedForceSyncFiles(IClient client, String syncRoot) throws P4JavaException {
        List<IFileSpec> ret = forceSyncFiles(client, syncRoot);
        for (IFileSpec spec : ret) {
            if (spec.getStatusMessage() != null && spec.getStatusMessage().isInfoOrError()) {
                throw new RequestException(spec.getStatusMessage());
            }
        }
        return ret;
    }

    /**
     * Do an unchecked forced sync of the passed-in root / client pair. No
     * checking is done to see whether it actually worked or not...
     */
    public static List<IFileSpec> forceSyncFiles(IClient client, String syncRoot)
            throws P4JavaException {
        return forceSyncFiles(client, FileSpecBuilder.makeFileSpecList(syncRoot));
    }

    public static List<IFileSpec> forceSyncFiles(IClient client, List<IFileSpec> fileSpecs)
            throws P4JavaException {
        assertNotNull(client);
        assertNotNull(fileSpecs);
        List<IFileSpec> refFiles = client.sync(fileSpecs, new SyncOptions().setForceUpdate(true));
        assertNotNull(refFiles);
        return refFiles;
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
    @Nonnull
    public static String getSystemPath(IClient client, String filePath) {
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
        return ""; // not reached
    }

    /**
     * Do a general byte-level copy of source file to target file. Will fail if
     * the targetfile and / or its parent can't be created, or if an I/O
     * exception occurs, or if either argument is null.
     */
    public static void copyFile(String source, String target)
            throws IOException {
        copyFile(source, target, true);
    }

    /**
     * Do a general byte-level copy of source file to target file. Will fail if
     * the targetfile and / or its parent can't be created, or if an I/O
     * exception occurs, or if either argument is null. If overwrite is true,
     * ignore
     */
    public static void copyFile(String source, String target, boolean overwrite)
            throws IOException {
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
        try (FileInputStream inStream = new FileInputStream(sourceFile);
                FileOutputStream outStream = new FileOutputStream(targetFile)) {
            byte[] inBytes = new byte[1024];
            int bytesRead = 0;

            while ((bytesRead = inStream.read(inBytes)) > 0) {
                outStream.write(inBytes, 0, bytesRead);
            }
        }
        // Ignore
    }

    /**
     * End the server session by disconnecting.
     */
    public static void endServerSession(IServer server)
            throws ConnectionException, AccessException {
        assertNotNull("null server passed to P4JavaTestCase.endServerSession", server);
        server.disconnect();
    }


    public static void setUtf8CharsetIfServerSupportUnicode(final IOptionsServer server)
            throws ConnectionException, AccessException, RequestException {
        setClientCharsetIfServerSupportUnicode(server, "utf8");
    }

    public static void setClientCharsetIfServerSupportUnicode(final IOptionsServer server,
            String clientCharset) throws ConnectionException, AccessException, RequestException {
        if (clientCharset != null && !clientCharset.isEmpty() && server.isConnected()
                && server.supportsUnicode()) {
            server.setCharsetName(clientCharset);
        }
    }
}
