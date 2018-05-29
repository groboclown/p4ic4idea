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

package net.groboclown.p4.server.api;

import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MockConfigPart
        implements ConfigPart {
    private String sourceName = "blah";
    private P4ServerName serverName;
    private String clientname;
    private String username;
    private boolean hasPasswordSet;
    private String password;
    private File authTicketFile;
    private File trustTicketFile;
    private String serverFingerprint;
    private String clientHostname;
    private String ignoreFileName;
    private String defaultCharset;
    private String loginSso;
    private boolean reloadValue = false;
    private boolean requiresUserEnteredPassword;
    private Collection<ConfigProblem> configProblems = new ArrayList<>();

    public MockConfigPart copy() {
        MockConfigPart ret = new MockConfigPart()
                .withSourceName(sourceName)
                .withP4ServerName(serverName)
                .withClientname(clientname)
                .withUsername(username)
                .withAuthTicketFile(authTicketFile)
                .withTrustTicketFile(trustTicketFile)
                .withServerFingerprint(serverFingerprint)
                .withClientHostname(clientHostname)
                .withIgnoreFileName(ignoreFileName)
                .withDefaultCharset(defaultCharset)
                .withLoginSso(loginSso)
                .withReloadValue(reloadValue)
                .withRequiresUserEnteredPassword(requiresUserEnteredPassword);
        if (hasPasswordSet) {
            ret.withPassword(password);
        } else {
            ret.withNoPassword();
        }
        return ret;
    }

    @Nls
    @NotNull
    @Override
    public String getSourceName() {
        return sourceName;
    }

    public MockConfigPart withSourceName(@NotNull String s) {
        sourceName = s;
        return this;
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

    public MockConfigPart withServerName(String s) {
        serverName = P4ServerName.forPort(s);
        return this;
    }

    public MockConfigPart withP4ServerName(@Nullable P4ServerName name) {
        this.serverName = name;
        return this;
    }

    @Override
    public boolean hasClientnameSet() {
        return clientname != null;
    }

    @Nullable
    @Override
    public String getClientname() {
        return clientname;
    }

    public MockConfigPart withClientname(String s) {
        clientname = s;
        return this;
    }

    @Override
    public boolean hasUsernameSet() {
        return username != null;
    }

    @Nullable
    @Override
    public String getUsername() {
        return username;
    }

    public MockConfigPart withUsername(String s) {
        username = s;
        return this;
    }

    @Override
    public boolean hasPasswordSet() {
        return hasPasswordSet;
    }

    @Nullable
    @Override
    public String getPlaintextPassword() {
        return password;
    }

    public MockConfigPart withNoPassword() {
        hasPasswordSet = false;
        password = null;
        return this;
    }

    public MockConfigPart withPassword(String s) {
        hasPasswordSet = true;
        password = s;
        return this;
    }

    @Override
    public boolean requiresUserEnteredPassword() {
        return requiresUserEnteredPassword;
    }

    public MockConfigPart withRequiresUserEnteredPassword(boolean b) {
        requiresUserEnteredPassword = b;
        return this;
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        return authTicketFile != null;
    }

    @Nullable
    @Override
    public File getAuthTicketFile() {
        return authTicketFile;
    }

    public MockConfigPart withAuthTicketFile(File f) {
        authTicketFile = f;
        return this;
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        return trustTicketFile != null;
    }

    @Nullable
    @Override
    public File getTrustTicketFile() {
        return trustTicketFile;
    }

    public MockConfigPart withTrustTicketFile(File f) {
        trustTicketFile = f;
        return this;
    }

    @Override
    public boolean hasServerFingerprintSet() {
        return serverFingerprint != null;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        return serverFingerprint;
    }

    public MockConfigPart withServerFingerprint(String s) {
        serverFingerprint = s;
        return this;
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

    public MockConfigPart withClientHostname(String s) {
        clientHostname = s;
        return this;
    }

    @Override
    public boolean hasIgnoreFileNameSet() {
        return ignoreFileName != null;
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        return ignoreFileName;
    }

    public MockConfigPart withIgnoreFileName(String s) {
        ignoreFileName = s;
        return this;
    }

    @Override
    public boolean hasDefaultCharsetSet() {
        return defaultCharset != null;
    }

    @Nullable
    @Override
    public String getDefaultCharset() {
        return defaultCharset;
    }

    public MockConfigPart withDefaultCharset(String s) {
        defaultCharset = s;
        return this;
    }

    @Override
    public boolean hasLoginSsoSet() {
        return loginSso != null;
    }

    @Nullable
    @Override
    public String getLoginSso() {
        return loginSso;
    }

    public MockConfigPart withLoginSso(String s) {
        loginSso = s;
        return this;
    }

    @Override
    public boolean reload() {
        return reloadValue;
    }

    public MockConfigPart withReloadValue(boolean b) {
        reloadValue = b;
        return this;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        return configProblems;
    }

    @Override
    public boolean hasError() {
        for (ConfigProblem problem : getConfigProblems()) {
            if (problem.isError()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getRawPort() {
        return serverName.getFullPort();
    }

    public MockConfigPart withConfigProblems(ConfigProblem... problems) {
        configProblems = Arrays.asList(problems);
        return this;
    }
}
