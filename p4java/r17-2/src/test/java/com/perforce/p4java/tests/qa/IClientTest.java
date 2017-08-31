package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class IClientTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IUser otherUser = null;
    private static IClient client = null;
    private static IClient lockedClient = null;
    private static File testFile = null;

    // simple setup with one file
    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());
        otherUser = h.createUser(server, "otherUser");
        server.setUserName(ts.getUser());

        client = h.createClient(server, "client1");
        lockedClient = h.createClient(server, "lockedClient");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "IChangelistTest", "text");
    }

    @Before
    public void Setup() {
        try {
            // reset the locked client as otherUser
            server.setUserName(otherUser.getLoginName());
            ClientOptions opts = new ClientOptions();
            lockedClient.setOwnerName(otherUser.getLoginName());
            opts.setLocked(true);
            lockedClient.setOptions(opts);
            lockedClient.update();
            server.setUserName(ts.getUser());
        } catch (Throwable t) {

            h.error(t);

        }
    }

    // verify that we can force a change to a locked client
    @Test
    public void updateLockedClient() throws Throwable {
        server.setUserName(user.getLoginName());
        lockedClient.setDescription("forced description");
        lockedClient.update(true);

        lockedClient.refresh();

        assertThat("forced change didn't work", lockedClient.getDescription(), containsString("forced description"));
    }

    // verify that we can force a change to a locked client
    @Test
    public void updateLockedClient2() throws Throwable {
        lockedClient.setDescription("forced description");
        server.updateClient(lockedClient, true);

        lockedClient.refresh();

        assertThat("forced change didn't work", lockedClient.getDescription(), containsString("forced description"));
    }

    // verify that we can force a change to a locked client
    @Test
    public void failedUpdateLockedClient() {
        try {
            lockedClient.setDescription("forced description");
            lockedClient.update(false);

            fail("update should have failed");

        } catch (Throwable t) {

            assertThat("did not get force error", t.getLocalizedMessage(), containsString("Locked client 'lockedClient' owned by 'otherUser'; use -f to force update"));

        }
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	