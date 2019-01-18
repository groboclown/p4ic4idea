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

package net.groboclown.p4.server.impl.cache.store;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.values.P4ChangelistType;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.values.P4LocalChangelistImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class P4LocalChangelistStore {

    @SuppressWarnings("WeakerAccess")
    public static class State {
        public P4ChangelistIdStore.State changelistId;
        public String comment;
        public boolean deleted;
        public List<String> containedFiles;
        public List<P4RemoteFileStore.State> shelvedFiles;
        public P4ChangelistType type;
        public String clientname;
        public String username;
        public List<P4JobStore.State> jobs;
        public String jobStatus;
    }


    @NotNull
    public static State getState(@NotNull P4LocalChangelist changelist) {
        State ret = new State();
        ret.changelistId = P4ChangelistIdStore.getState(changelist.getChangelistId());
        ret.comment = changelist.getComment();
        ret.deleted = changelist.isDeleted();
        ret.containedFiles = new ArrayList<>(changelist.getFiles().size());
        for (FilePath containedFile : changelist.getFiles()) {
            ret.containedFiles.add(containedFile.getPath());
        }
        ret.shelvedFiles = new ArrayList<>(changelist.getShelvedFiles().size());
        for (P4RemoteFile shelvedFile : changelist.getShelvedFiles()) {
            ret.shelvedFiles.add(P4RemoteFileStore.getState(shelvedFile));
        }
        ret.type = changelist.getChangelistType();
        ret.clientname = changelist.getClientname();
        ret.username = changelist.getUsername();
        ret.jobs = new ArrayList<>(changelist.getAttachedJobs().size());
        for (P4Job job : changelist.getAttachedJobs()) {
            ret.jobs.add(P4JobStore.getState(job));
        }
        ret.jobStatus = changelist.getJobStatus() == null
                ? null
                : changelist.getJobStatus().getName();
        return ret;
    }


    public static P4LocalChangelist read(@NotNull State state) {
        List<FilePath> containedFiles = new ArrayList<>(state.containedFiles.size());
        for (String containedFile : state.containedFiles) {
            containedFiles.add(VcsUtil.getFilePath(containedFile));
        }
        List<P4RemoteFile> shelvedFiles = new ArrayList<>(state.shelvedFiles.size());
        for (P4RemoteFileStore.State shelvedFile : state.shelvedFiles) {
            shelvedFiles.add(P4RemoteFileStore.read(shelvedFile));
        }
        List<P4Job> jobs = new ArrayList<>(state.jobs.size());
        for (P4JobStore.State job : state.jobs) {
            jobs.add(P4JobStore.read(job));
        }
        return new P4LocalChangelistImpl.Builder()
                .withChangelistId(P4ChangelistIdStore.read(state.changelistId))
                .withComment(state.comment)
                .withDeleted(state.deleted)
                .withContainedFiles(containedFiles)
                .withShelvedFiles(shelvedFiles)
                .withChangelistType(state.type)
                .withClientname(state.clientname)
                .withUsername(state.username)
                .withJobs(jobs)
                .withJobStatus(state.jobStatus)
                .build();
    }
}
