/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r141;

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
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.ProtectionEntry;
import com.perforce.p4java.option.server.GetProtectionEntriesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r152.ShelveChangelistClientTest;

/**
 * Test Perforce protection entries with quoted exclude paths.
 */
@Jobs({ "job071635" })
@TestId("Dev141_ProtectionsQuotedExcludePathTest")
public class ProtectionsQuotedExcludePathTest extends P4JavaRshTestCase {

	
	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ProtectionsQuotedExcludePathTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestsuper", "p4jtestsuper", false, null);
    
    }
	
	
	

	/**
	 * Test protection entries with quoted exclude paths.
	 */
	@Test
	public void testQuotedExcludePath() {
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
					"read", false, "*", "p4jtestdummy", "\"//depot/f o o/...\"",
					false);
			protectionEntry1.setPathExcluded(true);
			newEntryList.add(protectionEntry1);

			// Append new test protection entries to the end of the list
			IProtectionEntry protectionEntry2 = new ProtectionEntry(order + 1,
					"write", true, "*", "p4jtestdummygroup",
					"-//depot/f o o 2/...", false);
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
}
