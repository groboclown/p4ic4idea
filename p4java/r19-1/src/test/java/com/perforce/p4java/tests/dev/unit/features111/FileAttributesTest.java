/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features111;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.SetFileAttributesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simple tests for setting and getting file attributes using the
 * IOptionsServer setFileattributes method and associated IExtendedFileSpec
 * gubbins.
 */
@TestId("Features102_FileAttributesTest")
public class FileAttributesTest extends P4JavaRshTestCase {
    
    public FileAttributesTest() {
    }

    IClient client = null;
	final private String filePath = "//depot/102Dev/Attributes";
	final private String test1fileName = filePath + "/" + "test01.txt";
	final private String attr1Name = "test1";
	final private String attr1Value = "Test1Value";
	final private String attr2Name = "test2";
	final private String attr2Value = this.getRandomName("test1");

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", FileAttributesTest.class.getSimpleName());
    
    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            server = getSuperConnection(p4d.getRSHURL());
            assertNotNull(server);
            client = getClient(server);

			Map<String, String> attrMap = new HashMap<String, String>();
			attrMap.put(attr1Name, attr1Value);
			attrMap.put(attr2Name, attr2Value);
			List<IFileSpec> fileList = server.setFileAttributes(
					FileSpecBuilder.makeFileSpecList(test1fileName),
					attrMap,
					new SetFileAttributesOptions().setSetOnSubmittedFiles(true)
			);
			assertNotNull("null file list returned", fileList);
			assertEquals(1, fileList.size());
			assertNotNull(fileList.get(0));
			assertNotNull(fileList.get(0).getOpStatus());
			assertEquals(FileSpecOpStatus.VALID, fileList.get(0).getOpStatus());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

