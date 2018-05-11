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
package net.groboclown.p4.server.impl.client;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.test.P4ServerExtension;
import com.perforce.test.ServerRule;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.connection.MockP4RequestErrorHandler;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.connection.impl.SimpleConnectionManager;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.groboclown.idea.ExtAsserts.assertSize;
import static net.groboclown.p4.server.impl.AssertFileSpec.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Ensures file handling and name escaping is done correctly.
 */
class OpenedFilesChangesFactoryTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @RegisterExtension
    P4ServerExtension p4 = new P4ServerExtension(false,
            new ServerRule.Unicode(),
            new ServerRule.RunAsync());

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void simpleAddedFiles_localFiles(TemporaryFolder tmpDir)
            throws IOException, InterruptedException {
        ClientConfig clientConfig = createClientConfig();
        File clientRoot = tmpDir.newFile("clientRoot");
        if (!clientRoot.isDirectory() && !clientRoot.mkdirs()) {
            throw new IOException("could not mkdir " + clientRoot);
        }

        setupClient(clientConfig, tmpDir, clientRoot)
        .map((mgr) -> mgr.withConnection(clientConfig, (client) -> {
            // Create initial files
            File f1 = touchFile(clientRoot, "abc.txt");
            File f2 = touchFile(clientRoot, "a@b.txt");
            try {
                List<IFileSpec> addedFiles =
                        client.addFiles(FileSpecBuildUtil.forFiles(f1, f2),
                                new AddFilesOptions(false, -1, "text", true));
                assertSize(2, addedFiles);
                assertNoErrors(addedFiles);

                // Yoinked from ConnectCommandRunner
                List<IExtendedFileSpec> openedDefaultChangelistFiles =
                        P4CommandUtil.getFilesOpenInDefaultChangelist(client.getServer());

                List<FilePath> paths = OpenedFilesChangesFactory.getLocalFiles(openedDefaultChangelistFiles);

                assertSize(2, paths);
                FilePath path1 = paths.get(0);
                FilePath path2 = paths.get(1);
                if (path2.getName().equals("abc.txt")) {
                    FilePath t = path1;
                    path1 = path2;
                    path2 = t;
                }
                assertEquals("abc.txt", path1.getName());
                assertEquals("a@b.txt", path2.getName());

                return Promise.resolve(null);
            } catch (P4JavaException e) {
                return Promises.rejectedPromise(e);
            }
        })).blockingWait(5, TimeUnit.SECONDS);
    }


    private ClientConfig createClientConfig() {
        MockConfigPart part = new MockConfigPart()
                // By using the RSH port, it means that the connection will be kept open
                // (NTS connection).  By keeping the connection open until explicitly
                // disconnected, this will indirectly be testing that the
                // SimpleConnectionManager closes the connection.
                .withServerName(p4.getPort())
                .withUsername(p4.getUser())
                .withClientname("client")
                .withNoPassword();

        final ServerConfig serverConfig = ServerConfig.createFrom(part);
        return ClientConfig.createFrom(serverConfig, part);
    }


    private Answer<SimpleConnectionManager> setupClient(final ClientConfig config,
            final TemporaryFolder tmpDir, File clientRoot) {
        final MockP4RequestErrorHandler errorHandler = new MockP4RequestErrorHandler();
        final SimpleConnectionManager mgr = new SimpleConnectionManager(
                tmpDir.newFile("tmpdir"), 1000, "v1",
                errorHandler);
        return mgr.withConnection(config.getServerConfig(), (server) ->
                CoreFactory.createClient(server, "client", "new client from CoreFactory",
                clientRoot.getAbsolutePath(), new String[]{"//depot/... //client/..."}, true))
            .map((x) -> mgr);
    }


    private static File touchFile(File parent, String name) {
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