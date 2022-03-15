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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerGate;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MockChangeListManagerGate implements ChangeListManagerGate {
    public LocalChangeList defaultChangelist;

    private final ChangeListManager clMgr;

    private final List<LocalChangeList> changes = new ArrayList<>();

    // public so that tests can clean it out and inspect it.
    public final List<LocalChangeList> added = new ArrayList<>();

    // public so that tests can clean it out and inspect it.
    public final List<LocalChangeList> removed = new ArrayList<>();

    public MockChangeListManagerGate(ChangeListManager clMgr) {
        this.clMgr = clMgr;
    }

    @Override
    public List<LocalChangeList> getListsCopy() {
        return new ArrayList<>(changes);
    }

    @Nullable
    @Override
    public synchronized LocalChangeList findChangeList(String name) {
        if (name == null) {
            return null;
        }
        for (LocalChangeList change : changes) {
            if (name.equals(change.getName())) {
                return change;
            }
        }
        return clMgr.findChangeList(name);
    }

    @Override
    public synchronized LocalChangeList addChangeList(String name, String comment) {
        if (findChangeList(name) != null) {
            // TODO right exception?
            throw new IllegalArgumentException("changelist already exists: " + name);
        }
        LocalChangeList cl = new MockLocalChangeList()
                .withName(name)
                .withComment(comment);
        changes.add(cl);
        added.add(cl);
        return cl;
    }

    @Override
    public synchronized LocalChangeList findOrCreateList(String name, String comment) {
        LocalChangeList cl = findChangeList(name);
        if (cl != null) {
            cl.setComment(comment);
            return cl;
        }
        return addChangeList(name, comment);
    }

    @Override
    public void editComment(@NotNull String name, @Nullable String comment) {
        LocalChangeList cl = getExisting(name);
        cl.setComment(comment);
    }

    @Override
    public void editName(@NotNull String oldName, @NotNull String newName) {
        getExisting(oldName).setName(newName);
    }

    @Override
    public void setListsToDisappear(Collection<String> names) {
        for (String name : names) {
            LocalChangeList cl = getExisting(name);
            changes.remove(cl);
            removed.add(cl);
        }
    }

    @Override
    public FileStatus getStatus(VirtualFile virtualFile) {
        throw new IllegalStateException("not implemented");
        //return null;
    }

    @Nullable
    @Override
    public FileStatus getStatus(@NotNull FilePath filePath) {
        throw new IllegalStateException("not implemented");
        //return null;
    }

    // Removed in v>=211
    @Deprecated
    //@Override
    public FileStatus getStatus(File file) {
        throw new IllegalStateException("not implemented");
        //return null;
    }

    @Override
    public void setDefaultChangeList(@NotNull String name) {
        defaultChangelist = getExisting(name);
    }

    @NotNull
    synchronized LocalChangeList getExisting(@NotNull String name) {
        LocalChangeList cl = findChangeList(name);
        if (cl == null) {
            // TODO right exception?
            throw new IllegalArgumentException("changelist does not exists: " + name);
        }
        return cl;
    }
}
