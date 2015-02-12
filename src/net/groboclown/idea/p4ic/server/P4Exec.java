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
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.*;
import com.perforce.p4java.exception.*;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.generic.core.file.FilePath;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.IProgressCallback;
import net.groboclown.idea.p4ic.background.VcsFuture;
import net.groboclown.idea.p4ic.background.VcsSettableFuture;
import net.groboclown.idea.p4ic.config.PasswordStore;
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

/**
 * Runs the commands against the server/client.  Handles the reconnection and login requirements.
 * The connection will remain open (or will be retried) until a {@link #dispose()}
 * call happens.
 */
public class P4Exec {
    private static final Logger LOG = Logger.getInstance(P4Exec.class);

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
        if (! connectionHandler.isConfigValid(serverStatus.getConfig())) {
            throw new P4InvalidConfigException();
        }
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
                public List<IClientSummary> run(@NotNull IOptionsServer server) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
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
    public IClientSummary getClient(@NotNull Project project) throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<IClientSummary>() {
            @Override
            public IClientSummary run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
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
            public IChangelist run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                IChangelist cl = getChangelist(server, id);
                if (id != cl.getId()) {
                    LOG.warn("Perforce Java API error: returned changelist with id " + cl.getId() + " when requested " + id);
                    //throw new P4Exception("Perforce Java API error: returned changelist with id " + cl.getId() + " when requested " + id);
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


    @NotNull
    public List<P4FileInfo> loadFileInfo(@NotNull Project project, @NotNull List<IFileSpec> fileSpecs)
            throws VcsException, CancellationException {
        return runWithClient(project, new P4FileInfo.FstatLoadSpecs(fileSpecs));
    }


    @NotNull
    public List<P4FileInfo> loadOpenedFiles(@NotNull Project project, @NotNull List<IFileSpec> openedSpecs)
            throws VcsException, CancellationException {
        LOG.info("loading open files " + openedSpecs);
        return runWithClient(project, new P4FileInfo.OpenedSpecs(openedSpecs));
    }


    @NotNull
    public List<P4StatusMessage> revertFiles(@NotNull Project project, @NotNull final List<IFileSpec> files)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
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
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                List<IFileSpec> ret = new ArrayList<IFileSpec>();
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
                    throw new P4Exception("Adding a non-existent file: " + f);
                }
                // We must set the original path the hard way, to avoid the FilePath
                // stripping off the stuff after the '#' or '@', if it was escaped originally.
                file.setPath(new FilePath(FilePath.PathType.ORIGINAL, f.getAbsolutePath(), true));
            } else if (file.getLocalPathString() == null) {
                throw new IllegalStateException("Must pass client spec setting to addFile");
            }
            unescapedFiles.add(file);
        }


        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                List<IFileSpec> ret = new ArrayList<IFileSpec>();
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
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                List<IFileSpec> ret = new ArrayList<IFileSpec>();
                // TODO ensure this allows for wildcard characters (*, #, @) in the file name.
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
            public Void run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
                // Note that, because the server connection can be closed after an
                // invocation, we must perform all the changelist updates within this
                // call, and we can't go outside this method.

                IChangelist changelist = getChangelist(server, changelistId);
                if (changelist != null && changelist.getStatus() == ChangelistStatus.PENDING) {
                    changelist.setDescription(description);
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
            public List<IChangelistSummary> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException {
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
            public IChangelist run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                Changelist newChange = new Changelist();
                newChange.setUsername(serverStatus.getConfig().getUsername());
                newChange.setClientId(client.getName());
                newChange.setDescription(comment);

                // This version doesn't do the right thing.  It incorrectly
                // returns a changelist with a -1 id.
                //IChangelist ret = client.createChangelist(newChange);
                int newId = createChangelist(server, client, newChange);
                if (newId <= 0) {
                    throw new P4Exception("P4JavaAPI returned a changelist with an invalid changelist id: " + newId);
                }
                newChange.setId(newId);
                newChange.setServer(null);
                newChange.setServerImpl(null);
                newChange.setStatus(ChangelistStatus.PENDING);
                if (newChange.getId() <= 0) {
                    throw new P4Exception("P4JavaAPI returned a changelist with an invalid changelist id: " + newChange.getId());
                }

                // server cannot leave this method
                newChange.setServer(null);

                return newChange;
            }
        });
    }


    @Nullable
    public List<P4FileInfo> getFilesInChangelist(@NotNull Project project, final int id)
            throws VcsException, CancellationException {
        final List<IFileSpec> files = getFileSpecsInChangelist(project, id);
        if (files == null) {
            return null;
        }
        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        return runWithClient(project, new P4FileInfo.FstatLoadSpecs(files));
    }


    @Nullable
    public List<IFileSpec> getFileSpecsInChangelist(@NotNull Project project, final int id)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<IFileSpec>>() {
            @Override
            public List<IFileSpec> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                IChangelist cl = getChangelist(server, id);
                if (cl == null) {
                    return null;
                }
                return cl.getFiles(false);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> reopenFiles(@NotNull Project project, @NotNull final List<IFileSpec> files,
            final int newChangelistId, @Nullable final String newFileType) throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                return getErrors(client.reopenFiles(files, newChangelistId, newFileType));
            }
        });
    }


    @Nullable
    public String deletePendingChangelist(@NotNull Project project, final int changelistId)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<String>() {
            @Override
            public String run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                return server.deletePendingChangelist(changelistId);
            }
        });
    }

    @NotNull
    public List<P4StatusMessage> deleteFiles(@NotNull Project project, @NotNull final List<IFileSpec> deleted,
            final int changelistId, final boolean deleteLocalFiles) throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                return getErrors(client.deleteFiles(deleted, changelistId, deleteLocalFiles));
            }
        });
    }


    @NotNull
    public byte[] loadFile(@NotNull Project project, @NotNull final IFileSpec spec)
            throws VcsException, CancellationException, IOException {

        return runWithClient(project, new WithClient<byte[]>() {
            @Override
            public byte[] run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetFileContentsOptions fileContentsOptions = new GetFileContentsOptions(false, true);
                // setting "don't annotate files" to true means we ignore the revision
                fileContentsOptions.setDontAnnotateFiles(false);
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
                byte[] ret = baos.toByteArray();
                return ret;
            }
        });
    }


    @NotNull
    public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(@NotNull Project project,
            @NotNull final List<IFileSpec> depotFiles, final int maxRevisions)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<Map<IFileSpec, List<IFileRevisionData>>>() {
            @Override
            public Map<IFileSpec, List<IFileRevisionData>> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                return server.getRevisionHistory(depotFiles, maxRevisions, false, true, true, false);
            }
        });
    }


    public List<P4StatusMessage> moveFile(@NotNull Project project, @NotNull final IFileSpec source,
            @NotNull final IFileSpec target, final int changelistId, final boolean leaveLocalFiles)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                return getErrors(server.moveFile(changelistId, false, leaveLocalFiles, null,
                        source, target));
            }
        });
    }

    public void synchronizeFiles(@NotNull Project project, @NotNull final List<IFileSpec> files)
            throws VcsException, CancellationException {
        // FIXME
        runWithClient(project, new WithClient<Void>() {
            @Override
            public Void run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                client.sync(files, new SyncOptions(false, false, false, false, true));
                return null;
            }
        });
    }

    public List<IFileAnnotation> getAnnotationsFor(@NotNull Project project, @NotNull final List<IFileSpec> specs)
            throws VcsException, CancellationException {
        return runWithClient(project, new WithClient<List<IFileAnnotation>>() {
            @Override
            public List<IFileAnnotation> run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
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
            public Void run(@NotNull IOptionsServer server) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                server.getServerInfo();
                return null;
            }
        });
    }


    protected <T> T runWithClient(@NotNull final Project project, @NotNull final WithClient<T> runner)
            throws VcsException, CancellationException {
        return p4RunFor(project, new P4Runner<T>() {
            @Override
            public T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                final IOptionsServer server = connectServer(getTempDir(project));

                // note: we're not caching the client
                final IClient client = loadClient(server);
                if (client == null) {
                    throw new ConfigException("Invalid client name: " + clientName);
                }

                // disconnect happens as a separate activity.
                return runner.run(server, client);
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
                return runner.run(connectServer(getTempDir(project)));
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
    private IOptionsServer connectServer(@NotNull File tempDir) throws P4JavaException, URISyntaxException {
        synchronized (sync) {
            if (disposed) {
                throw new ConnectionException("connection on disposed P4Exec");
            }
            if (cachedServer == null || ! cachedServer.isConnected()) {
                final Properties properties;
                final String url;
                final IOptionsServer server;
                properties = connectionHandler.getConnectionProperties(serverStatus.getConfig(), clientName);
                properties.setProperty(PropertyDefs.P4JAVA_TMP_DIR_KEY, tempDir.getAbsolutePath());
                url = connectionHandler.createUrl(serverStatus.getConfig());
                LOG.info("Opening connection to " + url + " with " + serverStatus.getConfig().getUsername());

                server = ServerFactory.getOptionsServer(url, properties);

                // for debugging
                //server = new P4ServerProxy(server);

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
                // FIXME should never get to this point; the online/offline status should
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
                        public Boolean run(@NotNull IOptionsServer server) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
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
                        public Boolean run(@NotNull IOptionsServer server) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException {
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

    @NotNull
    private static <T extends IFileOperationResult> List<P4StatusMessage> getErrors(
            Collection<T> specs) {
        List<P4StatusMessage> ret = new ArrayList<P4StatusMessage>();

        for (T spec : specs) {
            if (P4StatusMessage.isErrorStatus(spec)) {
                ret.add(new P4StatusMessage(spec));
            } else if (spec.getOpStatus() == FileSpecOpStatus.INFO) {
                LOG.info("result: " + spec.getStatusMessage());
            }
        }
        return ret;
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


    private static class LoggingCommandCallback implements ICommandCallback {

        @Override
        public void issuingServerCommand(int i, String s) {
            // System.out.println("P4 cmd start: " + i + " - " + s);
        }

        @Override
        public void completedServerCommand(int i, long l) {
            // System.out.println("P4 cmd finished: " + i + " - " + l);
        }

        @Override
        public void receivedServerInfoLine(int i, String s) {
            // System.out.println("P4 cmd info: " + i + " - " + s);
        }

        @Override
        public void receivedServerErrorLine(int i, String s) {
            LOG.warn("P4 cmd error: " + i + " - " + s);
        }

        @Override
        public void receivedServerMessage(int i, int i1, int i2, String s) {
            // System.out.println("P4 cmd msg: " + i + ":" + i1 + ":" + i2 + " - " + s);
        }
    }


    static class LoggingProgressCallback implements IProgressCallback {

        @Override
        public void start(int i) {
            // System.out.println("P4 cmd start progress: " + i);
        }

        @Override
        public boolean tick(int i, String s) {
            // System.out.println("P4 cmd progress: " + i + " - " + s);
            // If this returns "false", then this will stop the line processing
            // from the server.
            return true;
        }

        @Override
        public void stop(int i) {
            // System.out.println("P4 cmd stop progress: " + i);
        }
    }


    static interface P4Runner<T> {
        T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception;
    }


    static interface WithServer<T> {
        T run(@NotNull IOptionsServer server) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception;
    }


    static interface WithClient<T> {
        T run(@NotNull IOptionsServer server, @NotNull IClient client) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception;
    }


    /**
     * A re-implementation of the create changelist.  For some reason,
     * the server.getChangelist() command can not load the data correctly,
     * so we need to change this to make it return the correct ID.
     *
     * @param oServer server connection
     * @param client client
     * @param newChangelist changelist description
     * @return a positive changelist number
     * @throws ConnectionException
     * @throws RequestException
     * @throws AccessException
     */
    private static int createChangelist(IOptionsServer oServer, IClient client, IChangelist newChangelist) throws ConnectionException, RequestException, AccessException {
        LOG.info("creating changelist");
        if (client.getName() == null) {
            throw new NullPointerError("Null client name in newChangelist method call");
        } else if (newChangelist == null) {
            throw new NullPointerError("Null new change list specification in newChangelist method call");
        } else if (newChangelist.getId() != -1) {
            throw new RequestException("New changelist ID must be set to IChangelist.UNKNOWN");
        }

        Server server;
        if (oServer instanceof P4ServerProxy) {
            server = (Server) ((P4ServerProxy) oServer).getRealServer();
        } else {
            server = (Server) oServer;
        }

        List resultMaps = server.execMapCmdList(CmdSpec.CHANGE, new String[]{"-i"}, InputMapper.map(newChangelist));
        if (resultMaps != null) {
            for (Object resultMap : resultMaps) {
                Map<String, Object> map = (Map<String, Object>) resultMap;
                if (!server.handleErrorStr(map)) {
                    String infoStr;
                    if (map.containsKey("change")) {
                        infoStr = (String) map.get("change");
                        if (infoStr != null) {
                            LOG.info("+Read change line [" + infoStr + "]");
                            int spacePos = infoStr.indexOf(' ');
                            if (spacePos > 0 && spacePos < infoStr.length()) {
                                LOG.info("+Checking id in [" + infoStr.substring(spacePos + 1) + "]");
                                try {
                                    int id = Integer.parseInt(infoStr.substring(spacePos + 1));
                                    LOG.info("+Found id " + id);
                                    if (id > 0) {
                                        LOG.info("+returning id " + id);
                                        return id;
                                    }
                                } catch (Exception e) {
                                    LOG.info(e);
                                }
                            }
                        }
                    } else {
                        infoStr = server.getInfoStr(map);
                        LOG.info("info string: [" + infoStr + "]");
                        if (infoStr != null && infoStr.contains("Change ") && infoStr.contains(" created")) {
                            String[] strs1 = infoStr.split(" ");
                            if (strs1.length >= 3 && strs1[1] != null) {
                                try {
                                    int id = Integer.parseInt(strs1[1]);
                                    if (id > 0) {
                                        return id;
                                    }
                                } catch (Exception e) {
                                    LOG.info(e);
                                }
                            }
                        }
                    }
                }
            }
        }

        throw new IllegalStateException("Could not find new changelist ID");
    }


    /**
     * A reimplementation of IOptionsServer.getChangelist(int), which just
     * doesn't work - the Changelist constructor doesn't perform the correct
     * loading.  It looks like the code receives multiple lines from the
     * server, the last one being an empty set which is then parsed as a
     * new changelist with nothing.  The fix is to return the changelist
     * early, rather than later.
     *
     * @param oServer server connection
     * @param id changelist id
     * @return changelist with that ID, or null if it doesn't exist.
     */
    private static IChangelist getChangelist(IOptionsServer oServer, int id) throws P4JavaException {
        Server server;
        if (oServer instanceof P4ServerProxy) {
            server = (Server) ((P4ServerProxy) oServer).getRealServer();
        } else {
            server = (Server) oServer;
        }

        Changelist changeList = null;
        String[] args;
        if (id <= 0) {
            args = new String[]{"-o"};
        } else {
            args = new String[]{"-o", "" + id};
        }

        List<Map<String, Object>> resultMaps = (List<Map<String, Object>>) server.execMapCmdList(
                (CmdSpec) CmdSpec.CHANGE, Parameters.processParameters((ChangelistOptions)null,
                        (List) null, args, server), (Map) null);
        if (resultMaps != null) {
            for (Map<String, Object> map: resultMaps) {
                if (map != null) {
                    server.handleErrorStr(map);
                    if (!server.isInfoMessage(map)) {
                        changeList = new Changelist(map, server);
                        return changeList;
                    }
                }
            }
        }
        return null;
    }

}
