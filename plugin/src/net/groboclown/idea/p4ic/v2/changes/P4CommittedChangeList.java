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
package net.groboclown.idea.p4ic.v2.changes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeListImpl;
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.extension.P4ChangelistNumber;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.history.P4ContentRevision;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class P4CommittedChangeList extends CommittedChangeListImpl implements VcsRevisionNumberAware {

    @NotNull
    private final P4Vcs myVcs;
    @NotNull
    private final P4ChangelistNumber myRevision;

    public P4CommittedChangeList(@NotNull P4Vcs vcs, @NotNull P4Server server, @NotNull IChangelist changelist) throws VcsException {
        super(changelist.getId() + ": " + changelist.getDescription(),
                changelist.getDescription(),
                changelist.getUsername(),
                changelist.getId(),
                changelist.getDate(),
                createChanges(vcs, server, changelist.getId()));
        myVcs = vcs;

        // Does not use the P4CurrentRevisionNumber, because this is for changelist
        myRevision = new P4ChangelistNumber(changelist);
    }

    @Override
    public AbstractVcs getVcs() {
        return myVcs;
    }

    @Override
    public String toString() {
        return getComment();
    }

    @Nullable
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return myRevision;
    }


    @NotNull
    private static Collection<Change> createChanges(@NotNull P4Vcs vcs, @NotNull P4Server server, int changelistId) throws VcsException {
        final Collection<P4FileAction> files;
        try {
            files = server.getOpenFiles();
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
        List<FilePath> inChange = new ArrayList<FilePath>(files.size());
        for (P4FileAction file : files) {
            if (file.getChangeList() == changelistId) {
                inChange.add(file.getFile());
            }
        }
        try {
            final Map<FilePath, IExtendedFileSpec> status =
                    server.getFileStatus(inChange);
            if (status == null) {
                throw new P4DisconnectedException();
            }
            Map<P4FileAction, IExtendedFileSpec> actionStatus = new HashMap<P4FileAction, IExtendedFileSpec>();
            for (P4FileAction file : files) {
                IExtendedFileSpec spec = status.remove(file.getFile());
                if (spec != null) {
                    actionStatus.put(file, spec);
                }
            }
            return createChanges(vcs.getProject(), actionStatus);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }

    @NotNull
    private static Collection<Change> createChanges(@NotNull Project project,
            @NotNull Map<P4FileAction, IExtendedFileSpec> files) {
        List<Change> ret = new ArrayList<Change>(files.size());
        for (Entry<P4FileAction, IExtendedFileSpec> entry : files.entrySet()) {
            P4ContentRevision before = null;
            P4ContentRevision after = null;

            assert entry.getKey().getFile() != null: "file is null";
            final FileStatus fileStatus = entry.getKey().getClientFileStatus();
            if (fileStatus != P4Vcs.DELETED_OFFLINE && fileStatus != FileStatus.DELETED) {
                // not open for delete
                after = new P4ContentRevision(project, entry.getKey().getFile(), entry.getValue());
            }
            if (entry.getValue().getHaveRev() > 0) {
                before = new P4ContentRevision(project, entry.getKey().getFile(), entry.getValue(),
                        entry.getValue().getHaveRev() - 1);
            }

            Change c = new Change(before, after);
            ret.add(c);
        }
        return ret;
    }
}
