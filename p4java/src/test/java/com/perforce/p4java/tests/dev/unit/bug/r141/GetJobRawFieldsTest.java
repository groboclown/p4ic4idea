/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r141;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r152.ShelveChangelistClientTest;

/**
 * Test getting raw fields from a job. The "specFormatted" raw field is in the
 * Job's "rawFields" when calling IServer.getJob().
 */
@Jobs({ "job072366" })
@TestId("Dev141_GetJobRawFieldsTest")
public class GetJobRawFieldsTest extends P4JavaRshTestCase {

	

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetJobRawFieldsTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
    
    }

	
	/**
	 * Test getting raw fields from a job. The "specFormatted" raw field is in
	 * the Job's "rawFields" when calling IServer.getJob().
	 */
	@Test
	public void testGetJobRawFields() {

		IJob newJob = null;
		Map<String, Object> jobMap = new HashMap<String, Object>();

		try {
			jobMap.put("Job", "new");
			jobMap.put("Description", "Temporary test job for " + this.testId);
			jobMap.put("Status", "open");
			jobMap.put("User", this.getUserName());
			
			newJob = server.createJob(jobMap);
			assertNotNull("Null job returned from createJob method", newJob);
			assertNotNull("Null raw fields returned for this job", newJob.getRawFields());
			assertFalse("The 'specFormatted' field should not be included in the job raw fields",
					newJob.getRawFields().containsKey("specFormatted"));

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

}
