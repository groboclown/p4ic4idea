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

package net.groboclown.idea.p4ic.config;

import com.perforce.p4java.server.IServerAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User-readable protocol + server location
 */
public class P4ServerName {
    private static final String PROTOCOL_SEP = "://";

    private final String fullName;
    private final String server;
    private final String protocolName;
    private final IServerAddress.Protocol protocol;

    @Nullable
    public static P4ServerName forPort(@Nullable final String portText) {
        if (portText == null) {
            return null;
        }

        String protocolText = null;
        String port = portText.trim();
        int splitter = portText.indexOf(PROTOCOL_SEP);
        if (splitter >= portText.length() - PROTOCOL_SEP.length()) {
            // '://' is on the end, which is invalid.
            // set the value to an invalid setting, but not null
            // to avoid an NPE.
            port = ":";
        } else if (splitter >= 0) {
            protocolText = portText.substring(0, splitter);
            port = portText.substring(splitter + PROTOCOL_SEP.length());
        } else {
            // based on http://www.perforce.com/perforce/r14.1/manuals/p4guide/chapter.configuration.html
            // format can be "port", "hostname:port", "ssl:hostname:port", "tcp:hostname:port"
            splitter = portText.indexOf(':');
            if (splitter > 0) {
                int splitter2 = portText.indexOf(':', splitter + 1);
                if (splitter2 > 0) {
                    protocolText = portText.substring(0, splitter);
                    port = portText.substring(splitter + 1);
                }
            }
        }

        if (port.indexOf(':') <= 0) {
            // This is the form "port", which is not supported by the
            // P4 java api.  So we must prepend a localhost to conform
            // to what P4 java supports.
            port = "localhost:" + port;
        }
        return new P4ServerName(port, parseProtocol(protocolText));
    }

    private P4ServerName(
            @NotNull String server,
            @NotNull IServerAddress.Protocol protocol) {
        this.server = server;
        this.protocol = protocol;

        // Bug #109: Switch the default connections over to the Nts server impl.
        // Only if the user explicitly requests the old one do we use it.
        switch (protocol) {
            case P4JRPC:
                this.protocolName = "rpc";
                break;
            case P4JRPCSSL:
                this.protocolName = "rpcssl";
                break;
            case P4JAVASSL:
                this.protocolName = "javassl";
                break;
            case P4JAVA:
                this.protocolName = "java";
                break;
            case P4JRPCNTSSSL:
                this.protocolName = "ssl";
                break;
            case P4JRPCNTS:
            default:
                // do nothing - it's the default
                this.protocolName = null;
                break;
        }

        if (this.protocolName == null) {
            this.fullName = server;
        } else {
            this.fullName = this.protocolName + PROTOCOL_SEP + server;
        }
    }

    @NotNull
    public IServerAddress.Protocol getServerProtocol() {
        return protocol;
    }

    @NotNull
    public String getProtocolName() {
        if (protocolName == null) {
            return "java";
        }
        return protocolName;
    }

    @NotNull
    public String getServerPort() {
        return server;
    }

    @NotNull
    public String getFullPort() {
        return fullName;
    }

    @NotNull
    public String getDisplayName() {
        if (getServerProtocol().isSecure()) {
            return "ssl:" + getServerPort();
        }
        return getServerPort();
    }

    @NotNull
    public String getUrl() {
        // Trim the config port.  See bug #23
        return getServerProtocol() + PROTOCOL_SEP + getServerPort().trim();
    }

    public boolean isSecure() {
        return protocol.isSecure();
    }

    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || ! (obj.getClass().equals(P4ServerName.class))) {
            return false;
        }
        P4ServerName that = (P4ServerName) obj;
        return that.server.equals(server) && that.protocol.equals(protocol);
    }

    @Override
    public int hashCode() {
        return server.hashCode() + protocol.hashCode();
    }

    @NotNull
    private static IServerAddress.Protocol parseProtocol(@Nullable String protocol) {
        // Bug #109: Switch the default connections over to the Nts server impl.
        // Only if the user explicitly requests the old one do we use it.

        if (protocol == null) {
            return IServerAddress.Protocol.P4JRPCNTS;
        }
        protocol = protocol.toLowerCase().trim();
        if ("ssl".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCNTSSSL;
        }
        if ("tcp".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCNTS;
        }
        if ("javassl".equals(protocol) ||
                "javas".equals(protocol)) {
            // explicit request for the old ones
            return IServerAddress.Protocol.P4JAVASSL;
        }
        if ("java".equals(protocol)) {
            // explicit request for the old ones
            return IServerAddress.Protocol.P4JAVA;
        }
        if ("rpc".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPC;
        }
        if ("rpcs".equals(protocol) ||
                "rpcssl".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCSSL;
        }
        if ("nts".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCNTS;
        }
        if ("ntss".equals(protocol) ||
                "ntsssl".equals(protocol)) {
            return IServerAddress.Protocol.P4JRPCNTSSSL;
        }
        return IServerAddress.Protocol.P4JRPCNTS;
    }
}
