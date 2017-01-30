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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.P4ServerName;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultipleDataPart implements DataPart {
    private static final Logger LOG = Logger.getInstance(MultipleDataPart.class);

    private final VirtualFile root;
    private final List<DataPart> parts;

    public MultipleDataPart(@Nullable VirtualFile root, @NotNull List<DataPart> parts) {
        this.root = root;
        this.parts = parts;
    }

    @NotNull
    @Override
    public Element marshal() {
        throw new IllegalStateException("Should not be called");
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
        MultipleDataPart that = (MultipleDataPart) o;
        return ((root == null && that.root == null)
                || (root != null && root.equals(that.root))) &&
                (parts.equals(that.parts));
    }

    @Override
    public int hashCode() {
        return (root == null ? 0 : root.hashCode()) + parts.hashCode();
    }

    @Override
    public String toString() {
        return "MultipleDataPart(" + root + ")";
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        LOG.info("Config for MultipleDataPart under " + root);

        Set<ConfigProblem> problems = new HashSet<ConfigProblem>();
        for (DataPart part : parts) {
            LOG.info(part + " P4USER: " + part.hasUsernameSet() + " - " + part.getUsername());
            LOG.info(part + " P4PORT: " + part.hasServerNameSet() + " - " + part.getServerName());
            LOG.info(part + " P4CLIENT: " + part.hasClientnameSet() + " - " + part.getClientname());
            LOG.info(part + " P4HOST: " + part.hasClientHostnameSet() + " - " + part.getClientHostname());

            problems.addAll(part.getConfigProblems());
        }
        return problems;
    }

    @Nullable
    @Override
    public VirtualFile getRootPath() {
        return root;
    }

    @Override
    public boolean hasServerNameSet() {
        for (DataPart part : parts) {
            if (part.hasServerNameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public P4ServerName getServerName() {
        for (DataPart part : parts) {
            if (part.hasServerNameSet()) {
                return part.getServerName();
            }
        }
        return null;
    }

    @Override
    public boolean hasClientnameSet() {
        for (DataPart part : parts) {
            if (part.hasClientnameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getClientname() {
        for (DataPart part : parts) {
            if (part.hasClientnameSet()) {
                return part.getClientname();
            }
        }
        return null;
    }

    @Override
    public boolean hasUsernameSet() {
        for (DataPart part : parts) {
            if (part.hasUsernameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getUsername() {
        for (DataPart part : parts) {
            if (part.hasUsernameSet()) {
                return part.getUsername();
            }
        }
        return null;
    }

    @Override
    public boolean hasPasswordSet() {
        for (DataPart part : parts) {
            if (part.hasPasswordSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getPlaintextPassword() {
        for (DataPart part : parts) {
            if (part.hasPasswordSet()) {
                return part.getPlaintextPassword();
            }
        }
        return null;
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        for (DataPart part : parts) {
            if (part.hasAuthTicketFileSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public File getAuthTicketFile() {
        for (DataPart part : parts) {
            if (part.hasAuthTicketFileSet()) {
                return part.getAuthTicketFile();
            }
        }
        return null;
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        for (DataPart part : parts) {
            if (part.hasTrustTicketFileSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public File getTrustTicketFile() {
        for (DataPart part : parts) {
            if (part.hasTrustTicketFileSet()) {
                return part.getTrustTicketFile();
            }
        }
        return null;
    }

    @Override
    public boolean hasServerFingerprintSet() {
        for (DataPart part : parts) {
            if (part.hasServerFingerprintSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        for (DataPart part : parts) {
            if (part.hasServerFingerprintSet()) {
                return part.getServerFingerprint();
            }
        }
        return null;
    }

    @Override
    public boolean hasClientHostnameSet() {
        for (DataPart part : parts) {
            if (part.hasClientHostnameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        for (DataPart part : parts) {
            if (part.hasClientHostnameSet()) {
                return part.getClientHostname();
            }
        }
        return null;
    }

    @Override
    public boolean hasIgnoreFileNameSet() {
        for (DataPart part : parts) {
            if (part.hasIgnoreFileNameSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        for (DataPart part : parts) {
            if (part.hasIgnoreFileNameSet()) {
                return part.getIgnoreFileName();
            }
        }
        return null;
    }

    @Override
    public boolean hasDefaultCharsetSet() {
        for (DataPart part : parts) {
            if (part.hasDefaultCharsetSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getDefaultCharset() {
        for (DataPart part : parts) {
            if (part.hasDefaultCharsetSet()) {
                return part.getDefaultCharset();
            }
        }
        return null;
    }

    @Override
    public boolean hasLoginSsoSet() {
        for (DataPart part : parts) {
            if (part.hasLoginSsoSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public File getLoginSso() {
        for (DataPart part : parts) {
            if (part.hasLoginSsoSet()) {
                return part.getLoginSso();
            }
        }
        return null;
    }
}
