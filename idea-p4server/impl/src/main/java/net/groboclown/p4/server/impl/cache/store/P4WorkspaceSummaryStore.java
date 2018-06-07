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

package net.groboclown.p4.server.impl.cache.store;

import net.groboclown.p4.server.api.values.P4WorkspaceSummary;
import net.groboclown.p4.server.impl.values.P4WorkspaceSummaryImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class P4WorkspaceSummaryStore {
    @SuppressWarnings("WeakerAccess")
    public static class State {
        public String clientname;
        public long lastUpdate;
        public long lastAccess;
        public String owner;
        public String description;
        Map<P4WorkspaceSummary.ClientOption, Boolean> clientOptions;
        P4WorkspaceSummary.SubmitOption submitOption;
        P4WorkspaceSummary.LineEnding lineEnding;
        P4WorkspaceSummary.ClientType clientType;
        List<String> roots;
        String host;
        String serverId;
        String stream;
        int streamAtChange;
    }

    @NotNull
    public static State getState(@NotNull P4WorkspaceSummary summary) {
        State ret = new State();
        ret.clientname = summary.getClientName();
        ret.lastUpdate = summary.getLastUpdate().getTime();
        ret.lastAccess = summary.getLastAccess().getTime();
        ret.owner = summary.getOwner();
        ret.description = summary.getOwner();
        ret.clientOptions = new HashMap<>(summary.getClientOptions());
        ret.submitOption = summary.getSubmitOption();
        ret.lineEnding = summary.getLineEnding();
        ret.clientType = summary.getClientType();
        ret.roots = new ArrayList<>(summary.getRoots());
        ret.host = summary.getHost();
        ret.serverId = summary.getServerId();
        ret.stream = summary.getStream();
        ret.streamAtChange = summary.getStreamAtChange();
        return ret;
    }

    @NotNull
    public P4WorkspaceSummary read(@NotNull State state) {
        return new P4WorkspaceSummaryImpl.Builder()
                .setClientname(state.clientname)
                .setLastUpdate(new Date(state.lastUpdate))
                .setLastAccess(new Date(state.lastAccess))
                .setOwner(state.owner)
                .setDescription(state.description)
                .setOptions(state.clientOptions)
                .setSubmitOption(state.submitOption)
                .setLineEnding(state.lineEnding)
                .setClientType(state.clientType)
                .setRoots(state.roots)
                .setHost(state.host)
                .setServerId(state.serverId)
                .setStream(state.stream)
                .setStreamAtChange(state.streamAtChange)
                .createP4WorkspaceSummary();
    }
}
