/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.GetCountersOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the 'p4 counters -u -e[nameFilter] -e[nameFilter] -e[nameFilter] ...'
 */
@Jobs({ "job062715" })
@TestId("Dev131_UndocCountersFiltersTest")
public class UndocCountersFiltersTest extends P4JavaTestCase {

	IOptionsServer server = null;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = getServerAsSuper();
			assertNotNull(server);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
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
     * Test processing multiple filters.
     */
    @Test
    public void testProcessingFilters() {
    	
    	String[] filters = new String[] {"abc", "xyz", "best", "worst"};
    	
        try {
        	GetCountersOptions opts = new GetCountersOptions();
            opts.setNameFilters(filters);
            
            opts.setNameFilter("second");
            
            List<String> options = opts.processOptions(server);
            assertNotNull(options);
 
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }
	
    /**
     * Test the 'p4 counters -u -e[nameFilter] -e[nameFilter] -e[nameFilter] ...'
     */
    @Test
	public void testGetUndocCountersWithFilters() {

		String result = null;

		try {
			// Set "undoc" counters
			for (int i = 0; i < 3; i++) {
				result = server.setCounter("Xtestcounter" + i, "10" + i,
						new CounterOptions().setUndocCounter(true));
				assertNotNull(result);
			}
			for (int i = 0; i < 3; i++) {
				result = server.setCounter("Ytestcounter" + i, "10" + i,
						new CounterOptions().setUndocCounter(true));
				assertNotNull(result);
			}
			for (int i = 0; i < 3; i++) {
				result = server.setCounter("Ztestcounter" + i, "10" + i,
						new CounterOptions().setUndocCounter(true));
				assertNotNull(result);
			}

			// Get max number (9) of "undoc" counters with 3 filters
			Map<String, String> counters = server
					.getCounters(new GetCountersOptions()
							.setUndocCounter(true)
							.setMaxResults(9)
							.setNameFilters(
									new String[] { 
											"Xtestcounter*",
											"Ytestcounter*",
											"Ztestcounter*" }));

			assertNotNull(counters);
			assertEquals(9, counters.size());

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
