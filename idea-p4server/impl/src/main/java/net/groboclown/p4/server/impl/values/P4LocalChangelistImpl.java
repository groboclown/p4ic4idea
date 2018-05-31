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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistType;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

@Immutable
public class P4LocalChangelistImpl implements P4LocalChangelist {
    private final P4ChangelistId changelistId;
    private final String comment;
    private final boolean deleted;
    private final List<FilePath> containedFiles;
    private final List<P4RemoteFile> shelvedFiles;
    private final P4ChangelistType type;
    private final String clientname;
    private final String username;
    private final List<P4Job> jobs;
    private final JobStatus jobStatus;


    public static class Builder {
        private P4ChangelistId changelistId;
        private String comment;
        private boolean deleted;
        private List<FilePath> containedFiles = new ArrayList<>();
        private List<P4RemoteFile> shelvedFiles = new ArrayList<>();
        private P4ChangelistType type;
        private String clientname;
        private String username;
        private List<P4Job> jobs = new ArrayList<>();
        private JobStatus jobStatus;

        public Builder withSrc(P4LocalChangelist cl) {
            this.changelistId = cl.getChangelistId();
            this.comment = cl.getComment();
            this.deleted = cl.isDeleted();
            this.containedFiles = cl.getFiles();
            this.shelvedFiles = cl.getShelvedFiles();
            this.type = cl.getChangelistType();
            this.clientname = cl.getClientname();
            this.username = cl.getUsername();
            this.jobs = new ArrayList<>(cl.getAttachedJobs());
            this.jobStatus = cl.getJobStatus();
            return this;
        }

        public boolean is(P4ChangelistId id) {
            return id.equals(this.changelistId);
        }

        public Builder withChangelistId(P4ChangelistId id) {
            this.changelistId = id;
            return this;
        }

        public Builder withComment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder withClientname(String clientname) {
            this.clientname = clientname;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public P4LocalChangelistImpl build() {
            return new P4LocalChangelistImpl(changelistId, comment, deleted, containedFiles,
                    shelvedFiles, type, clientname, username, jobs, jobStatus);
        }

        public Builder withDelete(boolean b) {
            this.deleted = b;
            return this;
        }

        public void addFiles(List<FilePath> files) {
            this.containedFiles.addAll(files);
        }

        public void removeFiles(List<FilePath> files) {
            this.containedFiles.removeAll(files);
        }

        public Builder withJob(P4Job job) {
            this.jobs.add(job);
            return this;
        }

        public Builder withVirtualFiles(VirtualFile... files) {
            for (VirtualFile file : files) {
                this.containedFiles.add(VcsUtil.getFilePath(file));
            }
            return this;
        }
    }



    P4LocalChangelistImpl(
            @NotNull P4ChangelistId changelistId,
            @NotNull String comment, boolean deleted,
            @NotNull List<FilePath> containedFiles,
            @NotNull List<P4RemoteFile> shelvedFiles,
            @NotNull P4ChangelistType type,
            @NotNull String clientname,
            @NotNull String username,
            @NotNull List<P4Job> jobs,
            @Nullable JobStatus jobStatus) {
        this.changelistId = changelistId;
        this.comment = comment;
        this.deleted = deleted;
        this.containedFiles = new ArrayList<>(containedFiles);
        this.shelvedFiles = new ArrayList<>(shelvedFiles);
        this.type = type;
        this.clientname = clientname;
        this.username = username;
        this.jobs = new ArrayList<>(jobs);
        this.jobStatus = jobStatus;
    }


    @NotNull
    @Override
    public P4ChangelistId getChangelistId() {
        return changelistId;
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
        return !deleted && changelistId.getState() == P4ChangelistId.State.NUMBERED;
    }

    @Override
    public boolean hasShelvedFiles() {
        return !shelvedFiles.isEmpty();
    }

    @NotNull
    @Override
    public P4ChangelistType getChangelistType() {
        return type;
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
        return jobs;
    }

    @Nullable
    @Override
    public JobStatus getJobStatus() {
        return jobStatus;
    }

    @NotNull
    @Override
    public List<FilePath> getFiles() {
        return containedFiles;
    }

    @NotNull
    @Override
    public List<P4RemoteFile> getShelvedFiles() {
        return shelvedFiles;
    }

    @Override
    public String toString() {
        return "P4LocalChangelist(" + changelistId
                + ": [" + comment + "] with files "
                + containedFiles
                + ')';
    }
}
