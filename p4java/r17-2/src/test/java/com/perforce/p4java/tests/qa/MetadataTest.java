package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.Metadata.getP4JDateString;
import static com.perforce.p4java.Metadata.getP4JVersionString;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.Metadata;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class MetadataTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
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

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "ExeMapCmdTest", "text");
    }

    @Before
    public void reset() {
        try {

        } catch (Throwable t) {

        }
    }

    // we should get something here
    @Test
    public void basic() throws Throwable {
        try {

            String version = getP4JVersionString();
            String date = getP4JDateString();

            assertNotNull(version);
            assertNotNull(date);

        } catch (Throwable t) {

            h.error(t);

        }
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}