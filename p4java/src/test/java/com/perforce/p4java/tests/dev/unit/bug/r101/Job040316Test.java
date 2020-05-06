/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
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
@TestId("Bugs101_Job040316Test")
public class Job040316Test extends P4JavaRshTestCase {

	public Job040316Test() {
	}
	
	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job040316Test.class.getSimpleName());

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
	public void testJob040316Issue() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job040316Test";
		IChangelist changelist = null;

		try {
			this.forceSyncFiles(client, testRoot + "/...");
			changelist = client.createChangelist(new Changelist(
					IChangelist.UNKNOWN,
					client.getName(),
					this.getUserName(),
					ChangelistStatus.NEW,
					null,
					"Bugs101_Job040316Test test changelist",
					false,
					(Server) server
				));
			assertNotNull(changelist);
			List<IFileSpec> editFiles = client.editFiles(FileSpecBuilder.makeFileSpecList(testRoot + "/..."),
												new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(editFiles);
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(editFiles).size());
			List<IFileSpec> openedFiles = client.openedFiles(
										FileSpecBuilder.makeFileSpecList(testRoot + "/..."),
										new OpenedFilesOptions());
			assertNotNull(openedFiles);
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(openedFiles).size());
			assertEquals(2, openedFiles.size());
			openedFiles = client.openedFiles(
										FileSpecBuilder.makeFileSpecList(testRoot + "/..."),
										new OpenedFilesOptions().setMaxFiles(1));
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(openedFiles).size());
			assertEquals(1, openedFiles.size());
			openedFiles = client.openedFiles(
										FileSpecBuilder.makeFileSpecList(testRoot + "/..."),
										new OpenedFilesOptions("-m1"));
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(openedFiles).size());
			assertEquals(1, openedFiles.size());
			openedFiles = client.openedFiles(
										FileSpecBuilder.makeFileSpecList(testRoot + "/..."),
										new OpenedFilesOptions("-m1", "-uhreid"));
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(openedFiles).size());
			assertEquals(0, openedFiles.size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					try {
						client.revertFiles(FileSpecBuilder.makeFileSpecList(
												testRoot + "/..."),
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
