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
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.changes.*;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ShelvedFile;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches up {@link LocalChangeList} objects to {@link P4ChangeListValue}
 * objects, and creates local changes if there isn't one for a P4 change.
 */
public class ChangeListMatcher {
    private static final Logger LOG = Logger.getInstance(ChangeListMatcher.class);

    private final Project project;
    private final P4ChangeListMapping changeListMapping;


    public ChangeListMatcher(@NotNull final P4Vcs vcs, @NotNull AlertManager alerts) {
        this.project = vcs.getProject();
        this.changeListMapping = P4ChangeListMapping.getInstance(project);
    }


    @NotNull
    public Map<P4Server, Map<P4ChangeListValue, LocalChangeList>> getLocalChangelistMapping(
            @NotNull Set<P4Server> p4Servers, @NotNull ChangeListManagerGate addGate)
            throws InterruptedException {
        Map<P4Server, Map<P4ChangeListValue, LocalChangeList>> ret =
                new HashMap<P4Server, Map<P4ChangeListValue, LocalChangeList>>();
        for (P4Server server : p4Servers) {
            if (server != null) {
                final Collection<P4ChangeListValue> changes = server.getOpenChangeLists();
                changeListMapping.cleanServerMapping(server.getClientServerId(), changes);
                ret.put(server, mapIdeaChanges(addGate, changes));
            }
        }
        return ret;
    }


    // This can return null if a changelist was submitted, and there is no corresponding real p4 change
    // anymore.
    @Nullable
    public LocalChangeList getChangeList(@NotNull P4FileAction action, @NotNull P4Server server,
            @NotNull Map<P4Server, Map<P4ChangeListValue, LocalChangeList>> mappedChanges) {
        final Map<P4ChangeListValue, LocalChangeList> changes = mappedChanges.get(server);
        if (changes == null) {
            throw new IllegalStateException("Bad setup: no changes set for server " + server.getClientServerId());
        }

        for (Entry<P4ChangeListValue, LocalChangeList> entry: changes.entrySet()) {
            final P4ChangeListValue p4cl = entry.getKey();
            // only need to check for the changelist #.  The changes are going to be
            // in the same server.
            if (p4cl.getChangeListId() == action.getChangeList()) {
                return entry.getValue();
            }
        }

        return null;
        //
        //throw new IllegalStateException("Bad setup: no matching changelist for server " + server.getClientServerRef() +
        //        " changelist " + action.getChangeList() + " (known changes for server: " + changes.keySet() + ")");
    }

