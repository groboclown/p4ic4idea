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

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.part.DataPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MockDataPart implements DataPart {
    private VirtualFile rootPath;
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

    @NotNull
    public Collection<ConfigProblem> configProblems = new ArrayList<>();

    @Nullable
    @Override
    public VirtualFile getRootPath() {
        return rootPath;
    }

    public MockDataPart withRootPath(VirtualFile vf) {
        rootPath = vf;
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

    public MockDataPart withServerName(String s) {
        serverName = P4ServerName.forPort(s);
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

    public MockDataPart withClientname(String s) {
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

    public MockDataPart withUsername(String s) {
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

    public MockDataPart withNoPassword() {
        hasPasswordSet = false;
        password = null;
        return this;
    }

    public MockDataPart withPassword(String s) {
        hasPasswordSet = true;
        password = s;
        return this;
    }

    @Override
    public boolean requiresUserEnteredPassword() {
        return requiresUserEnteredPassword;
    }

    public MockDataPart withRequiresUserEnteredPassword(boolean b) {
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

    public MockDataPart withAuthTicketFile(File f) {
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

    public MockDataPart withTrustTicketFile(File f) {
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

    public MockDataPart withServerFingerprint(String s) {
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

    public MockDataPart withClientHostname(String s) {
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

    public MockDataPart withIgnoreFileName(String s) {
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

    public MockDataPart withDefaultCharset(String s) {
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

    public MockDataPart withLoginSso(String s) {
        loginSso = s;
        return this;
    }

    @Override
    public boolean reload() {
        return reloadValue;
    }

    public MockDataPart withReloadValue(boolean b) {
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

    public MockDataPart withConfigProblems(ConfigProblem... problems) {
        configProblems = Arrays.asList(problems);
        return this;
    }
}
