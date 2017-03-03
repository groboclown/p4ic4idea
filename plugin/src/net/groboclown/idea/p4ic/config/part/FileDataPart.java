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

package net.groboclown.idea.p4ic.config.part;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

public class FileDataPart implements DataPart {
    static final String TAG_NAME = "file-data-part";
    static final ConfigPartFactory<FileDataPart> FACTORY = new Factory();
    private static final String FILE_PATH_ATTRIBUTE = "file";

    private final Project project;
    @Nullable
    private File filePath;

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
    private File loginSsoScript;

    private IOException loadError = null;

    public FileDataPart(@NotNull Project project) {
        this.project = project;
    }

    FileDataPart(@NotNull Project project, @Nullable File filePath) {
        this.project = project;
        this.filePath = filePath;
        reload();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! getClass().equals(o.getClass())) {
            return false;
        }
        FileDataPart that = (FileDataPart) o;
        return FileUtil.filesEqual(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        if (filePath == null) {
            return 0;
        }
        return FileUtil.fileHashCode(filePath);
    }

    @Override
    public String toString() {
        return "FileDataPart(" + filePath + ")";
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
        loginSsoScript = toFile(props.getProperty("P4LOGINSSO"));

        return getConfigProblems().isEmpty();
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        if (filePath == null) {
            return Collections.singletonList(new ConfigProblem(this, true, "configuration.p4config.no-file"));
        }
        if (! filePath.exists() || ! filePath.isFile()) {
            return Collections.singletonList(new ConfigProblem(this, true, "configuration.p4config.bad-file",
                    filePath));
        }
        if (loadError != null) {
            return Collections.singletonList(new ConfigProblem(this, loadError));
        }
        PartValidation validation = new PartValidation();
        validation.checkPort(this, rawPort);
        validation.checkAuthTicketFile(this);
        validation.checkTrustTicketFile(this);
        return validation.getProblems();
    }


    public void setConfigFile(@Nullable File filePath) {
        this.filePath = filePath;
        reload();
    }

    @Nullable
    public File getConfigFile() {
        return filePath;
    }


    @Nullable
    @Override
    public VirtualFile getRootPath() {
        if (filePath == null) {
            return null;
        }
        // We don't want the file itself as the root path, but rather the
        // owning directory.
        final FilePath parent = FilePathUtil.getFilePath(filePath).getParentPath();
        if (parent == null) {
            return null;
        }
        return parent.getVirtualFile();
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
    public File getLoginSso() {
        return loginSsoScript;
    }

    // ----------------------------------------------------------------

    @NotNull
    @Override
    public Element marshal() {
        final Element ret = new Element(TAG_NAME);
        if (filePath != null) {
            ret.setAttribute(FILE_PATH_ATTRIBUTE, filePath.getAbsolutePath());
        }
        return ret;
    }


    private static class Factory extends ConfigPartFactory<FileDataPart> {
        @Override
        FileDataPart create(@NotNull Project project, @NotNull Element element) {
            final FileDataPart ret = new FileDataPart(project);
            if (isTag(TAG_NAME, element)) {
                final Attribute attr = element.getAttribute(FILE_PATH_ATTRIBUTE);
                if (attr != null) {
                    final File f = ret.toFile(attr.getValue());
                    if (f != null) {
                        ret.setConfigFile(f);
                    }
                }
            }
            return ret;
        }
    }


    // ----------------------------------------------------------------


    @Nullable
    private File toFile(@Nullable String name) {
        if (name == null) {
            return null;
        }
        File ret = new File(name);
        if (! ret.isAbsolute()) {
            ret = new File(new File(project.getBaseDir().getPath()), name);
        }
        return ret;
    }

    private static Properties loadProps(@Nullable File filePath) throws IOException {
        Properties props = new Properties();
        if (filePath != null) {
            FileReader reader = new FileReader(filePath);
            try {
                // The Perforce config file is NOT the same as a Java
                // config file.  Java config files will read the "\" as
                // an escape character, whereas the Perforce config file
                // will keep it.

                BufferedReader inp = new BufferedReader(reader);
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
            } finally {
                reader.close();
            }
        }
        return props;
    }
}
