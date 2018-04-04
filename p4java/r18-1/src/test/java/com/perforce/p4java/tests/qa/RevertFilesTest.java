package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class RevertFilesTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IChangelist pendingChangelist = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;

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
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "RevertFilesTest", "text");

        pendingChangelist = h.createChangelist(server, user, client);
    }

    @Before
    public void resetFileState() {
        try {
            List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
            client.revertFiles(fileSpec, null);
            h.editFile(testFile.getAbsolutePath(), "RevertFilesTest\n2", pendingChangelist, client);
        } catch (Throwable t) {

        }
    }

    // verify that the spec list we get back from openedFiles can be fed into reopen
    @Test
    public void revertOpenFiles() throws Throwable {
        List<IFileSpec> openFiles = client.openedFiles(null, new OpenedFilesOptions());

        assertTrue(openFiles.size() == 1);
        h.validateFileSpecs(openFiles);

        List<IFileSpec> revertedFiles = client.revertFiles(openFiles, new RevertFilesOptions());

        h.validateFileSpecs(revertedFiles);

        for (IFileSpec file : revertedFiles) {
            assertEquals(FileAction.UNKNOWN, file.getAction());
        }
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	