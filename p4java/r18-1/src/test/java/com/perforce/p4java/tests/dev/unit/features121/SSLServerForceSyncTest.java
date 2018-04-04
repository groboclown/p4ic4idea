/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test sync -f against a SSL Perforce server.
 */
@Jobs({ "job051534" })
@TestId("Dev121_SSLForceSyncTest")
public class SSLServerForceSyncTest extends P4JavaTestCase {

	final static String sslServerURL = "p4javassl://eng-p4java-vm.perforce.com:30121";

	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;
	long completedTime = 0;

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
			server = ServerFactory.getOptionsServer(sslServerURL, null);
			assertNotNull(server);
	
			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					serverMessage = message;
				}
	
				public void receivedServerInfoLine(int key, String infoLine) {
					serverMessage = infoLine;
				}
	
				public void receivedServerErrorLine(int key, String errorLine) {
					serverMessage = errorLine;
				}
	
				public void issuingServerCommand(int key, String command) {
					serverMessage = command;
				}
	
				public void completedServerCommand(int key, long millisecsTaken) {
					completedTime = millisecsTaken;
				}
			});
	
			try {
				// Connect to the server.
				server.connect();
			} catch (P4JavaException e) {
				assertNotNull(e);
				assertTrue(e.getCause() instanceof TrustException);
				if (((TrustException) e.getCause()).getType() == TrustException.Type.NEW_CONNECTION
						|| ((TrustException) e.getCause()).getType() == TrustException.Type.NEW_KEY) {
					// Add trust WITH 'force' option
					try {
						String result = server.addTrust(new TrustOptions()
								.setForce(true).setAutoAccept(true));
						assertNotNull(result);
					} catch (P4JavaException e2) {
						assertNotNull(e2);
					}
				}
			}
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}
	
			// Set the server user
			server.setUserName("p4jtestuser");
	
			// Login using the normal method
			server.login("p4jtestuser", new LoginOptions());
	
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
	
			// Check server info
			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);
			assertTrue(serverInfo.isCaseSensitive());
			assertTrue(serverInfo.isServerEncrypted());
	
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
	 * Test sync -f against a SSL server
	 */
	@Test
	public void testForceSync() {

		String depotPath = "//depot/112Dev/Attributes/...";		
		List<IFileSpec> files = null;

		try {
			// sync deletedFile@30155
			// Get a revision of the file before it was deleted
			files = client.sync(
					FileSpecBuilder.makeFileSpecList(depotPath),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);
			assertTrue(files.size() > 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
