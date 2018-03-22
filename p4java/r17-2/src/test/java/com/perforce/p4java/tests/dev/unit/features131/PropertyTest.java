/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.admin.IProperty;
import com.perforce.p4java.option.server.GetPropertyOptions;
import com.perforce.p4java.option.server.PropertyOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the 'p4 property' command
 */
@Jobs({ "job059605" })
@TestId("Dev131_PropertyTest")
public class PropertyTest extends P4JavaTestCase {
	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() throws Exception{
		server = getServerAsSuper();
		assertNotNull(server);
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		afterEach(server);
	}

    /**
     * Test set/get/delete property values
     */
    @Test
	public void testPropertyValues() {

		String result = null;

		try {
			// Set property values
			for (int i = 0; i < 3; i++) {
				result = server.setProperty("XpropName_" + i, "propVal_" + i,
						new PropertyOptions().setSequence(i+1));
				assertTrue(result.contains("Property XpropName_" + i));
			}
			for (int i = 0; i < 3; i++) {
				result = server.setProperty("YpropName_" + i, "propVal_" + i,
						new PropertyOptions().setSequence(i+1));
				assertTrue(result.contains("Property YpropName_" + i));
			}
			for (int i = 0; i < 3; i++) {
				result = server.setProperty("ZpropName_" + i, "propVal_" + i,
						new PropertyOptions().setSequence(i+1));
				assertTrue(result.contains("Property ZpropName_" + i));
			}

			// Get property values
			List<IProperty> props = server
					.getProperty(new GetPropertyOptions()
							.setListAll(true)
							.setFilter("name=XpropName*")
							.setMax(4)
							);

			assertNotNull(props);
			assertEquals(3, props.size());

			props = server
					.getProperty(new GetPropertyOptions()
							.setListAll(true)
							.setMax(5)
							);

			assertNotNull(props);
			assertEquals(5, props.size());

			props = server
					.getProperty(new GetPropertyOptions()
							.setListAll(true)
							);

			assertNotNull(props);
			assertTrue(props.size() >= 9);

			// Delete property values
			for (int i = 0; i < 3; i++) {
				result = server.deleteProperty("XpropName_" + i,
						new PropertyOptions().setSequence(i+1));
				assertEquals("Property XpropName_" + i + " deleted.", result);
			}
			for (int i = 0; i < 3; i++) {
				result = server.deleteProperty("YpropName_" + i,
						new PropertyOptions().setSequence(i+1));
				assertEquals("Property YpropName_" + i + " deleted.", result);
			}
			for (int i = 0; i < 3; i++) {
				result = server.deleteProperty("ZpropName_" + i,
						new PropertyOptions().setSequence(i+1));
				assertEquals("Property ZpropName_" + i + " deleted.", result);
			}
			
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

    /**
     * Test property values with single and double quotes.
     */
    @Test
	public void testSpecialPropertyValues() {

		String result = null;

		try {
			// Set property values
			result = server.setProperty("proptest_" + "job059605",
					"tester's test", new PropertyOptions().setSequence(8));
			assertTrue(result.contains("proptest_" + "job059605"));

			result = server.setProperty("proptest2_" + "job059605",
					"tester\"s test", new PropertyOptions().setSequence(9));
			assertTrue(result.contains("proptest2_" + "job059605"));

			// Get property values
			List<IProperty> props = server.getProperty(new GetPropertyOptions()
					.setListAll(true)
					.setFilter("name=" + "proptest*_" + "job059605").setMax(8));

			assertNotNull(props);
			assertEquals(2, props.size());

			// Delete property values
			result = server.deleteProperty("proptest_" + "job059605", new PropertyOptions().setSequence(8));
			assertTrue(result.contains("proptest_" + "job059605" + " deleted."));

			result = server.deleteProperty("proptest2_" + "job059605", new PropertyOptions().setSequence(9));
			assertTrue(result.contains("proptest2_" + "job059605" + " deleted."));

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

    /**
     * Test the setProperty method with null property name or value
     */
    @Test
	public void testSetPropertyValues() {

        String result = null;

        // Set property value error - null name
        try {
                result = server.setProperty(null,
                                "tester's test", new PropertyOptions().setSequence(8));
        } catch (IllegalArgumentException iae) {
                assertTrue(iae.getLocalizedMessage().contains("Property/option name shouldn't null or empty"));
        } catch (Exception exc) {
                fail("Unexpected exception: " + exc);
        }
        try {
                result = server.setProperty(null,
                                "tester's test", new PropertyOptions().setName("proptest_" + "job059605").setSequence(8));
                assertTrue(result.contains("proptest_" + "job059605"));
        } catch (Exception exc) {
                fail("Unexpected exception: " + exc.getLocalizedMessage());
        }

        // Set property value error - null value
        try {
                result = server.setProperty("proptest2_" + "job059605",
                                null, new PropertyOptions().setSequence(9));
        } catch (IllegalArgumentException iae) {
                assertTrue(iae.getLocalizedMessage().contains("Property/option value shouldn't null or empty"));
        } catch (Exception exc) {
                fail("Unexpected exception: " + exc);
        }
        try {
                result = server.setProperty("proptest2_" + "job059605",
                                null, new PropertyOptions().setValue("tester\"s test").setSequence(9));
                assertTrue(result.contains("proptest2_" + "job059605"));
        } catch (Exception exc) {
                fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }
}
