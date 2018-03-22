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

package net.groboclown.idea.p4ic.v2.history;

import com.intellij.diagnostic.LogEventException;
import com.intellij.diagnostic.ThreadDumper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import net.groboclown.idea.p4ic.v2.changes.P4CommittedChangeList;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * The full details of a committed changelist, including the jobs,
 * files, author, and description.
 */
public class P4CommittedChangeListDetails {
    private final P4CommittedChangeList changelist;
    private final Collection<P4ChangeListJob> jobs;
    private final Collection<FileChange> changes;

    public static class FileChange {
        private final String srcDepotPath;
        private final String destDepotPath;
        private final FileStatus status;

        private FileChange(Change change) {
            ContentRevision before = change.getBeforeRevision();
            ContentRevision after = change.getAfterRevision();
            if (before != null) {
                if (before instanceof P4ContentRevision) {
                    srcDepotPath = ((P4ContentRevision) before).getDepotPath();
                } else {
                    srcDepotPath = before.getFile().getPath();
                }
            } else {
                srcDepotPath = null;
            }

            if (after != null) {
                if (after instanceof P4ContentRevision) {
                    destDepotPath = ((P4ContentRevision) after).getDepotPath();
                } else {
                    destDepotPath = after.getFile().getPath();
                }
            } else {
                destDepotPath = null;
            }

            if (before != null && after != null) {
                if (srcDepotPath.equals(destDepotPath)) {
                    status = FileStatus.MODIFIED;
                } else {
                    status = FileStatus.MERGE;
                }
            } else if (before == null && after != null) {
                status = FileStatus.ADDED;
            } else if (after == null && before != null) {
                status = FileStatus.DELETED;
            } else {
                // weird state - both are null
                status = FileStatus.UNKNOWN;
            }
        }

        public FileStatus getStatus() {
            return status;
        }

        public String getSource() {
            return srcDepotPath;
        }

        public String getDestination() {
            return destDepotPath;
        }

        /**
         *
         * @return the depot path that the user actually cares about.
         */
        public String getPrimaryPath() {
            if (destDepotPath != null) {
                return destDepotPath;
            }
            return srcDepotPath;
        }
    }


    @Nullable
    public static P4CommittedChangeListDetails create(P4FileRevision rev)
            throws InterruptedException {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            final Attachment dump = new Attachment("threadDump.txt", ThreadDumper.dumpThreadsToString());
            throw new LogEventException("Access is allowed from event dispatch thread only.",
                    " EventQueue.isDispatchThread()=" + EventQueue.isDispatchThread() +
                    " isDispatchThread()=true" +
                    " Toolkit.getEventQueue()=" + Toolkit.getDefaultToolkit().getSystemEventQueue() +
                    " Current thread: " + Thread.currentThread(),
                    dump);
        }

        P4Server server = rev.getServer();
        if (server != null && server.isWorkingOnline()) {
            final P4CommittedChangeList cl = server.getChangelistForOnline(rev.getChangeListId());
            if (cl == null) {
                return null;
            }
            final Collection<P4ChangeListJob> jobs = server.getJobsInChangelistForOnline(rev.getChangeListId());
            return new P4CommittedChangeListDetails(cl, jobs);
        }
        return null;
    }

    private P4CommittedChangeListDetails(@NotNull P4CommittedChangeList changelist,
            @NotNull Collection<P4ChangeListJob> jobs) {
        this.changelist = changelist;
        this.jobs = Collections.unmodifiableCollection(jobs);
        List<FileChange> changes = new ArrayList<FileChange>();
        for (Change change : changelist.getChanges()) {
            changes.add(new FileChange(change));
        }
        this.changes = Collections.unmodifiableCollection(changes);
    }

    @NotNull
    public String getComment() {
        String desc = changelist.getComment();
        if (desc == null || desc.isEmpty()) {
            // Should never happen
            return P4Bundle.getString("changelist.detail.empty-comment");
        }
        return desc;
    }

    @NotNull
    public String getAuthor() {
        return changelist.getCommitterName();
    }

    @NotNull
    public Date getDate() {
        return changelist.getCommitDate();
    }

    @Nullable
    public VcsRevisionNumber getChangeListId() {
        return changelist.getRevisionNumber();
    }

    @NotNull
    public Collection<P4ChangeListJob> getJobs() {
        return jobs;
    }

    @NotNull
    public Collection<FileChange> getChanges() {
        return changes;
    }
}
