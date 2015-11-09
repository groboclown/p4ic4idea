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
package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.*;
import com.perforce.p4java.core.file.*;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.file.ExtendedFileSpec;
import com.perforce.p4java.impl.generic.core.file.FilePath;
import com.perforce.p4java.impl.generic.core.file.FilePath.PathType;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.connection.ClientExec.ServerCount;
import net.groboclown.idea.p4ic.v2.server.connection.ClientExec.WithClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import static net.groboclown.idea.p4ic.server.P4StatusMessage.getErrors;

/**
 * A project-aware command executor against the server/client.
 */
public class P4Exec2 {
    private static final Logger LOG = Logger.getInstance(P4Exec2.class);

    private final Project project;
    private final ClientExec exec;

    private final Object sync = new Object();

    private boolean disposed = false;


    public P4Exec2(@NotNull Project project, @NotNull ClientExec exec) {
        this.project = project;
        this.exec = exec;
    }


    @Nullable
    public String getClientName() {
        return exec.getClientName();
    }


    @NotNull
    public String getUsername() {
        return getServerConfig().getUsername();
    }


    @NotNull
    public ServerConfig getServerConfig() {
        return exec.getServerConfig();
    }

    @NotNull
    public Project getProject() {
        return project;
    }


    public void dispose() {
        // in the future, this may clean up open connections
        synchronized (sync) {
            disposed = true;
            // Note: this does NOT dispose the ClientExec; this class borrows that.
        }
    }

