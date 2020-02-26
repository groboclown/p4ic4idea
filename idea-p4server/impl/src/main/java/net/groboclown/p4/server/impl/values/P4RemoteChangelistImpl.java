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

package net.groboclown.p4.server.impl.values;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4ChangelistType;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class P4RemoteChangelistImpl implements P4RemoteChangelist {
    private final P4ChangelistId changelistId;
    private final P4ChangelistSummary summary;
    private final String comment;
    private final boolean deleted;
    private final boolean onServer;
    private final boolean shelved;
    private final Date submittedDate;
    private final P4ChangelistType changelistType;
    private final String clientname;
    private final String username;
    private final List<P4Job> attachedJobs;
    private final JobStatus jobStatus;
    private final List<CommittedFile> files;

    public static class Builder {
        private P4ChangelistId changelistId;
        private P4ChangelistSummary summary;
        private String comment;
        private boolean deleted;
        private boolean onServer;
        private boolean shelved;
        private Date submittedDate;
        private P4ChangelistType changelistType;
        private String clientname;
        private String username;
        private List<P4Job> attachedJobs;
        private JobStatus jobStatus;
        private List<CommittedFile> files;

        public Builder withChangelistId(P4ChangelistId id) {
            this.changelistId = id;
            return this;
        }

        public Builder withSummary(P4ChangelistSummary summary) {
            this.summary = summary;
            return this;
        }

        public Builder withComment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder withDeleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder withOnServer(boolean onServer) {
            this.onServer = onServer;
            return this;
        }

        public Builder withShelved(boolean shelved) {
            this.shelved = shelved;
            return this;
        }

        public Builder withSubmittedDate(Date date) {
            this.submittedDate = date;
            return this;
        }

        public Builder withJobStatus(JobStatus status) {
            this.jobStatus = status;
            return this;
        }

        public Builder withChangelist(OptionalClientServerConfig config, IChangelist changelist) {
            ClientServerRef ref = new ClientServerRef(config.getServerName(), changelist.getClientId());
            this.changelistId = new P4ChangelistIdImpl(changelist.getId(), ref);
            this.summary = new P4ChangelistSummaryImpl(ref, changelist);
            this.comment = changelist.getDescription();
            this.deleted = false;
            this.onServer = true;
            this.shelved = changelist.isShelved();
            this.submittedDate = changelist.getDate();
            this.changelistType = (changelist.getVisibility() == IChangelistSummary.Visibility.PUBLIC
                    ? P4ChangelistType.PUBLIC
                    : (changelist.getVisibility() == IChangelistSummary.Visibility.RESTRICTED
                            ? P4ChangelistType.RESTRICTED
                            : P4ChangelistType.UNKNOWN));
            this.clientname = changelist.getClientId();
            this.username = changelist.getUsername();
            return this;
        }

        public Builder withJobs(Collection<IJob> jobs) {
            this.attachedJobs = P4JobImpl.createFor(jobs);
            this.jobStatus = null; // FIXME how to determine?
            return this;
        }

        public Builder withFiles(List<IFileSpec> files) {
            this.files = files.stream()
                    .map(Committed::new)
                    .collect(Collectors.toList());
            return this;
        }

        public P4RemoteChangelist build() {
            return new P4RemoteChangelistImpl(changelistId, summary, comment, deleted, onServer,
                    shelved, submittedDate, changelistType, clientname, username, attachedJobs,
                    jobStatus, files);
        }
    }


    private P4RemoteChangelistImpl(P4ChangelistId changelistId,
            P4ChangelistSummary summary, String comment, boolean deleted, boolean onServer, boolean shelved,
            Date submittedDate, P4ChangelistType changelistType, String clientname, String username,
            List<P4Job> attachedJobs, JobStatus jobStatus,
            List<CommittedFile> files) {
        this.changelistId = changelistId;
        this.summary = summary;
        this.comment = comment;
        this.deleted = deleted;
        this.onServer = onServer;
        this.shelved = shelved;
        this.submittedDate = submittedDate == null ? null : new Date(submittedDate.getTime());
        this.changelistType = changelistType;
        this.clientname = clientname;
        this.username = username;
        this.attachedJobs = attachedJobs;
        this.jobStatus = jobStatus;
        this.files = files;
    }

    public P4RemoteChangelistImpl(P4LocalChangelist changelist) {
        this.changelistId = changelist.getChangelistId();
        this.summary = new P4ChangelistSummaryImpl(changelist);
        this.comment = changelist.getComment();
        this.deleted = changelist.isDeleted();
        this.onServer = changelist.isOnServer();
        this.shelved = !changelist.getShelvedFiles().isEmpty();
        this.submittedDate = null;
        this.changelistType = changelist.getChangelistType();
        this.clientname = changelist.getClientname();
        this.username = changelist.getUsername();
        this.attachedJobs = changelist.getAttachedJobs();
        this.jobStatus = changelist.getJobStatus();

        // TODO is there a better way to get the associated remote files?
        //this.files = changelist.getFiles();
        this.files = Collections.emptyList();
    }


    @NotNull
    @Override
    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    @NotNull
    @Override
    public P4ChangelistSummary getSummary() {
        return summary;
    }

    @NotNull
    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean isOnServer() {
        return onServer;
    }

    @Override
    public boolean isSubmitted() {
        return submittedDate != null;
    }

    @Override
    public boolean hasShelvedFiles() {
        return shelved;
    }

    @Nullable
    @Override
    public Date getSubmittedDate() {
        return submittedDate;
    }

    @NotNull
    @Override
    public P4ChangelistType getChangelistType() {
        return changelistType;
    }

    @NotNull
    @Override
    public String getClientname() {
        return clientname;
    }

    @NotNull
    @Override
    public String getUsername() {
        return username;
    }

    @NotNull
    @Override
    public List<P4Job> getAttachedJobs() {
        return attachedJobs;
    }

    @Nullable
    @Override
    public JobStatus getJobStatus() {
        return jobStatus;
    }

    @NotNull
    @Override
    public List<CommittedFile> getFiles() {
        return files;
    }

    private static class Committed implements CommittedFile {
        private final P4RemoteFile file;
        private final int rev;
        private final P4FileAction action;
        private final P4RemoteFile from;
        private final int fromRev;

        Committed(@NotNull IFileSpec f) {
            this.file = new P4RemoteFileImpl(f);
            this.action = P4FileAction.convert(f.getAction());
            this.rev = f.getWorkRev();
            this.from = f.getFromFile() == null ? null : new P4RemoteFileImpl(f.getFromFile());
            this.fromRev = f.getFromFile() == null ? -1 : f.getEndFromRev();
        }

        @NotNull
        @Override
        public P4RemoteFile getDepotPath() {
            return file;
        }

        @Override
        public int getRevision() {
            return rev;
        }

        @NotNull
        @Override
        public P4FileAction getAction() {
            return action;
        }

        @Nullable
        @Override
        public P4RemoteFile getIntegratedFrom() {
            return from;
        }

        @Override
        public int getFromRevision() {
            return fromRev;
        }
    }
}
