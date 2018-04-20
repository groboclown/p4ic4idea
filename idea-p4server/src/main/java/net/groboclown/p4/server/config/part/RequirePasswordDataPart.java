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

package net.groboclown.p4.server.config.part;

import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class RequirePasswordDataPart extends DataPartAdapter {
    static final String TAG_NAME = "require-password-data-part";
    static final ConfigPartFactory<RequirePasswordDataPart> FACTORY = new Factory();

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
    public boolean equals(Object o) {
        return o != null && getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return 2;
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
}
