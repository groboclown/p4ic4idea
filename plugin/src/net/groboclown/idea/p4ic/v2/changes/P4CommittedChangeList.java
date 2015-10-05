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

import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeListImpl;
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware;
import com.perforce.p4java.core.IChangelist;
import net.groboclown.idea.p4ic.extension.P4ChangelistNumber;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

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
        /*
        List<P4FileInfo> files = client.getServer().getFilesInChangelist(changelistId);
        if (files == null) {
            return Collections.emptyList();
        }
        return createChanges(vcs.getProject(), files);
        */
        // FIXME implement
        throw new IllegalStateException("not implemented");
    }

    /*
    @NotNull
    private static Collection<Change> createChanges(@NotNull Project project, @NotNull List<P4FileInfo> files) {
        List<Change> ret = new ArrayList<Change>(files.size());
        for (P4FileInfo p4file : files) {
            P4ContentRevision before = null;
            P4ContentRevision after = null;

            if (p4file.isInClientView()) {
                if (! p4file.isOpenForDelete()) {
                    after = new P4ContentRevision(project, p4file, p4file.getHaveRev());
                }
                if (! p4file.isDeletedInDepot() || ! p4file.isInDepot()) {
                    before = new P4ContentRevision(project, p4file, p4file.getHaveRev() - 1);
                }
            }
            // else it's not in the client view, and thus not a valid change

            Change c = new Change(before, after);
            ret.add(c);
        }
        return ret;
    }
    */

}
