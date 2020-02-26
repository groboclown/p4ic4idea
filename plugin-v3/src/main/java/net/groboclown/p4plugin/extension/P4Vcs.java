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
package net.groboclown.p4plugin.extension;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.CommittedChangesProvider;
import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusFactory;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.VcsRootSettings;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.diff.RevisionSelector;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.merge.MergeProvider;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThreeState;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.p4.server.api.P4VcsKey;
import net.groboclown.p4.server.api.util.ProjectUtil;
import net.groboclown.p4.server.api.values.P4CommittedChangelist;
import net.groboclown.p4.server.impl.config.P4VcsRootSettingsImpl;
import net.groboclown.p4.server.impl.tasks.TempFileWatchDog;
import net.groboclown.p4.server.impl.util.ChangeListUtil;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.ColorUtil;
import net.groboclown.p4plugin.ui.config.P4ProjectConfigurable;
import net.groboclown.p4plugin.ui.vcsroot.P4VcsRootConfigurable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;

public class P4Vcs extends AbstractVcs<P4CommittedChangelist> {
    private static final Logger LOG = Logger.getInstance(P4Vcs.class);

    public static final FileStatus ADDED_OFFLINE =
            FileStatusFactory.getInstance().createFileStatus(
                    "ADDED_OFFLINE",
                    P4Bundle.message("filestatus.added_offline"),
                    FileStatus.ADDED.getColor()
            );
    public static final FileStatus MODIFIED_OFFLINE =
            FileStatusFactory.getInstance().createFileStatus(
                    "MODIFIED_OFFLINE",
                    P4Bundle.message("filestatus.edited_offline"),
                    FileStatus.MODIFIED.getColor()
            );
    public static final FileStatus DELETED_OFFLINE =
            FileStatusFactory.getInstance().createFileStatus(
                    "DELETED_OFFLINE",
                    P4Bundle.message("filestatus.deleted_offline"),
                    FileStatus.DELETED_FROM_FS.getColor()
            );
    public static final FileStatus REVERTED_OFFLINE =
            FileStatusFactory.getInstance().createFileStatus(
                    "REVERTED_OFFLINE",
                    P4Bundle.message("filestatus.reverted_offline"),
                    FileStatus.NOT_CHANGED_IMMEDIATE.getColor()
            );
    public static final FileStatus SHELVED_ADDED =
            FileStatusFactory.getInstance().createFileStatus(
                    "SHELVED_ADDED",
                    P4Bundle.message("filestatus.shelved_added"),
                    toShelvedColor(FileStatus.ADDED.getColor())
            );
    public static final FileStatus SHELVED_DELETED =
            FileStatusFactory.getInstance().createFileStatus(
                    "SHELVED_DELETED",
                    P4Bundle.message("filestatus.shelved_deleted"),
                    toShelvedColor(FileStatus.DELETED.getColor())
            );
    public static final FileStatus SHELVED_MODIFIED =
            FileStatusFactory.getInstance().createFileStatus(
                    "SHELVED_MODIFIED",
                    P4Bundle.message("filestatus.shelved_modified"),
                    toShelvedColor(FileStatus.MODIFIED.getColor())
            );
    public static final FileStatus SHELVED_UNKNOWN =
            FileStatusFactory.getInstance().createFileStatus(
                    "SHELVED_UNKNOWN",
                    P4Bundle.message("filestatus.shelved_unknown"),
                    toShelvedColor(FileStatus.UNKNOWN.getColor())
            );


    private static final FileStatus[] PREFERRED_STATUS = new FileStatus[] {
            FileStatus.ADDED,
            FileStatus.DELETED,
            FileStatus.IGNORED,
            FileStatus.MERGE,
            FileStatus.MODIFIED,
            FileStatus.NOT_CHANGED,

            ADDED_OFFLINE,
            MODIFIED_OFFLINE,
            DELETED_OFFLINE,
            REVERTED_OFFLINE,

            SHELVED_ADDED,
            SHELVED_DELETED,
            SHELVED_MODIFIED,
            SHELVED_UNKNOWN
    };

    @NonNls
    public static final String VCS_NAME = P4VcsKey.VCS_NAME;

    private static final VcsKey VCS_KEY = createKey(VCS_NAME);

