package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetFixList {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;
    private static IJob job = null;
    private static IChangelist pendingChangelist = null;

    // simple setup with one file and a fix
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
        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetFixListTest", "text");

        job = h.addJob(server, user, "Description");

        pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "GetFixListTest\nLine 2", pendingChangelist, client);
        List<String> jobs = new ArrayList<String>();
        jobs.add(job.getId());
        List<IFileSpec> submittedFiles = pendingChangelist.submit(false, jobs, "closed");

        h.validateFileSpecs(submittedFiles);
    }

    // make sure we get jobs when asking for fixes with a "0" changelist
    @Test
    public void zeroChangelistRequest() throws Throwable {
        List<IFix> fixes = server.getFixList(null, 0, job.getId(), false, 0);
        assertTrue(fixes.size() == 1);

        for (IFix fix : fixes) {
            assertEquals("closed", fix.getStatus());
        }
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	