	@Test
	public void testFileAttributesRetrieval() {
		final String attr1HexValue = "546573743156616C7565";
		final String attr2HexValue = "ABCDEF";

		try {
			List<IFileSpec> syncFiles = this.forceSyncFiles(client, filePath + "/...");
			assertTrue(syncFiles.size() > 0);
			assertEquals(syncFiles.get(0).getStatusMessage(), 0, FileSpecBuilder.getInvalidFileSpecs(syncFiles).size());

			FileStatAncilliaryOptions fsaOpts = new FileStatAncilliaryOptions();
			fsaOpts.setShowAttributes(true);
			GetExtendedFilesOptions gefOpts = new GetExtendedFilesOptions();
			gefOpts.setAncilliaryOptions(fsaOpts);
			List<IExtendedFileSpec> fileSpecs = server.getExtendedFiles(
													FileSpecBuilder.makeFileSpecList(test1fileName),
													gefOpts);
			assertNotNull(fileSpecs);
			assertEquals(1, fileSpecs.size());
			IExtendedFileSpec fSpec = fileSpecs.get(0);
			Map<String,byte[]> expectedAttributes = new HashMap<String, byte[]>();
			expectedAttributes.put(attr1Name, attr1Value.getBytes());
			expectedAttributes.put(attr2Name, attr2Value.getBytes());
			checkAttributes(fSpec, expectedAttributes, false);

			fsaOpts.setShowHexAttributes(true);
			fileSpecs = server.getExtendedFiles(
												FileSpecBuilder.makeFileSpecList(test1fileName),
												gefOpts);
			assertNotNull(fileSpecs);
			assertEquals(1, fileSpecs.size());
			expectedAttributes.clear();
			checkAttributes(fileSpecs.get(0), expectedAttributes, false);
			expectedAttributes.put(attr1Name, attr1HexValue.getBytes());
			expectedAttributes.put(attr2Name, attr2HexValue.getBytes());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}

	@Test
	public void testFileTextAttributesSet() {

		final String fileName = filePath + "/" + "test03.txt";
		final String attr2Value = this.getRandomName("test1");

		try {
			List<IFileSpec> syncFiles = this.forceSyncFiles(client, filePath + "/...");
			assertTrue(syncFiles.size() > 0);
			assertEquals(syncFiles.get(0).getStatusMessage(), 0, FileSpecBuilder.getInvalidFileSpecs(syncFiles).size());
			Map<String, byte[]> expectedAttributes = new HashMap<String, byte[]>();
			Map<String, String> attrMap = new HashMap<String, String>();
			attrMap.put(attr1Name, attr1Value);
			attrMap.put(attr2Name, attr2Value);
			List<IFileSpec> fileList = server.setFileAttributes(
									FileSpecBuilder.makeFileSpecList(fileName),
									attrMap,
									new SetFileAttributesOptions().setSetOnSubmittedFiles(true)
								);
			assertNotNull("null file list returned", fileList);
			assertEquals(1, fileList.size());
			assertNotNull(fileList.get(0));
			assertNotNull(fileList.get(0).getOpStatus());
			assertEquals(FileSpecOpStatus.VALID, fileList.get(0).getOpStatus());


			expectedAttributes.put(attr1Name, attr1Value.getBytes());
			expectedAttributes.put(attr2Name, attr2Value.getBytes());
			FileStatAncilliaryOptions fsaOpts = new FileStatAncilliaryOptions();
			fsaOpts.setShowAttributes(true);
			GetExtendedFilesOptions gefOpts = new GetExtendedFilesOptions();
			gefOpts.setAncilliaryOptions(fsaOpts);
			List<IExtendedFileSpec> fileSpecs = server.getExtendedFiles(
													FileSpecBuilder.makeFileSpecList(fileName),
													gefOpts);
			assertNotNull(fileSpecs);
			assertEquals(1, fileSpecs.size());
			checkAttributes(fileSpecs.get(0), expectedAttributes, false);

			attrMap.clear();
			attrMap.put(attr1Name, null);
			attrMap.put(attr2Name, null);
			expectedAttributes.clear();
			expectedAttributes.put(attr1Name, null);
			expectedAttributes.put(attr2Name, null);
			fileList = server.setFileAttributes(
									FileSpecBuilder.makeFileSpecList(fileName),
									attrMap,
									new SetFileAttributesOptions().setSetOnSubmittedFiles(true)
								);
			assertNotNull("null file list returned", fileList);
			assertEquals(1, fileList.size());
			assertNotNull(fileList.get(0));
			assertNotNull(fileList.get(0).getOpStatus());

			fileSpecs = server.getExtendedFiles(
									FileSpecBuilder.makeFileSpecList(fileName),
									gefOpts);
			assertNotNull(fileSpecs);
			assertEquals(1, fileSpecs.size());
			checkAttributes(fileSpecs.get(0), expectedAttributes, false);

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}

	@Test
	public void testStreamAttributeSet() {
		final String fileName = filePath + "/" + "test04.txt";
		final String inputFile = filePath + "/" + "test04inputA.jpg";
		final int imageSize = 92647 * 2; // as hex bytes
		final String hexStart = "ffd8ffe000104a46494600010200006400640000ffec00114475636b79000100".toUpperCase();
		final String hexEnd = "3a74ebfb5d96a22defb62f9ade0b5adb7f96dfcda0ffd9".toUpperCase();

		try {
			List<IFileSpec> syncFiles = this.forceSyncFiles(client, filePath + "/...");
			assertTrue(syncFiles.size() > 0);
			assertEquals(syncFiles.get(0).getStatusMessage(), 0, FileSpecBuilder.getInvalidFileSpecs(syncFiles).size());
			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put(attr1Name, null);
			List<IFileSpec> fileList = server.setFileAttributes(
					FileSpecBuilder.makeFileSpecList(fileName),
					attributes,
					new SetFileAttributesOptions().setSetOnSubmittedFiles(true)
					);
			assertNotNull("null file list returned", fileList);
			assertEquals(1, fileList.size());
			assertNotNull(fileList.get(0));
			assertNotNull(fileList.get(0).getOpStatus());
			// going to assume that worked, given the other tests in this file -- HR.

			fileList = server.setFileAttributes(
					FileSpecBuilder.makeFileSpecList(fileName),
					attr1Name,
					new FileInputStream(new File(this.getSystemPath(client, inputFile))),
					new SetFileAttributesOptions().setSetOnSubmittedFiles(true)
					);
			assertNotNull("null file list returned", fileList);
			assertEquals(1, fileList.size());
			assertNotNull(fileList.get(0));
			assertNotNull(fileList.get(0).getOpStatus());

			FileStatAncilliaryOptions fsaOpts = new FileStatAncilliaryOptions();
			fsaOpts.setShowHexAttributes(true);
			GetExtendedFilesOptions gefOpts = new GetExtendedFilesOptions();
			gefOpts.setAncilliaryOptions(fsaOpts);
			List<IExtendedFileSpec> fileSpecs = server.getExtendedFiles(fileList, gefOpts);
			assertNotNull("null file list returned", fileSpecs);
			assertEquals(1, fileSpecs.size());
			Map<String, byte[]> observedAttributes = fileSpecs.get(0).getAttributes();
			assertNotNull(observedAttributes);
			String observedHex = new String(observedAttributes.get(attr1Name));
			assertNotNull("null hex string returned", observedHex);
			// Perform sanity checks only here...
			assertEquals("attribute size mismatch", imageSize, observedHex.length());
			assertTrue("attribute start mismatch", observedHex.startsWith(hexStart));
			assertTrue("attribute end mismatch", observedHex.endsWith(hexEnd));

			// null it back out again...

			fileList = server.setFileAttributes(
					FileSpecBuilder.makeFileSpecList(fileName),
					attributes,
					new SetFileAttributesOptions().setSetOnSubmittedFiles(true)
					);
			assertNotNull("null file list returned", fileList);
			assertEquals(1, fileList.size());
			assertNotNull(fileList.get(0));
			assertNotNull(fileList.get(0).getOpStatus());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}

	/**
	 * Tests fstat -A workings (or not workings...)
	 */
	@Test
	public void testAttributePatterns() {
		try {
			List<IFileSpec> syncFiles = this.forceSyncFiles(client, filePath + "/...");
			assertTrue(syncFiles.size() > 0);
			assertEquals(syncFiles.get(0).getStatusMessage(), 0, FileSpecBuilder.getInvalidFileSpecs(syncFiles).size());

			FileStatAncilliaryOptions fsaOpts = new FileStatAncilliaryOptions();
			fsaOpts.setShowAttributes(true);
			GetExtendedFilesOptions gefOpts = new GetExtendedFilesOptions();
			gefOpts.setAncilliaryOptions(fsaOpts);
			List<IExtendedFileSpec> fileSpecs = server.getExtendedFiles(
													FileSpecBuilder.makeFileSpecList(test1fileName),
													gefOpts);
			assertNotNull(fileSpecs);
			assertEquals(1, fileSpecs.size());
			IExtendedFileSpec fSpec = fileSpecs.get(0);
			Map<String,byte[]> expectedAttributes = new HashMap<String, byte[]>();
			expectedAttributes.put(attr1Name, attr1Value.getBytes());
			expectedAttributes.put(attr2Name, attr2Value.getBytes());
			checkAttributes(fSpec, expectedAttributes, false);

			gefOpts.setAttributePattern(attr1Name);
			fileSpecs = server.getExtendedFiles(
												FileSpecBuilder.makeFileSpecList(test1fileName),
												gefOpts);
			assertNotNull(fileSpecs);
			assertEquals(1, fileSpecs.size());
			expectedAttributes.clear();
			expectedAttributes.put(attr1Name, attr1Value.getBytes());
			checkAttributes(fileSpecs.get(0), expectedAttributes, true);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}

	private void checkAttributes(IExtendedFileSpec fileSpec,
						Map<String, byte[]> expectedAttributes, boolean strict) throws Exception {
		assertNotNull("null file spec", fileSpec);
		assertNotNull(fileSpec.getOpStatus());
		assertEquals(fileSpec.getStatusMessage(), FileSpecOpStatus.VALID, fileSpec.getOpStatus());
		Map<String, byte[]> attributes = fileSpec.getAttributes();
		assertNotNull(attributes);

		if (expectedAttributes != null) {
			if (strict) {
				assertEquals(expectedAttributes.size(), attributes.size());
			}
			for (String name : expectedAttributes.keySet()) {
				byte[] expectedValue = expectedAttributes.get(name);
				byte[] actualValue = attributes.get(name);

				if ((expectedValue == null) && (actualValue == null)) {
					continue;
				}

				if ((expectedValue == null) && (actualValue != null)) {
					fail("expected attribute value == null, but actual attribute value != null");
				}

				if ((expectedValue != null) && (actualValue == null)) {
					fail("expected attribute value != null, but actual attribute value == null");
				}

				assertEquals("actual != expected attribute size", expectedValue.length, actualValue.length);

				for (int i = 0; i < expectedValue.length; i++) {
					assertEquals("attribute value mismatch", expectedValue[i], actualValue[i]);
				}
			}
		}
	}
}
