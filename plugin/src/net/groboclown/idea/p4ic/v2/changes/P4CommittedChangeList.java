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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeListImpl;
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.extension.P4ChangelistNumber;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.history.P4ContentRevision;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class P4CommittedChangeList extends CommittedChangeListImpl implements VcsRevisionNumberAware {
    private static final Logger LOG = Logger.getInstance(P4CommittedChangeList.class);


    @NotNull
    private final P4Vcs myVcs;
    @NotNull
    private final P4ChangelistNumber myRevision;
    private final boolean hasShelved;

    public P4CommittedChangeList(@NotNull P4Vcs vcs, @NotNull P4Server server, @NotNull IChangelist changelist,
            final List<Pair<IExtendedFileSpec, IExtendedFileSpec>> changelistFiles) throws VcsException {
        // FIXME format via bundle
        super(changelist.getId() + ": " + changelist.getDescription(),
                changelist.getDescription(),
                changelist.getUsername(),
                changelist.getId(),
                changelist.getDate(),
                createChanges(vcs, server, changelist, changelistFiles));
        myVcs = vcs;
        hasShelved = changelist.isShelved();

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

    public boolean hasShelved() {
        return hasShelved;
    }


    @NotNull
    private static Collection<Change> createChanges(@NotNull P4Vcs vcs, @NotNull P4Server server,
            @NotNull IChangelist changelist, final List<Pair<IExtendedFileSpec, IExtendedFileSpec>> changelistFiles)
            throws VcsException {
        final Project project = vcs.getProject();
        final List<Change> ret = new ArrayList<Change>(changelistFiles.size());
        final Set<IExtendedFileSpec> allSpecs = new HashSet<IExtendedFileSpec>();
        for (Pair<IExtendedFileSpec, IExtendedFileSpec> specPair : changelistFiles) {
            if (specPair.getFirst() != null) {
                allSpecs.add(specPair.getFirst());
            }
            if (specPair.getSecond() != null) {
                allSpecs.add(specPair.getSecond());
            }
        }
        try {
            final Map<IExtendedFileSpec, FilePath> mappedTo = server.mapSpecsToPath(allSpecs);
            for (Pair<IExtendedFileSpec, IExtendedFileSpec> specPair : changelistFiles) {
                final IExtendedFileSpec primary = specPair.getFirst();
                assert primary != null: "Null source spec in " + changelistFiles;
                P4ContentRevision before;
                P4ContentRevision after;
                if (specPair.getSecond() != null) {
                    // copied from second
                    before = new P4ContentRevision(project, mappedTo.get(specPair.getSecond()), specPair.getSecond());
                    after = new P4ContentRevision(project, mappedTo.get(primary), primary);
                } else {
                    switch (primary.getHeadAction()) {
                        case ADD:
                        case ADDED:
                            before = null;
                            after = new P4ContentRevision(project, mappedTo.get(primary), primary);
                            break;
                        case DELETE:
                        case DELETED:
                        case MOVE_DELETE:
                            before = new P4ContentRevision(project, mappedTo.get(primary),
                                    primary, primary.getHaveRev() - 1);
                            after = null;
                            break;
                        case BRANCH:
                        case MOVE_ADD:
                            throw new IllegalStateException("should already be handled with second");
                        default:
                            if (primary.getHaveRev() <= 1) {
                                LOG.info("action: " + primary.getHeadAction() + ", but rev is " + primary.getHaveRev());
                                before = null;
                            } else {
                                before = new P4ContentRevision(project, mappedTo.get(primary),
                                        primary, primary.getHaveRev() - 1);
                            }
                            after = new P4ContentRevision(project, mappedTo.get(primary), primary);
                    }
                }

                Change c = new Change(before, after);
                ret.add(c);
            }
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
        LOG.info("Changes for @" + changelist.getId() + ": " + ret);
        return ret;
    }
}
