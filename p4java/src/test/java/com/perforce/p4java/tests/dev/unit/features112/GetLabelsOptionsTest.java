/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the GetLabelsOptions values and functionality.
 */
@Jobs({ "job046578" })
@TestId("Dev112_GetLabelsOptionsTest")
public class GetLabelsOptionsTest extends P4JavaRshTestCase {

	IClient client = null;
	
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetLabelsOptionsTest.class.getSimpleName());
    
	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
            client = getClient(server);
			ILabel label = Label.newLabel(server, "test-label", "testlabel", new String[]{});
		    server.createLabel(label);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

    @Test
    public void testConstructors() {
        try {
            GetLabelsOptions opts = new GetLabelsOptions();
            assertNotNull(opts.processOptions(null));
            assertEquals(0, opts.processOptions(null).size());

            opts = new GetLabelsOptions("-m10", "-up4jtestuser");
            assertNotNull(opts.getOptions());
            String[] optsStrs = opts.getOptions().toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(optsStrs.length, 2);
            assertEquals("-m10", optsStrs[0]);
            assertEquals("-up4jtestuser", optsStrs[1]);

            opts = new GetLabelsOptions(50, "p4jtestuser", "test-label",
                    true);
            assertEquals(50, opts.getMaxResults());
            assertEquals("p4jtestuser", opts.getUserName());
            assertEquals("test-label", opts.getNameFilter());
            assertTrue(opts.isShowTime());
        } catch (OptionsException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testToStrings() {
        try {
            GetLabelsOptions opts = new GetLabelsOptions(-1, "p4jtestuser",
                    null, true);

            assertNotNull(opts.processOptions(null));
            String[] optsStrs = opts.processOptions(null)
                    .toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(2, optsStrs.length);

            // Order is not guaranteed here, so we have to
            // search for the expected strings at each position

            boolean foundMaxFiles = false;
            boolean foundUserName = false;
            boolean foundNameFilter = false;
            boolean foundShowTime = false;

            for (String optStr : optsStrs) {
                if (optStr.equals("-m50"))
                    foundMaxFiles = true;
                if (optStr.equals("-up4jtestuser"))
                    foundUserName = true;
                if (optStr.equals("-etest-label"))
                    foundNameFilter = true;
                if (optStr.equals("-t"))
                    foundShowTime = true;
            }

            assertFalse(foundMaxFiles);
            assertTrue(foundUserName);
            assertFalse(foundNameFilter);
            assertTrue(foundShowTime);

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test setters and chaining
     */
    @Test
    public void testSetters() {
        try {
            GetLabelsOptions opts = new GetLabelsOptions();
            opts.setMaxResults(100);
            opts.setUserName("p4jtestuser");
            opts.setNameFilter("test-label");
            opts.setShowTime(true);

            assertEquals(100, opts.getMaxResults());
            assertEquals("p4jtestuser", opts.getUserName());
            assertEquals("test-label", opts.getNameFilter());
            assertEquals(true, opts.isShowTime());
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test get labels with show time (-t)
     */
    @Test
    public void testGetLabelsWithShowTime() {

        try {
            // Get the labels with the '-t' option
            List<ILabelSummary> labelSummaries = server
                    .getLabels(null, new GetLabelsOptions().setMaxResults(10)
                            .setNameFilter("test-label")
                            .setUserName("p4jtestuser").setShowTime(true));
            assertNotNull(labelSummaries);

            // There should be one label with the specified name and owned by
            // the specified user
            assertEquals(1, labelSummaries.size());

            Calendar updatedCal = Calendar.getInstance();
            updatedCal.setTime(labelSummaries.get(0).getLastUpdate());

            // Check if the time portion is set. Most likely one of the parts
            // (hours/minutes/seconds) is greater than zero. However, it is
            // possible that they are all zero when the client is
            // created/updated.
            assertTrue(updatedCal.get(Calendar.HOUR) > 0
                    || updatedCal.get(Calendar.MINUTE) > 0
                    || updatedCal.get(Calendar.SECOND) > 0);

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

}
