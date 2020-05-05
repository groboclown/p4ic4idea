/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 */
@TestId("Bugs101_Job040205Test")
public class Job040205Test extends P4JavaRshTestCase {

	public Job040205Test() {
	}

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job040205Test.class.getSimpleName());

	IClient client = null;
	
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
	public void testDeletedActionSet() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job040205Test";
		final String testFile = testRoot + "/" + "test.txt";
		IChangelist changelist = null;

		try {
			this.forceSyncFiles(client, testRoot + "/...");
			List<IFileSpec> syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFile + "#0"),
														new SyncOptions().setForceUpdate(true));
			assertNotNull(syncFiles);
			for (IFileSpec syncFile : syncFiles) {
				assertNotNull(syncFile);
				if (syncFile.getOpStatus() == FileSpecOpStatus.VALID) {
					if (syncFile.getDepotPathString().equals(testFile)) {
						assertNotNull("file action is null", syncFile.getAction());
						assertEquals("", FileAction.DELETED, syncFile.getAction());
					}
				}
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
				}
				this.endServerSession(server);
			}
		}
	}
}
