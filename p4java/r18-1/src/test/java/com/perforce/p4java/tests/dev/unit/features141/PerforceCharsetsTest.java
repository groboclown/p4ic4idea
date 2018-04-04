/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test Perforce charsets cp1253, cp737, and iso8859-7.
 */
@Jobs({ "job071638" })
@TestId("Dev141_PerforceCharsetsTestTest")
public class PerforceCharsetsTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	IOptionsServer superserver = null;
	IClient superclient = null;

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
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
	}

	/**
	 * Test Perforce charsets cp1253, cp737, and iso8859-7.
	 */
	@Test
	public void testPerforceCharsets() {

		// Printout the supported Perforce charsets
		String[] knownCharsets = PerforceCharsets.getKnownCharsets();
		StringBuffer badCharsets = new StringBuffer();
		
		for (int i=0; i<knownCharsets.length; i++ ) {
			String p4CharsetName = PerforceCharsets.getJavaCharsetName(knownCharsets[i]);
			System.out.println(knownCharsets[i] + "=" +	p4CharsetName);
			
			if (Charset.isSupported(p4CharsetName)) {
				Charset charset = Charset.forName(p4CharsetName);
				if (charset == null) {
					System.out.println("Cannot find charset: " + PerforceCharsets.getJavaCharsetName(knownCharsets[i]));
					badCharsets.append(p4CharsetName).append(",");
				} else {
					System.out.println("Java charset: " + charset.name());
				}
			} else {
				System.out.println("Charset not supported: " + p4CharsetName);
			}
		}
		if(badCharsets.length() > 0) {
		    fail("failed to load charsets: " + badCharsets.toString());
		}
		
		try {
			// Connect to a unicode enabled server
			server = getServer("p4java://eng-p4java-vm.perforce.com:30132", null);
			assertNotNull(server);

			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					// Set the Perforce charsets
					server.setCharsetName("cp1253");
					server.setCharsetName("cp737");
                    server.setCharsetName("iso8859-7");
                    server.setCharsetName("cp1250");
                    server.setCharsetName("cp852");
                    server.setCharsetName("iso8859-2");
					
				}
			}

			// Set the server user
			server.setUserName(this.userName);

			// Login using the normal method
			server.login(this.password, new LoginOptions());

			// Get the default client
			client = getDefaultClient(server);
			
			assertNotNull(client);
			server.setCurrentClient(client);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
			if (superserver != null) {
				this.endServerSession(superserver);
			}
		}
	}
}
