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

package net.groboclown.p4.server.api.commands;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class AbstractNonCachedServerAction<R extends P4CommandRunner.ServerResult> implements P4CommandRunner.ServerAction<R> {
    private final String actionId = ActionUtil.createActionId(getClass());

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    @NotNull
    @Override
    public String[] getDisplayParameters() {
        return EMPTY;
    }

    @NotNull
    @Override
    public List<FilePath> getAffectedFiles() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<P4CommandRunner.ResultError> getPreviousExecutionProblems() {
        return Collections.emptyList();
    }

    @Override
    public void addExecutionProblem(@NotNull P4CommandRunner.ResultError error) {
        // Do nothing
    }
}
