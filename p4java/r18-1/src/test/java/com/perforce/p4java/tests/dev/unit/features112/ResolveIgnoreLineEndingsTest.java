/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
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
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test resolve file -dl: Ignore differences in line-ending convention
 */
@Jobs({ "job046102" })
@TestId("Dev112_ResolveIgnoreLineEndingsTest")
public class ResolveIgnoreLineEndingsTest extends P4JavaTestCase {

    IOptionsServer server = null;
    IClient client = null;
    IChangelist changelist = null;
    List<IFileSpec> files = null;

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
            client = server.getClient(getPlatformClientName("p4TestUserWS20112"));
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

    /**
     * Save new client line end in spec and return the setting that was replaced.
     *
     * @param client the client
     * @param newLineEnd the new line end
     * @return the i client summary. client line end
     * @throws ConnectionException the connection exception
     * @throws RequestException the request exception
     * @throws AccessException the access exception
     */
    private IClientSummary.ClientLineEnd saveNewClientLineEndInSpec(IClient client,
            IClientSummary.ClientLineEnd newLineEnd)
            throws ConnectionException, RequestException, AccessException {
        // get the old line ending
        assertNotNull("Client object is null.", client);
        IClientSummary.ClientLineEnd oldLineEnd = client.getLineEnd();
        // set the new line ending
        client.setLineEnd(newLineEnd);
        client.update();
        // check that the changes stuck
        IClientSummary.ClientLineEnd cLineEnd = client.getLineEnd();
        debugPrint("setLineEnd to: " + newLineEnd, "OLD: " + oldLineEnd, "NEW: " + cLineEnd);
        return oldLineEnd;
    }

