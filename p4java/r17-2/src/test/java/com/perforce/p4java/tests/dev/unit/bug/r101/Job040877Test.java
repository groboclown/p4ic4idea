/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests for job fixes not being reflected back into the
 * changelist during a changelist refresh.
 */
@TestId("Bugs101_Job040877Test")
public class Job040877Test extends P4JavaTestCase {

	public Job040877Test() {
	}

	@Test
	public void testFixJobs() {
		final String testJobId = "job000802";
		final String testRoot = "//depot/101Bugs/Bugs101_Job040877Test";
		final String testFile = testRoot + "/test01.txt";
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			this.forceSyncFiles(client, testRoot + "/...");
			changelist = client.createChangelist(new Changelist(
													IChangelist.UNKNOWN,
													client.getName(),
													this.getUserName(),
													ChangelistStatus.NEW,
													null,
													"Bugs101_Job040877Test test fixes changelist",
													false,
													(Server) server
												));
			assertNotNull(changelist);
			List<IFileSpec> editFiles = client.editFiles(
									FileSpecBuilder.makeFileSpecList(testFile),
									new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(editFiles);
			changelist.refresh();
			List<String> jobs = new ArrayList<String>();
			jobs.add(testJobId);
			List<IFix> fixes = server.fixJobs(jobs, changelist.getId(), null, false);
			assertNotNull(fixes);
			changelist.refresh();
			assertNotNull(changelist.getCachedJobIdList());
			assertTrue("empty job id list in changelist",
					changelist.getCachedJobIdList().size() > 0);
			boolean found = false;
			for (String jobId : changelist.getCachedJobIdList()) {
				assertNotNull(jobId);
				if (jobId.equals(testJobId)) {
					found = true;
					break;
				}
			}
			assertTrue("target fixed job not in changelist job list", found);
			List<IFileSpec> submitList = changelist.submit(null);
			assertNotNull(submitList);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if ((changelist != null)
							&& (changelist.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client.revertFiles(FileSpecBuilder.makeFileSpecList(testRoot + "/..."),
															new RevertFilesOptions()
																.setChangelistId(changelist
																.getId()));
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
