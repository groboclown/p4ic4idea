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

import com.intellij.openapi.diagnostic.Logger;
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
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.values.P4CommittedChangelist;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.extension.P4CommittedChangesProvider.P4ChangeBrowserSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class P4CommittedChangesProvider implements CommittedChangesProvider<P4CommittedChangelist, P4ChangeBrowserSettings> {
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

        // FIXME
        throw new IllegalStateException("not implemented");
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
     * @param settings
     * @param location
     * @param maxCount
     * @return
     * @throws VcsException
     */
    @Override
    public List<P4CommittedChangelist> getCommittedChanges(P4ChangeBrowserSettings settings, RepositoryLocation
            location, int maxCount) throws VcsException {
        final List<P4CommittedChangelist> ret = new ArrayList<P4CommittedChangelist>();
        loadCommittedChanges(settings, location, maxCount, new AsynchConsumer<CommittedChangeList>() {
            @Override
            public void finished() {
                // do nothing
            }

            @Override
            public void consume(CommittedChangeList committedChangeList) {
                if (committedChangeList instanceof P4CommittedChangelist) {
                    ret.add((P4CommittedChangelist) committedChangeList);
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
        // FIXME
        throw new IllegalStateException("not implemented");
        /*
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
        */
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
        // FIXME
        throw new IllegalStateException("not implemented");
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
     * @param file
     * @param number
     * @return required list and path of the target file in that revision (changes when move/rename)
     */
    @Nullable
    @Override
    public Pair<P4CommittedChangelist, FilePath> getOneList(VirtualFile file, VcsRevisionNumber number)
            throws VcsException {
        // FIXME
        throw new IllegalStateException("not implemented");
        /*
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
        */
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
            // committed changelists can't have shelved files... so this is probably the wrong object.
            // FIXME
            throw new IllegalStateException("not implemented");
            //return changeList.hasShelved();
        }
    };
}
