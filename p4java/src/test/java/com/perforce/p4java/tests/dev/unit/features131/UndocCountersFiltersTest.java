/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.GetCountersOptions;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test the 'p4 counters -u -e[nameFilter] -e[nameFilter] -e[nameFilter] ...'
 */
@Jobs({ "job062715" })
@TestId("Dev131_UndocCountersFiltersTest")
public class UndocCountersFiltersTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", UndocCountersFiltersTest.class.getSimpleName());

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
