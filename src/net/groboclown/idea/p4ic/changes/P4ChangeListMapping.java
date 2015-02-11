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
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CancellationException;

/**
 * Maps between perforce changelists and IDEA changelists.  Due to the
 * IDEA changelist mechanism, we must persist this mapping.
 * <p>
 * This handles the creation of the Perforce changelists, but the IDEA changelist
 * creation should only happen during the
 * {@link P4ChangesViewRefresher} life cycle.
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
        for (Map.Entry<String, Map<String, P4ClId>> en: state.ideaToPerforce.entrySet()) {
            Element idea = new Element("idea-map");
            ret.addContent(idea);
            idea.setAttribute("idea-id", en.getKey());
            for (P4ClId p4cl: en.getValue().values()) {
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
                    Map<String, P4ClId> i2p = new HashMap<String, P4ClId>();
                    newState.ideaToPerforce.put(idea, i2p);
                    for (Element p4id: p4idList) {
                        String scid = p4id.getAttributeValue("scid");
                        String client = p4id.getAttributeValue("client");
                        try {
                            int clid = Integer.parseInt(p4id.getAttributeValue("clid"));
                            if (scid != null && client != null) {
                                P4ClId p4cl = new P4ClId(scid, client, clid);
                                i2p.put(p4cl.getServerClientId(), p4cl);
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
        LOG.info("Loaded state: " + this.state.perforceToIdea + " ; " + this.state.ideaToPerforce);
    }

    private static class State {
        // idea change id -> (server / client ID -> p4 change)
        Map<String, Map<String, P4ClId>> ideaToPerforce = new HashMap<String, Map<String, P4ClId>>();
        Map<P4ClId, String> perforceToIdea = new HashMap<P4ClId, String>();
    }


    // To be part of the state means that this can't be immutable - it needs setters and getters.
    public static class P4ClId implements P4ChangeListId {
        private final int clid;

        @NotNull
        private final String scid;

        @NotNull
        private final String clientName;

        P4ClId(@NotNull ServerConfig sc, @NotNull IChangelistSummary summary) {
            if (summary.getId() < 0) {
                LOG.debug("pending changelist: " + summary.getDescription() + " has invalid id: " + summary.getId() +
                        "; marking it as the default changelist");
                clid = P4_DEFAULT;
            } else {
                clid = summary.getId();
            }
            this.scid = sc.getServiceName();
            this.clientName = summary.getClientId();
            assert this.clid >= 0;
        }

        P4ClId(@NotNull Client client, int clid) {
            if (clid < 0) {
                LOG.warn("pending changelist has invalid id: " + clid + "; marking it as the default changelist");
                clid = P4_DEFAULT;
            }
            this.clid = clid;
            this.scid = client.getConfig().getServiceName();
            this.clientName = client.getClientName();
            assert clid >= 0;
        }

        P4ClId(@NotNull String scid, @NotNull String clientName, int clid) {
            if (clid < 0) {
                LOG.warn("pending changelist has invalid id: " + clid + "; marking it as the default changelist");
                clid = P4_DEFAULT;
            }
            this.clid = clid;
            this.scid = scid;
            this.clientName = clientName;
        }

        @Override
        public int getChangeListId() {
            return clid;
        }

        @NotNull
        @Override
        public String getServerConfigId() {
            return scid;
        }

        @NotNull
        @Override
        public String getClientName() {
            return clientName;
        }

        @Override
        public boolean isNumberedChangelist() {
            return clid > 0;
        }

        @Override
        public boolean isDefaultChangelist() {
            return clid == P4_DEFAULT;
        }

        @Override
        public boolean isIn(@NotNull Client client) {
            return scid.equals(client.getConfig().getServiceName()) &&
                    clientName.equals(client.getClientName());
        }


        @NotNull
        String getServerClientId() {
            return scid + ((char) 1) + clientName;
        }


        @Override
        public boolean equals(Object o) {
            if (o == null || ! (o.getClass().equals(P4ClId.class))) {
                return false;
            }
            if (o == this) {
                return true;
            }
            P4ClId that = (P4ClId) o;
            // due to the dynamic loading nature of this class, there are some
            // weird circumstances where the scid and client name can be null.
            return that.clid == this.clid &&
                    Comparing.equal(that.scid, this.scid) &&
                    Comparing.equal(that.clientName, this.clientName);
        }

        @Override
        public int hashCode() {
            return clid + scid.hashCode();
        }

        @Override
        public String toString() {
            return getServerConfigId() + "+" + getClientName() + "@" + getChangeListId();
        }
    }


    public static boolean isDefaultChangelist(@Nullable IChangelistSummary p4) {
        return (p4 != null && p4.getId() <= 0);
    }


    public static boolean isDefaultChangelist(int p4) {
        return (p4 <= 0);
    }


    public static boolean isDefaultChangelist(@Nullable LocalChangeList idea) {
        return (idea != null && idea.getName().equals(DEFAULT_CHANGE_NAME));
    }

    @Nullable
    public LocalChangeList getDefaultIdeaChangelist() {
        return ChangeListManager.getInstance(project).getDefaultChangeList();
    }

    @NotNull
    public P4ChangeListId getProjectDefaultPerforceChangelist(@NotNull Client client) {
        LocalChangeList change = getDefaultIdeaChangelist();
        P4ChangeListId ret = null;
        if (change != null) {
            LOG.warn("Default IDEA changelist is null");
            ret = getPerforceChangelistFor(client, change);
        }
        if (ret == null) {
            return new P4ClId(client, P4_DEFAULT);
        }
        return ret;
    }


    /**
     * Creates a mapping between a local IDEA changelist and a Perforce
     * changelist.
     * <p/>
     * This should never be called for the default change lists.
     *
     * @param idea idea-backed changelist
     * @param p4   perforce-backed changelist
     */
    void createMapping(@NotNull LocalChangeList idea, @NotNull ServerConfig sc, @NotNull IChangelistSummary p4) {
        createMappingToP4Id(idea, new P4ClId(sc, p4));
    }


    /**
     * Creates a mapping between a local IDEA changelist and a Perforce
     * changelist.
     * <p/>
     * This should never be called for the default change lists.
     *
     * @param idea idea-backed changelist
     * @param p4   perforce-backed changelist
     */
    void createMapping(@NotNull LocalChangeList idea, @NotNull Client client, int p4) {
        createMappingToP4Id(idea, new P4ClId(client, p4));
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
    void createMappingToP4Id(@NotNull LocalChangeList idea, @NotNull P4ClId p4id) {
        checkForInvalidMapping(idea, p4id);
        if (isDefaultChangelist(idea) && isDefaultChangelist(p4id.getChangeListId())) {
            // this is an implicit mapping
            return;
        }
        synchronized (sync) {
            Map<String, P4ClId> p4ChangeMap = state.ideaToPerforce.get(idea.getId());
            if (p4ChangeMap != null) {
                if (p4ChangeMap.containsKey(p4id.getServerClientId())) {
                    // ensure the other-way-around exists and is correct
                    state.perforceToIdea.put(p4id, idea.getId());

                    //throw new IllegalStateException("Already have mapping for IDEA changelist " + idea +
                    //        " to perforce change " + state.ideaToPerforce.get(idea.getId()) +
                    //        "; cannot overwrite this to Perforce change " + p4.getId());
                    LOG.warn("Already have mapping for IDEA changelist " + idea +
                            " to perforce change " + state.ideaToPerforce.get(idea.getId()) +
                            "; going to overwrite this to Perforce change " + p4id);
                    return;
                }
            } else {
                p4ChangeMap = new HashMap<String, P4ClId>();
                state.ideaToPerforce.put(idea.getId(), p4ChangeMap);
            }
            p4ChangeMap.put(p4id.getServerClientId(), p4id);


            if (state.perforceToIdea.containsKey(p4id)) {
                if (! state.perforceToIdea.get(p4id).equals(idea.getId())) {
                    //throw new IllegalStateException("Already have mapping for Perforce changelist " + p4.getId() +
                    //        " to IDEA change " + state.perforceToIdea.get(p4.getId()) +
                    //        "; cannot overwrite this to IDEA change '" + idea + "'");
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
    String removePerforceMapping(@NotNull P4ClId p4id) {
        if (isDefaultChangelist(p4id.getChangeListId())) {
            throw new IllegalArgumentException("cannot remove the default changelist mapping");
        }
        String idea;
        synchronized (sync) {
            idea = state.perforceToIdea.remove(p4id);
            if (idea != null) {
                // FIXME WRONG

                state.ideaToPerforce.remove(idea);
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
    Map<String, P4ClId> removeMapping(@NotNull LocalChangeList idea) {
        if (isDefaultChangelist(idea)) {
            throw new IllegalArgumentException("cannot remove the default changelist mapping");
        }
        final Map<String, P4ClId> p4idList;
        synchronized (sync) {
            p4idList = state.ideaToPerforce.remove(idea.getId());
            if (p4idList != null) {
                for (P4ClId p4id: p4idList.values()) {
                    state.perforceToIdea.remove(p4id);
                }
            }
        }
        return p4idList;
    }


    @Nullable
    public LocalChangeList getLocalChangelist(@NotNull ServerConfig sc, @NotNull IChangelistSummary p4) {
        return getLocalChangelist(new P4ClId(sc, p4));
    }


    @Nullable
    public LocalChangeList getLocalChangelist(@NotNull Client client, int p4id) {
        return getLocalChangelist(new P4ClId(client, p4id));
    }

    @Nullable
    LocalChangeList getLocalChangelist(@NotNull P4ClId p4) {
        ChangeListManager clm = ChangeListManager.getInstance(project);
        if (isDefaultChangelist(p4.getChangeListId())) {
            for (LocalChangeList cl: clm.getChangeLists()) {
                if (cl.getName().equals(DEFAULT_CHANGE_NAME)) {
                    return cl;
                }
            }
            LocalChangeList cl = clm.getDefaultChangeList();
            LOG.warn("Could not find changelist named " + DEFAULT_CHANGE_NAME + "; returning " + cl.getName());
            return cl;
        }

        final String id;
        synchronized (sync) {
            id = state.perforceToIdea.get(p4);
        }
        if (id == null) {
            return null;
        }
        LocalChangeList cl = clm.getChangeList(id);
        LOG.info("Mapped p4 changelist " + p4 + " to " + cl);
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
        Collection<P4ClId> changelists = getInnerPerforceChangelists(idea);
        if (changelists != null) {
            for (P4ClId clid : changelists) {
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
        Collection<P4ClId> ret = getInnerPerforceChangelists(idea);
        if (ret == null) {
            return null;
        }
        return Collections.<P4ChangeListId>unmodifiableCollection(ret);
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
    private Collection<P4ClId> getInnerPerforceChangelists(@NotNull LocalChangeList idea) {
        if (isDefaultChangelist(idea)) {
            List<Client> clients = P4Vcs.getInstance(project).getClients();
            List<P4ClId> ret = new ArrayList<P4ClId>(clients.size());
            for (Client client: clients) {
                ret.add(new P4ClId(client, P4_DEFAULT));
            }
            return ret;
        }

        Map<String, P4ClId> ret;
        synchronized (sync) {
            ret = state.ideaToPerforce.get(idea.getId());
        }
        if (ret == null) {
            return null;
        }
        return ret.values();
    }


    /**
     * Updates the internal mapping for the given list of changes.
     * This should be implicitly called by the
     * {@link net.groboclown.idea.p4ic.server.RawServerExecutor} during
     * requests from the server for a list of all the pending changelists.
     */
    public void updatePendingP4ChangeLists(@NotNull ServerConfig sc, @NotNull List<IChangelistSummary> changes) {
        synchronized (sync) {
            Set<P4ClId> current = new HashSet<P4ClId>(state.perforceToIdea.keySet());
            for (IChangelistSummary summary: changes) {
                if (summary != null) {
                    P4ClId p4id = new P4ClId(sc, summary);
                    if (summary.getStatus() == ChangelistStatus.SUBMITTED) {
                        LOG.info("Remove mapping for P4 changelist " + summary.getId() + "; it is now submitted");
                        removePerforceMapping(p4id);
                    }
                    current.remove(p4id);
                }
            }
            // Anything left in the current list isn't valid anymore - it could
            // have been deleted.
            for (P4ClId p4: current) {
                LOG.info("Remove mapping for P4 changelist " + p4 + "; it is now deleted");
                removePerforceMapping(p4);
            }
        }
    }


    /**
     * Updates the internal mapping for the changelist that was retrieved
     * from the server.  This should be implicitly called by the
     * {@link net.groboclown.idea.p4ic.server.RawServerExecutor} when single
     * changelists are fetched.
     *
     * @param changelistId changelist ID that was fetched
     * @param change summary retrieved by the server.
     */
    public void updateP4Changelist(@NotNull Client client, int changelistId, @Nullable IChangelistSummary change) {
        if (change == null) {
            // deleted
            removePerforceMapping(new P4ClId(client, changelistId));
        } else if (change.getStatus() == ChangelistStatus.SUBMITTED) {
            if (change.getId() != changelistId) {
                LOG.warn("Fetched changelist id " + changelistId + ", but received changelist " + change.getId() +
                        " (" + change.getDescription() + ")");
            }
            removePerforceMapping(new P4ClId(client, changelistId));
        }
    }


    /**
     * Removes mappings for IDEA changelists that no longer exist, and Perforce
     * changelists that no longer exist.
     *
     * @param pendingChangelists mapping of all pending changelists (passed in to prevent
     *                           duplicate p4 calls)
     * @return the known mappings
     * @throws com.intellij.openapi.vcs.VcsException
     * @throws java.util.concurrent.CancellationException
     */
    @NotNull
    Map<LocalChangeList, Map<Client, IChangelistSummary>> cleanMappings(@NotNull Map<Client, List<IChangelistSummary>> pendingChangelists) throws VcsException, CancellationException {
        // Load in the project changelists and use that to see what is valid.
        Set<String> localIdea;
        synchronized (sync) {
            localIdea = new HashSet<String>(state.ideaToPerforce.keySet());
        }
        List<P4ClId> changeIds = new ArrayList<P4ClId>();
        for (Map.Entry<Client, List<IChangelistSummary>> e: pendingChangelists.entrySet()) {
            for (IChangelistSummary cs : e.getValue()) {
                changeIds.add(new P4ClId(e.getKey().getConfig(), cs));
            }
        }

        // Find any current local changelists that don't have a valid pending p4 changelist
        for (LocalChangeList lcl: ChangeListManager.getInstance(project).getChangeLists()) {
            if (localIdea.remove(lcl.getId())) {
                // This is a changelist that we haven't seen in this loop, and that
                // exists in our state mapping.

                // If the mapped-to p4 changelist is not known, then we need to
                // remove this mapping.
                Collection<P4ClId> p4ids = getInnerPerforceChangelists(lcl);
                if (p4ids != null) {
                    for (P4ClId p4id: p4ids) {
                        if (!changeIds.contains(p4id)) {
                            removePerforceMapping(p4id);
                        }
                    }
                }
            }
        }

        // All known local changelists that no longer exist must be removed from the
        // mappings; we removed all the live ones from this set in the above loop
        for (String ideaCl: localIdea) {
            synchronized (sync) {
                Map<String, P4ClId> p4ids = state.ideaToPerforce.remove(ideaCl);
                if (p4ids != null) {
                    for (P4ClId p4id: p4ids.values()) {
                        state.perforceToIdea.remove(p4id);
                    }
                }
            }
        }

        // Find any registered p4 changelist that was either deleted or submitted
        Set<P4ClId> localP4;
        synchronized (sync) {
            localP4 = new HashSet<P4ClId>(state.perforceToIdea.keySet());
        }
        localP4.removeAll(changeIds);
        for (P4ClId p4id: localP4) {
            removePerforceMapping(p4id);
        }

        // Load up all known mappings for the changes
        Map<LocalChangeList, Map<Client, IChangelistSummary>> ret = new HashMap<LocalChangeList, Map<Client, IChangelistSummary>>();
        synchronized (sync) {
            for (Map.Entry<String, Map<String, P4ClId>> e: state.ideaToPerforce.entrySet()) {
                String ideaChangeId = e.getKey();

                // note that for each idea change, there can be at
                // most 1 changelist per client.
                Map<Client, IChangelistSummary> retMap = new HashMap<Client, IChangelistSummary>();
                for (P4ClId p4id: e.getValue().values()) {
                    for (Map.Entry<Client, List<IChangelistSummary>> e2 : pendingChangelists.entrySet()) {
                        Client client = e2.getKey();
                        if (client.getConfig().getServiceName().equals(p4id.getServerConfigId()) &&
                                client.getClientName().equals(p4id.getClientName())) {
                            for (IChangelistSummary cs : e2.getValue()) {
                                if (cs.getId() == p4id.clid) {
                                    retMap.put(e2.getKey(), cs);
                                    break;
                                }
                            }
                        }
                    }
                }
                ret.put(ChangeListManager.getInstance(project).getChangeList(ideaChangeId), retMap);
            }
        }
        return ret;
    }


    private void checkForInvalidMapping(@NotNull LocalChangeList idea, @NotNull P4ClId p4) {
        if (isDefaultChangelist(idea)) {
            if (! isDefaultChangelist(p4.getChangeListId())) {
                throw new IllegalArgumentException("cannot pair default IDEA changelist with the non-default P4 changelist (" + p4 + ")");
            }
            // else it's fine
        } else if (isDefaultChangelist(p4.getChangeListId())) {
            throw new IllegalArgumentException("cannot pair non-default IDEA changelist (" + idea + ") with the default P4 changelist");
        }
        // else it's fine.
    }


    private boolean isPendingOrNew(@Nullable IChangelist p4) {
        return (p4 != null) &&
                (p4.getStatus() != null) &&
                (p4.getStatus() != ChangelistStatus.SUBMITTED);
    }
}
