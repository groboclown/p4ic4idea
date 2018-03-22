/*
NOT TESTING:
.applyRule 
.getOptions
.processFields
.processOptions
.setOptions
Can't test old method with string constructor
Can't test old method with immutability true
*/

package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.core.IChangelist.DEFAULT;
import static com.perforce.p4java.core.IChangelist.UNKNOWN;
import static com.perforce.p4java.option.client.ReopenFilesOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;

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
import com.perforce.p4java.option.client.ReopenFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class ReopenFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static IChangelist pendingChangelist = null;
    private static ReopenFilesOptions reopenFilesOptions = null;
    private static Valids valids = null;
    private static List<IFileSpec> testFiles = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();
        String user1Name = ts.getUser();

        server = h.getServer(ts);
        server.setUserName(user1Name);
        server.connect();

        user = server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        testFiles = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "foo.txt", "ReopenFilesOptions", "text");

        pendingChangelist = h.createChangelist(server, user, client);
    }


    // OPTIONS SPECS
    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("i:c:cl s:t s:Q", OPTIONS_SPECS);
    }


    // DEFAULTS
    @Test
    public void defaultConstructor() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions();
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions(DEFAULT, null);
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }


    // CHANGELIST
    @Test
    public void explicitConstructorChangelistIdDefault() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions(DEFAULT, null);
        valids = new Valids();
        valids.changelistIdGet = DEFAULT;
        valids.changelistId = DEFAULT;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void explicitConstructorChangelistIdPending() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions(pendingChangelist.getId(), null);
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(true);
        testMethod(false);
    }


    @Test
    public void setChangelistId() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions();
        reopenFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void stringConstructorChangelistId() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions("-c" + pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistId = pendingChangelist.getId();
        testMethod(false);
    }

    @Test
    public void setImmutableFalseChangelistId() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions();
        reopenFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        reopenFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setImmutableTrueChangelistId() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions();
        reopenFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        reopenFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = pendingChangelist.getId();
        testMethod(false);
    }


    // FILE TYPE
    @Test
    public void explicitConstructorFileType() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions(DEFAULT, "binary");
        valids = new Valids();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        testMethod(true);
        testMethod(false);
    }


    @Test
    public void setFileType() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions();
        reopenFilesOptions.setFileType("binary");
        valids = new Valids();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void stringConstructorFileType() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions("-tbinary");
        valids = new Valids();
        valids.immutable = true;
        valids.fileType = "binary";
        testMethod(false);
    }

    @Test
    public void setImmutableFalseFileType() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions();
        reopenFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        reopenFilesOptions.setFileType("binary");
        valids = new Valids();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setImmutableTrueFileType() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions();
        reopenFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        reopenFilesOptions.setFileType("binary");
        valids = new Valids();
        valids.immutable = true;
        valids.fileTypeGet = "binary";
        testMethod(false);
    }


    // ALL
    @Test
    public void explicitConstructorAll() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions(DEFAULT, "binary");
        valids = new Valids();
        valids.changelistIdGet = DEFAULT;
        valids.changelistId = DEFAULT;
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        testMethod(true);
        testMethod(false);
    }


    // SETTER RETURNS
    @Test
    public void setterReturns() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions();
        assertEquals(ReopenFilesOptions.class, reopenFilesOptions.setChangelistId(DEFAULT).getClass());
        assertEquals(ReopenFilesOptions.class, reopenFilesOptions.setFileType("asdf").getClass());
    }


    // OVERRIDE STRING CONSTRUCTOR
    @Test
    public void overrideStringConstructor() throws Throwable {
        reopenFilesOptions = new ReopenFilesOptions("-c" + pendingChangelist.getId(), "-tbinary");
        reopenFilesOptions.setChangelistId(DEFAULT);
        reopenFilesOptions.setFileType("text");
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = DEFAULT;
        valids.changelistId = pendingChangelist.getId();
        valids.fileTypeGet = "text";
        valids.fileType = "binary";
        testMethod(false);
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

    private static void testMethod(boolean useOldMethod) throws Throwable {

        assertEquals(valids.immutable, reopenFilesOptions.isImmutable());
        assertEquals(valids.changelistIdGet, reopenFilesOptions.getChangelistId());
        assertEquals(valids.fileTypeGet, reopenFilesOptions.getFileType());

        client.revertFiles(testFiles, false, UNKNOWN, false, false);
        client.editFiles(testFiles, new EditFilesOptions());

        List<IFileSpec> beforeOpenedFiles = client.openedFiles(testFiles, new OpenedFilesOptions());

        h.validateFileSpecs(beforeOpenedFiles);

        assertEquals(DEFAULT, beforeOpenedFiles.get(0).getChangelistId());
        assertEquals("text", beforeOpenedFiles.get(0).getFileType());

        List<IFileSpec> reopenedFiles = null;

        if (useOldMethod) {

            reopenedFiles = client.reopenFiles(testFiles, reopenFilesOptions.getChangelistId(), reopenFilesOptions.getFileType());

        } else {

            reopenedFiles = client.reopenFiles(testFiles, reopenFilesOptions);

        }

        h.validateFileSpecs(reopenedFiles);

        List<IFileSpec> afterOpenedFiles = client.openedFiles(testFiles, new OpenedFilesOptions());

        h.validateFileSpecs(afterOpenedFiles);

        assertEquals(valids.changelistId, afterOpenedFiles.get(0).getChangelistId());
        assertEquals(valids.fileType, afterOpenedFiles.get(0).getFileType());

    }

    @Ignore
    private static class Valids {

        private boolean immutable = false;
        private int changelistIdGet = DEFAULT;
        private int changelistId = DEFAULT;
        private String fileTypeGet = null;
        private String fileType = "text";

    }

}