/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Simple test scaffold for 10.1 IOptionsServer development. Probably
 * not a particularly useful test under normal conditions.
 */

@Jobs({"job039408"})
@TestId("Dev101_OptionsServerFactoryTest")
public class OptionsServerFactoryTest extends P4JavaRshTestCase {
	
	public OptionsServerFactoryTest() {
		super();
	}

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", OptionsServerFactoryTest.class.getSimpleName());

	/**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() throws Exception {
        // initialization code (before each test).
        try {
            setupServer(p4d.getRSHURL(), userName, password, true, props);
            } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }
	@Test
	public void testServerFactoryBasics() {
		try {
			server.disconnect();

			server = ServerFactory.getOptionsServer(p4d.getRSHURL(), null);
			assertNotNull(server);
			server.connect();
			server.disconnect();

			IOptionsServer optsServer = ServerFactory.getOptionsServer(p4d.getRSHURL(), null);
			assertNotNull(optsServer);
			optsServer.connect();
			optsServer.disconnect();
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
