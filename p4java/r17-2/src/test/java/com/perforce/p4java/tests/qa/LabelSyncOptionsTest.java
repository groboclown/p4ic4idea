/*
NOT TESTING:
.applyRule 
.getOptions
.processFields
.processOptions
.setOptions
*/

package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.option.client.LabelSyncOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.LabelSyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class LabelSyncOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static LabelSyncOptions labelSyncOptions = null;
    private static Valids valids = null;
    private static File testFile = null;
    private static List<IFileSpec> testFileSpecs = null;
    private static List<IFileSpec> getDepotFilesSpecs = null;
    private static ILabel label = null;

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

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        testFileSpecs = h.addFile(server, user, client, testFile.getAbsolutePath(), "LabelSyncOptions", "text");
        h.validateFileSpecs(testFileSpecs);

        getDepotFilesSpecs = makeFileSpecList(testFileSpecs.get(0).getOriginalPathString());
    }


    // OPTIONS SPECS
    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("b:n b:a b:d", OPTIONS_SPECS);
    }


    // CONSTRUCTORS
    @Test
    public void defaultConstructor() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        labelSyncOptions = new LabelSyncOptions(false, false, false);
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }


    // NO UPDATE
    @Test
    public void explicitConstructorNoUpdate() throws Throwable {
        labelSyncOptions = new LabelSyncOptions(true, false, false);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setNoUpdateFalse() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setNoUpdate(false);
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setNoUpdateTrue() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setNoUpdate(true);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void stringConstructorNoUpdate() throws Throwable {
        labelSyncOptions = new LabelSyncOptions("-n");
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        testMethod(false);
    }

    @Test
    public void setImmutableFalseNoUpdate() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        labelSyncOptions.setNoUpdate(true);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        testMethod(false);
    }

    @Test
    public void setImmutableTrueNoUpdate() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        labelSyncOptions.setNoUpdate(true);
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdateGet = true;
        testMethod(false);
    }


    // DELETE FILES
    @Test
    public void explicitConstructorDeleteFiles() throws Throwable {
        labelSyncOptions = new LabelSyncOptions(false, false, true);
        valids = new Valids();
        valids.deleteFilesGet = true;
        valids.deleteFiles = true;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setDeleteFilesFalse() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setDeleteFiles(false);
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setDeleteFilesTrue() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setDeleteFiles(true);
        valids = new Valids();
        valids.deleteFilesGet = true;
        valids.deleteFiles = true;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void stringConstructorDeleteFiles() throws Throwable {
        labelSyncOptions = new LabelSyncOptions("-d");
        valids = new Valids();
        valids.immutable = true;
        valids.deleteFiles = true;
        testMethod(false);
    }

    @Test
    public void setImmutableFalseDeleteFiles() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        labelSyncOptions.setDeleteFiles(true);
        valids = new Valids();
        valids.deleteFilesGet = true;
        valids.deleteFiles = true;
        testMethod(false);
    }

    @Test
    public void setImmutableTrueDeleteFiles() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        labelSyncOptions.setDeleteFiles(true);
        valids = new Valids();
        valids.immutable = true;
        valids.deleteFilesGet = true;
        testMethod(false);
    }


    // ADD FILES
    @Test
    public void explicitConstructorAddFiles() throws Throwable {
        labelSyncOptions = new LabelSyncOptions(false, true, false);
        valids = new Valids();
        valids.addFilesGet = true;
        valids.addFiles = true;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setAddFilesFalse() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setAddFiles(false);
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setAddFilesTrue() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setAddFiles(true);
        valids = new Valids();
        valids.addFilesGet = true;
        valids.addFiles = true;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void stringConstructorAddFiles() throws Throwable {
        labelSyncOptions = new LabelSyncOptions("-a");
        valids = new Valids();
        valids.immutable = true;
        valids.addFiles = true;
        testMethod(false);
    }

    @Test
    public void setImmutableFalseAddFiles() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        labelSyncOptions.setAddFiles(true);
        valids = new Valids();
        valids.addFilesGet = true;
        valids.addFiles = true;
        testMethod(false);
    }

    @Test
    public void setImmutableTrueAddFiles() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        labelSyncOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        labelSyncOptions.setAddFiles(true);
        valids = new Valids();
        valids.immutable = true;
        valids.addFilesGet = true;
        testMethod(false);
    }


    // SETTER RETURNS
    @Test
    public void setterReturns() throws Throwable {
        labelSyncOptions = new LabelSyncOptions();
        assertEquals(LabelSyncOptions.class, labelSyncOptions.setNoUpdate(true).getClass());
        assertEquals(LabelSyncOptions.class, labelSyncOptions.setDeleteFiles(true).getClass());
        assertEquals(LabelSyncOptions.class, labelSyncOptions.setAddFiles(true).getClass());
    }


    // OVERRIDE STRING CONSTRUCTOR
    @Test
    public void overrideStringConstructor() throws Throwable {
        labelSyncOptions = new LabelSyncOptions("-n", "-d", "-a");
        labelSyncOptions.setNoUpdate(false);
        labelSyncOptions.setDeleteFiles(false);
        labelSyncOptions.setAddFiles(false);
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        valids.addFiles = true;
        valids.deleteFiles = true;
        testMethod(false);
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

    private static void testMethod(boolean useOldMethod) throws Throwable {

        assertEquals(valids.immutable, labelSyncOptions.isImmutable());
        assertEquals(valids.noUpdateGet, labelSyncOptions.isNoUpdate());
        assertEquals(valids.addFilesGet, labelSyncOptions.isAddFiles());
        assertEquals(valids.deleteFilesGet, labelSyncOptions.isDeleteFiles());

        if (label != null) {

            server.deleteLabel(label.getName(), true);

        }
        label = h.addLabel(server, user, "label1", "//depot/...");
        getDepotFilesSpecs.get(0).setLabel(label.getName());

        if (useOldMethod) {

            client.labelSync(testFileSpecs, label.getName(), labelSyncOptions.isNoUpdate(), labelSyncOptions.isAddFiles(), labelSyncOptions.isDeleteFiles());

        } else {

            client.labelSync(testFileSpecs, label.getName(), labelSyncOptions);

        }

        List<IFileSpec> depotFileSpecs = server.getDepotFiles(getDepotFilesSpecs, false);

        if (valids.noUpdate || valids.deleteFiles) {

            assertTrue(depotFileSpecs.get(0).getStatusMessage().contains("- no such file(s)."));

        } else {

            assertEquals(testFileSpecs.get(0).getOriginalPathString(), depotFileSpecs.get(0).getDepotPathString());
            assertEquals(1, depotFileSpecs.get(0).getEndRevision());

        }

    }

    @Ignore
    private static class Valids {

        private boolean immutable = false;
        private boolean noUpdateGet = false;
        private boolean noUpdate = false;
        private boolean addFilesGet = false;
        @SuppressWarnings("unused")
        private boolean addFiles = false;
        private boolean deleteFilesGet = false;
        private boolean deleteFiles = false;

    }

}