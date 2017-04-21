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

package net.groboclown.idea.p4ic.server;

import com.intellij.openapi.diagnostic.Logger;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.v2.extension.P4PluginVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.Properties;

public class P4OptionsServerConnectionFactory {
    private static final Logger LOG = Logger.getInstance(P4OptionsServerConnectionFactory.class);

    private static final P4OptionsServerConnectionFactory INSTANCE = new P4OptionsServerConnectionFactory();

    private static final String PLUGIN_VERSION = P4PluginVersion.getPluginVersion();
    private static final String PLUGIN_P4HOST_KEY = "P4HOST";
    private static final String PLUGIN_LANGUAGE_KEY = "P4LANG";

    public static P4OptionsServerConnectionFactory getInstance() {
        return INSTANCE;
    }

    private P4OptionsServerConnectionFactory() {
        // do nothing
    }

    @NotNull
    public IOptionsServer createConnection(@NotNull ClientConfig clientConfig, @NotNull File tempDir)
            throws P4JavaException, URISyntaxException {
        Properties props = createProperties(clientConfig, tempDir);
        final UsageOptions options = new UsageOptions(props);

        // These are not set in the usage options via the properties, so we
        // need to manually configure them.
        // TODO alter the p4java API code so that it is properly loaded
        // in the UsageOptions class.
        {
            // see bug #61
            // Hostname as used by the Java code:
            //   Mac clients can incorrectly set the hostname.
            //   The underlying code will use:
            //      InetAddress.getLocalHost().getHostName()
            //   or from the UsageOptions passed into the
            //   server configuration `init` method.
            String hostname = props.getProperty(PLUGIN_P4HOST_KEY, null);
            if (hostname == null || hostname.trim().length() <= 0) {
                hostname = getDefaultHostname();
            } else {
                hostname = hostname.trim();
            }
            if (hostname != null && hostname.length() > 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Using hostname [" + hostname + "]");
                }
                options.setHostName(hostname);
            }

            // Note: this one will only be added once we get the P4Java API changed
            // such that it has the error code & parameters in the exceptions.
            String language = props.getProperty(PLUGIN_LANGUAGE_KEY, null);
            if (language != null) {
                options.setTextLanguage(language);
            }
        }

