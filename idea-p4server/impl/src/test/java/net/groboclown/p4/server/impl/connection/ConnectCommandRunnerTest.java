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

import com.perforce.p4java.exception.RequestException;
import com.perforce.test.P4ServerExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobResult;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecQuery;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4JobField;
import net.groboclown.p4.server.impl.connection.impl.SimpleConnectionManager;
import net.groboclown.p4.server.impl.p4.MockCacheQueryHandler;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import org.jetbrains.concurrency.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;


class ConnectCommandRunnerTest {
    private static final String JOB_ID = "job-123";
    private static final String JOB_DESCRIPTION = "this is the job description";
    @RegisterExtension
    P4ServerExtension server = new P4ServerExtension(false);

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getJobSpec_CreateJob(TemporaryFolder tmpDir)
            throws IOException {
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
        ConnectCommandRunner runner = new ConnectCommandRunner(mgr, new MockCacheQueryHandler());
        Promise<CreateJobResult> res = runner.query(config, new GetJobSpecQuery())
                .thenAsync((jobSpec) ->
                    runner.perform(config, new CreateJobAction(createP4Job(config, jobSpec)))
                )
                .rejected(Assertions::fail);

        CreateJobResult result = res.blockingGet(1000);
        assertNotNull(result);
        assertEmpty(errorHandler.getExceptions());
        assertEmpty(errorHandler.getDisconnectExceptions());

        assertSame(config, result.getServerConfig());
        assertNotNull(result.getJob());
        assertEquals(JOB_ID, result.getJob().getJobId());
        assertNotNull(result.getJob().getDescription());
        // The server can add extra whitespace to the description.
        assertEquals(JOB_DESCRIPTION, result.getJob().getDescription().trim());
        assertEquals(config.getUsername(), result.getJob().getRawDetails().get("User"));
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void createJob_error(TemporaryFolder tmpDir)
            throws IOException {
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
        ConnectCommandRunner runner = new ConnectCommandRunner(mgr, new MockCacheQueryHandler());
        runner.query(config, new GetJobSpecQuery())
                .thenAsync((jobSpec) ->
                        // Do not set any expected details.
                        runner.perform(config, new CreateJobAction(new P4JobImpl("j", "x", null)))
                )
                .then((x) -> {
                    fail("Did not throw an error");
                    return null;
                })
                .rejected((ex) -> {
                    assertThat(ex.getCause(), instanceOf(RequestException.class));
                    // FIXME better checks
                });
        assertSize(1, errorHandler.getExceptions());
        assertThat(errorHandler.getExceptions().get(0), instanceOf(RequestException.class));
    }

    private P4Job createP4Job(ServerConfig config, GetJobSpecResult jobSpec) {
        assertNotNull(jobSpec);
        Map<String, String> details = new HashMap<>();
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