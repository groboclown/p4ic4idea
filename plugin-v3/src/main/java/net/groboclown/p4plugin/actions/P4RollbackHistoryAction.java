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

package net.groboclown.idea.p4ic.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4ApiException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Rollback a file to a specific revision.
 */
public class P4RollbackHistoryAction extends BasicAction {
    private static final Logger LOG = Logger.getInstance(P4RollbackHistoryAction.class);
    public static final String ACTION_NAME = "Rollback";

    @Override
    protected void perform(@NotNull final Project project, @NotNull final P4Vcs vcs,
            @NotNull final List<VcsException> exceptions,
            @NotNull final List<VirtualFile> affectedFiles) {
        if (affectedFiles.isEmpty()) {
            return;
        }
        if (affectedFiles.size() > 1) {
            exceptions.add(new P4ApiException(P4Bundle.message("exception.rollback.history")));
            return;
        }
        LOG.debug("perform rollback for " + affectedFiles);

        // TODO get the head revision and the rollback revision.
        exceptions.add(new P4InvalidConfigException("not implemented yet"));
    }

    @NotNull
    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected boolean isEnabled(@NotNull final Project project, @NotNull final P4Vcs vcs,
            @NotNull final VirtualFile... vFiles) {
        // TODO return false if this is the latest version.
        LOG.debug("is enabled for " + Arrays.asList(vFiles));

        return true;
    }
}
