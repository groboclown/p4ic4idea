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

import com.perforce.p4java.env.PerforceEnvironment;
import com.perforce.p4java.server.IServerAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated should instead be using the new config API.
 */
public class P4ConfigUtil {
    /**
     * @deprecated use {@link P4ServerName} instead.
     */
    public static final String PROTOCOL_SEP = "://";


    /**
     *
     * @param protocol
     * @param simplePort
     * @return
     * @deprecated use {@link P4ServerName} instead.
     */
    @Nullable
    public static String toFullPort(@Nullable IServerAddress.Protocol protocol, @Nullable String simplePort) {
        if (protocol == null && simplePort == null) {
            return null;
        }
        String ret = "";
        if (simplePort != null) {
            ret = simplePort;
        }
        if (protocol != null) {
            // Bug #109: Switch the default connections over to the Nts server impl.
            // Only if the user explicitly requests the old one do we use it.
            switch (protocol) {
                case P4JRPC:
                    ret = "rpc" + PROTOCOL_SEP + ret;
                    break;
                case P4JRPCSSL:
                    ret = "rpcssl" + PROTOCOL_SEP + ret;
                    break;
                case P4JAVASSL:
                    ret = "javassl" + PROTOCOL_SEP + ret;
                    break;
                case P4JAVA:
                    ret = "java" + PROTOCOL_SEP + ret;
                case P4JRPCNTSSSL:
                    ret = "ssl" + PROTOCOL_SEP + ret;
                    break;
                case P4JRPCNTS:
                default:
                    // do nothing - it's the default
                    break;
            }
        }
        return ret;
    }

    /**
     *
     * @param port
     * @return
     * @deprecated use {@link P4ServerName} instead.
     */
    @Nullable
    public static String getSimplePortFromPort(String port) {
        return portSplit(port)[1];
    }


    /**
     *
     * @param port
     * @return
     * @deprecated use {@link P4ServerName} instead.
     */
    @Nullable
    public static IServerAddress.Protocol getProtocolFromPort(String port) {
        String protocol = portSplit(port)[0];

        // Bug #109: Switch the default connections over to the Nts server impl.
        // Only if the user explicitly requests the old one do we use it.

        if (protocol == null) {
            return IServerAddress.Protocol.P4JRPCNTS;
        }
        protocol = protocol.toLowerCase();
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
        return null;
        //return IServerAddress.P4ServerName.P4JAVA;
    }


    /**
     *
     * @param current
     * @param setting
     * @return
     * @deprecated use {@link P4ServerName} instead.
     */
    public static boolean isPortModified(@NotNull P4Config current, @Nullable String setting) {
        String port = getSimplePortFromPort(setting);
        IServerAddress.Protocol protocol = getProtocolFromPort(setting);
        if (current.getProtocol() != protocol) {
            return true;
        }
        if (current.getPort() == null) {
            return port != null;
        } else {
            return current.getPort().equals(port);
        }
    }


    /**
     *
     * @param port
     * @return
     * @deprecated use {@link P4ServerName} instead.
     */
    @Deprecated
    private static String[] portSplit(@Nullable String port) {
        String[] ret = new String[] { null, port };
        if (port != null) {
            int splitter = port.indexOf(PROTOCOL_SEP);
            if (splitter >= port.length() - 1) {
                // ':' is on the last character, which is invalid
                // set the value to an invalid setting, but not null
                // to avoid an NPE.
                ret[1] = ":";
            } else if (splitter >= 0) {
                ret[0] = port.substring(0, splitter);
                ret[1] = port.substring(splitter + PROTOCOL_SEP.length());
            } else {
                // based on http://www.perforce.com/perforce/r14.1/manuals/p4guide/chapter.configuration.html
                // format can be "port", "hostname:port", "ssl:hostname:port", "tcp:hostname:port"
                splitter = port.indexOf(':');
                if (splitter > 0) {
                    int splitter2 = port.indexOf(':', splitter + 1);
                    if (splitter2 > 0) {
                        ret[0] = port.substring(0, splitter);
                        ret[1] = port.substring(splitter + 1);
                    }
                }
            }

            if (ret[1].indexOf(':') < 0) {
                // This is the form "port", which is not supported by the
                // P4 java api.  So we must prepend a localhost to conform
                // to what P4 java supports.
                ret[1] = "localhost:" + ret[1];
            }
        }
        return ret;
    }


    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     * @return the default ticket file, which is OS dependent.
     */
    @NotNull
    public static File getDefaultTrustTicketFile() {
        // Windows check
        String userprofile = System.getenv("USERPROFILE");
        if (userprofile != null) {
            return new File(userprofile +
                File.separator + "p4trust.txt");
        }
        return new File(System.getenv("HOME") + File.separator + ".p4trust");
    }


    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     *
     * @return the default ticket file, which is OS dependent.
     */
    @NotNull
    public static File getDefaultTicketFile() {
        // Windows check
        String userprofile = System.getenv("USERPROFILE");
        if (userprofile != null) {
            return new File(userprofile +
                    File.separator + "p4tickets.txt");
        }
        return new File(System.getenv("HOME") + File.separator + ".p4tickets");
    }


    @NotNull
    public static Map<String, String> getProperties(@Nullable P4Config config) {
        if (config == null) {
            return Collections.emptyMap();
        }
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(PerforceEnvironment.P4CLIENT, config.getClientname());
        ret.put(PerforceEnvironment.P4CONFIG, config.getConfigFile());
        ret.put(PerforceEnvironment.P4PASSWD,
                config.getPassword() == null ? "<no password>" : "<password provided>");
        ret.put(PerforceEnvironment.P4PORT, toFullPort(config.getProtocol(), config.getPort()));
        ret.put(PerforceEnvironment.P4TRUST, config.getTrustTicketPath());
        ret.put(PerforceEnvironment.P4USER, config.getUsername());
        ret.put(PerforceEnvironment.P4TICKETS, config.getAuthTicketPath());

        // Even though this is kind of a back-door item (it can only be set through
        // environment or p4config files), it should still be
        // present so that people can kind of discover it and realize that it's
        // supported without looking at the documentation.
        ret.put(PerforceEnvironment.P4HOST, config.getClientHostname());

        return ret;
    }

}
