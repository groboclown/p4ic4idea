package com.perforce.p4java.tests.qa;


import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.GetBranchSpecsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
@RunWith(JUnitPlatform.class)
public class GetBranchspecsTest {

	private static TestServer ts = null;
	private static Helper helper = null;
	private static IOptionsServer server = null;

	@BeforeAll
	public static void beforeClass() throws Throwable {
		helper = new Helper();
		ts = new TestServer();
		ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
		ts.start();

		server = helper.getServer(ts);
		server.setUserName(ts.getUser());
		server.connect();

		IUser user = server.getUser(ts.getUser());

		IClient client = helper.createClient(server, "client1");
		server.setCurrentClient(client);

		File testFile = new File(client.getRoot(), "foo.txt");
		helper.addFile(server, user, client, testFile.getAbsolutePath(), "ProxytTest", "text");

		helper.addBranchspec(server, user, "branch1", "//depot/foo...", "//depot/bar...");
		helper.addBranchspec(server, user, "BRANCH2", "//depot/foo...", "//depot/baz...");
	}


	@DisplayName("verify job046825: case-insensitive name matching")
	@Test
	public void caseInsensitiveListing() throws Throwable {
		GetBranchSpecsOptions opts = new GetBranchSpecsOptions();
		opts.setCaseInsensitiveNameFilter("branch*");
		List<IBranchSpecSummary> branches = server.getBranchSpecs(opts);

		// we should get two branches here
		assertThat("wrong number of branches",  branches.size(), is(2));
	}


	@DisplayName("verify case-sensitive name matching")
	@Test
	public void caseSensitiveListing() throws Throwable {
		GetBranchSpecsOptions opts = new GetBranchSpecsOptions();
		opts.setNameFilter("branch*");
		List<IBranchSpecSummary> branches = server.getBranchSpecs(opts);

		// we should get one branch here
		if (server.isCaseSensitive()) {
			assertThat("wrong number of branches", branches.size(), is(1));
		} else {
			assertThat("wrong number of branches", branches.size(), is(2));
		}
	}


	@DisplayName("verify setting the -t flag doesn't break anything")
	@Test
	public void getTime() throws Throwable {
		GetBranchSpecsOptions opts = new GetBranchSpecsOptions();
		opts.setShowTime(true);
		List<IBranchSpecSummary> branches = server.getBranchSpecs(opts);

		// we should get two branches here
		assertThat("wrong number of branches",  branches.size(), is(2));
	}


	@AfterAll
	public static void afterClass() {
		helper.after(ts);
	}
}
	
