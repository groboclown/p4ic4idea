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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.P4Exec;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.P4Job;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

public class SubmitRunner extends ServerTask<List<P4StatusMessage>> {
    @NotNull
    private final Project project;

    @NotNull
    private final List<FilePath> actualFiles;

    @NotNull
    private final List<String> jobIds;

    @Nullable
    private final String jobStatus;

    private final int changelistId;

    public SubmitRunner(
            @NotNull Project project, @Nullable List<FilePath> actualFiles,
            @Nullable Collection<P4Job> jobs,
            @Nullable String jobStatus,
            int changelistId) {
        this.project = project;
        if (actualFiles == null) {
            actualFiles = Collections.emptyList();
        }
        this.actualFiles = actualFiles;
        if (jobs == null) {
            this.jobIds = Collections.emptyList();
        } else {
            this.jobIds = new ArrayList<String>(jobs.size());
            for (P4Job job: jobs) {
                jobIds.add(job.getJobId());
            }
        }
        this.jobStatus = jobStatus;
        this.changelistId = changelistId;
        assert changelistId > 0;
    }

    @Override
    public List<P4StatusMessage> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        IChangelist changelist = exec.getChangelist(project, changelistId);
        if (changelist == null) {
            throw new P4Exception("changelist does not exist: " + changelistId);
        }
        //if (! changelist.getUsername().equals(exec.getOwnerName())) {
        //    throw new P4Exception("changelist not owned by client");
        //}
        if (changelist.getStatus() == ChangelistStatus.SUBMITTED) {
            throw new P4Exception("changelist is already submitted");
        }
        // Should do host check, too, to ensure the client host matches.


        //throw new P4Exception("submit not supported at the moment (it crashes the server)");

        // Manipulate the changelist such that only these
        // files are in the change.  The responsibility of sorting the
        // files into changelists, and putting default changelists into
        // real changelists, is the responsibility of the caller.

        List<P4FileInfo> files = exec.getFilesInChangelist(project, changelistId);
        if (files != null) {
            // Add the missing files to the changelist
            List<IFileSpec> toMoveFiles = new ArrayList<IFileSpec>();
            for (P4FileInfo file : files) {
                if (! actualFiles.contains(file.getPath())) {
                    // Found a file in the changelist that is not requested
                    // to be submitted.
                    toMoveFiles.add(file.toDepotSpec());
                }
            }
            if (! toMoveFiles.isEmpty()) {
                final List<P4StatusMessage> messages =
                        exec.reopenFiles(project, toMoveFiles, IChangelist.DEFAULT, null);
                boolean hasError = false;
                for (P4StatusMessage message : messages) {
                    if (message.isError()) {
                        hasError = true;
                        break;
                    }
                }
                if (hasError) {
                    return messages;
                }
            }
        }


        return exec.submit(project, changelistId, jobIds, jobStatus);

        // perforce mapping will be cleaned up automatically
    }
}
