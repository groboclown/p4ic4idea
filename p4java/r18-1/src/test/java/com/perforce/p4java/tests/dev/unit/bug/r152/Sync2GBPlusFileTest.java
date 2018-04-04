/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test sync large size file > 2GB
 */
@Jobs({ "job074785" })
@TestId("Dev152_Sync2GBPlusFileTest")
public class Sync2GBPlusFileTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Sync2GBPlusFileTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), P4JTEST_SUPERUSERNAME_DEFAULT, P4JTEST_SUPERPASSWORD_DEFAULT, false, null);
    }

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			assertNotNull(server);
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}
			server.setUserName("mruan");
			server.setCurrentClient(server.getClient("mruan-mac"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}
	
	/**
	 * Test sync large size file > 2GB
	 */
	@Test
	@Ignore("Points to a client workspace that no longer exists")
	public void testSync2GBPlusFile() {
		String depotFile = null;

		try {
			depotFile = "//depot/large-file/test-2gb-plus.tar";

			List<IFileSpec> files = server.getCurrentClient().sync(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
