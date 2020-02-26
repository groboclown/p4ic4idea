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
import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.test.P4ServerExtension;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.changelist.AddJobToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.AddJobToChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobResult;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistQuery;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.EditChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.EditChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserQuery;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserResult;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.AddEditResult;
import net.groboclown.p4.server.api.commands.file.AnnotateFileQuery;
import net.groboclown.p4.server.api.commands.file.AnnotateFileResult;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.DeleteFileResult;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesResult;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileResult;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.api.commands.file.RevertFileResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4AnnotatedLine;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4JobField;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.connection.impl.MessageStatusUtil;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.connection.impl.SimpleConnectionManager;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static net.groboclown.p4.server.impl.ClientTestUtil.setupClient;
import static net.groboclown.p4.server.impl.ClientTestUtil.touchFile;
import static net.groboclown.p4.server.impl.ClientTestUtil.withConnection;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;


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
        ConnectCommandRunner runner = new ConnectCommandRunner(idea.getMockProject(), mgr);
        final CreateJobResult[] result = new CreateJobResult[1];
        runner.getJobSpec(new OptionalClientServerConfig(config, null))
                .mapActionAsync((jobSpec) ->
                    runner.perform(new OptionalClientServerConfig(config, null),
                            new CreateJobAction(createP4Job(config, jobSpec)))
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
        ConnectCommandRunner runner = new ConnectCommandRunner(idea.getMockProject(), mgr);
        // Should run without needing a blockingGet, because of the inline thread handler.
        runner.getJobSpec(new OptionalClientServerConfig(config, null))
                .mapActionAsync((jobSpec) ->
                        // Do not set any expected details.
                        runner.perform(new OptionalClientServerConfig(config, null),
                                new CreateJobAction(new P4JobImpl("j", "x", null)))
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());

        setupClient(clientConfig, tmpDir, clientRoot)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                    runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());

        setupClient(clientConfig, tmpDir, clientRoot)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());

        setupClient(clientConfig, tmpDir, clientRoot)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, "add file", null)))
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new AddEditAction(newFile, null, defaultId, (String) null)))
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
                    assertEquals(IChangelist.DEFAULT, res.getChangelistId().getChangelistId());
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, null, null)))
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
        touchFile(clientRoot, "a@b.txt");
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());
        final Map<String, Object> jobDetails = new HashMap<>();
        jobDetails.put("Status", "closed");
        jobDetails.put("User", serverConfig.getUsername());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(new OptionalClientServerConfig(clientConfig),
                                new CreateJobAction(new P4JobImpl("j1", "x", jobDetails)))
                        .mapActionAsync((res) ->
                                runner.perform(clientConfig,
                                    new CreateChangelistAction(clientConfig.getClientServerRef(), "a change",
                                            "local-id")))
                        .mapActionAsync((res) ->
                                runner.perform(clientConfig, new AddJobToChangelistAction(
                                    new P4ChangelistIdImpl(res.getChangelistId(), clientConfig.getClientServerRef()),
                                    new P4JobImpl("j1", "a job", null)))
                                )
                        .whenCompleted(sink::resolve)
                        .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(AddJobToChangelistResult.class));
                    AddJobToChangelistResult res = (AddJobToChangelistResult) r;

                    assertSame(clientConfig, res.getClientConfig());
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
        touchFile(clientRoot, "a@b.txt");
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig,
                                new CreateChangelistAction(clientConfig.getClientServerRef(), "simple",
                                        "local-id"))
                        .whenCompleted(sink::resolve)
                        .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(CreateChangelistResult.class));
                    CreateChangelistResult res = (CreateChangelistResult) r;
                    assertSame(clientConfig, res.getClientConfig());
                    assertEquals(1, res.getChangelistId());
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
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig,
                                new CreateChangelistAction(clientConfig.getClientServerRef(), "new change",
                                        "local-id"))
                            .mapActionAsync((res) ->
                                runner.perform(clientConfig, new DeleteChangelistAction(new P4ChangelistIdImpl(res.getChangelistId(),
                                        clientConfig.getClientServerRef())))
                            )
                            .whenCompleted(sink::resolve)
                            .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(DeleteChangelistResult.class));
                    DeleteChangelistResult res = (DeleteChangelistResult) r;

                    assertSame(clientConfig, res.getClientConfig());
                    // TODO remove language-dependent fragility
                    assertEquals("Change 1 deleted.", res.getMessage());
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
        final List<IFileSpec> specs = FileSpecBuildUtil.forFilePaths(newFile);
        final P4CommandUtil cmd = new P4CommandUtil();

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .mapAsync((runner) ->
                    runner.withConnection(clientConfig, (client) -> {
                        List<IFileSpec> msgs = cmd.addFiles(client, specs, null, null, null);
                        MessageStatusUtil.throwIfError(msgs);
                        IChangelist change = client.getServer().getChangelist(IChangelist.DEFAULT);
                        change.setDescription("add initial file");
                        msgs = cmd.submitChangelist(client, null, null, change, Collections.singletonList(newFile));
                        MessageStatusUtil.throwIfError(msgs);
                        return runner;
                    })
                )
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new DeleteFileAction(newFile,
                                    new P4ChangelistIdImpl(0, clientConfig.getClientServerRef())))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(DeleteFileResult.class));
                    DeleteFileResult res = (DeleteFileResult) r;

                    assertSame(clientConfig, res.getClientConfig());
                    assertEquals("", res.getMessage());
                    assertSize(1, res.getFiles());
                    P4RemoteFile remote = res.getFiles().get(0);
                    assertEquals("//depot/a@b.txt", remote.getDisplayName());
                    assertEquals("//depot/a%40b.txt", remote.getDepotPath());
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
        touchFile(clientRoot, "a@b.txt");
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig,
                                new CreateChangelistAction(clientConfig.getClientServerRef(),"old comment",
                                        "local-id"))
                        .mapActionAsync((res) ->
                                runner.perform(clientConfig, new EditChangelistAction(
                                    new P4ChangelistIdImpl(res.getChangelistId(), clientConfig.getClientServerRef()), "new comment")))
                        .whenCompleted(sink::resolve)
                        .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(EditChangelistResult.class));
                    EditChangelistResult res = (EditChangelistResult) r;

                    assertSame(clientConfig, res.getClientConfig());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void fetchFiles_noFiles(TemporaryFolder tmpDir) throws IOException {
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
        touchFile(clientRoot, "a@b.txt");
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig,
                                    new FetchFilesAction(Collections.singletonList(VcsUtil.getFilePath(clientRoot)),
                                            null, false))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    // Special code is present to handle an empty sync.
                    FetchFilesResult res = (FetchFilesResult) r;
                    assertEquals("", res.getMessage());
                    assertEmpty(res.getFiles());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void moveFile_notOnServer(TemporaryFolder tmpDir) throws IOException {
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
        assertNotNull(clientConfig);
        final File clientRoot = tmpDir.newFile("clientRoot");
        final FilePath srcFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@src.txt"));
        final FilePath tgtFile = VcsUtil.getFilePath(touchFile(clientRoot, "a@tgt.txt"));
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        // Perform a move operation, but with neither the source nor the target being on the
        // server.  This means the command should perform an add action for the target file.

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new MoveFileAction(srcFile, tgtFile,
                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef())))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertNotNull(r);
                    assertThat(r, instanceOf(MoveFileResult.class));
                    MoveFileResult mfr = (MoveFileResult) r;
                    assertSame(clientConfig, mfr.getClientConfig());

                    // We didn't create the target file, so this message comes back.
                    // TODO remove language-dependent fragility
                    assertThat(mfr.getMessages(), containsString("a%40tgt.txt - missing, assuming text"));
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                                runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
                                        .mapActionAsync((res) -> runner.perform(clientConfig,
                                                new CreateChangelistAction(clientConfig.getClientServerRef(),
                                                        "Destination change", "local-id")))
                                        .mapActionAsync((res) -> runner.perform(clientConfig,
                                                new MoveFilesToChangelistAction(
                                                    new P4ChangelistIdImpl(res.getChangelistId(), clientConfig.getClientServerRef()),
                                                    Collections.singletonList(newFile))))
                                        .whenCompleted(sink::resolve)
                                        .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(MoveFilesToChangelistResult.class));
                    MoveFilesToChangelistResult res = (MoveFilesToChangelistResult) r;

                    assertSame(clientConfig, res.getClientConfig());
                    assertNull(res.getMessage());
                    assertSize(1, res.getFiles());
                    P4RemoteFile remote = res.getFiles().get(0);
                    assertEquals("//depot/a@b.txt", remote.getDisplayName());
                    assertEquals("//depot/a%40b.txt", remote.getDepotPath());
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
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new RevertFileAction(newFile, false))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(RevertFileResult.class));
                    RevertFileResult res = (RevertFileResult) r;

                    assertSame(clientConfig, res.getClientConfig());
                    assertEquals(newFile, res.getRevertedFile());
                    assertSize(1, res.getResults());

                    IFileSpec message = res.getResults().get(0);
                    assertEquals(FileSpecOpStatus.INFO, message.getOpStatus());
                    IServerMessage msg = message.getStatusMessage();
                    assertNotNull(msg);
                    assertEquals(6526, msg.getUniqueCode());
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());
        assertNotNull(clientConfig.getClientname());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, "add file", null)))
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new AddEditAction(newFile, null, defaultId, (String) null)))
                                .mapQueryAsync((res) ->
                                        runner.getFileAnnotation(clientConfig,
                                            new AnnotateFileQuery(newFile, 1)))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(AnnotateFileResult.class));
                    AnnotateFileResult res = (AnnotateFileResult) r;

                    assertSame(clientConfig, res.getClientConfig());
                    assertEquals("x", res.getContent());
                    assertEquals(1, res.getHeadRevision().getRevision().getValue());
                    assertSize(1, res.getAnnotatedFile().getAnnotatedLines());
                    P4AnnotatedLine line = res.getAnnotatedFile().getAnnotatedLines().get(0);
                    assertEquals("//depot/a@b.txt", line.getDepotPath().getDisplayName());
                    assertEquals("//depot/a%40b.txt", line.getDepotPath().getDepotPath());
                    assertEquals(1, line.getRevNumber());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void describeChangelist_defaultEmpty(TemporaryFolder tmpDir) throws IOException {
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
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.describeChangelist(new OptionalClientServerConfig(clientConfig), new DescribeChangelistQuery(
                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef())))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(DescribeChangelistResult.class));
                    DescribeChangelistResult res = (DescribeChangelistResult) r;

                    assertSame(serverConfig, res.getServerConfig());
                    assertEquals(clientConfig.getClientServerRef(), res.getRequestedChangelist().getClientServerRef());
                    assertEquals(0, res.getRequestedChangelist().getChangelistId());
                    assertFalse(res.isFromCache());

                    P4RemoteChangelist remote = res.getRemoteChangelist();
                    assertNotNull(remote);
                    assertEquals(res.getRequestedChangelist(), remote.getChangelistId());
                    assertEmpty(remote.getAttachedJobs());
                    assertEmpty(remote.getFiles());
                    assertNotNull(remote.getSubmittedDate());
                    assertEquals(clientConfig.getClientname(), remote.getClientname());
                    assertEquals(serverConfig.getUsername(), remote.getUsername());

                    // TODO not very good check.  Too dependent on language.
                    assertEquals("<enter description here>\n", remote.getComment());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getClientsForUser_nonExistent(TemporaryFolder tmpDir) throws IOException {
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
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());

        setupClient(clientConfig, tmpDir, clientRoot, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.getClientsForUser(new OptionalClientServerConfig(clientConfig),
                                new ListClientsForUserQuery("not-a-user", 50))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertThat(r, instanceOf(ListClientsForUserResult.class));
                    ListClientsForUserResult res = (ListClientsForUserResult) r;
                    assertSame(serverConfig, res.getServerConfig());
                    assertEquals("not-a-user", res.getRequestedUser());
                    assertEmpty(res.getClients());
                })
                .whenFailed(Assertions::fail);
    }


    // Simulates the "try online check" operation.
    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getClientsForUser_passwordNotGiven(TemporaryFolder tmpDir)
            throws IOException, InterruptedException {
        idea.useInlineThreading(null);
        final MockConfigPart setupPart = new MockConfigPart()
                .withServerName(server.getRshUrl())
                .withUsername(server.getUser())
                .withNoPassword();
        final ServerConfig setupServerConfig = ServerConfig.createFrom(setupPart);
        final File clientRoot = tmpDir.newFile("clientRoot");

        // Create the client manually and set the password
        assertTrue(withConnection(setupServerConfig, tmpDir)
                .mapAsync((mgr) -> mgr.withConnection(new OptionalClientServerConfig(setupServerConfig, null),
                        server -> {
                    CoreFactory.createClient(
                            server, "client1", "new client from CoreFactory",
                            clientRoot.getAbsolutePath(), new String[]{"//depot/... //client1/..."},
                            true);
                    return mgr;
                }))
                .mapAsync((mgr) -> mgr.withConnection(new OptionalClientServerConfig(setupServerConfig, null),
                server -> {
                    String res = server.changePassword(null, "x", server.getUserName());
                    System.err.println("Password change: [" + res + "]");
                    return null;
                }))
                .blockingWait(5, TimeUnit.SECONDS));

        // Continue on, with no password set.
        final MockConfigPart part = new MockConfigPart()
                .withServerName(server.getRshUrl())
                .withUsername(server.getUser())
                .withNoPassword()
                .withClientname("client1");
        final ServerConfig serverConfig = ServerConfig.createFrom(part);
        final ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, part);
        final TestableP4RequestErrorHandler errorHandler = new TestableP4RequestErrorHandler(idea.getMockProject());


        withConnection(clientConfig.getServerConfig(), tmpDir, errorHandler)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        runner.getClientsForUser(new OptionalClientServerConfig(clientConfig),
                                new ListClientsForUserQuery(server.getUser(), 1))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> fail("Did not fail with password error"))
                .whenFailed((e) -> {
                    assertEquals(e.getResultError().getCategory(), P4CommandRunner.ErrorCategory.ACCESS_DENIED);
                    assertEquals(e.getLocalizedMessage(), e.getLocalizedMessage());
                    assertThat(e.getResultError().getMessage().orElse(null),
                            containsString(e.getLocalizedMessage()));
                });
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void listFilesDetails_have_is_head(TemporaryFolder tmpDir) throws IOException {
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());
        final int[] committedChangelistId = { 0 };

        setupClient(clientConfig, tmpDir, clientRoot)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        // Add the file and submit, so that head == have.
                        runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, "add file", null)))
                                .whenCompleted(res -> committedChangelistId[0] = res.getChangelistId().getChangelistId())
                                .mapQueryAsync((res) -> runner.listFilesDetails(clientConfig, new ListFilesDetailsQuery(
                                                Collections.singletonList(newFile.getParentPath()),
                                                ListFilesDetailsQuery.RevState.HAVE, 100)))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertNotNull(r);
                    assertThat(r, instanceOf(ListFilesDetailsResult.class));
                    ListFilesDetailsResult res = (ListFilesDetailsResult) r;
                    assertSame(clientConfig, res.getClientConfig());
                    assertSize(1, res.getFiles());
                    assertEquals(1, res.getFiles().size());

                    P4FileRevision details = res.getFiles().get(0);
                    assertNotNull(details.getDate());

                    // Should have been the first check-in.
                    assertNotNull(details.getChangelistId());
                    assertEquals(committedChangelistId[0], details.getChangelistId().getChangelistId());
                    assertEquals(1, details.getRevision().getLongRevisionNumber());

                    assertEquals(P4FileAction.ADD, details.getFileAction());
                    assertEquals(P4FileType.BaseType.TEXT, details.getFileType().getBaseType());
                    assertNull(details.getIntegratedFrom());

                    P4RemoteFile file = details.getFile();
                    assertNotNull(file);
                    assertEquals("//depot/a%40b.txt", file.getDepotPath());
                    assertEquals("//depot/a@b.txt", file.getDisplayName());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void listFilesDetails_have_not_head(TemporaryFolder tmpDir) throws IOException {
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());
        final int[] committedChangelistId = { 0, 0 };

        setupClient(clientConfig, tmpDir, clientRoot)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        // Add the file, create a second revision, and fetch the first, so that
                        // have is different than head.
                        runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
                                // Submit the add
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, "add file", null)))
                                .whenCompleted(res -> committedChangelistId[0] = res.getChangelistId().getChangelistId())
                                // Open for edit
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new AddEditAction(newFile, null, defaultId, (String) null)))
                                // Submit the open file
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, "add file", null)))
                                .whenCompleted(res -> committedChangelistId[1] = res.getChangelistId().getChangelistId())
                                // Fetch the first submit version
                                .mapActionAsync(res -> runner.perform(
                                        clientConfig, new FetchFilesAction(Collections.singletonList(newFile),
                                                "@" + committedChangelistId[0], false)))
                                // Get the have files
                                .mapQueryAsync((res) -> runner.listFilesDetails(
                                        clientConfig, new ListFilesDetailsQuery(
                                                Collections.singletonList(newFile.getParentPath()),
                                                ListFilesDetailsQuery.RevState.HAVE, 100)))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertNotNull(r);
                    assertThat(r, instanceOf(ListFilesDetailsResult.class));
                    ListFilesDetailsResult res = (ListFilesDetailsResult) r;
                    assertSame(clientConfig, res.getClientConfig());
                    assertSize(1, res.getFiles());
                    assertEquals(1, res.getFiles().size());

                    P4FileRevision details = res.getFiles().get(0);
                    assertNotNull(details.getDate());

                    // Should have been the first check-in.
                    assertNotNull(details.getChangelistId());
                    assertEquals(committedChangelistId[0], details.getChangelistId().getChangelistId());
                    assertEquals(1, details.getRevision().getLongRevisionNumber());

                    assertEquals(P4FileAction.ADD, details.getFileAction());
                    assertEquals(P4FileType.BaseType.TEXT, details.getFileType().getBaseType());
                    assertNull(details.getIntegratedFrom());

                    P4RemoteFile file = details.getFile();
                    assertNotNull(file);
                    assertEquals("//depot/a%40b.txt", file.getDepotPath());
                    assertEquals("//depot/a@b.txt", file.getDisplayName());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void listFilesDetails_head_have_head(TemporaryFolder tmpDir) throws IOException {
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());
        final int[] committedChangelistId = { 0, 0 };

        setupClient(clientConfig, tmpDir, clientRoot)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        // Open for Add
                        runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
                                // Submit the add
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, "add file", null)))
                                .whenCompleted(res -> committedChangelistId[0] = res.getChangelistId().getChangelistId())
                                // Open for edit
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new AddEditAction(newFile, null, defaultId, (String) null)))
                                // Submit the open file
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, "add file", null)))
                                .whenCompleted(res -> committedChangelistId[1] = res.getChangelistId().getChangelistId())
                                .mapQueryAsync((res) -> runner.listFilesDetails(
                                        clientConfig, new ListFilesDetailsQuery(
                                                Collections.singletonList(newFile.getParentPath()),
                                                ListFilesDetailsQuery.RevState.HEAD, 100)))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertNotNull(r);
                    assertThat(r, instanceOf(ListFilesDetailsResult.class));
                    ListFilesDetailsResult res = (ListFilesDetailsResult) r;
                    assertSame(clientConfig, res.getClientConfig());
                    assertSize(1, res.getFiles());
                    assertEquals(1, res.getFiles().size());

                    P4FileRevision details = res.getFiles().get(0);
                    assertNotNull(details.getDate());

                    // Should have been the second check-in.
                    assertNotNull(details.getChangelistId());
                    assertEquals(committedChangelistId[1], details.getChangelistId().getChangelistId());
                    assertEquals(2, details.getRevision().getLongRevisionNumber());

                    assertEquals(P4FileAction.EDIT, details.getFileAction());
                    assertEquals(P4FileType.BaseType.TEXT, details.getFileType().getBaseType());
                    assertNull(details.getIntegratedFrom());

                    P4RemoteFile file = details.getFile();
                    assertNotNull(file);
                    assertEquals("//depot/a%40b.txt", file.getDepotPath());
                    assertEquals("//depot/a@b.txt", file.getDisplayName());
                })
                .whenFailed(Assertions::fail);
    }


    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void listFilesDetails_head_have_not_head(TemporaryFolder tmpDir) throws IOException {
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
        final P4ChangelistId defaultId = new P4ChangelistIdImpl(0, clientConfig.getClientServerRef());
        final int[] committedChangelistId = { 0, 0 };

        setupClient(clientConfig, tmpDir, clientRoot)
                .map((cm) -> new ConnectCommandRunner(idea.getMockProject(), cm))
                .futureMap((runner, sink) ->
                        // Open for Add
                        runner.perform(clientConfig, new AddEditAction(newFile, null, defaultId, (String) null))
                                // Submit the add
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, "add file", null)))
                                .whenCompleted(res -> committedChangelistId[0] = res.getChangelistId().getChangelistId())
                                // Open for edit
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new AddEditAction(newFile, null, defaultId, (String) null)))
                                // Submit the open file
                                .mapActionAsync((res) -> runner.perform(
                                        clientConfig, new SubmitChangelistAction(
                                                new P4ChangelistIdImpl(0, clientConfig.getClientServerRef()),
                                                Collections.singletonList(newFile), null, "add file", null)))
                                .whenCompleted(res -> committedChangelistId[1] = res.getChangelistId().getChangelistId())
                                // Fetch the first submit version
                                .mapActionAsync(res -> runner.perform(
                                        clientConfig, new FetchFilesAction(Collections.singletonList(newFile),
                                                "@" + committedChangelistId[0], false)))
                                .mapQueryAsync((res) -> runner.listFilesDetails(
                                        clientConfig, new ListFilesDetailsQuery(
                                                Collections.singletonList(newFile.getParentPath()),
                                                ListFilesDetailsQuery.RevState.HEAD, 100)))
                                .whenCompleted(sink::resolve)
                                .whenServerError(sink::reject)
                )
                .whenCompleted((r) -> {
                    assertNotNull(r);
                    assertThat(r, instanceOf(ListFilesDetailsResult.class));
                    ListFilesDetailsResult res = (ListFilesDetailsResult) r;
                    assertSame(clientConfig, res.getClientConfig());
                    assertSize(1, res.getFiles());
                    assertEquals(1, res.getFiles().size());

                    P4FileRevision details = res.getFiles().get(0);
                    assertNotNull(details.getDate());

                    // Should have been the second check-in.
                    assertNotNull(details.getChangelistId());
                    assertEquals(committedChangelistId[1], details.getChangelistId().getChangelistId());
                    assertEquals(2, details.getRevision().getLongRevisionNumber());

                    assertEquals(P4FileAction.EDIT, details.getFileAction());
                    assertEquals(P4FileType.BaseType.TEXT, details.getFileType().getBaseType());
                    assertNull(details.getIntegratedFrom());

                    P4RemoteFile file = details.getFile();
                    assertNotNull(file);
                    assertEquals("//depot/a%40b.txt", file.getDepotPath());
                    assertEquals("//depot/a@b.txt", file.getDisplayName());
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
