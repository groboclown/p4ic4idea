/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r141;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r152.ShelveChangelistClientTest;

/**
 * Test IServer.getFileContents() with non-existing file and non-existing
 * revision.
 */
@Jobs({ "job072521" })
@TestId("Dev141_GetFileContentNonExistingFileTest")
public class GetFileContentNonExistingFileTest extends P4JavaRshTestCase {

	
	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetFileContentNonExistingFileTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
    
    }
	
	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			//server = getServer();
			//assertNotNull(server);
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}

	
	/**
	 * Test IServer.getFileContents() with non-existing file and non-existing
	 * revision.
	 */
	@Test
	public void testGetFileContentNonExistingFile() {

		// Non-existing file
		String[] depotFiles = { "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java-non-existing",
				"//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java#9999999"	
		};

		InputStream is = null;

		try {
			is = server.getFileContents(FileSpecBuilder.makeFileSpecList(depotFiles), new GetFileContentsOptions());
			assertNotNull(is);

			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			// Capture file headers
			Map<String, String> headers = new HashMap<String, String>();
			String line;
			while ((line = br.readLine()) != null) {
				headers.put(line, line);
			}
			br.close();
			assertNotNull(headers);
			// Should contain headers
			assertTrue(headers.size() > 0);

			assertTrue(headers.containsKey(depotFiles[0] + " - no such file(s)."));
			assertTrue(headers.containsKey(depotFiles[1] + " - no file(s) at that revision."));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// Do nothing
				}
			}
		}
	}
}
