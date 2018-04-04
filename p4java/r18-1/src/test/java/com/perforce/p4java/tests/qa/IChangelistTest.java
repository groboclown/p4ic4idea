package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.core.file.FileSpecOpStatus.INFO;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static java.util.Calendar.YEAR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class IChangelistTest {

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
        h.addFile(server, user, client, testFile.getAbsolutePath(), "IChangelistTest", "text");
    }

    // make sure a submit occurs even if null is passed for the SubmitOptions
    @Test
    public void nullSubmitOptions() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
        List<IChangelistSummary> changes = server.getChangelists(fileSpec, new GetChangelistsOptions("-ssubmitted"));
        int changeCount = changes.size();

        IChangelist pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "IChangelistTest\nLine2", pendingChangelist, client);

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

    // verify that we can change the date of our checkins
    // Verifies job045050
    @Test
    public void changeDate() throws Throwable {
        Calendar cal = new GregorianCalendar(2010, 1, 1);
        IChangelist change = server.getChangelist(1);

        // update the date
        change.setDate(cal.getTime());
        change.update(true);

        change = server.getChangelist(1);
        Calendar testDate = new GregorianCalendar();
        testDate.setTime(change.getDate());
        assertThat("date was not set correctly", testDate.get(YEAR), equalTo(2010));
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	