    @NotNull
    Change createChange(@NotNull P4FileAction file) {
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
                beforeRev = new P4CurrentContentRevision(project, file);
                afterRev = null;
                break;
            default:
                beforeRev = new P4CurrentContentRevision(project, file);
                afterRev = new CurrentContentRevision(file.getFile());
        }
        return new Change(beforeRev, afterRev, file.getClientFileStatus());
    }

    @NotNull
    Change createChange(@NotNull ClientServerRef clientServerRef, @NotNull P4ShelvedFile shelvedFile) {
        final ContentRevision rev = new P4ShelvedContentRevision(project, clientServerRef, shelvedFile.getDepotPath());
        if (shelvedFile.isAdded()) {
            return new Change(null, rev, shelvedFile.getStatus());
        }
        if (shelvedFile.isDeleted()) {
            return new Change(rev, null, shelvedFile.getStatus());
        }
        if (shelvedFile.isEdited()) {
            // TODO understand this better.  We need to somehow construct the
            // before revision right.
            return new Change(null, rev, shelvedFile.getStatus());
        }
        return new Change(null, rev, shelvedFile.getStatus());
    }

    @NotNull
    private Map<P4ChangeListValue, LocalChangeList> mapIdeaChanges(@NotNull ChangeListManagerGate addGate,
            @NotNull Collection<P4ChangeListValue> changes) {
        // Note: the "unused local changes" is based on a per-server basis.
        // This means an idea changelist could potentially be shared across servers
        // when it was constructed by this mapping, rather than by the user.
        // However, that should be fine.

        final List<LocalChangeList> allLocalChanges = ChangeListManager.getInstance(project).getChangeLists();
        final List<LocalChangeList> unusedLocalChanges = new ArrayList<LocalChangeList>(allLocalChanges);
        final Map<P4ChangeListValue, LocalChangeList> ret = new HashMap<P4ChangeListValue, LocalChangeList>();
        final List<P4ChangeListValue> unmapped = new ArrayList<P4ChangeListValue>();
        for (P4ChangeListValue change: changes) {
            LocalChangeList local = changeListMapping.getIdeaChangelistFor(change);
            if (local == null) {
                unmapped.add(change);
            } else {
                ret.put(change, local);
                unusedLocalChanges.remove(local);
            }
        }

        if (! unmapped.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Attempting to match p4 changelists " + unmapped + " to existing IDEA changelists " +
                        unusedLocalChanges);
            }
            loadUnmappedChangelists(ret, unmapped, unusedLocalChanges, allLocalChanges, addGate);
        }

        return ret;
    }

    private void loadUnmappedChangelists(
            @NotNull Map<P4ChangeListValue, LocalChangeList> mapping,
            @NotNull List<P4ChangeListValue> unmappedP4,
            @NotNull Collection<LocalChangeList> unusedIdeaChangeLists,
            @NotNull Collection<LocalChangeList> allIdeaChangeLists,
            @NotNull ChangeListManagerGate addGate) {
        // For each p4 pending changelist that isn't already associated with an
        // IDEA changelist, find a match to any IDEA changelist that doesn't
        // already have a p4 changelist for that specific client.

        // However, the outer loop is the matchers.  This prevents aggressive
        // consumption of changes - allow the more exact matchers to match the
        // changelists before a looser one tries to match.

        for (ClMatcher matcher : CL_MATCHERS) {
            final Iterator<P4ChangeListValue> iter = unmappedP4.iterator();
            while (iter.hasNext()) {
                final P4ChangeListValue p4cl = iter.next();
                if (mapping.containsKey(p4cl)) {
                    // Already been mapped in this loop
                    iter.remove();
                    continue;
                }

                if (p4cl.isDefaultChangelist()) {
                    // special default changelist handling.  The IDEA default
                    // named changelist should always exist to correspond to the
                    // Perforce default changelist.  Note that this is
                    // independent of the matchers.

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Associating " + p4cl.getClientServerRef() + " default changelist to IDEA changelist " +
                                P4ChangeListId.DEFAULT_CHANGE_NAME);
                    }
                    LocalChangeList local = addGate.findOrCreateList(P4ChangeListId.DEFAULT_CHANGE_NAME, "");
                    mapping.put(p4cl, local);
                    iter.remove();
                } else {
                    // it's not associated with any IDEA changelist.
                    LocalChangeList match = matcher.match(p4cl, splitNameComment(p4cl), unusedIdeaChangeLists);

                    if (match != null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Associating " + p4cl.getChangeListId() + " to IDEA changelist " + match);
                        }

                        // Ensure the name matches the changelist, with a unique name
                        setUniqueName(match, p4cl, allIdeaChangeLists);

                        // Record this new association
                        mapping.put(p4cl, match);
                        iter.remove();

                        // Force the binding to take effect.
                        changeListMapping.bindChangelists(match, p4cl.getIdObject());
                    }
                }
            }
        }

        for (P4ChangeListValue p4cl : unmappedP4) {
            LocalChangeList lcl = createUniqueChangeList(addGate, p4cl, allIdeaChangeLists);
            mapping.put(p4cl, lcl);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Binding " + p4cl.getChangeListId() + " to IDEA changelist " + lcl);
            }
            changeListMapping.bindChangelists(lcl, p4cl.getIdObject());
        }
    }

    private static String[] splitNameComment(P4ChangeListValue p4cl) {
        String[] ret = new String[2];
        String desc = p4cl.getComment();
        if (p4cl.isDefaultChangelist()) {
            ret[0] = P4ChangeListId.DEFAULT_CHANGE_NAME;
            ret[1] = "";
        } else if (desc == null) {
            ret[0] = "";
            ret[1] = "";
        } else if (desc.indexOf('\n') > 0) {
            ret[0] = desc.substring(0, desc.indexOf('\n')).trim();
            ret[1] = desc.substring(desc.indexOf('\n') + 1).trim();
        } else if (desc.length() > 20) {
            ret[0] = desc.substring(0, 20).trim();
            ret[1] = desc.trim();
        } else {
            ret[0] = desc.trim();
            ret[1] = "";
        }
        return ret;
    }

    // This matches with createUniqueChangeList
    private void setUniqueName(@NotNull LocalChangeList changeList,
            @NotNull P4ChangeListValue cls,
            @NotNull Collection<LocalChangeList> existingIdeaChangeLists) {
        String[] desc = splitNameComment(cls);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Mapped @" + cls.getChangeListId() + " to " + changeList.getName());
        }
        String name = desc[0];
        int count = -1;
        findLoop:
        while (true) {
            for (LocalChangeList lcl : existingIdeaChangeLists) {
                if (!Comparing.equal(lcl.getId(), changeList.getId()) &&
                        lcl.getName().equals(name)) {
                    // the new name collides with an existing change (not the same change)
                    name = desc[0] + " (" + (++count) + ")";
                    continue findLoop;
                }
            }

            // it's a unique name!
            changeList.setName(name);
            changeList.setComment(desc[1]);
            return;
        }
    }

    // This matches with setUniqueName
    private LocalChangeList createUniqueChangeList(@NotNull final ChangeListManagerGate addGate,
            @NotNull P4ChangeListValue cls, @NotNull Collection<LocalChangeList> allIdeaChangeLists) {
        String[] desc = splitNameComment(cls);
        String baseDesc = desc[0];
        if (baseDesc == null || baseDesc.length() <= 0) {
            baseDesc = '@' + Integer.toString(cls.getChangeListId());
        }
        String name = baseDesc;
        int count = -1;
        findLoop:
        while (true) {
            // Make sure we check against ALL change lists, not just the
            // filtered ones.
            for (LocalChangeList lcl : allIdeaChangeLists) {
                if (lcl.getName().equals(name)) {
                    // the new name collides with an existing change (not the same change)
                    name = baseDesc + " (" + (++count) + ")";
                    continue findLoop;
                }
            }

            // it's a unique name!
            LOG.info("Mapped " + cls.getChangeListId() + " to new IDEA change " + name);
            // This can sometimes cause an assertion error, so don't let that
            // happen - use "findOrCreateList" instead of "addList".
            LocalChangeList ret = addGate.findOrCreateList(name, desc[1]);
            allIdeaChangeLists.add(ret);
            return ret;
        }
    }


    private interface ClMatcher {
        @Nullable
        LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists);
    }

    private static ClMatcher[] CL_MATCHERS = new ClMatcher[]{
            new ExactMatcher(),
            new CasedNameMatcher(),
            new NameMatcher(),
            new NameNumberedMatcher(),
            new NumberedMatcher(),
            new NumberedIndexedMatcher(),
            new ChangedNumberMatcher()
    };

    private static class ExactMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (desc[0].equals(lcl.getName().trim())) {
                    String comment = lcl.getComment();
                    if (comment == null && desc[1].length() <= 0) {
                        return lcl;
                    }
                    if (comment != null && desc[1].equals(comment.trim())) {
                        return lcl;
                    }
                }
            }
            return null;
        }
    }

    private static class CasedNameMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (desc[0].equals(lcl.getName().trim())) {
                    // IDEA has unique changelist names
                    return lcl;
                }
            }
            return null;
        }
    }

    private static class NameMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (desc[0].equalsIgnoreCase(lcl.getName().trim())) {
                    // IDEA has unique changelist names
                    return lcl;
                }
            }
            return null;
        }
    }

    private static class NameNumberedMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            // match ignore case "[p4 name](\s+\(\d+\)\)"
            Pattern pattern = Pattern.compile(Pattern.quote(desc[0]) + "\\s+\\(\\d+\\)", Pattern.CASE_INSENSITIVE);
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (pattern.matcher(lcl.getName().trim()).matches()) {
                    return lcl;
                }
            }
            return null;
        }
    }

    private static class NumberedMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            // matches "(@1234)"
            String match = "(@" + cls.getChangeListId() + ")";
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (match.equals(lcl.getName().trim())) {
                    return lcl;
                }
            }
            return null;
        }
    }

    private static class NumberedIndexedMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            // Matches "@1234 (212)" and "@1234"
            Pattern pattern = Pattern.compile("^@" + cls.getChangeListId() + "(\\s+\\(\\d+\\d\\))?$");
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                final Matcher match = pattern.matcher(lcl.getName().trim());
                if (match.matches()) {
                    return lcl;
                }
            }
            return null;
        }
    }

    private static class ChangedNumberMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            // Matches "asdf.@1234" and "x @1234 y" and so on.
            Pattern pattern = Pattern.compile("\\W@" + cls.getChangeListId() + "(\\W|$)");
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                final Matcher match = pattern.matcher(lcl.getName().trim());
                if (match.matches()) {
                    return lcl;
                }
            }
            return null;
        }
    }
}
