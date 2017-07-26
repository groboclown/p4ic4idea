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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.*;
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
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4CommittedChangesProvider.P4ChangeBrowserSettings;
import net.groboclown.idea.p4ic.ui.history.ChangelistDescriptionAction;
import net.groboclown.idea.p4ic.v2.changes.P4CommittedChangeList;
import net.groboclown.idea.p4ic.v2.history.P4RepositoryLocation;
import net.groboclown.idea.p4ic.v2.history.P4SimpleRepositoryLocation;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class P4CommittedChangesProvider implements CommittedChangesProvider<P4CommittedChangeList, P4ChangeBrowserSettings> {
    private static final Logger LOG = Logger.getInstance(P4CommittedChangesProvider.class);

    private final P4Vcs vcs;

    public P4CommittedChangesProvider(@NotNull final P4Vcs vcs) {
        this.vcs = vcs;
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
    public RepositoryLocation getLocationFor(FilePath root) {
        // TODO cache the values?

        try {
            final P4Server server = vcs.getP4ServerFor(root);
            if (server == null) {
                return null;
            }
            final Map<FilePath, IExtendedFileSpec> specMap =
                    server.getFileStatus(Collections.singletonList(root));
            if (specMap == null) {
                return null;
            }
            IExtendedFileSpec spec = specMap.get(root);
            if (spec == null) {
                return null;
            }
            return new P4RepositoryLocation(spec);
        } catch (InterruptedException e) {
            // FIXME send notification to the Alert system.
        }

        return null;
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

    @Override
    public List<P4CommittedChangeList> getCommittedChanges(P4ChangeBrowserSettings settings, RepositoryLocation location, int maxCount) throws VcsException {
        final List<P4CommittedChangeList> ret = new ArrayList<P4CommittedChangeList>();
        loadCommittedChanges(settings, location, maxCount, new AsynchConsumer<CommittedChangeList>() {
            @Override
            public void finished() {
                // do nothing
            }

            @Override
            public void consume(CommittedChangeList committedChangeList) {
                if (committedChangeList instanceof P4CommittedChangeList) {
                    ret.add((P4CommittedChangeList) committedChangeList);
                } else {
                    throw new IllegalArgumentException("Must be P4CommitedChangeList: " + committedChangeList);
                }
            }
        });
        return ret;
    }

    @Override
    public void loadCommittedChanges(P4ChangeBrowserSettings settings, RepositoryLocation location, int maxCount,
            AsynchConsumer<CommittedChangeList> consumer) throws VcsException {
        if (consumer == null) {
            return;
        }
        if (location == null) {
            consumer.finished();
            return;
        }
        final IFileSpec spec;
        if (location instanceof P4RepositoryLocation) {
            spec = ((P4RepositoryLocation) location).getP4FileInfo();
        } else if (location instanceof P4SimpleRepositoryLocation) {
            spec = ((P4SimpleRepositoryLocation) location).getP4FileInfo();
        } else {
            LOG.warn("Must be P4RepositoryLocation or P4SimpleRepositoryLocation: " + location);
            consumer.finished();
            return;
        }

        for (P4Server p4Server : vcs.getP4Servers()) {
            try {
                for (P4CommittedChangeList changeList: p4Server.getChangelistsForOnline(spec, maxCount)) {
                    consumer.consume(changeList);
                }
            } catch (InterruptedException e) {
                LOG.info(e);
            }
        }
        consumer.finished();
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
        List<AnAction> allActions = Collections.<AnAction>singletonList(new ChangelistDescriptionAction());
        return new VcsCommittedViewAuxiliary(
                allActions,
                new Runnable() {
                    @Override
                    public void run() {
                        // do nothing
                    }
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
     * @param file
     * @param number
     * @return required list and path of the target file in that revision (changes when move/rename)
     */
    @Nullable
    @Override
    public Pair<P4CommittedChangeList, FilePath> getOneList(VirtualFile file, VcsRevisionNumber number) throws VcsException {
        FilePath fp = FilePathUtil.getFilePath(file);
        try {
            final P4Server server = vcs.getP4ServerFor(fp);
            if (server == null) {
                return new Pair<P4CommittedChangeList, FilePath>(null, fp);
            }
            if (number != null) {
                String revision = number.asString();
                if (revision != null && revision.length() > 0 && (revision.charAt(0) == '@' || revision
                        .charAt(0) == '#')) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Getting changelist for revision " + revision + "; " + fp);
                    }
                    P4CommittedChangeList changeList = server.getChangelistForOnline(fp, revision);

                    return Pair.create(changeList, fp);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.info("Getting changelist for head revision; " + fp);
            }
            // FIXME use the correct constant string
            P4CommittedChangeList changeList = server.getChangelistForOnline(fp, "#head");
            return Pair.create(changeList, fp);
        } catch (InterruptedException e) {
            // FIXME use alert manager to report it
            LOG.warn(e);
            return null;
        }
    }

    @Override
    public RepositoryLocation getForNonLocal(VirtualFile file) {
        return getLocationFor(FilePathUtil.getFilePath(file));
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
        public String SHOW_ONLY_SHELVED_FILTER = "false";

        public void setShowOnlyShelvedFilter(@Nullable String showFilter) {
            SHOW_ONLY_SHELVED_FILTER = showFilter == null ? "false" :
                    Boolean.valueOf(Boolean.parseBoolean(showFilter)).toString();
        }

        public boolean isShowOnlyShelvedFilter() {
            return SHOW_ONLY_SHELVED_FILTER != null && Boolean.parseBoolean(SHOW_ONLY_SHELVED_FILTER);
        }

        @NotNull
        @Override
        protected List<Filter> createFilters() {
            final List<Filter> ret = super.createFilters();

            if (isShowOnlyShelvedFilter()) {
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
            }

            return ret;
        }
    }


    static final ChangeListColumn<P4CommittedChangeList> HAS_SHELVED = new ChangeListColumn<P4CommittedChangeList>() {
        @Override
        public String getTitle() {
            return P4Bundle.message("changelist.shelved");
        }

        @Override
        public Object getValue(final P4CommittedChangeList changeList) {
            return changeList.hasShelved();
        }
    };
}
