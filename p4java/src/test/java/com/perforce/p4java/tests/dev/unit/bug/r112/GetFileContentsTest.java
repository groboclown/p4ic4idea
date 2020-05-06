/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 print -a' command. Retrieve the contents of a depot file to the
 * client's standard output. The file is not synced. If file is specified using
 * client syntax, Perforce uses the client view to determine the corresponding
 * depot file.
 * 
 * The "-a" flag prints all revisions within the specified range, rather than
 * just the highest revision in the range.
 * 
 * Since "-q" is not used, the output will have the initial line that displays
 * the file name and revision.
 */
@Jobs({ "job042748" })
@TestId("Dev112_GetFileContentsTest")
public class GetFileContentsTest extends P4JavaRshTestCase {

	IClient client = null;

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetFileContentsTest.class.getSimpleName());

	private String fileName = "112Dev/GetOpenedFilesTest/src/com/perforce/main/P4CmdDispatcher.java";
	private String copyFile = "//depot/" + fileName;
	private String depotFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java";

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = getClient(server);
			createTextFileOnServer(client, fileName, "desc");
			//make 6 revs of depotFile
			copyFile(server, client, "desc", copyFile, depotFile);
			editFile(server, client, "edit file", depotFile);
			editFile(server, client, "edit file", depotFile);
			editFile(server, client, "edit file", depotFile);
			editFile(server, client, "edit file", depotFile);
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
	 * When requesting all revisions of a given file, and setting the third
	 * argument "setNoHeaders" to false, which should cause this to work almost
	 * like "p4 print -a" no headers are returned between the files content.
	 * 
	 * When specifying a specific file revision in the first argument, such as
	 * "myfile.java#9", but setting the second argument "setAllRevs" to true,
	 * all revisions are still returned. I would suggest that the scope of the
	 * filespec should limit what could be returned. I think most devs would
	 * take "setAllRevs" to mean "all revs within the bounds of the specified
	 * filespec".
	 */
	@Test
	public void testGetFileContent() {
		// Limit the file to a rev (up-to that rev)
		String depotFileRev = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java#3";

		try {
			// Test "p4 -a" with file header (file name, rev, etc)
			BufferedReader br = new BufferedReader(new InputStreamReader(
					server.getFileContents(
							FileSpecBuilder.makeFileSpecList(depotFile), true,
							false)));

			// Capture file headers
			List<String> headers = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains(depotFile)) {
					headers.add(line);
				}
			}
			br.close();
			assertNotNull(headers);
			// Should contain at least 6 headers
			assertTrue(headers.size() >= 6);
			
			// Test "p4 -a" with revision number appended to the depot file 
			br = new BufferedReader(new InputStreamReader(
					server.getFileContents(
							FileSpecBuilder.makeFileSpecList(depotFileRev), true,
							false)));

			// Capture file headers
			headers = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				if (line.contains(depotFile)) {
					headers.add(line);
				}
			}
			br.close();
			// Should contain 3 headers
			assertTrue(headers.size() == 3);
			
			// Test "p4 -a -q" suppressing file header
			br = new BufferedReader(new InputStreamReader(
					server.getFileContents(
							FileSpecBuilder.makeFileSpecList(depotFile), true,
							true)));

			// Capture file headers
			headers = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				if (line.contains(depotFile)) {
					headers.add(line);
				}
			}
			br.close();
			// Should contain no headers
			assertTrue(headers.size() == 0);

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}
}
