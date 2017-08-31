package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static com.perforce.p4java.common.base.StringHelper.format;
import static com.perforce.p4java.core.IChangelistSummary.Visibility.PUBLIC;
import static com.perforce.p4java.core.IChangelistSummary.Visibility.RESTRICTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(JUnitPlatform.class)
public class GetChangelistsTest {

    private static final String PENDING_DESCRIPTION_MORE_THAN_250 = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
    private static final String CLIENT_NAME = "client1";

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;

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

        IClient client = helper.createClient(server, CLIENT_NAME);
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "GetChangelistsTest", "text");

        IChangelist pendingChange = helper.createChangelist(server, user, client);
        helper.editFile(testFile.getAbsolutePath(), "GetChangelistsTest", pendingChange, client);
        pendingChange.setDescription(PENDING_DESCRIPTION_MORE_THAN_250);
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

    @DisplayName("Default changelist descriptions use 31 characters")
    @Test
    public void basic() throws Throwable {
        List<IChangelistSummary> changes = server.getChangelists(null, null);
        assertThat("wrong number of changes", changes.size(), is(4));
        assertThat(changes.get(0).getVisibility(), is(PUBLIC));
        assertThat(changes.get(1).getVisibility(), is(RESTRICTED));
        assertThat(changes.get(2).getVisibility(), is(PUBLIC));
        // change descriptions should be truncated
        assertThat("wrong changelist description", changes.get(3).getDescription().length(), is(31));
    }

    @DisplayName("The -L flag displays the changelist descriptions, truncated to 250 characters if longer.")
    @Test
    public void slightlyShortenedDescriptions() throws Throwable {
        GetChangelistsOptions opts = new GetChangelistsOptions();
        opts.setTruncateDescriptions(true);
        List<IChangelistSummary> changes = server.getChangelists(null, opts);
        assertThat("wrong number of changes", changes.size(), is(4));
        assertThat("wrong changelist description", changes.get(2).getDescription().length(), is(250));
        assertThat("wrong changelist description", changes.get(2).getDescription(), is(PENDING_DESCRIPTION_MORE_THAN_250.substring(0, 250)));
        assertThat("wrong changelist description", changes.get(3).getDescription(), is(format("Changelist for user %s and client %s.\n", ts.getUser(), CLIENT_NAME)));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}
