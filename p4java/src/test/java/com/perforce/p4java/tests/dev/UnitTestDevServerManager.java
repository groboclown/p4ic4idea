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

package com.perforce.p4java.tests.dev;

import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.test.TestServer;
import org.junit.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

// p4ic4idea: compatibility for running unit tests from outside the Perforce network.
public class UnitTestDevServerManager {
    public static final UnitTestDevServerManager INSTANCE = new UnitTestDevServerManager();

    private final Map<String, LocalServer> servers = new HashMap<>();
    private int nextPort = 30121;

    UnitTestDevServerManager() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeAll));
    }

    public void startTestClass() {
        startTestClass(null);
    }

    public synchronized void startTestClass(String serverHost) {
        if (serverHost == null) {
            serverHost = MakePublic.getServerHost(MakePublic.getDefaultServerUrl());
        }
        LocalServer server = servers.get(serverHost);
        if (server == null) {
            server = new LocalServer(serverHost, nextPort++);
            servers.put(serverHost, server);
        }
        MakePublic.setServerUrlProp("p4java://localhost:" + server.port);
    }

    public synchronized void endTestClass() {
        endTestClass(null);
    }

    public synchronized void endTestClass(String serverHost) {
        // Let it keep running for performance reasons.  The cleanup will happen with the finalize,
        // which will happen at JVM stop.
        // if (serverHost == null) {
        //     serverHost = MakePublic.getServerHost(MakePublic.getDefaultServerUrl());
        // }
        // Assert.assertNotNull(serverHost);
        // LocalServer server = servers.get(serverHost);
        // if (server != null) {
        //     server.close();
        // }
        // servers.remove(serverHost);
        MakePublic.setServerUrlProp(null);
    }

    public synchronized void closeAll() {
        MakePublic.setServerUrlProp(null);
        for (LocalServer server : servers.values()) {
            try {
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
                // ignore
            }
        }
        servers.clear();
    }

    @Override
    protected void finalize()
            throws Throwable {
        closeAll();
        super.finalize();
    }


    private static class LocalServer {
        private final File tmpDir;
        private final int port;
        private final TestServer server;

        LocalServer(final String serverHost, final int port) {
            this.tmpDir = new File("tmp/" + serverHost);
            this.server = new TestServer(tmpDir, Integer.toString(port));
            this.server.setP4dVersion("r17.1");
            this.port = port;
            try {
                server.initialize(getClass().getClassLoader(),
                        "data/" + serverHost + "/depot.tar.gz",
                        "data/" + serverHost + "/checkpoint.gz");
                server.startAsync();
            } catch (Exception e) {
                try {
                    server.delete();
                } catch (Exception e2) {
                    throw new RuntimeException(e2);
                }
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void finalize()
                throws Throwable {
            close();
            super.finalize();
        }

        void close() {
            try {
                server.stopServer();
            } finally {
                try {
                    server.delete();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static class MakePublic extends P4JavaTestCase {
        static final String ORIG_PROP = System.getProperty(P4JTEST_SERVER_URL_PROPNAME);
        static void setServerUrlProp(String serverUrl) {
            if (serverUrl == null) {
                if (ORIG_PROP == null) {
                    System.clearProperty(P4JTEST_SERVER_URL_PROPNAME);
                } else {
                    System.setProperty(P4JTEST_SERVER_URL_PROPNAME, ORIG_PROP);
                }
            } else {
                System.setProperty(P4JTEST_SERVER_URL_PROPNAME, serverUrl);
            }
        }

        static String getDefaultServerUrl() {
            return P4JTEST_SERVER_URL_DEFAULT;
        }

        static String getServerHost(String serverUrl) {
            if (serverUrl == null) {
                return null;
            }
            if (serverUrl.startsWith("rsh:")) {
                throw new IllegalStateException("cannot use this with rsh instances");
            }
            String ret = serverUrl;
            final int pos1 = ret.indexOf("://");
            final int pos2 = ret.indexOf('?');
            if (pos2 > pos1) {
                ret = ret.substring(0, pos2);
                // this keeps pos1 correct.
            }
            if (pos1 > 0) {
                ret = ret.substring(pos1 + 3);
            }
            final int pos3 = ret.indexOf(':');
            if (pos3 > 0) {
                ret = ret.substring(0, pos3);
            }
            return ret;
        }
    }
}
