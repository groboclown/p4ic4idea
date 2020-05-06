/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.features112.DeleteFilesOptionsTest;

/**
 * Tests very basic label creation and deletion functionality.
 * Inspired by job 036573, but applicable to a great deal
 * more than that...
 * 
 * @job job036573
 * @testid LabelCreateDelete01
 */

@Jobs({"job036573"})
@TestId("LabelCreateDelete01")
public class LabelCreateDeleteBasicsTest extends P4JavaRshTestCase {

    private IClient client = null;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", LabelCreateDeleteBasicsTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void beforeEach() throws Exception{
        Properties properties = new Properties();
        setupServer(p4d.getRSHURL(), userName, password, true, properties);
        client = getClient(server);
     }
	/**
	 * Just attempt to create then delete a label; nothing
	 * significant in the view or any other field.
	 */
	@Test
	public void testLabelCreateDeleteAbsoluteBasics() {
		final int MAX_ATTEMPTS = 5;
		try {
			String newLabelName = getRandomLabelName(testId);
			
			ILabel newLabel = null;
			int i = 0;
			
			for (newLabel = server.getLabel(newLabelName); newLabel != null;
								newLabel = server.getLabel(newLabelName)) {
				if (i++ >= MAX_ATTEMPTS) {
					fail("Unable to find an unused label name on server after "
							+ MAX_ATTEMPTS + " attempts");
				}
			}
			
			newLabel = new Label(
						newLabelName,
						getUserName(),
						null,	// lastAccess
						null,	// lastUpdate
						"Temporary label created for test " + testId,
						null,	// revisionSpec
						false,	// locked
						new ViewMap<ILabelMapping>()
					);
			
			String createResult = server.createLabel(newLabel);
			assertNotNull("Null create label result string returned", createResult);
			assertTrue ("Bad create label result string: '" + createResult + "'",
											createResult.contains("saved"));
			
			ILabel createdLabel = server.getLabel(newLabelName);
			assertNotNull("Unable to retrieve newly-created label '" + createResult + "'",
											createdLabel);
			
			String deleteResult = server.deleteLabel(newLabelName, false);
			assertNotNull("Null label delete string result returned from server",
								deleteResult);
			assertTrue("Unexpected label deletion string returned from server: '"
								+ deleteResult + "'", deleteResult.contains("deleted"));
			
			ILabel deletedLabel = server.getLabel(newLabelName);
			assertNull("Label '" + newLabelName + "' not actually deleted from server",
									deletedLabel);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getMessage());
		}
	}
	
	private String getRandomLabelName(String str) {
		return this.testPrefix
				+ (str == null ? "" : str)
				+ "Label"
				+ Math.abs(this.rand.nextInt(9999));
	}
}
