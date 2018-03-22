/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r151;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test sync files with exec bit set on client. Like the command line client,
 * the Owner, Group and World should all be set.
 */
@Jobs({ "job075630" })
@TestId("Dev151_SyncFilesWithExecBitTest")
public class SyncFilesWithExecBitTest extends P4JavaRshTestCase {

    IChangelist changelist = null;
    List<IFileSpec> files = null;

    @ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SyncFilesWithExecBitTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    }
    
    /**
     * Test sync files with exec bit set on client. Like the command line
     * client, the Owner, Group and World should all be set.
     * 
     * @throws IOException
     */
    @Test
    public void testSyncFilesWithExecBit() throws IOException {
        String depotFile = null;

        try {
			IClient client = server.getClient("p4TestUserWS20112");
            assertNotNull(client);
            server.setCurrentClient(client);
            // use file that has binary and +x bit set on it
            depotFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4JCommandCallbackImpl-13135.java";
            String clientFile = client.getRoot()
                    + "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4JCommandCallbackImpl-13135.java";

            File testClientFile = new File(clientFile);

            List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList(depotFile),
                    new SyncOptions().setForceUpdate(true));
            assertNotNull(files);
            assertTrue(testClientFile.exists());

            Path path = Paths.get(testClientFile.getPath());
            // check that file synced with only owner given permissions
            assertTrue(Files.isExecutable(path));
        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }
}
