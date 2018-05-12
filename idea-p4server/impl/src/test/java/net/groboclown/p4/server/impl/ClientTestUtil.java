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

package net.groboclown.p4.server.impl;

import com.perforce.p4java.core.CoreFactory;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.impl.connection.FailP4RequestErrorHandler;
import net.groboclown.p4.server.impl.connection.P4RequestErrorHandler;
import net.groboclown.p4.server.impl.connection.impl.SimpleConnectionManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

public class ClientTestUtil {
    public static Answer<SimpleConnectionManager> setupClient(final ClientConfig config,
            final TemporaryFolder tmpDir, File clientRoot) {
        return setupClient(config, tmpDir, clientRoot, new FailP4RequestErrorHandler());
    }

    public static Answer<SimpleConnectionManager> setupClient(final ClientConfig config,
            final TemporaryFolder tmpDir, File clientRoot, P4RequestErrorHandler errorHandler) {
        final SimpleConnectionManager mgr = new SimpleConnectionManager(
                tmpDir.newFile("tmpdir"), 1000, "v1",
                errorHandler);
        if (!clientRoot.isDirectory()) {
            if (!clientRoot.mkdirs()) {
                fail("Could not create " + clientRoot);
            }
        }
        return mgr.withConnection(config.getServerConfig(), (server) ->
                CoreFactory.createClient(server, config.getClientname(), "new client from CoreFactory",
                        clientRoot.getAbsolutePath(), new String[]{"//depot/... //" + config.getClientname() + "/..."},
                        true))
                .map((x) -> mgr);
    }



    public static File touchFile(File parent, String name) {
        if (!parent.isDirectory()) {
            if (!parent.mkdirs()) {
                fail("Could not create " + parent);
            }
        }
        File ret = new File(parent, name);
        try {
            try (FileWriter fw = new FileWriter(ret)) {
                fw.write('x');
            }
        } catch (IOException e) {
            fail(e);
        }
        return ret;
    }
}
