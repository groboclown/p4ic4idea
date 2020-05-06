package com.perforce.p4java.tests.dev.unit.features132;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.ShelveFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.fail;

/**
 * Test submit a shelved changelist without transferring files or modifying the
 * workspace (p4 submit -e shelvedChange#).
 */

@Jobs({ "job066386" })
@TestId("Dev132_SubmitShelvedChangelistTest")
public class SubmitShelvedChangelistTest extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SubmitShelvedChangelistTest.class.getSimpleName());

    public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");
    private IClient client2 = null;

    @Before
    public void setUp() {
        try {
            setupServer(p4d.getRSHURL(), userName, password, true, null);
            client = getClient(server);
            client2 = server.getClient("p4TestUserWS20112");
            assertThat(client2, notNullValue());

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        afterEach(server);
    }

    /**
     * Test submit a shelved changelist without transferring files or modifying
     * the workspace (p4 submit -e shelvedChange#).
     */
    @Test
    public void testSubmitShelvedChangelist() throws Exception {

        IChangelist changelist = null;
        List<IFileSpec> files;

        String dir = "branch" + getRandomInt();

        String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
        String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/" + dir
                + "/MessagesBundle_es.properties";

        try {
            // Copy a file to be used for shelving
            changelist = getNewChangelist(server, client,
                    "Dev132_SubmitShelvedChangelistTest copy files");
            assertThat(changelist, notNullValue());
            changelist = client.createChangelist(changelist);
            files = client.copyFiles(new FileSpec(sourceFile), new FileSpec(targetFile), null,
                    new CopyFilesOptions().setChangelistId(changelist.getId()));
            assertThat(files, notNullValue());
            changelist.refresh();
            files = changelist.submit(new SubmitOptions());
            assertThat(files, notNullValue());

            // Make changes to the file and shelve it
            changelist = getNewChangelist(server, client,
                    "Dev132_SubmitShelvedChangelistTest edit files");
            assertThat(changelist, notNullValue());
            changelist = client.createChangelist(changelist);
            files = client.editFiles(FileSpecBuilder.makeFileSpecList(targetFile),
                    new EditFilesOptions().setChangelistId(changelist.getId()));

            assertThat(files, notNullValue());
            assertThat(files.size(), is(1));
            assertThat(files.get(0), notNullValue());

            String localFilePath = files.get(0).getClientPathString();
            assertThat(localFilePath, notNullValue());

            writeFileBytes(localFilePath, "// some test text." + LINE_SEPARATOR, true);

            changelist.refresh();

            // Shelve the file
            files = client.shelveFiles(FileSpecBuilder.makeFileSpecList(targetFile),
                    changelist.getId(), new ShelveFilesOptions());
            assertThat(files, notNullValue());

            // Revert the opened file
            files = client.revertFiles(changelist.getFiles(true),
                    new RevertFilesOptions().setChangelistId(changelist.getId()));
            assertThat(files, notNullValue());

            // Submit the shelved changelist from client2
            files = client2.submitShelvedChangelist(changelist.getId());
            assertThat(files, notNullValue());
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
                            "Dev132_SubmitShelvedChangelistTest delete submitted files");
                    deleteChangelist = client.createChangelist(deleteChangelist);
                    client.deleteFiles(FileSpecBuilder.makeFileSpecList(targetFile),
                            new DeleteFilesOptions().setChangelistId(deleteChangelist.getId()));
                    deleteChangelist.refresh();
                    deleteChangelist.submit(null);
                } catch (P4JavaException e) {
                    // Can't do much here...
                }
            }
        }
    }
}
