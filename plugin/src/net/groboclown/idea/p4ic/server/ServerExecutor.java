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
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.idea.p4ic.history.P4AnnotatedLine;
import net.groboclown.idea.p4ic.history.P4FileRevision;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

public class ServerExecutor {
    private final Project project;
    private final RawServerExecutor exec;
    private final JobCache jobCache;


    public ServerExecutor(Project project, RawServerExecutor exec) {
        this.project = project;
        this.exec = exec;
        this.jobCache = new JobCache(project, exec);
    }


    public boolean isWorkingOnline() {
        return exec.isWorkingOnline();
    }


    public void invalidateCache() {
        exec.invalidateFileInfoCache();
        jobCache.invalidateCache();
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


    /**
     * Should only be called from {@link net.groboclown.idea.p4ic.changes.P4ChangeListCache}
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
     * Should only be called from {@link net.groboclown.idea.p4ic.changes.P4ChangeListCache}
     *
     * @param changelistId
     * @return
     * @throws VcsException
     */
    @Nullable
    public List<P4FileInfo> getFilesInChangelist(int changelistId) throws VcsException {
        return exec.getFilesInChangelist(project, changelistId);
    }

    /**
     * Should only be called from {@link net.groboclown.idea.p4ic.changes.P4ChangeListCache}
     *
     * @param comment
     * @return
     * @throws VcsException
     */
    @NotNull
    public IChangelist createChangelist(@NotNull String comment) throws VcsException {
        return exec.createChangelist(project, comment);
    }

    @NotNull
    public List<P4StatusMessage> deleteFiles(@NotNull List<FilePath> filesToDelete, int changelistId) throws VcsException {
        return exec.deleteFiles(project, filesToDelete, changelistId);
    }

    @NotNull
    public List<P4StatusMessage> moveFiles(@NotNull Map<FilePath, FilePath> movedFiles, int changelistId) throws VcsException {
        return exec.moveFiles(project, movedFiles, changelistId);
    }

    @NotNull
    public List<P4StatusMessage> addOrCopyFiles(@NotNull Collection<VirtualFile> addedFiles,
            @NotNull Map<VirtualFile, VirtualFile> copyFromMap, int changelistId) throws VcsException {
        return exec.addOrCopyFiles(project, addedFiles, copyFromMap, changelistId);
    }

    @NotNull
    public List<P4StatusMessage> editFiles(List<VirtualFile> edited, int changelistId) throws VcsException {
        return exec.editFiles(project, edited, changelistId);
    }

    public void deleteChangelist(int changelistId) throws VcsException {
        exec.deleteChangelist(project, changelistId);
    }

    /**
     * Should only be called from {@link net.groboclown.idea.p4ic.changes.P4ChangeListCache}
     *
     * @param targetChangelistId
     * @param affected
     * @return
     * @throws VcsException
     */
    public List<P4StatusMessage> moveFilesToChangelist(int targetChangelistId, List<FilePath> affected)
            throws VcsException {
        return exec.moveFilesToChangelist(project, targetChangelistId, affected);
    }

    public void updateChangelistComment(int changelistId, @NotNull String comment) throws VcsException {
        exec.updateChangelistComment(project, changelistId, comment);
    }

    /**
     * NOTE should be only called from the P4ChangeListCache.
     *
     * @return pending changelists
     * @throws VcsException
     */
    public List<IChangelistSummary> getPendingClientChangelists() throws VcsException {
        return exec.getPendingClientChangelists(project);
    }

    public List<P4FileInfo> getFilePathInfo(@NotNull Collection<FilePath> filePaths) throws VcsException {
        return exec.getFilePathInfo(project, filePaths);
    }


    public Collection<VirtualFile> findRoots(@Nullable final Collection<VirtualFile> requestedRoots) throws VcsException, CancellationException {
        return exec.findRoots(project, requestedRoots);
    }

    public List<P4FileInfo> loadOpenFiles(@Nullable VirtualFile[] roots) throws VcsException {
        return exec.loadOpenFiles(project, roots);
    }

    public List<P4FileRevision> getRevisionHistory(@NotNull P4FileInfo file, int maxRevs)
            throws VcsException {
        return exec.getRevisionHistory(project, file, maxRevs);
    }

    public List<P4StatusMessage> revertFiles(@NotNull List<FilePath> filePaths) throws VcsException {
        return exec.revertFiles(project, filePaths);
    }

    public List<P4FileInfo> getVirtualFileInfo(@NotNull Collection<VirtualFile> virtualFiles) throws VcsException {
        return exec.getVirtualFileInfo(project, virtualFiles);
    }

    public List<P4StatusMessage> submitChangelist(@NotNull List<FilePath> files, @NotNull Collection<P4Job> jobs,
            String jobStatus, int changelistId) throws VcsException {
        return exec.submitChangelist(project, files, jobs, jobStatus, changelistId);
    }

    public byte[] loadFileAsBytes(@NotNull FilePath file, int rev) throws VcsException, CancellationException {
        return exec.loadFileAsBytes(project, file, rev);
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
    public List<P4AnnotatedLine> getAnnotationsFor(@NotNull VirtualFile file, int rev)
            throws VcsException, CancellationException {
        return exec.getAnnotationsFor(project, file, rev);
    }

    public void checkConnection() throws P4InvalidConfigException, CancellationException {
        exec.checkConnection(project);
    }

    @NotNull
    public List<P4FileInfo> synchronizeFiles(@NotNull final Collection<FilePath> path, final int revision,
            final int changelist, @NotNull final Collection<VcsException> errorsOutput)
            throws VcsException, CancellationException {
        return exec.synchronizeFiles(project, path, revision, changelist, errorsOutput);
    }

    @NotNull
    public Collection<P4StatusMessage> integrateFiles(@NotNull final P4FileInfo src, @NotNull final FilePath tgt,
            final int changeListId) throws VcsException, CancellationException {
        return exec.integrateFiles(project, src, tgt, changeListId);
    }

    @NotNull
    public List<String> getJobStatusValues() throws VcsException, CancellationException {
        //return exec.getJobStatusValues(project);
        return jobCache.getJobStatusValues();
    }

    @Nullable
    public Collection<P4Job> getJobsForChangelist(final int id) throws VcsException, CancellationException {
        //return exec.getJobsForChangelist(project, id);
        return jobCache.getJobsForChangelist(id);
    }

    @Nullable
    public P4Job getJobForId(final String jobId) throws VcsException, CancellationException {
        //return exec.getJobForId(project, jobId);
        return jobCache.getJob(jobId);
    }
}
