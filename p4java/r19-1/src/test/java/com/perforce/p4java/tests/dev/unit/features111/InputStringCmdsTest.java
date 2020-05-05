/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MapUnmapper;
import com.perforce.p4java.option.server.DeleteLabelOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Simple tests of the new (2011.1) input string exec cmds.
 * Absolutely not intended to be comprehensive.
 */

@TestId("Features111_InputStringCmdsTest")
public class InputStringCmdsTest extends P4JavaRshTestCase {

	public InputStringCmdsTest() {
	}

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", InputStringCmdsTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            Properties properties = new Properties();
            setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
            assertNotNull(server);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }
    
	@Test
	public void testSimpleInputStringMapCmds() {
		ILabel label = null;
		final String labelName = this.getRandomName("Label");
		final String description = "Created for test " + testId;
		final String[] mapping = {"//depot/dev/...", "//depot/test/..."};
		final String cmdName = "label";
		final String[] cmdArgs = new String[] {"-i"};
		
		try {
			label = Label.newLabel(server, labelName, description, mapping);
			assertNotNull("unable to create new label locally", label);
			StringBuffer strBuf = new StringBuffer();
			Map<String, Object> inMap = InputMapper.map(label);
			MapUnmapper.unmapLabelMap(inMap, strBuf);
			assertNotNull("null string buffer returned from MapUnmapper", strBuf);
			Map<String, Object>[] retMaps = server.execInputStringMapCmd(cmdName, cmdArgs, strBuf.toString());
			assertNotNull("null returned from execInputStringMapCmd", retMaps);
			ILabel newLabel = server.getLabel(labelName);
			assertNotNull("could not retrieve new label", newLabel);
			assertEquals("description mismatch", label.getDescription(), newLabel.getDescription());
			assertEquals("owner mismatch", label.getOwnerName(), newLabel.getOwnerName());
			assertEquals("revision spec mismatch", label.getRevisionSpec(), newLabel.getRevisionSpec());
			ViewMap<ILabelMapping> view1 = label.getViewMapping();
			ViewMap<ILabelMapping> view2 = newLabel.getViewMapping();
			assertNotNull("missing view map in label", view1);
			assertNotNull("missing view map in new label", view2);
			assertEquals("view map size mismatch", view1.getSize(), view2.getSize());
			int i = 0;
			for (ILabelMapping labelMapping : view1.getEntryList()) {
				assertNotNull("null label mapping in entry list", labelMapping);
				assertNotNull("null label mapping in entry list (new label)", view2.getEntry(i));
				assertEquals("map entry mismatch", labelMapping.getLeft(), view2.getEntry(i).getLeft());
				i++;
			}
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (label != null) {
					try {
						server.deleteLabel(labelName, new DeleteLabelOptions());
					} catch (P4JavaException e) {
						// ignore
					}
				}
				this.endServerSession(server);
			}
		}
	}
	
	@Test
	public void testSimpleInputStringStreamingCmds() {
		
	}
}
