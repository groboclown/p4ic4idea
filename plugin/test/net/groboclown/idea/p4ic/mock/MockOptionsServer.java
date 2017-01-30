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

package net.groboclown.idea.p4ic.mock;

import com.perforce.p4java.exception.*;
import com.perforce.p4java.impl.mapbased.rpc.RpcServer;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.server.ServerStatus;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MockOptionsServer extends RpcServer {
    private final String serverUriString;
    private final Responses responses;


    public MockOptionsServer(@NotNull String serverUriString) {
        this(serverUriString, new Responses());
    }

    public MockOptionsServer(@NotNull String serverUriString, @NotNull Responses responses) {
        this.responses = responses;
        this.serverUriString = serverUriString;

        // FIXME change to ensure that the server is registered in the
        // P4Connection...Factory class, which should have INSTANCE be mocked out.
        // TestConnectionHandler.registerServer(serverUriString, this);
    }


    public void simulateSetup(@NotNull ServerConfig config) throws ConnectionException, ConfigException {
        // TODO actually pull apart the host / port
        init(config.getServerName().getFullPort(), 1, new Properties(), new UsageOptions(new Properties()),
                config.getServerName().isSecure());
    }


    public void close() {
        // FIXME change to ensure that the server is registered in the
        // P4Connection...Factory class, which should have INSTANCE be mocked out.
        // TestConnectionHandler.deregisterServer(serverUriString);
    }



    public void addOnce(@NotNull P4Request request, @NotNull P4Response response) {
        responses.addOnce(request, response);
    }

    public void add(@NotNull P4Request request, @NotNull P4Response response) {
        responses.add(request, response);
    }



    @Override
    public Map<String, Object>[] execMapCmd(final String cmdName, final String[] cmdArgs,
            final Map<String, Object> inMap)
            throws ConnectionException, AccessException, RequestException {
        try {
            return responses.pull(new P4Request(cmdName, cmdArgs, inMap, null)).getArray();
        } catch (ConnectionException e) {
            throw e;
        } catch (AccessException e) {
            throw e;
        } catch (RequestException e) {
            throw e;
        } catch (P4JavaException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Map<String, Object>> execMapCmdList(final String cmdName, final String[] cmdArgs,
            final Map<String, Object> inMap)
            throws P4JavaException {
        return responses.pull(new P4Request(cmdName, cmdArgs, inMap, null)).getList();
    }

    @Override
    public List<Map<String, Object>> execMapCmdList(final String cmdName, final String[] cmdArgs,
            final Map<String, Object> inMap,
            final IFilterCallback filterCallback) throws P4JavaException {
        return responses.pull(new P4Request(cmdName, cmdArgs, inMap, null)).getList(filterCallback);
    }

    @Override
    public Map<String, Object>[] execInputStringMapCmd(final String cmdName, final String[] cmdArgs,
            final String inString)
            throws P4JavaException {
        return responses.pull(new P4Request(cmdName, cmdArgs, null, inString)).getArray();
    }

    @Override
    public List<Map<String, Object>> execInputStringMapCmdList(final String cmdName, final String[] cmdArgs,
            final String inString)
            throws P4JavaException {
        return responses.pull(new P4Request(cmdName, cmdArgs, null, inString)).getList();
    }

    @Override
    public List<Map<String, Object>> execInputStringMapCmdList(final String cmdName, final String[] cmdArgs,
            final String inString,
            final IFilterCallback filterCallback) throws P4JavaException {
        return responses.pull(new P4Request(cmdName, cmdArgs, null, inString)).getList(filterCallback);
    }

    @Override
    public Map<String, Object>[] execQuietMapCmd(final String cmdName, final String[] cmdArgs,
            final Map<String, Object> inMap)
            throws ConnectionException, RequestException, AccessException {
        try {
            return responses.pull(new P4Request(cmdName, cmdArgs, inMap, null)).getArray();
        } catch (ConnectionException e) {
            throw e;
        } catch (AccessException e) {
            throw e;
        } catch (RequestException e) {
            throw e;
        } catch (P4JavaException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Map<String, Object>> execQuietMapCmdList(final String cmdName, final String[] cmdArgs,
            final Map<String, Object> inMap)
            throws P4JavaException {
        return responses.pull(new P4Request(cmdName, cmdArgs, inMap, null)).getList();
    }

    @Override
    public InputStream execStreamCmd(final String cmdName, final String[] cmdArgs)
            throws ConnectionException, RequestException, AccessException {
        try {
            return responses.pull(new P4Request(cmdName, cmdArgs, null, null)).getResponse();
        } catch (ConnectionException e) {
            throw e;
        } catch (AccessException e) {
            throw e;
        } catch (RequestException e) {
            throw e;
        } catch (P4JavaException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InputStream execStreamCmd(final String cmdName, final String[] cmdArgs, final Map<String, Object> inMap)
            throws P4JavaException {
        return responses.pull(new P4Request(cmdName, cmdArgs, inMap, null)).getResponse();
    }

    @Override
    public InputStream execInputStringStreamCmd(final String cmdName, final String[] cmdArgs, final String inString)
            throws P4JavaException {
        return responses.pull(new P4Request(cmdName, cmdArgs, null, inString)).getResponse();
    }

    @Override
    public InputStream execQuietStreamCmd(final String cmdName, final String[] cmdArgs)
            throws ConnectionException, RequestException, AccessException {
        try {
            return responses.pull(new P4Request(cmdName, cmdArgs, null, null)).getResponse();
        } catch (ConnectionException e) {
            throw e;
        } catch (AccessException e) {
            throw e;
        } catch (RequestException e) {
            throw e;
        } catch (P4JavaException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void execStreamingMapCommand(final String cmdName, final String[] cmdArgs, final Map<String, Object> inMap,
            final IStreamingCallback callback, final int key) throws P4JavaException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void execInputStringStreamingMapComd(final String cmdName, final String[] cmdArgs, final String inString,
            final IStreamingCallback callback, final int key) throws P4JavaException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void execInputStringStreamingMapCmd(final String cmdName, final String[] cmdArgs, final String inString,
            final IStreamingCallback callback, final int key) throws P4JavaException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public ServerStatus init(final String host, final int port, final Properties props, final UsageOptions opts,
            final boolean secure, final String rsh)
            throws ConfigException, ConnectionException {
        return null;
    }
}
