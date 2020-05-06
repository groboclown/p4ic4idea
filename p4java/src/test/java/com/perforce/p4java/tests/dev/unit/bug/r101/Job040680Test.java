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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
@TestId("Bugs101_Job040680Test")
public class Job040680Test extends P4JavaRshTestCase {

	public Job040680Test() {
	}

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job040680Test.class.getSimpleName());

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
	public void testExtendedFilesResults() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job040680Test";
		final String testFile = testRoot + "/" + "test.txt";
		IChangelist changelist = null;

		try {
			this.forceSyncFiles(client, testRoot + "/...");
			changelist = client.createChangelist(new Changelist(
											IChangelist.UNKNOWN,
											client.getName(),
											userName,
											ChangelistStatus.NEW,
											null,
											"Bugs101_Job040680Test test changelist",
											false,
											(Server) server
										));
			assertNotNull(changelist);
			List<IFileSpec> editList = client.editFiles(FileSpecBuilder.makeFileSpecList(testFile),
													new EditFilesOptions().setChangelistId(changelist.getId()));

			assertNotNull(editList);
			assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());
			changelist.refresh();

			List<IFileSpec> shelveList = client.shelveChangelist(changelist);
			assertNotNull(shelveList);
			assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());
			FileStatOutputOptions outputOptions = new FileStatOutputOptions();
			outputOptions.setShelvedFiles(true);
			List<IFileSpec> files = changelist.getFiles(false);
			List<IExtendedFileSpec> fstatFiles = server.getExtendedFiles(files, new GetExtendedFilesOptions()
															.setOutputOptions(outputOptions)
															.setAffectedByChangelist(changelist.getId()));
			assertNotNull(fstatFiles);
			for (IExtendedFileSpec file : fstatFiles) {
			    assertNotNull("File was null", file.getDepotPath() );
			    assertTrue("File was not shelved", file.isShelved() );
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
