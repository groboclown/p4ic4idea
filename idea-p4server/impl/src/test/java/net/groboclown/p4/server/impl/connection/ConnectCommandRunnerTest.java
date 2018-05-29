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
package net.groboclown.p4.server.impl.connection;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.exception.RequestException;
import com.perforce.test.P4ServerExtension;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.changelist.AddJobToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobResult;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistQuery;
import net.groboclown.p4.server.api.commands.changelist.EditChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserQuery;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.AddEditResult;
import net.groboclown.p4.server.api.commands.file.AnnotateFileQuery;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4JobField;
import net.groboclown.p4.server.impl.connection.impl.SimpleConnectionManager;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static net.groboclown.p4.server.impl.ClientTestUtil.setupClient;
import static net.groboclown.p4.server.impl.ClientTestUtil.touchFile;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


class ConnectCommandRunnerTest {
    private static final String JOB_ID = "job-123";
    private static final String JOB_DESCRIPTION = "this is the job description";

    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @RegisterExtension
    P4ServerExtension server = new P4ServerExtension(false);

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getJobSpec_CreateJob(TemporaryFolder tmpDir)
            throws IOException, InterruptedException {
        idea.useInlineThreading(null);
        final ServerConfig config = ServerConfig.createFrom(
                new MockConfigPart()
                    // By using the RSH port, it means that the connection will be kept open
                    // (NTS connection).  By keeping the connection open until explicitly
                    // disconnected, this will indirectly be testing that the
                    // SimpleConnectionManager closes the connection.
                    .withServerName(server.getRshUrl())
                    .withUsername(server.getUser())
                    .withNoPassword()
        );
        final MockP4RequestErrorHandler errorHandler = new MockP4RequestErrorHandler();
        SimpleConnectionManager mgr = new SimpleConnectionManager(
                tmpDir.newFile("out"), 1000, "v1",
                errorHandler);
        ConnectCommandRunner runner = new ConnectCommandRunner(mgr);
        final CreateJobResult[] result = new CreateJobResult[1];
        runner.getJobSpec(config)
                .mapActionAsync((jobSpec) ->
                    runner.perform(config, new CreateJobAction(createP4Job(config, jobSpec)))
                )
                .whenCompleted((res) -> result[0] = res)
                .whenServerError(Assertions::fail)
                .waitForCompletion(5, TimeUnit.SECONDS);
        assertNotNull(result[0]);
        assertEmpty(errorHandler.getExceptions());
        assertEmpty(errorHandler.getDisconnectExceptions());

        assertSame(config, result[0].getServerConfig());
        assertNotNull(result[0].getJob());
        assertEquals(JOB_ID, result[0].getJob().getJobId());
        assertNotNull(result[0].getJob().getDescription());
        // The server can add extra whitespace to the description.
        assertEquals(JOB_DESCRIPTION, result[0].getJob().getDescription().trim());
        assertEquals(config.getUsername(), result[0].getJob().getRawDetails().get("User"));
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void createJob_error(TemporaryFolder tmpDir)
            throws IOException {
        idea.useInlineThreading(null);
        final ServerConfig config = ServerConfig.createFrom(
                new MockConfigPart()
                        // By using the RSH port, it means that the connection will be kept open
                        // (NTS connection).  By keeping the connection open until explicitly
                        // disconnected, this will indirectly be testing that the
                        // SimpleConnectionManager closes the connection.
                        .withServerName(server.getRshUrl())
                        .withUsername(server.getUser())
                        .withNoPassword()
        );
        final MockP4RequestErrorHandler errorHandler = new MockP4RequestErrorHandler();
        SimpleConnectionManager mgr = new SimpleConnectionManager(
                tmpDir.newFile("out"), 1000, "v1",
                errorHandler);
        ConnectCommandRunner runner = new ConnectCommandRunner(mgr);
        // Should run without needing a blockingGet, because of the inline thread handler.
        runner.getJobSpec(config)
                .mapActionAsync((jobSpec) ->
                        // Do not set any expected details.
                        runner.perform(config, new CreateJobAction(new P4JobImpl("j", "x", null)))
                )
                .whenCompleted((x) -> fail("Did not throw an error"))
                .whenServerError((ex) -> {
                    assertNotNull(ex);
                    assertThat(ex.getCause(), instanceOf(RequestException.class));
                    // FIXME add better assertions
                });
        assertSize(1, errorHandler.getExceptions());
        assertThat(errorHandler.getExceptions().get(0), instanceOf(RequestException.class));
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void addEditFile_add(TemporaryFolder tmpDir) throws IOException {
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

        setupClient(clientConfig, tmpDir, clientRoot)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                    runner.perform(clientConfig, new AddEditAction(newFile, null, null, null))
                        .whenCompleted(sink::resolve)
                        .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertNotNull(r);
                    assertThat(r, instanceOf(AddEditResult.class));
                    AddEditResult res = (AddEditResult) r;
                    assertEquals(newFile, res.getFile());
                    assertTrue(res.isAdd());
                    assertFalse(res.isEdit());
                    assertNotNull(res.getFileType());
                    assertEquals("text", res.getFileType().toString());
                    assertTrue(res.getChangelistId().isDefaultChangelist());
                    assertSame(clientConfig, res.getClientConfig());
                    assertEquals("//depot/abc.txt", res.getDepotPath().getDepotPath());
                    assertEquals("//depot/abc.txt", res.getDepotPath().getDisplayName());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void addEditFile_addWildcard(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));

        setupClient(clientConfig, tmpDir, clientRoot)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddEditAction(newFile, null, null, null))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertNotNull(r);
                    assertThat(r, instanceOf(AddEditResult.class));
                    AddEditResult res = (AddEditResult) r;
                    assertEquals(newFile, res.getFile());
                    assertTrue(res.isAdd());
                    assertFalse(res.isEdit());
                    assertNotNull(res.getFileType());
                    assertEquals("text", res.getFileType().toString());
                    assertTrue(res.getChangelistId().isDefaultChangelist());
                    assertSame(clientConfig, res.getClientConfig());
                    assertEquals("//depot/a%40b.txt", res.getDepotPath().getDepotPath());
                    assertEquals("//depot/a@b.txt", res.getDepotPath().getDisplayName());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void addEditFile_notInClient(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(tmpDir.newFile("outsideRoot"), "abc.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddEditAction(newFile, null, null, null))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> fail("Should have caused a problem; instead it returned " + r))
                .whenFailed((t) -> {
                    P4CommandRunner.ResultError re = t.getResultError();
                    assertEquals(P4CommandRunner.ErrorCategory.SERVER_ERROR, re.getCategory());
                    assertTrue(re.getMessage().isPresent());
                    assertThat(re.getMessage().get(), containsString(" is not under client's root "));
                });
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void addEditFile_editWildcard(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));

        setupClient(clientConfig, tmpDir, clientRoot)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddEditAction(newFile, null, null, null))
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(-1, clientConfig.getClientServerRef()),
                                                null, "add file", null)))
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new AddEditAction(newFile, null, null, null)))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertNotNull(r);
                    assertThat(r, instanceOf(AddEditResult.class));
                    AddEditResult res = (AddEditResult) r;
                    assertEquals(newFile, res.getFile());
                    assertFalse(res.isAdd());
                    assertTrue(res.isEdit());
                    assertNotNull(res.getFileType());
                    assertEquals("text", res.getFileType().toString());
                    assertTrue(res.getChangelistId().isDefaultChangelist());
                    assertSame(clientConfig, res.getClientConfig());
                    assertEquals("//depot/a%40b.txt", res.getDepotPath().getDepotPath());
                    assertEquals("//depot/a@b.txt", res.getDepotPath().getDisplayName());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void submit_noDescription(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddEditAction(newFile, null, null, null))
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(-1, clientConfig.getClientServerRef()),
                                                null, null, null)))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> fail("Should have caused a problem; instead it returned " + r))
                .whenFailed((t) -> {
                    P4CommandRunner.ResultError re = t.getResultError();
                    assertEquals(P4CommandRunner.ErrorCategory.SERVER_ERROR, re.getCategory());
                    assertTrue(re.getMessage().isPresent());
                    assertThat(re.getMessage().get(), containsString("Change description missing.  You must enter one."));
                });
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void addJobToChangelist(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddJobToChangelistAction(
                                    new P4ChangelistIdImpl(2, clientConfig.getClientServerRef()),
                                    new P4JobImpl("a", "a job", null)))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void createChangelist(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig,
                                new CreateChangelistAction(clientConfig.getClientServerRef(), "simple"))
                        .whenCompleted(sink::resolve)
                        .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void deleteChangelist(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new DeleteChangelistAction(new P4ChangelistIdImpl(2,
                                    clientConfig.getClientServerRef())))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void deleteFile(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new DeleteFileAction(newFile))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void editChangelistDescription(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new EditChangelistAction(
                                    new P4ChangelistIdImpl(2, clientConfig.getClientServerRef()), "new comment"))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void fetchFiles(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new FetchFilesAction())
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void moveFile(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new MoveFileAction(newFile, newFile))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void moveFilesToChangelist(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new MoveFilesToChangelistAction(
                                    new P4ChangelistIdImpl(4, clientConfig.getClientServerRef()),
                                    Collections.emptyList()))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void revertFile(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new RevertFileAction(newFile))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getFileAnnotation(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.getFileAnnotation(serverConfig, new AnnotateFileQuery())
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void describeChangelist(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.describeChangelist(serverConfig, new DescribeChangelistQuery(
                                new P4ChangelistIdImpl(-1, clientConfig.getClientServerRef())))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getClientsForUser(TemporaryFolder tmpDir) throws IOException {
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
        final FilePath newFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@b.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map(ConnectCommandRunner::new)
                .futureMap((runner, sink) ->
                        runner.getClientsForUser(serverConfig, new ListClientsForUserQuery("not-a-user", 50))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // FIXME
                    fail("Add tests");
                })
                .whenFailed(Assertions::fail);
    }


    private P4Job createP4Job(ServerConfig config, GetJobSpecResult jobSpec) {
        assertNotNull(jobSpec);
        Map<String, Object> details = new HashMap<>();
        for (P4JobField jobField : jobSpec.getJobSpec().getFields()) {
            if (jobField.getSelectValues() != null && !jobField.getSelectValues().isEmpty()) {
                details.put(jobField.getName(), jobField.getSelectValues().get(0));
            } else if (jobField.getPreset() == null) {
                details.put(jobField.getName(), "unknown");
            }
        }
        details.put("User", config.getUsername());
        return new P4JobImpl(JOB_ID, JOB_DESCRIPTION, details);
    }

}