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
import com.intellij.openapi.util.Disposer;
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
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.background.TempFileWatchDog;
import net.groboclown.idea.p4ic.compat.CompatFactoryLoader;
import net.groboclown.idea.p4ic.compat.VcsCompat;
import net.groboclown.idea.p4ic.config.P4ConfigProject;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.ui.P4MultipleConnectionWidget;
import net.groboclown.idea.p4ic.ui.config.P4ProjectConfigurable;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeProvider;
import net.groboclown.idea.p4ic.v2.changes.P4ChangelistListener;
import net.groboclown.idea.p4ic.v2.changes.P4CommittedChangeList;
import net.groboclown.idea.p4ic.v2.extension.P4StatusUpdateEnvironment;
import net.groboclown.idea.p4ic.v2.extension.P4SyncUpdateEnvironment;
import net.groboclown.idea.p4ic.v2.file.FileExtensions;
import net.groboclown.idea.p4ic.v2.file.P4CheckinEnvironment;
import net.groboclown.idea.p4ic.v2.file.P4EditFileProvider;
import net.groboclown.idea.p4ic.v2.file.P4VFSListener;
import net.groboclown.idea.p4ic.v2.history.P4AnnotationProvider;
import net.groboclown.idea.p4ic.v2.history.P4DiffProvider;
import net.groboclown.idea.p4ic.v2.history.P4HistoryProvider;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.P4ServerManager;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public class P4Vcs extends AbstractVcs<P4CommittedChangeList> {
    private static final Logger LOG = Logger.getInstance(P4Vcs.class);


    public static final FileStatus MODIFIED_OFFLINE =
            FileStatusFactory.getInstance().createFileStatus(
                    "MODIFIED_OFFLINE",
                    P4Bundle.message("filestatus.edited_offline"),
                    FileStatus.COLOR_MODIFIED
            );
    public static final FileStatus DELETED_OFFLINE =
            FileStatusFactory.getInstance().createFileStatus(
                    "DELETED_OFFLINE",
                    P4Bundle.message("filestatus.deleted_offline"),
                    FileStatus.COLOR_MISSING
            );
    public static final FileStatus REVERTED_OFFLINE =
            FileStatusFactory.getInstance().createFileStatus(
                    "REVERTED_OFFLINE",
                    P4Bundle.message("filestatus.reverted_offline"),
                    FileStatus.COLOR_NOT_CHANGED_IMMEDIATE
            );


    private static final FileStatus[] PREFERRED_STATUS = new FileStatus[] {
            FileStatus.ADDED,
            FileStatus.DELETED,
            FileStatus.IGNORED,
            FileStatus.MERGE,
            FileStatus.MODIFIED,
            FileStatus.NOT_CHANGED,

            MODIFIED_OFFLINE,
            DELETED_OFFLINE,
            REVERTED_OFFLINE
    };

    @NonNls
    public static final String VCS_NAME = "p4ic";

    private static final VcsKey VCS_KEY = createKey(VCS_NAME);

    @NotNull
    private final Configurable myConfigurable;

    @NotNull
    private final P4HistoryProvider historyProvider;

    private final P4StatusUpdateEnvironment statusUpdateEnvironment;

    private final P4CommittedChangesProvider committedChangesProvider;

    private final TempFileWatchDog tempFileWatchDog;

    private MessageBusConnection projectMessageBusConnection;

    private MessageBusConnection appMessageBusConnection;

    //private P4ConnectionWidget connectionWidget;
    private P4MultipleConnectionWidget connectionWidget;

    private P4VFSListener myVFSListener;

    private P4EditFileProvider editProvider;

    // Not used
    //private final CommitExecutor commitExecutor;

    private final P4ChangelistListener changelistListener;

    private final P4ChangeProvider changeProvider;

    private final UserProjectPreferences userPreferences;

    private final DiffProvider diffProvider;

    private final P4AnnotationProvider annotationProvider;

    private final FileExtensions fileExtensions;

    private final P4ServerManager serverManager;

    private final P4RevisionSelector revisionSelector;

    // Capture the VCS roots list.  This is necessary, because the standard call
    // requires getting an IDE read lock, and that leads to all kinds of synchronization issues.
    private final List<VirtualFile> vcsRootsCache;


    private boolean autoOffline = false;


    @NotNull
    public static P4Vcs getInstance(Project project) {
        if (project == null || project.isDisposed()) {
            throw new NullPointerException(P4Bundle.message("error.no-active-project"));
        }
        P4Vcs ret = (P4Vcs) ProjectLevelVcsManager.getInstance(project).findVcsByName(VCS_NAME);
        if (ret == null) {
            throw new NullPointerException(P4Bundle.message("error.no-active-project"));
        }
        return ret;
    }


    public static boolean isProjectValid(@NotNull Project project) {
        if (project.isDisposed()) {
            return false;
        }
        ProjectLevelVcsManager mgr = ProjectLevelVcsManager.getInstance(project);
        return mgr.checkVcsIsActive(P4Vcs.VCS_NAME);
    }


    public P4Vcs(
            @NotNull Project project,
            @NotNull P4ConfigProject configProject,
            @NotNull UserProjectPreferences preferences) {
        super(project, VCS_NAME);

        this.userPreferences = preferences;
        this.myConfigurable = new P4ProjectConfigurable(project);
        this.changelistListener = new P4ChangelistListener(project, this);
        this.changeProvider = new P4ChangeProvider(this);
        this.historyProvider = new P4HistoryProvider(project, this);
        this.diffProvider = new P4DiffProvider(project);
        this.statusUpdateEnvironment = new P4StatusUpdateEnvironment(project);
        this.annotationProvider = new P4AnnotationProvider(this);
        this.committedChangesProvider = new P4CommittedChangesProvider();
        this.serverManager = new P4ServerManager(project);
        this.revisionSelector = new P4RevisionSelector(this);
        this.tempFileWatchDog = new TempFileWatchDog();
        this.fileExtensions = new FileExtensions(this, AlertManager.getInstance());
        this.vcsRootsCache = new ArrayList<VirtualFile>();
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

        synchronized (vcsRootsCache) {
            vcsRootsCache.clear();
            vcsRootsCache.addAll(Arrays.asList(
                    ProjectLevelVcsManager.getInstance(myProject).getRootsUnderVcs(this)));
        }

        if (myVFSListener == null) {
            myVFSListener = fileExtensions.createVcsVFSListener();
        }

        VcsCompat.getInstance().setupPlugin(myProject);
        ChangeListManager.getInstance(myProject).addChangeListListener(changelistListener);

        if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
            final StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
            if (statusBar != null) {
                connectionWidget = new P4MultipleConnectionWidget(this, myProject);
                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                    statusBar.addWidget(connectionWidget,
                            "after " + (SystemInfo.isMac ? "Encoding" : "InsertOverwrite"), myProject);
                    }
                }, ModalityState.NON_MODAL);
            }
        }

        projectMessageBusConnection = myProject.getMessageBus().connect();
        appMessageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();

        // FIXME enable if it makes sense to handle these here.
        //Events.appBaseConfigUpdated(projectMessageBusConnection, problemListener);
        //Events.appConfigInvalid(projectMessageBusConnection, problemListener);
        //Events.appServerConnectionState(appMessageBusConnection, disconnectListener);

        // Keep our cache up-to-date
        projectMessageBusConnection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, new VcsListener() {
            @Override
            public void directoryMappingChanged() {
                final VirtualFile[] cache = ProjectLevelVcsManager.getInstance(myProject).getRootsUnderVcs(P4Vcs.this);
                synchronized (vcsRootsCache) {
                    vcsRootsCache.clear();
                    vcsRootsCache.addAll(Arrays.asList(cache));
                }
            }
        });

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
        myConfigurable.disposeUIResources();

        ChangeListManager.getInstance(myProject).removeChangeListListener(changelistListener);

        tempFileWatchDog.stop();
        tempFileWatchDog.cleanUpTempDir();

        if (connectionWidget != null && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
            final StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
            if (statusBar != null) {
                connectionWidget = new P4MultipleConnectionWidget(this, myProject);
                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        statusBar.removeWidget(connectionWidget.ID());
                        connectionWidget.deactivate();
                    }
                }, ModalityState.NON_MODAL);
            }
        }

        if (myVFSListener != null) {
            Disposer.dispose(myVFSListener);
            myVFSListener = null;
        }

        if (projectMessageBusConnection != null) {
            projectMessageBusConnection.disconnect();
            projectMessageBusConnection = null;
        }
        if (appMessageBusConnection != null) {
            appMessageBusConnection.disconnect();
            appMessageBusConnection = null;
        }

        super.deactivate();
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
            editProvider = fileExtensions.createEditFileProvider();
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
        // It can return a proxy to the real environment, which is fine.
        if (ret == null) {
            // really yikes!
            throw new IllegalStateException(P4Bundle.message("error.checkin-env.null"));
        }
        return ret;
    }

    /**
     * Returns the interface for performing revert / rollback operations.
     */
    @Override
    @Nullable
    protected RollbackEnvironment createRollbackEnvironment() {
        return fileExtensions.createRollbackEnvironment();
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


    /**
     * Returns the interface for selecting file version numbers.
     *
     * @return the revision selector implementation, or null if none is provided.
     * @since 5.0.2
     */
    @Override
    @Nullable
    public RevisionSelector getRevisionSelector() {
        return revisionSelector;
    }


    // TODO implement these


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
    public UserProjectPreferences getUserPreferences() {
        return userPreferences;
    }


    @NotNull
    public File getTempDir() {
        return tempFileWatchDog.getTempDir();
    }


    /**
     * A thread-safe way to get the VCS roots for this VCS.  The standard call
     * will perform an IDE-wide read lock, which can lead to massive thread
     * deadlocking.
     *
     * @return the vcs roots.
     */
    @NotNull
    public List<VirtualFile> getVcsRoots() {
        synchronized (vcsRootsCache) {
            return new ArrayList<VirtualFile>(vcsRootsCache);
        }
    }


    /**
     *
     * @param files files
     * @return the matched mapping of files to the servers.  There might be a "null" server entry, which
     *      contains a list of file paths that didn't map to a client.
     */
    @NotNull
    public Map<P4Server, List<FilePath>> mapFilePathsToP4Server(Collection<FilePath> files)
            throws InterruptedException {
        return serverManager.mapFilePathsToP4Server(files);
    }


    /**
     * @param files files
     * @return the matched mapping of files to the servers.  There might be a "null" server entry, which
     * contains a list of file paths that didn't map to a client.
     */
    @NotNull
    public Map<P4Server, List<VirtualFile>> mapVirtualFilesToP4Server(Collection<VirtualFile> files)
            throws InterruptedException {
        // TODO make more efficient.  This currently just remaps.
        Map<FilePath, VirtualFile> input = new HashMap<FilePath, VirtualFile>(files.size());
        for (VirtualFile file : files) {
            input.put(FilePathUtil.getFilePath(file), file);
        }
        final Map<P4Server, List<FilePath>> output = serverManager.mapFilePathsToP4Server(input.keySet());
        final Map<P4Server, List<VirtualFile>> ret = new HashMap<P4Server, List<VirtualFile>>();
        for (Entry<P4Server, List<FilePath>> entry : output.entrySet()) {
            List<VirtualFile> vfList = new ArrayList<VirtualFile>(entry.getValue().size());
            for (FilePath filePath : entry.getValue()) {
                vfList.add(filePath.getVirtualFile());
            }
            ret.put(entry.getKey(), vfList);
        }
        return ret;
    }


    public List<P4Server> getP4Servers() {
        return serverManager.getServers();
    }


    @Nullable
    public P4Server getP4ServerFor(@NotNull FilePath fp) throws InterruptedException {
        return serverManager.getForFilePath(fp);
    }

    @Nullable
    public P4Server getP4ServerFor(@NotNull VirtualFile vf) throws InterruptedException {
        return serverManager.getForVirtualFile(vf);
    }


    /**
     * Checks if the input path is under the config root, and how deep
     * the config root is.
     * <p>
     * This does not perform link expansion (get absolute path).  We
     * assume that if you have a file under a path in a link, you want
     * it to be at that location, and not at its real location.
     * </p>
     *
     * @param input the file to match against a P4 client directory
     * @param configRoot root directory of a client
     * @return &lt; 0 if the file is not under the client directory, otherwise the
     *      directory depth (from root) of the config root.
     */
    private int getFilePathMatchDepth(@NotNull final FilePath input, @NotNull final FilePath configRoot) {
        final List<FilePath> inputParts = getPathParts(input);
        final List<FilePath> rootParts = getPathParts(configRoot);

        if (inputParts.size() < rootParts.size()) {
            // input is at a higher ancestor level than the root parts,
            // so there's no way it could be in this root.
            return -1;
        }

        // See if input is under the root.
        // We should be able to just  call input.isUnder(configRoot), but
        // that seems to be buggy - it reported that "/a/b/c" was under "/a/b/d".

        final FilePath sameRootDepth = inputParts.get(rootParts.size() - 1);
        if (sameRootDepth.equals(configRoot)) {
            // it's a match.  The input file ancestor path that is
            // at the same directory depth as the config root is the same
            // path.
            return rootParts.size();
        }

        // Not under the same path, so it's not a match.
        return -1;
    }

    @NotNull
    private List<FilePath> getPathParts(@NotNull final FilePath child) {
        List<FilePath> ret = new ArrayList<FilePath>();
        FilePath next = child;
        while (next != null) {
            ret.add(next);
            next = next.getParentPath();
        }
        Collections.reverse(ret);
        return ret;
    }

    public boolean isAutoOffline() {
        return autoOffline;
    }
}
