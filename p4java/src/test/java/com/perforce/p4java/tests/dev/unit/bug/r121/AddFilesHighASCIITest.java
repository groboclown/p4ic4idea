/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r121;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.zip.ZipException;

import static com.perforce.p4java.tests.ServerMessageMatcher.doesMessageContainText;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 add' ignore files
 */
@Jobs({ "job052619" })
@TestId("Dev121_IgnoreFilesTest")
public class AddFilesHighASCIITest extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", AddFilesHighASCIITest.class.getSimpleName());

	IClient client = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			Properties props = new Properties();
			props.put(PropertyDefs.IGNORE_FILE_NAME_KEY, ".p4ignore");
			setupServer(p4d.getRSHURL(), userName, password, true, props);
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
	 * Test 'p4 add' files with high ASCII names.
	 */
	@Test
	public void testHighASCIINames() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = client.getRoot() + "/112Dev/GetOpenedFilesTest/"
				+ "branch" + randNum;

		String testZipFile = client.getRoot()
				+ "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd2/dir1.zip";

		try {
			SortedMap<String,Charset> charsetMap = Charset.availableCharsets();
			debugPrint("------------- availableCharsets ----------------");
			for (Map.Entry<String, Charset> entry : charsetMap.entrySet())
			{
				String canonicalCharsetName = entry.getKey();
				Log.stats(canonicalCharsetName);
				Charset charset = entry.getValue();
				Set<String> aliases = charset.aliases();
				for (String alias : aliases) {
					Log.stats("\t" + alias);
				}
			}
			debugPrint("-----------------------------------------");

			String[] perforceCharsets = PerforceCharsets.getKnownCharsets();
			debugPrint("------------- perforceCharsets ----------------");
			for (String perforceCharset : perforceCharsets) {
				debugPrint(perforceCharset + " ... " + PerforceCharsets.getJavaCharsetName(perforceCharset));
			}
			debugPrint("-----------------------------------------");
			
			debugPrint("Charset.defaultCharset().name(): " + Charset.defaultCharset().name());

			String charsetName = server.supportsUnicode() ? server.getCharsetName() : CharsetDefs.UTF8_NAME;
			
			debugPrint("Add files default charset: " + Charset.defaultCharset().name());

			debugPrint("Add files charset: " + charsetName);
			
			files = client.sync(FileSpecBuilder.makeFileSpecList(testZipFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);
			assertTrue("Sync returned an empty list of FileSpecs<"+files+">",files.size() > 0 );
			
			List<IServerMessage> syncErrorMessages = new ArrayList<>();

			for (IFileSpec f : files) {
				if ( FileSpecOpStatus.ERROR.equals(f.getOpStatus())) {
					syncErrorMessages.add(f.getStatusMessage());
				}
			}
			assertTrue("Add files returned errors <" + syncErrorMessages + ">", syncErrorMessages.size() == 0);

			unpack(new File(testZipFile), new File(dir));

			dir += File.separator + "dir1";
			//dir += File.separator + "dir1" + File.separator + "highascii";

			File addDir = new File(dir);

			FilenameFilter addFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.startsWith(".");
				}
			};
			List<File> addFiles = new ArrayList<File>();

			getFiles(addDir, addFilter, addFiles);
			assertTrue(addFiles.size() > 0);

			debugPrint("------------- addFiles ----------------");
			for (File file : addFiles) {
				if (file != null) {
					debugPrint(file.getAbsolutePath());
				}
			}
			debugPrint("-----------------------------------------");

			List<String> addFilePaths = new ArrayList<String>();
			for (File f : addFiles) {
				// Add the file paths using the client's encoding
				// Use default if it is not set
				addFilePaths.add(new String(f.getAbsolutePath().getBytes(
						charsetName), Charset.defaultCharset()));
				//addFilePaths.add(f.getAbsolutePath());
			}

			String[] localFiles = addFilePaths.toArray(new String[addFilePaths
					.size()]);

			debugPrint("------------- localFiles ----------------");
			for (String file : localFiles) {
				if (file != null) {
					debugPrint(file);
				}
			}
			debugPrint("-----------------------------------------");
			
			changelist = getNewChangelist(server, client,
					"Dev121_IgnoreFilesTest add files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Add files
			files = client.addFiles(
					FileSpecBuilder.makeFileSpecList(localFiles),
					new AddFilesOptions().setChangelistId(changelist.getId()));

			assertNotNull(files);
			assertTrue("Add files returned an empty list of FileSpecs<"+files+">",files.size() > 0 );
			int infoCount = 0;
			List<IServerMessage> addErrorMessages = new ArrayList<>();
			debugPrint("------------- files ----------------");
			for (IFileSpec f : files) {
				if (f.getOpStatus() == FileSpecOpStatus.INFO) {
					assertNotNull(f.getStatusMessage());
					debugPrint(f.getStatusString());
					if (doesMessageContainText(f.getStatusMessage(), "ignored file can't be added")
						|| doesMessageContainText(f.getStatusMessage(), "using text instead of")) {
						infoCount++;
					}
				} else if ( FileSpecOpStatus.ERROR.equals(f.getOpStatus())) {
					addErrorMessages.add(f.getStatusMessage());
				} else {
					debugPrint(f.toString());
				}
			}
			debugPrint("------------------------------------");
			assertTrue("Add files returned errors <" + addErrorMessages + ">", addErrorMessages.size() == 0);
			assertTrue("Add files returned no INFO messages!", infoCount > 0);

			changelist.refresh();
			files = changelist.getFiles(true);
			assertNotNull(files);
			assertTrue(files.size() > 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (ZipException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
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
			// Recursively delete the local test files
			if (dir != null) {
				deleteDir(new File(dir));
			}
		}
	}
}
