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

package net.groboclown.p4.server.impl.values;

import com.perforce.p4java.client.IClientSummary;
import net.groboclown.p4.server.api.values.P4WorkspaceSummary;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class P4WorkspaceSummaryImpl implements P4WorkspaceSummary {
    private final String clientname;
    private final Date lastUpdate;
    private final Date lastAccess;
    private final String owner;
    private final String description;
    private final Map<ClientOption, Boolean> options;
    private final SubmitOption submitOption;
    private final LineEnding lineEnding;
    private final ClientType clientType;
    private final List<String> roots;
    private final String host;
    private final String serverId;
    private final String stream;
    private final int streamAtChange;

    public static class Builder {
        private String clientname;
        private Date lastUpdate;
        private Date lastAccess;
        private String owner;
        private String description;
        private Map<P4WorkspaceSummary.ClientOption, Boolean> options;
        private P4WorkspaceSummary.SubmitOption submitOption;
        private P4WorkspaceSummary.LineEnding lineEnding;
        private P4WorkspaceSummary.ClientType clientType;
        private List<String> roots;
        private String host;
        private String serverId;
        private String stream;
        private int streamAtChange;

        public Builder setClientname(String clientname) {
            this.clientname = clientname;
            return this;
        }

        public Builder setLastUpdate(Date lastUpdate) {
            this.lastUpdate = lastUpdate;
            return this;
        }

        public Builder setLastAccess(Date lastAccess) {
            this.lastAccess = lastAccess;
            return this;
        }

        public Builder setOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setOptions(Map<P4WorkspaceSummary.ClientOption, Boolean> options) {
            this.options = options;
            return this;
        }

        public Builder setSubmitOption(P4WorkspaceSummary.SubmitOption submitOption) {
            this.submitOption = submitOption;
            return this;
        }

        public Builder setLineEnding(P4WorkspaceSummary.LineEnding lineEnding) {
            this.lineEnding = lineEnding;
            return this;
        }

        public Builder setClientType(P4WorkspaceSummary.ClientType clientType) {
            this.clientType = clientType;
            return this;
        }

        public Builder setRoots(List<String> roots) {
            this.roots = roots;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setServerId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        public Builder setStream(String stream) {
            this.stream = stream;
            return this;
        }

        public Builder setStreamAtChange(int streamAtChange) {
            this.streamAtChange = streamAtChange;
            return this;
        }

        public P4WorkspaceSummaryImpl createP4WorkspaceSummary() {
            return new P4WorkspaceSummaryImpl(clientname,
                    lastUpdate, lastAccess, owner, description, options, submitOption, lineEnding, clientType,
                    roots, host, serverId, stream, streamAtChange);
        }
    }

    public P4WorkspaceSummaryImpl(@NotNull IClientSummary client) {
        clientname = client.getName();
        lastUpdate = client.getUpdated();
        lastAccess = client.getAccessed();
        owner = client.getOwnerName();
        description = client.getDescription();
        options = convertOptions(client.getOptions());
        submitOption = convertSubmitOptions(client.getSubmitOptions());
        lineEnding = convertLineEnding(client.getLineEnd());
        clientType = convertClientType(client.getType());
        roots = convertRoots(client.getRoot(), client.getAlternateRoots());
        host = client.getHostName();
        serverId = client.getServerId();
        stream = client.getStream();
        streamAtChange = client.getStreamAtChange();
    }

    private P4WorkspaceSummaryImpl(String clientname, Date lastUpdate, Date lastAccess, String owner,
            String description, Map<ClientOption, Boolean> options, SubmitOption submitOption,
            LineEnding lineEnding, ClientType clientType, List<String> roots, String host,
            String serverId, String stream, int streamAtChange) {
        this.clientname = clientname;
        this.lastUpdate = lastUpdate;
        this.lastAccess = lastAccess;
        this.owner = owner;
        this.description = description;
        this.options = options;
        this.submitOption = submitOption;
        this.lineEnding = lineEnding;
        this.clientType = clientType;
        this.roots = roots;
        this.host = host;
        this.serverId = serverId;
        this.stream = stream;
        this.streamAtChange = streamAtChange;
    }

    @Override
    public String getClientName() {
        return clientname;
    }

    @Override
    public Date getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public Date getLastAccess() {
        return lastAccess;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<ClientOption, Boolean> getClientOptions() {
        return options;
    }

    @Override
    public SubmitOption getSubmitOption() {
        return submitOption;
    }

    @Override
    public LineEnding getLineEnding() {
        return lineEnding;
    }

    @Override
    public ClientType getClientType() {
        return clientType;
    }

    @Override
    public List<String> getRoots() {
        return roots;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getServerId() {
        return serverId;
    }

    @Override
    public String getStream() {
        return stream;
    }

    @Override
    public int getStreamAtChange() {
        return streamAtChange;
    }

    private static SubmitOption convertSubmitOptions(IClientSummary.IClientSubmitOptions submitOptions) {
        if (submitOptions == null) {
            return SubmitOption.SUBMIT_UNCHANGED;
        }
        if (submitOptions.isLeaveunchangedReopen()) {
            return SubmitOption.LEAVE_UNCHANGED_REOPEN;
        }
        if (submitOptions.isLeaveunchanged()) {
            return SubmitOption.LEAVE_UNCHANGED;
        }
        if (submitOptions.isRevertunchangedReopen()) {
            return SubmitOption.REVERT_UNCHANGED_REOPEN;
        }
        if (submitOptions.isRevertunchanged()) {
            return SubmitOption.REVERT_UNCHANGED;
        }
        if (submitOptions.isSubmitunchangedReopen()) {
            return SubmitOption.SUBMIT_UNCHANGED_REOPEN;
        }
        if (submitOptions.isSubmitunchanged()) {
            return SubmitOption.SUBMIT_UNCHANGED;
        }
        return SubmitOption.SUBMIT_UNCHANGED;
    }

    private static LineEnding convertLineEnding(IClientSummary.ClientLineEnd lineEnd) {
        if (lineEnd != null) {
            switch (lineEnd) {
                case LOCAL:
                    return LineEnding.LOCAL;
                case UNIX:
                    return LineEnding.UNIX;
                case MAC:
                    return LineEnding.MAC;
                case WIN:
                    return LineEnding.WIN;
                case SHARE:
                    return LineEnding.SHARE;
            }
        }
        return LineEnding.LOCAL;
    }

    private static ClientType convertClientType(String type) {
        if (type != null && type.toLowerCase().contains("read")) {
            return ClientType.READONLY;
        }
        return ClientType.WRITABLE;
    }

    private static Map<ClientOption, Boolean> convertOptions(IClientSummary.IClientOptions options) {
        Map<ClientOption, Boolean> ret = new HashMap<>();
        ret.put(ClientOption.ALLWRITE, options.isAllWrite());
        ret.put(ClientOption.CLOBBER, options.isClobber());
        ret.put(ClientOption.COMPRESS, options.isCompress());
        ret.put(ClientOption.LOCKED, options.isLocked());
        ret.put(ClientOption.MODTIME, options.isModtime());
        ret.put(ClientOption.RMDIR, options.isRmdir());
        return Collections.unmodifiableMap(ret);
    }

    private static List<String> convertRoots(String root, List<String> alternateRoots) {
        List<String> ret = new ArrayList<>(alternateRoots.size() + 1);
        ret.add(root);
        for (String alternateRoot : alternateRoots) {
            if (alternateRoot != null) {
                ret.add(alternateRoot);
            }
        }
        return Collections.unmodifiableList(ret);
    }
}
