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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;
import java.util.Date;
import java.util.List;

/**
 * Represents a changelist on the server.
 * <p>
 * Even though this doesn't implement P4ChangelistSummary, it contains those
 * methods.
 */
@Immutable
public interface P4RemoteChangelist {
    @NotNull
    P4ChangelistId getChangelistId();

    @NotNull
    P4ChangelistSummary getSummary();

    @NotNull
    String getComment();

    boolean isDeleted();

    boolean isOnServer();

    boolean isSubmitted();

    /**
     *
     * @return can return false if the information wasn't fetched.
     */
    boolean hasShelvedFiles();

    @Nullable
    Date getSubmittedDate();

    @NotNull
    P4ChangelistType getChangelistType();

    /**
     *
     * @return the workspace ID of the assigned client.
     */
    @NotNull
    String getClientname();

    /**
     *
     * @return the user who owns the changelist.
     */
    @NotNull
    String getUsername();

    @NotNull
    List<P4Job> getAttachedJobs();

    @Nullable
    JobStatus getJobStatus();

    @NotNull
    List<CommittedFile> getFiles();



    interface CommittedFile {
        @NotNull
        P4RemoteFile getDepotPath();

        int getRevision();

        @NotNull
        P4FileAction getAction();

        @Nullable
        P4RemoteFile getIntegratedFrom();

        int getFromRevision();
    }
}
