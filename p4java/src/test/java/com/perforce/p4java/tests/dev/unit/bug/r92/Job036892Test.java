/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r92;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary.IClientSubmitOptions;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that client submit options are correctly settable and sticky
 * in response to the specific bug in job036892.
 * 
 * @job job036892
 * @testid Job036892Test
 */

@TestId("Job036892Test")
@Jobs({"job036892"})
public class Job036892Test extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job036892Test.class.getSimpleName());
    
	@Test
	public void testOptions() {
		IClient client = null;
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			Client newClient = makeTempClient(null, server);
			assertNotNull(newClient);
			String rslt = server.createClient(newClient);
			assertNotNull(rslt);
			assertTrue(rslt.contains("saved"));
			client = server.getClient(newClient.getName());
			assertNotNull(client);
			server.setCurrentClient(client);
			IClientSubmitOptions submitOpts = client.getSubmitOptions();
			assertNotNull(submitOpts);
			boolean revertUnchanged = submitOpts.isRevertunchanged();
			if (revertUnchanged) {
				submitOpts.setRevertunchanged(false);
			} else {
				submitOpts.setRevertunchanged(true);
			}
			assertFalse(revertUnchanged == submitOpts.isRevertunchanged());
			client.setSubmitOptions(submitOpts);
			client.update();
			IClient retrievedClient = server.getClient(client.getName());
			assertNotNull(retrievedClient);
			IClientSubmitOptions retrievedSubOpts = retrievedClient.getSubmitOptions();
			assertNotNull(retrievedSubOpts);
			assertFalse(revertUnchanged == retrievedSubOpts.isRevertunchanged());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					try {
						server.deleteClient(client.getName(), false);
					} catch (Exception exc) {
					}
				}
			}
		}
	}
}
