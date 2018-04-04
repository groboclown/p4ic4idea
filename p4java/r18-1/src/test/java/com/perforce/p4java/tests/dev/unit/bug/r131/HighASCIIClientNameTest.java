package com.perforce.p4java.tests.dev.unit.bug.r131;

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test high-ascii client name with non-unicode server.
 *
 * <pre>
 * Create a client with a high-ascii char (i.e umlaut).
 * setCurrentClient() to this client
 * client.where(...) which fails with:
 * Client 'xxx_<wrongchar>' unknown - use 'client' command to create it.
 * </pre>
 */
@RunWith(JUnitPlatform.class)
@Jobs({ "job060527" })
@TestId("Dev131_HighASCIIClientNameTest")
public class HighASCIIClientNameTest extends P4JavaTestCase {

    /** The server. */
    private IOptionsServer server = null;

    /**
     * Sets the up.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        server = getServer();
        assertThat(server, notNullValue());
        IClient client = getDefaultClient(server);
        assertThat(client, notNullValue());
        server.setCurrentClient(client);
    }

    /**
     * Tear down.
     */
    @AfterEach
    public void tearDown() {
        if (nonNull(server)) {
            endServerSession(server);
        }
    }

    /**
     * Test high ascii client name.
     *
     * @throws Exception the exception
     */
    @Test
    public void testHighASCIIClientName() throws Exception {
        Charset cs = Charset.defaultCharset();
        String clientName = cs.name().equals("UTF-8") ? "Test_job060527_ù_abcd"
                : "Test2_job060527_ù_abcd";
        final int count = 99;
        List<IClientSummary> clients = server.getClients("p4jtestuser2", null, count);
        assertThat(clients.size() > 0, is(true));

        String[] args = { "-o", clientName };
        Map<String, Object>[] resultsMap = server.execMapCmd(CmdSpec.CLIENT.toString(), args, null);
        assertThat(resultsMap, notNullValue());

        IClient testClient = server.getClient(clientName);
        assertThat(clientName + "is not a known client on "
                + server.getServerInfo().getServerAddress(), testClient != null);

        server.setCurrentClient(testClient);
        List<IFileSpec> files = FileSpecBuilder
                .makeFileSpecList("//depot/112Dev/Attributes/test03.txt");

        files = testClient.where(files);

        assertThat(files, notNullValue());
        assertThat(files.size() > 0, is(true));

        assertThat(files.get(0).getOpStatus(), is(FileSpecOpStatus.VALID));
    }
}
