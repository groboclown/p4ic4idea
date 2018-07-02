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

package net.groboclown.p4.server.impl.connection.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ILogCallback;
import net.groboclown.p4.server.api.ApplicationPasswordRegistry;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.impl.connection.ConnectionManager;
import net.groboclown.p4.server.impl.connection.P4Func;
import net.groboclown.p4.server.impl.connection.P4RequestErrorHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.Callable;

public class SimpleConnectionManager implements ConnectionManager {
    private static final Logger LOG = Logger.getInstance(SimpleConnectionManager.class);
    private static final Logger P4LOG = Logger.getInstance("p4");

    private static final String PLUGIN_P4HOST_KEY = "P4HOST";
    private static final String PLUGIN_LANGUAGE_KEY = "P4LANGUAGE";

    private final File tmpDir;
    private final int socketSoTimeoutMillis;
    private final String pluginVersion;
    private final P4RequestErrorHandler errorHandler;

    public SimpleConnectionManager(File tmpDir, int socketSoTimeoutMillis, String pluginVersion,
            P4RequestErrorHandler errorHandler) {
        this.tmpDir = tmpDir;
        this.socketSoTimeoutMillis = socketSoTimeoutMillis;
        this.pluginVersion = pluginVersion;
        this.errorHandler = errorHandler;
    }


    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ClientConfig config, @Nonnull P4Func<IClient, R> fun) {
        if (config.getServerConfig().usesStoredPassword()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using password stored in registry.");
            }
            return Answer.forPromise(ApplicationPasswordRegistry.getInstance().get(config.getServerConfig()))
                    .mapAsync((password) -> handleAsync(config, () -> {
                        final IOptionsServer server = connect(
                                config.getServerConfig(),
                                password.toString(false),
                                createProperties(config));
                        try {
                            IClient client = server.getClient(config.getClientname());
                            if (client == null) {
                                throw new ConfigException("Client does not exist: " + config.getClientname());
                            }
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Connected to client " + client.getName());
                            }
                            return fun.func(client);
                        } finally {
                            close(server);
                        }
                    }));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Skipping password use.");
        }
        return handleAsync(config, () -> {
                    final IOptionsServer server = connect(
                            config.getServerConfig(),
                            null,
                            createProperties(config));
                    try {
                        IClient client = server.getClient(config.getClientname());
                        if (client == null) {
                            throw new ConfigException("Client does not exist: " + config.getClientname());
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Connected to client " + client.getName());
                        }
                        return fun.func(server.getClient(config.getClientname()));
                    } finally {
                        close(server);
                    }
                });
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ServerConfig config, @NotNull P4Func<IOptionsServer, R> fun) {
        if (config.usesStoredPassword()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using password stored in registry.");
            }
            return Answer.forPromise(ApplicationPasswordRegistry.getInstance().get(config))
                    .mapAsync((password) -> handleAsync(config, () -> {
                        final IOptionsServer server = connect(
                                config,
                                password.toString(false),
                                createProperties(config));
                        try {
                            return fun.func(server);
                        } finally {
                            close(server);
                        }
                    }));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Skipping password use.");
        }
        return handleAsync(config, () -> {
            final IOptionsServer server = connect(
                    config,
                    null,
                    createProperties(config));
            try {
                return fun.func(server);
            } finally {
                close(server);
            }
        });
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull P4ServerName config, P4Func<IOptionsServer, R> fun) {
        return handleAsync(config, () -> {
            final IOptionsServer server = getServer(
                    config,
                    createProperties());
            // FIXME need way to add SSL fingerprint; sso callback shouldn't be necessary for this operation.
            server.connect();
            try {
                return fun.func(server);
            } finally {
                close(server);
            }
        });
    }

    @Override
    public void disconnect(@NotNull P4ServerName config) {
        // Does nothing, because the underlying implementation does
        // not keep a pool of connections.
    }

    private IOptionsServer connect(ServerConfig serverConfig, String password, Properties props)
            throws P4JavaException, URISyntaxException {
        IOptionsServer server = getServer(serverConfig.getServerName(), props);
        if (serverConfig.getServerName().isSecure() && serverConfig.hasServerFingerprint()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Setting server fingerprint to " + serverConfig.getServerFingerprint());
            }
            server.addTrust(serverConfig.getServerFingerprint());
        }
        if (serverConfig.hasLoginSso() && serverConfig.getLoginSso() != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Setting up SSO login to execute `" + serverConfig.getLoginSso() + "`");
            }
            // FIXME pass in a error callback to the callback so that the connection manager can handle errors.
            // TODO look into registering the sso key through user options.
            server.registerSSOCallback(new LoginSsoCallbackHandler(serverConfig, 1000), null);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending connect request to server " + serverConfig.getServerName());
        }
        server.connect();

        // Seems to be connected.  Tell the world that it can be
        // connected to.  Note that this is independent of login validity.
        ServerConnectedMessage.send().serverConnected(serverConfig, false);

        // #147 if the user isn't logged in with an authentication ticket, but has P4LOGINSSO
        // set, then a simple password login attempt should be made.  The P4LOGINSSO will
        // ignore the password.
        // However, we will always perform the SSO login here.
        // FIXME with the addition of the LoginAction, we should only perform this when absolutely required.
        // The original server authentication terribleness is enshrined in v2's ServerAuthenticator.
        if (serverConfig.hasLoginSso() || (serverConfig.usesStoredPassword() && password != null)) {
            final boolean useTicket = serverConfig.getAuthTicket() != null && serverConfig.getAuthTicket().isFile()
                    && serverConfig.getAuthTicket().canWrite();
            final LoginOptions loginOptions = new LoginOptions();
            loginOptions.setDontWriteTicket(! useTicket);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Attempting to log into the server.  Auth Ticket: " +
                        (useTicket ? serverConfig.getAuthTicket() : "don't use") +
                        "; password? " + (password == null ? "(not used)" : "(set)")
                    );
            }
            server.login(password, loginOptions);
            ServerConnectedMessage.send().serverConnected(serverConfig, true);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No attempt made to authenticate with the server.");
            }
        }

        return server;
    }


    private IOptionsServer getServer(P4ServerName serverName, Properties props)
            throws ConnectionException, ConfigException, NoSuchObjectException, ResourceException, URISyntaxException {
        setupLogging();
        final UsageOptions options = new UsageOptions(props);
        // These are not set in the usage options via the properties, so we
        // need to manually configure them.
        // TODO alter the p4java API code so that these are properly loaded in the UsageOptions class.
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
        final String uri = serverName.getUrl();
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
                try {
                    StringWriter sw = new StringWriter();
                    options.getProps().store(sw, "Options Server Properties");
                    LOG.debug("Connection Properties:\n" + sw.toString());
                } catch (IOException e) {
                    LOG.debug(e);
                }
            }
        }
        return ServerFactory.getOptionsServer(uri, props, options);
    }


    private void setupLogging() {
        if (Log.getLogCallback() == null) {
            Log.setLogCallback(new ILogCallback() {
                // errors are pushed up to the normal logging mechanisms,
                // so no need to mark it as error here.

                @Override
                public void internalError(final String errorString) {
                    P4LOG.warn("p4java error: " + errorString);
                }

                @Override
                public void internalException(final Throwable thr) {
                    P4LOG.info("p4java error", thr);
                }

                @Override
                public void internalWarn(final String warnString) {
                    P4LOG.info("p4java warning: " + warnString);
                }

                @Override
                public void internalInfo(final String infoString) {
                    if (P4LOG.isDebugEnabled()) {
                        P4LOG.debug("p4java info: " + infoString);
                    }
                }

                @Override
                public void internalStats(final String statsString) {
                    if (P4LOG.isDebugEnabled()) {
                        P4LOG.debug("p4java stats: " + statsString);
                    }
                }

                @Override
                public void internalTrace(final LogTraceLevel traceLevel, final String traceMessage) {
                    if (P4LOG.isDebugEnabled()) {
                        P4LOG.debug("p4java trace: " + traceMessage);
                    }
                }

                @Override
                public LogTraceLevel getTraceLevel() {
                    return P4LOG.isDebugEnabled() ? LogTraceLevel.ALL : LogTraceLevel.FINE;
                }
            });
        }
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

    @NotNull
    private Properties createProperties(@NotNull ClientConfig clientConfig) {
        final Properties props = createProperties(clientConfig.getServerConfig());

        if (clientConfig.getDefaultCharSet() != null) {
            props.setProperty(PropertyDefs.DEFAULT_CHARSET_KEY, clientConfig.getDefaultCharSet());
        }
        if (clientConfig.getClientname() != null) {
            props.setProperty(PropertyDefs.CLIENT_NAME_KEY, clientConfig.getClientname());
        }

        // We handle the ignore file name within the plugin.
        if (clientConfig.getIgnoreFileName() != null) {
            props.setProperty(PropertyDefs.IGNORE_FILE_NAME_KEY, clientConfig.getIgnoreFileName());
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

        return props;
    }

    @NotNull
    private Properties createProperties(@NotNull ServerConfig serverConfig) {
        final Properties props = createProperties();

        // Should always be set.
        props.setProperty(PropertyDefs.USER_NAME_KEY, serverConfig.getUsername());

        if (serverConfig.hasAuthTicket() && serverConfig.getAuthTicket() != null) {
            props.setProperty(PropertyDefs.TICKET_PATH_KEY, serverConfig.getAuthTicket().getAbsolutePath());
        }
        // This doesn't actually do anything with the default connection that we use.
        // props.setProperty(PropertyDefs.PASSWORD_KEY, knownPassword);
        if (serverConfig.hasTrustTicket() && serverConfig.getTrustTicket() != null) {
            props.setProperty(PropertyDefs.TRUST_PATH_KEY, serverConfig.getTrustTicket().getAbsolutePath());
        }

        if (serverConfig.hasTrustTicket() && serverConfig.getTrustTicket() != null) {
            props.setProperty(PropertyDefs.TRUST_PATH_KEY,
                    serverConfig.getTrustTicket().getAbsolutePath());
        }

        return props;
    }

    @NotNull
    private Properties createProperties() {
        final Properties props = new Properties();

        props.setProperty(PropertyDefs.P4JAVA_TMP_DIR_KEY, tmpDir.getAbsolutePath());
        props.setProperty(PropertyDefs.PROG_NAME_KEY, "intellij-perforce-community-plugin-connection");
        // Find the version of our application, as it registered itself.
        props.setProperty(PropertyDefs.PROG_VERSION_KEY, pluginVersion);

        // Socket creation properties
        // See RpcSocketHelper
        // For Fixing #85.
        props.setProperty(RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_NICK,
                Integer.toString(socketSoTimeoutMillis));

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

    private <R> Answer<R> handleAsync(ClientConfig config, Callable<R> c) {
        return startPromise(() -> errorHandler.handle(config, c));
    }

    private <R> Answer<R> handleAsync(ServerConfig config, Callable<R> c) {
        return startPromise(() -> errorHandler.handle(config, c));
    }

    private <R> Answer<R> handleAsync(P4ServerName config, Callable<R> c) {
        return startPromise(() -> errorHandler.handle(config, c));
    }

    private interface ExecThrows<R> {
        R exec() throws P4CommandRunner.ServerResultException;
    }

    private <R> Answer<R> startPromise(ExecThrows<R> runner) {
        return Answer.background((sink) -> {
            try {
                R res = runner.exec();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Completed execution.");
                }
                sink.resolve(res);
            } catch (P4CommandRunner.ServerResultException e) {
                LOG.info("Command execution failed", e);
                sink.reject(e);
            }
        });
    }



    private void close(@NotNull final IServer server) {
        try {
            server.disconnect();
        } catch (ConnectionException e) {
            errorHandler.handleOnDisconnectError(e);
        } catch (AccessException e) {
            errorHandler.handleOnDisconnectError(e);
        }
    }
}
