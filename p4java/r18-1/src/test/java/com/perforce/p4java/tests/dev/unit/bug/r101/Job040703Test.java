/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 *
 */
@TestId("Bugs101_Job040703Test")
public class Job040703Test extends P4JavaTestCase {

	public Job040703Test() {
	}

	@Test
	public void testJob040703TestFixlistBehaviour() {
		final int fixedChangelist = 830; // Will need to change if we change servers -- HR.
		final String testRoot = null;
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			
			List<IFix> fixes = server.getFixList(null, IChangelist.UNKNOWN, null, false, 0);
			assertNotNull("null fix list returned", fixes);
			int allFixes = fixes.size();
			assertTrue("too few fixes on test server", allFixes > 1);
			fixes = server.getFixList(null, 0, null, false, 0);
			assertNotNull(fixes);
			assertEquals("fix list size differs with changelist == 0 and changelist missing",
								allFixes, fixes.size());
			fixes = server.getFixList(null, 830, null, false, 0);
			assertNotNull("null fix list returned", fixes);
			assertTrue("test changelist #'" + fixedChangelist + "' has no fixes associated with it",
										fixes.size() > 0);
			assertTrue("fix list with specific changelist gives too many fixes!", fixes.size() < allFixes);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if ((changelist != null)
							&& (changelist.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client.revertFiles(FileSpecBuilder.makeFileSpecList(testRoot+ "/..."),
											new RevertFilesOptions().setChangelistId(changelist.getId()));
							server.deletePendingChangelist(changelist.getId());
						} catch (P4JavaException exc) {
						}
					}
				}
				this.endServerSession(server);
			}
		}
	}
}
