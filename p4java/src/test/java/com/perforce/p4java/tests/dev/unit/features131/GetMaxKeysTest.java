/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.option.server.GetKeysOptions;
import com.perforce.p4java.option.server.KeyOptions;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test the 'p4 keys -m max'.
 */
@Jobs({ "job062032" })
@TestId("Dev131_GetMaxKeysTest")
public class GetMaxKeysTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", GetMaxKeysTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			server = getSuperConnection(p4d.getRSHURL());
			assertNotNull(server);
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

    /**
     * Test the 'p4 keys -m max'.
     */
    @Test
	public void testGetMaxKeys() {

		String result = null;

		try {
			// Set 10 keys
			for (int i = 0; i < 10; i++) {
				result = server.setKey("testkey" + i, "10" + i,
						new KeyOptions());
				assertNotNull(result);
			}

			// Get max number (5) of keys
			Map<String, String> keys = server
					.getKeys(new GetKeysOptions().setMaxResults(5));
			assertNotNull(keys);
			assertEquals(5, keys.size());

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
