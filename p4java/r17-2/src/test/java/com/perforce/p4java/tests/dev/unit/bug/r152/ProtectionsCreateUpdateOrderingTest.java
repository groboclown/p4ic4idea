/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.ProtectionEntry;
import com.perforce.p4java.option.server.GetProtectionEntriesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for ordering of creating and updating Perforce protections.
 * Requires super user login.
 */
@Jobs({ "job044327" })
@TestId("Dev112_ProtectionsTest")
public class ProtectionsCreateUpdateOrderingTest extends P4JavaRshTestCase {

	private static IClient client = null;
	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ProtectionsCreateUpdateOrderingTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), P4JTEST_SUPERUSERNAME_DEFAULT, P4JTEST_SUPERPASSWORD_DEFAULT, false, null);
    }

	
	/**
	 * Test ordering of create/update Perforce protections.
	 * Get the full list of protection entries.
	 * Add a new protection entry.
	 * Feed the new list back to the server.
	 * Get the full list again.
	 * The new list should contain the new/updated protection entries in expected order.
	 */
	@Test
	public void testCreateUpdatedProtectionsOrdering() {
		List<IProtectionEntry> originalEntryList = null;

		try {
			// Get the original list of protections
			originalEntryList = server.getProtectionEntries(null, new GetProtectionEntriesOptions().setAllUsers(true));
			assertNotNull(originalEntryList);

			List<IProtectionEntry> newEntryList = new ArrayList<IProtectionEntry>();
			newEntryList.addAll(originalEntryList);

			// Append new test protection entries to the end of the list
			int order = originalEntryList.size();
			IProtectionEntry protectionEntry1 = new ProtectionEntry(
					order++, "read", false, "*", "p4jtestdummy", "//depot/foo1/...", false);
			newEntryList.add(protectionEntry1);
			IProtectionEntry protectionEntry2 = new ProtectionEntry(
					order++, "write", true, "*", "p4jtestdummygroup", "//depot/foo2/...", true);
			newEntryList.add(protectionEntry2);
			IProtectionEntry protectionEntry3 = new ProtectionEntry(
					order++, "read", true, "*", "p4jtestdummygroup", "//depot/foo3/...", true);
			newEntryList.add(protectionEntry3);

			// Create the protections (along with the original protections)
			String message = server.createProtectionEntries(newEntryList);
			assertNotNull(message);
			assertNotNull("Protections saved.");

			// Get the protections
			List<IProtectionEntry> updatedEntryList = server.getProtectionEntries(null, new GetProtectionEntriesOptions().setAllUsers(true));
			assertNotNull(updatedEntryList);

			// Should have at least 3 entries
			int updatedSize = updatedEntryList.size();
			assertTrue(updatedSize >= 3);
			
			// Check the last 3 entries matching the new entries added
			IProtectionEntry lastProtectionEntry1 = updatedEntryList.get(updatedSize - 3);
			assertNotNull(lastProtectionEntry1);
			assertEquals(lastProtectionEntry1.toString(), protectionEntry1.toString());
			IProtectionEntry lastProtectionEntry2 = updatedEntryList.get(updatedSize - 2);
			assertNotNull(lastProtectionEntry2);
			assertEquals(lastProtectionEntry2.toString(), protectionEntry2.toString());
			IProtectionEntry lastProtectionEntry3 = updatedEntryList.get(updatedSize - 1);
			assertNotNull(lastProtectionEntry3);
			assertEquals(lastProtectionEntry3.toString(), protectionEntry3.toString());

			// Remove second to the last entry 
			updatedEntryList.remove(updatedSize - 2);
			assertEquals(updatedEntryList.size() + 1, updatedSize);

			// Update the protections
			message = server.updateProtectionEntries(updatedEntryList);
			assertNotNull(message);
			assertNotNull("Protections saved.");

			List<IProtectionEntry> newUpdatedEntryList = server.getProtectionEntries(null, new GetProtectionEntriesOptions().setAllUsers(true));
			assertNotNull(newUpdatedEntryList);

			// Should be updated
			assertEquals(newUpdatedEntryList.size(), updatedEntryList.size());

			// The second to last entry should now be the protectionEntry1
			IProtectionEntry lastUpdatedProtectionEntry2 = updatedEntryList.get(newUpdatedEntryList.size() - 2);
			assertNotNull(lastUpdatedProtectionEntry2);
			assertEquals(lastUpdatedProtectionEntry2.toString(), protectionEntry1.toString());
			
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
