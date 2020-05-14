/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test sync using IStreamingCallback.
 */
@Jobs({ "job059658" })
@TestId("Dev131_CancelCommandCallbackTest")
@Ignore("Can't run on windows because of filesystem character set")
public class CancelCommandCallbackTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", CancelCommandCallbackTest.class.getSimpleName());

	private static IClient client = null;

	public static class ListCallbackHandler implements IStreamingCallback {

		int expectedKey = 0;
		CancelCommandCallbackTest testCase = null;
		List<Map<String, Object>> resultsList = null;

		int limit = Integer.MAX_VALUE;

		int count = 0;

		public ListCallbackHandler(CancelCommandCallbackTest testCase,
				int key, List<Map<String, Object>> resultsList, int limit) {
			this.expectedKey = key;
			this.testCase = testCase;
			this.resultsList = resultsList;
			this.limit = limit;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {

			count++;

			if (count > limit) {
				fail("this callback method should not have been called after reaching the limit");
			}

			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			if (resultMap == null) {
				fail("null resultMap passed to handleResult callback");
			}
			this.resultsList.add(resultMap);

			if (count >= limit) {
				return false;
			}

			return true;
		}

		public List<Map<String, Object>> getResultsList() {
			return this.resultsList;
		}
	}

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// initialization code (before each test).
		try {
			Properties props = new Properties();
			props.put("enableProgress", "true");
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			assertNotNull(server);
			client = getClient(server);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}


	/**
	 * Test cancel sync many files using IStreamingCallback
	 */
	@Test
	public void testFileRename() {

		File sourceFile = null;
		File targetFile = null;
		String tmpDir = System.getProperty("java.io.tmpdir");
		String tmpFile = tmpDir + (tmpDir.endsWith("/") ? "" : "/")
				+ "target-job059658-" + getRandomInt() + ".tmp";

		try {
			sourceFile = File.createTempFile("source-job059658-", ".txt");
			targetFile = new File(tmpFile);
			
			boolean success = sourceFile.renameTo(targetFile);
			assertTrue(success);
			
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (sourceFile != null && sourceFile.exists()) {
				sourceFile.delete();
			}
			if (targetFile != null && targetFile.exists()) {
				targetFile.delete();
			}
		}

	}
	
	/**
	 * Test cancel sync many files using IStreamingCallback
	 */
	@Test
	public void testCancelSynManyFiles() {
		String depotFile = null;
		int cancelTrigger = 5;
		
		try {

			depotFile = "//depot/112Dev/GetOpenedFilesTest/bin/com/perforce/p4cmd/...";

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList, cancelTrigger);

			client.sync(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new SyncOptions().setForceUpdate(true)
									.setQuiet(true),
					handler,
					key);

			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			List<IFileSpec> fileList = new ArrayList<IFileSpec>();

			for (Map<String, Object> resultmap : resultsList) {
				if (resultmap != null) {
					for (Map.Entry<String, Object> entry : resultmap.entrySet()) {
					    String k = entry.getKey();
					    Object v = entry.getValue();
					    System.out.println(k + "=" + v);
					}

					fileList.add(ResultListBuilder.handleFileReturn(resultmap, server));
				}
			}
			
			assertNotNull(fileList);
			assertTrue(fileList.size() > 0);
			
			// Second sync using the same RPC connection (NTS)
			resultsList = new ArrayList<Map<String, Object>>();
			key = this.getRandomInt();
			handler = new ListCallbackHandler(this, key,
					resultsList, 1000);

			client.sync(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new SyncOptions().setForceUpdate(true)
									.setQuiet(true),
					handler,
					key);

			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			fileList = new ArrayList<IFileSpec>();

			for (Map<String, Object> resultmap : resultsList) {
				if (resultmap != null) {
					for (Map.Entry<String, Object> entry : resultmap.entrySet()) {
					    String k = entry.getKey();
					    Object v = entry.getValue();
					    System.out.println(k + "=" + v);
					}

					fileList.add(ResultListBuilder.handleFileReturn(resultmap, server));
				}
			}
			
			assertNotNull(fileList);
			assertTrue(fileList.size() > 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test cancel sync a large file using IStreamingCallback
	 */
	@Test
	public void testCancelSynLargeFile() {
		String depotFile = null;
		int cancelTrigger = 5;
		
		try {

			depotFile = "//depot/client/SimpleBigFileSyncTest/bigfile01.bin";

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList, cancelTrigger);

			client.sync(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new SyncOptions().setForceUpdate(true)
									.setQuiet(true),
					handler,
					key);

			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			List<IFileSpec> fileList = new ArrayList<IFileSpec>();

			for (Map<String, Object> resultmap : resultsList) {
				if (resultmap != null) {
					for (Map.Entry<String, Object> entry : resultmap.entrySet()) {
					    String k = entry.getKey();
					    Object v = entry.getValue();
					    System.out.println(k + "=" + v);
					}

					fileList.add(ResultListBuilder.handleFileReturn(resultmap, server));
				}
			}
			
			assertNotNull(fileList);
			assertTrue(fileList.size() > 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
