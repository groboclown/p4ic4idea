package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;









import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;



import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FixJobsTest {

	private static TestServer ts = null;
	private static Helper helper = null;
	private static IOptionsServer server = null;
	private static IUser user = null;
	private static IClient client = null;
	private static File testFile = null;
	private static IJob job = null;
	private static IChangelist pendingChangelist = null;

	@BeforeClass
	public static void beforeClass() throws Throwable {
		helper = new Helper();
		ts = new TestServer();
		ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
		ts.start();

		server = helper.getServer(ts);
		server.setUserName(ts.getUser());
		server.connect();

		user = server.getUser(ts.getUser());

		client = helper.createClient(server, "client1");
		server.setCurrentClient(client);

		testFile = new File(client.getRoot(), "foo.txt");
		helper.addFile(server, user, client, testFile.getAbsolutePath(), "GetFixListTest", "text");

		job = helper.addJob(server, user, "Description");
	}

	@Before
	public void Setup() {
		try {
			pendingChangelist = helper.createChangelist(server, user, client);
			helper.editFile(testFile.getAbsolutePath(), "GetFixListTest\nLine 2", pendingChangelist, client);

			Map<String, Object> reset = new HashMap<String, Object>();
			reset.put("Status", "open");
			job.setRawFields(reset);
			job.update();
		} catch (Throwable ignore) {
		}
	}

	/**
	 * just make sure the darn thing works
	 * @throws Throwable
	 */
	@Test
	public void basic() throws Throwable {
		List<String> jobs = new ArrayList<String>();
		jobs.add(job.getId());
		List<IFix> fixes = server.fixJobs(jobs, pendingChangelist.getId(), null);

		// refreshes are always required on the changelist object after applying fixes
		pendingChangelist.refresh();

		List<IFileSpec> submittedFiles = pendingChangelist.submit(false);

		helper.validateFileSpecs(submittedFiles);

		fixes = server.getFixList(null, pendingChangelist.getId(), job.getId(), false, 0);
		assertThat(fixes.size(), is(1));

		for (IFix fix : fixes) {
			assertThat(fix.getStatus(), is("closed"));
		}
	}


	/**
	 * verify SubmitOptions.set
	 * @throws Throwable
	 */
	@Test
	public void leaveOpen() throws Throwable {
		List<String> jobs = new ArrayList<String>();
		jobs.add(job.getId());
		List<IFix> fixes = server.fixJobs(jobs, pendingChangelist.getId(), null);

		// refreshes are always required on the changelist object after applying fixes
		pendingChangelist.refresh();

		List<IFileSpec> submittedFiles = pendingChangelist.submit(new SubmitOptions().setJobStatus("open"));

		helper.validateFileSpecs(submittedFiles);

		fixes = server.getFixList(null, pendingChangelist.getId(), job.getId(), false, 0);
		assertThat(fixes.size(), is(1));

		for (IFix fix : fixes) {
			assertThat(fix.getStatus(), is("open"));
		}
	}


	@AfterClass
	public static void afterClass() {
		helper.after(ts);
	}
}
	