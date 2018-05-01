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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class P4ServerExtension
        implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback {

    private final List<ExtensionContext> contextStack = new LinkedList<>();
    private final boolean classWide;
    private final ServerRule.Initializer[] initializers;


    public P4ServerExtension(boolean classWide, ServerRule.Initializer... initializers) {
        this.classWide = classWide;
        this.initializers = initializers;
    }

    public String getRshUrl()
            throws IOException {
        return getServer().getRSHURL();
    }

    public boolean isServerRunning() {
        return getServer().isAlive();
    }


    @Nonnull
    public String getPort() {
        return getServer().getPort();
    }

    public String getUser() {
        return getServer().getUser();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext)
            throws Exception {
        if (classWide) {
            contextStack.add(0, extensionContext);
            initialize(extensionContext);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext)
            throws Exception {
        if (classWide && !contextStack.isEmpty() && contextStack.get(0).equals(extensionContext)) {
            contextStack.remove(0);
            cleanup(extensionContext);
        }
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext)
            throws Exception {
        if (!classWide) {
            contextStack.add(0, extensionContext);
            initialize(extensionContext);
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext)
            throws Exception {
        if (!classWide && !contextStack.isEmpty() && contextStack.get(0).equals(extensionContext)) {
            contextStack.remove(0);
            cleanup(extensionContext);
        }
    }

    private void initialize(ExtensionContext extensionContext)
            throws Exception {
        ExtensionContext.Store store = getStore(extensionContext);
        TestServer server = new TestServer();
        store.put("server", server);
        server.delete();
        for (ServerRule.Initializer initializer : initializers) {
            initializer.initialize(server);
        }
        if (server.hasProcessError()) {
            Throwable err = server.getProcessError();
            if (err instanceof Exception) {
                throw (Exception) err;
            }
            if (err instanceof Error) {
                throw  (Error) err;
            }
            throw new RuntimeException(err);
        }
    }

    private void cleanup(ExtensionContext extensionContext)
            throws IOException {
        TestServer server = getServer(extensionContext);
        server.stopServer();
        server.delete();
    }

    private TestServer getServer() {
        return getServer(getTopContext());
    }

    private TestServer getServer(ExtensionContext context) {
        return (TestServer) getStore(context).get("server");
    }

    private ExtensionContext getTopContext() {
        assert !contextStack.isEmpty() : "No context stack known";
        return contextStack.get(0);
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
    }
}
