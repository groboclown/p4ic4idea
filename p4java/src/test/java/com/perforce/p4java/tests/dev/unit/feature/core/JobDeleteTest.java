/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.core.IJob;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple job deletion tests.
 * 
 * @testid JobDeleteTest
 */

@TestId("JobDeleteTest")
public class JobDeleteTest extends P4JavaTestCase {

	public JobDeleteTest() {
	}
	
	@Test
	public void testBasics() throws Exception {
		IServer server = null;
		IJob newJob = null;
		Map<String, Object> jobMap = new HashMap<String, Object>();
		
		try {
			server = getServer();
			assertNotNull("Null server returned from factory", server);
			
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
