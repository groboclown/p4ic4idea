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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.CachingCommittedChangesProvider;
import com.intellij.openapi.vcs.ChangeListColumn;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.committed.DecoratorManager;
import com.intellij.openapi.vcs.changes.committed.RepositoryLocationGroup;
import com.intellij.openapi.vcs.changes.committed.VcsCommittedListsZipper;
import com.intellij.openapi.vcs.changes.committed.VcsCommittedListsZipperAdapter;
import com.intellij.openapi.vcs.changes.committed.VcsCommittedViewAuxiliary;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vcs.versionBrowser.ChangesBrowserSettingsEditor;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vcs.versionBrowser.StandardVersionFilterComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.AsynchConsumer;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.commands.sync.SyncListFilesDetailsQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.repository.P4RepositoryLocation;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4CommittedChangelist;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.commands.DoneQueryAnswer;
import net.groboclown.p4.server.impl.repository.RepositoryLocationFactory;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4ChangelistSummaryImpl;
import net.groboclown.p4.server.impl.values.P4CommittedChangelistImpl;
import net.groboclown.p4.server.impl.values.P4RemoteFileImpl;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.actions.ChangelistDescriptionAction;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.extension.P4CommittedChangesProvider.P4ChangeBrowserSettings;
import net.groboclown.p4plugin.revision.P4RemoteFileContentRevision;
import net.groboclown.p4plugin.util.HistoryContentLoaderImpl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

