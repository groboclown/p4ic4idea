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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.part.ConfigPartAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A data part dedicated to selecting the client name.  It's separate
 * from the SimpleDataPart in that the UI component checks the current
 * configuration for a server to load the list of client names.
 */
public class ClientNameConfigPart
        extends ConfigPartAdapter
        implements ConfigStateProvider {
    private String clientName;
    private final transient List<ConfigProblem> additionalProblems = new ArrayList<>();

    public ClientNameConfigPart(@NotNull String sourceName) {
        super(sourceName);
    }

    public ClientNameConfigPart(@NotNull String sourceName, @NotNull VirtualFile root,
            @NotNull Map<String, String> stateValues) {
        super(sourceName);
        this.clientName = stateValues.get("c");
    }

    @Override
    public boolean reload() {
        // Nothing to do
        return false;
    }


    @Nonnull
    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        PartValidation validation = new PartValidation();
        validation.checkClientName(this, true);
        List<ConfigProblem> problems = new ArrayList<>(validation.getProblems());
        problems.addAll(additionalProblems);
        return problems;
    }


    @Override
    public boolean hasClientnameSet() {
        return true;
    }

    @Nullable
    @Override
    public String getClientname() {
        return clientName;
    }

    public void setClientname(@Nullable String clientName) {
        if (clientName == null) {
            this.clientName = null;
        } else {
            this.clientName = clientName.trim();
        }
    }


    public void addAdditionalProblem(@NotNull ConfigProblem problem) {
        additionalProblems.add(problem);
    }

    public void clearAdditionalProblems() {
        additionalProblems.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! getClass().equals(o.getClass())) {
            return false;
        }
        ClientNameConfigPart that = (ClientNameConfigPart) o;
        return StringUtil.equals(getClientname(), that.getClientname());
    }

    @Override
    public int hashCode() {
        if (getClientname() == null) {
            return 0;
        }
        return getClientname().hashCode();
    }

    @NotNull
    @Override
    public Map<String, String> getState() {
        Map<String, String> ret = new HashMap<>();
        ret.put("c", clientName);
        return ret;
    }
}
