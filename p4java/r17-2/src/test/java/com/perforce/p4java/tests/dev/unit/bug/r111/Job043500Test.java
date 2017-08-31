package com.perforce.p4java.tests.dev.unit.bug.r111;

import static com.perforce.p4java.core.file.FileSpecOpStatus.INFO;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Test;

import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests Job043500's issue with branch specs.
 */
@TestId("Bugs111_Job043500Test")
public class Job043500Test extends P4JavaTestCase {
    @AfterClass
    public static void tearDownAll() throws Exception {
        endServerSession(server);
    }

    @Test
    public void testCopyWithBranch() throws Exception {
        final String branchSpecName = "p4jtest-job043500-branch";
        final String testRoot = "//depot/111bugs/Bugs111_Job043500Test";
        final String srcFileName = "test01.txt";
        final String tgtFileName = "test01.txt";
        final String srcFileDepotPath = testRoot + "/src/" + srcFileName;
        final String tgtFileDepotPath = testRoot + "/tgt/" + tgtFileName;
        final String changelistDescription = "Changelist generated for p4java junit test " + getTestId();
        IChangelist changelist;
        File tmpFile;

        server = getServer();
        client = getDefaultClient(server);
        assertNotNull("unable to retrieve test client", client);
        server.setCurrentClient(client);
        IBranchSpec branchSpec = server.getBranchSpec(branchSpecName);
        assertNotNull("null branchspec retrieved", branchSpec);
        List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(testRoot + "/...");
        client.revertFiles(fileSpecs, null);
        List<IFileSpec> syncFiles = forceSyncFiles(client, fileSpecs);
        assertNotNull("null return from sync", syncFiles);
        assertEquals("wrong number of files force synced",
                2, syncFiles.size());    // May need adjustment over time...
        changelist = CoreFactory.createChangelist(client, changelistDescription, true);
        assertNotNull("unable to create edit changelist", changelist);
        List<IFileSpec> editFiles = client.editFiles(FileSpecBuilder.makeFileSpecList(srcFileDepotPath),
                new EditFilesOptions().setChangelistId(changelist.getId()));
        assertNotNull("null edit files list", editFiles);
        assertEquals(1, editFiles.size());
        changelist.refresh();
        tmpFile = File.createTempFile(getTestId(), null);

        try (InputStream inStream = new FileInputStream(new File(getSystemPath(client, srcFileDepotPath)));
             PrintStream outStream = new PrintStream(tmpFile)) {
            int severity = 0;
            while (severity < 500) {
                severity = rand.nextInt(5000); // file must be bigger than 5K for this to be effective...
            }
            mangleTextFile(severity, inStream, outStream);
            outStream.flush();
            outStream.close();
            inStream.close();
            this.copyFile(tmpFile.getAbsolutePath(), getSystemPath(client, srcFileDepotPath), true);
            List<IFileSpec> submitFiles = changelist.submit(new SubmitOptions());
            List<IFileSpec> validSpecs = getValidSpecs(submitFiles);
            assertNotNull(submitFiles);
            assertEquals(2, validSpecs.size());
            assertEquals("submit failure: " + validSpecs.get(0).getStatusMessage(),
                    VALID, validSpecs.get(0).getOpStatus());
            changelist = CoreFactory.createChangelist(client, changelistDescription, true);

            List<IFileSpec> copyFiles = client.copyFiles(
                    new FileSpec(srcFileDepotPath),
                    new FileSpec(tgtFileDepotPath),
                    branchSpecName,
                    new CopyFilesOptions().setBidirectional(true));
            assertNotNull(copyFiles);
            assertEquals(1, copyFiles.size());
            assertEquals("copy failure: " + copyFiles.get(0).getStatusMessage(),
                    VALID, copyFiles.get(0).getOpStatus());
            changelist.refresh();

            changelist.refresh();
            submitFiles = changelist.submit(new SubmitOptions());
            assertNotNull(submitFiles);
        } finally {
            deletePendingChangelist(changelist);
            Files.deleteIfExists(tmpFile.toPath());
        }
    }

    private List<IFileSpec> getValidSpecs(List<IFileSpec> specs) {
        List<IFileSpec> validSpecs = specs.stream()
                .filter(spec -> spec.getOpStatus().equals(VALID)
                        || (spec.getOpStatus().equals(INFO) && !isNumeric(spec.getStatusMessage())))
                .collect(Collectors.toList());
        return validSpecs;
    }
}
