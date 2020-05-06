/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.*;

import com.perforce.p4java.admin.ITriggerEntry;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.TriggerEntry;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * Test for Perforce triggers functionality. Requires super user login.
 */
@Jobs({ "job072794", "job071702" })
@TestId("Dev112_TriggersTest")
public class TriggersTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", TriggersTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), superUserName, superUserPassword, true, null);
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
	 * Test create Perforce triggers - get the full list of trigger entries; add
	 * a new trigger entry; feed the new list back to the server; get the full
	 * list again and the new list should contain the new trigger entry.
	 */
	@Test
	public void testTriggers() {
		List<ITriggerEntry> originalEntryList = null;

		try {
			originalEntryList = server.getTriggerEntries();
			assertNotNull(originalEntryList);

			List<ITriggerEntry> newEntryList = new ArrayList<ITriggerEntry>();
			newEntryList.addAll(originalEntryList);

			// Append new test protection entries to the end of the list
			// example99 change-submit //depot/Dev112/... "echo %changelist%"
			int order = originalEntryList.size();

			ITriggerEntry triggerEntry1 = new TriggerEntry(order, "example88",
					ITriggerEntry.TriggerType.CHANGE_SUBMIT,
					"//depot/Dev112/...", "\"echo %changelist%\""
					);
			newEntryList.add(triggerEntry1);

			// Append new test protection entries to the end of the list
			ITriggerEntry triggerEntry2 = new TriggerEntry(order + 1, "example99",
					ITriggerEntry.TriggerType.CHANGE_SUBMIT,
					"//depot/112Bugs/...", "\"echo %changelist%\""
					);
			newEntryList.add(triggerEntry2);

			String message = server.createTriggerEntries(newEntryList);
			assertNotNull(message);
			assertNotNull("Triggers saved.");

			List<ITriggerEntry> updatedEntryList = server.getTriggerEntries();
			assertNotNull(updatedEntryList);

			// Should have at least 2 entries
			assertTrue(updatedEntryList.size() >= 2);

			// Check the last 2 entries matching the entries added
			ITriggerEntry lastTriggerEntry1 = updatedEntryList
					.get(updatedEntryList.size() - 2);
			assertNotNull(lastTriggerEntry1);
			assertEquals(lastTriggerEntry1.toString(),
					triggerEntry1.toString());

			ITriggerEntry lastTriggerEntry2 = updatedEntryList
					.get(updatedEntryList.size() - 1);
			assertNotNull(lastTriggerEntry2);
			assertEquals(lastTriggerEntry2.toString(),
					triggerEntry2.toString());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				if (originalEntryList != null && !originalEntryList.isEmpty()) {
					// Change the triggers table back to it's original entries
					server.updateTriggerEntries(originalEntryList);
				}
			} catch (P4JavaException e) {
				// Can't do much here...
			}
		}
	}
}
