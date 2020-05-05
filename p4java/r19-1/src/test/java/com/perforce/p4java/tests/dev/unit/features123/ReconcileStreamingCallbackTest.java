package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder;
import com.perforce.p4java.option.client.ReconcileFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test reconcile using IStreamingCallback.
 */
@TestId("Dev123_ReconcileStreamingCallbackTest")
public class ReconcileStreamingCallbackTest extends P4JavaRshTestCase {

	IClient client = null;
	IChangelist changelist = null;
	List<IFileSpec> files = null;
	String serverMessage = null;
	long completedTime = 0;

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ReconcileStreamingCallbackTest.class.getSimpleName());


	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			Properties props = new Properties();
			setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, props);
			props.put("enableProgress", "true");
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
	 * Test reconcile files using IStreamingCallback
	 */
	@Test
	public void testReconcileFiles() {
		String depotFile = null;

		try {

			depotFile = "//depot/112Dev/GetOpenedFilesTest/bin/com/perforce/p4cmd/...";

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList);

			client.reconcileFiles(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new ReconcileFilesOptions(),
					handler,
					key);

			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			List<IFileSpec> fileList = new ArrayList<IFileSpec>();

			for (Map<String, Object> resultmap : resultsList) {
				if (resultmap != null) {
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
