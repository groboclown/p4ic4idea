package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;

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
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class ObliterateTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;
    private static String targetFile = "//depot/bar.txt";
    private static IChangelist pendingChangelist = null;

    // files are setup per run in the Before method
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
    }

    // setup the test environment with one added file and one branched file
    @Before
    public void before() {

        try {
            // clear the have list or the tests will fail after the -h oblit
            List<IFileSpec> fileSpec = makeFileSpecList("//...#0");
            client.sync(null, null);

            fileSpec = makeFileSpecList("//...");
            ObliterateFilesOptions opts = new ObliterateFilesOptions();
            opts.setExecuteObliterate(true);
            server.obliterateFiles(fileSpec, opts);

            testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
            h.addFile(server, user, client, testFile.getAbsolutePath(), "ObliterateTest", "text+w");

            pendingChangelist = h.createChangelist(server, user, client);
            IFileSpec sourceSpec = new FileSpec(testFile.getAbsolutePath());
            IFileSpec targetSpec = new FileSpec(targetFile);
            IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
            integOpts.setDisplayBaseDetails(true);
            integOpts.setChangelistId(pendingChangelist.getId());

            List<IFileSpec> integedFiles = client.integrateFiles(sourceSpec, targetSpec, null, integOpts);
            h.validateFileSpecs(integedFiles);
            pendingChangelist.submit(false);
        } catch (Throwable t) {

            h.error(t);

        }

    }

    // basic obliterate test, sans -y
    @Test
    public void previewObliterateFile() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        List<IObliterateResult> blitList = server.obliterateFiles(fileSpec, null);

        assertEquals("wrong number of results", 1, blitList.size());
        assertEquals("incorrect results", 1, blitList.get(0).getClientRecDeleted());
        assertTrue("was not a preview", blitList.get(0).isReportOnly());
    }

    // basic obliterate test
    @Test
    public void obliterateFile() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        ObliterateFilesOptions opts = new ObliterateFilesOptions();
        opts.setExecuteObliterate(true);
        List<IObliterateResult> blitList = server.obliterateFiles(fileSpec, opts);

        assertEquals("wrong number of results", 1, blitList.size());
        assertEquals("incorrect results", 1, blitList.get(0).getClientRecDeleted());
        assertFalse("was not a preview", blitList.get(0).isReportOnly());
    }

    // obliterate unedited, branched files
    @Test
    public void obliterateFirstRevs() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//...");
        ObliterateFilesOptions opts = new ObliterateFilesOptions();
        opts.setExecuteObliterate(true);
        opts.setBranchedFirstHeadRevOnly(true);
        List<IObliterateResult> blitList = server.obliterateFiles(fileSpec, opts);

        assertEquals("wrong number of results", 1, blitList.size());
        assertEquals("incorrect results", 1, blitList.get(0).getClientRecDeleted());
        assertFalse("was not a preview", blitList.get(0).isReportOnly());
    }

    // obliterate files, don't clear have
    @Test
    public void obliterateFileNotHave() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        ObliterateFilesOptions opts = new ObliterateFilesOptions();
        opts.setExecuteObliterate(true);
        opts.setSkipHaveSearch(true);
        List<IObliterateResult> blitList = server.obliterateFiles(fileSpec, opts);

        assertEquals("wrong number of results", 1, blitList.size());
        assertEquals("incorrect results", 0, blitList.get(0).getClientRecDeleted());
        assertEquals("incorrect results", 1, blitList.get(0).getRevisionRecDeleted());
        assertFalse("was not a preview", blitList.get(0).isReportOnly());

        // check the have list
        fileSpec = client.haveList(null);
        assertEquals("have rev cleared incorrectly", 2, fileSpec.size());
    }

    // obliterate files, don't clear archive
    // instead of checking the archive, we just verify the server got the correct flag
    @Test
    public void obliterateFileNotArchives() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        ObliterateFilesOptions opts = new ObliterateFilesOptions();
        opts.setExecuteObliterate(true);
        opts.setSkipArchiveSearchRemoval(true);
        List<IObliterateResult> blitList = server.obliterateFiles(fileSpec, opts);

        assertEquals("wrong number of results", 1, blitList.size());
        assertEquals("incorrect results", 1, blitList.get(0).getClientRecDeleted());
        assertEquals("incorrect results", 1, blitList.get(0).getRevisionRecDeleted());
        assertFalse("was not a preview", blitList.get(0).isReportOnly());

        String log = h.fileToString(ts.getLog());

        assertThat("-a not seen", log, containsString("user-obliterate -y -a"));
    }

    // obliterate branched revs, don't clear archive
    // instead of checking the archive, we just verify the server got the correct flag
    @Test
    public void obliterateBranchedNotArchives() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//...");
        ObliterateFilesOptions opts = new ObliterateFilesOptions();
        opts.setExecuteObliterate(true);
        opts.setSkipArchiveSearchRemoval(true);
        opts.setBranchedFirstHeadRevOnly(true);
        List<IObliterateResult> blitList = server.obliterateFiles(fileSpec, opts);

        assertEquals("wrong number of results", 1, blitList.size());
        assertEquals("incorrect results", 1, blitList.get(0).getClientRecDeleted());
        assertEquals("incorrect results", 1, blitList.get(0).getRevisionRecDeleted());
        assertFalse("was not a preview", blitList.get(0).isReportOnly());

        String log = h.fileToString(ts.getLog());

        assertThat("-a not seen", log, containsString("user-obliterate -y -a -b"));
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	