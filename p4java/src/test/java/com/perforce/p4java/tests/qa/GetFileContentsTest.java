package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetFileContentsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static IChangelist pendingChangelist = null;
    private static File testFile = null;


    // simple setup with one file with multiple revs
    // Verifies fix in job042748
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

        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetFileContentsTest", "text");

        pendingChangelist = h.createChangelist(server, user, client);

        h.editFile(testFile.getAbsolutePath(), "GetFileContentsTest\nLine2", pendingChangelist, client);
        pendingChangelist.submit(false);

        pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "GetFileContentsTest\nLine2\nLine3", pendingChangelist, client);
        pendingChangelist.submit(false);

        pendingChangelist = h.createChangelist(server, user, client);
        h.deleteFile(testFile.getAbsolutePath(), pendingChangelist, client);

        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetFileContentsTest\nLine 2\nline 3\n", "text");
    }

    // print all the revs using the -a flag
    // Verifies fix in job042748
    @Test
    public void printAll() throws Throwable {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        server.getFileContents(makeFileSpecList("//depot/foo.txt"),
                                true,
                                false)));
        String line = new String();
        ;
        ;
        String fullText = new String();
        while ((line = br.readLine()) != null) {
            fullText = fullText + line + "\n";
        }
        br.close();

        assertThat("rev data missing", fullText, containsString("#3"));
    }

    // print all the revs using the -a flag, but limited to a subset of total revs
    // Verifies fix in job042748
    @Test
    public void printAllWithRevLImiter() throws Throwable {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        server.getFileContents(makeFileSpecList("//depot/foo.txt#2,3"),
                                true,
                                false)));
        String line = new String();
        ;
        ;
        String fullText = new String();
        while ((line = br.readLine()) != null) {
            fullText = fullText + line + "\n";
        }
        br.close();

        assertThat("rev data missing", fullText, containsString("GetFileContentsTest"));
        assertThat("rev data missing", fullText, containsString("#3"));
        assertThat("rev data missing", fullText, not(containsString("#5")));
        assertThat("rev data missing", fullText, not(containsString("#1")));
    }

    // print all the revs using the -a flag, but with a non-existent set of revs
    // Verifies fix in job042748
    @Test
    public void printAllWithDeletedRev() throws Throwable {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        server.getFileContents(makeFileSpecList("//depot/foo.txt#4,4"),
                                true,
                                false)));
        String line = new String();
        ;
        ;
        String fullText = new String();
        while ((line = br.readLine()) != null) {
            fullText = fullText + line + "\n";
        }
        br.close();

        assertTrue("content incorrect", fullText.contains("delete change"));
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
