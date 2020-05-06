/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;

/**
 * Tests locking issue with job040562.
 */
@TestId("Bugs101_Job040562Test")
public class Job040562Test extends P4JavaRshTestCase {

	public Job040562Test() {
	}
	IClient client = null;
	
	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job040562Test.class.getSimpleName());

	/**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        try {
            setupServer(p4d.getRSHURL(), userName, password, true, props);
            client = getClient(server);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        } 
    }
    
	@Test
	public void testJob040562Locking() {
		final String testFile1 = "//depot/101Bugs/Bugs101_Job040562Test/test01.txt";
		final String testFile2 = "//depot/101Bugs/Bugs101_Job040562Test/test02.txt";
		IChangelist changelist = null;

		try {
			forceSyncFiles(client, "//depot/101Bugs/Bugs101_Job040562Test/...");
			List<IFileSpec> files1 = client.editFiles(FileSpecBuilder.makeFileSpecList(testFile1), null);
			assertNotNull(files1);
			assertEquals(1, files1.size());
			assertEquals(FileSpecOpStatus.VALID, files1.get(0).getOpStatus());
			changelist = client.createChangelist(new Changelist(
															IChangelist.UNKNOWN,
															client.getName(),
															this.getUserName(),
															ChangelistStatus.NEW,
															null,
															"Bugs101_Job040562Test test changelist",
															false,
															(Server) server
														));
			assertNotNull(changelist);
			List<IFileSpec> files2 = client.editFiles(
												FileSpecBuilder.makeFileSpecList(testFile2),
												new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files2);
			assertEquals(1, files2.size());
			assertEquals(FileSpecOpStatus.VALID, files2.get(0).getOpStatus());
			changelist.refresh();
			assertFalse(files1.get(0).isLocked());
			assertFalse(files2.get(0).isLocked());
			List<IFileSpec> lockFiles = client.lockFiles(null, changelist.getId());
			assertNotNull(lockFiles);
			assertEquals(1, lockFiles.size());
			List<IExtendedFileSpec> extFiles = server.getExtendedFiles(lockFiles, null);
			assertTrue(extFiles.get(0).isLocked());
			client.unlockFiles(lockFiles, null);
			extFiles = server.getExtendedFiles(lockFiles, null);
			assertFalse(extFiles.get(0).isLocked());
			client.unlockFiles(lockFiles, null);
			lockFiles = client.lockFiles(null, IChangelist.DEFAULT);
			assertNotNull(lockFiles);
			extFiles = server.getExtendedFiles(FileSpecBuilder.makeFileSpecList(testFile2), null);
			assertFalse(extFiles.get(0).isLocked());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					try {
							client.revertFiles(FileSpecBuilder.makeFileSpecList(
												"//depot/101Bugs/Bugs101_Job040562Test/..."),
												null);
						if (changelist != null) {
							server.deletePendingChangelist(changelist.getId());
						}
					} catch (P4JavaException exc) {
					}
				}
				this.endServerSession(server);
			}
		}
	}
}
