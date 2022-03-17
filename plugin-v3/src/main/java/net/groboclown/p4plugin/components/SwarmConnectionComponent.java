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

package net.groboclown.p4plugin.components;

import com.intellij.credentialStore.OneTimeString;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import net.groboclown.p4.server.api.ApplicationPasswordRegistry;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.commands.server.SwarmConfigQuery;
import net.groboclown.p4.server.api.commands.server.SwarmConfigResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.simpleswarm.SwarmLogger;
import org.jetbrains.annotations.NotNull;

/**
 * A component to manage connections to the swarm server.  Eventually, this might cache connection settings.
 */
public class SwarmConnectionComponent
        implements ProjectComponent, Disposable {
    private static final Logger LOG = Logger.getInstance(SwarmConnectionComponent.class);

    private final Project project;

    @NotNull
    public static SwarmConnectionComponent getInstance(@NotNull Project project) {
        return project.getComponent(SwarmConnectionComponent.class);
    }

    public SwarmConnectionComponent(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public P4CommandRunner.QueryAnswer<SwarmConfigResult> getSwarmClientFor(@NotNull final ClientConfig clientConfig) {
        SwarmConfigQuery query = new SwarmConfigQuery(
                // TODO This is EXTREMELY experimental code.
                // The user may need to use SSO to authenticate to get a ticket, or use an already established
                // ticket on the server.  Just need some use cases to discover the real usage.
                // Right now, this just uses passwords.
                serverConfig -> Answer.resolve(
                        new SwarmConfigQuery.AuthorizationOption(
                                () -> Answer.resolve(null)
                                        .futureMap((o, sink) ->
                                                ApplicationPasswordRegistry.getInstance().getOrAskFor(null, serverConfig)
                                                        .onProcessed(sink::resolve)
                                                        .onError((t) -> {
                                                            LOG.warn("Problem loading the password", t);
                                                            sink.resolve(new OneTimeString(new char[0]));
                                                        })),
                                () -> ApplicationPasswordRegistry.getInstance().remove(serverConfig)
                        )
                ),
                SWARM_LOGGER
        );
        return P4ServerComponent.query(project, new OptionalClientServerConfig(clientConfig), query);
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "SwarmConnectionComponent";
    }

    @Override
    public void initComponent() {
        // Do nothing
    }

    @Override
    public void disposeComponent() {
        Disposer.dispose(this);
    }

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public void projectOpened() {
        // do nothing
    }

    @Override
    public void projectClosed() {
        // do nothing
    }

    private static class SwarmLoggerImpl implements SwarmLogger {
        @Override
        public boolean isDebugEnabled() {
            return LOG.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            LOG.debug(msg);
        }

        @Override
        public void debug(Throwable e) {
            LOG.debug(e);
        }

        @Override
        public void debug(String msg, Throwable e) {
            LOG.debug(msg, e);
        }

        @Override
        public void info(String msg) {
            LOG.info(msg);
        }

        @Override
        public void info(Throwable e) {
            LOG.info(e);
        }

        @Override
        public void info(String msg, Throwable e) {
            LOG.info(msg, e);
        }

        @Override
        public void warn(String msg) {
            LOG.warn(msg);
        }

        @Override
        public void warn(Throwable e) {
            LOG.warn(e);
        }

        @Override
        public void warn(String msg, Throwable e) {
            LOG.warn(msg, e);
        }

        @Override
        public void error(String msg) {
            LOG.error(msg);
        }

        @Override
        public void error(Throwable e) {
            LOG.error(e);
        }

        @Override
        public void error(String msg, Throwable e) {
            LOG.error(msg, e);
        }
    }
    private static final SwarmLogger SWARM_LOGGER = new SwarmLoggerImpl();
}
