/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.client;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simple test of a sync / edit / revert cycle.
 */

@TestId("Client_SimpleEditRevertTest")
public class SimpleEditRevertTest extends P4JavaRshTestCase {

	public SimpleEditRevertTest() {
		super();
	}

	private IClient client = null;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SimpleEditRevertTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void beforeEach() throws Exception{
        Properties properties = new Properties();
        setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
        client = getClient(server);
     }
    
	@Test
	public void testSimpleEditRevert() {
		final String editRoot = "//depot/client/SimpleEditRevertTest/...";
		IChangelist changelist = null;
		try {
			List<IFileSpec> revertFiles = client.revertFiles(
								FileSpecBuilder.makeFileSpecList(editRoot),
								false,
								IChangelist.UNKNOWN,
								false,
								false);
			assertNotNull(revertFiles);
			this.forceSyncFiles(client, editRoot);
			changelist = createChangelist(
							client.getName(),
							server,
							"SimpleEditRevertTest changelist");
			changelist = client.createChangelist(changelist);
			client.refresh();
			List<IFileSpec> editList = client.editFiles(
					FileSpecBuilder.makeFileSpecList(editRoot), false, false, changelist.getId(), null);
			assertNotNull(editList);
			List<IFileSpec> badList = FileSpecBuilder.getInvalidFileSpecs(editList);
			assertNotNull(badList);
			assertEquals("non-zero errors in edit list return", 0, badList.size());
			List<IFileSpec> openedList = client.openedFiles(null, 0, changelist.getId());
			assertNotNull(openedList);
			assertEquals(editList.size(), openedList.size());
			revertFiles = client.revertFiles(
					FileSpecBuilder.makeFileSpecList(editRoot),
					false,
					IChangelist.UNKNOWN,
					false,
					false);
					assertNotNull(revertFiles);
			String retVal = server.deletePendingChangelist(changelist.getId());
			assertNotNull(retVal);
			assertTrue("changelist not deleted: " + retVal,
					retVal.equalsIgnoreCase("Change " + changelist.getId() + " deleted."));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			endServerSession(server);
		}
	}
}
