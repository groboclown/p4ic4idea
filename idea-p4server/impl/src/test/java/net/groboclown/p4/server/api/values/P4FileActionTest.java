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
package net.groboclown.p4.server.api.values;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.test.P4ServerExtension;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.MockConfigPart;
import net.groboclown.p4.server.impl.connection.impl.MessageStatusUtil;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.groboclown.p4.server.impl.ClientTestUtil.setupClient;
import static net.groboclown.p4.server.impl.ClientTestUtil.touchFile;
import static org.junit.jupiter.api.Assertions.*;

class P4FileActionTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @RegisterExtension
    P4ServerExtension server = new P4ServerExtension(false);

    @Test
    void convert_null() {
        assertEquals(P4FileAction.NONE, P4FileAction.convert(null));
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void convert_action_moveAdd(TemporaryFolder tmpDir)
            throws IOException {
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
        final FilePath toFile = VcsUtil.getFilePath(new File(clientRoot, "xyz.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                        runner.withConnection(clientConfig, (client) -> {
                            List<IFileSpec> srcFiles = FileSpecBuildUtil.forFilePaths(newFile);
                            List<IFileSpec> tgtFiles = FileSpecBuildUtil.forFilePaths(toFile);
                            List<IFileSpec> msgs = cmd.addFiles(client, srcFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);
                            IChangelist change = client.getServer().getChangelist(IChangelist.DEFAULT);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(client, null, null, change, Collections.singletonList(newFile));
                            MessageStatusUtil.throwIfError(msgs);

                            client.editFiles(srcFiles, new EditFilesOptions());
                            client.getServer().moveFile(
                                    srcFiles.get(0),
                                    tgtFiles.get(0),
                                    new MoveFileOptions());

                            List<IFileSpec> specs = new ArrayList<>(2);
                            specs.addAll(srcFiles);
                            specs.addAll(tgtFiles);
                            return cmd.getFileDetailsForOpenedSpecs(client.getServer(), specs, 10000);
                        })
                )
                .whenCompleted((specs) -> {
                    // 0 should be source, 1 should be target
                    assertEquals("//depot/abc.txt", specs.get(0).getDepotPathString());
                    assertEquals("//depot/xyz.txt", specs.get(1).getDepotPathString());

                    assertEquals(P4FileAction.MOVE_DELETE, P4FileAction.convert(specs.get(0).getAction()));
                    assertEquals(P4FileAction.MOVE_ADD, P4FileAction.convert(specs.get(1).getAction()));
                })
                .whenFailed(Assertions::fail);
    }

    // See MoveResponseTest for other variation states for the move operation.

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void convert_action_moveOverDeleted(TemporaryFolder tmpDir)
            throws IOException {
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
        final FilePath fromFile = VcsUtil.getFilePath(touchFile(clientRoot, "abc.txt"));
        final FilePath toFile = VcsUtil.getFilePath(touchFile(clientRoot, "xyz.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                        runner.withConnection(clientConfig, (client) -> {
                            List<IFileSpec> srcFiles = FileSpecBuildUtil.forFilePaths(fromFile);
                            List<IFileSpec> tgtFiles = FileSpecBuildUtil.forFilePaths(toFile);
                            List<IFileSpec> allFiles = new ArrayList<>(2);
                            allFiles.addAll(srcFiles);
                            allFiles.addAll(tgtFiles);
                            List<IFileSpec> msgs = cmd.addFiles(client, allFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);
                            IChangelist change = client.getServer().getChangelist(IChangelist.DEFAULT);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(client, null, null, change, Arrays.asList(fromFile, toFile));
                            MessageStatusUtil.throwIfError(msgs);

                            // Delete the target file
                            msgs = cmd.deleteFiles(client, tgtFiles, null);
                            MessageStatusUtil.throwIfError(msgs);
                            // make sure the command deleted the file.
                            assertFalse(toFile.getIOFile().exists());
                            change = client.getServer().getChangelist(IChangelist.DEFAULT);
                            change.setDescription("delete target file");
                            msgs = cmd.submitChangelist(client, null, null, change, Collections.singletonList(toFile));
                            MessageStatusUtil.throwIfError(msgs);
                            // make sure the file's still gone.
                            assertFalse(toFile.getIOFile().exists());

                            // Move over the deleted file.
                            msgs = client.editFiles(srcFiles, new EditFilesOptions());
                            MessageStatusUtil.throwIfError(msgs);
                            msgs = client.getServer().moveFile(
                                    srcFiles.get(0),
                                    tgtFiles.get(0),
                                    new MoveFileOptions());
                            // Note: if files are not open for edit, then this will report an INFO message that
                            // the files are not open for edit.
                            MessageStatusUtil.throwIfMessageOrEmpty("move", msgs);

                            List<IFileSpec> specs = new ArrayList<>(2);
                            specs.addAll(srcFiles);
                            specs.addAll(tgtFiles);
                            return cmd.getFileDetailsForOpenedSpecs(client.getServer(), specs, 10000);
                        })
                )
                .whenCompleted((specs) -> {
                    // 0 should be source, 1 should be target
                    assertEquals("//depot/abc.txt", specs.get(0).getDepotPathString());
                    assertEquals("//depot/xyz.txt", specs.get(1).getDepotPathString());

                    assertEquals(P4FileAction.MOVE_DELETE, P4FileAction.convert(specs.get(0).getAction()));
                    assertEquals(P4FileAction.MOVE_ADD, P4FileAction.convert(specs.get(1).getAction()));
                })
                .whenFailed(Assertions::fail);
    }
}