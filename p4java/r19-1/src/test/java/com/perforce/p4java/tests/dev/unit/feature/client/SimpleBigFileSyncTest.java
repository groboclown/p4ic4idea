/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.client;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Just tests syncing of a rather large binary file by first nuking it
 * on the client, then doing a standard forced sync. Can take
 * quite a long time...
 */
@TestId("Client_SimpleBigFileSyncTest")
public class SimpleBigFileSyncTest extends P4JavaRshTestCase {

	public SimpleBigFileSyncTest() {
	}

	IClient client = null;
	
	@Rule
    public ExpectedException exception = ExpectedException.none();

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SimpleBigFileSyncTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void beforeEach() throws Exception{
        Properties properties = new Properties();
        setupServer(p4d.getRSHURL(), userName, password, true, properties);
        client = getClient(server);
        createTextFileOnServer(client, "client/SimpleBigFileSyncTest/bigfile01.bin", "desc");
     }

    
	@Test
	public void testBigFileSync() {
		final String testFilePath = "//depot/client/SimpleBigFileSyncTest/bigfile01.bin";
	
		File targetFile = null;
		try {
			targetFile = new File(getSystemPath(client, testFilePath));
			if (targetFile.exists()) {
				// Nuke it with the old sync #0 trick...
				
				List<IFileSpec> syncList = client.sync(
									FileSpecBuilder.makeFileSpecList(testFilePath + "#0"),
									true, false, false, false);
				assertNotNull(syncList);
				assertEquals(1, syncList.size());
				assertNotNull(syncList.get(0));
				assertTrue(syncList.get(0).getOpStatus() != FileSpecOpStatus.ERROR);
			}
			
			// Now try to sync it:
			
			List<IFileSpec> syncList = client.sync(
					FileSpecBuilder.makeFileSpecList(testFilePath),
					true, false, false, false);
			assertNotNull(syncList);
			assertEquals(1, syncList.size());
			assertNotNull(syncList.get(0));
			assertTrue(syncList.get(0).getOpStatus() != FileSpecOpStatus.ERROR);
			
			// Check to see if it's all there:
			
			assertTrue("depot file diffs from local copy",
					this.diffTree(client, FileSpecBuilder.makeFileSpecList(testFilePath), true));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				endServerSession(server);
			}
			if (targetFile != null && targetFile.exists()) {
				targetFile.delete();
			}
		}
	}
}
