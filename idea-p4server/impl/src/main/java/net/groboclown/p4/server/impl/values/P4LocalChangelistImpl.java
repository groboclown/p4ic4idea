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
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistType;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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



    P4LocalChangelistImpl(
            @NotNull ServerConfig serverConfig,
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
    public JobStatus getJobStatus(@NotNull P4Job job) {
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
}
