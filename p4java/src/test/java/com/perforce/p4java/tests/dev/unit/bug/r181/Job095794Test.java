package com.perforce.p4java.tests.dev.unit.bug.r181;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Job095794Test extends P4JavaRshTestCase {

	IClient client = null;

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r18.1", Job095794Test.class.getSimpleName());

	String symlinkTestFileString = "symLinkTestFile";
	String targetTestFileString = "targetTestFile";


	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, null);
			client = createClient(server, "job095794TestClient");

			// Create target file
			Path targetTestFileStringPath = Paths.get(client.getRoot() + "/" + targetTestFileString);
			createFileOnDisk(targetTestFileStringPath.toString());
			File targetTestFile = createFileObject(targetTestFileStringPath.toString());

			// Create symlink
			Path symlinkTestFilePath = Paths.get(client.getRoot() + "/" + symlinkTestFileString);
			Files.createSymbolicLink(symlinkTestFilePath, targetTestFileStringPath);
			File symlinkTestFile = createFileObject(symlinkTestFilePath.toString());

			// Submit files
			IChangelist change = createNewChangelist(client, "test");
			AddFileToChangelist(targetTestFile, change, client);
			AddFileToChangelist(symlinkTestFile, change, client);
			submitChangelist(change);
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
		attemptCleanupFiles(client);
		if (server != null) {
			this.endServerSession(server);
		}
	}

	@Test
	public void testJob095794() throws Exception {
		String[] execMapArgs = {client.getRoot() + "/..."};
		List<Map<String, Object>> resultMaps = server.execMapCmdList("reconcile",execMapArgs, null);
		assertNotNull(resultMaps.get(0).get("fmt0"));
		assertTrue(resultMaps.get(0).get("fmt0").toString().contains("no|No] file(s) to reconcile."));
	}

}
