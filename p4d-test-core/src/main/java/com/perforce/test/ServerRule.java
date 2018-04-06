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

package com.perforce.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

@SuppressWarnings("unused")
public class ServerRule implements TestRule {
    public interface Initializer {
        void initialize(@Nonnull TestServer server) throws IOException;
    }

    /**
     * Sets the server to Unicode mode.  Should be run before "initialize" is run.
     */
    public static class Unicode implements Initializer {
        @Override
        public void initialize(@Nonnull TestServer server) {
            server.setUnicode(true);
        }
    }

    /**
     * Sets the p4d version to run.
     */
    public static class P4dVersion implements Initializer {
        private final String version;

        public P4dVersion(String version) {
            this.version = version;
        }

        @Override
        public void initialize(@Nonnull TestServer server) {
            server.setP4dVersion(version);
        }
    }

    /**
     * Starts the server in asynchronous (background) mode.  Most of the time, this
     * is unnecessary, because the p4java code can communicate with the server using
     * "rsh" mode - directly calling p4d rather than over a socket.
     */
    public static class RunAsync implements Initializer {
        @Override
        public void initialize(@Nonnull TestServer server)
                throws IOException {
            server.startAsync();
        }
    }

    /**
     * Run the server's initialize method.  This can optionally use an archive of
     * an initial set of depot database files, and an optional checkpoint archive file.
     */
    public static class InitializeWith implements Initializer {
        private final ClassLoader cl;
        private final String depotResource;
        private final String checkpointResource;

        public InitializeWith() {
            this((ClassLoader) null, null, null);
        }

        public InitializeWith(@Nullable Object parentObject, @Nullable String depotResource, @Nullable String checkpointResource) {
            this(
                    parentObject == null
                            ? null
                            : (parentObject instanceof Class<?>
                                ? ((Class<?>) parentObject).getClassLoader()
                                : parentObject.getClass().getClassLoader()),
                    depotResource,
                    checkpointResource
            );
        }

        @SuppressWarnings("WeakerAccess")
        public InitializeWith(@Nullable ClassLoader cl, @Nullable String depotResource, @Nullable String checkpointResource) {
            this.cl = cl;
            this.depotResource = depotResource;
            this.checkpointResource = checkpointResource;
        }

        @Override
        public void initialize(@Nonnull TestServer server)
                throws IOException {
            server.initialize(cl, depotResource, checkpointResource);
        }
    }


    public static ServerRule createCaseSensitive(Initializer... initializers) {
        return new ServerRule(new CaseSensitiveTestServer(nextRootDir()), initializers);
    }

    public static ServerRule createCaseInsensitive(Initializer... initializers) {
        return new ServerRule(new TestServer(nextRootDir()), initializers);
    }


    private static int creationCount = 0;


	private final TestServer testServer;
	private final Initializer[] initializers;

	public ServerRule(@Nonnull TestServer server, @Nonnull Initializer... initializers) {
		testServer = server;
		this.initializers = initializers;
	}

	public String getRshUrl()
			throws IOException {
		return testServer.getRSHURL();
	}

	public boolean isServerRunning() {
	    return testServer.isAlive();
    }

    @Nonnull
    public String getPort() {
	    return testServer.getPort();
    }

	@Override
	public Statement apply(Statement statement, Description description) {
		return new ServerStatement(statement);
	}

	@SuppressWarnings("unused")
	public String getPathToRoot() {
		return testServer.getPathToRoot();
	}

	private static synchronized File nextRootDir() {
	    int next = creationCount++;
	    return new File("out/test-server-" + next);
    }

	private class ServerStatement extends Statement {

		private final Statement statement;

		ServerStatement(Statement statement) {
			this.statement = statement;
		}

		@Override
		public void evaluate() throws Throwable {
			testServer.delete();
            for (Initializer initializer : initializers) {
                initializer.initialize(testServer);
            }
            if (testServer.hasProcessError()) {
                throw testServer.getProcessError();
            }
			statement.evaluate();
			testServer.stopServer();
			testServer.delete();
		}
	}
}
