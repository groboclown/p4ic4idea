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

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ConfigProblem;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MultipleConfigPart
        implements ConfigPart {
    private static final Logger LOG = Logger.getInstance(MultipleConfigPart.class);

    private static final AtomicInteger COUNT = new AtomicInteger(0);

    private final String sourceName;
    private final List<ConfigPart> parts;
    private final int index = COUNT.incrementAndGet();

    public MultipleConfigPart(@NotNull @Nls(capitalization = Nls.Capitalization.Title) String sourceName,
            @NotNull List<ConfigPart> parts) {
        this.sourceName = sourceName;
        this.parts = new ArrayList<>(parts);
    }

    @TestOnly
    int getInstanceIndex() {
        return index;
    }

    @Nls
    @NotNull
    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public boolean reload() {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || ! getClass().equals(o.getClass())) {
            return false;
        }
        MultipleConfigPart that = (MultipleConfigPart) o;
        return getSourceName().equals(that.getSourceName()) &&
                (parts.equals(that.parts));
    }

    @Override
    public int hashCode() {
        return (sourceName.hashCode()) + parts.hashCode();
    }

    @Override
    public String toString() {
        return "MultipleConfigPart(" + sourceName + "):" + index;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        if (LOG.isDebugEnabled()) {
            LOG.debug(this + ": finding config problems");
        }
        Set<ConfigProblem> problems = new HashSet<>();

        for (ConfigPart part : parts) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(part + " P4USER: " + part.hasUsernameSet() + " - " + part.getUsername());
                LOG.debug(part + " P4PORT: " + part.hasServerNameSet() + " - " + part.getServerName());
                LOG.debug(part + " P4CLIENT: " + part.hasClientnameSet() + " - " + part.getClientname());
                LOG.debug(part + " P4HOST: " + part.hasClientHostnameSet() + " - " + part.getClientHostname());
            }

            problems.addAll(part.getConfigProblems());
        }
        return problems;
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
    public boolean hasServerNameSet() {
        for (ConfigPart part : parts) {
            if (part.hasServerNameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public P4ServerName getServerName() {
        for (ConfigPart part : parts) {
            if (part.hasServerNameSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using server name from " + part);
                }
                return part.getServerName();
            }
        }
        return null;
    }

    @Override
    public boolean hasClientnameSet() {
        for (ConfigPart part : parts) {
            if (part.hasClientnameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getClientname() {
        for (ConfigPart part : parts) {
            if (part.hasClientnameSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using client name from " + part);
                }
                return part.getClientname();
            }
        }
        return null;
    }

    @Override
    public boolean hasUsernameSet() {
        for (ConfigPart part : parts) {
            if (part.hasUsernameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getUsername() {
        for (ConfigPart part : parts) {
            if (part.hasUsernameSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using user name from " + part);
                }
                return part.getUsername();
            }
        }
        return null;
    }

    @Override
    public boolean hasPasswordSet() {
        for (ConfigPart part : parts) {
            if (part.hasPasswordSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getPlaintextPassword() {
        for (ConfigPart part : parts) {
            if (part.hasPasswordSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using plaintext password from " + part);
                }
                return part.getPlaintextPassword();
            }
        }
        return null;
    }

    @Override
    public boolean requiresUserEnteredPassword() {
        for (ConfigPart part : parts) {
            if (part.requiresUserEnteredPassword()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        for (ConfigPart part : parts) {
            if (part.hasAuthTicketFileSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public File getAuthTicketFile() {
        for (ConfigPart part : parts) {
            if (part.hasAuthTicketFileSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using auth ticket file from " + part);
                }
                return part.getAuthTicketFile();
            }
        }
        return null;
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        for (ConfigPart part : parts) {
            if (part.hasTrustTicketFileSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public File getTrustTicketFile() {
        for (ConfigPart part : parts) {
            if (part.hasTrustTicketFileSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using trust ticket file from " + part);
                }
                return part.getTrustTicketFile();
            }
        }
        return null;
    }

    @Override
    public boolean hasServerFingerprintSet() {
        for (ConfigPart part : parts) {
            if (part.hasServerFingerprintSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        for (ConfigPart part : parts) {
            if (part.hasServerFingerprintSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using server fingerprint from " + part);
                }
                return part.getServerFingerprint();
            }
        }
        return null;
    }

    @Override
    public boolean hasClientHostnameSet() {
        for (ConfigPart part : parts) {
            if (part.hasClientHostnameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        for (ConfigPart part : parts) {
            if (part.hasClientHostnameSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using client hostname from " + part);
                }
                return part.getClientHostname();
            }
        }
        return null;
    }

    @Override
    public boolean hasIgnoreFileNameSet() {
        for (ConfigPart part : parts) {
            if (part.hasIgnoreFileNameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        for (ConfigPart part : parts) {
            if (part.hasIgnoreFileNameSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using ignore file name from " + part);
                }
                return part.getIgnoreFileName();
            }
        }
        return null;
    }

    @Override
    public boolean hasDefaultCharsetSet() {
        for (ConfigPart part : parts) {
            if (part.hasDefaultCharsetSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getDefaultCharset() {
        for (ConfigPart part : parts) {
            if (part.hasDefaultCharsetSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using default charset from " + part);
                }
                return part.getDefaultCharset();
            }
        }
        return null;
    }

    @Override
    public boolean hasLoginSsoSet() {
        for (ConfigPart part : parts) {
            if (part.hasLoginSsoSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getLoginSso() {
        for (ConfigPart part : parts) {
            if (part.hasLoginSsoSet()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": using login sso from " + part);
                }
                return part.getLoginSso();
            }
        }
        return null;
    }
}
