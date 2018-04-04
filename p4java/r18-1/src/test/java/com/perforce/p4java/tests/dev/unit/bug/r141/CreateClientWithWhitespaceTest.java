/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r141;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary.ClientLineEnd;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r152.ShelveChangelistClientTest;

/**
 * Test create client with whitespace in the name.
 */
@Jobs({ "job073878" })
@TestId("Dev141_CreateClientWithWhitespaceTest")
public class CreateClientWithWhitespaceTest extends P4JavaRshTestCase {

	
	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", CreateClientWithWhitespaceTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
    }
	
	
	/**
	 * Test create client with whitespace in the name
	 */
	@Test
	public void testCreateClientWithWhitespace() {

		IClient tempClient = null;
		
		try {
			// Create temp client
			String tempClientName = "testclient- white space -" + getRandomName(testId);
			tempClient = new Client(
					tempClientName,
                    null,	// accessed
                    null,	// updated
                    testId + " temporary test client",
                    null,
                    getUserName(),
                    getTempDirName() + "/" + testId,
                    ClientLineEnd.LOCAL,
                    null,	// client options
                    null,	// submit options
                    null,	// alt roots
                    server,
                    null
				);
			assertNotNull("Null client", tempClient);
			String resultStr = server.createClient(tempClient);
			assertNotNull(resultStr);
			//tempClient = server.getClient(tempClient.getName());
			//assertNotNull("couldn't retrieve new client", tempClient);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (tempClient != null) {
					try {
						String resultStr = server.deleteClient(tempClient.getName(), false);
						assertNotNull(resultStr);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
			}
		}
	}
}
