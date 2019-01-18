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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
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
    @SuppressWarnings("unused")
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
    public String getRawPort() {
        return configParts.getRawPort();
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

        WinRegDataPart userWinRegistry = null;
        WinRegDataPart systemWinRegistry = null;
        EnvPassword envPassword = new EnvPassword();

        if (WinRegDataPart.isAvailable()) {
            userWinRegistry = new WinRegDataPart(true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded user windows registry: " +
                        ConfigPropertiesUtil.toProperties(userWinRegistry,
                        "<unset>", "<empty>", "<set>"));
            }
            systemWinRegistry = new WinRegDataPart(false);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded system windows registry: " +
                        ConfigPropertiesUtil.toProperties(systemWinRegistry,
                                "<unset>", "<empty>", "<set>"));
            }
        }

        // TODO add OsXPreferencesPart

        SimpleDataPart envData = new SimpleDataPart(vcsRoot, getSourceName(), null);

        // Note: using the JreSettings rather than checking the System.env directly.
        envData.setServerName(JreSettings.getEnv(PerforceEnvironment.P4PORT));
        envData.setUsername(JreSettings.getEnv(PerforceEnvironment.P4USER));
        envData.setClientname(JreSettings.getEnv(PerforceEnvironment.P4CLIENT));
        envData.setClientHostname(JreSettings.getEnv(PerforceEnvironment.P4HOST));
        envData.setDefaultCharset(JreSettings.getEnv(PerforceEnvironment.P4CHARSET));
        envData.setAuthTicketFile(JreSettings.getEnv(PerforceEnvironment.P4TICKETS));
        envData.setTrustTicketFile(JreSettings.getEnv(PerforceEnvironment.P4TRUST));
        envData.setIgnoreFilename(JreSettings.getEnv(PerforceEnvironment.P4IGNORE));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loaded environment variables: " +
                    ConfigPropertiesUtil.toProperties(envData,
                            "<unset>", "<empty>", "<set>"));
        }

        // Order for environment:
        //   ENV
        //   p4config
        //   p4envio
        //   win/user registry
        //   win/system registry

        // Ordering is important.
        String p4enviro = JreSettings.getEnv(PerforceEnvironment.P4ENVIRO, null);
        if (userWinRegistry != null) {
            if (p4enviro == null) {
                p4enviro = userWinRegistry.getP4EnviroFile();
            }
        }
        if (systemWinRegistry != null) {
            if (p4enviro == null) {
                p4enviro = systemWinRegistry.getP4EnviroFile();
            }
        }
        if (p4enviro == null) {
            // Default location for the p4enviro file, if nothing else defined it.
            p4enviro = PerforceEnvironment.DEFAULT_P4ENVIRO_FILE;
        }


        // P4ENVIRO loading
        FileConfigPart p4enviroPart = null;
        {
            if (p4enviro != null) {
                VirtualFile f = getRelFile(p4enviro);
                // The P4ENVIRO will have a default value if the user didn't specify one.
                // Therefore, if the file doesn't exist, don't complain.
                if (f != null && f.exists()) {
                    p4enviroPart = new FileConfigPart(vcsRoot, f);
                    p4enviroPart.reload();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Env defined P4ENVIRO: " + f + ": " +
                                ConfigPropertiesUtil.toProperties(p4enviroPart,
                                        "<unset>", "<empty>", "<set>"));
                    }
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Env defined P4ENVIRO: " + f + ", but it does not exist.");
                }
            }
        }

        // ENV overrides P4ENVRIO
        String p4config = null;
        if (JreSettings.getEnv(PerforceEnvironment.P4CONFIG) != null) {
            p4config = JreSettings.getEnv(PerforceEnvironment.P4CONFIG);
        }
        if (p4config != null && p4enviroPart != null && p4enviroPart.getP4Config() != null) {
            p4config = p4enviroPart.getP4Config();
        }
        if (p4config == null && userWinRegistry != null) {
            p4config = userWinRegistry.getP4ConfigFile();
        }
        if (p4config == null && systemWinRegistry != null) {
            p4config = systemWinRegistry.getP4ConfigFile();
        }



        // P4CONFIG loading
        ConfigPart p4configPart = null;
        {
            if (p4config != null) {
                if (p4config.indexOf('/') >= 0 || p4config.indexOf('\\') >= 0 || p4config.indexOf(File.separatorChar) >= 0) {
                    // File location
                    VirtualFile f = getRelFile(p4config);
                    if (f != null) {
                        final FileConfigPart envConf = new FileConfigPart(vcsRoot, f);
                        envConf.reload();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Env defined absolute P4CONFIG: " + f + ": " +
                                    ConfigPropertiesUtil.toProperties(envConf,
                                            "<unset>", "<empty>", "<set>"));
                        }
                        p4configPart = envConf;
                    }
                } else {
                    // Scan from the vcs root up through parents for a matching file name.
                    // TODO eventually allow for proper p4config loading, with multiple
                    // roots generated.
                    VirtualFile f = scanParentsForFile(vcsRoot, p4config);
                    if (f != null) {
                        FileConfigPart part = new FileConfigPart(vcsRoot, f);
                        part.reload();

                        p4configPart = part;
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Env defined relative P4CONFIG: " + p4config);
                    }
                }
            }
        }

        List<ConfigPart> parts = new ArrayList<>();
        parts.add(envData);
        parts.add(envPassword);
        if (p4configPart != null) {
            parts.add(p4configPart);
        }
        if (p4enviroPart != null) {
            parts.add(p4enviroPart);
        }
        if (userWinRegistry != null) {
            parts.add(userWinRegistry);
        }
        if (systemWinRegistry != null) {
            parts.add(systemWinRegistry);
        }
        MultipleConfigPart finalPart = new MultipleConfigPart("Environment Settings", parts);

        // Default configuration settings, if they are not set.
        if (!finalPart.hasAuthTicketFileSet()) {
            envData.setAuthTicketFile(getDefaultAuthTicketFile());
        }
        if (!finalPart.hasTrustTicketFileSet()) {
            envData.setTrustTicketFile(getDefaultTrustTicketFile());
        }

        // Need to recreate the part, because we just modified it.
        this.configParts = finalPart;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Final configuration from environment: " +
                    ConfigPropertiesUtil.toProperties(this.configParts,
                            "<unset>", "<empty>", "<set>"));
        }
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
            return JreSettings.getEnv(PerforceEnvironment.P4PASSWD);
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
    private VirtualFile getDefaultAuthTicketFile() {
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
    private VirtualFile getDefaultTrustTicketFile() {
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

    @Nullable
    private VirtualFile getFileAt(@NotNull String dir, @NotNull String name) {
        return LocalFileSystem.getInstance().findFileByPath(FileUtil.join(dir, name));
    }

    @Nullable
    private VirtualFile getRelFile(@NotNull String path) {
        if (FileUtil.isAbsolute(path)) {
            return LocalFileSystem.getInstance().findFileByPath(path);
        }
        return getFileAt(vcsRoot.getPath(), path);
    }

    private VirtualFile scanParentsForFile(@NotNull VirtualFile initialDir, @NotNull String fileName) {
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
