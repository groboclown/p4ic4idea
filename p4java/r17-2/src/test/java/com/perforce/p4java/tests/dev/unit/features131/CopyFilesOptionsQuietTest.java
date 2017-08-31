/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the Options and CopyFilesOptions functionality.
 */
@Jobs({ "job059637" })
@TestId("Dev131_CopyFilesOptionsQuietTest")
public class CopyFilesOptionsQuietTest extends P4JavaTestCase {

	private static IClient client = null;


	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() throws Exception{
		// initialization code (before each test).
			server = getServer();
			assertNotNull(server);
            client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		afterEach(server);
	}

	/**
	 * Test default values.
	 */
    @Test
    public void testDefaultCopyFilesOptionsValues() {
        try {
            CopyFilesOptions opts = new CopyFilesOptions();
            assertFalse(opts.isNoUpdate());
            assertFalse(opts.isNoClientSyncOrMod());
            assertFalse(opts.isQuiet());
            assertFalse(opts.isBidirectional());
            assertFalse(opts.isForceStreamCopy());
            assertFalse(opts.isReverseMapping());
            assertEquals(IChangelist.UNKNOWN, opts.getChangelistId());
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test positive values.
     */
    @Test
    public void testAllPositiveCopyFilesOptionsValues() {
        try {
            CopyFilesOptions opts = new CopyFilesOptions()
            .setChangelistId(100)
            .setNoUpdate(true)
            .setQuiet(true)
            .setNoClientSyncOrMod(false)
            .setBidirectional(true)
            .setReverseMapping(false)
            .setMaxFiles(1000);

            assertEquals(100, opts.getChangelistId());
            assertEquals(true, opts.isNoUpdate());
            assertEquals(true, opts.isQuiet());
            assertEquals(false, opts.isNoClientSyncOrMod());
            assertEquals(true, opts.isBidirectional());
            assertEquals(false, opts.isReverseMapping());
            assertEquals(1000, opts.getMaxFiles());
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

}
