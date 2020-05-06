/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.tests.SSLServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaLocalServerTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test sync -f against a SSL Perforce server.
 */
@Jobs({ "job051534" })
@TestId("Dev121_SSLForceSyncTest")
public class SSLServerForceSyncTest extends P4JavaLocalServerTestCase {

	@ClassRule
	public static SSLServerRule p4d = new SSLServerRule("r16.1", SSLServerForceSyncTest.class.getSimpleName(), "ssl:localhost:10669");

	String serverMessage = null;
	long completedTime = 0;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupSSLServer(p4d.getP4JavaUri(), userName, password, true, props);
			client = server.getCurrentClient();
	
			// Check server info
			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);
			assertTrue(serverInfo.isCaseSensitive());
			assertTrue(serverInfo.isServerEncrypted());
	
		} catch (Exception e) {
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
