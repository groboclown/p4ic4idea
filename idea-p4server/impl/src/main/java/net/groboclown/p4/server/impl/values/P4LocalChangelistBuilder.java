package net.groboclown.p4.server.impl.values;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4ChangelistType;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

// FIXME either use this or P4LocalChangelistImpl.Builder, but not both.
public class P4LocalChangelistBuilder {
    private P4ChangelistId changelistId;
    private String comment;
    private boolean deleted = false;
    private List<FilePath> containedFiles = Collections.emptyList();
    private List<P4RemoteFile> shelvedFiles = Collections.emptyList();
    private P4ChangelistType type = P4ChangelistType.PUBLIC;
    private String clientname;
    private String username;
    private List<P4Job> jobs = Collections.emptyList();
    private JobStatus jobStatus = null;

    public P4LocalChangelistBuilder withChangelistId(@NotNull ClientServerRef ref, int id) {
        this.changelistId = new P4ChangelistIdImpl(id, ref);
        return this;
    }

    public P4LocalChangelistBuilder withDefaultChangelist(@NotNull ClientServerRef ref) {
        this.changelistId = new P4ChangelistIdImpl(IChangelist.DEFAULT, ref);
        return this;
    }

    public P4LocalChangelistBuilder withChangelistType(@NotNull P4ChangelistType type) {
        this.type = type;
        return this;
    }

    public P4LocalChangelistBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    public P4LocalChangelistBuilder withDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    public P4LocalChangelistBuilder withContainedFiles(List<FilePath> containedFiles) {
        this.containedFiles = containedFiles;
        return this;
    }

    public P4LocalChangelistBuilder withShelvedFiles(List<P4RemoteFile> shelvedFiles) {
        this.shelvedFiles = shelvedFiles;
        return this;
    }

    public P4LocalChangelistBuilder withRestrictedType() {
        this.type = P4ChangelistType.RESTRICTED;
        return this;
    }

    public P4LocalChangelistBuilder withClientname(String clientname) {
        this.clientname = clientname;
        return this;
    }

    public P4LocalChangelistBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public P4LocalChangelistBuilder withJobs(List<P4Job> jobs) {
        this.jobs = jobs;
        return this;
    }

    public P4LocalChangelistBuilder withJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
        return this;
    }

    public P4LocalChangelistBuilder withJobStatus(String jobStatusName) {
        this.jobStatus = jobStatusName == null ? null : new JobStatusImpl(jobStatusName);
        return this;
    }

    public P4LocalChangelistBuilder withChangelistId(P4ChangelistId id) {
        this.changelistId = id;
        return this;
    }

    public P4LocalChangelist build() {
        return new P4LocalChangelistImpl(changelistId, comment, deleted, containedFiles,
                shelvedFiles, type, clientname, username, jobs, jobStatus);
    }
}