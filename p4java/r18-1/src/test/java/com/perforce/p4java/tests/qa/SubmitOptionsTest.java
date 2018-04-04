package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.core.file.FileSpecOpStatus.INFO;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class SubmitOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;
    private static IJob job = null;

    // simple setup with one file and a couple jobs
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
        h.addFile(server, user, client, testFile.getAbsolutePath(), "SubmitOptionsTest", "text");

        job = h.addJob(server, user, "Description");
    }

    // revert open files and reset the job state between tests
    @Before
    public void cleanup() {

        try {
            List<IFileSpec> fileSpec = makeFileSpecList("//...");
            List<IFileSpec> revertedFiles = client.revertFiles(fileSpec, null);
            h.validateFileSpecs(revertedFiles);

            Map<String, Object> reset = new HashMap<String, Object>();
            reset.put("Status", "open");
            job.setRawFields(reset);
            job.update();

        } catch (Throwable t) {

        }

    }

    // make sure a submit occurs even if null is passed for the SubmitOptions
    @Test
    public void nullSubmitOptions() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        List<IChangelistSummary> changes = server.getChangelists(fileSpec, new GetChangelistsOptions("-ssubmitted"));
        int changeCount = changes.size();

        IChangelist pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "SubmitOptionsTest\nLine2", pendingChangelist, client);

        List<IFileSpec> submittedFiles = pendingChangelist.submit(null);

        // there are two specs: the actual file and an info message
        assertTrue("Wrong number of file specs", submittedFiles.size() == 2);
        assertTrue(submittedFiles.get(1).getOpStatus() == INFO);
        assertTrue(submittedFiles.get(1).getStatusMessage().startsWith("Submitted as change"));

        h.validateFileSpecs(submittedFiles);

        // there should be one new change now
        changes = server.getChangelists(submittedFiles, new GetChangelistsOptions("-ssubmitted"));
        assertTrue("Wrong number of changes", changes.size() == (changeCount + 1));
    }

    // verify the submitted files are reopened
    @Test
    public void reopenOption() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        List<IChangelistSummary> changes = server.getChangelists(fileSpec, new GetChangelistsOptions("-ssubmitted"));
        int changeCount = changes.size();


        SubmitOptions opts = new SubmitOptions();
        opts.setReOpen(true);

        IChangelist pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "SubmitOptionsTest\nReopenTest\n", pendingChangelist, client);

        List<IFileSpec> submittedFiles = pendingChangelist.submit(opts);

        // there are two specs: the actual file and an info message
        assertTrue("Wrong number of file specs", submittedFiles.size() == 2);
        assertTrue(submittedFiles.get(1).getOpStatus() == INFO);
        assertTrue(submittedFiles.get(1).getStatusMessage().startsWith("Submitted as change"));

        h.validateFileSpecs(submittedFiles);

        // there should be one new change now
        changes = server.getChangelists(submittedFiles, new GetChangelistsOptions("-ssubmitted"));
        assertTrue("Wrong number of changes", changes.size() == (changeCount + 1));

        // there should be an open file named "//depot/foo.txt"
        fileSpec = client.openedFiles(null, null);

        assertTrue(fileSpec.size() == 1);
        assertEquals("Wrong file is open", "//depot/foo.txt", fileSpec.get(0).getDepotPath().toString());
    }

    // verify the submitted files are not reopened
    @Test
    public void reopenOptionUnset() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        List<IChangelistSummary> changes = server.getChangelists(fileSpec, new GetChangelistsOptions("-ssubmitted"));
        int changeCount = changes.size();


        SubmitOptions opts = new SubmitOptions();

        IChangelist pendingChangelist = h.createChangelist(server, user, client);
        List<IFileSpec> edittedFiles = h.editFile(testFile.getAbsolutePath(), "SubmitOptionsTest\nReopenTest\n", pendingChangelist, client);

        h.validateFileSpecs(edittedFiles);

        List<IFileSpec> submittedFiles = pendingChangelist.submit(opts);

        // there are two specs: the actual file and an info message
        assertTrue("Wrong number of file specs", submittedFiles.size() == 2);
        assertTrue(submittedFiles.get(1).getOpStatus() == INFO);
        assertTrue(submittedFiles.get(1).getStatusMessage().startsWith("Submitted as change"));

        h.validateFileSpecs(submittedFiles);

        // there should be one new change now
        changes = server.getChangelists(submittedFiles, new GetChangelistsOptions("-ssubmitted"));
        assertTrue("Wrong number of changes", changes.size() == (changeCount + 1));

        // there should be no open files
        fileSpec = client.openedFiles(null, null);

        assertTrue(fileSpec.size() == 0);
    }

    // make sure we can set a job status option
    @Test
    public void jobStatusOption() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        List<IChangelistSummary> changes = server.getChangelists(fileSpec, new GetChangelistsOptions("-ssubmitted"));
        int changeCount = changes.size();

        SubmitOptions opts = new SubmitOptions();
        List<String> jobs = new ArrayList<String>();
        jobs.add(job.getId());
        opts.setJobIds(jobs);
        opts.setJobStatus("open");

        IChangelist pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "SubmitOptionsTest\nLine2", pendingChangelist, client);

        List<IFileSpec> submittedFiles = pendingChangelist.submit(opts);

        // there are two specs: the actual file and an info message
        assertTrue("Wrong number of file specs", submittedFiles.size() == 2);
        assertTrue(submittedFiles.get(1).getOpStatus() == INFO);
        assertTrue(submittedFiles.get(1).getStatusMessage().startsWith("Submitted as change"));

        h.validateFileSpecs(submittedFiles);

        // there should be one new change now
        changes = server.getChangelists(submittedFiles, new GetChangelistsOptions("-ssubmitted"));
        assertTrue("Wrong number of changes", changes.size() == (changeCount + 1));

        // the job should still be open
        IJob tmpJob = server.getJob(job.getId());
        Map<String, Object> map = tmpJob.getRawFields();
        String status = (String) map.get("Status");
        assertEquals("open", status);
    }

    // make sure a job is automatically marked as closed when submitted
    @Test
    public void jobStatusOptionUnset() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        List<IChangelistSummary> changes = server.getChangelists(fileSpec, new GetChangelistsOptions("-ssubmitted"));
        int changeCount = changes.size();

        SubmitOptions opts = new SubmitOptions();
        List<String> jobs = new ArrayList<String>();
        jobs.add(job.getId());
        opts.setJobIds(jobs);

        IChangelist pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "SubmitOptionsTest\nLine2\nLine3", pendingChangelist, client);

        List<IFileSpec> submittedFiles = pendingChangelist.submit(opts);

        // there are two specs: the actual file and an info message
        assertTrue("Wrong number of file specs", submittedFiles.size() == 2);
        assertTrue(submittedFiles.get(1).getOpStatus() == INFO);
        assertTrue(submittedFiles.get(1).getStatusMessage().startsWith("Submitted as change"));

        h.validateFileSpecs(submittedFiles);

        // there should be one new change now
        changes = server.getChangelists(submittedFiles, new GetChangelistsOptions("-ssubmitted"));
        assertTrue("Wrong number of changes", changes.size() == (changeCount + 1));

        // the job should be closed
        IJob tmpJob = server.getJob(job.getId());
        Map<String, Object> map = tmpJob.getRawFields();
        String status = (String) map.get("Status");
        assertEquals("closed", status);
    }

    // make sure we can set a job status option
    @Test
    public void classicSubmitMethod() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        List<IChangelistSummary> changes = server.getChangelists(fileSpec, new GetChangelistsOptions("-ssubmitted"));
        int changeCount = changes.size();

        List<String> jobs = new ArrayList<String>();
        jobs.add(job.getId());

        IChangelist pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "SubmitOptionsTest\nLine2", pendingChangelist, client);

        List<IFileSpec> submittedFiles = pendingChangelist.submit(true, jobs, "open");

        // there are two specs: the actual file and an info message
        assertTrue("Wrong number of file specs", submittedFiles.size() == 2);
        assertTrue(submittedFiles.get(1).getOpStatus() == INFO);
        assertTrue(submittedFiles.get(1).getStatusMessage().startsWith("Submitted as change"));

        h.validateFileSpecs(submittedFiles);

        // there should be one new change now
        changes = server.getChangelists(submittedFiles, new GetChangelistsOptions("-ssubmitted"));
        assertTrue("Wrong number of changes", changes.size() == (changeCount + 1));

        // the job should still be open
        IJob tmpJob = server.getJob(job.getId());
        Map<String, Object> map = tmpJob.getRawFields();
        String status = (String) map.get("Status");
        assertEquals("open", status);

        // there should be an open file named "//depot/foo.txt"
        fileSpec = client.openedFiles(null, null);

        assertTrue(fileSpec.size() == 1);
        assertEquals("Wrong file is open", "//depot/foo.txt", fileSpec.get(0).getDepotPath().toString());
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	