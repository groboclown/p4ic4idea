package com.perforce.p4java.tests.qa;

import com.perforce.p4java.admin.IDiskSpace;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(JUnitPlatform.class)
public class GetDiskspaceTest {

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

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "GetDiskspaceTest", "text");
    }


    @DisplayName("test basic diskspace query")
    @Test
    public void getSpace() throws Throwable {
        List<IDiskSpace> report = server.getDiskSpace(null);
        assertThat(report.get(0).getFileSystemType(), notNullValue());
        assertThat(report.get(0).getFreeBytes(), notNullValue());
        assertThat(report.get(0).getLocation(), notNullValue());
        assertThat(report.get(0).getPercentUsed(), notNullValue());
        assertThat(report.get(0).getTotalBytes(), notNullValue());
        assertThat(report.get(0).getUsedBytes(), notNullValue());

        assertThat("wrong number of elements", report.size(), is(5));
    }


    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}

