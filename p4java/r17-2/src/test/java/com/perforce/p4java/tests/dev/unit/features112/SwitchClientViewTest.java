/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertFalse;
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
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 client -s' command. Switch an existing client spec's view without
 * invoking the editor. With -t to switch to a view defined in another client
 * spec. The -f flag can be used with -s to force switching with opened files.
 */
@Jobs({ "job046682" })
@TestId("Dev112_SwitchClientViewTest")
public class SwitchClientViewTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient p4jTestClient = null;
	IClient testClient1 = null;
	IClient testClient2 = null;
	String message = null;
	IChangelist changelist = null;

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
	 * Switch an existing client spec's view.
	 */
	@Test
	public void testSwitchClientView() {
		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ "MessagesBundle_es.properties";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/MessagesBundle_es.properties";

		List<IFileSpec> files = null;

		try {
			p4jTestClient = server.getClient("p4TestUserWS");

			testClient1 = new Client();
			testClient1.setName("testClient1" + randNum);
			testClient1.setOwnerName(p4jTestClient.getOwnerName());
			testClient1.setDescription(testClient1.getName() + " description.");
			testClient1.setRoot(p4jTestClient.getRoot());
			ClientView clientView1 = new ClientView();
			String mapping1 = "//depot/101Bugs/... //" + testClient1.getName()
					+ "/101Bugs/...";
			ClientViewMapping clientViewMapping1 = new ClientViewMapping(0,
					mapping1);
			clientView1.addEntry(clientViewMapping1);
			testClient1.setClientView(clientView1);
			testClient1.setServer(server);
			server.setCurrentClient(testClient1);
			message = server.createClient(testClient1);
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + testClient1.getName() + " saved."));

			testClient1 = server.getClient(testClient1.getName());

			testClient2 = new Client();
			testClient2.setName("testClient2" + randNum);
			testClient2.setOwnerName(p4jTestClient.getOwnerName());
			testClient2.setDescription(testClient2.getName() + " description.");
			testClient2.setRoot(p4jTestClient.getRoot());
			ClientView clientView2 = new ClientView();
			String mapping2 = "//depot/112Dev/... //" + testClient2.getName()
					+ "/112Dev/...";
			ClientViewMapping clientViewMapping2 = new ClientViewMapping(0,
					mapping2);
			clientView2.addEntry(clientViewMapping2);
			testClient2.setClientView(clientView2);
			testClient2.setServer(server);
			server.setCurrentClient(testClient2);
			message = server.createClient(testClient2);
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + testClient2.getName() + " saved."));

			testClient2 = server.getClient(testClient2.getName());

			// The view mapping of test client 1 and test client 2 should be
			// different
			assertFalse(testClient1
					.getClientView()
					.getEntry(0)
					.getLeft()
					.contentEquals(
							testClient2.getClientView().getEntry(0).getLeft()));
			assertFalse(testClient1
					.getClientView()
					.getEntry(0)
					.getRight()
					.contentEquals(
							testClient2
									.getClientView()
									.getEntry(0)
									.getRight()
									.replace(testClient2.getName(),
											testClient1.getName())));

			// Copy a file to be used for testing
			changelist = getNewChangelist(server, testClient2,
					"Dev112_UnshelveTest copy files");
			assertNotNull(changelist);
			changelist = testClient2.createChangelist(changelist);
			files = testClient2.copyFiles(new FileSpec(sourceFile),
					new FileSpec(targetFile), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Open file for edit
			changelist = getNewChangelist(server, testClient2,
					"Dev112_SwitchClientViewTest edit files");
			assertNotNull(changelist);
			changelist = testClient2.createChangelist(changelist);
			files = testClient2.editFiles(
					FileSpecBuilder.makeFileSpecList(targetFile),
					new EditFilesOptions().setChangelistId(changelist.getId()));

			// Since there is an open file, the switch would fail.
			// Note: the current client is set to testClient2
			try {
				message = server.switchClientView(testClient1.getName(), null,
						new SwitchClientViewOptions());
			} catch (P4JavaException e) {
				assertTrue(e
						.getLocalizedMessage()
						.contains(
								"Client '"
										+ testClient2.getName()
										+ "' has files opened; use -f to force switch."));
			}

			// Use force (-f) to switch view, this will succeed.
			// Note: the current client is set to testClient2
			message = server.switchClientView(testClient1.getName(), null,
					new SwitchClientViewOptions().setForce(true));

			assertTrue(message.contains("Client " + testClient2.getName()
					+ " switched."));

			// Get the test client 2 after switching the view to match client 1
			testClient2 = server.getClient(testClient2.getName());

			// Now, the view mapping of test client 1 and test client 2 should
			// be the same
			assertTrue(testClient1
					.getClientView()
					.getEntry(0)
					.getLeft()
					.contentEquals(
							testClient2.getClientView().getEntry(0).getLeft()));

			// The right side should be the same except the client name, so we
			// must replace the client name and do the match.
			assertTrue(testClient1
					.getClientView()
					.getEntry(0)
					.getRight()
					.contentEquals(
							testClient2
									.getClientView()
									.getEntry(0)
									.getRight()
									.replace(testClient2.getName(),
											testClient1.getName())));

			// Let's switch the view of target testClient1 to the view of template
			// p4jTestClient ("p4TestUserWS")
			// Note: the current client is set to testClient2
			message = server.switchClientView(p4jTestClient.getName(), testClient1.getName(),
					new SwitchClientViewOptions().setForce(true));

			assertTrue(message.contains("Client " + testClient1.getName()
					+ " switched."));

			// Get the testClient1 after switching the view to match p4jTestClient
			testClient1 = server.getClient(testClient1.getName());

			// Now, the view mapping of testClient1 and p4jTestClient should
			// be the same
			assertTrue(testClient1
					.getClientView()
					.getEntry(0)
					.getLeft()
					.contentEquals(
							p4jTestClient.getClientView().getEntry(0).getLeft()));

			// The right side should be the same except the client name, so we
			// must replace the client name and do the match.
			assertTrue(testClient1
					.getClientView()
					.getEntry(0)
					.getRight()
					.contentEquals(
							p4jTestClient
									.getClientView()
									.getEntry(0)
									.getRight()
									.replace(p4jTestClient.getName(),
											testClient1.getName())));
		
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (testClient2 != null) {
				if (changelist != null) {
					if (changelist.getStatus() == ChangelistStatus.PENDING) {
						try {
							// Revert files in pending changelist
							testClient2
									.revertFiles(changelist.getFiles(true),
											new RevertFilesOptions()
													.setChangelistId(changelist
															.getId()));
						} catch (P4JavaException e) {
							// Can't do much here...
						}
					}
				}
			}
			if (testClient2 != null && server != null) {
				try {
					// Delete submitted test files
					IChangelist deleteChangelist = getNewChangelist(server,
							testClient2,
							"Dev112_SwitchClientViewTest delete submitted files");
					deleteChangelist = testClient2
							.createChangelist(deleteChangelist);
					testClient2.deleteFiles(FileSpecBuilder
							.makeFileSpecList(new String[] { targetFile }),
							new DeleteFilesOptions()
									.setChangelistId(deleteChangelist.getId()));
					deleteChangelist.refresh();
					deleteChangelist.submit(null);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
			try {
				// Delete the test clients
				server = getServerAsSuper();
				if (server != null) {
					if (testClient1 != null) {
						server.deleteClient(testClient1.getName(), true);
					}
					if (testClient2 != null) {
						server.deleteClient(testClient2.getName(), true);
					}
				}
			} catch (P4JavaException e) {
				// Can't do much here...
			} catch (URISyntaxException e) {
				// Can't do much here...
			}
		}
	}
}
