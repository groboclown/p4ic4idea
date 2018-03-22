
package com.perforce.p4java.tests.dev.unit.feature.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests text and binary resolves using the stream-based IClient resolveFile method.
 * Adapted from Job040601Test.
 */

@TestId("Client_ResolveFileStreamTest")
public class ResolveFileStreamTest extends P4JavaTestCase {
    private static final String TEST_ROOT = "//depot/client/ResolveFileStreamTest";
    private static IClient client;
    private IChangelist changelist = null;
    private String test02Name;

    @BeforeClass
    public static void beforeEach() throws Exception {
        server = getServer();
        client = getDefaultClient(server);
        assertNotNull(client);
        server.setCurrentClient(client);
    }

    @Test
    public void testBinaryStreamResolve() throws Exception {
        final String test01Name = TEST_ROOT + "/" + "test03.jpg";
        test02Name = TEST_ROOT + "/" + "test04.jpg";
        forceSyncFiles(client, TEST_ROOT + "/...");
        changelist = client.createChangelist(new Changelist(
                IChangelist.UNKNOWN,
                client.getName(),
                getUserName(),
                ChangelistStatus.NEW,
                null,
                "Bugs101_Job040601Test test submit changelist",
                false,
                (Server) server
        ));
        assertNotNull(changelist);
        List<IFileSpec> editList = client.editFiles(FileSpecBuilder.makeFileSpecList(test01Name),
                new EditFilesOptions().setChangelistId(changelist.getId()));
        assertNotNull(editList);
        assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());
        changelist.refresh();
        List<IFileSpec> submitList = changelist.submit(null);
        assertNotNull(submitList);
        changelist = client.createChangelist(new Changelist(
                IChangelist.UNKNOWN,
                client.getName(),
                getUserName(),
                ChangelistStatus.NEW,
                null,
                "Bugs101_Job040601Test test integration changelist",
                false,
                (Server) server
        ));
        List<IFileSpec> integFiles = client.integrateFiles(
                new FileSpec(test01Name),
                new FileSpec(test02Name),
                null,
                new IntegrateFilesOptions().setChangelistId(changelist.getId()));
        assertNotNull(integFiles);
        assertEquals(1, FileSpecBuilder.getValidFileSpecs(integFiles).size());
        String sourcePath = this.getSystemPath(client, test01Name);
        assertNotNull(sourcePath);
        File sourceFile = new File(sourcePath);
        assertTrue(sourceFile.canRead());
        try (FileInputStream sourceStream = new FileInputStream(sourceFile)) {
            IFileSpec resolvedFile = client.resolveFile(new FileSpec(test02Name), sourceStream);

            assertNotNull(resolvedFile);
            assertEquals("edit from", resolvedFile.getHowResolved());
        }
        changelist.refresh();
        List<IFileSpec> subList = changelist.submit(null);
        assertNotNull(subList);
    }

    @Test
    public void testTextStreamResolve() throws Exception {
        final String test01Name = TEST_ROOT + "/" + "test01.txt";
        test02Name = TEST_ROOT + "/" + "test02.txt";

        forceSyncFiles(client, TEST_ROOT + "/...");
        changelist = client.createChangelist(new Changelist(
                IChangelist.UNKNOWN,
                client.getName(),
                getUserName(),
                ChangelistStatus.NEW,
                null,
                "Bugs101_Job040601Test test submit changelist",
                false,
                (Server) server
        ));
        assertNotNull(changelist);
        List<IFileSpec> editList = client.editFiles(FileSpecBuilder.makeFileSpecList(test01Name),
                new EditFilesOptions().setChangelistId(changelist.getId()));
        assertNotNull(editList);
        assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());
        changelist.refresh();
        List<IFileSpec> submitList = changelist.submit(new SubmitOptions());
        assertNotNull(submitList);
        changelist = client.createChangelist(new Changelist(
                IChangelist.UNKNOWN,
                client.getName(),
                getUserName(),
                ChangelistStatus.NEW,
                null,
                "Bugs101_Job040601Test test integration changelist",
                false,
                (Server) server
        ));

        List<IFileSpec> integFiles = client.integrateFiles(
                new FileSpec(test01Name),
                new FileSpec(test02Name),
                null,
                new IntegrateFilesOptions().setChangelistId(changelist.getId()));
        assertNotNull(integFiles);
        assertEquals(1, FileSpecBuilder.getValidFileSpecs(integFiles).size());
        changelist.refresh();
        String sourcePath = this.getSystemPath(client, test01Name);
        assertNotNull(sourcePath);
        File sourceFile = new File(sourcePath);
        assertTrue(sourceFile.canRead());
        try (FileInputStream sourceStream = new FileInputStream(sourceFile)) {
            IFileSpec resolvedFile = client.resolveFile(new FileSpec(test02Name), sourceStream);
            assertNotNull(resolvedFile);
            assertEquals("edit from", resolvedFile.getHowResolved());
        }
        changelist.refresh();
        List<IFileSpec> subList = changelist.submit(new SubmitOptions());
        assertNotNull(subList);
    }

    @After
    public void afterEach() throws Exception {
        if (server != null) {
            if (client != null) {
                if ((changelist != null) && (changelist.getStatus() == ChangelistStatus.PENDING)) {
                    try {
                        client.revertFiles(FileSpecBuilder.makeFileSpecList(test02Name),
                                new RevertFilesOptions().setChangelistId(changelist.getId()));
                        server.deletePendingChangelist(changelist.getId()); // not strictly necessary...
                    } catch (P4JavaException exc) {
                    }
                }
            }
        }
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
