/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 add' ignore files
 */
@Jobs({ "job051892" })
@TestId("Dev121_IgnoreFilesTest")
public class IgnoreFilesTest extends P4JavaTestCase {

	final static String serverURL = "p4java://eng-p4java-vm.perforce.com:20121";

	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			Properties properties = new Properties();
			properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM,
					".p4ignore");

			server = ServerFactory.getOptionsServer(serverURL, properties);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					serverMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					serverMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					serverMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					serverMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					serverMessage = String.valueOf(millisecsTaken);
				}
			});
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}
			server.setUserName("p4jtestuser");
			server.login("p4jtestuser");
			client = server.getClient(getPlatformClientName("p4TestUserWS20112"));
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
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
	 * Test 'p4 add' ignore files
	 */
	@Test
	@Ignore("Skip until Windows unicode paths are resolved")
	public void testIgnoreUnicodeFiles() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = client.getRoot() + "/112Dev/GetOpenedFilesTest/"
				+ "branch" + randNum;

		String testZipFile = client.getRoot()
				+ "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd2/dir1.zip";

		String charsetName = server.getCharsetName() == null ? Charset
				.defaultCharset().name() : server.getCharsetName();
				
		try {
			files = client.sync(FileSpecBuilder.makeFileSpecList(testZipFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			unpack(new File(testZipFile), new File(dir));

			dir += File.separator + "dir1" + File.separator + "dir2"
					+ File.separator + "unicode";

			File addDir = new File(dir);

			FilenameFilter addFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.startsWith(".");
				}
			};
			List<File> addFiles = new ArrayList<File>();

			getFiles(addDir, addFilter, addFiles);
			assertTrue(addFiles.size() > 0);

			List<String> addFilePaths = new ArrayList<String>();
			for (File f : addFiles) {
				// Add the file paths using the client's encoding
				// Use default if it is not set
				addFilePaths.add(new String(f.getAbsolutePath()
						.getBytes(charsetName), charsetName));
			}

			String[] localFiles = addFilePaths.toArray(new String[addFilePaths
					.size()]);

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

			int ignoreCount = 0;
			for (IFileSpec f : files) {
				if (f.getOpStatus() == FileSpecOpStatus.INFO) {
					assertNotNull(f.getStatusMessage());
					System.out.println(f.getStatusMessage());
					if (f.getStatusMessage().contains(
							"ignored file can't be added")) {
						ignoreCount++;
					}
				} else {

				}
			}
			assertTrue(ignoreCount > 0);

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

	/**
	 * Test 'p4 add' ignore files
	 */
	@Test
	public void testIgnoreFiles() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = client.getRoot() + "/112Dev/GetOpenedFilesTest/"
				+ "branch" + randNum;

		String testZipFile = client.getRoot()
				+ "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd2/dir1.zip";

		try {
			files = client.sync(FileSpecBuilder.makeFileSpecList(testZipFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			unpack(new File(testZipFile), new File(dir));

			File addDir = new File(dir);

			FilenameFilter addFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.startsWith(".");
				}
			};
			List<File> addFiles = new ArrayList<File>();

			getFiles(addDir, addFilter, addFiles);
			assertTrue(addFiles.size() > 0);

			List<String> addFilePaths = new ArrayList<String>();
			for (File f : addFiles) {
				addFilePaths.add(f.getAbsolutePath());
			}

			String[] localFiles = addFilePaths.toArray(new String[addFilePaths
					.size()]);

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

			int ignoreCount = 0;
			for (IFileSpec f : files) {
				if (f.getOpStatus() == FileSpecOpStatus.INFO) {
					assertNotNull(f.getStatusMessage());
					System.out.println(f.getStatusMessage());
					if (f.getStatusMessage().contains(
							"ignored file can't be added")) {
						ignoreCount++;
					}
				} else {

				}
			}
			assertTrue(ignoreCount > 0);

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

	/**
	 * Test 'p4 add -t binary' ignore files with force type
	 */
	@Test
	public void testIgnoreWithForceType() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = client.getRoot() + "/112Dev/GetOpenedFilesTest/"
				+ "branch" + randNum;

		String testZipFile = client.getRoot()
				+ "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd2/dir1.zip";

		try {
			files = client.sync(FileSpecBuilder.makeFileSpecList(testZipFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			unpack(new File(testZipFile), new File(dir));

			File addDir = new File(dir);

			FilenameFilter addFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.startsWith(".");
				}
			};
			List<File> addFiles = new ArrayList<File>();

			getFiles(addDir, addFilter, addFiles);
			assertTrue(addFiles.size() > 0);

			List<String> addFilePaths = new ArrayList<String>();
			for (File f : addFiles) {
				addFilePaths.add(f.getAbsolutePath());
			}

			String[] localFiles = addFilePaths.toArray(new String[addFilePaths
					.size()]);

			changelist = getNewChangelist(server, client,
					"Dev121_IgnoreFilesTest add files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Add files with force type binary
			files = client.addFiles(FileSpecBuilder
					.makeFileSpecList(localFiles), new AddFilesOptions()
					.setChangelistId(changelist.getId()).setFileType("binary"));

			assertNotNull(files);

			int ignoreCount = 0;
			for (IFileSpec f : files) {
				if (f.getOpStatus() == FileSpecOpStatus.INFO) {
					assertNotNull(f.getStatusMessage());
					assertTrue(f.getStatusMessage().contains(
							"ignored file can't be added"));
					ignoreCount++;
				}
			}
			assertTrue(ignoreCount > 0);

			changelist.refresh();
			files = changelist.getFiles(true);
			assertNotNull(files);
			assertTrue(files.size() > 0);

			for (IFileSpec f : files) {
				assertTrue(f.getFileType().contentEquals("binary"));
			}

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

	/**
	 * Test 'p4 add -I' no ignore checking
	 */
	@Test
	public void testNoIgnoreChecking() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = client.getRoot() + "/112Dev/GetOpenedFilesTest/"
				+ "branch" + randNum;

		String testZipFile = client.getRoot()
				+ "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd2/dir1.zip";

		try {
			files = client.sync(FileSpecBuilder.makeFileSpecList(testZipFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			unpack(new File(testZipFile), new File(dir));

			File addDir = new File(dir);

			FilenameFilter addFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.startsWith(".");
				}
			};
			List<File> addFiles = new ArrayList<File>();

			getFiles(addDir, addFilter, addFiles);
			assertTrue(addFiles.size() > 0);

			List<String> addFilePaths = new ArrayList<String>();
			for (File f : addFiles) {
				addFilePaths.add(f.getAbsolutePath());
			}

			String[] localFiles = addFilePaths.toArray(new String[addFilePaths
					.size()]);

			changelist = getNewChangelist(server, client,
					"Dev121_IgnoreFilesTest add files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Add files with no ignore checking
			files = client.addFiles(FileSpecBuilder
					.makeFileSpecList(localFiles),
					new AddFilesOptions().setChangelistId(changelist.getId())
							.setNoIgnoreChecking(true));

			assertNotNull(files);

			int ignoreCount = 0;
			for (IFileSpec f : files) {
				if (f.getOpStatus() == FileSpecOpStatus.INFO) {
					assertNotNull(f.getStatusMessage());
					// If we try to add ignored files it should let us.
					// Therefore the status message should not contain
					// anything about ignored files.
					if (f.getStatusMessage().contains(
							"ignored file can't be added")){
						ignoreCount++;
					}
					
				}
			}
			assertTrue(ignoreCount == 0);

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

	/**
	 * Test set ignore file name
	 */
	@Test
	public void testSetIgnoreFileName() {

		String serverUri = "p4javassl://eng-p4java-vm.perforce.com:30121";

		try {
			Properties properties = new Properties();
			properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM,
					".myp4ignore");

			server = ServerFactory.getOptionsServer(serverUri, properties);
			assertNotNull(server);

			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}

			// Set the server user
			server.setUserName(this.userName);

			// Login using the normal method
			server.login(this.password, new LoginOptions());

			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);

			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);

			// The ignore file name should be set
			assertEquals(".myp4ignore", ((Server) server).getIgnoreFileName());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
