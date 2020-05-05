/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r121;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test the GetChangelistsOptions constructor
 */
@Jobs({ "job053580" })
@TestId("Dev121_GetChangelistsOptionsTest")
public class GetChangelistsOptionsTest extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetChangelistsOptionsTest.class.getSimpleName());

	IClient client = null;
	
	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = getClient(server);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

	/**
	 * Test GetChangelistsOptions constructor
	 */
	@Test
	public void testGetChangelistsOptions() {

		try {
			List<IFileSpec> files = new ArrayList<IFileSpec>();
			GetChangelistsOptions opts = new GetChangelistsOptions("-m20", "-ssubmitted", "//depot/...");
			List<IChangelistSummary> changeSummaries = server.getChangelists(files, opts);
			assertNotNull(changeSummaries);

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

}
