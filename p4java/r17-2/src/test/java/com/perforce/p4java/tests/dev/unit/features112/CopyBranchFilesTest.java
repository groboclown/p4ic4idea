/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "p4 copy -b branch -s fromFile toFiles".
 */
@Jobs({ "job046694" })
@TestId("Dev112_CopyBranchFilesTest")
public class CopyBranchFilesTest extends P4JavaTestCase {

	IOptionsServer server = null;

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
			server = getServer();
			assertNotNull(server);
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
	 * Test 'p4 copy -b branch -s fromFile toFiles' using the unified copyFiles
	 * method.
	 */
	@Test
	public void testCopyFilesWithBranchView() {
		IClient client = null;
		IChangelist changelist = null;
		List<IFileSpec> files = null;

		try {
			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);

			changelist = getNewChangelist(server, client,
					"Dev112_CopyBranchFilesTest copy files with branch view");
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			files = client
					.copyFiles(
							null,
							FileSpecBuilder
									.makeFileSpecList(new String[] { "//depot/SandboxTest/Attributes/test01.txt#1" }),
							new CopyFilesOptions().setChangelistId(
									changelist.getId()).setBranch(
									"test_sadfdsfasd").setBidirectional(true));
			assertNotNull(files);

			changelist.refresh();
			files = changelist.getFiles(true);
			assertNotNull(files);
			assertEquals(1, files.size());
			assertNotNull(files.get(0));
			assertNotNull(files.get(0).getAnnotatedPreferredPathString());
			assertEquals("//depot/SandboxTest/Attributes/test01.txt#2", files.get(0).getAnnotatedPreferredPathString());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if (changelist != null) {
						if (changelist.getStatus() == ChangelistStatus.PENDING) {
							try {
								// Revert files in pending changelist
								client.revertFiles(changelist.getFiles(true),
										new RevertFilesOptions()
												.setChangelistId(changelist
														.getId()));
							} catch (P4JavaException e) {
								// Can't do much here...
							}
						}
					}
				}
			}
		}

	}
}
