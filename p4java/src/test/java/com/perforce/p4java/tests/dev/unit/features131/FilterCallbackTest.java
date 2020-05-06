/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test describe changelist with many files using IStreamingCallback
 */
@Jobs({ "job065303" })
@TestId("Dev131_FilterCallbackTest")
public class FilterCallbackTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", FilterCallbackTest.class.getSimpleName());

	String serverMessage = null;
	long completedTime = 0;

	public static class SimpleCallbackHandler implements IFilterCallback {

		private final int limit;

		private int count = 0;
		private Map<String, String> doNotSkipKeysMap;

		public SimpleCallbackHandler(int limit) {
			this.limit = limit;
			this.doNotSkipKeysMap = new HashMap<>();
			this.doNotSkipKeysMap.put("func", null);
			doNotSkipKeysMap = new HashMap<>();
			doNotSkipKeysMap.put("unicode", "");
			doNotSkipKeysMap.put("serveAddress", "");
			doNotSkipKeysMap.put("token", "");
			doNotSkipKeysMap.put("confirm", "");
			doNotSkipKeysMap.put("himark", "");
			doNotSkipKeysMap.put("fseq", "");
		}

		@Override
		public void reset() {}

		@Override
		public boolean skip(String key, Object value, final AtomicBoolean skipSubsequent) throws P4JavaException {
			if (this.count++ >= this.limit) {
				skipSubsequent.set(true);
			}
			return false;
		}

		public Map<String, String> getDoNotSkipKeysMap() throws P4JavaException {
			
			return this.doNotSkipKeysMap;
		}

	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			Properties props = new Properties();
			props.put("enableProgress", "true");
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			String[] s = new String[20];
			for (int i = 0; i < 20; i++) {
				s[i] = "test" + i;
			}
			client = getClient(server);
			createTextFilesOnServer(client, s, "multiple files changelist");
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
	 * Test describe changelist with many files using IStreamingCallback
	 */
	@Test
	public void testDescribeChangelistWithManyFiles() {
		int limit = 1;
		
		try {
			SimpleCallbackHandler handler = new SimpleCallbackHandler(limit);

			List<Map<String, Object>> resultsList = server.execInputStringMapCmdList(CmdSpec.DESCRIBE.toString(), new String[] { "-s", "30125" }, null, handler);

			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);
			assertNotNull(resultsList.get(0));
			for (Map<String, Object> resultmap : resultsList) {
                assertTrue(resultmap.size() == limit);
            }
		} catch (P4JavaException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
