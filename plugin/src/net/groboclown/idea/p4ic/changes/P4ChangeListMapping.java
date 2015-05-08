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
package net.groboclown.idea.p4ic.changes;

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.perforce.p4java.core.IChangelist;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Maps between perforce changelists and IDEA changelists.  Due to the
 * IDEA changelist mechanism, we must persist this mapping.
 * <p>
 * This handles the creation of the Perforce changelists, but the IDEA changelist
 * creation should only happen during the
 * {@link P4ChangesViewRefresher} life cycle.
 * </p>
 * <p>
 * It's up to the calling classes to reload the {@link P4ChangeListCache}.
 * </p>
 */
@State(
        name = "P4ChangeListMapping",
        roamingType = RoamingType.DISABLED,
        storages = {
                @Storage(
                        file = StoragePathMacros.WORKSPACE_FILE
                )
        }
)
public class P4ChangeListMapping implements PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(P4ChangeListMapping.class);

    public static final String DEFAULT_CHANGE_NAME = VcsBundle.message("changes.default.changelist.name");

    public static final int P4_DEFAULT = IChangelist.DEFAULT;
    public static final int P4_UNKNOWN = IChangelist.UNKNOWN;

    @NotNull
    private final Project project;

    @NotNull
    private State state = new State();

    private final Object sync = new Object();

    public P4ChangeListMapping(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public Element getState() {
        Element ret = new Element("p4-idea-changelist-mapping");
        for (Map.Entry<String, Map<String, P4ChangeListId>> en: state.ideaToPerforce.entrySet()) {
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
                    Map<String, P4ChangeListId> i2p = new HashMap<String, P4ChangeListId>();
                    newState.ideaToPerforce.put(idea, i2p);
                    for (Element p4id: p4idList) {
                        String scid = p4id.getAttributeValue("scid");
                        String client = p4id.getAttributeValue("client");
                        try {
                            int clid = Integer.parseInt(p4id.getAttributeValue("clid"));
                            if (scid != null && client != null) {
                                P4ChangeListId p4cl = new P4ChangeListIdImpl(scid, client, clid);
                                i2p.put(getServerClientId(p4cl), p4cl);
                                newState.perforceToIdea.put(p4cl, idea);
                            }
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            }
        }
        this.state = newState;
        LOG.debug("Loaded state: " + this.state.perforceToIdea + " ; " + this.state.ideaToPerforce);
    }

    private static class State {
        // idea change id -> (server / client ID -> p4 change)
        Map<String, Map<String, P4ChangeListId>> ideaToPerforce = new HashMap<String, Map<String, P4ChangeListId>>();
        Map<P4ChangeListId, String> perforceToIdea = new HashMap<P4ChangeListId, String>();
    }


    public static boolean isDefaultChangelist(@Nullable LocalChangeList idea) {
        return (idea != null && idea.getName().equals(DEFAULT_CHANGE_NAME));
    }

    @Nullable
    public LocalChangeList getDefaultIdeaChangelist() {
        return ChangeListManager.getInstance(project).getDefaultChangeList();
    }


    /**
     * There is always a Perforce default changelist, so this will never return null.
     *
     * @param client
     * @return The Perforce changelist for the current project's default changelist, or
     *      just the Perforce default changelist.
     */
    @NotNull
    public P4ChangeListId getProjectDefaultPerforceChangelist(@NotNull Client client) {
        LocalChangeList change = getDefaultIdeaChangelist();
        P4ChangeListId ret = null;
        if (change != null) {
            ret = getPerforceChangelistFor(client, change);
        }
        if (ret == null) {
            return new P4ChangeListIdImpl(client, P4_DEFAULT);
        }
        return ret;
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
    void bindChangelists(@NotNull LocalChangeList idea, @NotNull P4ChangeListId p4id) {
        checkForInvalidMapping(idea, p4id);
        if (isDefaultChangelist(idea) && p4id.isDefaultChangelist()) {
            // this is an implicit mapping
            return;
        }
        synchronized (sync) {
            Map<String, P4ChangeListId> p4ChangeMap = state.ideaToPerforce.get(idea.getId());
            if (p4ChangeMap != null) {
                if (p4ChangeMap.containsKey(getServerClientId(p4id))) {
                    // ensure the other-way-around exists and is correct
                    state.perforceToIdea.put(p4id, idea.getId());

                    LOG.warn("Already have mapping for IDEA changelist " + idea +
                            " to perforce change " + state.ideaToPerforce.get(idea.getId()) +
                            "; going to overwrite this to Perforce change " + p4id);
                    return;
                }
            } else {
                p4ChangeMap = new HashMap<String, P4ChangeListId>();
                state.ideaToPerforce.put(idea.getId(), p4ChangeMap);
            }
            p4ChangeMap.put(getServerClientId(p4id), p4id);


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

    @Nullable
    String removePerforceMapping(@NotNull P4ChangeListId p4id) {
        if (p4id.isDefaultChangelist()) {
            LOG.warn("cannot remove the default changelist mapping");
            return null;
        }
        String idea;
        synchronized (sync) {
            idea = state.perforceToIdea.remove(p4id);
            if (idea != null) {
                final Map<String, P4ChangeListId> i2p = state.ideaToPerforce.get(idea);
                if (i2p != null) {
                    i2p.remove(getServerClientId(p4id));
                    if (i2p.isEmpty()) {
                        state.ideaToPerforce.remove(idea);
                    }
                }
            }
        }
        return idea;
    }


    /**
     * Removes the mapping for the IDEA changelist.  This should be
     * called if the user deletes the change, or if the change is
     * submitted.  The
     * {@link P4ChangesViewRefresher}
     * will manage recreating the IDEA changelist if necessary.
     *
     * @param idea IDEA backed changelist
     * @return the associated Perforce changelist ID if known, null if not.
     */
    @Nullable
    Map<String, P4ChangeListId> removeMapping(@NotNull LocalChangeList idea) {
        if (isDefaultChangelist(idea)) {
            LOG.warn("cannot remove the default changelist mapping");
            return null;
        }
        final Map<String, P4ChangeListId> p4idList;
        synchronized (sync) {
            p4idList = state.ideaToPerforce.remove(idea.getId());
            if (p4idList != null) {
                for (P4ChangeListId p4id: p4idList.values()) {
                    state.perforceToIdea.remove(p4id);
                }
            }
        }
        return p4idList;
    }

    @Nullable
    LocalChangeList getLocalChangelist(@NotNull Client client, int changeListId) {
        ChangeListManager clm = ChangeListManager.getInstance(project);
        if (changeListId <= P4_DEFAULT) {
            for (LocalChangeList cl: clm.getChangeLists()) {
                if (cl.getName().equals(DEFAULT_CHANGE_NAME)) {
                    return cl;
                }
            }
            LocalChangeList cl = clm.getDefaultChangeList();
            LOG.warn("Could not find changelist named " + DEFAULT_CHANGE_NAME + "; returning " + cl.getName());
            return cl;
        }

        P4ChangeListId p4id = new P4ChangeListIdImpl(client, changeListId);

        final String id;
        synchronized (sync) {
            id = state.perforceToIdea.get(p4id);
        }
        if (id == null) {
            return null;
        }
        LocalChangeList cl = clm.getChangeList(id);
        LOG.info("Mapped p4 changelist " + p4id + " to " + cl);
        return cl;
    }


    /**
     * Does this state contain the given mapping to the Perforce change list?
     *
     * @param changeList IDEA changelist
     * @return true if it maps to a Perforce changelist
     */
    public boolean hasPerforceChangelist(@NotNull LocalChangeList changeList) {
        if (isDefaultChangelist(changeList)) {
            return true;
        }

        synchronized (sync) {
            return state.ideaToPerforce.containsKey(changeList.getId());
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
    public P4ChangeListId getPerforceChangelistFor(@NotNull Client client, @NotNull LocalChangeList idea) {
        Collection<P4ChangeListId> changelists = getInnerPerforceChangelists(idea);
        if (changelists != null) {
            for (P4ChangeListId clid : changelists) {
                if (clid.isIn(client)) {
                    return clid;
                }
            }
        }
        return null;
    }


    /**
     * Fetches the cached mapping from IDEA to the Perforce changelists for
     * all clients.  If
     * the Perforce changelist is committed or deleted, this will not identify
     * that change.
     *
     * @param idea idea-backed changelist
     * @return null if the changelist mapping is not known, or if it is the
     *     default changelist, otherwise the corresponding Perforce changes.
     */
    @Nullable
    public Collection<P4ChangeListId> getPerforceChangelists(@NotNull LocalChangeList idea) {
        Collection<P4ChangeListId> ret = getInnerPerforceChangelists(idea);
        if (ret == null) {
            return null;
        }
        return Collections.unmodifiableCollection(ret);
    }


    /**
     * Fetches the cached mapping from IDEA to the Perforce changelist.  If
     * the Perforce changelist is committed or deleted, this will not identify
     * that change.
     *
     * @param idea idea-backed changelist
     * @return null if the changelist mapping is not known,
     *      otherwise the corresponding Perforce changes.
     */
    @Nullable
    private Collection<P4ChangeListId> getInnerPerforceChangelists(@NotNull LocalChangeList idea) {
        if (isDefaultChangelist(idea)) {
            List<Client> clients = P4Vcs.getInstance(project).getClients();
            List<P4ChangeListId> ret = new ArrayList<P4ChangeListId>(clients.size());
            for (Client client: clients) {
                ret.add(new P4ChangeListIdImpl(client, P4_DEFAULT));
            }
            return ret;
        }

        Map<String, P4ChangeListId> ret;
        synchronized (sync) {
            ret = state.ideaToPerforce.get(idea.getId());
        }
        if (ret == null) {
            return null;
        }
        return ret.values();
    }


    /**
     * Removes mappings for IDEA changelists that no longer exist, and Perforce
     * changelists that no longer exist.  Used by the {@link P4ChangeProvider}.
     *
     * @return a copy of the known mappings
     */
    @NotNull
    Map<LocalChangeList, Map<Client, P4ChangeList>> cleanMappings() throws VcsException {
        // Step 1: Get all of the changes this mapping knows about
        // Load in the project changelists and use that to see what is valid.
        Set<String> localIdea;
        synchronized (sync) {
            localIdea = new HashSet<String>(state.ideaToPerforce.keySet());
        }

        // Step 2: load all the cached pending change lists
        final Map<Client, List<P4ChangeList>> pendingChanges = P4ChangeListCache.getInstance().getChangeListsForAll(
                P4Vcs.getInstance(project).getClients());
        final Map<String, Client> clientIdMap = new HashMap<String, Client>();
        final Map<P4ChangeListId, P4ChangeList> allP4Changes = new HashMap<P4ChangeListId, P4ChangeList>();
        for (Map.Entry<Client, List<P4ChangeList>> changes: pendingChanges.entrySet()) {
            clientIdMap.put(getServerClientId(changes.getKey()), changes.getKey());
            for (P4ChangeList change: changes.getValue()) {
                allP4Changes.put(change.getId(), change);
            }
        }

        // Step 3 and 4:
        Map<String, LocalChangeList> ideaIdToChange = new HashMap<String, LocalChangeList>();
        for (LocalChangeList lcl : ChangeListManager.getInstance(project).getChangeLists()) {
            ideaIdToChange.put(lcl.getId(), lcl);

            // Step 3: find all the IDEA changelists that we have as a mapping, but don't exist
            // any more.  That will be what remains in the localIdea variable after  this loop.
            if (localIdea.remove(lcl.getId())) {
                // This is a changelist that still exists and that we have in a mapping.

                // Step 4: remove all unknown changelists from the mapping.
                Collection<P4ChangeListId> p4ids = getInnerPerforceChangelists(lcl);
                if (p4ids != null) {
                    for (P4ChangeListId p4id : p4ids) {
                        if (!allP4Changes.containsKey(p4id)) {
                            removePerforceMapping(p4id);
                        }
                    }
                }
            }
        }

        // Our state is now such that, for all valid IDEA changelists, we only have
        // them mapped to valid P4 changelists.  There may still be IDEA changelists
        // in our mapping that no longer exist.

        // Step 5: Remove any non-existent IDEA changes from the mapping.
        for (String ideaCl : localIdea) {
            synchronized (sync) {
                final Map<String, P4ChangeListId> p4idList = state.ideaToPerforce.remove(ideaCl);
                if (p4idList != null) {
                    for (P4ChangeListId p4id : p4idList.values()) {
                        state.perforceToIdea.remove(p4id);
                    }
                }
            }
        }

        // Now return the valid values
        Map<LocalChangeList, Map<Client, P4ChangeList>> ret = new HashMap<LocalChangeList, Map<Client, P4ChangeList>>();
        synchronized (sync) {
            for (Map.Entry<String, Map<String, P4ChangeListId>> en : state.ideaToPerforce.entrySet()) {
                @NotNull
                LocalChangeList lcl = ideaIdToChange.get(en.getKey());

                Map<Client, P4ChangeList> changeMap = new HashMap<Client, P4ChangeList>();
                ret.put(lcl, changeMap);

                for (Map.Entry<String, P4ChangeListId> stringMap: en.getValue().entrySet()) {
                    @NotNull
                    Client client = clientIdMap.get(stringMap.getKey());

                    @NotNull
                    P4ChangeList change = allP4Changes.get(stringMap.getValue());

                    changeMap.put(client, change);
                }
            }
        }
        return ret;
    }


    private void checkForInvalidMapping(@NotNull LocalChangeList idea, @NotNull P4ChangeListId p4) {

        if (isDefaultChangelist(idea)) {
            if (! p4.isDefaultChangelist()) {
                LOG.warn("cannot pair default IDEA changelist with the non-default P4 changelist (" + p4 + ")");
            }
            // else it's fine
        } else if (p4.isDefaultChangelist()) {
            LOG.warn("cannot pair non-default IDEA changelist (" + idea + ") with the default P4 changelist");
        }
        // else it's fine.
    }


    private String getServerClientId(@NotNull P4ChangeListId id) {
        return id.getServerConfigId() + ((char) 1) + id.getClientName();
    }


    private String getServerClientId(@NotNull Client client) {
        return client.getConfig().getServiceName() + ((char) 1) + client.getClientName();
    }
}