    @NotNull
    private final P4HistoryProvider historyProvider;

    private final P4StatusUpdateEnvironment statusUpdateEnvironment;

    private final P4CommittedChangesProvider committedChangesProvider;

    private final TempFileWatchDog tempFileWatchDog;

    private MessageBusConnection projectMessageBusConnection;

    private MessageBusConnection appMessageBusConnection;

    private P4VFSListener myVFSListener;

    private P4EditFileProvider editProvider;

    private final P4ProjectConfigurable myConfigurable;

    private final P4ChangelistListener changelistListener;

    private final P4ChangeProvider changeProvider;

    private final DiffProvider diffProvider;

    private final P4AnnotationProvider annotationProvider;

    private final P4RevisionSelector revisionSelector;


    @NotNull
    public static P4Vcs getInstance(@Nullable Project project) {
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


    public P4Vcs(@NotNull Project project) {
        super(project, VCS_NAME);

        this.changelistListener = new P4ChangelistListener(project);
        this.changeProvider = new P4ChangeProvider(this, project);
        this.historyProvider = new P4HistoryProvider(project);
        this.diffProvider = new P4DiffProvider(project);
        this.statusUpdateEnvironment = new P4StatusUpdateEnvironment(project);
        this.annotationProvider = new P4AnnotationProvider(this);
        this.committedChangesProvider = new P4CommittedChangesProvider(this);
        this.myConfigurable = new P4ProjectConfigurable(project);

        this.revisionSelector = new P4RevisionSelector(this);
        this.tempFileWatchDog = new TempFileWatchDog();
    }

    public static VcsKey getKey() {
        return VCS_KEY;
    }

    @Override
    public String getDisplayName() {
        return P4Bundle.message("p4ic.name");
    }


    /**
     * Returns the configurable to be shown in the VCS directory mapping dialog which should be displayed
     * for configuring VCS-specific settings for the specified root, or null if no such configuration is required.
     * The VCS-specific settings are stored in {@link VcsDirectoryMapping#getRootSettings()}.
     *
     * @param mapping the mapping being configured
     * @return the configurable instance, or null if no configuration is required.
     */
    @Override
    public UnnamedConfigurable getRootConfigurable(VcsDirectoryMapping mapping) {
        return new P4VcsRootConfigurable(getProject(), mapping);
    }

    @Nullable
    public VcsRootSettings createEmptyVcsRootSettings() {
        return new P4VcsRootSettingsImpl(
                myProject,
                ProjectUtil.guessProjectBaseDir(myProject)
        );
    }

    // This is only needed if the user's defined VCS roots don't
    // necessarily match up with the actual VCS.  For this plugin, at the
    // moment, we're defining the per-client setup at the VCS root level,
    // so we don't need this conversion (we have an identity mapping).
    // Eventually, if the relative config file is implemented again, we can use this.
    //@Nullable
    //public RootsConvertor getCustomConvertor() {
    //    return null;
    //}


    /**
     *
     * @return plugin-wide configuration UI.
     */
    @Override
    public Configurable getConfigurable() {
        return myConfigurable;
    }

    @Override
    protected void start() throws VcsException {
    }

    @Override
    protected void shutdown() {
        deactivate();
    }

    @Override
    protected void activate() {
        tempFileWatchDog.start();

        if (myVFSListener == null) {
            myVFSListener = new P4VFSListener(getProject(), this);
        }

        // VcsCompat.getInstance().setupPlugin(myProject);

        ChangeListManager.getInstance(myProject).addChangeListListener(changelistListener);

        projectMessageBusConnection = myProject.getMessageBus().connect();
        appMessageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();

        // If additional actions need to happen at plugin startup time, add them here to execute in
        // a background thread.
    }

    @Override
    public void deactivate() {
        myConfigurable.disposeUIResources();

        tempFileWatchDog.stop();
        tempFileWatchDog.cleanUpTempDir();

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

        // This can cause an error if the project isn't setup.  So put it at the end.
        if (!myProject.isDisposed()) {
            ChangeListManager.getInstance(myProject).removeChangeListListener(changelistListener);
        }

        super.deactivate();
    }

    @Override
    @Nullable
    public VcsRevisionNumber parseRevisionNumber(String revisionNumberString) throws VcsException {
        try {
            return new VcsRevisionNumber.Int(Integer.parseInt(revisionNumberString));
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
        return "#?\\d+";
    }

    /**
     * Returns the interface for performing check out / edit file operations.
     *
     * @return the interface implementation, or null if none is provided.
     */
    @Override
    @NotNull
    public synchronized EditFileProvider getEditFileProvider() {
        // TODO remove statement when debugging is done
        LOG.info("Getting EditFileProvider");
        if (editProvider == null) {
            editProvider = new P4EditFileProvider(this);
        }
        return editProvider;
    }


    /*
    Executors are an attempt to regain control over whether the "submit" button is enabled or not (bug #52).
    Unfortunately, the checkin executors are ignored for the purpose of UI and button state.

    TODO maybe just use P4CheckinEnvironment in the getCommitExecutors(), rather than the createCheckinEnvironment?

    private P4CheckinEnvironment checkinEnvironment;

    @Override
    public List<CommitExecutor> getCommitExecutors() {
        if (checkinEnvironment == null) {
            checkinEnvironment = (P4CheckinEnvironment) createCheckinEnvironment();
        }
        return Collections.singletonList(checkinEnvironment);
    }
    */


    /**
     * creates the object for performing checkin / commit / submit operations.
     */
    @Override
    @Nullable
    protected CheckinEnvironment createCheckinEnvironment() {
        return new P4CheckinEnvironment(this);
    }

    @NotNull
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
        return new P4RollbackEnvironment(this);
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
        return new P4SyncUpdateEnvironment(myProject);
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


    /**
     * Invoked when a changelist is deleted explicitly by user or implicitly (e.g. after default changelist switch
     * when the previous one was empty).
     * @param list change list that's about to be removed
     * @param explicitly whether it's a result of explicit Delete action, or just after switching the active changelist.
     * @return UNSURE if the VCS has nothing to say about this changelist.
     * YES or NO if the changelist has to be removed or not, and no further confirmations are needed about this changelist
     * (in particular, the VCS can show a confirmation to the user by itself)
     */
    @Override
    // @CalledInAwt
    @NotNull
    public ThreeState mayRemoveChangeList(@NotNull LocalChangeList list, boolean explicitly) {
        if (!explicitly || ChangeListUtil.isDefaultChangelist(list)) {
            return ThreeState.NO;
        }
        return ThreeState.YES;
    }


    public boolean allowsNestedRoots() {
        return true;
    }

    public boolean fileListenerIsSynchronous() {
        return false;
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


    /**
     * Can be temporarily forbidden, for instance, when authorization credentials are wrong - to
     * don't repeat wrong credentials passing (in some cases it can produce user's account blocking)
     *
     * This is handled better by P4V in connection management.  Invalid credentials still allow offline action
     * caching.
     */
    @Override
    public boolean isVcsBackgroundOperationsAllowed(final VirtualFile root) {
        return true;
    }



    /**
     * Returns true if the specified file path represents a file which exists in the VCS repository (is neither
     * unversioned nor scheduled for addition).
     * This method is called only for directories which are mapped to this VCS in the project configuration.
     *
     * @param path the path to check.
     * @return true if the corresponding file exists in the repository, false otherwise.
     */
    @Override
    public boolean fileExistsInVcs(FilePath path) {
        // TODO if this ends up being called as we expect, then delete this overridden method.

        final VirtualFile virtualFile = path.getVirtualFile();
        if (virtualFile != null) {
            final FileStatus fileStatus = FileStatusManager.getInstance(myProject).getStatus(virtualFile);
            LOG.info("Checking if file exists in VCS; if so, then it will use the `VcsHandleType` to edit the file.  File: [" + path + "]; status: " + fileStatus);
            return fileStatus != FileStatus.UNKNOWN && fileStatus != FileStatus.ADDED;
        }
        return true;
    }

    // ---------------------------------------------------------------------------
    // Specialized P4Vcs methods


    @NotNull
    public File getTempDir() {
        return tempFileWatchDog.getTempDir();
    }

    @Nullable
    private static Color toShelvedColor(@Nullable Color color) {
        return ColorUtil.lightenColor(color);
    }

}
