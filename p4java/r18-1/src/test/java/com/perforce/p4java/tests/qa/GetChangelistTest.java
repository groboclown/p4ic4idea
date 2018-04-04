package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static com.perforce.p4java.core.IChangelistSummary.Visibility.PUBLIC;
import static com.perforce.p4java.core.IChangelistSummary.Visibility.RESTRICTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(JUnitPlatform.class)
public class GetChangelistTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;

    // a few changes
    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
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
        pendingChange.setDescription("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
        pendingChange.update();
        pendingChange.submit(false);

        pendingChange = helper.createChangelist(server, user, client);
        helper.editFile(testFile.getAbsolutePath(), "GetChangelistsTest", pendingChange, client);
        pendingChange.setVisibility(RESTRICTED);
        pendingChange.update();
        pendingChange.submit(false);

        pendingChange = helper.createChangelist(server, user, client);
        helper.editFile(testFile.getAbsolutePath(), "GetChangelistsTest", pendingChange, client);
        pendingChange.submit(false);
    }


    @Test
    public void basic() throws Throwable {
        IChangelist change = server.getChangelist(1);
        List<IFileSpec> files = change.getFiles(false);

        assertThat(change.getVisibility(), is(PUBLIC));
        assertThat("did not retrieve files", files, notNullValue());
        assertThat("wrong number of files", files.size(), is(1));
        assertTrue(change.getDescription().contains("Changelist for user"));
    }


    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}
