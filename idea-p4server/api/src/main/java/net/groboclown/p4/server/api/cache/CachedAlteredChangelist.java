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

package net.groboclown.p4.server.api.cache;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.values.P4ChangelistAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Record of a changelist that has pending changes to send to the server.
 */
public interface CachedAlteredChangelist {
    @NotNull
    P4ChangelistId getChangelistId();

    @Nullable
    String getDescription();

    @NotNull
    Collection<FilePath> getAddedFiles();

    @NotNull
    Collection<FilePath> getRemovedFiles();

    @NotNull
    Collection<P4Job> getAddedJobs();

    @NotNull
    Collection<P4Job> getRemovedJobs();

    /**
     * Returns all local file paths to files that are pending to be shelved.
     *
     * @return list of all files locally known to be shelved.  These must not be the same files
     *      as what {@link #getKnownRemoteShelvedFiles()} returns; that is, the intersection of local and
     *      remote files is empty.
     */
    @NotNull
    Collection<FilePath> getPendingShelvedFiles();

    @NotNull
    Collection<P4RemoteFile> getKnownRemoteShelvedFiles();

    @NotNull
    List<P4CommandRunner.ClientAction<?>> getPendingChangelistActions();
}
