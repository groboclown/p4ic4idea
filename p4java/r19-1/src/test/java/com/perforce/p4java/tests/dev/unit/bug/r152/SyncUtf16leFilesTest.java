/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test utf16-le encoded files: UTF-16LE BOM: "ff fe"
 * sync'd to client as UTF-16BE BOM: "fe ff"
 */
@Jobs({ "job074785" })
@TestId("Dev151_SyncFilesUTF16LETest")
public class SyncUtf16leFilesTest extends P4JavaRshTestCase {


	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SyncUtf16leFilesTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
    }

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		try {
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}

	
	/**
	 * Test sync utf16-le encoded files: UTF-16LE BOM: ff fe
	 */
	@Test
	public void testSyncUtf16leFiles() {
		String depotFile = null;

		try {
			IClient client = server.getClient("p4TestUserWSMac");
			assertNotNull(client);
			server.setCurrentClient(client);
			SortedMap<String, Charset> charsetMap = Charset.availableCharsets();

			debugPrint("------------- availableCharsets ----------------");
			for (Map.Entry<String, Charset> entry : charsetMap.entrySet()) {
				String canonicalCharsetName = entry.getKey();
				debugPrint(canonicalCharsetName);
				Charset charset = entry.getValue();
				Set<String> aliases = charset.aliases();
				for (String alias : aliases) {
					debugPrint("\t" + alias);
				}
			}
			debugPrint("-----------------------------------------");

			String[] perforceCharsets = PerforceCharsets.getKnownCharsets();
			debugPrint("------------- perforceCharsets ----------------");
			for (String perforceCharset : perforceCharsets) {
				debugPrint(perforceCharset + " ... "
						+ PerforceCharsets.getJavaCharsetName(perforceCharset));
			}
			debugPrint("-----------------------------------------");

			debugPrint("-----------------------------------------");
			debugPrint("Charset.defaultCharset().name(): "
					+ Charset.defaultCharset().name());
			debugPrint("-----------------------------------------");
			
			
			depotFile = "//depot/152Bugs/utf16-le/utf16-le_test.txt";

			List<IFileSpec> files = client.sync(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
