/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test force update of the "Date" field for submitted changelists.
 */
@Jobs({ "job045050" })
@TestId("Dev112_UpdateChangelistDateTest")
public class UpdateChangelistDateTest extends P4JavaRshTestCase {
	private static IClient client = null;

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", UpdateChangelistDateTest.class.getSimpleName());

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		afterEach(server);
	}

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() throws Exception{
		// initialization code (before each test).
	        setupServer(p4d.getRSHURL(), superUserName, superUserPassword, false, props);
			client = server.getClient("p4TestSuperWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);
	}

	/**
	 * Test force update of the "Date" field for submitted changelists.
	 */
	@Test
	public void testUpdateChangelistDate() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ "p4cmd/P4CmdDispatcher.java";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ dir + "/P4CmdDispatcher.java";

		try {
			// Copy the source file to target
			changelist = getNewChangelist(server, client,
					"Dev112_UpdateChangelistDateTest copy files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			files = client.copyFiles(new FileSpec(sourceFile), new FileSpec(
					targetFile), null, new CopyFilesOptions()
					.setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Update the submitted changelist date with an older day
			changelist.refresh();
			Calendar cal = Calendar.getInstance();
			cal.setTime(changelist.getDate());
			cal.add(Calendar.DATE, -30);

			changelist.setDate(cal.getTime());
			changelist.update(true);
			changelist.refresh();

			// The changelist date should be updated
			assertEquals(cal.getTime().getTime(), changelist.getDate()
					.getTime());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (client != null && server != null) {
				try {
					// Delete submitted test files
					IChangelist deleteChangelist = getNewChangelist(server,
							client,
							"Dev112_UpdateChangelistDateTest delete submitted files");
					deleteChangelist = client
							.createChangelist(deleteChangelist);
					client.deleteFiles(FileSpecBuilder
							.makeFileSpecList(new String[] { targetFile }),
							new DeleteFilesOptions()
									.setChangelistId(deleteChangelist.getId()));
					deleteChangelist.refresh();
					deleteChangelist.submit(null);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}

	/**
	 * Change the date of an existing changelist
	 */
	@Test
	public void testUpdateExistingChangelistDate() {
		try {
			Calendar cal = new GregorianCalendar(2010,1,1);
			IChangelist change = server.getChangelist(1);

			// update the date
			change.setDate(cal.getTime());
			change.update(true);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
