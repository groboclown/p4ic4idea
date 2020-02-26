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

package net.groboclown.p4.server.impl.config.part;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.util.EqualUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FileConfigPart implements ConfigPart, ConfigStateProvider {
    private static final Logger LOG = Logger.getInstance(FileConfigPart.class);

    @Nullable
    private VirtualFile filePath;
    private final VirtualFile vcsRoot;

    private String rawPort;
    private P4ServerName serverName;
    private String clientName;
    private String password;
    private File tickets;
    private File trust;
    private String user;
    private String clientHostname;
    private String charset;
    private String ignoreFile;
    private String loginSsoScript;
    private String p4config;

    private IOException loadError = null;

    public FileConfigPart(@NotNull VirtualFile vcsRoot, @Nullable VirtualFile filePath) {
        this.vcsRoot = vcsRoot;
        this.filePath = filePath;
        reload();
    }

    // for ConfigStateProvider
    @SuppressWarnings("unused")
    public FileConfigPart(String sourceName, @NotNull VirtualFile vcsRoot, @NotNull Map<String, String> stateValues) {
        this(vcsRoot, stateValues.get("f") == null
                ? null
                : LocalFileSystem.getInstance().findFileByPath(stateValues.get("f")));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! getClass().equals(o.getClass())) {
            return false;
        }
        FileConfigPart that = (FileConfigPart) o;
        return EqualUtil.isEqual(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        if (filePath == null) {
            return 0;
        }
        return filePath.hashCode();
    }

    @Override
    public String toString() {
        return "FileConfigPart(" + filePath + ")";
    }

    String getP4Config() {
        return p4config;
    }

    public VirtualFile getVcsRoot() {
        return vcsRoot;
    }

    private void setConfigFile(@Nullable VirtualFile filePath) {
        this.filePath = filePath;
        reload();
    }

    public void setConfigFile(@Nullable String filePath) {
        setConfigFile(filePath == null ? null : LocalFileSystem.getInstance().findFileByPath(filePath));
    }

    @Nullable
    public VirtualFile getConfigFile() {
        return filePath;
    }

    @Override
    public boolean reload() {
        loadError = null;
        final Properties props;
        try {
            props = loadProps(filePath);
        } catch (IOException e) {
            loadError = e;
            return false;
        }

        /* All P4 client parameters.
   P4CHARSET        Client's local character set    p4 help charset
   P4COMMANDCHARSET Client's local character set
                    (for command line operations)   p4 help charset
   P4CLIENT         Name of client workspace        p4 help client
   P4CLIENTPATH     Directories client can access   Perforce Command Reference
   P4CONFIG         Name of configuration file      Perforce Command Reference
   P4DIFF           Diff program to use on client   p4 help diff
   P4DIFFUNICODE    Diff program to use on client   p4 help diff
   P4EDITOR         Editor invoked by p4 commands   p4 help change, etc
   P4ENVIRO         Name of enviroment file         Perforce Command Reference
   P4HOST           Name of host computer           p4 help usage
   P4IGNORE         Name of ignore file             Perforce Command Reference
   P4LANGUAGE       Language for text messages      p4 help usage
   P4LOGINSSO       Client side credentials script  p4 help triggers
   P4MERGE          Merge program to use on client  p4 help resolve
   P4MERGEUNICODE   Merge program to use on client  p4 help resolve
   P4PAGER          Pager for 'p4 resolve' output   p4 help resolve
   P4PASSWD         User password passed to server  p4 help passwd
   P4PORT           Port to which client connects   p4 help info
   P4SSLDIR         SSL server credential directory Perforce Command Reference
   P4TICKETS        Location of tickets file        Perforce Command Reference
   P4TRUST          Location of ssl trust file      Perforce Command Reference
   P4USER           Perforce user name              p4 help usage
   PWD              Current working directory       p4 help usage
   TMP, TEMP        Directory for temporary files   Perforce Command Reference
         */
        // Look into parsing these:
        // "P4LOGINSSO"
        // "P4SSLDIR"

        // Explicitly ignored values:
        // "P4ENVIRO" - handled indirectly through the config stack.
        // "P4CONFIG" - no recursive config file support
        // "P4LANGUAGE" - we have special parsing for English messages, unfortunately.

        clientName = props.getProperty("P4CLIENT");
        password = props.getProperty("P4PASSWD");
        rawPort = props.getProperty("P4PORT");
        serverName = P4ServerName.forPort(rawPort);
        tickets = toFile(props.getProperty("P4TICKETS"));
        trust = toFile(props.getProperty("P4TRUST"));
        user = props.getProperty("P4USER");
        clientHostname = props.getProperty("P4HOST");
        ignoreFile = props.getProperty("P4IGNORE");
        charset = props.getProperty("P4CHARSET");
        loginSsoScript = props.getProperty("P4LOGINSSO");
        p4config = props.getProperty("P4CONFIG");

        return hasError();
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        LOG.debug("FIXME SET MESSAGE TEXT CORRECTLY");
        if (filePath == null) {
            // FIXME SET MESSAGE CORRECTLY
            LOG.debug("FIXME use message catalog");
            //return Collections.singletonList(new ConfigProblem(this, "configuration.p4config.no-file", true));
            return Collections.singleton(new ConfigProblem(this, "No file set", true));
        }
        if (! filePath.exists() || filePath.isDirectory()) {
            // FIXME SET MESSAGE CORRECTLY, and use "filePath"
            LOG.debug("FIXME use message catalog");
            //return Collections.singletonList(new ConfigProblem(this, "configuration.p4config.bad-file", true));
            return Collections.singleton(new ConfigProblem(this, "Invalid file " + filePath, true));
        }
        if (loadError != null) {
            // TODO properly handle exception messages
            return Collections.singletonList(new ConfigProblem(this, loadError.getMessage(), true));
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasError() {
        for (ConfigProblem configProblem : getConfigProblems()) {
            if (configProblem.isError()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getRawPort() {
        return rawPort;
    }

    @Nls
    @NotNull
    @Override
    public String getSourceName() {
        return filePath == null ? "<null file>" : filePath.getPath();
    }

    @Override
    public boolean hasServerNameSet() {
        return serverName != null;
    }

    @Nullable
    @Override
    public P4ServerName getServerName() {
        return serverName;
    }

    @Override
    public boolean hasClientnameSet() {
        return clientName != null;
    }

    @Nullable
    @Override
    public String getClientname() {
        return clientName;
    }

    @Override
    public boolean hasUsernameSet() {
        return user != null;
    }

    @Nullable
    @Override
    public String getUsername() {
        return user;
    }

    @Override
    public boolean hasPasswordSet() {
        return password != null;
    }

    @Nullable
    @Override
    public String getPlaintextPassword() {
        return password;
    }

    @Override
    public boolean requiresUserEnteredPassword() {
        return false;
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        return tickets != null;
    }

    @Nullable
    @Override
    public File getAuthTicketFile() {
        return tickets;
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        return trust != null;
    }

    @Nullable
    @Override
    public File getTrustTicketFile() {
        return trust;
    }

    // Fingerprint not supported by p4conf file

    @Override
    public boolean hasServerFingerprintSet() {
        return false;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        return null;
    }

    @Override
    public boolean hasClientHostnameSet() {
        return clientHostname != null;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        return clientHostname;
    }

    @Override
    public boolean hasIgnoreFileNameSet() {
        return ignoreFile != null;
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        return ignoreFile;
    }

    @Override
    public boolean hasDefaultCharsetSet() {
        return charset != null;
    }

    @Nullable
    @Override
    public String getDefaultCharset() {
        return charset;
    }

    @Override
    public boolean hasLoginSsoSet() {
        return loginSsoScript != null;
    }

    @Nullable
    @Override
    public String getLoginSso() {
        return loginSsoScript;
    }

    // ----------------------------------------------------------------


    @Nullable
    private File toFile(@Nullable String name) {
        if (name == null) {
            return null;
        }
        File ret = new File(name);
        if (! ret.isAbsolute()) {
            ret = new File(new File(vcsRoot.getPath()), name);
        }
        return ret;
    }

    private static Properties loadProps(@Nullable VirtualFile filePath) throws IOException {
        Properties props = new Properties();
        if (filePath != null) {
            try (InputStreamReader reader = new InputStreamReader(filePath.getInputStream(), filePath.getCharset())) {
                // The Perforce config file is NOT the same as a Java
                // config file.  Java config files will read the "\" as
                // an escape character, whereas the Perforce config file
                // will keep it.

                try (BufferedReader inp = new BufferedReader(reader)) {
                    String line;
                    while ((line = inp.readLine()) != null) {
                        int pos = line.indexOf('=');
                        if (pos > 0) {
                            final String key = line.substring(0, pos).trim();
                            final String value = line.substring(pos + 1).trim();
                            // NOTE: an empty value is a set value!
                            if (key.length() > 0) {
                                props.setProperty(key, value);
                            }
                        }
                    }
                }
            }
        }
        LOG.debug("Loaded property file " + filePath + " keys " + props.keySet());
        return props;
    }

    @NotNull
    @Override
    public Map<String, String> getState() {
        Map<String, String> ret = new HashMap<>();
        ret.put("f", filePath == null ? null : filePath.getPath());
        return ret;
    }
}
