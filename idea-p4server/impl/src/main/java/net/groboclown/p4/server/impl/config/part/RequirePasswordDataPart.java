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

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.part.ConfigPartAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class RequirePasswordDataPart extends ConfigPartAdapter
        implements ConfigStateProvider {
    public RequirePasswordDataPart() {
        super("Require User Password");
    }

    // for ConfigStateProvider
    public RequirePasswordDataPart(String sourceName, VirtualFile vcsRoot, Map<String, String> values) {
        super(sourceName);
    }

    // Explicitly override other authentication methods
    @Override
    public boolean hasAuthTicketFileSet() {
        // By returning "true" here, we allow for this data part to
        // be the authoritative auth ticket file.  However, we also
        // return "null" for the auth ticket file, so the auth
        // ticket won't actually be used during authentication.

        return true;
    }

    @Nullable
    @Override
    public File getAuthTicketFile() {
        return null;
    }

    @Nullable
    @Override
    public String getPlaintextPassword() {
        return null;
    }

    @Override
    public boolean hasPasswordSet() {
        return false;
    }

    @Override
    public boolean requiresUserEnteredPassword() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return 2;
    }

    @NotNull
    @Override
    public Map<String, String> getState() {
        return Collections.emptyMap();
    }
}
