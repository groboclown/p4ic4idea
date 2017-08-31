/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features122;

import static org.junit.Assert.assertEquals;
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
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IReviewChangelist;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.GetReviewChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 review'.
 */
@Jobs({ "job043626" })
@TestId("Dev122_GetReviewChangesTest")
public class GetReviewChangesTest extends P4JavaTestCase {

	IOptionsServer superServer = null;
	IClient superClient = null;

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
			superServer = getServerAsSuper();
			superClient = superServer.getClient("p4TestSuperWS20112");
			assertNotNull(superClient);
			superServer.setCurrentClient(superClient);
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
		if (superServer != null) {
			this.endServerSession(superServer);
		}
	}

	/**
	 * Test 'p4 review'.
	 */
	@Test
	public void tesGetReviewChanges() {

		int randNum = getRandomInt();
		String testReviewCounter = "testReviewCounter" + randNum;
		
		try {
			// Get max 200 changelists
			List<IChangelistSummary> changelists = superServer.getChangelists(null, new GetChangelistsOptions().setMaxMostRecent(200));
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
