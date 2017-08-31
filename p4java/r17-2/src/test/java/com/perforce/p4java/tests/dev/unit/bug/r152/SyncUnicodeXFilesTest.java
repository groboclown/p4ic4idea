/**
 * Copyright (c) 2016 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.SyncOptions;

import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test unicode-x encoded files
 */
@Jobs({ "job085473" })
@TestId("Dev151_SyncUnicodeXFilesTest")
public class SyncUnicodeXFilesTest extends P4JavaRshTestCase {
	
	
	@ClassRule
	public static SimpleServerRule p4d = new UnicodeServerRule("r16.1", SyncUnicodeXFilesTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
    	//unicodeServerUrlString
    }
    
	/**
	 * Test sync unicode-x encoded files
	 */
	@Test
	public void testSyncUnicodeXFiles() {
		try {
			IClient client = server.getClient("p4TestUserWS");
			server.setCurrentClient(server.getClient("p4TestUserWS"));
			server.setCharsetName("shiftjis");
			String unicodeXDepotFile = "//depot/case909/vvvv.bat";
			byte[] bytesOfUnicodeXFile = syncFileFromDepot(unicodeXDepotFile, client);
			String unicodeDepotFile = "//depot/case909/vvvv-nox.bat";
			byte[] bytesOfUnicodeFile = syncFileFromDepot(unicodeDepotFile, client);
			assertArrayEquals(bytesOfUnicodeXFile, bytesOfUnicodeFile);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	private byte[] syncFileFromDepot(String depotFile, IClient client) throws P4JavaException, IOException {
		List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList(depotFile), new SyncOptions().setForceUpdate(true));
		if (files.size() < 1) {
			throw new P4JavaException("Can't find the test file: " + depotFile);
		} else {
			InputStream FileContents = files.get(0).getContents(true);
			return readBytesFromInputStream(FileContents);
		}
	}

	private byte[] readBytesFromInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int length;
		byte[] data = new byte[1024];
		while ((length = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, length);
		}

		buffer.flush();
		return buffer.toByteArray();
	}
}
