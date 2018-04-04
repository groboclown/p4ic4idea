/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r92;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test that file revision end specs are honored by the
 * filespec getRevisionHistory method; motivated by job035290.<p>
 * 
 * Doesn't need a working client -- goes direct to the depot. Does
 * need a simple test file that was not integrated from anywhere else
 * or even moved form elsewhere so the revListMap has only
 * on key filespec...
 * 
 * @job job035290
 * @testid RevisionHistory01Test
 */

@TestId("Job035290Test")
@Jobs({"job035290"})
public class Job035290Test extends P4JavaTestCase {
	
	/**
	 * Code adapted from user's example in job 035290 description.
	 */
	@Test
	public void testBasic() {
		final int maxRevs = 4;
		final String testFileName = "//depot/92bugs/" + testId + "/" + testId + "New.txt#" + maxRevs;
		
		IServer server = null;
		
		try {
			server = getServer();
			assertNotNull("Null server returned", server);
			
			IFileSpec filespec = server.getDepotFiles(
				   FileSpecBuilder.makeFileSpecList(new String[] {testFileName}), true).get(0);
			assertNotNull("Null filespec returned", filespec);
			assertEquals(filespec.getStatusMessage(), FileSpecOpStatus.VALID, filespec.getOpStatus());
			assertEquals(maxRevs, filespec.getEndRevision());
			
			Map<IFileSpec, List<IFileRevisionData>> revListMap =
				   filespec.getRevisionHistory(0, false, true, true, true);
			
			assertNotNull("Null rev list map returned", revListMap);			
			for (IFileSpec fSpec: revListMap.keySet()) {
				assertNotNull(fSpec);
				List<IFileRevisionData> revList = revListMap.get(fSpec);
				assertNotNull(revList);
				assertEquals(maxRevs, revList.size());
			}
			 
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
