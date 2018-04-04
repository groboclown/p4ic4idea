/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.GetChangelistDiffsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Note that this test is somewhat brittle; for example,
 * if the changelist number changes or the describe output is even
 * slightly different in the next release, things will fail...
 */
@TestId("Bugs101_Job040762Test")
public class Job040762Test extends P4JavaTestCase {

	public Job040762Test() {
	}

	@Test
	public void testMethodTemplate() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job040762Test";
		final int changelistId = 6421; // Will need to change over time -- HR.
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
			changelist = server.getChangelist(changelistId);
			assertNotNull("test changelist missing", changelist);
			diffStream = changelist.getDiffsStream(new GetChangelistDiffsOptions().setSummaryDiff(true));
			LineNumberReader inReader = new LineNumberReader(new InputStreamReader(diffStream));
			int line = 0;
			for (String inLine = inReader.readLine(); inLine != null; inLine = inReader.readLine()) {
				if (line == 6) {
					assertEquals("... //depot/101Bugs/Bugs101_Job040762Test/test01.txt#2 edit", inLine);
				}
				line++;
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if ((changelist != null)
							&& (changelist.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client.revertFiles(
									FileSpecBuilder.makeFileSpecList(testRoot + "/..."),
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
