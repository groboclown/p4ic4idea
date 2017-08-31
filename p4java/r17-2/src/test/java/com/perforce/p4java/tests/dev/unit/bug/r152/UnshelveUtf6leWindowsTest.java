/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.UnshelveFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test unshelve utf16-le encoded files in the Windows environment.
 */
@Jobs({ "job080389" })
@TestId("Dev152_UnshelveUtf6leWindowsTest")
public class UnshelveUtf6leWindowsTest extends P4JavaRshTestCase {

	
	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", UnshelveUtf6leWindowsTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
    }

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}
	/**
	 * Test unshelve utf16-le encoded files in the Windows environment
	 */
	@Test
	public void testUnshelveUtf6leWindows() {
		int changelistNumber = 24424;
		String depotFile = "//depot/cases/180445/utf16leExample.fm";
		IChangelist changelist = null;
		IChangelist targetChangelist = null;

		try {
			IClient client = server.getClient("p4TestUserWS20112Windows");
			assertNotNull(client);
			server.setCurrentClient(client);
			
			changelist = server.getChangelist(changelistNumber);
			assertNotNull(changelist);
			targetChangelist = client.createChangelist(this.createChangelist(client));
			assertNotNull(targetChangelist);

			List<IFileSpec> unshelveList = client.unshelveFiles(
					FileSpecBuilder.makeFileSpecList(depotFile),
					changelist.getId(), targetChangelist.getId(),
					new UnshelveFilesOptions().setForceUnshelve(true));
			assertNotNull(unshelveList);
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(unshelveList).size());			

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (client != null) {
				try {
					@SuppressWarnings("unused")
					List<IFileSpec> fileList = client.revertFiles(
							FileSpecBuilder.makeFileSpecList(depotFile),
							new RevertFilesOptions().setChangelistId(targetChangelist.getId()));
					try {
						String deleteResult = server.deletePendingChangelist(targetChangelist.getId());
						assertNotNull(deleteResult);
					} catch (Exception exc) {
						System.out.println(exc.getLocalizedMessage());
					}
				} catch (P4JavaException exc) {
					// Can't do much here...
				}
			}
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
