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

package net.groboclown.p4plugin.mock;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.extensions.Errors;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.ResultErrorUtil;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.connection.ConnectionManager;
import net.groboclown.p4.server.impl.connection.P4Func;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockConnectionManager implements ConnectionManager {
    public List<Throwable> errors = new ArrayList<>();
    public P4CommandRunner.ServerResultException problem = null;
    public IClient client = mock(IClient.class);
    public Server server = mock(Server.class);
    public ClientConfigurator clientConfig = null;
    public ServerConfigurator serverConfig = null;
    public ServerNameConfigurator serverNameConfig = null;
    public int disconnectedCount = 0;

    public interface ClientConfigurator {
        IClient configure(ClientConfig config, IClient base)
                throws P4JavaException;
    }

    public interface ServerConfigurator {
        Server configure(ServerConfig config, Server base)
                throws P4JavaException;
    }

    public interface ServerNameConfigurator {
        Server configure(P4ServerName config, Server base)
                throws P4JavaException;
    }



    @NotNull
    public MockConnectionManager withNoProblem() {
        problem = null;
        return this;
    }

    @NotNull
    public MockConnectionManager withProblem(@NotNull P4CommandRunner.ErrorCategory category,
            @Nullable String message) {
        problem = ResultErrorUtil.createException(category, message, null);
        return this;
    }

    @NotNull
    public MockConnectionManager withProblem(@NotNull P4CommandRunner.ErrorCategory category,
            @Nullable String message, @NotNull Throwable t) {
        problem = ResultErrorUtil.createException(category, message, t);
        return this;
    }

    @NotNull
    public MockConnectionManager withClientSetup(@NotNull ClientConfigurator cc) {
        clientConfig = cc;
        return this;
    }

    @NotNull
    public MockConnectionManager withServerSetup(@NotNull ServerConfigurator sc) {
        serverConfig = sc;
        return this;
    }

    @NotNull
    public MockConnectionManager withServerNameSetup(@NotNull ServerNameConfigurator sc) {
        serverNameConfig = sc;
        return this;
    }

    @NotNull
    public MockConnectionManager withErrors(@NotNull Errors err) {
        this.errors = err.get();
        return this;
    }



    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ClientConfig config, @NotNull P4Func<IClient, R> fun) {
        return withConnection(config, null, fun);
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ClientConfig config,
            @Nullable File cwd, @NotNull P4Func<IClient, R> fun) {
        if (problem != null) {
            return Answer.reject(problem);
        }
        try {
            return Answer.resolve(fun.func(setupClient(config)));
        } catch (Exception e) {
            errors.add(e);
            return Answer.reject(ResultErrorUtil.createException(
                    P4CommandRunner.ErrorCategory.CONNECTION,
                    e.getMessage() == null ? "(null message)" : e.getMessage(), e));
        }
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull OptionalClientServerConfig config, @NotNull P4Func<IOptionsServer, R> fun) {
        if (problem != null) {
            return Answer.reject(problem);
        }
        try {
            return Answer.resolve(fun.func(setupServer(config.getServerConfig())));
        } catch (Exception e) {
            errors.add(e);
            return Answer.reject(ResultErrorUtil.createException(
                    P4CommandRunner.ErrorCategory.CONNECTION, e.getMessage(), e));
        }
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull P4ServerName config, P4Func<IOptionsServer, R> fun) {
        if (problem != null) {
            return Answer.reject(problem);
        }
        try {
            return Answer.resolve(fun.func(setupServer(config)));
        } catch (Exception e) {
            errors.add(e);
            return Answer.reject(ResultErrorUtil.createException(
                    P4CommandRunner.ErrorCategory.CONNECTION, e.getMessage(), e));
        }
    }

    @Override
    public void disconnect(@NotNull P4ServerName config) {
        disconnectedCount++;
    }


    private IClient setupClient(@NotNull ClientConfig config)
            throws P4JavaException {
        IOptionsServer s = setupServer(config.getServerConfig());
        IClient c = client;
        if (clientConfig != null) {
            c = clientConfig.configure(config, c);
        }
        when(c.getServer()).thenReturn(s);
        when(c.getName()).thenReturn(config.getClientname());
        return c;
    }

    private IOptionsServer setupServer(@NotNull ServerConfig config)
            throws P4JavaException {
        Server s = setupServer(config.getServerName());
        if (serverConfig != null) {
            s = serverConfig.configure(config, s);
        }
        when(s.getUserName()).thenReturn(config.getUsername());
        return s;
    }

    private Server setupServer(@NotNull P4ServerName config)
            throws P4JavaException {
        Server s = server;
        if (serverNameConfig != null) {
            s = serverNameConfig.configure(config, s);
        }
        return s;
    }
}
