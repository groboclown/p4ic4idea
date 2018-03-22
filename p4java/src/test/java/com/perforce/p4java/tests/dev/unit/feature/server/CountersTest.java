/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple tests for counter retrieval and set / increment / delete, etc.
 * Needs Perforce superuser permissions and login.
 */
@TestId("Server_CountersTest")
public class CountersTest extends P4JavaTestCase {

	public CountersTest() {
	}

	@Test
	public void testCounters() {
		final String TESTCOUNTER_NAME = "p4jtestCounter";
		
		IOptionsServer server = null;

		try {
			server = this.getOptionsServer(
							this.serverUrlString,
							null,
							this.superUserName,
							this.superUserPassword);
			Map<String, String> counters = server.getCounters();
			assertNotNull(counters);
			assertTrue(counters.containsKey("change"));
			assertTrue("Main test counter missing", counters.containsKey(TESTCOUNTER_NAME));
			String counterVal = server.getCounter(TESTCOUNTER_NAME);
			assertNotNull(counterVal);
			String newCounterVal = server.setCounter(TESTCOUNTER_NAME, null,
											new CounterOptions().setIncrementCounter(true));
			counterVal = server.getCounter(TESTCOUNTER_NAME);
			assertEquals(counterVal, newCounterVal);
			newCounterVal = server.setCounter(TESTCOUNTER_NAME,
									"" + ((new Integer(counterVal)).intValue() - 1), null);
			assertNotNull(newCounterVal);
			counterVal = server.getCounter(TESTCOUNTER_NAME);
			assertNotNull(counterVal);
			assertEquals(counterVal, newCounterVal);
			String createdCounterName = TESTCOUNTER_NAME + this.getRandomName(null);
			String createdCounter = server.setCounter(createdCounterName, "abc", null);
			assertNotNull(createdCounter);
			assertEquals(createdCounter, server.getCounter(createdCounterName));
			server.deleteCounter(createdCounterName, false);
			counters = server.getCounters();
			assertNotNull(counters);
			assertFalse(counters.containsKey(createdCounterName));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
