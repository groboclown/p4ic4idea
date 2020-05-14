package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;








import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.perforce.p4java.common.base.StringHelper.format;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;



public class ExceptionCodesTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static final String CLIENT_NAME = "client1";


    @BeforeClass
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
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "ExceptionCodesTest", "text");
    }

    /**
     * verify the error code
     * @throws Throwable
     */
    @Test
    public void basicUsage() throws Throwable {
        String depotPath = "//asdfaasd/...";
        try {
            server.getChangelists(makeFileSpecList(depotPath), null);
            fail("The test shouldn't come to here");
        } catch (P4JavaException e) {
            assertThat(e.getLocalizedMessage(), containsString(format("%s - must refer to client '%s'", depotPath, CLIENT_NAME)));
        }
    }

    @AfterClass
    public static void afterClass() {
        helper.after(ts);
    }
}