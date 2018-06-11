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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.test.P4ServerExtension;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static net.groboclown.p4.server.impl.ClientTestUtil.setupClient;
import static net.groboclown.p4.server.impl.ClientTestUtil.touchFile;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenFileStatusTest {

    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @RegisterExtension
    P4ServerExtension server = new P4ServerExtension(false);

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void statusSort_notOnServer(TemporaryFolder tmpDir)
            throws IOException {
        idea.useInlineThreading(null);
        MockConfigPart part = new MockConfigPart()
                // By using the RSH port, it means that the connection will be kept open
                // (NTS connection).  By keeping the connection open until explicitly
                // disconnected, this will indirectly be testing that the
                // SimpleConnectionManager closes the connection.
                .withServerName(server.getRshUrl())
                .withUsername(server.getUser())
                .withNoPassword()
                .withClientname("client1");
        final ServerConfig serverConfig = ServerConfig.createFrom(part);
        final ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, part);
        final File clientRoot = tmpDir.newFile("clientRoot");
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "abc.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                    runner.withConnection(clientConfig, (client) -> {
                        List<IFileSpec> srcFiles = FileSpecBuildUtil.escapedForFilePaths(newFile);
                        return new OpenFileStatus(cmd.getFileDetailsForOpenedSpecs(client.getServer(), srcFiles, 1000));
                    })
                )
                .whenCompleted((r) -> {
                    assertEmpty(r.getAdd());
                    assertSize(1, r.getMessages());
                    assertEmpty(r.getFilesWithMessages());
                    assertEmpty(r.getMoveMap().keySet());
                    assertEmpty(r.getDelete());
                    assertEmpty(r.getEdit());
                    assertEmpty(r.getSkipped());
                    assertEmpty(r.getOpen());
                    assertEquals(0, r.getOpenedCount());

                    IServerMessage msg = r.getMessages().get(0);
                    assertNotNull(msg);
                    assertTrue(msg.isWarning());
                    assertThat(msg.getAllMessages().iterator().next().getLocalizedMessage(),
                            containsString("abc.txt - no such file(s)."));
                })
                .whenFailed(Assertions::fail);
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void statusSort_add(TemporaryFolder tmpDir)
            throws IOException {
        idea.useInlineThreading(null);
        MockConfigPart part = new MockConfigPart()
                // By using the RSH port, it means that the connection will be kept open
                // (NTS connection).  By keeping the connection open until explicitly
                // disconnected, this will indirectly be testing that the
                // SimpleConnectionManager closes the connection.
                .withServerName(server.getRshUrl())
                .withUsername(server.getUser())
                .withNoPassword()
                .withClientname("client1");
        final ServerConfig serverConfig = ServerConfig.createFrom(part);
        final ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, part);
        final File clientRoot = tmpDir.newFile("clientRoot");
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "abc.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                        runner.withConnection(clientConfig, (client) -> {
                            // Add the file to Perforce, but don't submit it.
                            List<IFileSpec> srcFiles = FileSpecBuildUtil.forFilePaths(newFile);
                            List<IFileSpec> msgs = cmd.addFiles(client, srcFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);

                            // Get the status
                            List<IFileSpec> serverFiles = FileSpecBuildUtil.escapedForFilePaths(newFile);
                            return new OpenFileStatus(cmd.getFileDetailsForOpenedSpecs(client.getServer(),
                                    serverFiles, 1000));
                        })
                )
                .whenCompleted((r) -> {
                    assertSize(1, r.getAdd());
                    assertEmpty(r.getMessages());
                    assertEmpty(r.getFilesWithMessages());
                    assertEmpty(r.getMoveMap().keySet());
                    assertEmpty(r.getDelete());
                    assertEmpty(r.getEdit());
                    assertEmpty(r.getSkipped());
                    assertSize(1, r.getOpen());
                    assertEquals(1, r.getOpenedCount());

                    IExtendedFileSpec spec = r.getAdd().iterator().next();
                    assertNotNull(spec);
                    assertEquals(spec.getDepotPathString(), "//depot/abc.txt");
                    assertEquals(spec.getClientPathString(), "//client1/abc.txt");
                    assertEquals(spec.getLocalPathString(), newFile.getPath());
                })
                .whenFailed(Assertions::fail);
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void statusSort_notOpen(TemporaryFolder tmpDir)
            throws IOException {
        idea.useInlineThreading(null);
        MockConfigPart part = new MockConfigPart()
                // By using the RSH port, it means that the connection will be kept open
                // (NTS connection).  By keeping the connection open until explicitly
                // disconnected, this will indirectly be testing that the
                // SimpleConnectionManager closes the connection.
                .withServerName(server.getRshUrl())
                .withUsername(server.getUser())
                .withNoPassword()
                .withClientname("client1");
        final ServerConfig serverConfig = ServerConfig.createFrom(part);
        final ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, part);
        final File clientRoot = tmpDir.newFile("clientRoot");
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "abc.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                        runner.withConnection(clientConfig, (client) -> {
                            // Add and submit the file to Perforce
                            List<IFileSpec> srcFiles = FileSpecBuildUtil.forFilePaths(newFile);
                            List<IFileSpec> msgs = cmd.addFiles(client, srcFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);
                            IChangelist change = client.getServer().getChangelist(IChangelist.DEFAULT);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(null, null, change);
                            MessageStatusUtil.throwIfError(msgs);

                            // Get the status
                            List<IFileSpec> serverFiles = FileSpecBuildUtil.escapedForFilePaths(newFile);
                            return new OpenFileStatus(cmd.getFileDetailsForOpenedSpecs(client.getServer(),
                                    serverFiles, 1000));
                        })
                )
                .whenCompleted((r) -> {
                    assertEmpty(r.getAdd());
                    assertEmpty(r.getMessages());
                    assertEmpty(r.getFilesWithMessages());
                    assertEmpty(r.getMoveMap().keySet());
                    assertEmpty(r.getDelete());
                    assertEmpty(r.getEdit());
                    assertSize(1, r.getSkipped());
                    assertEmpty(r.getOpen());
                    assertEquals(0, r.getOpenedCount());

                    IExtendedFileSpec spec = r.getSkipped().iterator().next();
                    assertNotNull(spec);
                    assertEquals(spec.getDepotPathString(), "//depot/abc.txt");
                    assertEquals(spec.getClientPathString(), "//client1/abc.txt");
                    assertEquals(spec.getLocalPathString(), newFile.getPath());
                })
                .whenFailed(Assertions::fail);
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void statusSort_edit(TemporaryFolder tmpDir)
            throws IOException {
        idea.useInlineThreading(null);
        MockConfigPart part = new MockConfigPart()
                // By using the RSH port, it means that the connection will be kept open
                // (NTS connection).  By keeping the connection open until explicitly
                // disconnected, this will indirectly be testing that the
                // SimpleConnectionManager closes the connection.
                .withServerName(server.getRshUrl())
                .withUsername(server.getUser())
                .withNoPassword()
                .withClientname("client1");
        final ServerConfig serverConfig = ServerConfig.createFrom(part);
        final ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, part);
        final File clientRoot = tmpDir.newFile("clientRoot");
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "abc.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                        runner.withConnection(clientConfig, (client) -> {
                            // Add and submit the file to Perforce
                            List<IFileSpec> srcFiles = FileSpecBuildUtil.forFilePaths(newFile);
                            List<IFileSpec> msgs = cmd.addFiles(client, srcFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);
                            IChangelist change = client.getServer().getChangelist(IChangelist.DEFAULT);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(null, null, change);
                            MessageStatusUtil.throwIfError(msgs);

                            // Open for edit
                            List<IFileSpec> serverFiles = FileSpecBuildUtil.escapedForFilePaths(newFile);
                            msgs = cmd.editFiles(client, serverFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);

                            // Get the status
                            return new OpenFileStatus(cmd.getFileDetailsForOpenedSpecs(client.getServer(),
                                    serverFiles, 1000));
                        })
                )
                .whenCompleted((r) -> {
                    assertEmpty(r.getAdd());
                    assertEmpty(r.getMessages());
                    assertEmpty(r.getFilesWithMessages());
                    assertEmpty(r.getMoveMap().keySet());
                    assertEmpty(r.getDelete());
                    assertSize(1, r.getEdit());
                    assertEmpty(r.getSkipped());
                    assertSize(1, r.getOpen());
                    assertEquals(1, r.getOpenedCount());

                    IExtendedFileSpec spec = r.getEdit().iterator().next();
                    assertNotNull(spec);
                    assertEquals(spec.getDepotPathString(), "//depot/abc.txt");
                    assertEquals(spec.getClientPathString(), "//client1/abc.txt");
                    assertEquals(spec.getLocalPathString(), newFile.getPath());
                })
                .whenFailed(Assertions::fail);
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void statusSort_delete(TemporaryFolder tmpDir)
            throws IOException {
        idea.useInlineThreading(null);
        MockConfigPart part = new MockConfigPart()
                // By using the RSH port, it means that the connection will be kept open
                // (NTS connection).  By keeping the connection open until explicitly
                // disconnected, this will indirectly be testing that the
                // SimpleConnectionManager closes the connection.
                .withServerName(server.getRshUrl())
                .withUsername(server.getUser())
                .withNoPassword()
                .withClientname("client1");
        final ServerConfig serverConfig = ServerConfig.createFrom(part);
        final ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, part);
        final File clientRoot = tmpDir.newFile("clientRoot");
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "abc.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                        runner.withConnection(clientConfig, (client) -> {
                            // Add and submit the file to Perforce
                            List<IFileSpec> srcFiles = FileSpecBuildUtil.forFilePaths(newFile);
                            List<IFileSpec> msgs = cmd.addFiles(client, srcFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);
                            IChangelist change = client.getServer().getChangelist(IChangelist.DEFAULT);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(null, null, change);
                            MessageStatusUtil.throwIfError(msgs);

                            // Open for delete
                            List<IFileSpec> serverFiles = FileSpecBuildUtil.escapedForFilePaths(newFile);
                            msgs = cmd.deleteFiles(client, serverFiles, null);
                            MessageStatusUtil.throwIfError(msgs);

                            // Get the status
                            return new OpenFileStatus(cmd.getFileDetailsForOpenedSpecs(client.getServer(),
                                    serverFiles, 1000));
                        })
                )
                .whenCompleted((r) -> {
                    assertEmpty(r.getAdd());
                    assertEmpty(r.getMessages());
                    assertEmpty(r.getFilesWithMessages());
                    assertEmpty(r.getMoveMap().keySet());
                    assertSize(1, r.getDelete());
                    assertEmpty(r.getEdit());
                    assertEmpty(r.getSkipped());
                    assertSize(1, r.getOpen());
                    assertEquals(1, r.getOpenedCount());

                    IExtendedFileSpec spec = r.getDelete().iterator().next();
                    assertNotNull(spec);
                    assertEquals(spec.getDepotPathString(), "//depot/abc.txt");
                    assertEquals(spec.getClientPathString(), "//client1/abc.txt");
                    assertEquals(spec.getLocalPathString(), newFile.getPath());
                })
                .whenFailed(Assertions::fail);
    }
}