/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.ProtectionEntry;
import com.perforce.p4java.option.server.GetProtectionEntriesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for Perforce protections functionality. Requires super user login.
 */
@Jobs({ "job044327" })
@TestId("Dev112_ProtectionsTest")
public class ProtectionsTest extends P4JavaRshTestCase {

	IOptionsServer server = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ProtectionsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			// Requires super user
			server = getSuperConnection(p4d.getRSHURL());
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
	 * Test create Perforce protections - get the full list of protection
	 * entries; add a new protection entry; feed the new list back to the
	 * server; get the full list again and the new list should contain the new
	 * protection entry.
	 */
	@Test
	public void testCreateProtections() {
		List<IProtectionEntry> originalEntryList = null;

		try {
			originalEntryList = server
					.getProtectionEntries(null,
							new GetProtectionEntriesOptions().setAllUsers(true));
			assertNotNull(originalEntryList);

			List<IProtectionEntry> newEntryList = new ArrayList<IProtectionEntry>();
			newEntryList.addAll(originalEntryList);

			// Append new test protection entries to the end of the list
			int order = originalEntryList.size();

			IProtectionEntry protectionEntry1 = new ProtectionEntry(order,
					"read", false, "*", "p4jtestdummy", "//depot/foo/...",
					false);
			newEntryList.add(protectionEntry1);

			// Append new test protection entries to the end of the list
			IProtectionEntry protectionEntry2 = new ProtectionEntry(order + 1,
					"write", true, "*", "p4jtestdummygroup",
					"//depot/foo2/...", true);
			newEntryList.add(protectionEntry2);

			String message = server.createProtectionEntries(newEntryList);
			assertNotNull(message);
			assertNotNull("Protections saved.");

			List<IProtectionEntry> updatedEntryList = server
					.getProtectionEntries(null,
							new GetProtectionEntriesOptions().setAllUsers(true));
			assertNotNull(updatedEntryList);

			// Should have at least 2 entries
			assertTrue(updatedEntryList.size() >= 2);

			// Check the last 2 entries matching the entries added
			IProtectionEntry lastProtectionEntry1 = updatedEntryList
					.get(updatedEntryList.size() - 2);
			assertNotNull(lastProtectionEntry1);
			assertEquals(lastProtectionEntry1.toString(),
					protectionEntry1.toString());

			IProtectionEntry lastProtectionEntry2 = updatedEntryList
					.get(updatedEntryList.size() - 1);
			assertNotNull(lastProtectionEntry2);
			assertEquals(lastProtectionEntry2.toString(),
					protectionEntry2.toString());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				if (originalEntryList != null && !originalEntryList.isEmpty()) {
					// Change the protections table back to it's original entries
					server.updateProtectionEntries(originalEntryList);
				}
			} catch (P4JavaException e) {
				// Can't do much here...
			}
		}
	}

	/**
	 * Test update Perforce protections - currently update protections is doing
	 * the same things same as create protections.
	 */
	@Test
	public void testUpdateProtections() {
		List<IProtectionEntry> originalEntryList = null;

		try {
			originalEntryList = server.getProtectionEntries(null,
					new GetProtectionEntriesOptions().setAllUsers(true));
			assertNotNull(originalEntryList);

			// Append a new test protection entry to the end of the list
			IProtectionEntry protectionEntry = new ProtectionEntry(
					originalEntryList.size(), "write", false, "*",
					"p4jtestdummy", "//depot/112Dev/...", false);
			List<IProtectionEntry> newEntryList = new ArrayList<IProtectionEntry>();
			newEntryList.addAll(originalEntryList);
			newEntryList.add(protectionEntry);

			String message = server.updateProtectionEntries(newEntryList);
			assertNotNull(message);
			assertNotNull("Protections saved.");

			List<IProtectionEntry> updatedEntryList = server
					.getProtectionEntries(null,
							new GetProtectionEntriesOptions().setAllUsers(true));
			assertNotNull(updatedEntryList);

			// Should have at least one entry
			assertTrue(updatedEntryList.size() >= 1);

			// Check the last entry matching the entry added
			IProtectionEntry lastProtectionEntry = updatedEntryList
					.get(updatedEntryList.size() - 1);
			assertNotNull(lastProtectionEntry);
			assertEquals(lastProtectionEntry.toString(),
					protectionEntry.toString());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				if (originalEntryList != null && !originalEntryList.isEmpty()) {
					// Change the protections table back to it's original entries
					server.updateProtectionEntries(originalEntryList);
				}
			} catch (P4JavaException e) {
				// Can't do much here...
			}
		}
	}
}
