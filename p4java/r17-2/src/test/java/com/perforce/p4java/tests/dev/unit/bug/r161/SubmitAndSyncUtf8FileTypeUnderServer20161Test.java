package com.perforce.p4java.tests.dev.unit.bug.r161;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;


import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;

/**
 * @author Sean Shou
 * @since 18/07/2016
 */
public class SubmitAndSyncUtf8FileTypeUnderServer20161Test extends P4JavaRshTestCase {
    private static final long UTF_8_BOM_SIZE = 3;
    private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";
    private static final String RELATIVE_DEPOT_PATH = "/152Bugs/job086058/" + System.currentTimeMillis();
    private static final String TEST_FILE_PARENT_DEPOT_PATH = "//depot" + RELATIVE_DEPOT_PATH;
    private IChangelist changelist = null;
    private List<IFileSpec> submittedOrPendingFileSpecs = null;
    
    @ClassRule
	public static SimpleServerRule p4d = new UnicodeServerRule("r16.1", SubmitAndSyncUtf8FileTypeUnderServer20161Test.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    }

    @Test
    public void testSubmitFileHasUtf8BomButHasBytesBetween7FTo9FAndItShouldDetectAsText_Server_20161() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/aw30_101x_Rev_1_ui_configurations_ja_JP.xml");
        long originalSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "utf8",
                originalSize - UTF_8_BOM_SIZE,
                originalSize
        );

    }

    private void testSubmitFile(
            final String clientName,
            final File testResourceFile,
            final String expectedFileTypeString,
            final long expectedServerSideFileSize,
            final long expectedSyncedLocalFileSize) throws Exception {

        String depotPath = TEST_FILE_PARENT_DEPOT_PATH + "/" + testResourceFile.getName();
        SubmittingSupplier submittingSupplier = submitFileThatLoadFromClassPath(clientName,
                depotPath,
                RELATIVE_DEPOT_PATH,
                testResourceFile);

        submittedOrPendingFileSpecs = new ArrayList<>();
        submittedOrPendingFileSpecs.add(submittingSupplier.submittedFileSpec());
        changelist = submittingSupplier.changelist();
        Path targetLocalFile = submittingSupplier.targetLocalFile();

        verifyServerSideFileType(depotPath, expectedFileTypeString);
        verifyServerSideFileSize(depotPath, expectedServerSideFileSize);
        verifyFileSizeAfterSyncFromDepot(depotPath, targetLocalFile, expectedSyncedLocalFileSize);
    }

    @After
    public void afterEach() throws Exception {
        revertChangelist(changelist, submittedOrPendingFileSpecs);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
