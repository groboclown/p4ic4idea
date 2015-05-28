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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.history.P4FileRevision;
import net.groboclown.idea.p4ic.server.P4Exec;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;

public class GetRevisionHistoryTask extends ServerTask<List<P4FileRevision>> {
    private static final Logger LOG = Logger.getInstance(GetRevisionHistoryTask.class);

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

        // TODO there's a bug here where getting the revision for a file with a special character won't return
        // any history.  e.g.
        // //depot/projecta/hotfix/a/test%23one.txt returns map {null=null}

        Map<IFileSpec, List<IFileRevisionData>> history =
                exec.getRevisionHistory(project, depotFiles, maxRevs);
        LOG.info("history for " + file + ": " + history);

        List<P4FileRevision> ret = new ArrayList<P4FileRevision>();
        for (Entry<IFileSpec, List<IFileRevisionData>> entry : history.entrySet()) {
            if (entry.getValue() == null) {
                LOG.info("history for " + file + ": null values for " + entry.getKey());
            } else {
                for (IFileRevisionData rev : entry.getValue()) {
                    if (rev != null) {
                        ret.add(createRevision(exec, entry.getKey(), rev));
                    }
                }
            }
        }

        // Note that these are not sorted.  Sort by date.
        Collections.sort(ret, REV_COMPARE);

        return ret;
    }


    @NotNull
    private P4FileRevision createRevision(@NotNull P4Exec exec, @Nullable IFileSpec spec,
            @NotNull final IFileRevisionData rev) throws VcsException {
        if ((spec == null || spec.getDepotPathString() == null) && rev.getDepotFileName() == null) {
            throw new P4FileException(P4Bundle.message("exception.find-file.failed", rev.getDepotFileName()));
        }
        LOG.info("Finding location of " + spec);
        // Note: check above performs the NPE checks.
        return new P4FileRevision(project, exec, file.getDepotPath(),
                rev.getDepotFileName() == null ? spec.getDepotPathString() : rev.getDepotFileName(),
                rev);
    }

    private static final RevCompare REV_COMPARE = new RevCompare();

    private static class RevCompare implements Comparator<P4FileRevision> {

        @Override
        public int compare(final P4FileRevision o1, final P4FileRevision o2) {
            // compare in reverse order
            return o2.getRevisionDate().compareTo(o1.getRevisionDate());
        }
    }

}
