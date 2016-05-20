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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.connection.AuthTicketConnectionHandler;
import net.groboclown.idea.p4ic.server.connection.ClientPasswordConnectionHandler;
import net.groboclown.idea.p4ic.server.connection.EnvConnectionHandler;
import net.groboclown.idea.p4ic.server.connection.TestConnectionHandler;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.extension.P4PluginVersion;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

public abstract class ConnectionHandler {
    private static final Logger LOG = Logger.getInstance(ConnectionHandler.class);

    public static final String PLUGIN_P4HOST_KEY = "P4HOST";
    public static final String PLUGIN_LANGUAGE_KEY = "P4LANG";

    private static final String PLUGIN_VERSION = P4PluginVersion.getPluginVersion();


    @NotNull
    public static ConnectionHandler getHandlerFor(@NotNull ServerConfig config) {
        return getHandlerFor(config.getConnectionMethod());
    }

    @NotNull
    public static ConnectionHandler getHandlerFor(@NotNull P4Config.ConnectionMethod method) {
        switch (method) {
            case AUTH_TICKET:
                return AuthTicketConnectionHandler.INSTANCE;
            case CLIENT:
                return ClientPasswordConnectionHandler.INSTANCE;
            case P4CONFIG:
            case DEFAULT:
                return EnvConnectionHandler.INSTANCE;
            case UNIT_TEST_MULTIPLE:
            case UNIT_TEST_SINGLE:
                return TestConnectionHandler.INSTANCE;
            default:
                throw new IllegalStateException(
                        "Unknown connection method: " + method);
        }
    }


    public abstract Properties getConnectionProperties(@NotNull ServerConfig config, @Nullable String clientName);


    public String createUrl(@NotNull ServerConfig config) {
        // Trim the config port.  See bug #23
        return config.getProtocol().toString() + "://" + config.getPort().trim();
    }


    @NotNull
    public IOptionsServer getOptionsServer(@NotNull String serverUriString,
            @NotNull Properties props,
            final ServerConfig config) throws URISyntaxException, P4JavaException {
        final UsageOptions options = new UsageOptions(props);

        // These are not set in the usage options via the properties, so we
        // need to manually configure them.
        // TODO alter the p4java API code so that it is properly loaded
        // in the UsageOptions class.
        {
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

        final IOptionsServer server = ServerFactory.getOptionsServer(serverUriString, props, options);
        if (config.getProtocol().isSecure() && config.hasServerFingerprint()) {
            server.addTrust(config.getServerFingerprint());
        }
        return server;
    }



    /**
     * Always called on a new server connection.
     *
     * @param server server connection
     * @param config configuration
     * @throws P4JavaException
     */
    public abstract void defaultAuthentication(@Nullable Project project, @NotNull IOptionsServer server, @NotNull ServerConfig config) throws P4JavaException;

    /**
     * Called when the server challenges us for authentication.
     *
     * @param server server connection
     * @param config configuration
     * @return false if authentication could not be done, or true if it was attempted.
     * @throws P4JavaException
     */
    public abstract boolean forcedAuthentication(@Nullable Project project, @NotNull IOptionsServer server,
            @NotNull ServerConfig config, @NotNull AlertManager alerts) throws P4JavaException;


    /**
     * server configs are required to have all values set in a pretty
     * solid valid config.
     */
    @NotNull
    public abstract List<ConfigurationProblem> getConfigProblems(@NotNull ServerConfig config);


    /**
     * Perform the correct validation of the configuration, which includes correctly sending
     * events to the message bus if there is a problem.
     *
     * @param project project
     * @param config config to check
     * @throws P4InvalidConfigException
     */
    public void validateConfiguration(@Nullable Project project, @NotNull ServerConfig config)
            throws P4InvalidConfigException {
        final List<ConfigurationProblem> problems = getConfigProblems(config);
        if (! problems.isEmpty()) {
            P4InvalidConfigException ex = new P4InvalidConfigException(config, problems);
            if (project != null) {
                ManualP4Config badConfig = new ManualP4Config(config, null);
                Events.configInvalid(project, badConfig, ex);
            }
            throw ex;
        }
    }


    protected Properties initializeConnectionProperties(@NotNull ServerConfig config) {
        Properties ret = new Properties();
        ret.setProperty(PropertyDefs.PROG_NAME_KEY, "intellij-perforce-community-plugin-connection");
        // Find the version of our application, as it registered itself.
        ApplicationManager.getApplication().getComponent("PerforceIC");
        ret.setProperty(PropertyDefs.PROG_VERSION_KEY, PLUGIN_VERSION);

        if (config.getIgnoreFileName() != null) {
            ret.setProperty(PropertyDefs.IGNORE_FILE_NAME_KEY, config.getIgnoreFileName());
        }

        //ret.setProperty(PropertyDefs.ENABLE_PROGRESS, "1");

        // This is the -ZTrack option, which spits out a bunch of
        // table lock commands:
        // http://answers.perforce.com/articles/KB/3090/?l=en_US&fs=RelatedArticle
        // It causes some commands, like getFileContents,
        // to spew out a bunch of stuff that we don't want.
        // See issue #12.
        //ret.setProperty(PropertyDefs.ENABLE_TRACKING, "1");

        ret.setProperty(PropertyDefs.WRITE_IN_PLACE_KEY, "1");

        //ret.setProperty(PropertyDefs.AUTO_CONNECT_KEY, "0");
        //ret.setProperty(PropertyDefs.AUTO_LOGIN_KEY, "0");
        //ret.setProperty(PropertyDefs.ENABLE_PROGRESS, "0");
        //ret.setProperty(PropertyDefs.ENABLE_TRACKING, "0");
        //ret.setProperty(PropertyDefs.NON_CHECKED_SYNC, "0");

        if (config.hasTrustTicket() && config.getTrustTicket() != null) {
            ret.setProperty(PropertyDefs.TRUST_PATH_KEY, config.getTrustTicket().getAbsolutePath());
        }

        if (config.getClientHostname() != null) {
            ret.setProperty(PLUGIN_P4HOST_KEY, config.getClientHostname());
        }

        return ret;
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
                    LOG.info("Default client hostname includes domain: [" + hostname + "]");
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
