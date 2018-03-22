/*
NOT TESTING:
.applyRule 
.getOptions
.processFields
.processOptions
.setOptions
*/

package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.core.IChangelist.DEFAULT;
import static com.perforce.p4java.core.IChangelist.UNKNOWN;
import static com.perforce.p4java.option.client.RevertFilesOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class RevertFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static List<IFileSpec> defaultFile = null;
    private static IChangelist pendingChangelist = null;
    private static List<IFileSpec> pendingFile = null;
    private static String changedFilePath = null;
    private static List<IFileSpec> changedFile = null;
    private static List<IFileSpec> allFiles = null;
    private static RevertFilesOptions revertFilesOptions = null;
    private static Valids valids = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        allFiles = new ArrayList<IFileSpec>();

        defaultFile = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "default.txt", "RevertFilesOptions", "text");
        allFiles.addAll(defaultFile);

        pendingFile = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "pending.txt", "RevertFilesOptions", "text");
        allFiles.addAll(pendingFile);

        pendingChangelist = h.createChangelist(server, user, client);

        changedFilePath = client.getRoot() + FILE_SEP + "changed.txt";
        changedFile = h.addFile(server, user, client, changedFilePath, "RevertFilesOptions", "text");
        allFiles.addAll(changedFile);
    }


    // OPTIONS SPECS
    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("b:n i:c:cl b:a b:k b:w", OPTIONS_SPECS);
    }


    // DEFAULTS
    @Test
    public void defaultConstructor() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        valids = new Valids();
        valids.revertedFiles.addAll(allFiles);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        revertFilesOptions = new RevertFilesOptions(false, UNKNOWN, false, false);
        valids = new Valids();
        valids.revertedFiles.addAll(allFiles);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }


    // CHANGELIST ID
    @Test
    public void explicitConstructorChangelistId() throws Throwable {
        revertFilesOptions = new RevertFilesOptions(false, pendingChangelist.getId(), false, false);
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.revertedFiles.addAll(pendingFile);
        valids.revertedFiles.addAll(changedFile);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setChangelistIdPending() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.revertedFiles.addAll(pendingFile);
        valids.revertedFiles.addAll(changedFile);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setChangelistIdDefault() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setChangelistId(DEFAULT);
        valids = new Valids();
        valids.changelistIdGet = DEFAULT;
        valids.revertedFiles.addAll(defaultFile);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void stringConstructorChangelistId() throws Throwable {
        revertFilesOptions = new RevertFilesOptions("-c" + pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.revertedFiles.addAll(pendingFile);
        valids.revertedFiles.addAll(changedFile);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setImmutableFalseChangelistId() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        revertFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.revertedFiles.addAll(pendingFile);
        valids.revertedFiles.addAll(changedFile);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setImmutableTrueChangelistId() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        revertFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = pendingChangelist.getId();
        valids.revertedFiles.addAll(allFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }


    // NO CLIENT REFRESH
    @Test
    public void explicitConstructorNoClientRefresh() throws Throwable {
        revertFilesOptions = new RevertFilesOptions(false, UNKNOWN, false, true);
        valids = new Valids();
        valids.noClientRefreshGet = true;
        valids.revertedFiles.addAll(allFiles);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        assertFileUnchanged(changedFilePath);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
        assertFileUnchanged(changedFilePath);
    }

    @Test
    public void setNoClientRefresh() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setNoClientRefresh(true);
        valids = new Valids();
        valids.noClientRefreshGet = true;
        valids.revertedFiles.addAll(allFiles);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        assertFileUnchanged(changedFilePath);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
        assertFileUnchanged(changedFilePath);
    }

    @Test
    public void stringConstructorNoClientRefresh() throws Throwable {
        revertFilesOptions = new RevertFilesOptions("-k");
        valids = new Valids();
        valids.immutable = true;
        valids.revertedFiles.addAll(allFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
        assertFileUnchanged(changedFilePath);
    }

    @Test
    public void setImmutableFalseNoClientRefresh() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        revertFilesOptions.setNoClientRefresh(true);
        valids = new Valids();
        valids.noClientRefreshGet = true;
        valids.revertedFiles.addAll(allFiles);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        assertFileUnchanged(changedFilePath);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
        assertFileUnchanged(changedFilePath);
    }

    @Test
    public void setImmutableTrueNoClientRefresh() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        revertFilesOptions.setNoClientRefresh(true);
        valids = new Valids();
        valids.immutable = true;
        valids.noClientRefreshGet = true;
        valids.revertedFiles.addAll(allFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }


    // NO UPDATE
    @Test
    public void explicitConstructorNoUpdate() throws Throwable {
        revertFilesOptions = new RevertFilesOptions(true, UNKNOWN, false, false);
        valids = new Valids();
        valids.noUpdateGet = true;
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setNoUpdate() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.noUpdateGet = true;
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void stringConstructorNoUpdate() throws Throwable {
        revertFilesOptions = new RevertFilesOptions("-n");
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setImmutableFalseNoUpdate() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        revertFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.noUpdateGet = true;
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setImmutableTrueNoUpdate() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        revertFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdateGet = true;
        valids.revertedFiles.addAll(allFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }


    // REVERT ONLY UNCHANGED
    @Test
    public void explicitConstructorRevertOnlyUnchanged() throws Throwable {
        revertFilesOptions = new RevertFilesOptions(false, UNKNOWN, true, false);
        valids = new Valids();
        valids.revertOnlyUnchangedGet = true;
        valids.revertedFiles.addAll(defaultFile);
        valids.revertedFiles.addAll(pendingFile);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setRevertOnlyUnchanged() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setRevertOnlyUnchanged(true);
        valids = new Valids();
        valids.revertOnlyUnchangedGet = true;
        valids.revertedFiles.addAll(defaultFile);
        valids.revertedFiles.addAll(pendingFile);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void stringConstructorRevertOnlyUnchanged() throws Throwable {
        revertFilesOptions = new RevertFilesOptions("-a");
        valids = new Valids();
        valids.immutable = true;
        valids.revertedFiles.addAll(defaultFile);
        valids.revertedFiles.addAll(pendingFile);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setImmutableFalseRevertOnlyUnchanged() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        revertFilesOptions.setRevertOnlyUnchanged(true);
        valids = new Valids();
        valids.revertOnlyUnchangedGet = true;
        valids.revertedFiles.addAll(defaultFile);
        valids.revertedFiles.addAll(pendingFile);
        testMethod(true);
        assertFilesReverted(valids.revertedFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }

    @Test
    public void setImmutableTrueRevertOnlyUnchanged() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        revertFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        revertFilesOptions.setRevertOnlyUnchanged(true);
        valids = new Valids();
        valids.immutable = true;
        valids.revertOnlyUnchangedGet = true;
        valids.revertedFiles.addAll(allFiles);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }


    // SETTER RETURNS
    @Test
    public void setterReturns() throws Throwable {
        revertFilesOptions = new RevertFilesOptions();
        assertEquals(RevertFilesOptions.class, revertFilesOptions.setChangelistId(DEFAULT).getClass());
        assertEquals(RevertFilesOptions.class, revertFilesOptions.setNoClientRefresh(true).getClass());
        assertEquals(RevertFilesOptions.class, revertFilesOptions.setNoUpdate(true).getClass());
        assertEquals(RevertFilesOptions.class, revertFilesOptions.setRevertOnlyUnchanged(true).getClass());
    }


    // OVERRIDE STRING CONSTRUCTOR
    @Test
    public void overrideStringConstructor() throws Throwable {
        revertFilesOptions = new RevertFilesOptions("-c" + pendingChangelist.getId());
        revertFilesOptions.setChangelistId(DEFAULT);
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = DEFAULT;
        valids.revertedFiles.addAll(pendingFile);
        valids.revertedFiles.addAll(changedFile);
        testMethod(false);
        assertFilesReverted(valids.revertedFiles);
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

    private static void testMethod(boolean useOldMethod) throws Throwable {

        assertEquals(valids.immutable, revertFilesOptions.isImmutable());
        assertEquals(valids.changelistIdGet, revertFilesOptions.getChangelistId());
        assertEquals(valids.noClientRefreshGet, revertFilesOptions.isNoClientRefresh());
        assertEquals(valids.noUpdateGet, revertFilesOptions.isNoUpdate());
        assertEquals(valids.revertOnlyUnchangedGet, revertFilesOptions.isRevertOnlyUnchanged());

        List<IFileSpec> editedFiles = null;

        editedFiles = client.editFiles(defaultFile, new EditFilesOptions());
        h.validateFileSpecs(editedFiles);

        editedFiles = client.editFiles(pendingFile, new EditFilesOptions().setChangelistId(pendingChangelist.getId()));
        h.validateFileSpecs(editedFiles);

        h.editFile(changedFilePath, "Edited", pendingChangelist, client);

        assertEquals(allFiles.size(), getOpenedFiles().size());

        if (useOldMethod) {

            client.revertFiles(allFiles, revertFilesOptions.isNoUpdate(), revertFilesOptions.getChangelistId(), revertFilesOptions.isRevertOnlyUnchanged(), revertFilesOptions.isNoClientRefresh());

        } else {

            client.revertFiles(allFiles, revertFilesOptions);

        }

    }

    private static List<IFileSpec> getOpenedFiles() throws Throwable {

        List<IFileSpec> openedFiles = null;

        openedFiles = client.openedFiles(null, new OpenedFilesOptions());

        h.validateFileSpecs(openedFiles);

        return openedFiles;

    }

    private static void assertFilesReverted(List<IFileSpec> revertFiles) throws Throwable {

        List<IFileSpec> openedFiles = getOpenedFiles();

        for (IFileSpec file : allFiles) {

            boolean fileShouldBeReverted = false;

            for (IFileSpec revertFile : revertFiles) {

                if (file.getOriginalPathString().equals(revertFile.getOriginalPathString())) {

                    fileShouldBeReverted = true;

                }
            }

            if (fileShouldBeReverted) {

                for (IFileSpec openedFile : openedFiles) {

                    assertTrue(!openedFile.getDepotPathString().equals(file.getOriginalPathString()));

                }

            } else {

                boolean fileOpened = false;

                for (IFileSpec openedFile : openedFiles) {

                    if (openedFile.getDepotPathString().equals(file.getOriginalPathString())) {

                        fileOpened = true;

                    }

                }

                assertTrue(fileOpened);

            }

        }

    }

    private static void assertFileUnchanged(String filePath) throws Throwable {

        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        assertTrue(reader.readLine().equals("Edited"));

        reader.close();

    }

    @Ignore
    private static class Valids {

        private boolean immutable = false;
        private int changelistIdGet = UNKNOWN;
        private boolean noClientRefreshGet = false;
        private boolean noUpdateGet = false;
        private boolean revertOnlyUnchangedGet = false;
        private List<IFileSpec> revertedFiles = new ArrayList<IFileSpec>();

    }

}