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
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.P4ServerName;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class RequirePasswordDataPart implements DataPart {
    static final String TAG_NAME = "require-password-data-part";
    static final ConfigPartFactory<RequirePasswordDataPart> FACTORY = new Factory();

    // Explicitly override other authentication methods
    @Override
    public boolean hasAuthTicketFileSet() {
        return true;
    }

    @NotNull
    @Override
    public Element marshal() {
        return new Element(TAG_NAME);
    }


    private static class Factory extends ConfigPartFactory<RequirePasswordDataPart> {
        @Override
        RequirePasswordDataPart create(@NotNull Project project, @NotNull Element element) {
            return new RequirePasswordDataPart();
        }
    }


    @Override
    public boolean reload() {
        // Do nothing
        return true;
    }


    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public VirtualFile getRootPath() {
        return null;
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
}
