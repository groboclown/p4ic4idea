/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r141;

import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test "p4 sync" deleted files.
 */
@Jobs({"job074183"})
@TestId("Dev141_SyncDeletedFilesTest")
public class SyncDeletedFilesTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SyncDeletedFilesTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {

		setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
		server.setCurrentClient(server.getClient("p4TestUserWS"));
	}

	/**
	 * Test "p4 sync" deleted files.
	 */
	@Test
	public void testSyncDeletedFiles() {

		List<IFileSpec> files = null;

		String depotPath = "//depot/112Dev/Attributes1358938299/test02.txt";

		try {
			client = server.getCurrentClient();
			files = client.sync(FileSpecBuilder.makeFileSpecList(depotPath), true, false, false, false);
			assertNotNull(files);
			assertTrue(files.size() > 0);
			assertEquals(1, files.size());
			assertNotNull(files.get(0));
			assertEquals(FileAction.DELETED, files.get(0).getAction());
			assertEquals(FileSpecOpStatus.VALID, files.get(0).getOpStatus());
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
