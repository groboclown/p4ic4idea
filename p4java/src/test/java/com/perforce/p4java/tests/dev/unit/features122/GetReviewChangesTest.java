/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features122;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IReviewChangelist;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.GetReviewChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 review'.
 */
@Jobs({ "job043626" })
@TestId("Dev122_GetReviewChangesTest")
public class GetReviewChangesTest extends P4JavaRshTestCase {

	IOptionsServer superServer = null;
	IClient superClient = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetReviewChangesTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() throws Exception {
		// initialization code (before each test).
		try {
		    superServer = getSuperConnection(p4d.getRSHURL());
			superClient = superServer.getClient("p4TestSuperWS20112");
			assertNotNull(superClient);
			superServer.setCurrentClient(superClient);
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
		if (superServer != null) {
			this.endServerSession(superServer);
		}
	}

	/**
	 * Test 'p4 review'.
	 */
	@Test
	public void testGetReviewChanges() {

		int randNum = getRandomInt();
		String testReviewCounter = "testReviewCounter" + randNum;
		
		try {
			// Get max 200 changelists
			List<IChangelistSummary> changelists = superServer.getChangelists(null, new GetChangelistsOptions().setMaxMostRecent(10));
			assertNotNull(changelists);
			assertTrue(changelists.size() > 0);
			
			// Get review changes starting from a changelist id onward
			int changelistId = changelists.get(changelists.size()/2).getId();
			List<IReviewChangelist> reviewChanges2 = superServer.getReviewChangelists(new GetReviewChangelistsOptions().setChangelistId(changelistId));
			assertNotNull(reviewChanges2);
			assertTrue(reviewChanges2.size() > 0);

			// Set a counter with a changelist id
			// The user have the permission to set counters
			List<IReviewChangelist> reviewChanges3 = superServer.getReviewChangelists(new GetReviewChangelistsOptions().setChangelistId(changelistId).setCounter(testReviewCounter));
			assertNotNull(reviewChanges3);
			assertEquals(reviewChanges3.size(), 0);

			// Check if the counter is set
			String testReviewCounterValue = superServer.getCounter(testReviewCounter);
			assertNotNull(testReviewCounterValue);
			assertEquals(new Integer(testReviewCounterValue), new Integer(changelistId));
			
			// Get review changes starting from a changelist id on the testReviewCounter counter
			List<IReviewChangelist> reviewChanges4 = superServer.getReviewChangelists(new GetReviewChangelistsOptions().setCounter(testReviewCounter));
			assertNotNull(reviewChanges4);
			assertTrue(reviewChanges4.size() > 0);
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (superServer != null) {
				try {
					superServer.deleteCounter(testReviewCounter, false);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}
}