// CachingCommittedChangesProvider is required to use the Repository built-in tab, and that built-in tab
// always appears when the VCS is marked as centralized (as opposed to distributed).
public class P4CommittedChangesProvider implements
        CachingCommittedChangesProvider<P4CommittedChangelist, P4ChangeBrowserSettings> {
    private static final Logger LOG = Logger.getInstance(P4CommittedChangesProvider.class);

    private final Project project;
    private final HistoryContentLoader loader;

    P4CommittedChangesProvider(@NotNull final P4Vcs vcs) {
        this.project = vcs.getProject();
        this.loader = new HistoryContentLoaderImpl(vcs.getProject());
    }


    @NotNull
    @Override
    public P4ChangeBrowserSettings createDefaultSettings() {
        LOG.debug("Creating default settings");
        return new P4ChangeBrowserSettings();
    }

    @Override
    public ChangesBrowserSettingsEditor<P4ChangeBrowserSettings> createFilterUI(boolean showDateFilter) {
        LOG.debug("Creating filter UI");
        return new StandardVersionFilterComponent<P4ChangeBrowserSettings>() {
            @Override
            public JComponent getComponent() {
                return (JComponent) getStandardPanel();
            }
        };
    }

    @Nullable
    @Override
    public RepositoryLocation getLocationFor(@Nullable FilePath root) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding location for " + root);
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || root == null) {
            LOG.debug("No registry, or root is null");
            return null;
        }
        ClientConfigRoot client = registry.getClientFor(root);
        if (client == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No client for root " + root);
            }
            return null;
        }
        ListFilesDetailsResult details;
        if (ApplicationManager.getApplication().isDispatchThread()) {
            // Use the cache
            LOG.debug("Loading file location from the cache due to execution within the dispatch thread");
            details = P4ServerComponent
                    .syncCachedQuery(project, client.getClientConfig(),
                            new SyncListFilesDetailsQuery(root));
        } else {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Loading file location from the server for " + root);
                }
                details = P4ServerComponent
                        .query(project, client.getClientConfig(),
                                new ListFilesDetailsQuery(
                                        Collections.singletonList(root),  ListFilesDetailsQuery.RevState.HAVE, 1))
                        .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(
                        "Encountered error looking for files under " + root, e)));
                return null;
            } catch (P4CommandRunner.ServerResultException e) {
                LOG.warn("Encountered error looking for files under " + root, e);
                return null;
            }
        }
        return RepositoryLocationFactory.getLocationFor(root, client, details);
    }

    @Deprecated
    @Nullable
    //@Override
    public RepositoryLocation getLocationFor(FilePath root, String repositoryPath) {
        // TODO should this use repository path?
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding location for root [" + root + "], path [" + repositoryPath + "]");
        }
        return getLocationFor(root);
    }

    @Nullable
    @Override
    public VcsCommittedListsZipper getZipper() {
        LOG.debug("Creating the zipper");
        return new VcsCommittedListsZipperAdapter(new VcsCommittedListsZipperAdapter.GroupCreator() {
            @Override
            public Object createKey(RepositoryLocation repositoryLocation) {
                return repositoryLocation;
            }

            @Override
            public RepositoryLocationGroup createGroup(Object o, Collection<RepositoryLocation> collection) {
                return new RepositoryLocationGroup(o.toString());
            }
        }){};
    }

    /**
     * Called by IDE in a Worker Thread.
     *
     * @param settings settings
     * @param location location
     * @param maxCount count
     * @return list of changes, which MUST be modifiable.
     * @throws VcsException if there was a problem on the server.
     */
    @Override
    public List<P4CommittedChangelist> getCommittedChanges(P4ChangeBrowserSettings settings,
            RepositoryLocation location, int maxCount) throws VcsException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding committed changes for location " + location);
        }
        try {
            List<P4CommittedChangelist> changes = asyncLoadCommittedChanges(settings, location, maxCount)
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
            // MUST return a modifiable list
            return new ArrayList<>(changes);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        } catch (CancellationException e) {
            throw new VcsInterruptedException(e);
        }
    }

    // v171 - v183 has ... AsynchConsumer<CommittedChangelist>
    // while v191+ has ... AsynchConsumer<? super CommittedChangelist>
    // The "fix" is to strip off the typing conflict.
    @Override
    public void loadCommittedChanges(P4ChangeBrowserSettings settings, RepositoryLocation location, int maxCount,
            AsynchConsumer consumer) throws VcsException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading committed changes for location " + location);
        }
        if (consumer == null) {
            LOG.debug("Attempted to load committed changes with null consumer");
            return;
        }
        asyncLoadCommittedChanges(settings, location, maxCount)
                .whenCompleted((c) -> c.forEach(consumer::consume))
                .whenAnyState(consumer::finished)
                .whenServerError((e) -> LOG.warn("Problem loading committed changes", e));
    }

    @NotNull
    private P4CommandRunner.QueryAnswer<List<P4CommittedChangelist>> asyncLoadCommittedChanges(
            P4ChangeBrowserSettings settings, RepositoryLocation location, int maxCount) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading committed changes for " + location);
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (location == null || registry == null) {
            return new DoneQueryAnswer<>(Collections.emptyList());
        }
        if (location instanceof P4RepositoryLocation) {
            P4RepositoryLocation repo = (P4RepositoryLocation) location;
            ClientConfig clientConfig = registry.getRegisteredClientConfigState(repo.getClientServerRef());
            if (clientConfig == null) {
                LOG.warn("Could not find configuration for " + repo.getClientServerRef());
                return new DoneQueryAnswer<>(Collections.emptyList());
            }

            P4Vcs vcs = P4Vcs.getInstance(project);
            return P4ServerComponent
                    .query(project, clientConfig,
                            new ListSubmittedChangelistsQuery(repo, settings.getQueryFilter(), maxCount))
                    .mapQuery((c) -> c.getChangesForVcs(vcs));
        }
        LOG.warn("Cannot load changes for non-perforce repository location " + location);
        return new DoneQueryAnswer<>(Collections.emptyList());
    }

    @Override
    public ChangeListColumn[] getColumns() {
        LOG.debug("Getting columns");
        return new ChangeListColumn[] {
                // Need to support revision or changelist view...  See #180
                // ChangeListColumn.NUMBER,
                new RevisionColumn(project),

                ChangeListColumn.NAME,
                ChangeListColumn.DESCRIPTION,
                ChangeListColumn.DATE,
                HAS_SHELVED,
        };
    }

    @Nullable
    @Override
    public VcsCommittedViewAuxiliary createActions(DecoratorManager manager, RepositoryLocation location) {
        LOG.debug("Creating actions");
        List<AnAction> allActions =
                Collections.singletonList(new ChangelistDescriptionAction());
        return new VcsCommittedViewAuxiliary(
                allActions,
                () -> {
                    // on dispose - do nothing
                    LOG.debug("Disposing view");
                },
                allActions
        );
    }

    /**
     * since may be different for different VCSs
     */
    @Override
    public int getUnlimitedCountValue() {
        return 0;
    }

    /**
     * Called by IDE in a Worker Thread.
     *
     * @param file file
     * @param number revision
     * @return required list and path of the target file in that revision (changes when move/rename)
     */
    @Nullable
    @Override
    public Pair<P4CommittedChangelist, FilePath> getOneList(VirtualFile file, VcsRevisionNumber number) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting one list for " + file + " " + number);
        }
        FilePath fp = VcsUtil.getFilePath(file);
        if (fp == null) {
            return null;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            return Pair.create(null, fp);
        }
        ClientConfigRoot clientConfig = registry.getClientFor(file);
        if (clientConfig == null) {
            return Pair.create(null, fp);
        }

        String revision;
        if (number != null) {
            revision = number.asString();

            if (revision == null || revision.isEmpty()) {
                revision = "#head";
            } else if (!(revision.charAt(0) == '@' || revision.charAt(0) == '#')) {
                revision = '#' + revision;
            }
        } else {
            revision = "#head";
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting changelist for " + file + " " + revision);
        }

        // FIXME pull the revision from the server.  This is in a worker thread, so do a blocking wait.
        LOG.warn("FIXME pull the revision from the server.  This is in a worker thread, so do a blocking wait.");
        return Pair.create(null, fp);
    }

    @Override
    public RepositoryLocation getForNonLocal(VirtualFile file) {
        return getLocationFor(VcsUtil.getFilePath(file));
    }

    /**
     * Return true if this committed changes provider can be used to show the incoming changes.
     * If false is returned, the "Incoming" tab won't be shown in the Changes toolwindow.
     */
    @Override
    public boolean supportsIncomingChanges() {
        return false;
    }

    // --------------------------------------------------------------------------------------
    // Caching support API
    // The plugin has its own caching mechanism, but this is explicitly for the server-side
    // commits, which the plugin shouldn't maintain by itself.

    /**
     * Returns the current version of the binary data format that is read/written by the caching provider.
     * If the format version loaded from the cache stream does not match the format version returned by
     * the provider, the cache stream is discarded and changes are reloaded from server.
     *
     * @return binary format version.
     */
    @Override
    public int getFormatVersion() {
        return 0;
    }

    @Override
    public void writeChangeList(DataOutput dataOutput, P4CommittedChangelist p4CommittedChangelist)
            throws IOException {
        // 1. Commit Date - long
        Date commitDate = p4CommittedChangelist.getCommitDate();
        dataOutput.writeLong(commitDate.getTime());

        // 2. Change Collection size - int
        Collection<Change> changes = p4CommittedChangelist.getChanges();
        dataOutput.writeInt(changes.size());
        for (Change change : changes) {
            // 3.a. before revision
            writeContentRevision(change.getBeforeRevision(), dataOutput);

            // 3.b. after revision
            writeContentRevision(change.getAfterRevision(), dataOutput);
        }

        // 4. Summary
        P4ChangelistSummary summary = p4CommittedChangelist.getSummary();
        // 4.a. id
        P4ChangelistId changeId = summary.getChangelistId();
        // 4.a.i. client server ref
        ClientServerRef ref = changeId.getClientServerRef();
        // 4.a.i.A. server name full port - utf
        dataOutput.writeUTF(ref.getServerName().getFullPort());
        // 4.a.i.B. client name
        if (ref.getClientName() == null) {
            // 4.a.i.B.i has client name - int
            dataOutput.writeInt(0);
        } else {
            // 4.a.i.B.i has client name - int
            dataOutput.writeInt(1);
            // 4.a.i.B.ii client name - utf
            dataOutput.writeUTF(ref.getClientName());
        }
        // 4.a.ii. changelist ID - int
        dataOutput.writeInt(changeId.getChangelistId());

        // 4.b. Comment - utf
        dataOutput.writeUTF(summary.getComment());
        // 4.c. username - utf
        dataOutput.writeUTF(summary.getUsername());
        // 4.d. submitted - int
        dataOutput.writeInt(summary.isSubmitted() ? 1 : 0);
        // 4.e. has shelved - int
        dataOutput.writeInt(summary.hasShelvedFiles() ? 1 : 0);
    }

    @Override
    public P4CommittedChangelist readChangeList(RepositoryLocation repositoryLocation, DataInput dataInput)
            throws IOException {
        // 1. Commit Date - long
        Date commitDate = new Date(dataInput.readLong());

        // 2. Change Collection size - int
        int changeCount = dataInput.readInt();
        List<Change> changes = new ArrayList<>(changeCount);
        for (int i = 0; i < changeCount; i++) {
            // 3.a. before revision
            P4RemoteFileContentRevision beforeRevision = readContentRevision(dataInput);

            // 3.b. after revision
            P4RemoteFileContentRevision afterRevision = readContentRevision(dataInput);

            changes.add(new Change(beforeRevision, afterRevision));
        }

        // 4. Summary

        // 4. Summary
        // 4.a. id
        // 4.a.i. client server ref
        // 4.a.i.A. server name full port - utf
        P4ServerName serverName = P4ServerName.forPort(dataInput.readUTF());
        assert serverName != null;
        // 4.a.i.B. client name
        String clientName;
        // 4.a.i.B.i has client name - int
        int hasClientName = dataInput.readInt();
        if (hasClientName == 0) {
            clientName = null;
        } else {
            // 4.a.i.B.ii client name - utf
            clientName = dataInput.readUTF();
        }
        ClientServerRef ref = new ClientServerRef(serverName, clientName);
        // 4.a.ii. changelist ID - int
        int changelistId = dataInput.readInt();
        P4ChangelistId changeId = new P4ChangelistIdImpl(changelistId, ref);

        // 4.b. Comment - utf
        String comment = dataInput.readUTF();
        // 4.c. username - utf
        String username = dataInput.readUTF();
        // 4.d. submitted - int
        int hasSubmitted = dataInput.readInt();
        // 4.e. has shelved - int
        int hasShelvedFiles = dataInput.readInt();
        P4ChangelistSummaryImpl summary = new P4ChangelistSummaryImpl(
                changeId, comment, username, hasSubmitted != 0, hasShelvedFiles != 0
        );

        return new P4CommittedChangelistImpl(summary, changes, commitDate);
    }

    private void writeContentRevision(ContentRevision revision, DataOutput dataOutput)
            throws IOException {
        if (revision instanceof P4RemoteFileContentRevision) {
            P4RemoteFileContentRevision rev = (P4RemoteFileContentRevision) revision;

            // 1. Is "revision" set - int
            dataOutput.writeInt(1);

            // 2. depot file
            // 2.a. path - utf
            dataOutput.writeUTF(rev.getDepotPath().getDepotPath());
            // 2.b. display name - utf
            dataOutput.writeUTF(rev.getDepotPath().getDisplayName());
            String depotLocalPath = rev.getDepotPath().getLocalPath().orElse(null);
            if (depotLocalPath == null) {
                // 2.c. is local path non-null - int
                dataOutput.writeInt(0);
            } else {
                // 2.c. is local path non-null - int
                dataOutput.writeInt(1);
                // 2.c.i. local path - utf
                dataOutput.writeUTF(depotLocalPath);
            }

            // 3. file path
            FilePath filePath = rev.getFile();
            if (filePath.isNonLocal()) {
                // 3.a. is file path used - int
                dataOutput.writeInt(0);
            } else {
                // 3.a. is file path used - int
                dataOutput.writeInt(1);
                // 3.a.i. file path - utf
                dataOutput.writeUTF(filePath.getPath());
            }

            // 4. Revision - int
            dataOutput.writeInt(rev.getIntRevisionNumber().getValue());

            // 5. Charset - utf
            dataOutput.writeUTF(rev.getCharset().name());
        } else {
            // 1. Is "revision" set - int
            dataOutput.writeInt(0);
        }
    }

    private P4RemoteFileContentRevision readContentRevision(DataInput dataInput)
            throws IOException {
        // 1. Is "revision" set - int
        int isSet = dataInput.readInt();
        if (isSet == 1) {
            // 2. depot file
            // 2.a. path - utf
            String depotPath = dataInput.readUTF();
            // 2.b. display name - utf
            String depotDisplayName = dataInput.readUTF();
            // 2.c. is local path non-null - int
            String localPath;
            int hasLocalPath = dataInput.readInt();
            if (hasLocalPath == 0) {
                localPath = null;
            } else {
                // 2.c.i. local path - utf
                localPath = dataInput.readUTF();
            }
            P4RemoteFile file = new P4RemoteFileImpl(depotPath, depotDisplayName, localPath);

            // 3. file path
            FilePath path;
            // 3.a. is file path used - int
            int hasFilePath = dataInput.readInt();
            if (hasFilePath == 0) {
                path = null;
            } else {
                // 3.a.i. file path - utf
                path = VcsUtil.getFilePath(dataInput.readUTF());
            }

            // 4. Revision - int
            VcsRevisionNumber.Int rev = new VcsRevisionNumber.Int(dataInput.readInt());

            // 5. Charset - utf
            Charset charset = Charset.forName(dataInput.readUTF());

            return P4RemoteFileContentRevision.delayCreation(project, file, path, rev, loader, charset);
        } else {
            return null;
        }
    }

    @Override
    public boolean isMaxCountSupported() {
        return true;
    }

    @Nullable
    @Override
    public Collection<FilePath> getIncomingFiles(RepositoryLocation repositoryLocation)
            throws VcsException {
        // No real concept of incoming files.
        return null;
    }

    @Override
    public boolean refreshCacheByNumber() {
        // use numbers, not dates
        return true;
    }

    /**
     * Returns the name of the "changelist" concept in the specified VCS (changelist, revision etc.)
     *
     * @return the name of the concept, or null if the VCS (like CVS) does not use changelist numbering.
     */
    @Nls
    @Nullable
    @Override
    public String getChangelistTitle() {
        return P4Bundle.getString("changelist.title");
    }

    @Override
    public boolean isChangeLocallyAvailable(FilePath filePath, @Nullable VcsRevisionNumber localRevision,
            VcsRevisionNumber changeRevision, P4CommittedChangelist changelist) {
        // All committed revisions are remote.
        return false;
    }

    /**
     * Returns true if a timer-based refresh of committed changes should be followed by refresh of incoming changes, so that,
     * for example, changes from the wrong branch would be automatically removed from view.
     *
     * @return true if auto-refresh includes incoming changes refresh, false otherwise
     */
    @Override
    public boolean refreshIncomingWithCommitted() {
        return false;
    }


    public static class P4ChangeBrowserSettings extends ChangeBrowserSettings {
        String SHOW_ONLY_SHELVED_FILTER = "false";

        // TODO add shelved filter option to filter editor
        public void setShowOnlyShelvedFilter(@Nullable String showFilter) {
            SHOW_ONLY_SHELVED_FILTER = showFilter == null ? "false" :
                    Boolean.valueOf(Boolean.parseBoolean(showFilter)).toString();
        }

        boolean isShowOnlyShelvedFilter() {
            return Boolean.parseBoolean(SHOW_ONLY_SHELVED_FILTER);
        }

        ListSubmittedChangelistsQuery.Filter getQueryFilter() {
            ListSubmittedChangelistsQuery.Filter ret = new ListSubmittedChangelistsQuery.Filter();
            ret.setOnlyChangesWithShelvedFilesFilter(isShowOnlyShelvedFilter());
            ret.setChangesAfterChangelistFilter(getChangeAfterFilter());
            ret.setChangesBeforeChangelistFilter(getChangeBeforeFilter());
            ret.setChangesAfterDateFilter(getDateAfterFilter());
            ret.setChangesBeforeDateFilter(getDateBeforeFilter());
            return ret;
        }

        @NotNull
        @Override
        protected List<Filter> createFilters() {
            final List<Filter> ret = super.createFilters();

            if (isShowOnlyShelvedFilter()) {
                // FIXME
                throw new IllegalStateException("not implemented");
                /*
                ret.add(new Filter() {
                    @Override
                    public boolean accepts(final CommittedChangeList change) {
                        if (change != null && change instanceof P4CommittedChangeList) {
                            P4CommittedChangeList p4cl = (P4CommittedChangeList) change;
                            return p4cl.hasShelved();
                        }
                        return true;
                    }
                });
                */
            }

            return ret;
        }
    }

    private static class RevisionColumn extends ChangeListColumn<P4CommittedChangelist> {
        private final Project project;

        private RevisionColumn(Project project) {
            this.project = project;
        }

        @Override
        public String getTitle() {
            return NUMBER.getTitle();
        }

        @Override
        public Object getValue(P4CommittedChangelist p4CommittedChangelist) {
            if (UserProjectPreferences.getPreferRevisionsForFiles(project)) {
                return p4CommittedChangelist.getNumber();
            }
            return p4CommittedChangelist.getSummary().getChangelistId().getChangelistId();
        }

        @NotNull
        public Comparator<P4CommittedChangelist> getComparator() {
            return Comparator.comparing(CommittedChangeList::getNumber);
        }
    }

    private static final ChangeListColumn<P4CommittedChangelist> HAS_SHELVED =
            new ChangeListColumn<P4CommittedChangelist>() {
        @Override
        public String getTitle() {
            return P4Bundle.message("changelist.shelved");
        }

        @Override
        public Object getValue(final P4CommittedChangelist changeList) {
            // FIXME implement correctly
            // committed changelists can't have shelved files... so this is probably the wrong object.
            LOG.warn("FIXME implement HAS_SHELVED getValue correctly");
            return false;
        }
    };
}
