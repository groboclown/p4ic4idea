package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
@Disabled("We no longer support 2010.1.")
@RunWith(JUnitPlatform.class)
public class GetChangelists101Test {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;

    // server setup, nothing fancy
    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline("p10.1");
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "GetChangelistsTest", "text");

        IChangelist pendingChange = helper.createChangelist(server, user, client);
        helper.editFile(testFile.getAbsolutePath(), "GetChangelistsTest", pendingChange, client);
        pendingChange.submit(false);

        pendingChange = helper.createChangelist(server, user, client);
        helper.editFile(testFile.getAbsolutePath(), "GetChangelistsTest", pendingChange, client);
        pendingChange.submit(false);
    }

    @Test
    public void basic() throws Throwable {
        List<IChangelistSummary> changes = server.getChangelists(null, null);

        assertThat("wrong number of changes", changes.size(), is(3));
        assertThat(changes.get(0).getVisibility(), nullValue());
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}