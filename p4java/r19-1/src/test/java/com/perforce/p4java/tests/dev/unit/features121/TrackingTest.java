/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.features123.DiffFilesTest;

/**
 * Test tracking ("-Ztrack")
 */
@Jobs({ "job041786" })
@TestId("Dev121_TrackingTest")
public class TrackingTest extends P4JavaRshTestCase {

	IClient client = null;
	String serverMessage = null;
	long completedTime = 0;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", TrackingTest.class.getSimpleName());

    /**
     * @BeforeClass annotation to a method to be run before all the tests in a
     *              class.
     */
    @Before
    public void before() throws Exception {
		Properties props = new Properties();
		props.put("enableTracking", "true");
        setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, props);
		client = getClient(server);
    }
	

	/**
	 * Test tracking ("-Ztrack")
	 */
	@Test
	public void testTracking() {

		try {
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.PRINT.toString(), new String[] {"//depot/basic/readonly/grep/P4CmdGetter.java"}, null);
			assertNotNull(result);

			// Verifying the result
			Map<String, Object> lastResult = result[result.length - 1];
			assertNotNull(lastResult);
			
			assertNotNull(lastResult.get("data"));
			assertTrue(((String)lastResult.get("data")).contains("db.user"));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
