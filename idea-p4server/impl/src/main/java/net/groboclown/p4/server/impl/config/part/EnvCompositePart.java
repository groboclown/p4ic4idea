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
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.env.PerforceEnvironment;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.ConfigPropertiesUtil;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.config.part.ConfigPartAdapter;
import net.groboclown.p4.server.api.config.part.MultipleConfigPart;
import net.groboclown.p4.server.api.util.JreSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EnvCompositePart implements ConfigPart, ConfigStateProvider {
    private static final Logger LOG = Logger.getInstance(EnvCompositePart.class);

    private final VirtualFile vcsRoot;
    private MultipleConfigPart configParts;

    public EnvCompositePart(@NotNull VirtualFile vcsRoot) {
        this.vcsRoot = vcsRoot;
        loadEnvironmentParts();
    }

    // for ConfigStateProvider
    public EnvCompositePart(String sourceName, @NotNull VirtualFile vcsRoot, Map<String, String> values) {
        this(vcsRoot);
    }

    @Override
    public boolean reload() {
        loadEnvironmentParts();

        // generatedProblems are not thrown here.  ENV loading should not cause exceptions,
        // but instead report problems.

        return hasError();
    }

    @Override
    public String toString() {
        return configParts.toString();
    }

    @Override
    @NotNull
    public Collection<ConfigProblem> getConfigProblems() {
        return configParts.getConfigProblems();
    }

    @Override
    public boolean hasError() {
        return configParts.hasError();
    }

    @Override
    public boolean hasServerNameSet() {
        return configParts.hasServerNameSet();
    }

    @Override
    @Nullable
    public P4ServerName getServerName() {
        return configParts.getServerName();
    }

    @Override
    public boolean hasClientnameSet() {
        return configParts.hasClientnameSet();
    }

    @Override
    @Nullable
    public String getClientname() {
        return configParts.getClientname();
    }

    @Override
    public boolean hasUsernameSet() {
        return configParts.hasUsernameSet();
    }

    @Override
    @Nullable
    public String getUsername() {
        return configParts.getUsername();
    }

    @Override
    public boolean hasPasswordSet() {
        return configParts.hasPasswordSet();
    }

    @Override
    @Nullable
    public String getPlaintextPassword() {
        return configParts.getPlaintextPassword();
    }

    @Override
    public boolean requiresUserEnteredPassword() {
        return configParts.requiresUserEnteredPassword();
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        return configParts.hasAuthTicketFileSet();
    }

    @Override
    @Nullable
    public File getAuthTicketFile() {
        return configParts.getAuthTicketFile();
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        return configParts.hasTrustTicketFileSet();
    }

    @Override
    @Nullable
    public File getTrustTicketFile() {
        return configParts.getTrustTicketFile();
    }

    @Override
    public boolean hasServerFingerprintSet() {
        return configParts.hasServerFingerprintSet();
    }

    @Override
    @Nullable
    public String getServerFingerprint() {
        return configParts.getServerFingerprint();
    }

    @Override
    public boolean hasClientHostnameSet() {
        return configParts.hasClientHostnameSet();
    }

    @Override
    @Nullable
    public String getClientHostname() {
        return configParts.getClientHostname();
    }

    @Override
    public boolean hasIgnoreFileNameSet() {
        return configParts.hasIgnoreFileNameSet();
    }

    @Override
    @Nullable
    public String getIgnoreFileName() {
        return configParts.getIgnoreFileName();
    }

    @Override
    public boolean hasDefaultCharsetSet() {
        return configParts.hasDefaultCharsetSet();
    }

    @Override
    @Nullable
    public String getDefaultCharset() {
        return configParts.getDefaultCharset();
    }

    @Override
    public boolean hasLoginSsoSet() {
        return configParts.hasLoginSsoSet();
    }

    @Override
    @Nullable
    public String getLoginSso() {
        return configParts.getLoginSso();
    }

    private void loadEnvironmentParts() {
        List<ConfigPart> parts = new ArrayList<>();

        String p4config = null;
        String p4enviro = null;

        if (WinRegDataPart.isAvailable()) {
            WinRegDataPart userReg = new WinRegDataPart(true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded user windows registry: " +
                        ConfigPropertiesUtil.toProperties(userReg,
                        "<unset>", "<empty>", "<set>"));
            }
            // p4config is always null at this point.
            // if (p4config == null) {
                p4config = userReg.getP4ConfigFile();
            // }
            // if (p4enviro == null) {
                p4enviro = userReg.getP4EnviroFile();
            // }
            WinRegDataPart sysReg = new WinRegDataPart(false);
            if (p4config == null) {
                p4config = sysReg.getP4ConfigFile();
            }
            if (p4enviro == null) {
                p4enviro = sysReg.getP4EnviroFile();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded system windows registry: " +
                        ConfigPropertiesUtil.toProperties(sysReg,
                                "<unset>", "<empty>", "<set>"));
            }
            parts.add(userReg);
            parts.add(sysReg);
        }

        parts.add(new EnvPassword());

        SimpleDataPart envData = new SimpleDataPart(vcsRoot, getSourceName(), null);
        envData.setServerName(PerforceEnvironment.getP4Port());
        envData.setUsername(PerforceEnvironment.getP4User());
        envData.setClientname(PerforceEnvironment.getP4Client());
        envData.setClientHostname(PerforceEnvironment.getP4Host());
        envData.setDefaultCharset(PerforceEnvironment.getP4Charset());
        envData.setAuthTicketFile(PerforceEnvironment.getP4Tickets());
        envData.setTrustTicketFile(PerforceEnvironment.getP4Trust());
        envData.setIgnoreFilename(PerforceEnvironment.getP4Ignore());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loaded environment variables: " +
                    ConfigPropertiesUtil.toProperties(envData,
                            "<unset>", "<empty>", "<set>"));
        }

        if (p4config == null) {
            p4config = PerforceEnvironment.getP4Config();
        }
        if (p4enviro == null) {
            p4enviro = PerforceEnvironment.getP4Enviro();
        }

        // P4CONFIG loading
        {
            if (p4config != null) {
                if (p4config.indexOf('/') >= 0 || p4config.indexOf('\\') >= 0 || p4config.indexOf(File.separatorChar) >= 0) {
                    // File location
                    File f = new File(p4config);
                    if (!f.isAbsolute()) {
                        f = new File(new File(vcsRoot.getPath()), p4config);
                    }

                    final FileConfigPart envConf = new FileConfigPart(vcsRoot, f);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Env defined absolute P4CONFIG: " + f + ": " +
                                ConfigPropertiesUtil.toProperties(envConf,
                                        "<unset>", "<empty>", "<set>"));
                    }
                    parts.add(envConf);
                } else {
                    // Scan from the vcs root down for a matching file name.
                    File f = scanParentsForFile(vcsRoot, p4config);
                    if (f != null) {
                        parts.add(new FileConfigPart(vcsRoot, f));
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Env defined relative P4CONFIG: " + p4config);
                    }
                }
            }
        }

        // P4ENVIRO loading
        {
            if (p4enviro != null) {
                File f = getRelFile(p4enviro);
                // The P4ENVIRO will have a default value if the user didn't specify one.
                // Therefore, if the file doesn't exist, don't complain.
                if (f.exists()) {
                    final FileConfigPart envConf = new FileConfigPart(vcsRoot, f);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Env defined P4ENVIRO: " + f + ": " +
                                ConfigPropertiesUtil.toProperties(envConf,
                                        "<unset>", "<empty>", "<set>"));
                    }
                    parts.add(envConf);
                }
            }
        }

        // Default configuration settings, if they are not set.

        MultipleConfigPart try1 = new MultipleConfigPart("First Try", parts);
        if (!try1.hasAuthTicketFileSet()) {
            envData.setAuthTicketFile(getDefaultAuthTicketFile());
        }
        if (!try1.hasTrustTicketFileSet()) {
            envData.setTrustTicketFile(getDefaultTrustTicketFile());
        }

        // Need to recreate the part, because we just modified it.
        this.configParts = new MultipleConfigPart("Environment Settings", parts);
    }

    @Override
    @Nls
    @NotNull
    public String getSourceName() {
        return "Environment";
    }

    @NotNull
    @Override
    public Map<String, String> getState() {
        return Collections.emptyMap();
    }

    private static class EnvPassword extends ConfigPartAdapter {
        private EnvPassword() {
            super("Env Password");
        }

        @Override
        public boolean hasPasswordSet() {
            // Note: not trimmed, not empty string checked.
            return PerforceEnvironment.getP4Passwd() != null;
        }

        @Nullable
        @Override
        public String getPlaintextPassword() {
            return PerforceEnvironment.getP4Passwd();
        }

        @Override
        public boolean reload() {
            return true;
        }

        @NotNull
        @Override
        public Collection<ConfigProblem> getConfigProblems() {
            return Collections.emptyList();
        }
    }


    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     */
    @Nullable
    private File getDefaultAuthTicketFile() {
        // Cannot use the user.home system property, because there are very exact meanings to
        // the default locations.

        // Windows check
        String userprofile = JreSettings.getEnv("USERPROFILE");
        if (userprofile != null) {
            return getFileAt(userprofile, "p4tickets.txt");
        }
        String home = JreSettings.getEnv("HOME");
        if (home != null) {
            return getFileAt(home, ".p4tickets");
        }
        return null;
    }

    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     */
    @Nullable
    private File getDefaultTrustTicketFile() {
        // Windows check
        String userprofile = JreSettings.getEnv("USERPROFILE");
        if (userprofile != null) {
            return getFileAt(userprofile, "p4trust.txt");
        }
        String home = JreSettings.getEnv("HOME");
        if (home != null) {
            return getFileAt(home, ".p4trust");
        }
        return null;
    }

    @NotNull
    File getFileAt(@NotNull String dir, @NotNull String name) {
        return new File(new File(dir), name);
    }

    @NotNull
    File getRelFile(@NotNull String path) {
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(vcsRoot.getPath(), path);
        }
        return getFileAt(f.getParent(), f.getName());
    }

    File scanParentsForFile(@NotNull VirtualFile initialDir, @NotNull String fileName) {
        VirtualFile prevParent;
        VirtualFile parent = initialDir;
        do {
            VirtualFile f = parent.findChild(fileName);
            if (f != null && f.exists()) {
                return getFileAt(parent.getPath(), fileName);
            }
            prevParent = parent;
            parent = parent.getParent();
        } while (parent != null && !parent.equals(prevParent));
        return null;
    }
}
