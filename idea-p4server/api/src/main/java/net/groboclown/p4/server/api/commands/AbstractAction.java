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
import net.groboclown.p4.server.api.Displayable;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.PreviousExecutionProblems;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractAction implements PreviousExecutionProblems, Displayable {
    private final List<P4CommandRunner.ResultError> problems = new ArrayList<>();

    @NotNull
    @Override
    public List<P4CommandRunner.ResultError> getPreviousExecutionProblems() {
        return Collections.unmodifiableList(problems);
    }

    @Override
    public void addExecutionProblem(@NotNull P4CommandRunner.ResultError error) {
        problems.add(error);
    }

    protected static String createActionId(Class<? extends AbstractAction> clazz) {
        return ActionUtil.createActionId(clazz);
    }

    @NotNull
    @Override
    public List<FilePath> getAffectedFiles() {
        return Collections.emptyList();
    }

    protected static String changeId(P4ChangelistId id) {
        // TODO use message catalog
        if (id == null) {
            return "?";
        }
        if (id.isDefaultChangelist()) {
            return "default";
        }
        if (id.getChangelistId() < 0) {
            return "new";
        }
        return Integer.toString(id.getChangelistId());
    }
}
