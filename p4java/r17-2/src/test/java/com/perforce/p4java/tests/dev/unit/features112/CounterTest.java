/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;

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
 * Test the 'p4 counter -f -i <counter>' for incremented protected counters.
 */
@Jobs({ "job046611" })
@TestId("Dev112_CounterTest")
public class CounterTest extends P4JavaTestCase {

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
     * Test incrementing protected counters
     */
    @Test
    public void testIncrementProtectedCounter() {

        try {
            // Get the before value of the protected counter 'job'. Assume this
            // is numeric counter.
            int jobCounterBefore = parseCounterValue(server.getCounter("job"));

            // Check to make sure we got a positive counter value
            assertTrue(jobCounterBefore > 0);

            // Increment protected counter: 'p4 counter -f -i <counter>'
            server.setCounter("job", null, new CounterOptions()
                    .setPerforceCounter(true).setIncrementCounter(true));

            // Get the after value of the protected counter 'job'. Assume this
            // is numeric counter.
            int jobCounterAfter = parseCounterValue(server.getCounter("job"));

            // Check to make sure we got a positive counter value
            assertTrue(jobCounterAfter > 0);

            // Verify it is incremented by 1
            assertTrue(jobCounterAfter == (jobCounterBefore + 1));

            // Reset the counter back to the before value
            server.setCounter("job", String.valueOf(jobCounterBefore),
                    new CounterOptions().setPerforceCounter(true));

            // Get the reset value of the protected counter 'job'. Assume this
            // is numeric counter.
            int jobCounterReset = parseCounterValue(server.getCounter("job"));

            // Check to make sure we got a positive counter value
            assertTrue(jobCounterBefore == jobCounterReset);

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    protected int parseCounterValue(String counterValue) {
        int counter = -1;

        assertNotNull(counterValue);

        try {
            counter = Integer.parseInt(counterValue);
        } catch (NumberFormatException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }

        return counter;
    }
}
