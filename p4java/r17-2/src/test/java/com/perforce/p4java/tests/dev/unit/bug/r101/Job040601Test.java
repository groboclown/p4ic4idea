/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * Tests for job040601 -- resolveFiles with binary files.
 */
@TestId("Bugs101_Job040601Test")
public class Job040601Test extends Abstract101TestCase {

    /**
     * Test binary resolve.
     */
    @Test
    public void testBinaryResolve() throws Exception {
        final String testRoot = "//depot/101Bugs/Bugs101_Job040601Test";
        final String test01Name = testRoot + "/" + "test01.jpg";
        final String test02Name = testRoot + "/" + "test02.jpg";
        IOptionsServer server = null;
        IClient client = null;
        IChangelist changelist = null;
        File sourceFile = null;
        FileInputStream sourceStream = null;

        try {
            final int syncSize = 4;
            server = getServer();
            client = getDefaultClient(server);
            assertNotNull(client);
            server.setCurrentClient(client);
            client.revertFiles(FileSpecBuilder.makeFileSpecList(testRoot + "/..."), null);

            List<IFileSpec> synced = this.forceSyncFiles(client, testRoot + "/...");
            assertNotNull(synced);
            assertEquals(syncSize, synced.size());
            assertEquals(0, getErrorsFromFileSpecList(synced).size());
            changelist = client.createChangelist(new Changelist(IChangelist.UNKNOWN,
                    client.getName(), this.getUserName(), ChangelistStatus.NEW, null,
                    "Bugs101_Job040601Test test submit changelist", false, (Server) server));
            assertNotNull(changelist);
            List<IFileSpec> editList = client.editFiles(
                    FileSpecBuilder.makeFileSpecList(test01Name),
                    new EditFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(editList);
            assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());
            changelist.refresh();
            List<IFileSpec> submitList = changelist.submit(null);
            assertNotNull(submitList);
            changelist = client.createChangelist(new Changelist(IChangelist.UNKNOWN,
                    client.getName(), this.getUserName(), ChangelistStatus.NEW, null,
                    "Bugs101_Job040601Test test integration changelist", false, (Server) server));
            List<IFileSpec> integFiles = client.integrateFiles(new FileSpec(test01Name),
                    new FileSpec(test02Name), null,
                    new IntegrateFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(integFiles);
            assertEquals(1, FileSpecBuilder.getValidFileSpecs(integFiles).size());
            String sourcePath = this.getSystemPath(client, test01Name);
            assertNotNull(sourcePath);
            changelist.refresh();
            sourceFile = new File(sourcePath);
            assertTrue(sourceFile.canRead());
            sourceStream = new FileInputStream(sourceFile);
            IFileSpec resolvedFile = client.resolveFile(new FileSpec(test02Name), sourceStream);
            assertNotNull(resolvedFile);
            assertEquals("edit from", resolvedFile.getHowResolved());
            changelist.refresh();
            List<IFileSpec> subList = changelist.submit(null);
            assertNotNull(subList);
        } finally {
            if (server != null) {
                if (client != null) {
                    if ((changelist != null)
                            && (changelist.getStatus() == ChangelistStatus.PENDING)) {
                        try {
                            client.revertFiles(FileSpecBuilder.makeFileSpecList(test02Name),
                                    new RevertFilesOptions().setChangelistId(changelist.getId()));
                            server.deletePendingChangelist(changelist.getId()); // not
                                                                                // strictly
                                                                                // necessary...
                        } catch (P4JavaException exc) {
                        }
                    }
                }
                this.endServerSession(server);
            }
        }
    }
}
