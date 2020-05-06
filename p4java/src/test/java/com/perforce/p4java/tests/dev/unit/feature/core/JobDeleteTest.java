/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.core;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Simple job deletion tests.
 * 
 * @testid JobDeleteTest
 */

@TestId("JobDeleteTest")
public class JobDeleteTest extends P4JavaRshTestCase {

	public JobDeleteTest() {
	}
	
	 private IClient client = null;

	    @Rule
	    public ExpectedException exception = ExpectedException.none();

	    @ClassRule
	    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", JobDeleteTest.class.getSimpleName());

	    /**
	     * @Before annotation to a method to be run before each test in a class.
	     */
	    @Before
	    public void beforeEach() throws Exception{
	        Properties properties = new Properties();
	        setupServer(p4d.getRSHURL(), userName, password, true, properties);
	        assertNotNull(server);
	        client = getClient(server);
	     }
	@Test
	public void testBasics() throws Exception {
		IJob newJob = null;
		Map<String, Object> jobMap = new HashMap<String, Object>();
		
		try {
			jobMap.put("Job", "new");
			jobMap.put("Description", "Temporary test job for " + this.testId);
			jobMap.put("Status", "open");
			jobMap.put("User", this.getUserName());
			
			newJob = server.createJob(jobMap);
			assertNotNull("Null job returned from createJob method", newJob);
			
			String jobId = newJob.getId();
			assertNotNull("Null jobId field in new job", jobId);
			
			String deleteStr = server.deleteJob(jobId);
			assertNotNull("Null result string from delete job method", deleteStr);
			
			IJob deletedJob = server.getJob(jobId);
			assertNotNull("supposedly deleted job still on server", deletedJob);
			
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
