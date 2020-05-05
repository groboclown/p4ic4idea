/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.ShelveFilesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
@TestId("Bugs101_Job040656Test")
public class Job040656Test extends P4JavaRshTestCase {

	public Job040656Test() {
	}

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job040656Test.class.getSimpleName());
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
	public void testJob040656Shelving() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job040656Test";
		final String testFile = testRoot + "/" + "test01.txt";
		IChangelist changelist = null;
		FileStatOutputOptions outputOptions = null;

		try {
			List<IFileSpec> syncList = this.forceSyncFiles(client, testRoot + "/...");
			assertNull(this.reportInvalidSpecs(syncList));
			changelist = client.createChangelist(new Changelist(
										IChangelist.UNKNOWN,
										client.getName(),
										userName,
										ChangelistStatus.NEW,
										null,
										"Bugs101_Job040656Test test integration changelist",
										false,
										(Server) server
								));
			assertNotNull(changelist);
			List<IFileSpec> editList = client.editFiles(
											FileSpecBuilder.makeFileSpecList(testFile),
											new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(editList);
			assertNull(this.reportInvalidSpecs(editList));
			changelist.refresh();
			List<IFileSpec> shelvedFiles = client.shelveFiles(
												FileSpecBuilder.makeFileSpecList(testFile),
												changelist.getId(),
												new ShelveFilesOptions().setForceShelve(true));
			assertNotNull(shelvedFiles);
			assertEquals(1, FileSpecBuilder.getValidFileSpecs(shelvedFiles).size());
			outputOptions = new FileStatOutputOptions();
			outputOptions.setShelvedFiles(true);
			List<IExtendedFileSpec> extList = server.getExtendedFiles(
									FileSpecBuilder.makeFileSpecList(testFile),
									new GetExtendedFilesOptions().setAffectedByChangelist(changelist.getId())
												.setOutputOptions(outputOptions));
			assertNotNull(extList);
			assertTrue(extList.size() >= 1);
			boolean found = false;
			for (IExtendedFileSpec fSpec : extList) {
				assertNotNull(fSpec);
				if ((fSpec.getDepotPathString() != null) && fSpec.getDepotPathString().equals(testFile)) {
					assertTrue("file not shelved", fSpec.isShelved());
					found = true;
					break;
				}
			}
			assertTrue("shelved file not found", found);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if ((changelist != null) && (changelist.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client.revertFiles(FileSpecBuilder.makeFileSpecList(testRoot + "/..."),
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
