/**
 *
 */
package com.perforce.p4java.tests.dev.unit.feature.admin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the IServer.getExportList method. Not intended to be much
 * more than a quick sanity test, as getExportList is not a fully-supported
 * method (at least for the 9.2 release).
 */
public class ExportListTest extends P4JavaTestCase {

    public ExportListTest() {
    }

    @Test
    public void testLimitedCheckpointExport() {
        try {
            int maxRecs = 100; // NOTE; may not even be this many recs...
            IServer server = getServerAsSuper();
            assertNotNull("Null server returned from getServerAsSuper", server);
            int latestCheckpoint=new Integer(server.getCounter("journal"));

            List<Map<String, Object>> exportList = server.getExportRecords(false, maxRecs, latestCheckpoint, 0, true, null, null);
            assertNotNull("Null export list returned from server", exportList);
            assertTrue("Checkpoint output should have been limited to 100 (plus a summary record), by the -l flag. "
            		+ "It actually output " + exportList.size(),
            	(maxRecs + 1) >= exportList.size()); // adds status map entry at end

         } catch (Throwable thr) {
            fail("Unexpected exception: " + thr);
    	}
    }
    @Test
    public void testLimitedJournalExport() {
        try {
            int maxRecs = 100; // NOTE; may not even be this many recs...
            IServer server = getServerAsSuper();
            assertNotNull("Null server returned from getServerAsSuper", server);
            int latestCheckpoint=new Integer(server.getCounter("journal"));

            List<Map<String, Object>> exportList = server.getExportRecords(false, maxRecs, latestCheckpoint, 0, true, null, null);
            assertNotNull("Null export list returned from server", exportList);
            assertTrue("Journal output should have been limited to 100 (plus a summary record), by the -l flag. "
            		+ "It actually output " + exportList.size(),
            	(maxRecs + 1) >= exportList.size()); // adds status map entry at end
         } catch (Throwable thr) {
            fail("Unexpected exception: " + thr);
    	}
    }

}
