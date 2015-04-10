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
package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.diff.RevisionSelector;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.merge.MergeProvider;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.background.TempFileWatchDog;
import net.groboclown.idea.p4ic.background.VcsFutureSetter;
import net.groboclown.idea.p4ic.background.VcsSettableFuture;
import net.groboclown.idea.p4ic.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.changes.P4ChangeProvider;
import net.groboclown.idea.p4ic.changes.P4ChangelistListener;
import net.groboclown.idea.p4ic.changes.P4CommittedChangeList;
import net.groboclown.idea.p4ic.compat.CompatFactoryLoader;
import net.groboclown.idea.p4ic.compat.UICompat;
import net.groboclown.idea.p4ic.compat.VcsCompat;
import net.groboclown.idea.p4ic.config.*;
import net.groboclown.idea.p4ic.history.P4AnnotationProvider;
import net.groboclown.idea.p4ic.history.P4DiffProvider;
import net.groboclown.idea.p4ic.history.P4HistoryProvider;
import net.groboclown.idea.p4ic.server.ClientManager;
import net.groboclown.idea.p4ic.server.OnServerConfigurationProblem;
import net.groboclown.idea.p4ic.server.OnServerDisconnectListener;
import net.groboclown.idea.p4ic.server.ServerStoreService;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.ui.P4ConnectionWidget;
import net.groboclown.idea.p4ic.ui.P4ProjectConfigurable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class P4Vcs extends AbstractVcs<P4CommittedChangeList> {
    private static final Logger LOG = Logger.getInstance(P4Vcs.class);

    private static final FileStatus[] PREFERRED_STATUS = new FileStatus[] {
            FileStatus.ADDED,
            FileStatus.DELETED,
            FileStatus.IGNORED,
            FileStatus.MERGE,
            FileStatus.MODIFIED,
            FileStatus.NOT_CHANGED
    };

    @NonNls
    public static final String VCS_NAME = "p4ic";

    private static final VcsKey VCS_KEY = createKey(VCS_NAME);

    @NotNull
    private final Configurable myConfigurable;

    @NotNull
    private final P4ChangeListMapping changeListMapping;

    @NotNull
    private final P4HistoryProvider historyProvider;

    private final P4StatusUpdateEnvironment statusUpdateEnvironment;

    private final P4CommittedChangesProvider committedChangesProvider;

    private final TempFileWatchDog tempFileWatchDog;

    private P4ConnectionWidget connectionWidget;

    private P4VFSListener myVFSListener;

    private EditFileProvider editProvider;

    // Not used
    //private final CommitExecutor commitExecutor;

    private final P4ChangelistListener changelistListener;

    private final P4ChangeProvider changeProvider;

    private final DiffProvider diffProvider;

    @NotNull
    private final ConfigListener problemListener;

    @NotNull
    private final OnServerDisconnectListener disconnectListener;

    private final P4AnnotationProvider annotationProvider;

    private final Object vfsSync = new Object();

    private final ClientManager clients;

    private boolean autoOffline = false;


    @NotNull
    public static P4Vcs getInstance(Project project) {
        if (project == null || project.isDisposed()) {
            throw new NullPointerException("No active project");
        }
        P4Vcs ret = (P4Vcs) ProjectLevelVcsManager.getInstance(project).findVcsByName(VCS_NAME);
        if (ret == null) {
            throw new NullPointerException("No active project");
        }
        return ret;
    }

    public P4Vcs(
            @NotNull Project project,
            @NotNull P4ConfigProject configProject,
            @NotNull P4ChangeListMapping changeListMapping) {
        super(project, VCS_NAME);

        this.changeListMapping = changeListMapping;
        myConfigurable = new P4ProjectConfigurable(project);
        //commitExecutor = new P4CommitExecutor();
        changelistListener = new P4ChangelistListener(project, this);
        changeProvider = new P4ChangeProvider(this);
        historyProvider = new P4HistoryProvider(project);
        diffProvider = new P4DiffProvider(project);
        problemListener = new ConfigListener();
        disconnectListener = new DisconnectListener();
        statusUpdateEnvironment = new P4StatusUpdateEnvironment(project);
        annotationProvider = new P4AnnotationProvider(this);
        committedChangesProvider = new P4CommittedChangesProvider();
        clients = new ClientManager(project, configProject);
        tempFileWatchDog = new TempFileWatchDog();
    }

    public static VcsKey getKey() {
        return VCS_KEY;
    }

    @Override
    public String getDisplayName() {
        return P4Bundle.message("p4ic.name");
    }

    @Override
    public Configurable getConfigurable() {
        return myConfigurable;
    }


    @Override
    protected void start() throws VcsException {
        if (!CompatFactoryLoader.isSupported()) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    Messages.showMessageDialog(myProject,
                            P4Bundle.message("ide.not.supported.message",
                                    ApplicationInfo.getInstance().getApiVersion(),
                                    P4Bundle.getString("p4ic.name"),
                                    P4Bundle.getString("p4ic.bug.url")),
                            P4Bundle.message("ide.not.supported.title"),
                            Messages.getErrorIcon());
                }
            });
            throw new VcsException(P4Bundle.message("ide.not.supported.title"));
        }
    }

    @Override
    protected void shutdown() throws VcsException {
        deactivate();
    }

    @Override
    protected void activate() {
        tempFileWatchDog.start();

        if (myVFSListener == null) {
            myVFSListener = new P4VFSListener(myProject, this, vfsSync);
        }

        VcsCompat.getInstance().setupPlugin(myProject);
        ChangeListManager.getInstance(myProject).addChangeListListener(changelistListener);

        if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
            final StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
            if (statusBar != null) {
                connectionWidget = new P4ConnectionWidget(this, myProject);
                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                    statusBar.addWidget(connectionWidget,
                            "after " + (SystemInfo.isMac ? "Encoding" : "InsertOverwrite"), myProject);
                    }
                }, ModalityState.NON_MODAL);
            }
        }

        myProject.getMessageBus().connect().subscribe(OnServerConfigurationProblem.TOPIC, problemListener);
        //ApplicationManager.getApplication().getMessageBus().connect().subscribe(
        //        P4ConfigListener.TOPIC, problemListener);
        myProject.getMessageBus().connect().subscribe(P4ConfigListener.TOPIC, problemListener);
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(OnServerDisconnectListener.TOPIC, disconnectListener);

        clients.initialize();

        /*

        // Look at adding file annotations (locally checked out or open for delete).
        if (myRepositoryForAnnotationsListener == null) {
            myRepositoryForAnnotationsListener = new P4RepositoryForAnnotationsListener(myProject);
        }

        // Activate any other services that are required.
        */
    }

    @Override
    public void deactivate() {
        clients.dispose();

        ChangeListManager.getInstance(myProject).removeChangeListListener(changelistListener);
        if (connectionWidget != null) {
            connectionWidget.deactivate();
            connectionWidget = null;
        }

        tempFileWatchDog.stop();
        tempFileWatchDog.cleanUpTempDir();
    }

    @Override
    @Nullable
    public VcsRevisionNumber parseRevisionNumber(String revisionNumberString) throws VcsException {
        try {
            return new VcsRevisionNumber.Long(Long.parseLong(revisionNumberString));
        } catch (NumberFormatException e) {
            throw new VcsException(e);
        }
    }

    /**
     * @return null if does not support revision parsing
     */
    @Override
    @Nullable
    public String getRevisionPattern() {
        return "\\d+";
    }

    /**
     * Returns the interface for performing check out / edit file operations.
     *
     * @return the interface implementation, or null if none is provided.
     */
    @Override
    @NotNull
    public synchronized EditFileProvider getEditFileProvider() {
        if (editProvider == null) {
            editProvider = new P4EditFileProvider(this, vfsSync);
        }
        return editProvider;
    }


    // Use the standard commit instead
    //@Override
    //public List<CommitExecutor> getCommitExecutors() {
    //    return Collections.singletonList(commitExecutor);
    //}

    /**
     * creates the object for performing checkin / commit / submit operations.
     */
    @Override
    @Nullable
    protected CheckinEnvironment createCheckinEnvironment() {
        return new P4CheckinEnvironment(this);
    }

    @Override
    public CheckinEnvironment getCheckinEnvironment() {
        // There is a weird situation where the parent wouldn't
        // be loading the check-in environment correctly.
        CheckinEnvironment ret = super.getCheckinEnvironment();
        if (ret == null) {
            super.setCheckinEnvironment(createCheckinEnvironment());
            ret = super.getCheckinEnvironment();
        }
        //if (ret == null || ! (ret instanceof P4CheckinEnvironment)) {
        if (ret == null) {
                // really yikes!
            throw new IllegalStateException("created wrong checkin environment: " + ret);
        }
        return ret;
    }

    /**
     * Returns the interface for performing revert / rollback operations.
     */
    @Override
    @Nullable
    protected RollbackEnvironment createRollbackEnvironment() {
        return new P4RollbackEnvironment(this, vfsSync);
    }

    @Override
    @Nullable
    public RollbackEnvironment getRollbackEnvironment() {
        RollbackEnvironment ret = super.getRollbackEnvironment();
        if (ret == null) {
            ret = createRollbackEnvironment();
            setRollbackEnvironment(ret);
        }
        return ret;
    }

    @Override
    public ChangeProvider getChangeProvider() {
        return changeProvider;
    }

    @Override
    @Nullable
    public VcsHistoryProvider getVcsHistoryProvider() {
        return historyProvider;
    }

    @Override
    @Nullable
    public VcsHistoryProvider getVcsBlockHistoryProvider() {
        return getVcsHistoryProvider();
    }

    @Override
    @Nullable
    public DiffProvider getDiffProvider() {
        return diffProvider;
    }


    /**
     * Returns true if the specified file path is located under a directory which is managed by this VCS.
     * This method is called only for directories which are mapped to this VCS in the project configuration.
     *
     * @param filePath the path to check.
     * @return true if the path is managed by this VCS, false otherwise.
     */
    @Override
    public boolean fileIsUnderVcs(FilePath filePath) {
        // TODO add checking against the P4IGNORE file.
        return filePath != null &&
                ! filePath.isDirectory() &&
                // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
                // use this instead: getIOFile().getAbsolutePath()
                ! filePath.getIOFile().getAbsolutePath().contains("...");
    }


    @Override
    public FileStatus[] getProvidedStatuses() {
        return PREFERRED_STATUS;
    }


    /**
     * Returns the interface for performing "check status" operations (operations which show the differences between
     * the local working copy state and the latest server state).
     *
     * @return the status interface, or null if the check status operation is not supported or required by the VCS.
     */
    @Override
    @Nullable
    public UpdateEnvironment getStatusEnvironment() {
        return statusUpdateEnvironment;
    }

    @Override
    @Nullable
    public AnnotationProvider getAnnotationProvider() {
        return annotationProvider;
    }


    @Override
    @Nullable
    public CommittedChangesProvider getCommittedChangesProvider() {
        return committedChangesProvider;
    }

    /**
     * Returns the interface for performing update/sync operations.
     */
    @Override
    @Nullable
    protected UpdateEnvironment createUpdateEnvironment() {
        //return IllegalStateProxy.create(UpdateEnvironment.class);
        return new P4SyncUpdateEnvironment(this);
    }


    // TODO implement these


    /**
     * Returns the interface for selecting file version numbers.
     *
     * @return the revision selector implementation, or null if none is provided.
     * @since 5.0.2
     */
    @Override
    @Nullable
    public RevisionSelector getRevisionSelector() {
        return null;
    }


    /**
     * Returns the interface for performing integrate operations (merging changes made in another branch of
     * the project into the current working copy).
     *
     * @return the update interface, or null if the integrate operations are not supported by the VCS.
     */
    @Override
    @Nullable
    public UpdateEnvironment getIntegrateEnvironment() {
        return null;
    }

    /**
     * Returns the implementation of the merge provider which is used to load the revisions to be merged
     * for a particular file.
     *
     * @return the merge provider implementation, or null if the VCS doesn't support merge operations.
     */
    @Override
    @Nullable
    public MergeProvider getMergeProvider() {
        return null;
    }


    // ---------------------------------------------------------------------------
    // Specialized P4Vcs methods

    @NotNull
    public File getTempDir() {
        return tempFileWatchDog.getTempDir();
    }

    @NotNull
    public P4ChangeListMapping getChangeListMapping() {
        return changeListMapping;
    }


    @NotNull
    public List<Client> getClients() {
        return clients.getClients();
    }


    public void reloadConfigs() {
        clients.loadConfig();
    }


    /**
     *
     *
     * @param filePaths paths mapped
     * @return a mapping of all the input files to the corresponding Perforce server configuration.  If a file
     *      isn't mapped to any server configs, then it will be assigned to the "null" entry.
     * @throws P4InvalidConfigException
     */
    public Map<Client, List<FilePath>> mapFilePathToClient(Collection<FilePath> filePaths)
            throws P4InvalidConfigException {
        List<FilePath> paths = new ArrayList<FilePath>(filePaths);
        Map<Client, List<FilePath>> ret = new HashMap<Client, List<FilePath>>();
        for (Client config: getClients()) {
            List<FilePath> configFiles = new ArrayList<FilePath>();
            for (FilePath configRoot: config.getFilePathRoots()) {
                Iterator<FilePath> iter = paths.iterator();
                while (iter.hasNext()) {
                    FilePath next = iter.next();
                    if (next != null && next.isUnder(configRoot, false)) {
                        configFiles.add(next);
                        iter.remove();
                    }
                }
                if (!configFiles.isEmpty()) {
                    ret.put(config, configFiles);
                }
            }
        }
        return ret;
    }


    /**
     * @param virtualFiles paths mapped
     * @return a mapping of all the input files to the corresponding Perforce server configuration.  If a file
     * isn't mapped to any server configs, then it will be assigned to the "null" entry.
     * @throws P4InvalidConfigException the underlying calls that generate this exception (Client implementations)
     *      should properly handle calls to
     *      {@link net.groboclown.idea.p4ic.config.P4ConfigListener#configurationProblem(com.intellij.openapi.project.Project, net.groboclown.idea.p4ic.config.P4Config, net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException)}
     */
    public Map<Client, List<VirtualFile>> mapVirtualFilesToClient(Collection<VirtualFile> virtualFiles)
            throws P4InvalidConfigException {
        List<VirtualFile> paths = new ArrayList<VirtualFile>(virtualFiles);
        Map<Client, List<VirtualFile>> ret = new HashMap<Client, List<VirtualFile>>();
        for (Client config : getClients()) {
            List<VirtualFile> configFiles = new ArrayList<VirtualFile>();
            for (FilePath configRoot: config.getFilePathRoots()) {
                Iterator<VirtualFile> iter = paths.iterator();
                while (iter.hasNext()) {
                    VirtualFile next = iter.next();
                    if (next != null) {
                        FilePath fp = VcsUtil.getFilePath(next);
                        if (fp.isUnder(configRoot, false)) {
                            configFiles.add(next);
                            iter.remove();
                        }
                    }
                }
                if (!configFiles.isEmpty()) {
                    ret.put(config, configFiles);
                }
            }
        }
        return ret;
    }


    public boolean isWorkingOffline(@NotNull String serverConfigId) {
        for (Client client: getClients()) {
            if (client.getConfig().getServiceName().equals(serverConfigId)) {
                return isWorkingOffline(client.getConfig());
            }
        }
        // unknown config; report it as offline
        return true;
    }


    public boolean isWorkingOffline(@NotNull ServerConfig config) {
        try {
            return ServerStoreService.getInstance().
                    getServerStatus(myProject, config).isWorkingOffline();
        } catch (P4InvalidConfigException e) {
            LOG.debug(e);
            return true;
        }
    }

    @Nullable
    public Client getClientFor(@NotNull FilePath fp) {
        for (Client config: getClients()) {
            try {
                for (FilePath root : config.getFilePathRoots()) {
                    if (fp.isUnder(root, false)) {
                        return config;
                    }
                }
            } catch (P4InvalidConfigException e) {
                // ignore
                LOG.debug(e);
            }
        }
        return null;
    }

    @Nullable
    public Client getClientFor(VirtualFile vf) {
        return getClientFor(VcsUtil.getFilePath(vf));
    }

    @NotNull
    public Map<Client, Collection<FilePath>> sortClientByFilePaths(List<FilePath> fpList) {
        Map<Client, Collection<FilePath>> ret = new HashMap<Client, Collection<FilePath>>();
        for (FilePath fp: fpList) {
            Client sc = getClientFor(fp);
            if (sc != null) {
                Collection<FilePath> files = ret.get(sc);
                if (files == null) {
                    files = new ArrayList<FilePath>();
                    ret.put(sc, files);
                }
                files.add(fp);
            }
        }
        return ret;
    }

    @NotNull
    public Map<Client, Collection<VirtualFile>> sortClientByVirtualFiles(List<VirtualFile> vfList) {
        Map<Client, Collection<VirtualFile>> ret = new HashMap<Client, Collection<VirtualFile>>();
        for (VirtualFile vf : vfList) {
            Client sc = getClientFor(vf);
            if (sc != null) {
                Collection<VirtualFile> files = ret.get(sc);
                if (files == null) {
                    files = new ArrayList<VirtualFile>();
                    ret.put(sc, files);
                }
                files.add(vf);
            }
        }
        return ret;
    }


    class ConfigListener implements P4ConfigListener, OnServerConfigurationProblem {

        @Override
        public void configChanges(@NotNull Project project, @NotNull P4Config original, @NotNull P4Config config) {
            autoOffline = config.isAutoOffline();
        }

        @Override
        public void configurationProblem(@NotNull Project project, @NotNull P4Config config, @NotNull P4InvalidConfigException ex) {
            onInvalidConfiguration(VcsSettableFuture.<Boolean>create(), null, ex.getMessage());
        }

        @Override
        public void onInvalidConfiguration(@NotNull final VcsFutureSetter<Boolean> future,
                @Nullable ServerConfig config, @Nullable final String message) {
            if (future.isDone()) {
                // already handled
                return;
            }

            // show the config dialog
            future.runInEdt(new Runnable() {
                @Override
                public void run() {
                    int result = Messages.showYesNoDialog(myProject,
                            P4Bundle.message("configuration.connection-problem-ask", message),
                            P4Bundle.message("configuration.check-connection"),
                            Messages.getErrorIcon());
                    boolean changed = false;
                    if (result == Messages.YES) {
                        // Signal to the API to try again only if
                        // the user selected "okay".
                        changed = UICompat.getInstance().editVcsConfiguration(myProject, getConfigurable());
                    }
                    if (!changed) {
                        // Work offline
                        Messages.showMessageDialog(myProject,
                                P4Bundle.message("dialog.offline.went-offline.message"),
                                P4Bundle.message("dialog.offline.went-offline.title"),
                                Messages.getInformationIcon());
                    }
                    future.set(changed);
                }
            });
        }
    }


    class DisconnectListener implements OnServerDisconnectListener {
        @Override
        public void onDisconnect(@NotNull ServerConfig config, @NotNull final VcsFutureSetter<OnDisconnectAction> retry) {
            LOG.warn("Disconnected from Perforce server");
            if (retry.isDone()) {
                // already handled
                return;
            }

            // We may need to switch to automatically work offline due
            // to a user setting.
            if (autoOffline) {
                LOG.info("User running in auto-offline mode.  Will silently work disconnected.");
                retry.set(OnDisconnectAction.WORK_OFFLINE);
                return;
            }

            // Ask the user if they want to disconnect.
            retry.runInEdt(new Runnable() {
                @Override
                public void run() {
                    LOG.info("Asking user to reconnect");
                    int choice = Messages.showDialog(myProject,
                            P4Bundle.message("dialog.offline.message"),
                            P4Bundle.message("dialog.offline.title"),
                            new String[]{
                                    P4Bundle.message("dialog.offline.reconnect"),
                                    P4Bundle.message("dialog.offline.offline-mode")
                            },
                            1,
                            Messages.getErrorIcon());
                    if (choice == 0) {
                        retry.set(OnDisconnectAction.RETRY);
                    } else {
                        retry.set(OnDisconnectAction.WORK_OFFLINE);
                        Messages.showMessageDialog(myProject,
                                P4Bundle.message("dialog.offline.went-offline.message"),
                                P4Bundle.message("dialog.offline.went-offline.title"),
                                Messages.getInformationIcon());
                    }
                }
            });
        }

    }

}
