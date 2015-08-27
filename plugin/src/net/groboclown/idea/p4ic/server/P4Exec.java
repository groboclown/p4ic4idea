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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.*;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.*;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.Job;
import com.perforce.p4java.impl.generic.core.file.FilePath;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.background.VcsFuture;
import net.groboclown.idea.p4ic.background.VcsSettableFuture;
import net.groboclown.idea.p4ic.config.PasswordStore;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.*;
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
 * Runs the commands against the server/client.  Handles the reconnection and login requirements.
 * The connection will remain open (or will be retried) until a {@link #dispose()}
 * call happens.
 */
public class P4Exec {
    private static final Logger LOG = Logger.getInstance(P4Exec.class);

    // Default list of status, in case of a problem.
    public static final List<String> DEFAULT_JOB_STATUS = Arrays.asList(
            "open", "suspended", "closed"
    );

    private static final AllServerCount SERVER_COUNT = new AllServerCount();

    private final Object sync = new Object();

    @NotNull
    private final ServerStatus serverStatus;

    @Nullable
    private final String clientName;

    @NotNull
    private final ConnectionHandler connectionHandler;

    @NotNull
    private final OnServerConfigurationProblem onServerProblem;

    private boolean disposed = false;

    @Nullable
    private IOptionsServer cachedServer;


    public P4Exec(@NotNull ServerStatus serverStatus, @Nullable String clientName,
                  @NotNull ConnectionHandler connectionHandler, @NotNull OnServerConfigurationProblem onServerProblem)
            throws P4InvalidConfigException {
        this.serverStatus = serverStatus;
        this.clientName = clientName;
        this.connectionHandler = connectionHandler;
        this.onServerProblem = onServerProblem;
        connectionHandler.validateConfiguration(null, serverStatus.getConfig());
    }


    @Nullable
    public String getClientName() {
        return clientName;
    }


    @NotNull
    public ServerConfig getServerConfig() {
        return serverStatus.getConfig();
    }


    public void dispose() {
        // in the future, this may clean up open connections
        synchronized (sync) {
            if (! disposed) {
                disposed = true;
            }
            invalidateCache();
        }
    }


    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }


    public List<IClientSummary> getClientsForUser(@NotNull Project project) throws VcsConnectionProblem {
        try {
            return runWithServer(project, new WithServer<List<IClientSummary>>() {
                @Override
                public List<IClientSummary> run(@NotNull IOptionsServer server, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
                    count.invoke("getClients");
                    List<IClientSummary> ret = server.getClients(serverStatus.getConfig().getUsername(), null, 0);
                    assert ret != null;
                    return ret;
                }
            });
        } catch (VcsConnectionProblem e) {
            throw e;
        } catch (VcsException e) {
            LOG.warn("Raised a general VCS exception", e);
            throw new P4DisconnectedException(e);
        }
    }


    @NotNull
    public IClient getClient(@NotNull Project project) throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<IClient>() {
            @Override
            public IClient run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                client.setServer(null);
                return client;
            }
        });
    }


    @Nullable
    public IChangelist getChangelist(@NotNull Project project, final int id)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<IChangelist>() {
            @Override
            public IChangelist run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
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
     * Returns the information for each file spec passed in.  If the file spec list
     * is for a precise list of files (not a wildcard), then the returned info
     * will match the order that was passed in.
     *
     * @param project
     * @param fileSpecs
     * @param fileInfoCache
     * @return the in-order file info related to the file specs.
     * @throws VcsException
     * @throws CancellationException
     */
    @NotNull
    public List<P4FileInfo> loadFileInfo(@NotNull Project project, @NotNull List<IFileSpec> fileSpecs, @NotNull FileInfoCache fileInfoCache)
            throws VcsException, CancellationException {
        // Avoid the dreaded "Usage: fstat ..." error.
        if (! fileSpecs.isEmpty()) {
            return runWithClient(project, new P4FileInfo.FstatLoadSpecs(fileSpecs, fileInfoCache));
        }
        return Collections.emptyList();
    }


    @NotNull
    public List<P4FileInfo> loadOpenedFiles(@NotNull Project project, @NotNull List<IFileSpec> openedSpecs, @NotNull FileInfoCache fileInfoCache)
            throws VcsException, CancellationException {
        LOG.debug("loading open files " + openedSpecs);
        return runWithClient(project, new P4FileInfo.OpenedSpecs(openedSpecs, fileInfoCache));
    }


    @NotNull
    public List<P4StatusMessage> revertFiles(@NotNull Project project, @NotNull final List<IFileSpec> files)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
                count.invoke("revertFiles");
                List<IFileSpec> ret = client.revertFiles(files, false, -1, false, false);
                return getErrors(ret);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> integrateFiles(@NotNull Project project, @NotNull final IFileSpec src,
            @NotNull final IFileSpec target, final int changelistId, final boolean dontCopyToClient)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
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
    public List<P4StatusMessage> addFiles(@NotNull Project project, @NotNull List<IFileSpec> files,
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

        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
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
    public List<P4StatusMessage> editFiles(@NotNull Project project, @NotNull final List<IFileSpec> files,
            final int changelistId) throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
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


    public void updateChangelistDescription(@NotNull Project project, final int changelistId,
            @NotNull final String description) throws VcsException, CancellationException {
        runWithClient(project, new WithClient<Void>() {
            @Override
            public Void run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
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


    @NotNull
    public List<IChangelistSummary> getPendingClientChangelists(@NotNull Project project)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<IChangelistSummary>>() {
            @Override
            public List<IChangelistSummary> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException {
                count.invoke("getChangelists");
                return server.getChangelists(0,
                        Collections.<IFileSpec>emptyList(),
                        client.getName(), null, false, false, true, true);
            }
        });
    }


    @NotNull
    public IChangelist createChangeList(@NotNull Project project, @NotNull final String comment)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<IChangelist>() {
            @Override
            public IChangelist run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                Changelist newChange = new Changelist();
                newChange.setUsername(serverStatus.getConfig().getUsername());
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


    @Nullable
    public List<P4FileInfo> getFilesInChangelist(@NotNull Project project, final int id, @NotNull FileInfoCache fileInfoCache)
            throws VcsException, CancellationException {
        final List<IFileSpec> files = getFileSpecsInChangelist(project, id);
        if (files == null) {
            return null;
        }
        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        return runWithClient(project, new P4FileInfo.FstatLoadSpecs(files, fileInfoCache));
    }


    @Nullable
    public List<IFileSpec> getFileSpecsInChangelist(@NotNull Project project, final int id)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<IFileSpec>>() {
            @Override
            public List<IFileSpec> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("getChangelist");
                IChangelist cl = server.getChangelist(id);
                if (cl == null) {
                    return null;
                }
                count.invoke("changelist.getFiles");
                return cl.getFiles(false);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> reopenFiles(@NotNull Project project, @NotNull final List<IFileSpec> files,
            final int newChangelistId, @Nullable final String newFileType) throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("reopenFiles");
                return getErrors(client.reopenFiles(files, newChangelistId, newFileType));
            }
        });
    }


    @Nullable
    public String deletePendingChangelist(@NotNull Project project, final int changelistId)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<String>() {
            @Override
            public String run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("deletePendingChangelist");
                return server.deletePendingChangelist(changelistId);
            }
        });
    }

    @NotNull
    public List<P4StatusMessage> deleteFiles(@NotNull Project project, @NotNull final List<IFileSpec> deleted,
            final int changelistId, final boolean deleteLocalFiles) throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("deleteFiles");
                return getErrors(client.deleteFiles(deleted, changelistId, deleteLocalFiles));
            }
        });
    }


    @NotNull
    public byte[] loadFile(@NotNull Project project, @NotNull final IFileSpec spec)
            throws VcsException, CancellationException, IOException {

        return runWithClient(project, new WithClient<byte[]>() {
            @Override
            public byte[] run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetFileContentsOptions fileContentsOptions = new GetFileContentsOptions(false, true);
                // setting "don't annotate files" to true means we ignore the revision
                fileContentsOptions.setDontAnnotateFiles(false);
                count.invoke("getFileContents");
                InputStream inp = server.getFileContents(Collections.singletonList(spec),
                        fileContentsOptions);

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
    public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(@NotNull Project project,
            @NotNull final List<IFileSpec> depotFiles, final int maxRevisions)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<Map<IFileSpec, List<IFileRevisionData>>>() {
            @Override
            public Map<IFileSpec, List<IFileRevisionData>> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("getRevisionHistory");

                return server.getRevisionHistory(depotFiles, maxRevisions, false, true, true, false);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> moveFile(@NotNull Project project, @NotNull final IFileSpec source,
            @NotNull final IFileSpec target, final int changelistId, final boolean leaveLocalFiles)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("moveFile");
                return getErrors(server.moveFile(changelistId, false, leaveLocalFiles, null,
                        source, target));
            }
        });
    }

    @NotNull
    public List<IFileSpec> synchronizeFiles(@NotNull Project project, @NotNull final List<IFileSpec> files,
            final boolean forceSync)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<IFileSpec>>() {
            @Override
            public List<IFileSpec> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("sync");
                final List<IFileSpec> ret = client.sync(files, new SyncOptions(forceSync, false, false, false, true));
                if (ret == null) {
                    return Collections.emptyList();
                }
                return ret;
            }
        });
    }

    @NotNull
    public List<IFileAnnotation> getAnnotationsFor(@NotNull Project project, @NotNull final List<IFileSpec> specs)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<IFileAnnotation>>() {
            @Override
            public List<IFileAnnotation> run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
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

    public void getServerInfo(@NotNull Project project) throws VcsException, CancellationException {
        runWithServer(project, new WithServer<Void>() {
            @Override
            public Void run(@NotNull IOptionsServer server, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                count.invoke("getServerInfo");
                server.getServerInfo();
                return null;
            }
        });
    }

    @NotNull
    public List<P4StatusMessage> submit(@NotNull final Project project, final int changelistId,
            @NotNull final List<String> jobIds,
            @Nullable final String jobStatus) throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull final IOptionsServer server, @NotNull final IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
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

    @NotNull
    public Collection<P4FileInfo> revertUnchangedFiles(final Project project,
            @NotNull final List<IFileSpec> fileSpecs, final int changeListId,
            @NotNull final List<P4StatusMessage> errors, @NotNull FileInfoCache fileInfoCache)
            throws VcsException, CancellationException {
        final List<IFileSpec> reverted = runWithClient(project, new WithClient<List<IFileSpec>>() {
            @Override
            public List<IFileSpec> run(@NotNull final IOptionsServer server, @NotNull final IClient client, @NotNull final ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                RevertFilesOptions options = new RevertFilesOptions(false, changeListId, true, false);
                count.invoke("revertFiles");
                final List<IFileSpec> results = client.revertFiles(fileSpecs, options);
                List<IFileSpec> reverted = new ArrayList<IFileSpec>(results.size());
                for (IFileSpec spec : results) {
                    if (P4StatusMessage.isErrorStatus(spec)) {
                        final P4StatusMessage msg = new P4StatusMessage(spec);
                        if (!msg.isFileNotFoundError()) {
                            errors.add(msg);
                        }
                    } else {
                        LOG.info("Revert for spec " + spec + ": action " + spec.getAction());
                        reverted.add(spec);
                    }
                }
                LOG.info("reverted specs: " + reverted);
                LOG.info("reverted errors: " + errors);
                return reverted;
            }
        });
        return runWithClient(project, new P4FileInfo.FstatLoadSpecs(reverted, fileInfoCache));
    }


    /**
     * Returns the list of job status used by the server.  If there was a
     * problem reading the list, then the default list is returned instead.
     *
     * @param project project
     * @return list of job status used by the server
     * @throws CancellationException
     */
    public List<String> getJobStatusValues(@NotNull final Project project) throws CancellationException {
        try {
            return runWithServer(project, new WithServer<List<String>>() {
                @Override
                public List<String> run(@NotNull final IOptionsServer server, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                    count.invoke("getJobSpec");
                    final IJobSpec spec = server.getJobSpec();
                    final Map<String, List<String>> values = spec.getValues();
                    if (values != null && values.containsKey("Status")) {
                        return values.get("Status");
                    }
                    LOG.info("No Status values listed in job spec");
                    return DEFAULT_JOB_STATUS;
                }
            });
        } catch (VcsException e) {
            LOG.info("Could not access the job spec", e);
            return DEFAULT_JOB_STATUS;
        }
    }


    /**
     *
     * @param project project
     * @param changelistId Perforce changelist id
     * @return null if there is no such changelist.
     * @throws VcsException
     * @throws CancellationException
     */
    @Nullable
    public Collection<String> getJobIdsForChangelist(final Project project, final int changelistId) throws VcsException, CancellationException {
        if (changelistId <= IChangelist.DEFAULT) {
            // These changelists can never have a job associated with them.
            // Additionally, actually inquiring about the jobs will result
            // in returning *every job in Perforce*, which could potentially
            // be HUGE.
            return Collections.emptyList();
        }
        return runWithServer(project, new WithServer<List<String>>() {
            @Override
            public List<String> run(@NotNull final IOptionsServer server, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
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
    public P4Job getJobForId(@NotNull final Project project, @NotNull final String jobId) throws VcsException, CancellationException {
        return runWithServer(project, new WithServer<P4Job>() {
            @Override
            public P4Job run(@NotNull final IOptionsServer server, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                P4Job job = null;
                LOG.debug("Loading information for job " + jobId);
                IJob iJob;
                count.invoke("getJob");
                try {
                    iJob = server.getJob(jobId);
                    job = iJob == null ? null : new P4Job(iJob);
                } catch (RequestException re) {
                    // Bug #33
                    LOG.warn(re);
                    if (re.getMessage().contains("Syntax error in")) {
                        job = new P4Job(jobId, P4Bundle.message("error.job.parse", jobId, re.getMessage()));
                    }
                }
                return job;
            }
        });
    }


    protected <T> T runWithClient(@NotNull final Project project, @NotNull final WithClient<T> runner)
            throws VcsException, CancellationException {
        return p4RunFor(project, new P4Runner<T>() {
            @Override
            public T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                final IOptionsServer server = connectServer(project, getTempDir(project));

                // note: we're not caching the client
                final IClient client = loadClient(server);
                if (client == null) {
                    throw new ConfigException(P4Bundle.message("error.run-client.invalid-client", clientName));
                }

                // disconnect happens as a separate activity.
                return runner.run(server, client, new WithClientCount(serverStatus.getConfig().getServiceName(), clientName));
            }
        });
    }


    private <T> T runWithServer(@NotNull Project project, @NotNull final WithServer<T> runner)
            throws VcsException, CancellationException {
        return runWithServer(project, runner, false);
    }


    private <T> T runWithServer(@NotNull final Project project, @NotNull final WithServer<T> runner, boolean triedLogin)
            throws VcsException, CancellationException {
        return p4RunFor(project, new P4Runner<T>() {
            @Override
            public T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                // disconnect happens as a separate activity.
                return runner.run(connectServer(project, getTempDir(project)), new WithClientCount(serverStatus.getConfig().getServiceName()));
            }
        });
    }


    // Seems like a hack...
    @NotNull
    private File getTempDir(@NotNull Project project) {
        return P4Vcs.getInstance(project).getTempDir();
    }


    private void invalidateCache() {
        synchronized (sync) {
            if (cachedServer != null) {
                try {
                    cachedServer.disconnect();
                } catch (ConnectionException e) {
                    LOG.debug("error on disconnect", e);
                } catch (AccessException e) {
                    LOG.debug("error on disconnect", e);
                } finally {
                    cachedServer = null;
                }
            }

        }
    }


    @NotNull
    private IOptionsServer connectServer(@NotNull final Project project, @NotNull final File tempDir) throws P4JavaException, URISyntaxException {
        synchronized (sync) {
            if (disposed) {
                throw new ConnectionException(P4Bundle.message("error.p4exec.disposed"));
            }
            if (cachedServer == null || ! cachedServer.isConnected()) {
                final Properties properties;
                final String url;
                final IOptionsServer server;
                properties = connectionHandler.getConnectionProperties(serverStatus.getConfig(), clientName);
                properties.setProperty(PropertyDefs.P4JAVA_TMP_DIR_KEY, tempDir.getAbsolutePath());
                url = connectionHandler.createUrl(serverStatus.getConfig());
                LOG.info("Opening connection to " + url + " with " + serverStatus.getConfig().getUsername());

                // see bug #61
                // Hostname as used by the Java code:
                //   Mac clients can incorrectly set the hostname.
                //   The underlying code will use:
                //      InetAddress.getLocalHost().getHostName()
                //   or from the UsageOptions passed into the
                //   server configuration `init` method.

                // Use the ConnectionHandler so that mock objects can work better
                server = connectionHandler.getOptionsServer(url, properties, serverStatus.getConfig());

                // These seem to cause issues.
                //server.registerCallback(new LoggingCommandCallback());
                //server.registerProgressCallback(new LoggingProgressCallback());

                server.connect();

                cachedServer = server;

                // if there is a password problem, we still want
                // to maintain our cached server, so a retry doesn't
                // recreate the server connection again.
                char[] password = PasswordStore.getOptionalPasswordFor(serverStatus.getConfig());
                try {
                    connectionHandler.defaultAuthentication(server, serverStatus.getConfig(), password);
                } finally {
                    if (password != null) {
                        Arrays.fill(password, (char) 0);
                    }
                }
            }
        }


        return cachedServer;
    }


    @Nullable
    private IClient loadClient(@NotNull final IOptionsServer server) throws ConnectionException, AccessException, RequestException {
        if (clientName == null) {
            return null;
        }
        IClient client = server.getClient(clientName);
        if (client != null) {
            LOG.debug("Connected to client " + clientName);
            server.setCurrentClient(client);
        }
        return client;
    }


    private <T> T p4RunFor(@NotNull Project project, @NotNull P4Runner<T> runner)
            throws VcsException, CancellationException {
        return p4RunFor(project, runner, false);
    }


    private <T> T p4RunFor(@NotNull Project project, @NotNull P4Runner<T> runner, boolean triedLogin)
            throws VcsException, CancellationException {
        while (true) {
            // Must check offline status in the loop.
            if (serverStatus.isWorkingOffline()) {
                // should never get to this point; the online/offline status should
                // have already been determined before entering this method.
                invalidateCache();
                throw new P4WorkingOfflineException();
            }
            try {
                return runner.run();
            } catch (ClientError e) {
                LOG.warn("ClientError in P4JavaApi", e);
                throw new P4ApiException(e);
            } catch (NullPointerError e) {
                LOG.warn("NullPointerException in P4JavaApi", e);
                throw new P4ApiException(e);
            } catch (ProtocolError e) {
                LOG.warn("ProtocolError in P4JavaApi", e);
                VcsSettableFuture<Boolean> future = VcsSettableFuture.create();
                onServerProblem.onInvalidConfiguration(
                        future, serverStatus.getConfig(), e.getMessage());
                Boolean b = getWithoutCancel(future);
                if (b == null || b == Boolean.FALSE) {
                    throw new P4ApiException(e);
                }
                // else, run the loop again.
            } catch (UnimplementedError e) {
                LOG.warn("Unimplemented API in P4JavaApi", e);
                throw new P4ApiException(e);
            } catch (P4JavaError e) {
                LOG.warn("General error in P4JavaApi", e);
                throw new P4ApiException(e);
            } catch (AccessException e) {
                LOG.info("Problem accessing resources", e);
                if (isPasswordProblem(e)) {
                    onPasswordProblem(project, triedLogin, new P4LoginException(e));
                    triedLogin = true;
                } else {
                    VcsSettableFuture<Boolean> future = VcsSettableFuture.create();
                    onServerProblem.onInvalidConfiguration(future, serverStatus.getConfig(), e.getMessage());
                    Boolean b = getWithoutCancel(future);
                    if (b == null || b == Boolean.FALSE) {
                        // force offline mode
                        serverStatus.forceDisconnect();
                        throw new P4InvalidConfigException(e);
                    }
                    // else, run the loop again.
                }
            } catch (ConfigException e) {
                LOG.info("Problem with configuration", e);
                VcsSettableFuture<Boolean> future = VcsSettableFuture.create();
                onServerProblem.onInvalidConfiguration(
                        future, serverStatus.getConfig(), e.getMessage());
                Boolean b = getWithoutCancel(future);
                if (b == null || b == Boolean.FALSE) {
                    // force offline mode
                    serverStatus.forceDisconnect();
                    throw new P4InvalidConfigException(e);
                }
                // else, run the loop again.
            } catch (ConnectionNotConnectedException e) {
                LOG.info("Wasn't connected", e);
                invalidateCache();
                if (! serverStatus.onDisconnect()) {
                    throw new P4WorkingOfflineException(e);
                }
                // else, run the loop again.
            } catch (TrustException e) {
                LOG.info("SSL trust problem", e);
                VcsSettableFuture<Boolean> future = VcsSettableFuture.create();
                onServerProblem.onInvalidConfiguration(future,
                        serverStatus.getConfig(), e.getMessage());
                Boolean b = getWithoutCancel(future);
                if (b == null || b == Boolean.FALSE) {
                    throw new P4LoginException(e);
                }
                // else, run the loop again.
            } catch (ConnectionException e) {
                LOG.info("Connection problem", e);
                invalidateCache();

                if (isSSLFingerprintProblem(e)) {
                    // incorrect or not set trust fingerprint
                    // TODO pass the actual server fingerprint, rather than the whole message.
                    throw new P4SSLFingerprintException(serverStatus.getConfig().getServerFingerprint(), e);
                }


                if (isSSLHandshakeProblem(e)) {
                    // SSL extensions are not installed.
                    throw new P4JavaSSLStrengthException(e);
                }

                // Ask the user if it should be a real disconnect, or if we should
                // retry.
                if (! serverStatus.onDisconnect()) {
                    throw new P4WorkingOfflineException(e);
                }
            } catch (FileDecoderException e) {
                LOG.info("File decoder problem", e);
                throw new P4FileException(e);
            } catch (FileEncoderException e) {
                LOG.info("File encoder problem", e);
                throw new P4FileException(e);
            } catch (NoSuchObjectException e) {
                LOG.info("No such object problem", e);
                throw new P4Exception(e);
            } catch (OptionsException e) {
                LOG.info("Input options problem", e);
                throw new P4Exception(e);
            } catch (RequestException e) {
                LOG.info("Request problem", e);
                if (isPasswordProblem(e)) {
                    onPasswordProblem(project, triedLogin, new P4LoginException(e));
                    triedLogin = true;
                } else {
                    // Don't know what it really is
                    throw new P4Exception(e);
                }
            } catch (ResourceException e) {
                LOG.info("Resource problem", e);
                throw new P4Exception(e);
            } catch (P4JavaException e) {
                LOG.info("General Perforce problem", e);
                throw new P4Exception(e);
            } catch (IOException e) {
                LOG.info("IO problem", e);
                throw new P4Exception(e);
            } catch (URISyntaxException e) {
                LOG.info("Invalid URI", e);
                VcsSettableFuture<Boolean> future = VcsSettableFuture.create();
                onServerProblem.onInvalidConfiguration(future, serverStatus.getConfig(), e.getMessage());
                Boolean b = getWithoutCancel(future);
                if (b == null || b == Boolean.FALSE) {
                    throw new P4InvalidConfigException(e);
                }
                // else, run the loop again.
            } catch (CancellationException e) {
                // no need to catch; it's part of the throw clause
                throw e;
            } catch (InterruptedException e) {
                LOG.info("Cancelled", e);
                CancellationException ce = new CancellationException(e.getMessage());
                ce.initCause(e);
                throw ce;
            } catch (TimeoutException e) {
                // the equivalent of a cancel, because the limited time window
                // ran out.
                LOG.info("Timed out", e);
                CancellationException ce = new CancellationException(e.getMessage());
                ce.initCause(e);
                throw ce;
            } catch (VcsException e) {
                // P4Exec generated error
                throw e;
            } catch (ThreadDeath e) {
                // Never handle
                throw e;
            } catch (VirtualMachineError e) {
                // Never handle
                throw e;
            } catch (Throwable t) {
                if (t.getMessage() != null &&
                        t.getMessage().equals("Task was cancelled.")) {
                    CancellationException ce = new CancellationException(t.getMessage());
                    ce.initCause(t);
                    throw ce;
                }
                LOG.warn("Unexpected exception", t);
                throw new P4Exception(t);
            }
        }
    }

    private void onPasswordProblem(@NotNull Project project, boolean triedLogin, @NotNull P4LoginException e)
            throws VcsException {
        synchronized (serverStatus) {
            if (triedLogin) {
                final char[] password = PasswordStore.getRequiredPasswordFor(project, serverStatus.getConfig(), true);
                try {
                    boolean res = runWithServer(project, new WithServer<Boolean>() {
                        @Override
                        public Boolean run(@NotNull IOptionsServer server, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
                            count.invoke("forcedAuthentication");
                            return connectionHandler.forcedAuthentication(server, serverStatus.getConfig(), password);
                        }
                    }, true);
                    if (!res) {
                        throw e;
                    }
                } finally {
                    if (password != null) {
                        Arrays.fill(password, (char) 0);
                    }
                }
            } else {
                final char[] password = PasswordStore.getOptionalPasswordFor(serverStatus.getConfig());
                try {
                    runWithServer(project, new WithServer<Boolean>() {
                        @Override
                        public Boolean run(@NotNull IOptionsServer server, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
                            count.invoke("defaultAuthentication");
                            connectionHandler.defaultAuthentication(server, serverStatus.getConfig(), password);
                            return null;
                        }
                    }, true);
                } finally {
                    if (password != null) {
                        Arrays.fill(password, (char) 0);
                    }
                }
            }
        }
    }


    /**
     * Returns null on a cancellation.
     *
     * @param f future to get
     * @param <T> type of returned value
     * @return null on cancellation, or the result of the get call.
     * @throws VcsException
     */
    private static <T> T getWithoutCancel(VcsFuture<T> f) throws VcsException {
        try {
            return f.get();
        } catch (CancellationException e) {
            return null;
        }
    }


    private static boolean isPasswordProblem(@NotNull AccessException e) {
        // these kinds of exceptions are *always* password problems
        return true;
    }


    private static boolean isPasswordProblem(@NotNull RequestException e) {
        String message = e.getMessage();
        return (message != null &&
                (message.contains("Your session has expired, please login again.")
                        || message.contains("Perforce password (P4PASSWD) invalid or unset.")));
    }


    private boolean isSSLHandshakeProblem(@NotNull final ConnectionException e) {
        String message = e.getMessage();
        return message != null &&
            message.contains("invalid SSL session");
    }


    private boolean isSSLFingerprintProblem(@NotNull final ConnectionException e) {
        String message = e.getMessage();
        return message != null &&
                message.contains("The fingerprint for the public key sent to your client is");
    }


    private synchronized P4Job customGetJob(@NotNull final Server server, @NotNull final String jobId) throws ConnectionException, AccessException {
        List<Map<String, Object>> resultMaps = server.execMapCmdList(CmdSpec.JOB, new String[]{"-o", jobId}, null);
        if (resultMaps != null) {
            for (final Map<String, Object> resultMap : resultMaps) {
                final IServerMessage err = server.getErrorStr(resultMap);
                if (err != null) {
                    if (server.isAuthFail(err)) {
                        throw new AccessException(err);
                    } else {
                        final String errorMessage = P4Bundle.message("error.job.parse", jobId, resultMap.get("code0"));
                        LOG.error(errorMessage);
                        LOG.warn("Problem parsing job " + jobId + " with result maps: " + resultMaps);
                        // Still create the job, because it exists
                        return new P4Job(jobId, errorMessage);
                    }
                }
                if (!server.isInfoMessage(resultMap)) {
                    return new P4Job(new Job(server, resultMap));
                }
            }
        }
        return null;
    }


    interface P4Runner<T> {
        T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception;
    }


    interface WithServer<T> {
        T run(@NotNull IOptionsServer server, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception;
    }


    interface WithClient<T> {
        T run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception;
    }



    interface ServerCount {
        void invoke(@NotNull String operation);
    }


    // TODO in the future, this can be a source for analytical information
    // regarding the activity of the plugin with the different servers, including
    // rate of invocations, number of invocations for different calls, and so on.
    private static class AllServerCount {
        final Map<String, Map<String, Integer>> callCounts = new HashMap<String, Map<String, Integer>>();

        synchronized void invoke(@NotNull String operation, @NotNull String serverId, @NotNull String clientId) {
            Map<String, Integer> clientCount = callCounts.get(serverId);
            if (clientCount == null) {
                clientCount = new HashMap<String, Integer>();
                callCounts.put(serverId, clientCount);
            }
            Integer count = clientCount.get(clientId);
            if (count == null) {
                count = 0;
            }
            clientCount.put(clientId, count + 1);
            if (count + 1 % 100 == 0) {
                LOG.info("Invocations against " + serverId + " " + clientId + " = " + (count + 1));
            }
        }
    }

    private static class WithClientCount implements ServerCount {
        private final String serverId;
        private final String clientId;

        private WithClientCount(final String serverId) {
            this(serverId, "");
        }

        private WithClientCount(final String serverId, final String clientId) {
            this.serverId = serverId;
            this.clientId = clientId;
        }

        @Override
        public void invoke(@NotNull final String operation) {
            SERVER_COUNT.invoke(operation, serverId, clientId);
        }
    }

}