    /**
     * Test resolve file -dl: Ignore differences in line-ending convention
     */
    @Test
    public void testResolveIgnoreLineEndings() {
        int randNum = getRandomInt();
        String dir = "branch" + randNum;

        // Source and target files for integrate with content changes
        String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_it.properties";
        String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/" + dir
                + "/MessagesBundle_it.properties";
        String targetFile2 = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/" + dir + randNum
                + "/MessagesBundle_it.properties";

        String testTextWindowsLineEnding = "///// added test text ///// - " + randNum + "@\r\n";
        String testTextUnixLineEnding = "///// added test text ///// - " + randNum + "@\n";

        // Error message indicating merges still pending
        String mergesPending = "Merges still pending -- use 'resolve' to merge files.";

        // Info message indicating submitted change
        String submittedChange = "Submitted as change";
        IClientSummary.ClientLineEnd oldLineEnd = null;
        try {
            // Set to shared so that there is no auto conversion of lineendings when trying to 
            // resolve otherwise CRLF vs LF change on the same line in Windows will not be
            // seen as a conflict. Keep the old line end so we can restore it at the end of
            // the test.
            oldLineEnd =
                    saveNewClientLineEndInSpec(client, IClientSummary.ClientLineEnd.SHARE);
            // Copy a file to be used as a target for integrate content changes
            changelist = getNewChangelist(server, client,
                    "Dev112_ResolveIgnoreLineEndingsTest copy files changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);
            assertNotNull(changelist);
            List<IFileSpec> files = client.copyFiles(new FileSpec(sourceFile),
                    new FileSpec(targetFile), null,
                    new CopyFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(files);
            changelist.refresh();
            files = changelist.submit(new SubmitOptions());
            assertNotNull(files);

            // Integrate targetFile to targetFile2
            changelist = getNewChangelist(server, client,
                    "Dev112_ResolveIgnoreLineEndingsTest integrate files changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);
            assertNotNull(changelist);
            List<IFileSpec> integrateFiles = client.integrateFiles(new FileSpec(targetFile),
                    new FileSpec(targetFile2), null,
                    new IntegrateFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(integrateFiles);
            changelist.refresh();
            integrateFiles = changelist.submit(new SubmitOptions());
            assertNotNull(integrateFiles);

            // Edit targetFile
            changelist = getNewChangelist(server, client,
                    "Dev112_ResolveIgnoreLineEndingsTest edit files changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);
            assertNotNull(changelist);
            files = client.editFiles(FileSpecBuilder.makeFileSpecList(targetFile),
                    new EditFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(files);

            // Append some text (Windows line ending) to targetFile
            assertNotNull(files.get(0).getClientPathString());
            writeFileBytes(files.get(0).getClientPathString(), testTextWindowsLineEnding, true);

            // Submit the changes
            changelist.refresh();
            List<IFileSpec> submittedFiles = changelist.submit(new SubmitOptions());
            assertNotNull(submittedFiles);

            // Edit targetFile2
            changelist = getNewChangelist(server, client,
                    "Dev112_ResolveIgnoreLineEndingsTest edit files changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);
            assertNotNull(changelist);
            files = client.editFiles(FileSpecBuilder.makeFileSpecList(targetFile2),
                    new EditFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(files);

            // Append some text (Unix line ending) to targetFile2
            assertNotNull(files.get(0).getClientPathString());
            writeFileBytes(files.get(0).getClientPathString(), testTextUnixLineEnding, true);

            // Submit the changes
            changelist.refresh();
            submittedFiles = changelist.submit(new SubmitOptions());
            assertNotNull(submittedFiles);

            changelist = getNewChangelist(server, client,
                    "Dev112_ResolveIgnoreLineEndingsTest integrate files changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);
            assertNotNull(changelist);

            // Run integrate
            integrateFiles = client.integrateFiles(new FileSpec(targetFile),
                    new FileSpec(targetFile2), null,
                    new IntegrateFilesOptions().setChangelistId(changelist.getId()));

            // Check for null
            assertNotNull(integrateFiles);

            // Check for invalid filespecs
            List<IFileSpec> invalidFiles = FileSpecBuilder.getInvalidFileSpecs(integrateFiles);
            if (invalidFiles.size() != 0) {
                fail(invalidFiles.get(0).getOpStatus() + ": "
                        + invalidFiles.get(0).getStatusMessage());
            }

            // Refresh changelist
            changelist.refresh();

            // Check for correct number of valid filespecs in changelist
            List<IFileSpec> changelistFiles = changelist.getFiles(true);
            assertEquals("Wrong number of filespecs in changelist", 1,
                    FileSpecBuilder.getValidFileSpecs(changelistFiles).size());

            // Validate file action type
            assertEquals(FileAction.INTEGRATE, changelistFiles.get(0).getAction());

            // Run resolve with 'resolve file content changes'
            List<IFileSpec> resolveFiles = client.resolveFilesAuto(integrateFiles,
                    new ResolveFilesAutoOptions().setChangelistId(changelist.getId()));

            // Check for null
            assertNotNull(resolveFiles);

            // Check for correct number of filespecs
            assertEquals(3, resolveFiles.size());

            // Check file operation status info message
            assertEquals(FileSpecOpStatus.INFO, resolveFiles.get(1).getOpStatus());
            assertTrue(resolveFiles.get(1).getStatusMessage()
                    .contains("Diff chunks: 0 yours + 0 theirs + 1 both + 0 conflicting"));

            // Refresh changelist
            changelist.refresh();

            // Submit should succeed as 'shared' line-ending client should have filtered out \r
            List<IFileSpec> submitFiles = changelist.submit(new SubmitOptions());

            // There should be 5 filespecs (triggers)
            assertEquals(5, submitFiles.size());

            // Check the status and file action of the submitted file
            assertEquals(FileSpecOpStatus.VALID, submitFiles.get(3).getOpStatus());
            assertEquals(FileAction.INTEGRATE, submitFiles.get(3).getAction());

            // Check for 'Submitted as change' in the info message
            assertTrue(submitFiles.get(4).getStatusMessage()
                    .contains(submittedChange + " " + changelist.getId()));

            // Make sure the changelist is submitted
            changelist.refresh();
            assertTrue(changelist.getStatus() == ChangelistStatus.SUBMITTED);

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        } finally {
            if (client != null) {
                if (changelist != null) {
                    if (changelist.getStatus() == ChangelistStatus.PENDING) {
                        try {
                            // Revert files in pending changelist
                            client.revertFiles(changelist.getFiles(true),
                                    new RevertFilesOptions().setChangelistId(changelist.getId()));
                        } catch (P4JavaException e) {
                            // Can't do much here...
                        }
                    }
                }
            }
            if (client != null && server != null) {
                try {
                    // Delete submitted test files
                    IChangelist deleteChangelist = getNewChangelist(server, client,
                            "Dev112_ResolveIgnoreLineEndingsTest delete submitted test files changelist");
                    deleteChangelist = client.createChangelist(deleteChangelist);
                    client.deleteFiles(
                            FileSpecBuilder
                                    .makeFileSpecList(new String[] { targetFile, targetFile2 }),
                            new DeleteFilesOptions().setChangelistId(deleteChangelist.getId()));
                    deleteChangelist.refresh();
                    deleteChangelist.submit(null);
                    if (oldLineEnd != null) {
                        saveNewClientLineEndInSpec(client, oldLineEnd);
                    }
                } catch (P4JavaException e) {
                    // Can't do much here...
                }
            }
        }
    }
}
