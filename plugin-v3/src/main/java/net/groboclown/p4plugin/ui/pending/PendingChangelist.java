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

package net.groboclown.p4plugin.ui.pending;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ShelvedFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingChangelist implements ChangeList {
    private final String name;
    private final String description;
    private final P4ChangeListValue p4;
    private final Collection<Change> changes;
    private final List<P4FileAction> projectFiles;
    private final List<P4FileAction> nonProjectFiles;

    @Nullable
    private final LocalChangeList local;

    @NotNull
    static List<PendingChangelist> getPendingChanges(@NotNull Project project) {
        List<PendingChangelist> ret = new ArrayList<PendingChangelist>();
        P4Vcs vcs = P4Vcs.getInstance(project);
        for (P4Server server : vcs.getP4Servers()) {
            try {
                Collection<P4FileAction> opened = server.getOpenFiles();
                Collection<P4ChangeListValue> changes =
                        server.getOpenChangeLists();
                for (P4ChangeListValue change : changes) {
                    PendingChangelist pc = PendingChangelist.create(project, change, opened);
                    ret.add(pc);
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        return ret;
    }

    /**
     *
     * @param p4 the Perforce changelist.
     * @param allOpenedFiles all opened files for the server ID in the Perforce changelist.
     * @return the pending changelist object.
     */
    @NotNull
    private static PendingChangelist create(@NotNull Project project, @NotNull P4ChangeListValue p4,
            @NotNull Collection<P4FileAction> allOpenedFiles) {
        List<P4FileAction> files = new ArrayList<P4FileAction>(allOpenedFiles.size());
        for (P4FileAction file : allOpenedFiles) {
            if (file.getChangeList() == p4.getChangeListId()) {
                files.add(file);
            }
        }
        LocalChangeList lcl = P4ChangeListMapping.getInstance(project).getIdeaChangelistFor(p4);
        Map<P4FileAction, Pair<Change, LocalChangeList>> lc = new HashMap<P4FileAction, Pair<Change, LocalChangeList>>();
        for (P4FileAction file : files) {
            Pair<Change, LocalChangeList> c = getChange(lcl, file);
            lc.put(file, c);
        }
        return new PendingChangelist(lcl, p4, lc);
    }

    @NotNull
    private static Pair<Change, LocalChangeList> getChange(@Nullable LocalChangeList lcl, @NotNull P4FileAction file) {
        if (lcl != null && file.getFile() != null) {
            for (Change change : lcl.getChanges()) {
                if (change.affectsFile(file.getFile().getIOFile())) {
                    return Pair.create(change, lcl);
                }
            }
        }
        /*
        Do not create an actual change object for non-project files.
        These should instead be referenced by their depot path.

        final ContentRevision beforeRev;
        final ContentRevision afterRev;
        switch (file.getFileUpdateAction()) {
            case ADD_FILE:
            case MOVE_FILE:
                // TODO move file should be a single change
                beforeRev = null;
                afterRev = new CurrentContentRevision(file.getFile());
                break;
            case DELETE_FILE:
            case MOVE_DELETE_FILE:
                beforeRev = new CurrentContentRevision(file.getFile());
                afterRev = null;
                break;
            default:
                beforeRev = new CurrentContentRevision(file.getFile());
                afterRev = new CurrentContentRevision(file.getFile());
        }
        return Pair.create(new Change(beforeRev, afterRev, file.getClientFileStatus()), null);
        */
        return Pair.create(null, null);
    }

    private PendingChangelist(@Nullable LocalChangeList local, @NotNull P4ChangeListValue p4,
            @NotNull Map<P4FileAction, Pair<Change, LocalChangeList>> fcl) {
        this.name = local == null ? p4.getComment() : local.getName();
        this.description = local == null ? "" : local.getComment();
        this.local = local;
        this.p4 = p4;

        List<Change> allChanges = new ArrayList<Change>(fcl.size());
        List<P4FileAction> pf = new ArrayList<P4FileAction>();
        List<P4FileAction> npf = new ArrayList<P4FileAction>();

        for (Map.Entry<P4FileAction, Pair<Change, LocalChangeList>> entry : fcl.entrySet()) {
            if (entry.getValue().first != null) {
                allChanges.add(entry.getValue().first);
            }
            if (entry.getValue().second == null) {
                npf.add(entry.getKey());
            } else {
                pf.add(entry.getKey());
            }
        }

        this.changes = Collections.unmodifiableCollection(allChanges);
        this.projectFiles = Collections.unmodifiableList(pf);
        this.nonProjectFiles = Collections.unmodifiableList(npf);
    }

    @Override
    public Collection<Change> getChanges() {
        return changes;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getComment() {
        return description;
    }

    @NotNull
    public List<P4FileAction> getProjectFiles() {
        return projectFiles;
    }

    @NotNull
    public List<P4FileAction> getNonProjectFiles() {
        return nonProjectFiles;
    }

    @NotNull
    public Collection<P4ShelvedFile> getShelvedFiles() {
        return p4.getShelved();
    }

    @NotNull
    public Collection<P4JobState> getJobs() {
        return p4.getJobStates();
    }

    public ChangeList getLocal() {
        return local;
    }

    public P4ChangeListValue getP4ChangeListValue() {
        return p4;
    }
}
