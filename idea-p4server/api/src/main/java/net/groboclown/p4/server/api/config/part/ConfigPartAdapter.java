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

package net.groboclown.p4.server.api.config.part;

import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ConfigProblem;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * Basic adapter to implement all the of the {@link ConfigPart} methods.  {@link #hasError()} is
 * fully implemented to examine the reported {@link #getConfigProblems()}.  {@link #toString()}
 * is implemented, but only reports the class name.
 */
public class ConfigPartAdapter
        implements ConfigPart {
    private final String sourceName;

    public ConfigPartAdapter(@NotNull @Nls(capitalization = Nls.Capitalization.Title) String sourceName) {
        this.sourceName = sourceName;
    }

    @Nls
    @NotNull
    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public boolean hasServerNameSet() {
        return false;
    }

    @Nullable
    @Override
    public P4ServerName getServerName() {
        return null;
    }

    @Override
    public boolean hasClientnameSet() {
        return false;
    }

    @Nullable
    @Override
    public String getClientname() {
        return null;
    }

    @Override
    public boolean hasUsernameSet() {
        return false;
    }

    @Nullable
    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public boolean hasPasswordSet() {
        return false;
    }

    @Nullable
    @Override
    public String getPlaintextPassword() {
        return null;
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        return false;
    }

    @Nullable
    @Override
    public File getAuthTicketFile() {
        return null;
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        return false;
    }

    @Nullable
    @Override
    public File getTrustTicketFile() {
        return null;
    }

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
        return false;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        return null;
    }

    @Override
    public boolean hasIgnoreFileNameSet() {
        return false;
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        return null;
    }

    @Override
    public boolean hasDefaultCharsetSet() {
        return false;
    }

    @Nullable
    @Override
    public String getDefaultCharset() {
        return null;
    }

    @Override
    public boolean hasLoginSsoSet() {
        return false;
    }

    @Nullable
    @Override
    public String getLoginSso() {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
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
        return null;
    }

    @Override
    public boolean requiresUserEnteredPassword() {
        return false;
    }
}
