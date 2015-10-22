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

package net.groboclown.idea.p4ic.v2.server.cache.state;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps track of the local and server state for a single client.
 * This is how each one views the client state, as well as the
 * pending updates.
 */
public class ClientLocalServerState {
    private static final Logger LOG = Logger.getInstance(ClientLocalServerState.class);

    private final P4ClientState localClientState;
    private final P4ClientState cachedServerState;
    private final List<PendingUpdateState> pendingUpdates;
    private final FileMappingRepo fileRepo;

    public ClientLocalServerState(
            @NotNull final P4ClientState localClientState,
            @NotNull final P4ClientState cachedServerState,
            @NotNull final List<PendingUpdateState> pendingUpdates) {
        this.localClientState = localClientState;
        this.cachedServerState = cachedServerState;
        this.pendingUpdates = pendingUpdates;
        this.fileRepo = new FileMappingRepo(cachedServerState.isServerCaseInsensitive());
    }

    @NotNull
    public ClientServerId getClientServerId() {
        return cachedServerState.getClientServerId();
    }

    @NotNull
    public P4ClientState getLocalClientState() {
        return localClientState;
    }

    @NotNull
    public List<PendingUpdateState> getPendingUpdates() {
        return Collections.unmodifiableList(pendingUpdates);
    }

    @NotNull
    public PendingUpdateState addPendingUpdate(@NotNull PendingUpdateState update) {
        pendingUpdates.add(update);
        return update;
    }

    public void removePendingUpdate(@NotNull PendingUpdateState pendingUpdateState) {
        pendingUpdates.remove(pendingUpdateState);
    }


    @NotNull
    public P4ClientState getCachedServerState() {
        return cachedServerState;
    }

    @NotNull
    public FileMappingRepo getFileMappingRepo() {
        return fileRepo;
    }

    protected void serialize(@NotNull final Element wrapper, @NotNull final EncodeReferences refs) {
        Element local = new Element("local");
        wrapper.addContent(local);
        localClientState.serialize(local, refs);
        Element server = new Element("server");
        wrapper.addContent(server);
        cachedServerState.serialize(server, refs);
        for (PendingUpdateState pendingUpdate : pendingUpdates) {
            Element update = new Element("update");
            wrapper.addContent(update);
            pendingUpdate.serialize(update, refs);
        }

        for (P4ClientFileMapping file : fileRepo.getAllFiles()) {
            refs.getFileMappingId(file);
        }
    }

    @Nullable
    protected static ClientLocalServerState deserialize(@NotNull final Element wrapper, @NotNull final DecodeReferences refs) {
        LOG.debug("deserializing");
        Element local = wrapper.getChild("local");
        Element server = wrapper.getChild("server");
        if (local == null || server == null) {
            LOG.warn("deserializing state discovered local or server workspace xml element is null");
            return null;
        }
        P4ClientState localClient = P4ClientState.deserialize(local, refs);
        P4ClientState cachedRemote = P4ClientState.deserialize(server, refs);
        if (localClient == null || cachedRemote == null) {
            LOG.warn("deserializing state discovered local or remote workspace deserialize is null");
            return null;
        }
        List<PendingUpdateState> pending = new ArrayList<PendingUpdateState>();
        for (Element el: wrapper.getChildren("update")) {
            PendingUpdateState update = PendingUpdateState.deserialize(el, refs);
            if (update != null) {
                pending.add(update);
            }
        }


        final ClientLocalServerState ret = new ClientLocalServerState(localClient, cachedRemote, pending);

        ret.fileRepo.refreshFiles(refs.getFileMappings());

        return ret;
    }
}
