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

package net.groboclown.idea.p4ic.config;

import net.groboclown.idea.p4ic.config.part.DataPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ClientConfigSetup {
    private final ClientConfig config;
    private final Collection<ConfigProblem> configProblems;
    private final DataPart source;

    public ClientConfigSetup(@Nullable ClientConfig config, @Nullable Collection<ConfigProblem> configProblems,
            @NotNull DataPart source) {
        this.config = config;
        this.source = source;
        Set<ConfigProblem> problems = new HashSet<ConfigProblem>(source.getConfigProblems());
        if (configProblems != null) {
            problems.addAll(configProblems);
        }
        this.configProblems = Collections.unmodifiableCollection(problems);
    }

    @Nullable
    public ClientConfig getClientConfig() {
        return config;
    }

    @NotNull
    public Collection<ConfigProblem> getConfigProblems() {
        return configProblems;
    }

    @NotNull
    public DataPart getSource() {
        return source;
    }

    public boolean hasProblems() {
        return ! configProblems.isEmpty();
    }

    public boolean hasClientConfig() {
        return config != null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
             return true;
        }
        if (o == null || ! (getClass().equals(o.getClass()))) {
            return false;
        }
        ClientConfigSetup that = (ClientConfigSetup) o;
        return config.equals(that.config);
        // "problems" shouldn't matter for this setup.
    }

    @Override
    public int hashCode() {
        return config != null ? config.hashCode() : source.hashCode();
    }
}