        final String uri = getServerUri(clientConfig.getServerConfig());
        {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Connecting to server [" + uri + "]");
                LOG.debug(String.format("Usage options:\n"
                                + "  Host Name: %s\n"
                                + "  Program Name: %s\n"
                                + "  Program Version: %s\n"
                                + "  Text Language: %s\n"
                                + "  Unset Client Name: %s\n"
                                + "  Unset User Name: %s\n"
                                + "  Working Directory: %s",
                        options.getHostName(),
                        options.getProgramName(),
                        options.getProgramVersion(),
                        options.getTextLanguage(),
                        options.getUnsetClientName(),
                        options.getUnsetUserName(),
                        options.getWorkingDirectory()));
            }
            try {
                StringWriter sw = new StringWriter();
                options.getProps().store(sw, "Options Server Properties");
            } catch (IOException e) {
                // Ignore
            }
        }
        final IOptionsServer server = ServerFactory.getOptionsServer(uri, props, options);
        if (clientConfig.getServerConfig().getServerName().isSecure()
                && clientConfig.getServerConfig().hasServerFingerprint()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Setting server fingerprint to " + clientConfig.getServerConfig().getServerFingerprint());
            }
            server.addTrust(clientConfig.getServerConfig().getServerFingerprint());
        }
        return server;
    }

    @NotNull
    private String getServerUri(@NotNull ServerConfig config) {
        // Trim the config port.  See bug #23
        return config.getServerName().getUrl();
    }

    @NotNull
    private Properties createProperties(@NotNull ClientConfig clientConfig, @NotNull File tempDir) {
        final Properties props = new Properties();

        final ServerConfig serverConfig = clientConfig.getServerConfig();

        // Should always be set.
        props.setProperty(PropertyDefs.USER_NAME_KEY, serverConfig.getUsername());

        if (clientConfig.getDefaultCharSet() != null) {
            props.setProperty(PropertyDefs.DEFAULT_CHARSET_KEY, clientConfig.getDefaultCharSet());
        }
        if (clientConfig.getClientName() != null) {
            props.setProperty(PropertyDefs.CLIENT_NAME_KEY, clientConfig.getClientName());
        }

        if (serverConfig.hasAuthTicket() && serverConfig.getAuthTicket() != null) {
            props.setProperty(PropertyDefs.TICKET_PATH_KEY, serverConfig.getAuthTicket().getAbsolutePath());
        }
        // if (knownPassword != null) {
        //     // This doesn't actually do anything with the default connection that we use.
        //     props.setProperty(PropertyDefs.PASSWORD_KEY, knownPassword);
        // }
        if (serverConfig.hasTrustTicket() && serverConfig.getTrustTicket() != null) {
            props.setProperty(PropertyDefs.TRUST_PATH_KEY, serverConfig.getTrustTicket().getAbsolutePath());
        }

        props.setProperty(PropertyDefs.P4JAVA_TMP_DIR_KEY, tempDir.getAbsolutePath());
        props.setProperty(PropertyDefs.PROG_NAME_KEY, "intellij-perforce-community-plugin-connection");
        // Find the version of our application, as it registered itself.
        props.setProperty(PropertyDefs.PROG_VERSION_KEY, PLUGIN_VERSION);

        // We handle the ignore file name within the plugin.
        if (clientConfig.getIgnoreFileName() != null) {
            props.setProperty(PropertyDefs.IGNORE_FILE_NAME_KEY, clientConfig.getIgnoreFileName());
        }

        // Socket creation properties
        // See RpcSocketHelper
        // For Fixing #85.
        props.setProperty(RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_NICK,
                Integer.toString(UserProjectPreferences.getSocketSoTimeoutMillis(clientConfig.getProject())));

        // Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
        // "true" or "false"; default is "true"
        // RpcPropertyDefs.RPC_SOCKET_TCP_NO_DELAY_NICK

        // Defaults to true.  Only disabled if it starts with 'N' or 'n'
        // RpcPropertyDefs.RPC_SOCKET_USE_KEEPALIVE_NICK

        // Setting the socket performance preferences, described by three
        // integers whose values indicate the relative importance of short
        // connection time, low latency, and high bandwidth.
        // Socket.setPerformancePreferences(int connectionTime, int latency, int bandwidth)
        // The default values is (1, 2, 0), assume no one changes them.
        // This gives the highest importance to low latency, followed by
        // short connection time, and least importance to high bandwidth.
        // RpcPropertyDefs.RPC_SOCKET_PERFORMANCE_PREFERENCES_NICK

        //props.setProperty(PropertyDefs.ENABLE_PROGRESS, "1");

        // This is the -ZTrack option, which spits out a bunch of
        // table lock commands:
        // http://answers.perforce.com/articles/KB/3090/?l=en_US&fs=RelatedArticle
        // It causes some commands, like getFileContents,
        // to spew out a bunch of stuff that we don't want.
        // See issue #12.
        //props.setProperty(PropertyDefs.ENABLE_TRACKING, "1");

        props.setProperty(PropertyDefs.WRITE_IN_PLACE_KEY, "1");

        //props.setProperty(PropertyDefs.AUTO_CONNECT_KEY, "0");
        //props.setProperty(PropertyDefs.AUTO_LOGIN_KEY, "0");
        //props.setProperty(PropertyDefs.ENABLE_PROGRESS, "0");
        //props.setProperty(PropertyDefs.ENABLE_TRACKING, "0");
        //props.setProperty(PropertyDefs.NON_CHECKED_SYNC, "0");

        if (serverConfig.hasTrustTicket() && serverConfig.getTrustTicket() != null) {
            props.setProperty(PropertyDefs.TRUST_PATH_KEY,
                    serverConfig.getTrustTicket().getAbsolutePath());
        }

        // see bug #61
        // Hostname as used by the Java code:
        //   Mac clients can incorrectly set the hostname.
        //   The underlying code will use:
        //      InetAddress.getLocalHost().getHostName()
        //   or from the UsageOptions passed into the
        //   server configuration `init` method.
        if (clientConfig.getClientHostName() != null) {
            props.setProperty(PLUGIN_P4HOST_KEY, clientConfig.getClientHostName());
        }

        // We will explicitly log in when ready.
        // This key is set to null to mean false.  Note that any other
        // value means true (even the text "false").
        props.remove(PropertyDefs.AUTO_LOGIN_KEY);

        // Turning this flag on means that the server connection
        // will attempt to get the client object without having
        // logged in first, so we want it turned off.
        // This key is set to null to mean false.  Note that any other
        // value means true (even the text "false").
        props.remove(PropertyDefs.AUTO_CONNECT_KEY);

        // For tracking purposes
        // properties.setProperty(PropertyDefs.PROG_NAME_KEY,
        //        properties.getProperty(PropertyDefs.PROG_NAME_KEY) + " connection " +
        //        serverInstance);

        return props;
    }


    // See bug #61 - some JVM implementations add the domain to the
    // hostname, which causes a conflict between the sent host and
    // the clientspec host.
    @Nullable
    private String getDefaultHostname() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            if (hostname != null) {
                int pos = hostname.indexOf('.');
                if (pos >= 0) {
                    LOG.debug("Default client hostname includes domain: [" + hostname + "]");
                    hostname = hostname.substring(0, pos);
                }
                hostname = hostname.trim();
            }
            LOG.debug("Using client hostname [" + hostname + "]");

            return hostname;
        } catch (Exception e) {
            LOG.error("Could not find default client hostname; try using 'P4HOST' setting.", e);

            // This exception will be thrown again, deep down in the P4Java API.
            // for now, don't report it other than the error message.
            return null;
        }
    }
}
