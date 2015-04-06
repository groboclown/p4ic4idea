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
package net.groboclown.idea.p4ic.server.tasks;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.P4Exec;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CancellationException;

public class DeleteChangelistTask extends ServerTask<Void> {
    private final Project project;
    private final int changelistId;

    public DeleteChangelistTask(@NotNull Project project, int changelistId) {
        this.project = project;
        this.changelistId = changelistId;
    }

    @Override
    public Void run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        // Reload the changelist to get the latest status
        IChangelist current = exec.getChangelist(project, changelistId);
        if (current == null || current.getStatus() == ChangelistStatus.SUBMITTED || current.getId() <= 0) {
            // nothing to do
            return null;
        }

        // First, check if there are shelved changes.  If so,
        // we need to cancel, because it's up to the user
        // to handle the shelved changes.
        if (current.isShelved()) {
            throw new P4Exception("Perforce changelist is shelved");
        }

        List<IFileSpec> files = exec.getFileSpecsInChangelist(project, current.getId());
        if (files != null && ! files.isEmpty()) {
            // need to move the files into the default changelist.
            final List<P4StatusMessage> messages =
                    exec.reopenFiles(project, files, IChangelist.DEFAULT, null);
            P4StatusMessage.throwIfError(messages, false);
        }

        String res = exec.deletePendingChangelist(project, current.getId());
        log("deletePendingChangelist: returned [" + res + "]");
        // This is usually in the form "Change X deleted".  Can't really
        // parse errors with this.

        return null;
    }
}
