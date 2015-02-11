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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.AsynchConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class P4CommittedChangesProvider implements CommittedChangesProvider<CommittedChangeList, ChangeBrowserSettings> {
    @NotNull
    @Override
    public ChangeBrowserSettings createDefaultSettings() {
        return new ChangeBrowserSettings();
    }

    @Override
    public ChangesBrowserSettingsEditor<ChangeBrowserSettings> createFilterUI(boolean showDateFilter) {
        return null;
    }

    @Nullable
    @Override
    public RepositoryLocation getLocationFor(FilePath root) {
        return null;
    }

    @Nullable
    @Override
    public RepositoryLocation getLocationFor(FilePath root, String repositoryPath) {
        return null;
    }

    @Nullable
    @Override
    public VcsCommittedListsZipper getZipper() {
        return null;
    }

    @Override
    public List<CommittedChangeList> getCommittedChanges(ChangeBrowserSettings settings, RepositoryLocation location, int maxCount) throws VcsException {
        return null;
    }

    @Override
    public void loadCommittedChanges(ChangeBrowserSettings settings, RepositoryLocation location, int maxCount, AsynchConsumer<CommittedChangeList> consumer) throws VcsException {

    }

    @Override
    public ChangeListColumn[] getColumns() {
        return new ChangeListColumn[0];
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
    public Pair<CommittedChangeList, FilePath> getOneList(VirtualFile file, VcsRevisionNumber number) throws VcsException {
        return null;
    }

    @Override
    public RepositoryLocation getForNonLocal(VirtualFile file) {
        return null;
    }

    /**
     * Return true if this committed changes provider can be used to show the incoming changes.
     * If false is returned, the "Incoming" tab won't be shown in the Changes toolwindow.
     */
    @Override
    public boolean supportsIncomingChanges() {
        return false;
    }
}
