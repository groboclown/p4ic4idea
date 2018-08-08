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

package net.groboclown.idea.mock;

import com.intellij.openapi.util.Factory;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.LocallyDeletedChange;
import com.intellij.openapi.vcs.changes.LogicalLock;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MockChangelistBuilder implements ChangelistBuilder {
    private final MockChangeListManagerGate gate;
    private final VcsKey expectedKey;

    // public for test alteration and inspection
    public final Map<String, FilePath> addedChangedFiles = new HashMap<>();
    public final Map<String, Change> addedChanges = new HashMap<>();
    public final Set<FilePath> removedChanges = new HashSet<>();
    public final Set<VirtualFile> unversioned = new HashSet<>();
    public final Set<FilePath> locallyDeleted = new HashSet<>();
    public final Set<VirtualFile> modifiedWithoutCheckout = new HashSet<>();
    public final Set<VirtualFile> ignored = new HashSet<>();
    public final Set<VirtualFile> lockedFolder = new HashSet<>();


    public MockChangelistBuilder(MockChangeListManagerGate gate, AbstractVcs vcs) {
        this.gate = gate;
        this.expectedKey = vcs.getKeyInstanceMethod();
    }

    @Override
    public void processChange(Change change, VcsKey vcsKey) {
        assertEquals(expectedKey, vcsKey);
        addChange(null, change);
    }

    @Override
    public void processChangeInList(Change change, @Nullable ChangeList changeList, VcsKey vcsKey) {
        assertEquals(expectedKey, vcsKey);
        if (changeList == null) {
            addChange(null, change);
        } else {
            // ensure the changelist exists in the gate.
            gate.getExisting(changeList.getName());
            addChange(changeList.getName(), change);
        }
    }

    @Override
    public void processChangeInList(Change change, String changeListName, VcsKey vcsKey) {
        assertEquals(expectedKey, vcsKey);
        // ensure the changelist exists.
        gate.getExisting(changeListName);
        addChange(changeListName, change);
    }

    @Override
    public void removeRegisteredChangeFor(FilePath path) {
        removedChanges.add(path);
    }

    @Override
    public void processUnversionedFile(VirtualFile file) {
        unversioned.add(file);
    }

    @Override
    public void processLocallyDeletedFile(FilePath file) {
        locallyDeleted.add(file);
    }

    @Override
    public void processLocallyDeletedFile(LocallyDeletedChange locallyDeletedChange) {
        locallyDeleted.add(locallyDeletedChange.getPath());
    }

    @Override
    public void processModifiedWithoutCheckout(VirtualFile file) {
        modifiedWithoutCheckout.add(file);
    }

    @Override
    public void processIgnoredFile(VirtualFile file) {
        ignored.add(file);
    }

    @Override
    public void processLockedFolder(VirtualFile file) {
        lockedFolder.add(file);
    }

    @Override
    public void processLogicallyLockedFolder(VirtualFile file, LogicalLock logicalLock) {
        lockedFolder.add(file);
    }

    @Override
    public void processSwitchedFile(VirtualFile file, String branch, boolean recursive) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void processRootSwitch(VirtualFile file, String branch) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public boolean reportChangesOutsideProject() {
        return false;
    }

    @Override
    public void reportAdditionalInfo(String text) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void reportAdditionalInfo(Factory<JComponent> infoComponent) {
        throw new IllegalStateException("not implemented");
    }

    private void addChange(@Nullable String changeListName, @NotNull Change change) {
        addedChanges.put(changeListName, change);
        if (change.getBeforeRevision() != null) {
            addedChangedFiles.put(changeListName, change.getBeforeRevision().getFile());
        }
        if (change.getAfterRevision() != null) {
            addedChangedFiles.put(changeListName, change.getAfterRevision().getFile());
        }
    }
}
