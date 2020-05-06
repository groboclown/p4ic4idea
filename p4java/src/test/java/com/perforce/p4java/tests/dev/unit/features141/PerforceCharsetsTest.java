/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test Perforce charsets cp1253, cp737, and iso8859-7.
 */
@Jobs({ "job071638" })
@TestId("Dev141_PerforceCharsetsTestTest")
public class PerforceCharsetsTest extends P4JavaRshTestCase {

	IOptionsServer superserver = null;
	IClient superclient = null;

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", PerforceCharsetsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, null);
			client = getClient(server);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
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
			assertTrue(server.isConnected());
			assertTrue(server.supportsUnicode());
			// Set the Perforce charsets
			server.setCharsetName("cp1253");
			server.setCharsetName("cp737");
			server.setCharsetName("iso8859-7");
			server.setCharsetName("cp1250");
			server.setCharsetName("cp852");
			server.setCharsetName("iso8859-2");
		} catch (P4JavaException e) {
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

	@Test
	public void testUnsetCharsets()
			throws ConnectionException, ConfigException, NoSuchObjectException, ResourceException, URISyntaxException,
			RequestException, AccessException, IOException {
		IServer server = ServerFactory.getServer(p4d.getRSHURL(), null);
		Assert.assertNotNull(server);
		server.setUserName("bruno");
		server.setCharsetName(null);
		server.connect();
		assertTrue(server.isConnected());
	}

}
