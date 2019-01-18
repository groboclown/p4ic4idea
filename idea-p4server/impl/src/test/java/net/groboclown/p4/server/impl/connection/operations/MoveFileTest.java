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
package net.groboclown.p4.server.impl.connection.operations;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.MessageSubsystemCode;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.IServerMessageCode;
import com.perforce.p4java.server.ISingleServerMessage;
import com.perforce.test.P4ServerExtension;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.impl.connection.MockP4RequestErrorHandler;
import net.groboclown.p4.server.impl.connection.impl.MessageStatusUtil;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static net.groboclown.idea.ExtAsserts.assertSize;
import static net.groboclown.p4.server.impl.ClientTestUtil.setupClient;
import static net.groboclown.p4.server.impl.ClientTestUtil.touchFile;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**`
 * Tests the server responses on various move operation states.  Some of these directly ensure that the
 * underlying Perforce server responds in specific ways.
 */
class MoveFileTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @RegisterExtension
    P4ServerExtension server = new P4ServerExtension(false);

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void move_add(TemporaryFolder tmpDir)
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
        final FilePath toFile = VcsUtil.getFilePath(new File(clientRoot, "xyz.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();
        final MoveFile moveFile = new MoveFile(idea.getMockProject(), cmd);

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                        runner.withConnection(clientConfig, (client) -> {
                            List<IFileSpec> srcFiles = FileSpecBuildUtil.forFilePaths(newFile);
                            List<IFileSpec> tgtFiles = FileSpecBuildUtil.forFilePaths(toFile);
                            List<IFileSpec> msgs = cmd.addFiles(client, srcFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);
                            IChangelist change = client.getServer().getChangelist(IChangelist.DEFAULT);
                            P4ChangelistId clId = createChangelistId(change, clientConfig);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(client, null, null, change, Collections.singletonList(newFile));
                            MessageStatusUtil.throwIfError(msgs);

                            client.editFiles(srcFiles, new EditFilesOptions());
                            MoveFileResult res = moveFile.moveFile(client, clientConfig,
                                    new MoveFileAction(newFile, toFile, clId));
                            msgs = res.getServerMessages();
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

                    assertEquals(FileAction.MOVE_DELETE, specs.get(0).getAction());
                    assertEquals(FileAction.MOVE_ADD, specs.get(1).getAction());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void move_sourceEdited(TemporaryFolder tmpDir)
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
        final FilePath fromFile = VcsUtil.getFilePath(touchFile(clientRoot, "abc.txt"));
        final FilePath toFile = VcsUtil.getFilePath(touchFile(clientRoot, "xyz.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();
        final MoveFile moveFile = new MoveFile(idea.getMockProject(), cmd);

        setContents(fromFile, "initial");
        assertEquals("initial", getContents(fromFile));
        assertTrue(toFile.getIOFile().delete());

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                        runner.withConnection(clientConfig, (client) -> {
                            List<IFileSpec> srcFiles = FileSpecBuildUtil.forFilePaths(fromFile);
                            List<IFileSpec> msgs = cmd.addFiles(client, srcFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);
                            IChangelist change = client.getServer().getChangelist(IChangelist.DEFAULT);
                            P4ChangelistId clId = createChangelistId(change, clientConfig);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(client, null, null, change, Collections.singletonList(fromFile));
                            MessageStatusUtil.throwIfError(msgs);

                            // Open for edit source.
                            msgs = client.editFiles(srcFiles, new EditFilesOptions());
                            MessageStatusUtil.throwIfError(msgs);

                            // Alter contents of source.
                            setContents(fromFile, "updated");
                            assertEquals("updated", getContents(fromFile));

                            MoveFileResult res =
                                    moveFile.moveFile(client, clientConfig,
                                            new MoveFileAction(fromFile, toFile, clId));
                            msgs = res.getServerMessages();
                            assertSize(1, msgs);
                            assertNull(msgs.get(0).getStatusMessage());
                            assertEquals(toFile.getPath(), msgs.get(0).getLocalPathString());

                            // The move operation is setup to not perform the underlying move, that is left to the IDE.
                            assertTrue(fromFile.getIOFile().exists());
                            assertFalse(toFile.getIOFile().exists());

                            return null;
                        })
                )
                .whenFailed(Assertions::fail);
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void moveMessage_addNotOpen(TemporaryFolder tmpDir)
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
        final FilePath fromFile = VcsUtil.getFilePath(touchFile(clientRoot, "abc.txt"));
        final FilePath toFile = VcsUtil.getFilePath(new File(clientRoot, "xyz.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot)
                .mapAsync((runner) ->
                        runner.withConnection(clientConfig, (client) -> {
                            List<IFileSpec> srcFiles = FileSpecBuildUtil.forFilePaths(fromFile);
                            List<IFileSpec> tgtFiles = FileSpecBuildUtil.forFilePaths(toFile);
                            List<IFileSpec> msgs = cmd.addFiles(client, srcFiles, null, null, null);
                            MessageStatusUtil.throwIfError(msgs);
                            IChangelist change = client.getServer().getChangelist(IChangelist.DEFAULT);
                            //P4ChangelistId clId = createChangelistId(change, clientConfig);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(client, null, null, change, Collections.singletonList(fromFile));
                            MessageStatusUtil.throwIfError(msgs);

                            // Test the message directly.
                            //MoveFileResult res = MoveFile.INSTANCE.moveFile(client, clientConfig,
                            //        new MoveFileAction(fromFile, toFile, clId));
                            //msgs = res.getServerMessages();
                            msgs = client.getServer().moveFile(
                                    srcFiles.get(0),
                                    tgtFiles.get(0),
                                    new MoveFileOptions());

                            // Note: if files are not open for edit, then this will report an INFO message that
                            // the files are not open for edit.
                            assertSize(1, msgs);
                            IServerMessage msg = msgs.get(0).getStatusMessage();
                            assertNotNull(msg);
                            assertTrue(msg.isWarning());
                            Iterator<ISingleServerMessage> it = msg.getAllMessages().iterator();
                            assertTrue(it.hasNext());
                            ISingleServerMessage m = it.next();
                            assertFalse(it.hasNext());
                            assertEquals(m.getMessageFormat(), "[%argc% - file(s)|File(s)] not opened on this client.");
                            assertEquals(382, m.getSubCode());
                            return null;
                        })
                )
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void moveMessage_targetExists_notOpen(TemporaryFolder tmpDir)
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
                            //P4ChangelistId clId = createChangelistId(change, clientConfig);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(client, null, null, change, Arrays.asList(fromFile, toFile));
                            MessageStatusUtil.throwIfError(msgs);

                            msgs = client.editFiles(srcFiles, new EditFilesOptions());
                            MessageStatusUtil.throwIfError(msgs);
                            assertTrue(toFile.getIOFile().setWritable(false));

                            // Test the message directly.
                            //MoveFileResult res = MoveFile.INSTANCE.moveFile(client, clientConfig,
                            //        new MoveFileAction(fromFile, toFile, clId));
                            //msgs = res.getServerMessages();
                            msgs = client.getServer().moveFile(
                                    srcFiles.get(0),
                                    tgtFiles.get(0),
                                    new MoveFileOptions());
                            // Note: if files are not open for edit, then this will report an INFO message that
                            // the files are not open for edit.
                            assertSize(1, msgs);
                            IServerMessage msg = msgs.get(0).getStatusMessage();
                            assertNotNull(msg);
                            assertTrue(msg.isInfo());
                            assertThat(msg.getAllInfoStrings(), containsString(" - is synced; use -f to force move"));
                            Iterator<ISingleServerMessage> it = msg.getAllMessages().iterator();
                            assertTrue(it.hasNext());
                            ISingleServerMessage m = it.next();
                            assertFalse(it.hasNext());
                            assertEquals(530, m.getSubCode());
                            return null;
                        })
                )
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void moveMessage_targetExists_notOpenForce(TemporaryFolder tmpDir)
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
        final FilePath fromFile = VcsUtil.getFilePath(touchFile(clientRoot, "abc.txt"));
        final FilePath toFile = VcsUtil.getFilePath(touchFile(clientRoot, "xyz.txt"));
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot, new MockP4RequestErrorHandler())
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
                            // P4ChangelistId clId = createChangelistId(change, clientConfig);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(client, null, null, change, Arrays.asList(fromFile, toFile));
                            MessageStatusUtil.throwIfError(msgs);

                            msgs = client.editFiles(srcFiles, new EditFilesOptions());
                            MessageStatusUtil.throwIfError(msgs);

                            // Even forcing the file to not-writable causes an issue.
                            assertTrue(toFile.getIOFile().setWritable(false));

                            // Test the message directly.
                            //MoveFileResult res = MoveFile.INSTANCE.moveFile(client, clientConfig,
                            //        new MoveFileAction(fromFile, toFile, clId));
                            msgs = client.getServer().moveFile(
                                    srcFiles.get(0),
                                    tgtFiles.get(0),
                                    new MoveFileOptions("-f"));
                            fail("Should have thrown an error, returned messages " + msgs);
                            return null;
                        })
                )
                .whenFailed((t) -> {
                    assertThat(t.getCause(), instanceOf(RequestException.class));
                    RequestException ex = (RequestException) t.getCause();
                    IServerMessage msg = ex.getServerMessage();
                    assertNotNull(msg);
                    assertTrue(msg.isError());
                    Iterator<ISingleServerMessage> it = msg.getAllMessages().iterator();
                    assertTrue(it.hasNext());
                    ISingleServerMessage m = it.next();
                    assertFalse(it.hasNext());
                    assertEquals(m.getMessageFormat(), "Can't clobber writable file %file%");
                    assertEquals(MessageSubsystemCode.ES_DB, m.getSubCode());
                });
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void moveMessage_targetExists_edit(TemporaryFolder tmpDir)
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
                            //P4ChangelistId clId = createChangelistId(change, clientConfig);
                            change.setDescription("add initial file");
                            msgs = cmd.submitChangelist(client, null, null, change, Arrays.asList(fromFile, toFile));
                            MessageStatusUtil.throwIfError(msgs);

                            // Open for edit source and target.
                            msgs = client.editFiles(allFiles, new EditFilesOptions());
                            MessageStatusUtil.throwIfError(msgs);

                            // Test the message directly.
                            //MoveFileResult res = MoveFile.INSTANCE.moveFile(client, clientConfig,
                            //        new MoveFileAction(fromFile, toFile, clId));
                            //msgs = res.getServerMessages();
                            msgs = client.getServer().moveFile(
                                    srcFiles.get(0),
                                    tgtFiles.get(0),
                                    // If tgtFiles are not open for edit, then this will fail with
                                    // tgt file is synced, use -f.
                                    // If -f is used, then the move operation fails because tgt is writable,
                                    // and it won't clobber writable files.
                                    new MoveFileOptions());

                            // Note: if files are not open for edit, then this will report an INFO message that
                            // the files are not open for edit.
                            assertSize(1, msgs);
                            IServerMessage msg = msgs.get(0).getStatusMessage();
                            assertNotNull(msg);
                            assertTrue(msg.isInfo());
                            assertThat(msg.getAllInfoStrings(), containsString(" - can't move to an existing file"));
                            Iterator<ISingleServerMessage> it = msg.getAllMessages().iterator();
                            assertTrue(it.hasNext());
                            ISingleServerMessage m = it.next();
                            assertFalse(it.hasNext());
                            assertEquals(IServerMessageCode.MOVE_EXISTS, m.getSubCode());
                            return null;
                        })

                )
                .whenFailed(Assertions::fail);
    }


    private static void setContents(FilePath file, String contents) throws IOException {
        try (FileWriter out = new FileWriter(file.getIOFile())) {
            out.write(contents);
        }
    }

    private static String getContents(FilePath file) throws IOException {
        try (FileReader inp = new FileReader(file.getIOFile())) {
            StringBuilder sb = new StringBuilder();
            char[] b = new char[4096];
            int len;
            while ((len = inp.read(b, 0, 4096)) > 0) {
                sb.append(b, 0, len);
            }
            return sb.toString();
        }
    }

    private P4ChangelistId createChangelistId(IChangelist change, ClientConfig clientConfig) {
        return new P4ChangelistIdImpl(change.getId(), clientConfig.getClientServerRef());
    }
}