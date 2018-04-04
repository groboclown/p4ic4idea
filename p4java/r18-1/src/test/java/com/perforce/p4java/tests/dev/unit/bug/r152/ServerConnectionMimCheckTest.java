/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test connection to Perforce server with net.mimcheck=5 (or >= 4)
 */
@Jobs({ "job081080" })
@TestId("Dev152_ServerConnectionMimCheckTest")
public class ServerConnectionMimCheckTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ServerConnectionMimCheckTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), P4JTEST_SUPERUSERNAME_DEFAULT, P4JTEST_SUPERPASSWORD_DEFAULT, false, null);
    }

	/**
	 * Test connection to Perforce server with net.mimcheck=5 (or >= 4)
	 */
	@Test
	public void testServerConnectionMimCheck() {

		try {
			String retVal = server.setOrUnsetServerConfigurationValue("net.mimcheck", "5");
			assertNotNull(retVal);
			assertEquals("For server '" + "any" + "', configuration variable '" + "net.mimcheck" + "' set to '" + "5" + "'\n", retVal);

			List<IChangelistSummary> changelistSummaries = server
					.getChangelists(null, new GetChangelistsOptions().setMaxMostRecent(10));
			assertEquals(10, changelistSummaries.size());

			// Set configurable back to net.mimcheck=0
			retVal = server.setOrUnsetServerConfigurationValue("net.mimcheck", "0");
			assertNotNull(retVal);
			assertEquals("For server '" + "any" + "', configuration variable '" + "net.mimcheck" + "' set to '" + "0" + "'\n", retVal);
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
