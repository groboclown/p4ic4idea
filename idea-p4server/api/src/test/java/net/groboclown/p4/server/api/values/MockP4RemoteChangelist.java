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

package net.groboclown.p4.server.api.values;

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MockP4RemoteChangelist implements P4RemoteChangelist {
    // Mock object, breaking immutability.
    private P4ChangelistId changelistId;
    private String username;
    private ClientServerRef csRef;
    private String comment = "<unknown>";
    private boolean deleted;
    private boolean submitted;
    private boolean hasShelved;
    private Date date = new Date();
    private P4ChangelistType type = P4ChangelistType.PUBLIC;
    private List<P4Job> jobs = Collections.emptyList();
    private JobStatus jobStatus = new MockJobStatus("closed");
    private List<CommittedFile> files = Collections.emptyList();
    private MockP4ChangelistSummary summary;


    @NotNull
    @Override
    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    public MockP4RemoteChangelist withConfig(ClientConfig cc) {
        csRef = cc.getClientServerRef();
        //clientname = cc.getClientname();
        //serverName = csRef.getServerName();
        username = cc.getServerConfig().getUsername();
        summary = null;
        return this;
    }

    public MockP4RemoteChangelist withConfig(ConfigPart cp) {
        csRef = new ClientServerRef(Objects.requireNonNull(cp.getServerName()), cp.getClientname());
        //clientname = cp.getClientname();
        //serverName = csRef.getServerName();
        username = cp.getUsername();
        summary = null;
        return this;
    }

    public MockP4RemoteChangelist withChangelistId(int id) {
        assertNotNull(csRef);
        changelistId = new MockP4ChangelistId(csRef, id);
        summary = null;
        return this;
    }

    public MockP4RemoteChangelist withDefaultChangelist() {
        assertNotNull(csRef);
        changelistId = new MockP4ChangelistId(csRef);
        summary = null;
        return this;
    }

    @NotNull
    @Override
    public P4ChangelistSummary getSummary() {
        if (summary == null) {
            summary = new MockP4ChangelistSummary()
                    .withRemoteChangelist(this);
        }
        return summary;
    }

    @NotNull
    @Override
    public String getComment() {
        return comment;
    }

    public MockP4RemoteChangelist withComment(String s) {
        comment = s;
        summary = null;
        return this;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public MockP4RemoteChangelist withDeleted(boolean b) {
        deleted = b;
        summary = null;
        return this;
    }

    @Override
    public boolean isOnServer() {
        return submitted || getChangelistId().getChangelistId() > 0;
    }

    @Override
    public boolean isSubmitted() {
        return submitted;
    }

    public MockP4RemoteChangelist withSubmitted(boolean b) {
        submitted = b;
        summary = null;
        return this;
    }

    @Override
    public boolean hasShelvedFiles() {
        return hasShelved;
    }

    public MockP4RemoteChangelist withShelvedFiles(boolean b) {
        hasShelved = b;
        summary = null;
        return this;
    }

    @Nullable
    @Override
    public Date getSubmittedDate() {
        return date;
    }

    public MockP4RemoteChangelist withSubmittedDate(Date d) {
        date = d;
        summary = null;
        return this;
    }

    @NotNull
    @Override
    public P4ChangelistType getChangelistType() {
        return type;
    }

    public MockP4RemoteChangelist withChangelistType(P4ChangelistType t) {
        type = t;
        summary = null;
        return this;
    }

    @NotNull
    @Override
    public String getClientname() {
        return csRef.getClientName();
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

    public MockP4RemoteChangelist withJobs(P4Job... j) {
        jobs = Arrays.asList(j);
        summary = null;
        return this;
    }

    @Nullable
    @Override
    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public MockP4RemoteChangelist withJobStatus(String s) {
        jobStatus = new MockJobStatus(s);
        summary = null;
        return this;
    }

    @NotNull
    @Override
    public List<CommittedFile> getFiles() {
        return files;
    }

    public MockP4RemoteChangelist withFiles(P4RemoteFile... f) {
        files = new ArrayList<>(f.length);
        for (P4RemoteFile p4RemoteFile : f) {
            files.add(new CommittedFileImpl(p4RemoteFile));
        }
        summary = null;
        return this;
    }

    private static class CommittedFileImpl implements CommittedFile {
        final P4RemoteFile f;
        final P4FileAction a;

        private CommittedFileImpl(P4RemoteFile f) {
            this.f = f;
            this.a = P4FileAction.ADD;
        }

        @NotNull
        @Override
        public P4RemoteFile getDepotPath() {
            return f;
        }

        @Override
        public int getRevision() {
            return 1;
        }

        @NotNull
        @Override
        public P4FileAction getAction() {
            return a;
        }

        @Nullable
        @Override
        public P4RemoteFile getIntegratedFrom() {
            return null;
        }

        @Override
        public int getFromRevision() {
            return 0;
        }
    }
}
