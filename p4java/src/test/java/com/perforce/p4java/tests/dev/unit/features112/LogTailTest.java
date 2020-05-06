/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.admin.ILogTail;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.LogTail;
import com.perforce.p4java.option.server.LogTailOptions;
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
 * Test the 'p4 logtail' command.
 */
@Jobs({ "job043229" })
@TestId("Dev112_LogTailTest")
public class LogTailTest extends P4JavaRshTestCase {

	IClient client = null;
	
	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", LogTailTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = getSuperConnection(p4d.getRSHURL());
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
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
	 * Test LogTail constructor.
	 */
	@Test
	public void testLogTailConstructor() {

		List<String> logData = new ArrayList<String>();
		logData.add("test data");

		// Test LogTail constructor with correct parameters
		try {
			ILogTail logTail = new LogTail("testlogfile",0,logData);
			assertNotNull(logTail);
			assertEquals(logTail.getLogFilePath(), "testlogfile");
			assertEquals(logTail.getOffset(), 0);
			assertEquals(logTail.getData(), "test data");
		} catch (Throwable e) {
		}
		
		// Test LogTail constructor with null "logFilePath"
		try {
			new LogTail(null,0,logData);
		} catch (Throwable e) {
			assertNotNull(e);
			assertEquals("logFilePath shouldn't null or empty", e.getLocalizedMessage());
		}
		
		// Test LogTail constructor with negative "offset"
		try {
			new LogTail("testlogfile",-1,logData);
		} catch (Throwable e) {
			assertNotNull(e);
			assertEquals("offset should be greater than or equal to 0", e.getLocalizedMessage());
		}
		
		// Test LogTail constructor with null "data"
		try {
			new LogTail("testlogfile",0,null);
		} catch (Throwable e) {
			assertNotNull(e);
			assertEquals("No data passed to the LogTail constructor.", e.getMessage());
		}

	}
	
	/**
	 * Logtail outputs the last block(s) of the errorLog and the offset required
	 * to get the next block when it becomes available.
	 */
	@Test
	public void testLogTail() {

		try {
			ILogTail logTail = server.getLogTail(new LogTailOptions());
			assertNotNull(logTail);

			// Verify the logTail fields
			assertNotNull(logTail.getLogFilePath());
			assertTrue(logTail.getOffset() > 0);
			assertNotNull(logTail.getData());

			logTail = server.getLogTail(new LogTailOptions().setBlockSize(2*8192).setMaxBlocks(10).setStartingOffset(logTail.getOffset()));
			assertNotNull(logTail);

			// Verify the logTail fields
			assertNotNull(logTail.getLogFilePath());
			assertTrue(logTail.getOffset() > 0);
			assertNotNull(logTail.getData());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test logtail with small block size.
	 */
	@Test
	public void testLogTailSmallBocks() {

		try {
			ILogTail logTail = server.getLogTail(new LogTailOptions().setBlockSize(10).setMaxBlocks(100).setStartingOffset(0));
			assertNotNull(logTail);

			// Verify the logTail fields
			assertNotNull(logTail.getLogFilePath());
			assertTrue(logTail.getOffset() > 0);
			assertNotNull(logTail.getData());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
