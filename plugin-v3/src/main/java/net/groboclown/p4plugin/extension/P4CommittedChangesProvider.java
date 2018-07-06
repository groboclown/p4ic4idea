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
import com.intellij.openapi.vcs.ChangeListColumn;
import com.intellij.openapi.vcs.CommittedChangesProvider;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.committed.DecoratorManager;
import com.intellij.openapi.vcs.changes.committed.VcsCommittedListsZipper;
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
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsQuery;
import net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsResult;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.commands.sync.SyncListFilesDetailsQuery;
import net.groboclown.p4.server.api.repository.P4RepositoryLocation;
import net.groboclown.p4.server.api.values.P4CommittedChangelist;
import net.groboclown.p4.server.impl.commands.DoneQueryAnswer;
import net.groboclown.p4.server.impl.repository.RepositoryLocationFactory;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.extension.P4CommittedChangesProvider.P4ChangeBrowserSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public class P4CommittedChangesProvider implements CommittedChangesProvider<P4CommittedChangelist, P4ChangeBrowserSettings> {
    private static final Logger LOG = Logger.getInstance(P4CommittedChangesProvider.class);

    private final Project project;

    P4CommittedChangesProvider(@NotNull final P4Vcs vcs) {
        this.project = vcs.getProject();
    }


    @NotNull
    @Override
    public P4ChangeBrowserSettings createDefaultSettings() {
        return new P4ChangeBrowserSettings();
    }

    @Override
    public ChangesBrowserSettingsEditor<P4ChangeBrowserSettings> createFilterUI(boolean showDateFilter) {
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
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || root == null) {
            return null;
        }
        ClientConfigRoot client = registry.getClientFor(root);
        if (client == null) {
            return null;
        }
        ListFilesDetailsResult details;
        if (ApplicationManager.getApplication().isDispatchThread()) {
            // Use the cache
            details = P4ServerComponent.getInstance(project).getCommandRunner()
                    .syncCachedQuery(client.getClientConfig().getServerConfig(),
                            new SyncListFilesDetailsQuery(root));
        } else {
            try {
                details = P4ServerComponent.getInstance(project).getCommandRunner()
                        .query(client.getClientConfig().getServerConfig(),
                                new ListFilesDetailsQuery(client.getClientConfig().getClientServerRef(),
                                        Collections.singletonList(root),  ListFilesDetailsQuery.RevState.HAVE, 1))
                        .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | P4CommandRunner.ServerResultException e) {
                LOG.warn(e);
                details = P4ServerComponent.getInstance(project).getCommandRunner()
                        .syncCachedQuery(client.getClientConfig().getServerConfig(),
                                new SyncListFilesDetailsQuery(root));
            }
        }
        return RepositoryLocationFactory.getLocationFor(root, client, details);
    }

    @Nullable
    @Override
    public RepositoryLocation getLocationFor(FilePath root, String repositoryPath) {
        return getLocationFor(root);
    }

    @Nullable
    @Override
    public VcsCommittedListsZipper getZipper() {
        return null;
    }

    /**
     * Called by IDE in a Worker Thread.
     *
     * @param settings settings
     * @param location location
     * @param maxCount count
     * @return list of changes
     * @throws VcsException if there was a problem on the server.
     */
    @Override
    public List<P4CommittedChangelist> getCommittedChanges(P4ChangeBrowserSettings settings,
            RepositoryLocation location, int maxCount) throws VcsException {
        try {
            return asyncLoadCommittedChanges(settings, location, maxCount)
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | CancellationException e) {
            throw new VcsException(e);
        }
    }

    @Override
    public void loadCommittedChanges(P4ChangeBrowserSettings settings, RepositoryLocation location, int maxCount,
            AsynchConsumer<CommittedChangeList> consumer) throws VcsException {
        if (consumer == null) {
            return;
        }
        asyncLoadCommittedChanges(settings, location, maxCount)
                .whenCompleted((c) -> c.forEach(consumer::consume))
                .after(consumer::finished);
    }

    @NotNull
    private P4CommandRunner.QueryAnswer<List<P4CommittedChangelist>> asyncLoadCommittedChanges(
            P4ChangeBrowserSettings settings, RepositoryLocation location, int maxCount) throws VcsException {
        // TODO use settings to determine if shelved changes should be returned.
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (location == null || registry == null) {
            return new DoneQueryAnswer<>(Collections.emptyList());
        }
        if (location instanceof P4RepositoryLocation) {
            P4RepositoryLocation repo = (P4RepositoryLocation) location;
            ClientConfigRoot clientConfig = registry.getRegisteredClientConfigState(repo.getClientServerRef());
            if (clientConfig == null) {
                LOG.warn("Could not find configuration for " + repo.getClientServerRef());
                return new DoneQueryAnswer<>(Collections.emptyList());
            }

            return P4ServerComponent.getInstance(project).getCommandRunner()
                    .query(clientConfig.getServerConfig(), new ListSubmittedChangelistsQuery(repo, maxCount))
                    .mapQuery(ListSubmittedChangelistsResult::getChanges);
        }
        LOG.warn("Cannot load changes for non-perforce repository location " + location);
        return new DoneQueryAnswer<>(Collections.emptyList());
    }

    @Override
    public ChangeListColumn[] getColumns() {
        return new ChangeListColumn[] {
                ChangeListColumn.NUMBER,
                ChangeListColumn.NAME,
                ChangeListColumn.DESCRIPTION,
                ChangeListColumn.DATE,
                HAS_SHELVED,
        };
    }

    @Nullable
    @Override
    public VcsCommittedViewAuxiliary createActions(DecoratorManager manager, RepositoryLocation location) {
        List<AnAction> allActions =
                // FIXME add an action to view the description of a changelist.
                // Collections.<AnAction>singletonList(new ChangelistDescriptionAction());
                Collections.emptyList();
        LOG.warn("FIXME add an action to view the description of a changelist.");
        return new VcsCommittedViewAuxiliary(
                allActions,
                () -> {
                    // on dispose - do nothing
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

        LOG.info("Getting changelist for " + file + " " + revision);

        // FIXME pull the revision from the server.  This is in a worker thread, so do a blocking wait.
        LOG.warn("FIXME pull the revision from the server.  This is in a worker thread, so do a blocking wait.");
        return Pair.create(null, fp);
    }

    @Override
    public RepositoryLocation getForNonLocal(VirtualFile file) {
        // FIXME
        throw new IllegalStateException("not implemented");
        //return getLocationFor(FilePathUtil.getFilePath(file));
    }

    /**
     * Return true if this committed changes provider can be used to show the incoming changes.
     * If false is returned, the "Incoming" tab won't be shown in the Changes toolwindow.
     */
    @Override
    public boolean supportsIncomingChanges() {
        return false;
    }


    public static class P4ChangeBrowserSettings extends ChangeBrowserSettings {
        String SHOW_ONLY_SHELVED_FILTER = "false";

        public void setShowOnlyShelvedFilter(@Nullable String showFilter) {
            SHOW_ONLY_SHELVED_FILTER = showFilter == null ? "false" :
                    Boolean.valueOf(Boolean.parseBoolean(showFilter)).toString();
        }

        boolean isShowOnlyShelvedFilter() {
            return Boolean.parseBoolean(SHOW_ONLY_SHELVED_FILTER);
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


    static final ChangeListColumn<P4CommittedChangelist> HAS_SHELVED = new ChangeListColumn<P4CommittedChangelist>() {
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
