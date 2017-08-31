/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 *
 */
@TestId("Bugs101_Job039015Test")
public class Job039015Test extends P4JavaTestCase {

	public Job039015Test() {
	}

	@Test
	public void testJob039015AuthException() {
		final String testRoot = "//depot/101Bugs/Job039015Test";
		final String testFile01 = testRoot + "/" + "test01.txt";
		final String testFile02 = testRoot + "/" + "test02.txt";
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;
		InputStream diffStream = null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			this.forceSyncFiles(client, testRoot + "/...");
			diffStream = server.getServerFileDiffs(new FileSpec(testFile01), new FileSpec(testFile02),
															null, null, false, false, false);
			assertNotNull(diffStream);
			server.logout();
			try {
				diffStream = server.getServerFileDiffs(new FileSpec(testFile01), new FileSpec(testFile02),
						null, null, false, false, false);
				fail("did not see expected access exception after logging out");
			} catch (AccessException aexc) {
				// success...
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if ((changelist != null)
							&& (changelist.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client
									.revertFiles(
											FileSpecBuilder
													.makeFileSpecList(testRoot
															+ "/..."),
											new RevertFilesOptions()
													.setChangelistId(changelist
															.getId()));
							server.deletePendingChangelist(changelist.getId());
						} catch (P4JavaException exc) {
						}
					}
					if (diffStream != null) {
						try {
							diffStream.close();
						} catch (IOException exc) {
						}
					}
				}
				this.endServerSession(server);
			}
		}
	}
}
