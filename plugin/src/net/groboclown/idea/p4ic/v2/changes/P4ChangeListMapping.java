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

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

/**
 * Maps between perforce changelists and IDEA changelists.  Due to the
 * IDEA changelist mechanism, we must persist this mapping.
 * <p>
 * This handles the creation of the Perforce changelists, but the IDEA changelist
 * creation should only happen during the
 * {@link P4ChangeProvider} life cycle.
 * </p>
 */
@State(
        name = "P4ChangeListMapping",
        storages = {
                @Storage(
                        file = StoragePathMacros.WORKSPACE_FILE
                )
        }
)
public class P4ChangeListMapping implements PersistentStateComponent<Element>, ProjectComponent {
    private static final Logger LOG = Logger.getInstance(P4ChangeListMapping.class);

    @NotNull
    private final Project project;

    @NotNull
    private State state = new State();

    private final Object sync = new Object();

    public static P4ChangeListMapping getInstance(@NotNull Project project) {
        return project.getComponent(P4ChangeListMapping.class);
    }

    public P4ChangeListMapping(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public Element getState() {
        Element ret = new Element("p4-idea-changelist-mapping");
        for (Map.Entry<String, Map<ClientServerId, P4ChangeListId>> en: state.ideaToPerforce.entrySet()) {
            Element idea = new Element("idea-map");
            ret.addContent(idea);
            idea.setAttribute("idea-id", en.getKey());
            for (P4ChangeListId p4cl: en.getValue().values()) {
                Element p4 = new Element("p4-map");
                idea.addContent(p4);
                p4.setAttribute("scid", p4cl.getServerConfigId());
                p4.setAttribute("client", p4cl.getClientName());
                p4.setAttribute("clid", Integer.toString(p4cl.getChangeListId()));
            }
        }
        return ret;
    }

    @Override
    public void loadState(@NotNull Element state) {
        State newState = new State();
        List<Element> ideaMaps = state.getChildren("idea-map");
        if (ideaMaps != null) {
            for (Element ideaMap: ideaMaps) {
                String idea = ideaMap.getAttributeValue("idea-id");
                List<Element> p4idList = ideaMap.getChildren("p4-map");
                if (idea != null && p4idList != null) {
                    Map<ClientServerId, P4ChangeListId> i2p = new HashMap<ClientServerId, P4ChangeListId>();
                    newState.ideaToPerforce.put(idea, i2p);
                    for (Element p4id: p4idList) {
                        String scid = p4id.getAttributeValue("scid");
                        String client = p4id.getAttributeValue("client");
                        try {
                            int clid = Integer.parseInt(p4id.getAttributeValue("clid"));
                            if (scid != null && client != null) {
                                P4ChangeListId p4cl = new P4ChangeListIdImpl(
                                        ClientServerId.create(scid, client), clid);
                                i2p.put(p4cl.getClientServerId(), p4cl);
                                newState.perforceToIdea.put(p4cl, idea);
                            }
                        } catch (NumberFormatException e) {
                            // ignore
                            LOG.info(e);
                        }
                    }
                }
            }
        }
        this.state = newState;
        LOG.debug("Loaded state: " + this.state.perforceToIdea + " ; " + this.state.ideaToPerforce);
    }

    @Override
    public void projectOpened() {
        // nothing to do
    }

    @Override
    public void projectClosed() {
        // nothing to do
    }

    @Override
    public void initComponent() {
        // nothing to do
    }

    @Override
    public void disposeComponent() {
        // nothing to do
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "P4ChangeListMapping";
    }

    private static class State {
        // idea change id -> (server / client ID -> p4 change)
        Map<String, Map<ClientServerId, P4ChangeListId>> ideaToPerforce = new HashMap<String, Map<ClientServerId, P4ChangeListId>>();
        Map<P4ChangeListId, String> perforceToIdea = new HashMap<P4ChangeListId, String>();
    }


    public static boolean isDefaultChangelist(@Nullable ChangeList idea) {
        return (idea != null && isIdeaDefaultChangelistName(idea.getName()));
    }

    public static boolean isIdeaDefaultChangelistName(@Nullable String name) {
        return P4ChangeListId.DEFAULT_CHANGE_NAME.equals(name);
    }

    @Nullable
    public LocalChangeList getDefaultIdeaChangelist() {
        return ChangeListManager.getInstance(project).getDefaultChangeList();
    }


    @Nullable
    public LocalChangeList getIdeaChangelistFor(@NotNull P4ChangeListValue p4cl) {
        ChangeListManager clm = ChangeListManager.getInstance(project);
        int changeListId = p4cl.getChangeListId();
        if (changeListId <= P4ChangeListId.P4_DEFAULT) {
            for (LocalChangeList cl : clm.getChangeLists()) {
                if (isIdeaDefaultChangelistName(cl.getName())) {
                    return cl;
                }
            }
            LocalChangeList cl = clm.getDefaultChangeList();
            LOG.warn("Could not find changelist named " + P4ChangeListId.DEFAULT_CHANGE_NAME + "; returning " + cl.getName());
            return cl;
        }

        P4ChangeListId p4id = new P4ChangeListIdImpl(p4cl.getClientServerId(), changeListId);

        final String id;
        synchronized (sync) {
            id = state.perforceToIdea.get(p4id);
        }
        if (id == null) {
            return null;
        }
        LocalChangeList cl = clm.getChangeList(id);
        LOG.debug("Mapped p4 changelist " + p4id + " to " + cl);
        return cl;
    }

    @NotNull
    public Collection<P4ChangeListId> getAllPerforceChangelistsFor(@NotNull LocalChangeList idea) {
        Set<P4ChangeListId> ret = new HashSet<P4ChangeListId>();
        synchronized (sync) {
            final Map<ClientServerId, P4ChangeListId> perServer = state.ideaToPerforce.get(idea.getId());
            if (perServer != null) {
                ret.addAll(perServer.values());
            }
        }
        return ret;
    }

    /**
     * There is always a Perforce default changelist, so this will never return null.
     *
     * @param server p4 server
     * @return The Perforce changelist for the current project's default changelist, or
     * just the Perforce default changelist.
     */
    @NotNull
    public P4ChangeListId getProjectDefaultPerforceChangelist(final P4Server server) {
        LocalChangeList change = getDefaultIdeaChangelist();
        P4ChangeListId ret = null;
        if (change != null) {
            ret = getPerforceChangelistFor(server, change);
        }
        if (ret == null) {
            return new P4ChangeListIdImpl(server.getClientServerId(), P4ChangeListId.P4_DEFAULT);
        }
        return ret;
    }

    public boolean hasPerforceChangelist(@NotNull LocalChangeList cl) {
        synchronized (sync) {
            return state.ideaToPerforce.containsKey(cl.getId());
        }
    }


    public Map<ClientServerId, P4ChangeListId> rebindChangelistAsDefault(@NotNull LocalChangeList list) {
        if (! isDefaultChangelist(list)) {
            throw new IllegalStateException("Can only be called after list has been renamed to the default change");
        }
        synchronized (sync) {
            LOG.info("Mapped idea " + list.getId() + " to default p4 change");
            return state.ideaToPerforce.remove(list.getId());
        }

    }


    /**
     * Creates a mapping between a local IDEA changelist and a Perforce
     * changelist.
     *
     * This should never be called for the default change lists.
     *
     * @param idea idea-backed changelist
     * @param p4id perforce-backed changelist id
     */
    public void bindChangelists(@NotNull LocalChangeList idea, @NotNull P4ChangeListId p4id) {
        // There's a situation that can occur where the IDEA changelist changed the
        // name to be default, but the ID is still bound to a real changelist.  The
        // caller should be handling this weird situation (should be done in
        // P4ChangelistListener to move the files into default),
        // but the binding needs to be redone here.

        if (isDefaultChangelist(idea) && p4id.isDefaultChangelist()) {
            // this is an implicit mapping
            return;
        }
        if (isDefaultChangelist(idea) || p4id.isDefaultChangelist()) {
            LOG.error("Attempted to bind a default changelist to a non-default changelist: " +
                idea.getName() + " to " + p4id.getChangeListId());
            return;
        }

        synchronized (sync) {
            Map<ClientServerId, P4ChangeListId> p4ChangeMap = state.ideaToPerforce.get(idea.getId());
            if (p4ChangeMap != null) {
                if (p4ChangeMap.containsKey(p4id.getClientServerId())) {
                    // ensure the other-way-around exists and is correct
                    state.perforceToIdea.put(p4id, idea.getId());

                    LOG.warn("Already have mapping for IDEA changelist " + idea +
                            " to perforce change " + state.ideaToPerforce.get(idea.getId()) +
                            "; going to overwrite this to Perforce change " + p4id);
                    return;
                }
            } else {
                p4ChangeMap = new HashMap<ClientServerId, P4ChangeListId>();
                state.ideaToPerforce.put(idea.getId(), p4ChangeMap);
            }
            p4ChangeMap.put(p4id.getClientServerId(), p4id);


            if (state.perforceToIdea.containsKey(p4id)) {
                if (! state.perforceToIdea.get(p4id).equals(idea.getId())) {
                    LOG.warn("Already have mapping for Perforce changelist " + p4id +
                            " to IDEA change " + state.perforceToIdea.get(p4id) +
                            "; going to overwrite this to IDEA change '" + idea + "'");
                }
            } else {
                state.perforceToIdea.put(p4id, idea.getId());
            }
            LOG.info("Mapped idea " + idea.getId() + " to p4 " + p4id);
        }
    }

    public void replace(@NotNull P4ChangeListId oldChangeList, @NotNull P4ChangeListId newChangeList) {
        if (! oldChangeList.getClientServerId().equals(newChangeList.getClientServerId())) {
            throw new IllegalArgumentException("client/server must match: was " +
                oldChangeList.getClientServerId() + ", now " + newChangeList.getClientServerId());
        }
        if (oldChangeList.getChangeListId() == newChangeList.getChangeListId()) {
            return;
        }
        if (! oldChangeList.isUnsynchedChangelist()) {
            throw new IllegalArgumentException("can only replace unsynchronized changelist");
        }
        synchronized (sync) {
            if (state.perforceToIdea.get(newChangeList) != null) {
                throw new IllegalArgumentException("new changelist already mapped; " + newChangeList);
            }
            final String idea = state.perforceToIdea.remove(oldChangeList);
            if (idea != null) {
                state.perforceToIdea.put(newChangeList, idea);
                final Map<ClientServerId, P4ChangeListId> changes = state.ideaToPerforce.get(idea);
                // A simple put should remove the old one, because the client server id are
                // the same, but this is just to be sure.
                changes.remove(oldChangeList.getClientServerId());
                changes.put(newChangeList.getClientServerId(), newChangeList);
            }
        }
    }


    /**
     * Fetches the cached mapping from IDEA to the Perforce changelist
     * on the given client.  This does not differentiate between a
     * changelist that never existed, and a changelist that was deleted or
     * submitted.
     *
     * @param idea idea-backed changelist
     * @return null if the changelist mapping is not known, or if it is the
     * default changelist, otherwise the corresponding Perforce change.
     */
    @Nullable
    public P4ChangeListId getPerforceChangelistFor(@NotNull P4Server server, @NotNull LocalChangeList idea) {
        // This is a little weird.  There are circumstances where the IDEA
        // changelist has changed its name to the default changelist.  So,
        // instead, we check if we have a mapping for the ID, and if so,
        // return that mapping first.

        synchronized (sync) {
            Map<ClientServerId, P4ChangeListId> ret = state.ideaToPerforce.get(idea.getId());
            if (ret == null) {
                if (isDefaultChangelist(idea)) {
                    return new P4ChangeListIdImpl(server.getClientServerId(), P4ChangeListId.P4_DEFAULT);
                }
                return null;
            }
            return ret.get(server.getClientServerId());
        }
    }


    /**
     * Removes mappings for IDEA changelists that no longer exist, and Perforce
     * changelists that no longer exist.  Used by the {@link ChangeListMatcher}.
     *
     * @param changes the changes loaded from the server.
     */
    void cleanServerMapping(@NotNull ClientServerId clientServerId,
            @NotNull Collection<P4ChangeListValue> changes) {
        final Set<Integer> newChangeIds = new HashSet<Integer>(changes.size());
        for (P4ChangeListValue change : changes) {
            newChangeIds.add(change.getChangeListId());
        }

        synchronized (sync) {
            // only need to worry about removing old mappings.

            for (Entry<String, Map<ClientServerId, P4ChangeListId>> entry : state.ideaToPerforce.entrySet()) {
                final P4ChangeListId p4clForIdea = entry.getValue().get(clientServerId);
                if (p4clForIdea != null && !newChangeIds.contains(p4clForIdea.getChangeListId())) {
                    entry.getValue().remove(clientServerId);
                    state.perforceToIdea.remove(p4clForIdea);
                }
            }

        }
    }
}
