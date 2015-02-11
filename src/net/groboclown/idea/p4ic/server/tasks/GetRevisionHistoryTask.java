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
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.history.P4FileRevision;
import net.groboclown.idea.p4ic.server.P4Exec;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CancellationException;

public class GetRevisionHistoryTask extends ServerTask<List<P4FileRevision>> {
    private final Project project;
    private final P4FileInfo file;
    private final int maxRevs;

    public GetRevisionHistoryTask(Project project, @NotNull P4FileInfo file, int maxRevs) {
        this.project = project;
        this.file = file;
        this.maxRevs = maxRevs;
    }

    @Override
    public List<P4FileRevision> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        List<IFileSpec> depotFiles = P4FileInfo.toDepotList(Collections.singletonList(file));

        Map<IFileSpec, List<IFileRevisionData>> history = exec.getRevisionHistory(project, depotFiles, maxRevs);

        List<IFileSpec> findFiles = new ArrayList<IFileSpec>(history.size());
        for (Map.Entry<IFileSpec, List<IFileRevisionData>> e: history.entrySet()) {
            IFileSpec spec = e.getKey();
            if (
                    // History can return spec values that are essentially nulls.
                    // If loaded, these will cause issues
                    ! e.getValue().isEmpty() &&
                    !"null".equals(spec.getDepotPathString()) &&

                    // We also don't want to add in the initial file, because
                    // that FileInfo has already been loaded.
                    ! file.isSameFile(spec)) {
                findFiles.add(spec);
            }
        }
        List<P4FileInfo> files = new ArrayList<P4FileInfo>(exec.loadFileInfo(project, findFiles));
        files.add(file);

        List<P4FileRevision> ret = new ArrayList<P4FileRevision>();
        for (Map.Entry<IFileSpec, List<IFileRevisionData>> e: history.entrySet()) {
            if (
                    // nulls happen on a rename or delete
                    e.getValue() != null &&
                    ! e.getValue().isEmpty() &&
                    e.getKey() != null &&
                    ! "null".equals(e.getKey().getDepotPathString())) {
                // valid file
                for (P4FileInfo src : files) {
                    if (src.isSameFile(e.getKey())) {
                        for (IFileRevisionData rev: e.getValue()) {
                            ret.add(new P4FileRevision(project, src, rev));
                        }
                        break;
                    }
                }
            // Ignore - there's no information to give the UI here -
            // we have no changelist or date or revision or file or anything.
            //} else {
            //    ret.add(new P4FileRevision(project, null, null));
            }
        }

        return ret;
    }
}
