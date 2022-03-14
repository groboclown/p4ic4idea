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

import com.intellij.credentialStore.OneTimeString;
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
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.ILogCallback;
import net.groboclown.p4.server.api.ApplicationPasswordRegistry;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.exceptions.NotOnServerException;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.impl.commands.AnswerUtil;
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
    private static final Logger P4LOG = Logger.getInstance("p4.api");
    private static final Logger P4CMDLOG = Logger.getInstance("p4");
    private static final Logger P4STATSLOG = Logger.getInstance("p4.stats");

    private static final String PLUGIN_P4HOST_KEY = "P4HOST";
    private static final String PLUGIN_LANGUAGE_KEY = "P4LANGUAGE";

    private static final char[] EMPTY_PASSWORD = new char[0];

    private final File tmpDir;
    private int socketSoTimeoutMillis;
    private final String pluginVersion;
    private final P4RequestErrorHandler errorHandler;

    public SimpleConnectionManager(File tmpDir, int socketSoTimeoutMillis, String pluginVersion,
            P4RequestErrorHandler errorHandler) {
        this.tmpDir = tmpDir;
        this.socketSoTimeoutMillis = socketSoTimeoutMillis;
        this.pluginVersion = pluginVersion;
        this.errorHandler = errorHandler;
    }


    public void setSocketSoTimeoutMillis(int socketSoTimeoutMillis) {
        this.socketSoTimeoutMillis = socketSoTimeoutMillis;
        // If this ever caches the connection, then the connection should be closed here.
    }


    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ClientConfig config, @Nonnull P4Func<IClient, R> fun) {
        return withConnection(config, null, fun);
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ClientConfig config, @Nullable File cwd, @Nonnull P4Func<IClient, R> fun) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Performing client execution from cwd " + cwd);
        }

        return getPassword(config.getServerConfig())
                .mapAsync((password) -> handleAsync(config, () -> {
                    String passwdStr = password == null ? null : password.toString(true);
                    if (passwdStr == null || passwdStr.isEmpty()) {
                        passwdStr = null;
                    }
                    final IOptionsServer server = connect(
                            new OptionalClientServerConfig(config),
                            passwdStr,
                            createProperties(config, cwd));
                    try {
                        IClient client = server.getClient(config.getClientname());
                        if (client == null) {
                            throw new NotOnServerException("client", config.getClientname());
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Connected to client " + client.getName());
                        }
                        server.setCurrentClient(client);
                        return fun.func(client);
                    } finally {
                        close(server);
                    }
                }));
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(
            @NotNull OptionalClientServerConfig config,
            @NotNull P4Func<IOptionsServer, R> fun) {
        return getPassword(config.getServerConfig())
                .mapAsync((password) -> handleAsync(config.getServerConfig(), () -> {
                    String passwdStr = password == null ? null : password.toString(true);
                    if (passwdStr == null || passwdStr.isEmpty()) {
                        passwdStr = null;
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Connecting to " + config.getServerName());
                    }
                    final Properties props;
                    if (config.getClientConfig() != null) {
                        props = createProperties(config.getClientConfig(), null);
                    } else {
                        props = createProperties(config.getServerConfig());
                    }
                    final IOptionsServer server = connect(config, passwdStr, props);
                    try {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Running invocation for " + fun);
                        }
                        return fun.func(server);
                    } finally {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Closing connection to server " + config.getServerName());
                        }
                        close(server);
                    }
                }));
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

    private IOptionsServer connect(OptionalClientServerConfig config, String password, Properties props)
            throws P4JavaException, URISyntaxException {
        ServerConfig serverConfig = config.getServerConfig();
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
            // pass in a error callback to the callback so that the connection manager can handle errors.
            // TODO look into registering the sso key through user options.
            // TODO don't hard-code the timeout; use something else.
            server.registerSSOCallback(new LoginSsoCallbackHandler(config, 10_000), null);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending connect request to server " + serverConfig.getServerName());
        }
        server.connect();

        // Seems to be connected.  Tell the world that it can be
        // connected to.  Note that this is independent of login validity.
        ServerConnectedMessage.send().serverConnected(
                new ServerConnectedMessage.ServerConnectedEvent(config, false));

        // #147 if the user isn't logged in with an authentication ticket, but has P4LOGINSSO
        // set, then a simple password login attempt should be made.  The P4LOGINSSO will
        // ignore the password.
        // However, we will always perform the SSO login here.
        // With the addition of the LoginAction, we should only perform this when absolutely required.
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
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No attempt made to authenticate with the server.");
            }
        }
        // TODO should this always be sent???
        ServerConnectedMessage.send().serverConnected(
                new ServerConnectedMessage.ServerConnectedEvent(config, true));

        return server;
    }


    private IOptionsServer getServer(P4ServerName serverName, Properties props)
            throws ConnectionException, ConfigException, NoSuchObjectException, ResourceException, URISyntaxException {
        setupTempDir();
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
        IOptionsServer ret = ServerFactory.getOptionsServer(uri, props, options);
        setupLogging(ret);
        return ret;
    }


    private void setupLogging(@NotNull IOptionsServer server) {
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
                        P4STATSLOG.debug("p4java stats: " + statsString);
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

        server.registerCallback(new ICommandCallback() {
            @Override
            public void issuingServerCommand(int key, String commandString) {
                P4CMDLOG.info("cmd (" + key + "): p4 " + commandString);
            }

            @Override
            public void completedServerCommand(int key, long millisecsTaken) {
                if (P4CMDLOG.isDebugEnabled()) {
                    P4CMDLOG.debug("Command " + key + " ran in " + millisecsTaken + " milliseconds");
                }
            }

            @Override
            public void receivedServerInfoLine(int key, IServerMessage infoLine) {
                if (P4CMDLOG.isDebugEnabled()) {
                    P4CMDLOG.debug("cmd (" + key + "): INFO " + infoLine.getAllInfoStrings("\n"));
                }
            }

            @Override
            public void receivedServerErrorLine(int key, IServerMessage errorLine) {
                P4CMDLOG.info("cmd (" + key + "): ERROR " + errorLine.getAllInfoStrings("\n"));
            }

            @Override
            public void receivedServerMessage(int key, IServerMessage message) {

            }
        });
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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Default client hostname includes domain: [" + hostname + "]");
                    }
                    hostname = hostname.substring(0, pos);
                }
                hostname = hostname.trim();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using client hostname [" + hostname + "]");
            }

            return hostname;
        } catch (Exception e) {
            LOG.error("Could not find default client hostname; try using 'P4HOST' setting.", e);

            // This exception will be thrown again, deep down in the P4Java API.
            // for now, don't report it other than the error message.
            return null;
        }
    }

    private void setupTempDir() {
        // Before invoking the Perforce command, ensure that the temporary directory exists.
        // Windows has been known to delete the directory on its own from time to time.  See #172.
        if (!tmpDir.exists()) {
            if (!tmpDir.mkdirs()) {
                LOG.warn("Could not create temporary directory (" + tmpDir + ").  Some operations may fail.");
                // TODO send a message, but it should be project-specific, but this object doesn't have a project
                // reference.  Application message?  But that just seems wrong.
            }
        }
    }

    @NotNull
    private Properties createProperties(@NotNull ClientConfig clientConfig, @Nullable File cwd) {
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

        // Strange as it may sound, but for fstat and other file-based commands to work right, we need to set the
        // current working directory.
        if (cwd != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Setting working directory for command to " + cwd.getAbsolutePath());
            }
            props.setProperty(UsageOptions.WORKING_DIRECTORY_PROPNAME, cwd.getAbsolutePath());
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("No working directory set for connection.");
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
            } catch (Throwable t) {
                LOG.error("Command generated unexpected problem; making it look like an interrupted error", t);
                sink.reject(AnswerUtil.createFor(new InterruptedException()));
            }
        });
    }

    private Answer<OneTimeString> getPassword(@NotNull final ServerConfig serverConfig) {
        if (serverConfig.usesStoredPassword()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using password stored in registry.");
            }
            // Custom promise handler.  We want all password responses, whether a failure or a success,
            // to resolve.  Failures shouldn't be bubbled up from this call.
            return Answer.resolve(null)
                    .futureMap((x, sink) ->
                        ApplicationPasswordRegistry.getInstance().get(serverConfig)
                            .onProcessed(sink::resolve)
                            .onError((t) -> {
                                LOG.warn("Problem loading the password", t);
                                sink.resolve(new OneTimeString(EMPTY_PASSWORD));
                            }));
        }
        return Answer.resolve(new OneTimeString(EMPTY_PASSWORD));
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
