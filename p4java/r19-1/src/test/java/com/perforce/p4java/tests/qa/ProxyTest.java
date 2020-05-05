package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;








import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;


@Ignore("These tests hang in BeforeClass on dev environment, so disable for later fix.")
public class ProxyTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static File testFile = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.setProxy(true);
        ts.start();

        server = helper.getProxy(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "ProxytTest", "text");

        ts.stopServer();
    }

    // attempt to get file info with downed server
    @Test
    public void nullGetExtendedFilesOptions() {
        try {
            List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
            server.getExtendedFiles(fileSpec, null);
            fail("low level excception should have been thrown");
        } catch (Throwable t) {
            assertThat("incorrect error", t.getLocalizedMessage(), containsString("Unexpected release2 message in protocol dispatcher"));
        }
    }

    @AfterClass
    public static void afterClass() {
        helper.after(ts);
    }
}

