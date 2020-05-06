/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test submit big files using IStreamingCallback.
 */
@Jobs({ "job059434" })
@TestId("Dev123_SubmitBigFileProgressIndicatorTest")
public class SubmitBigFileProgressIndicatorTest extends P4JavaRshTestCase {

	IClient client = null;
	IChangelist changelist = null;
	List<IFileSpec> files = null;
	String serverMessage = null;
	long completedTime = 0;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SubmitBigFileProgressIndicatorTest.class.getSimpleName());

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
			Properties props = new Properties();
			props.put("enableProgress", "true");
			setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, props);
            assertNotNull(server);
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
	 * Test submit big files using IStreamingCallback.
	 */
	@Test
	public void testSubmitFiles() {
		int randNum = getRandomInt();
		String depotFile = null;

		try {

			String sourceFile = client.getRoot() + File.separator + "localTestFiles2.tar.gz";
			String targetFile = client.getRoot() + File.separator + "localTestFiles2.tar.gz" + "-" + randNum;
			depotFile = "//depot/localTestFiles2.tar.gz" + "-" + randNum;

			List<IFileSpec> files = client.sync(
					FileSpecBuilder.makeFileSpecList(sourceFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			// Copy a file to be used for add
			copyFile(sourceFile, targetFile);

			changelist = getNewChangelist(server, client,
					"SubmitStreamingCallbackTest add files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Add a file specified as "binary" even though it is "text"
			files = client.addFiles(FileSpecBuilder.makeFileSpecList(targetFile),
					new AddFilesOptions().setChangelistId(changelist.getId())
							.setFileType("binary"));

			assertNotNull(files);
			changelist.refresh();

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList);

			changelist.submit(new SubmitOptions(), handler, key);

			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			List<IFileSpec> fileList = new ArrayList<IFileSpec>();

			for (Map<String, Object> resultmap : resultsList) {
				if (resultmap != null) {
					for (Map.Entry<String, Object> entry : resultmap.entrySet()) {
					    String k = entry.getKey();
					    Object v = entry.getValue();
					    debugPrint(k + "=" + v);
					}

					if (resultmap.get("submittedChange") != null) {
						int id = new Integer((String) resultmap.get("submittedChange"));
						ChangelistStatus status = ChangelistStatus.SUBMITTED;
						fileList.add(new FileSpec(FileSpecOpStatus.INFO,
								dummyServerInfoMessage("Submitted as change " + id)));
					} else if (resultmap.get("locked") != null) {
						// disregard this message for now
					} else {
						fileList.add(ResultListBuilder.handleFileReturn(resultmap, server));
					}
				}
			}

			assertNotNull(fileList);
			assertTrue(fileList.size() > 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (client != null) {
				if (changelist != null) {
					if (changelist.getStatus() == ChangelistStatus.PENDING) {
						try {
							// Revert files in pending changelist
							client.revertFiles(
									changelist.getFiles(true),
									new RevertFilesOptions()
											.setChangelistId(changelist.getId()));
						} catch (P4JavaException e) {
							// Can't do much here...
						}
					}
				}
			}
			if (client != null && server != null) {
				if (depotFile != null) {
					try {
						// Delete submitted test files
						IChangelist deleteChangelist = getNewChangelist(server,
								client,
								"SubmitStreamingCallbackTest delete submitted files");
						deleteChangelist = client
								.createChangelist(deleteChangelist);
						client.deleteFiles(FileSpecBuilder
								.makeFileSpecList(new String[] { depotFile }),
								new DeleteFilesOptions()
										.setChangelistId(deleteChangelist
												.getId()));
						deleteChangelist.refresh();
						deleteChangelist.submit(null);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
			}
		}
	}

}
