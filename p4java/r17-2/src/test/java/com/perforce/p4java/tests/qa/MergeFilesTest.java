package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.IStreamSummary.Type.DEVELOPMENT;
import static com.perforce.p4java.core.IStreamSummary.Type.MAINLINE;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.MergeFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class MergeFilesTest {

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

        h.createDepot(server, "Ace", STREAM, null, "ace/...");
        h.createStream(server, "//Ace/main", MAINLINE, null);

        client.setStream("//Ace/main");
        client.update();
        client.sync(makeFileSpecList("//..."), null);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetStreamIntegrationStatusTest", "text");
        testFile = new File(client.getRoot() + FILE_SEP + "bar.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetStreamIntegrationStatusTest", "text");

        h.createStream(server, "//Ace/dev", DEVELOPMENT, "//Ace/main");
        client.setStream("//Ace/dev");
        client.update();
        client.sync(makeFileSpecList("//..."), null);

        h.addBranchspec(server, user, "branch1", "//Ace/main/...", "//Ace/dev/...");

        h.createStream(server, "//Ace/subDev", DEVELOPMENT, "//Ace/dev");
    }


    @Before
    public void before() throws Throwable {

        client.revertFiles(makeFileSpecList("//..."), null);

        client.setStream("//Ace/dev");
        client.update();
        client.sync(makeFileSpecList("//..."), null);

    }


    // just make sure the darn thing works
    @Test
    public void simple() throws Throwable {
        MergeFilesOptions opts = new MergeFilesOptions();
        opts.setStream("//Ace/dev");

        List<IFileSpec> merged = client.mergeFiles(null, null, opts);
        assertEquals("wrong number of files", 1, merged.size());
        assertThat("missing error", merged.get(0).getStatusMessage(), containsString("Stream //Ace/dev needs 'copy' not 'merge' in this direction."));

        List<IFileSpec> opened = client.openedFiles(null, null);
        assertEquals("files should not have been opened", 0, opened.size());
    }


    // verify the -n flag on merge
    @Test
    public void preview() throws Throwable {
        MergeFilesOptions opts = new MergeFilesOptions();
        opts.setStream("//Ace/dev");
        opts.setReverseMapping(true);
        opts.setShowActionsOnly(true);

        List<IFileSpec> merged = client.mergeFiles(null, null, opts);
        assertEquals("wrong number of files", 2, merged.size());

        List<IFileSpec> opened = client.openedFiles(null, null);
        assertEquals("files should not have been opened", 0, opened.size());
    }


    // verify the use of merge with the branchspec
    @Test
    public void branchspec() throws Throwable {
        MergeFilesOptions opts = new MergeFilesOptions();
        opts.setBranch("branch1");

        List<IFileSpec> merged = client.mergeFiles(null, null, opts);
        assertEquals("wrong number of files", 2, merged.size());

        List<IFileSpec> opened = client.openedFiles(null, null);
        assertEquals("files should not have been opened", 2, opened.size());
    }


    // verify the use of merge with the branchspec
    @Test
    public void noOptions() throws Throwable {
        MergeFilesOptions opts = new MergeFilesOptions();

        List<IFileSpec> merged = client.mergeFiles(null, null, opts);
        assertEquals("wrong number of files", 2, merged.size());

        List<IFileSpec> opened = client.openedFiles(null, null);
        assertEquals("files should not have been opened", 2, opened.size());
    }


    // verify the use of merge with the -s flag
    @Test
    public void bidirectional() throws Throwable {
        MergeFilesOptions opts = new MergeFilesOptions();
        opts.setStream("//Ace/dev");
        opts.setBidirectionalInteg(true);
        opts.setForceStreamMerge(true);

        List<IFileSpec> merged = client.mergeFiles(new FileSpec("//Ace/main/..."), null, opts);
        assertEquals("wrong number of files", 2, merged.size());

        List<IFileSpec> opened = client.openedFiles(null, null);
        assertEquals("files should not have been opened", 2, opened.size());
    }


    // verify the -P flag on merge
    @Test
    public void parent() throws Throwable {
        client.setStream("//Ace/subDev");
        client.update();
        client.sync(makeFileSpecList("//..."), null);

        MergeFilesOptions opts = new MergeFilesOptions();
        opts.setStream("//Ace/subDev");
        opts.setReverseMapping(true);
        opts.setParentStream("//Ace/main");

        List<IFileSpec> merged = client.mergeFiles(null, null, opts);
        assertEquals("wrong number of files", 2, merged.size());

        List<IFileSpec> opened = client.openedFiles(null, null);
        assertEquals("files should not have been opened", 2, opened.size());
    }


    // just make sure the darn thing works
    @Test
    public void maxFiles() throws Throwable {
        MergeFilesOptions opts = new MergeFilesOptions();
        opts.setStream("//Ace/dev");
        opts.setReverseMapping(true);
        opts.setMaxFiles(1);
        opts.setForceStreamMerge(true);

        List<IFileSpec> merged = client.mergeFiles(null, null, opts);
        assertEquals("wrong number of files", 1, merged.size());

        List<IFileSpec> opened = client.openedFiles(null, null);
        assertEquals("files should not have been opened", 1, opened.size());
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	