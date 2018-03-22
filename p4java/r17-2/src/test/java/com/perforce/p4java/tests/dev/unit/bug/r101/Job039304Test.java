/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * 
 * Test of annoted paths are handled in IServer.getProtectionEntries().
 * 
 * This test is motivated by job039304.<p>
 * 
 * @job job039304
 * @testid Job039304Test
 */

@TestId("Job039304Test")
@Jobs({"job039304"})
public class Job039304Test extends P4JavaTestCase {

	@Test
	public void testProtectionEntriesWithAnnotedPaths() {

		final String LISTTEST_ROOT_REV = "//depot/basic/readonly/list/...#1";

		IServer server = null;
		
		try {
			server = getServerAsSuper();
			assertNotNull("Null server returned", server);

			List<IProtectionEntry> files = server.getProtectionEntries(true, null, null, null,
							FileSpecBuilder.makeFileSpecList(LISTTEST_ROOT_REV));
			assertNotNull(files);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
