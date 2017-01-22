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

package net.groboclown.idea.p4ic.mock;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.config.part.DataPart;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MockDataPart
        implements DataPart {

    private final Map<String, String> properties = new HashMap<String, String>();
    private final VirtualFile root;

    public MockDataPart(@NotNull VirtualFile root) {
        this.root = root;
    }

    @Override
    public boolean reload() {
        // Do nothing
        return true;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        /*
        PartValidation validation = new PartValidation();
        validation.checkPort(trimmedProperty(PORT_KEY), getServerName());
        validation.checkAuthTicketFile(getAuthTicketFile());
        validation.checkTrustTicketFile(getTrustTicketFile());
        return validation.getProblems();
        */
        return Collections.emptyList();
    }


    @Nullable
    @Override
    public VirtualFile getRootPath() {
        return root;
    }

    // Password must be carefully stored.

    @Override
    public boolean hasPasswordSet() {
        return false;
    }

    @Nullable
    @Override
    public String getPlaintextPassword() {
        return null;
    }

    // ----------------------------------------------------------------------
    private static final String PORT_KEY = "port";

    @Override
    public boolean hasServerNameSet() {
        return getServerName() != null;
    }

    @Nullable
    @Override
    public P4ServerName getServerName() {
        return P4ServerName.forPort(trimmedProperty(PORT_KEY));
    }

    public void setServerName(@Nullable String port) {
        setTrimmed(PORT_KEY, port);
    }

    public void setServerName(@Nullable P4ServerName port) {
        if (port != null) {
            setTrimmed(PORT_KEY, port.getFullPort());
        } else {
            setTrimmed(PORT_KEY, null);
        }
    }

    // ----------------------------------------------------------------------
    private static final String CLIENTNAME_KEY = "clientname";

    @Override
    public boolean hasClientnameSet() {
        return isTrimmedKeySet(CLIENTNAME_KEY);
    }

    @Nullable
    @Override
    public String getClientname() {
        return trimmedProperty(CLIENTNAME_KEY);
    }

    public void setClientname(@Nullable String name) {
        setTrimmed(CLIENTNAME_KEY, name);
    }

    // ----------------------------------------------------------------------
    private static final String USERNAME_KEY = "username";

    @Override
    public boolean hasUsernameSet() {
        return isTrimmedKeySet(USERNAME_KEY);
    }

    @Nullable
    @Override
    public String getUsername() {
        return trimmedProperty(USERNAME_KEY);
    }

    public void setUsername(@Nullable String name) {
        setTrimmed(USERNAME_KEY, name);
    }

    // ----------------------------------------------------------------------
    private static final String AUTH_TICKET_KEY = "authticket";

    @Override
    public boolean hasAuthTicketFileSet() {
        return getAuthTicketFile() != null;
    }

    @Nullable
    @Override
    public File getAuthTicketFile() {
        return trimmedPropertyFile(AUTH_TICKET_KEY);
    }

    public void setAuthTicketFile(@Nullable String path) {
        setTrimmed(AUTH_TICKET_KEY, path);
    }

    public void setAuthTicketFile(@Nullable File file) {
        if (file != null) {
            setTrimmed(AUTH_TICKET_KEY, file.getAbsolutePath());
        } else {
            setTrimmed(AUTH_TICKET_KEY, null);
        }
    }

    // ----------------------------------------------------------------------
    private static final String TRUST_TICKET_KEY = "trustticket";

    @Override
    public boolean hasTrustTicketFileSet() {
        return getTrustTicketFile() != null;
    }

    @Nullable
    @Override
    public File getTrustTicketFile() {
        return trimmedPropertyFile(TRUST_TICKET_KEY);
    }


    public void setTrustTicketFile(@Nullable String path) {
        setTrimmed(TRUST_TICKET_KEY, path);
    }

    public void setTrustTicketFile(@Nullable File file) {
        if (file != null) {
            setTrimmed(TRUST_TICKET_KEY, file.getAbsolutePath());
        } else {
            setTrimmed(TRUST_TICKET_KEY, null);
        }
    }


    // ----------------------------------------------------------------------
    private static final String SERVER_FINGERPRINT_KEY = "serverfingerprint";

    @Override
    public boolean hasServerFingerprintSet() {
        return isTrimmedKeySet(SERVER_FINGERPRINT_KEY);
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        return trimmedProperty(SERVER_FINGERPRINT_KEY);
    }

    public void setServerFingerprint(@Nullable String fingerprint) {
        setTrimmed(SERVER_FINGERPRINT_KEY, fingerprint);
    }

    // ----------------------------------------------------------------------
    private static final String CLIENT_HOSTNAME_KEY = "clienthostname";

    @Override
    public boolean hasClientHostnameSet() {
        return isTrimmedKeySet(CLIENT_HOSTNAME_KEY);
    }

    @Nullable
    @Override
    public String getClientHostname() {
        return trimmedProperty(CLIENT_HOSTNAME_KEY);
    }

    public void setClientHostname(@Nullable String hostname) {
        setTrimmed(CLIENT_HOSTNAME_KEY, hostname);
    }

    // ----------------------------------------------------------------------
    private static final String IGNORE_FILENAME_KEY = "ignorefilename";

    @Override
    public boolean hasIgnoreFileNameSet() {
        return isTrimmedKeySet(IGNORE_FILENAME_KEY);
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        return trimmedProperty(IGNORE_FILENAME_KEY);
    }

    public void setIgnoreFilename(@Nullable String filename) {
        setTrimmed(IGNORE_FILENAME_KEY, filename);
    }

    // ----------------------------------------------------------------------
    private static final String DEFAULT_CHARSET_KEY = "defaultcharset";

    @Override
    public boolean hasDefaultCharsetSet() {
        return isTrimmedKeySet(DEFAULT_CHARSET_KEY);
    }

    @Nullable
    @Override
    public String getDefaultCharset() {
        return trimmedProperty(DEFAULT_CHARSET_KEY);
    }

    public void setDefaultCharset(@Nullable String charset) {
        setTrimmed(DEFAULT_CHARSET_KEY, charset);
    }

    // ----------------------------------------------------------------------

    @NotNull
    @Override
    public Element marshal() {
        return new Element("test-tag");
    }


    // ----------------------------------------------------------------------


    @Nullable
    private String trimmedProperty(@NotNull String key) {
        return trimmedValue(properties.get(key));
    }

    @Nullable
    private File trimmedPropertyFile(@NotNull String key) {
        String path = trimmedProperty(key);
        return new File(path);
    }

    private boolean isTrimmedKeySet(@NotNull String key) {
        return trimmedProperty(key) != null;
    }

    private void setTrimmed(@NotNull String key, @Nullable String value) {
        value = trimmedValue(value);
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
    }

    @Nullable
    private static String trimmedValue(@Nullable String value) {
        if (value != null) {
            value = value.trim();
            if (value.length() <= 0) {
                value = null;
            }
        }
        return value;
    }
}
