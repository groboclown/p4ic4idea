/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r92;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for bogus fix lists on newly-created changelists first noted
 * in job036767. This is a mildly irritating bug to test for,
 * as we need to ensure we have fixed jobs, which we do below; this
 * means we can fail in two different places depending on whether we had
 * any existing fixes or not. We use the "succeeded" boolean below
 * to ensure that if anything goes wrong we don't mask it with the
 * cascaded failures in the finally clause below.
 * 
 * @job job036767
 * @testid Job036767Test
 */

@TestId("Job036767Test")
@Jobs({"job036767"})
public class Job036767Test extends P4JavaTestCase {

	@Test
	public void testJobs() {
		IServer server = null;
		IClient client = null;
		IChangelist changelist = null;
		IChangelist newChangelist = null;
		boolean succeeded = false;
		try {
			server = getServer();
			assertNotNull("Null server returned", server);			
			client = makeTempClient(null, server);
			String rsltStr = server.createClient(client);
			assertNotNull("Null result string from test client creation",
										rsltStr);
			assertTrue("Test client creation error: " + rsltStr,
										rsltStr.contains("saved"));
			server.setCurrentClient(client);
			
			Map<String, Object> jobMap = new HashMap<String, Object>();
			jobMap.put("Description", "Automatically-created job for test " + testId);
			jobMap.put("User", getUserName());
			jobMap.put("Job", "new");
			jobMap.put("Status", "closed");
			IJob job = server.createJob(jobMap);
			assertNotNull("Error creating test job", job);
			
			Changelist changeListImpl = new Changelist(
					          Changelist.UNKNOWN,
					          client.getName(),
					          getUserName(),
					          ChangelistStatus.NEW,
					          null,
					          "Job036767Test changelist test changelist",
					          false,
					          (Server) server);

			changelist = client.createChangelist(changeListImpl);
			List<String> jobList = changelist.getJobIds();
			assertNotNull("Null job list returned by changelist.getJobIds()",
									jobList);
			assertEquals("Non-empty fix list for first newly-created changelist",
							0, jobList.size());	// Will fail if bug still exists and there are
												// any fixed jobs on the server.
			List<String> jobFixList = new ArrayList<String>();
			jobFixList.add(job.getId());
			List<IFix> fixList = server.fixJobs(jobFixList, changelist.getId(), null, false);
			assertNotNull("Null fix list after fix operation", fixList);
			
			Changelist newChangeListImpl = new Changelist(
			          Changelist.UNKNOWN,
			          client.getName(),
			          getUserName(),
			          ChangelistStatus.NEW,
			          null,
			          "Job036767Test changelist test changelist",
			          false,
			          (Server) server);

			newChangelist = client.createChangelist(newChangeListImpl);
			jobList = newChangelist.getJobIds();
			assertNotNull("Null job list returned by changelist.getJobIds()", jobList);
			assertEquals("Non-empty fix list for second newly-created changelist",
									0, jobList.size());
			fixList = server.fixJobs(jobFixList, changelist.getId(), null, true);
			assertNotNull("Null fix list after fix operation", fixList);
			succeeded = true;
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					try {
						if (changelist != null) {
							server.deletePendingChangelist(changelist.getId());
						}
						if (newChangelist != null) {
							server.deletePendingChangelist(newChangelist.getId());
						}
						if (succeeded) server.deleteClient(client.getName(), false);
					} catch (Exception exc1) {
						fail("Unexpected exception: " + exc1.getLocalizedMessage());
					}
				}
			}
		}
	}
}
