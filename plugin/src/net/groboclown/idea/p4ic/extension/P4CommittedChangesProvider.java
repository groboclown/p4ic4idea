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
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4CommittedChangesProvider.P4ChangeBrowserSettings;
import net.groboclown.idea.p4ic.v2.changes.P4CommittedChangeList;
import net.groboclown.idea.p4ic.v2.history.P4RepositoryLocation;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class P4CommittedChangesProvider implements CommittedChangesProvider<P4CommittedChangeList, P4ChangeBrowserSettings> {
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
        // FIXME cache the values?

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
            // FIXME alert the error
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
        // FIXME implement

        return null;
    }

    @Override
    public void loadCommittedChanges(P4ChangeBrowserSettings settings, RepositoryLocation location, int maxCount, AsynchConsumer<CommittedChangeList> consumer) throws VcsException {
        // FIXME
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
        return null;
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
                    P4CommittedChangeList changeList = server.getChangelistForOnline(fp, revision);

                    return Pair.create(changeList, fp);
                }
            }
            // FIXME use the correct string
            P4CommittedChangeList changeList = server.getChangelistForOnline(fp, "#head");
            return Pair.create(changeList, fp);
        } catch (InterruptedException e) {
            // FIXME show alert
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
