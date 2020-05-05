/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.ServerInfo;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test server info date as calendar
 */
@Jobs({ "job059516" })
@TestId("Dev141_ServerInfoDateAsCalendarTest")
public class ServerInfoDateAsCalendarTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1",ServerInfoDateAsCalendarTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			Properties properties = new Properties();
			properties.put("relaxCmdNameChecks", "true");
			setupServer(p4d.getRSHURL(), userName, password, true, properties);
			setupUtf8(server);
			client = getClient(server);
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
	 * Test server info date as calendar
	 */
	@Test
	public void testServerDateAsCalendar() {

		try {
			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);
			assertNotNull(serverInfo.getServerCalendar());
			assertNotNull(serverInfo.getServerCalendar().get(Calendar.ZONE_OFFSET));
			assertNotNull(serverInfo.getServerCalendar().get(Calendar.DST_OFFSET));
			
			// Print the date out from calendar
			DateFormat dateFormat = new SimpleDateFormat(ServerInfo.SERVER_INFO_DATE_PATTERN);
			Calendar cal = Calendar.getInstance();
			System.out.println(dateFormat.format(cal.getTime()));
		} catch (P4JavaException exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
