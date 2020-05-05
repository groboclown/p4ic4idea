/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * 
 * Test integrate from a remote depot. User indicates "IMPORT" action is used.
 * However, P4Java says it is an unknown action type, returned null.
 * 
 * Server: perforce:1666
 * Command: p4 describe 225655
 * Result: ... //depot/r09.2/p4-bin/bin.tools/p4ruby.tgz#1 import
 * 
 * Fixed: Updated the FileAction.java to include "import".
 * 
 * This test is motivated by job039331.<p>
 * 
 * @job job039331
 * @testid Job039331Test
 */

@TestId("Job039331Test")
@Jobs({"job039331"})
public class Job039331Test extends P4JavaRshTestCase {
	
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job039331Test.class.getSimpleName());

	/**
	 * Code adapted from user's example in job 039331 description.
	 */
	@Test
	public void testFileActionImport() {
		final int changeListId = 1751;
		
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);

			IChangelist changelist = server.getChangelist(changeListId);
			assertNotNull("Null changelist returned", changelist);
			
			List<IFileSpec> files = changelist.getFiles(false);
			assertNotNull("Null changelist files returned", files);
			
			for (IFileSpec fSpec : files) {
				assertNotNull("Null file spec returned", fSpec);
				assertNotNull("Null file action returned", fSpec.getAction());
				assertEquals(fSpec.getAction(), (FileAction.IMPORT));
			}
			
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
