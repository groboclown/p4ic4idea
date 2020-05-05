/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.IStreamingCallback;
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
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test 'p4 dirs' streaming command.
 */
@Jobs({ "job046233" })
@TestId("Dev112_StreamingDirsTest")
public class StreamingDirsTest extends P4JavaRshTestCase {

	IOptionsServer server = null;
	IClient client = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", StreamingDirsTest.class.getSimpleName());

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
	 * Test 'p4 dirs' streaming command.
	 */
	@Test
	public void testDirs() {

		try {
			server.execStreamingMapCommand("dirs",
					new String[] { "//p4TestUserWS/basic/readonly/*" }, null,
					new IStreamingCallback() {
						List<IFileSpec> specList = new ArrayList<IFileSpec>();

						public boolean startResults(int key)
								throws P4JavaException {
							return true;
						}

						public boolean handleResult(
								Map<String, Object> resultMap, int key)
								throws P4JavaException {
							IFileSpec file = null;
							if (resultMap != null) {
								String errStr = ((Server) server)
										.handleFileErrorStr(resultMap);
								if (errStr == null) {
									file = new FileSpec((String) resultMap
											.get("dir"));
									specList.add(file);
								} else {
									if (((Server) server)
											.isInfoMessage(resultMap)) {
										if (resultMap.get("dirName") != null) {
											file = new FileSpec(
													(String) resultMap
															.get("dirName"));
											specList.add(file);
										} else {
											file = new FileSpec(
													FileSpecOpStatus.INFO,
													errStr, (String) resultMap
															.get("code0"));
											specList.add(file);
										}
									} else {
										file = new FileSpec(
												FileSpecOpStatus.ERROR, errStr,
												(String) resultMap.get("code0"));
										specList.add(file);
									}
								}
							}
							assertNotNull(file);
							if (file.getOpStatus() == FileSpecOpStatus.VALID) {
								System.out.println(file.toString());
							} else {
								assertNotNull(file.getStatusMessage());
								System.out.println(file.getStatusMessage());
							}

							return true;
						}

						public boolean endResults(int key)
								throws P4JavaException {
							return true;
						}
					}, 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test 'p4 dirs' streaming command.
	 */
	@Test
	public void testDirsBadPath() {

		try {
			server.execStreamingMapCommand("dirs",
					new String[] { "//depot/baz/*" }, null,
					new IStreamingCallback() {
						List<IFileSpec> specList = new ArrayList<IFileSpec>();

						public boolean startResults(int key)
								throws P4JavaException {
							return true;
						}

						public boolean handleResult(
								Map<String, Object> resultMap, int key)
								throws P4JavaException {
							IFileSpec file = null;
							if (resultMap != null) {
								String errStr = ((Server) server)
										.handleFileErrorStr(resultMap);
								if (errStr == null) {
									file = new FileSpec((String) resultMap
											.get("dir"));
									specList.add(file);
								} else {
									if (((Server) server)
											.isInfoMessage(resultMap)) {
										if (resultMap.get("dirName") != null) {
											file = new FileSpec(
													(String) resultMap
															.get("dirName"));
											specList.add(file);
										} else {
											file = new FileSpec(
													FileSpecOpStatus.INFO,
													errStr, (String) resultMap
															.get("code0"));
											specList.add(file);
										}
									} else {
										file = new FileSpec(
												FileSpecOpStatus.ERROR, errStr,
												(String) resultMap.get("code0"));
										specList.add(file);
									}
								}
							}
							assertNotNull(file);
							if (file.getOpStatus() == FileSpecOpStatus.VALID) {
								System.out.println(file.toString());
							} else {
								assertNotNull(file.getStatusMessage());
								System.out.println(file.getStatusMessage());
							}

							return true;
						}

						public boolean endResults(int key)
								throws P4JavaException {
							return true;
						}
					}, 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
