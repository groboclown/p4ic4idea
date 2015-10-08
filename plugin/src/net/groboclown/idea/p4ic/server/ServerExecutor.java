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
package net.groboclown.idea.p4ic.server;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.IFileSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;

public class ServerExecutor {
    private final Project project;
    private final RawServerExecutor exec;
    //private final JobCache jobCache;


    public ServerExecutor(Project project, RawServerExecutor exec) {
        this.project = project;
        this.exec = exec;
        //this.jobCache = new JobCache(project, exec);
    }


    /**
     * @deprecated see ServerConnectionController
     */
    public boolean isWorkingOnline() {
        return exec.isWorkingOnline();
    }


    /**
     *
     * @param p4file file
     * @param rev file revision
     * @return null if the file doesn't exist
     * @throws VcsException
     */
    public String loadFileAsString(@NotNull P4FileInfo p4file, int rev) throws VcsException {
        return exec.loadFileAsString(project, p4file, rev);
    }


    public String loadFileAsString(@NotNull FilePath file, int rev)
            throws VcsException, CancellationException {
        return exec.loadFileAsString(project, file, rev);
    }


    public String loadFileAsString(@NotNull IFileSpec file)
            throws VcsException, CancellationException {
        return exec.loadFileAsString(project, file);
    }


    /**
     *
     * @param changelistId
     * @return
     * @throws VcsException
     */
    @Nullable
    public IChangelist getChangelist(int changelistId) throws VcsException {
        return exec.getChangelist(project, changelistId);
    }

    /**
     * NOTE should be only called from the P4ChangeListCache.
     *
     * @return pending changelists
     * @throws VcsException
     */
    @NotNull
    public List<IChangelistSummary> getPendingClientChangelists() throws VcsException {
        return exec.getPendingClientChangelists(project);
    }

    @NotNull
    public Collection<VirtualFile> findRoots(@Nullable final Collection<VirtualFile> requestedRoots) throws VcsException, CancellationException {
        return exec.findRoots(project, requestedRoots);
    }

    @NotNull
    public List<P4StatusMessage> revertFiles(@NotNull List<FilePath> filePaths) throws VcsException {
        return exec.revertFiles(project, filePaths);
    }

    /**
     * @param file file to load contents
     * @param rev  file revision
     * @return null if the file revision is 0; else not null
     * @throws VcsException
     * @throws CancellationException
     */
    @Nullable
    public byte[] loadFileAsBytes(@NotNull FilePath file, int rev) throws VcsException, CancellationException {
        return exec.loadFileAsBytes(project, file, rev);
    }

    /**
     *
     * @param file file info to load contents
     * @return null if the file revision is 0; else not null
     * @throws VcsException
     * @throws CancellationException
     */
    @Nullable
    public byte[] loadFileAsBytes(@NotNull IFileSpec file) throws VcsException, CancellationException {
        return exec.loadFileAsBytes(project, file);
    }

    /**
     * Used by the status update to get the information about the latest versions of the
     * files.
     *
     * @param paths directories or files
     * @return file status information
     * @throws VcsException
     * @throws CancellationException
     */
    public List<P4FileInfo> loadDeepFileInfo(@NotNull Collection<FilePath> paths)
            throws VcsException, CancellationException {
        return exec.loadDeepFileInfo(project, paths);
    }

    @NotNull
    public List<P4FileInfo> synchronizeFiles(@NotNull final Collection<FilePath> path, final int revision,
            @Nullable final String changelist, boolean forceSync, @NotNull final Collection<VcsException> errorsOutput)
            throws VcsException, CancellationException {
        return exec.synchronizeFiles(project, path, revision, changelist, forceSync, errorsOutput);
    }

    @NotNull
    public Collection<P4FileInfo> revertUnchangedFilesInChangelist(final int changeListId,
            @NotNull final List<P4StatusMessage> errors)
            throws VcsException, CancellationException {
        return exec.revertUnchangedFilesInChangelist(project, changeListId, errors);
    }

    @NotNull
    public Collection<P4FileInfo> revertUnchangedFiles(@NotNull List<FilePath> filePaths,
            @NotNull final List<P4StatusMessage> errors) throws VcsException {
        return exec.revertUnchangedFiles(project, filePaths, errors);
    }

    @NotNull
    public IClient getClient() throws VcsException, CancellationException {
        return exec.getClient(project);
    }
}
