/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r111;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.SetFileAttributesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests for file attribute visibility when a file is open
 * for edit (Job042258).
 */
@TestId("Bugs111_Job042258Test")
public class Job042258Test extends P4JavaRshTestCase {

	public Job042258Test() {
	}

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job042258Test.class.getSimpleName());

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
	public void testAttributeRetrieval() {
		IChangelist changelist = null;
		final String description = "Test changelist for test " + testId;
		final String testRoot = "//depot/111bugs/Bugs111_Job042258Test";
		final String testFile = testRoot + "/test01.txt";
		final List<IFileSpec> testFiles = FileSpecBuilder.makeFileSpecList(testFile);
		final String attrName = this.getRandomName("test1");
		final String attrValue = this.getRandomName("value");
		
		try {
			List<IFileSpec> syncFiles = this.forceSyncFiles(client, testRoot + "/...");
			assertNotNull("bad forced sync", syncFiles);
			changelist = CoreFactory.createChangelist(client, description, true);
			assertNotNull("changelist not created", changelist);
			List<IFileSpec> editFiles = client.editFiles(testFiles,
								new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull("null file list returned from edit", editFiles);
			assertEquals("edit error", 0, FileSpecBuilder.getInvalidFileSpecs(editFiles).size());
			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put(attrName, attrValue);
			List<IFileSpec> attrSpecs = server.setFileAttributes(testFiles, attributes,
												new SetFileAttributesOptions());
			assertNotNull("null filespecs from set attributes operation", attrSpecs);
			assertEquals("set attributes error", 0, FileSpecBuilder.getInvalidFileSpecs(attrSpecs).size());
			FileStatAncilliaryOptions fsaOpts = new FileStatAncilliaryOptions();
			fsaOpts.setShowAttributes(true);
			List<IExtendedFileSpec> statFiles = server.getExtendedFiles(testFiles,
													new GetExtendedFilesOptions().setAncilliaryOptions(fsaOpts));
			assertNotNull("null return from fstat", statFiles);
			IExtendedFileSpec attrFile = statFiles.get(0);
			assertNotNull("null attribute file", attrFile);
			assertNotNull("null attributes in retrieved open file", attrFile.getAttributes());
			assertNotNull("expected file attribute missing", attrFile.getAttributes().get(attrName));
			assertEquals("expected attribute value wrong",
								attrValue, new String(attrFile.getAttributes().get(attrName)));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if ((client != null) && (changelist != null)) {
					try {
						client.revertFiles(testFiles,
								new RevertFilesOptions().setChangelistId(changelist.getId()));
						server.deletePendingChangelist(changelist.getId());
					} catch (P4JavaException e) {
						// at least we tried...
					}
				}
				this.endServerSession(server);
			}
		}
	}
}
