/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.core.IJob;
import com.perforce.p4java.impl.generic.core.Job;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.impl.mapbased.server.cmd.JobDelegator;
import com.perforce.p4java.option.server.SearchJobsOptions;
import com.perforce.p4java.server.delegator.IJobDelegator;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Test the 'p4 search [-m max] words'.
 */
@Jobs({ "job062030" })
@TestId("Dev131_SearchJobsTest")
public class SearchJobsTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", SearchJobsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			server = getSuperConnection(p4d.getRSHURL());
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
     * Test the 'p4 search [-m max] words'.
     */
    @Test
	public void testSearchJobs() {
		try {
			// Get a list of job IDs with matching indexed words.
			List<String> jobIds = server.searchJobs("test", new SearchJobsOptions());
			assertNotNull(jobIds);

			// Assuming total matching jobs exceeds 50.
			assertTrue(jobIds.size() > 50);

			// Get max number (20) of matching job IDs
			jobIds = server.searchJobs("test", new SearchJobsOptions().setMaxResults(20));
			assertNotNull(jobIds);
			assertEquals(20, jobIds.size());

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
