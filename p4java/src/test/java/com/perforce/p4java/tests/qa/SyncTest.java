package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileAction.DELETED;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class SyncTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static IClient client2 = null;
    private static IChangelist pendingChangelist = null;
    private static File testFile = null;
    private static File testFile2 = null;


    // simple setup with one file with multiple revs
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
        client2 = h.createClient(server, "client2", null, "//depot/bar.txt //client2/bar.txt", null);
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");

        h.addFile(server, user, client, testFile.getAbsolutePath(), "SyncTest", "text");

        pendingChangelist = h.createChangelist(server, user, client);

        h.editFile(testFile.getAbsolutePath(), "SyncTest\nLine2", pendingChangelist, client);
        pendingChangelist.submit(false);

        pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "SyncTest\nLine2\nLine3", pendingChangelist, client);
        pendingChangelist.submit(false);

        pendingChangelist = h.createChangelist(server, user, client);
        h.deleteFile(testFile.getAbsolutePath(), pendingChangelist, client);

        h.addFile(server, user, client, testFile.getAbsolutePath(), "FileSpecTest", "text");

        testFile2 = new File(client.getRoot() + FILE_SEP + "bar.txt");
        h.addFile(server, user, client, testFile2.getAbsolutePath(), "SyncTest2", "text");
    }

    // sync to head for every test
    @Before
    public void SyncUp() {
        try {

            server.setCurrentClient(client);
            client.sync(makeFileSpecList("//depot/..."), new SyncOptions());
            server.setCurrentClient(client2);
            client2.sync(makeFileSpecList("//depot/..."), new SyncOptions());

        } catch (Throwable t) {

            h.error(t);

        }
    }

    @Test
    public void syncTo0() throws Throwable {
        server.setCurrentClient(client);
        List<IFileSpec> files = client.sync(
                makeFileSpecList("//depot/foo.txt#0"), new SyncOptions());

        for (IFileSpec file : files) {
            assertNotNull(file.getDepotPath());
            assertEquals("Expected action to be 'deleted', was " + file.getAction(), DELETED, file.getAction());
            assertEquals("Expected action to be 'deleted', was " + file.getAction(), "deleted", file.getAction().toString());
        }
        ;
    }

    @Test
    public void syncToNone() throws Throwable {
        server.setCurrentClient(client);
        List<IFileSpec> files = client.sync(
                makeFileSpecList("//depot/foo.txt#none"), new SyncOptions());

        for (IFileSpec file : files) {
            assertNotNull(file.getDepotPath());
            assertEquals("Expected action to be 'deleted', was " + file.getAction(), DELETED, file.getAction());
            assertEquals("Expected action to be 'deleted', was " + file.getAction(), "deleted", file.getAction().toString());
        }
        ;
    }

    @Test
    public void syncToDeletedRev() throws Throwable {
        server.setCurrentClient(client);
        List<IFileSpec> files = client.sync(
                makeFileSpecList("//depot/foo.txt#4"), new SyncOptions());

        for (IFileSpec file : files) {
            assertNotNull(file.getDepotPath());
            assertEquals("Expected action to be 'deleted', was " + file.getAction(), DELETED, file.getAction());
            assertEquals("Expected action to be 'deleted', was " + file.getAction(), "deleted", file.getAction().toString());
        }
        ;
    }

    // a test for job040829: syncing with the client object uses the server object's
    // current client instead of the client's client.
    @Test(expected = RequestException.class)
    public void syncUsingWrongClient() throws Throwable {
        // sync our clients to nothing
        server.setCurrentClient(client2);
        client.sync(makeFileSpecList("//depot/...#0"), new SyncOptions());
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
