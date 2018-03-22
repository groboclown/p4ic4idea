/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the '-u' option for 'p4 counter' and 'p4 counters'.
 * <p>
 * 
 * There is a new table, db.nameval, that is used solely for storing/accessing
 * the commons counters. Also, there are two new deep undoc commands to access,
 * set, and delete these counters.
 * <p>
 * 
 * p4 counters -u shows all counters in db.nameval only
 * <p>
 * p4 counter -u <usual counter flags & args> show, delete, change a counter in
 * db.nameval table only
 * <p>
 * 
 * Without the -u flag, counters in db.nameval are not displayed or set.
 * Likewise, using -u will only access counters from db.nameval.
 * 
 */
@Jobs({ "job055903" })
@TestId("Dev121_CounterTest")
public class UndocCounterTest extends P4JavaTestCase {

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
     * Test 'p4 counter -u' - set, change, show, delete in db.nameval
     * Test 'p4 counters -u' - show all counters in db.nameval
     */
    @Test
    public void testUndocCounter() {

        try {
        	// Set a new counter using -u (db.nameval)
            // 'p4 counter -u <counter> <value>'
            String result = server.setCounter("utestcounter", "10", new CounterOptions()
                    .setUndocCounter(true));
        	assertNotNull(result);
        	
            // Should NOT be in the general table
            String countervalue = server.getCounter("utestcounter");
            assertNotNull(countervalue);
            assertEquals(countervalue, "0");
        	
        	// Should be in the db.nameval
            countervalue = server.getCounter("utestcounter", new CounterOptions().setUndocCounter(true));
            assertNotNull(countervalue);
            assertEquals(countervalue, "10");
            
            // Increment this counter
            result = server.setCounter("utestcounter", null, new CounterOptions()
                    .setUndocCounter(true).setIncrementCounter(true));
            assertNotNull(result);
        	
            // Set another counter in the db.nameval table
            result = server.setCounter("utestcounter2", "20", new CounterOptions()
            .setUndocCounter(true));
            assertNotNull(result);

            // Increment this counter
            result = server.setCounter("utestcounter2", null, new CounterOptions()
                    .setUndocCounter(true).setIncrementCounter(true));
            assertNotNull(result);

            // Should be 21
            countervalue = server.getCounter("utestcounter2", new CounterOptions().setUndocCounter(true));
            assertNotNull(countervalue);
            assertEquals(countervalue, "21");

            // Get all the counters from db.nameval
            Map<String, String> counters = server.getCounters(new CounterOptions().setUndocCounter(true));
            assertNotNull(counters);
            assertTrue(counters.containsKey("utestcounter"));
            assertEquals((String)counters.get("utestcounter"), "11");
            assertTrue(counters.containsKey("utestcounter2"));
            assertEquals((String)counters.get("utestcounter2"), "21");
            
            // Delete these counters 
            result = server.setCounter("utestcounter", null, new CounterOptions()
            .setUndocCounter(true).setDelete(true));
            assertNotNull(result);
            result = server.setCounter("utestcounter2", null, new CounterOptions()
            .setUndocCounter(true).setDelete(true));
            assertNotNull(result);

            // Should not have those counters in db.nameval
            counters = server.getCounters(new CounterOptions().setUndocCounter(true));
            assertNotNull(counters);
            assertFalse(counters.containsKey("utestcounter"));
            assertFalse(counters.containsKey("utestcounter2"));
        
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }
}
