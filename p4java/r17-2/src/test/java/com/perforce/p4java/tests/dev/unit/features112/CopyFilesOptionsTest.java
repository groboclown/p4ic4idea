/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the Options and CopyFilesOptions functionality.
 */
@Jobs({ "job046063" })
@TestId("Dev112_CopyFilesOptionsTest")
public class CopyFilesOptionsTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

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
			server = getServer();
			assertNotNull(server);
            client = getDefaultClient(server);
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

    @Test
    public void testConstructors() {
        try {
            CopyFilesOptions opts = new CopyFilesOptions();
            assertNotNull(opts.processOptions(null));
            assertEquals(0, opts.processOptions(null).size());

            opts = new CopyFilesOptions("-m10", "-cdefault");
            assertNotNull(opts.getOptions());
            String[] optsStrs = opts.getOptions().toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(optsStrs.length, 2);
            assertEquals("-m10", optsStrs[0]);
            assertEquals("-cdefault", optsStrs[1]);

            opts = new CopyFilesOptions(IChangelist.DEFAULT, true, true, true,
                    false, 1000);
            assertTrue(opts.isNoUpdate());
            assertTrue(opts.isNoClientSyncOrMod());
            assertTrue(opts.isBidirectional());
            assertFalse(opts.isReverseMapping());
            assertEquals(IChangelist.DEFAULT, opts.getChangelistId());
        } catch (OptionsException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testToStrings() {
        try {
            CopyFilesOptions opts = new CopyFilesOptions(100, false, true,
                    false, true, 1000);

            assertNotNull(opts.processOptions(null));
            String[] optsStrs = opts.processOptions(null)
                    .toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(4, optsStrs.length);

            // Order is not guaranteed here, so we have to
            // search for the expected strings at each position

            boolean foundChangelistId = false;
            boolean foundNoUpdate = false;
            boolean foundNoClientSyncOrMod = false;
            boolean foundBidirectional = false;
            boolean foundReverseMapping = false;
            boolean foundMaxFiles = false;

            for (String optStr : optsStrs) {
                if (optStr.equals("-c100"))
                    foundChangelistId = true;
                if (optStr.equals("-n"))
                    foundNoUpdate = true;
                if (optStr.equals("-v"))
                    foundNoClientSyncOrMod = true;
                if (optStr.equals("-s"))
                    foundBidirectional = true;
                if (optStr.equals("-r"))
                    foundReverseMapping = true;
                if (optStr.equals("-m1000"))
                    foundMaxFiles = true;
            }

            assertTrue(foundChangelistId);
            assertFalse(foundNoUpdate);
            assertTrue(foundNoClientSyncOrMod);
            assertFalse(foundBidirectional);
            assertTrue(foundReverseMapping);
            assertTrue(foundMaxFiles);

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test setters and chaining
     */
    @Test
    public void testSetters() {
        try {
            CopyFilesOptions opts = new CopyFilesOptions();
            opts.setChangelistId(100);
            opts.setNoUpdate(true);
            opts.setNoClientSyncOrMod(false);
            opts.setBidirectional(true);
            opts.setReverseMapping(false);
            opts.setMaxFiles(1000);

            assertEquals(100, opts.getChangelistId());
            assertEquals(true, opts.isNoUpdate());
            assertEquals(false, opts.isNoClientSyncOrMod());
            assertEquals(true, opts.isBidirectional());
            assertEquals(false, opts.isReverseMapping());
            assertEquals(1000, opts.getMaxFiles());
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test copy of files with limit set (-m)
     */
    @Test
    public void testCopyFilesWithLimit() {
		int randNum = getRandomInt();
        String sourceFiles = "//depot/112Dev/GetOpenedFilesTest/...";
        String targetFiles = "//depot/112Dev/CopyGetOpenedFilesTest" + randNum + "/...";

        IChangelist copyChangelist = null;

        try {
            copyChangelist = getNewChangelist(server, client,
                    "Dev112_CopyFilesOptionsTest copy changelist");
            assertNotNull(copyChangelist);
            copyChangelist = client.createChangelist(copyChangelist);
            CopyFilesOptions options = new CopyFilesOptions();
            options.setChangelistId(copyChangelist.getId());
            options.setMaxFiles(10);

            List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
                    sourceFiles), new FileSpec(targetFiles), null, options);
            assertNotNull(copyFiles);
            List<IFileSpec> nonValidFiles = FileSpecBuilder
                    .getInvalidFileSpecs(copyFiles);
            if (nonValidFiles.size() != 0) {
                fail(nonValidFiles.get(0).getOpStatus() + ": "
                        + nonValidFiles.get(0).getStatusMessage());
            }
            assertEquals("wrong number of valid filespecs after copy", 10,
                    FileSpecBuilder.getValidFileSpecs(copyFiles).size());
            copyChangelist.refresh();
            List<IFileSpec> copyFilesList = copyChangelist.getFiles(true);
            assertEquals("wrong number of filespecs in copy changelist", 10,
                    FileSpecBuilder.getValidFileSpecs(copyFilesList).size());

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        } finally {
            if (client != null) {
                if (copyChangelist != null) {
                    try {
                        List<IFileSpec> fileList = copyChangelist
                                .getFiles(true);
                        RevertFilesOptions opts = new RevertFilesOptions();
                        opts.setChangelistId(copyChangelist.getId());
                        client.revertFiles(fileList, opts);
                    } catch (P4JavaException e) {
                        fail(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
