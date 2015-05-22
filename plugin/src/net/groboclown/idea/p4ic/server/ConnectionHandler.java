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

import com.intellij.openapi.project.Project;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.*;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigListener;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.connection.AuthTicketConnectionHandler;
import net.groboclown.idea.p4ic.server.connection.ClientPasswordConnectionHandler;
import net.groboclown.idea.p4ic.server.connection.EnvConnectionHandler;
import net.groboclown.idea.p4ic.server.connection.TestConnectionHandler;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

public abstract class ConnectionHandler {
    public static ConnectionHandler getHandlerFor(@NotNull ServerStatus status) {
        return getHandlerFor(status.getConfig());
    }

    public static ConnectionHandler getHandlerFor(@NotNull ServerConfig config) {
        switch (config.getConnectionMethod()) {
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
                        "Unknown connection method: " + config.getConnectionMethod());
        }
    }


    public abstract Properties getConnectionProperties(@NotNull ServerConfig config, @Nullable String clientName);


    public static String createUrlFor(@NotNull ServerConfig config) {
        return getHandlerFor(config).createUrl(config);
    }


    public String createUrl(@NotNull ServerConfig config) {
        // Trim the config port.  See bug #23
        return config.getProtocol().toString() + "://" + config.getPort().trim();
    }


    @NotNull
    public IOptionsServer getOptionsServer(@NotNull String serverUriString, @NotNull Properties props) throws URISyntaxException, ConnectionException, NoSuchObjectException, ConfigException, ResourceException {
        return ServerFactory.getOptionsServer(serverUriString, props);
    }


    /**
     * Always called on a new server connection.
     *
     * @param server server connection
     * @param config configuration
     * @param password
     * @throws P4JavaException
     */
    public abstract void defaultAuthentication(@NotNull IOptionsServer server, @NotNull ServerConfig config, char[] password) throws P4JavaException;

    /**
     * Called when the server challenges us for authentication.
     *
     * @param server server connection
     * @param config configuration
     * @param password new password
     * @return false if authentication could not be done, or true if it was attempted.
     * @throws P4JavaException
     */
    public abstract boolean forcedAuthentication(@NotNull IOptionsServer server, @NotNull ServerConfig config, char[] password) throws P4JavaException;


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
                project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project,
                        new ManualP4Config(config, null), ex);
            }
            throw ex;
        }
    }


    protected Properties initializeConnectionProperties(@NotNull ServerConfig config) {
        Properties ret = new Properties();

        ret.setProperty(PropertyDefs.PROG_NAME_KEY, "IntelliJ Perforce Community Plugin");
        ret.setProperty(PropertyDefs.PROG_VERSION_KEY, "1");

        ret.setProperty(PropertyDefs.IGNORE_FILE_NAME_KEY, P4Config.P4_IGNORE_FILE);

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

        if (config.hasTrustTicket()) {
            ret.setProperty(PropertyDefs.TRUST_PATH_KEY, config.getTrustTicket().getAbsolutePath());
        }

        return ret;
    }

}