    @NotNull
    public ServerConnectedController getServerConnectedController() {
        return exec.getServerConnectedController();
    }


    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }


    @NotNull
    public IClient getClient() throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<IClient>() {
            @Override
            public IClient run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                client.setServer(null);
                return client;
            }
        });
    }


    @Nullable
    public IChangelist getChangelist(final int id)
            throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<IChangelist>() {
            @Override
            public IChangelist run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("getChangelist");
                IChangelist cl = server.getChangelist(id);
                if (id != cl.getId()) {
                    LOG.warn("Perforce Java API error: returned changelist with id " + cl.getId() + " when requested " + id);
                    cl.setId(id);
                }

                // The server connection cannot leave this context.
                cl.setServer(null);
                if (cl instanceof Changelist) {
                    ((Changelist) cl).setServerImpl(null);
                }

                return cl;
            }
        });

    }


    /**
     *
     * @param openedSpecs query file specs, expected to be a "..." style.
     * @param fast runs with the "-s" argument, which means the revision and file type is not returned.
     * @return messages and results
     * @throws VcsException
     * @throws CancellationException
     */
    @NotNull
    public MessageResult<List<IExtendedFileSpec>> loadOpenedFiles(@NotNull final List<IFileSpec> openedSpecs, final boolean fast)
            throws VcsException, CancellationException {
        LOG.debug("loading open files " + openedSpecs);
        return exec.runWithClient(project, new ClientExec.WithClient<MessageResult<List<IExtendedFileSpec>>>() {
            @Override
            public MessageResult<List<IExtendedFileSpec>> run(@NotNull final IOptionsServer server, @NotNull final IClient client,
                    @NotNull final ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
                    P4Exception {
                OpenedFilesOptions options = new OpenedFilesOptions(
                        false, // all clients
                        client.getName(),
                        -1,
                        null,
                        -1).setShortOutput(fast);
                final List<IFileSpec> files = client.openedFiles(openedSpecs, options);
                // We will need to get the full information on these files, so grab that, too.
                // We keep a list of all the returned specs that were messages, and include the
                // specs for the fstat results.  The result from the "opened" operation will
                // only ever return the depot path.
                return loadFstatForFileResult(server, files, true);
            }
        });
    }


    @NotNull
    public List<IFileSpec> revertFiles(@NotNull final List<IFileSpec> files)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return exec.runWithClient(project, new ClientExec.WithClient<List<IFileSpec>>() {
            @Override
            public List<IFileSpec> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
                count.invoke("revertFiles");
                return client.revertFiles(files, false, -1, false, false);
            }
        });
    }


    @NotNull
    public List<IFileSpec> revertUnchangedFiles(
            @NotNull final List<IFileSpec> fileSpecs, final int changeListId)
            throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<List<IFileSpec>>() {
            @Override
            public List<IFileSpec> run(@NotNull final IOptionsServer server, @NotNull final IClient client,
                    @NotNull final ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
                    P4Exception {
                RevertFilesOptions options = new RevertFilesOptions(false, changeListId, true, false);
                count.invoke("revertFiles");
                return client.revertFiles(fileSpecs, options);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> integrateFiles(@NotNull final IFileSpec src,
            @NotNull final IFileSpec target, final int changelistId, final boolean dontCopyToClient)
            throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                List<IFileSpec> ret = new ArrayList<IFileSpec>();
                count.invoke("integrateFiles");
                ret.addAll(client.integrateFiles(
                        src, target,
                        null,
                        new IntegrateFilesOptions(
                                changelistId,
                                false, // bidirectionalInteg,
                                true, // integrateAroundDeletedRevs
                                false, // rebranchSourceAfterDelete,
                                true, // deleteTargetAfterDelete,
                                true, // integrateAllAfterReAdd,
                                false, // branchResolves,
                                false, // deleteResolves,
                                false, // skipIntegratedRevs,
                                true, // forceIntegration,
                                false, // useHaveRev,
                                true, // doBaselessMerge,
                                false, // displayBaseDetails,
                                false, // showActionsOnly,
                                false, // reverseMapping,
                                true, // propagateType,
                                dontCopyToClient,
                                0// maxFiles
                        )));
                return getErrors(ret);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> addFiles(@NotNull List<IFileSpec> files,
            final int changelistId) throws VcsException, CancellationException {
        // Adding files with wildcards in their name:
        // To add files with filenames that contain wildcard characters, specify
        // the -f flag. Filenames that contain the special characters '@', '#',
        // '%' or '*' are reformatted to encode the characters using ASCII
        // hexadecimal representation.  After the files are added, you must
        // refer to them using the reformatted file name, because Perforce
        // does not recognize the local filesystem name.
        //
        // (ASCII hexadecimal representation: "a@b" is turned into
        // "a%40b")
        //
        // Note that this escaping is addressed universally in all the
        // access classes.  Unfortunately, this means that all the filespecs
        // coming into this method are already escaped.  So, we'll have to
        // undo that.

        final List<IFileSpec> unescapedFiles = new ArrayList<IFileSpec>(files.size());
        for (IFileSpec file: files) {
            String original = file.getOriginalPathString();
            if (original != null) {
                File f = new File(FileSpecUtil.unescapeP4Path(original));
                if (! f.exists()) {
                    throw new P4Exception(P4Bundle.message("error.add.file-not-found", f));
                }
                // We must set the original path the hard way, to avoid the FilePath
                // stripping off the stuff after the '#' or '@', if it was escaped originally.
                file.setPath(new FilePath(FilePath.PathType.ORIGINAL, f.getAbsolutePath(), true));
            } else if (file.getLocalPathString() == null) {
                throw new IllegalStateException(P4Bundle.message("error.add.no-local-file", file));
            }
            unescapedFiles.add(file);
        }

        // debug for issue #6
        //LOG.info("Opening for add: " + unescapedFiles, new Throwable());

        return exec.runWithClient(project, new ClientExec.WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                List<IFileSpec> ret = new ArrayList<IFileSpec>();
                count.invoke("addFiles");
                ret.addAll(client.addFiles(unescapedFiles, false, changelistId, null,
                        // Use wildcards = true to allow file names that contain wildcards
                        true));
                return getErrors(ret);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> editFiles(@NotNull final List<IFileSpec> files,
            final int changelistId) throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                List<IFileSpec> ret = new ArrayList<IFileSpec>();
                // this allows for wildcard characters (*, #, @) in the file name,
                // if they were properly escaped.
                count.invoke("editFiles");
                ret.addAll(client.editFiles(files,
                    false, false, changelistId, null));
                return getErrors(ret);
            }
        });
    }


    public void updateChangelistDescription(final int changelistId,
            @NotNull final String description) throws VcsException, CancellationException {
        exec.runWithClient(project, new ClientExec.WithClient<Void>() {
            @Override
            public Void run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
                // Note that, because the server connection can be closed after an
                // invocation, we must perform all the changelist updates within this
                // call, and we can't go outside this method.

                count.invoke("getChangelist");
                IChangelist changelist = server.getChangelist(changelistId);
                if (changelist != null && changelist.getStatus() == ChangelistStatus.PENDING) {
                    changelist.setDescription(description);
                    count.invoke("changelist.update");
                    changelist.update();
                }
                return null;
            }
        });
    }


    public void addJobsToChangelist(final int changelistId,
            @NotNull final Collection<String> jobIds,
            @Nullable final String fixState) throws VcsException, CancellationException {
        exec.runWithClient(project, new WithClient<Void>() {
            @Override
            public Void run(@NotNull final IOptionsServer server, @NotNull final IClient client,
                    @NotNull final ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
                    P4Exception {
                server.fixJobs(new ArrayList<String>(jobIds), changelistId, fixState, false);
                return null;
            }
        });
    }


    public void removeJobsFromChangelist(final int changelistId,
            @NotNull final Collection<String> jobIds)
            throws VcsException, CancellationException {
        exec.runWithClient(project, new WithClient<Void>() {
            @Override
            public Void run(@NotNull final IOptionsServer server, @NotNull final IClient client,
                    @NotNull final ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
                    P4Exception {
                server.fixJobs(new ArrayList<String>(jobIds), changelistId, null, true);
                return null;
            }
        });
    }


    @NotNull
    public List<IChangelistSummary> getPendingClientChangelists()
            throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<List<IChangelistSummary>>() {
            @Override
            public List<IChangelistSummary> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count) throws P4JavaException {
                count.invoke("getChangelists");
                return server.getChangelists(0,
                        Collections.<IFileSpec>emptyList(),
                        client.getName(), null, false, false, true, true);
            }
        });
    }


    @NotNull
    public IChangelist createChangeList(@NotNull final String comment)
            throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<IChangelist>() {
            @Override
            public IChangelist run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                Changelist newChange = new Changelist();
                newChange.setUsername(getUsername());
                newChange.setClientId(client.getName());
                newChange.setDescription(comment);

                count.invoke("createChangelist");
                IChangelist ret = client.createChangelist(newChange);
                if (ret.getId() <= 0) {
                    throw new P4Exception(P4Bundle.message("error.changelist.add.invalid-id", newChange.getId()));
                }

                // server cannot leave this method
                ret.setServer(null);

                return ret;
            }
        });
    }


    /**
     * Get the state of the file specs.  This should only be invoked when the {@code files} references
     * only files, not glob patterns.  The {@code files} must be fully escaped IFileSpec objects.
     *
     * @param files files
     * @return file status
     * @throws VcsException
     * @throws CancellationException
     */
    @NotNull
    public List<IExtendedFileSpec> getFileStatus(@NotNull final List<IFileSpec> files)
            throws VcsException, CancellationException {

        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        return exec.runWithClient(project, new ClientExec.WithClient<List<IExtendedFileSpec>>() {
            @Override
            public List<IExtendedFileSpec> run(@NotNull final IOptionsServer server,
                    @NotNull final IClient client, @NotNull final ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
                    P4Exception {
                count.invoke("getFileStatus");
                return getExtendedFiles(files, server);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> reopenFiles(@NotNull final List<IFileSpec> files,
            final int newChangelistId, @Nullable final String newFileType) throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("reopenFiles");
                return getErrors(client.reopenFiles(files, newChangelistId, newFileType));
            }
        });
    }


    @Nullable
    public String deletePendingChangelist(final int changelistId)
            throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<String>() {
            @Override
            public String run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("deletePendingChangelist");
                return server.deletePendingChangelist(changelistId);
            }
        });
    }

    @NotNull
    public List<P4StatusMessage> deleteFiles(@NotNull final List<IFileSpec> deleted,
            final int changelistId, final boolean deleteLocalFiles) throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("deleteFiles");
                return getErrors(client.deleteFiles(deleted, changelistId, deleteLocalFiles));
            }
        });
    }


    @NotNull
    public byte[] loadFile(@NotNull final IFileSpec spec)
            throws VcsException, CancellationException, IOException {
        return exec.runWithClient(project, new ClientExec.WithClient<byte[]>() {
            @Override
            public byte[] run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetFileContentsOptions fileContentsOptions = new GetFileContentsOptions(false, true);
                // setting "don't annotate files" to true means we ignore the revision
                fileContentsOptions.setDontAnnotateFiles(false);
                count.invoke("getFileContents");
                InputStream inp = server.getFileContents(Collections.singletonList(spec),
                        fileContentsOptions);
                if (inp == null) {
                    return null;
                }

                try {
                    byte[] buff = new byte[4096];
                    int len;
                    while ((len = inp.read(buff, 0, 4096)) > 0) {
                        baos.write(buff, 0, len);
                    }
                } finally {
                    // Note: be absolutely sure to close the InputStream that is returned.
                    inp.close();
                }
                return baos.toByteArray();
            }
        });
    }


    @NotNull
    public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(
            @NotNull final List<IFileSpec> depotFiles, final int maxRevisions)
            throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<Map<IFileSpec, List<IFileRevisionData>>>() {
            @Override
            public Map<IFileSpec, List<IFileRevisionData>> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("getRevisionHistoryOnline");

                return server.getRevisionHistory(depotFiles, maxRevisions, false, true, true, false);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> moveFile(@NotNull final IFileSpec source,
            @NotNull final IFileSpec target, final int changelistId, final boolean leaveLocalFiles)
            throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("moveFile");
                final List<IFileSpec> res = server.moveFile(changelistId,
                        false, leaveLocalFiles, null, source, target);
                if (LOG.isDebugEnabled()) {
                    if (res.isEmpty()) {
                        LOG.debug("no move file results?");
                    }
                    for (IFileSpec spec : res) {
                        LOG.debug("move file: " + spec.getOpStatus() + "/" + spec.getStatusMessage() + "/" + spec);
                    }
                }
                return getErrors(res);
            }
        });
    }

    @NotNull
    public List<IFileSpec> synchronizeFiles(@NotNull final List<IFileSpec> files,
            final boolean forceSync)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return exec.runWithClient(project, new ClientExec.WithClient<List<IFileSpec>>() {
            @Override
            public List<IFileSpec> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("sync");
                final List<IFileSpec> ret = client.sync(files,
                        new SyncOptions(forceSync, false, false, false, false));
                if (ret == null) {
                    return Collections.emptyList();
                }
                return ret;
            }
        });
    }

    @NotNull
    public List<IFileAnnotation> getAnnotationsFor(@NotNull final List<IFileSpec> specs)
            throws VcsException, CancellationException {
        return exec.runWithClient(project,
                new ClientExec.WithClient<List<IFileAnnotation>>() {
            @Override
            public List<IFileAnnotation> run(@NotNull IOptionsServer server, @NotNull IClient client,
                    @NotNull ClientExec.ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("getFileAnnotations");
                return server.getFileAnnotations(specs,
                        new GetFileAnnotationsOptions(
                                false, // allResults
                                false, // useChangeNumbers
                                false, // followBranches
                                false, // ignoreWhitespaceChanges
                                false, // ignoreWhitespace
                                true, // ignoreLineEndings
                                false // followAllIntegrations
                        ));
            }
        });
    }

    public void getServerInfo() throws VcsException, CancellationException {
        exec.runWithServer(project, new ClientExec.WithServer<Void>() {
            @Override
            public Void run(@NotNull IOptionsServer server, @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("getServerInfo");
                server.getServerInfo();
                return null;
            }
        });
    }

    @NotNull
    public List<P4StatusMessage> submit(final int changelistId,
            @NotNull final List<String> jobIds,
            @Nullable final String jobStatus) throws VcsException, CancellationException {
        return exec.runWithClient(project, new ClientExec.WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull final IOptionsServer server, @NotNull final IClient client,
                    @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("getChangelist");
                final IChangelist changelist = server.getChangelist(changelistId);
                SubmitOptions options = new SubmitOptions();
                options.setJobIds(jobIds);
                if (jobStatus != null) {
                    options.setJobStatus(jobStatus);
                }
                count.invoke("submit");
                return getErrors(changelist.submit(options));
            }
        });
    }


    /**
     * Returns the list of job status used by the server.  If there was a
     * problem reading the list, then the default list is returned instead.
     *
     * @return list of job status used by the server
     * @throws CancellationException
     */
    public List<String> getJobStatusValues() throws CancellationException {
        try {
            return exec.runWithServer(project, new ClientExec.WithServer<List<String>>() {
                @Override
                public List<String> run(@NotNull final IOptionsServer server, @NotNull ClientExec.ServerCount count)
                        throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                    count.invoke("getJobSpec");
                    final IJobSpec spec = server.getJobSpec();
                    final Map<String, List<String>> values = spec.getValues();
                    if (values != null && values.containsKey("Status")) {
                        return values.get("Status");
                    }
                    LOG.info("No Status values listed in job spec");
                    return P4ChangeListJob.DEFAULT_JOB_STATUS;
                }
            });
        } catch (VcsException e) {
            LOG.info("Could not access the job spec", e);
            return P4ChangeListJob.DEFAULT_JOB_STATUS;
        }
    }


    /**
     *
     * @param changelistId Perforce changelist id
     * @return null if there is no such changelist.
     * @throws VcsException
     * @throws CancellationException
     */
    @Nullable
    public Collection<String> getJobIdsForChangelist(final int changelistId) throws VcsException, CancellationException {
        if (changelistId <= IChangelist.DEFAULT) {
            // These changelists can never have a job associated with them.
            // Additionally, actually inquiring about the jobs will result
            // in returning *every job in Perforce*, which could potentially
            // be HUGE.
            return Collections.emptyList();
        }
        return exec.runWithServer(project, new ClientExec.WithServer<List<String>>() {
            @Override
            public List<String> run(@NotNull final IOptionsServer server, @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
                count.invoke("getChangelist");
                final IChangelist changelist = server.getChangelist(changelistId);
                if (changelist == null) {
                    return null;
                }

                count.invoke("getJobIds");
                final List<String> jobIds = changelist.getJobIds();
                LOG.debug("Changelist " + changelistId + " has " + jobIds.size() + " jobs");
                return jobIds;
            }
        });
    }


    @Nullable
    public P4JobState getJobForId(@NotNull final String jobId) throws VcsException, CancellationException {
        return exec.runWithServer(project, new ClientExec.WithServer<P4JobState>() {
            @Override
            public P4JobState run(@NotNull final IOptionsServer server, @NotNull ClientExec.ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                P4JobState job = null;
                LOG.debug("Loading information for job " + jobId);
                IJob iJob;
                count.invoke("getJob");
                try {
                    iJob = server.getJob(jobId);
                    job = iJob == null ? null : new P4JobState(iJob);
                } catch (RequestException re) {
                    // Bug #33
                    LOG.warn(re);
                    if (re.getMessage().contains("Syntax error in")) {
                        job = new P4JobState(jobId, P4Bundle.message("error.job.parse", jobId, re.getMessage()));
                    }
                }
                return job;
            }
        });
    }


    public int updateChangelist(final int changelistId, @Nullable final String comment,
            @NotNull final List<IFileSpec> files) throws VcsException, CancellationException {
        // Make sure we have the full depot path of the input files for comparison.
        List<IExtendedFileSpec> fullFileSpecs = getFileStatus(files);
        final Set<String> fileDepos = new HashSet<String>(fullFileSpecs.size());
        for (IExtendedFileSpec fullFileSpec : fullFileSpecs) {
            fileDepos.add(fullFileSpec.getDepotPathString());
        }
        return exec.runWithClient(project, new WithClient<Integer>() {
            @NotNull
            @Override
            public Integer run(@NotNull final IOptionsServer server, @NotNull final IClient client,
                    @NotNull final ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
                // Look if we can keep the current changelist
                if (changelistId > P4ChangeListId.P4_DEFAULT) {
                    final IChangelist changelist = server.getChangelist(changelistId);
                    if (changelist != null && changelist.getId() > P4ChangeListId.P4_DEFAULT) {
                        // Remove files only.  If more files are present than
                        // what are in the changelist, we'll have to reopen files.

                        final List<IFileSpec> activeFiles = changelist.getFiles(true);
                        final Iterator<IFileSpec> iter = activeFiles.iterator();
                        while (iter.hasNext()) {
                            final IFileSpec next = iter.next();
                            if (! fileDepos.remove(next.getDepotPathString())) {
                                iter.remove();
                            }
                        }
                        if (fileDepos.isEmpty()) {
                            // used all the files.
                            changelist.setDescription(comment);
                            changelist.update();
                            return changelist.getId();
                        }
                    }
                }

                // Need to create a new changelist, and move the selected files into it.
                Changelist replacement = new Changelist();
                replacement.setUsername(getUsername());
                replacement.setClientId(client.getName());
                replacement.setDescription(comment);

                IChangelist newChange = client.createChangelist(replacement);
                final List<IFileSpec> result = client.reopenFiles(files, newChange.getId(), null);
                final List<P4StatusMessage> errors = getErrors(result);

                // FIXME handle these better
                try {
                    P4StatusMessage.throwIfError(errors, false);
                } catch (VcsException e) {
                    throw new P4JavaException(e);
                }

                return newChange.getId();
            }
        });
    }


    @NotNull
    private MessageResult<List<IExtendedFileSpec>> loadFstatForFileResult(@NotNull IOptionsServer server,
            @NotNull List<IFileSpec> files, boolean markFileNotFoundAsValid) throws P4JavaException, P4FileException {
        // We will need to get the full information on these files, so grab that, too.
        // We keep a list of all the returned specs that were messages, and include the
        // specs for the fstat results.  The result from the "opened" operation will
        // only ever return the depot path.
        List<IExtendedFileSpec> retSpecs = new ArrayList<IExtendedFileSpec>(files.size());
        List<IFileSpec> fstatSpecs = new ArrayList<IFileSpec>(files.size());
        for (IFileSpec spec : files) {
            if (spec.getDepotPathString() != null &&
                    (P4StatusMessage.isValid(spec) || P4StatusMessage.isFileNotFoundError(spec))) {
                // File is either in Perforce, or not in Perforce but is added for open,
                // or something else.
                // the old path is already escaped.  just shove it into a new file spec to
                // strip off any extra information we don't want to query.
                spec = FileSpecUtil.getAlreadyEscapedSpec(spec.getDepotPathString());
                fstatSpecs.add(spec);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found spec message (not going to fstat on it): " + spec.getOpStatus() + "/" +
                            spec.getStatusMessage());
                }
                retSpecs.add(new ExtendedFileSpec(spec.getStatusMessage()));
            }
        }
        if (! fstatSpecs.isEmpty()) {
            final List<IExtendedFileSpec> fstatRet = getExtendedFiles(fstatSpecs, server);
            retSpecs.addAll(fstatRet);
        }
        return MessageResult.create(retSpecs, markFileNotFoundAsValid);
    }



    private List<IExtendedFileSpec> getExtendedFiles(@NotNull List<IFileSpec> fstatSpecs,
            @NotNull IOptionsServer server) throws P4JavaException {
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions(
                "-m", Integer.toString(fstatSpecs.size()));
        final List<IExtendedFileSpec> specs = server.getExtendedFiles(fstatSpecs, opts);

        // Make sure the specs are unescaped on return
        for (IExtendedFileSpec spec : specs) {
            //LOG.info(" >>> " + spec.getDepotPathString());
            // this needs to be done *juuust* right, otherwise it escapes for us.
            spec.setPath(new FilePath(PathType.DEPOT,
                    FileSpecUtil.unescapeP4PathNullable(spec.getDepotPathString()),
                    true));
            // client path string is already unescaped, so don't touch it
            //spec.setClientPath(FileSpecUtil.unescapeP4PathNullable(spec.getClientPathString()));

            // original is usually messed up - it strips off the necessary escaping
            // (e.g. if path is //a@b, this will be //a), so we make it look identical
            // to the depot path.
            spec.setPath(new FilePath(PathType.ORIGINAL,
                    FileSpecUtil.unescapeP4PathNullable(spec.getDepotPathString()),
                    true));

            // local path is almost always null, so explicitly make it so
            // spec.setLocalPath(null);

            //LOG.info(" depot " + spec.getDepotPathString());
            //LOG.info(" client: " + spec.getClientPathString());

            // an "unknown" action with null head action means it's been
            // open for add.  This looks like a weird bug with the P4Java API
            if (spec.getAction() == FileAction.UNKNOWN && spec.getHeadAction() == null) {
                spec.setAction(FileAction.ADD);
            }

            spec.setServer(null);
        }
        return specs;
    }
}
