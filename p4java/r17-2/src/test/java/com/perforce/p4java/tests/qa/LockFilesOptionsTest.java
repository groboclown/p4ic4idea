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
import static com.perforce.p4java.option.client.LockFilesOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.LockFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class LockFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static IChangelist pendingChangelist = null;
    private static LockFilesOptions lockFilesOptions = null;
    private static Valids valids = null;
    private static List<IFileSpec> defaultFileSpecs = null;
    private static List<IFileSpec> pendingFileSpecs = null;

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

        defaultFileSpecs = h.createFile(client.getRoot() + FILE_SEP + "default.txt", "LockFilesOptions Default");
        h.validateFileSpecs(defaultFileSpecs);
        List<IFileSpec> addedDefaultFileSpecs = client.addFiles(defaultFileSpecs, new AddFilesOptions().setFileType("text"));
        h.validateFileSpecs(addedDefaultFileSpecs);

        pendingFileSpecs = h.createFile(client.getRoot() + FILE_SEP + "pending.txt", "LockFilesOptions Pending");
        h.validateFileSpecs(pendingFileSpecs);
        pendingChangelist = h.createChangelist(server, user, client);
        List<IFileSpec> addedPendingFileSpecs = client.addFiles(pendingFileSpecs, new AddFilesOptions().setChangelistId(pendingChangelist.getId()).setFileType("text"));
        h.validateFileSpecs(addedPendingFileSpecs);
        pendingChangelist.update();
    }


    // OPTIONS SPECS
    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("i:c:cl", OPTIONS_SPECS);
    }


    // CONSTRUCTORS
    @Test
    public void defaultConstructor() throws Throwable {
        lockFilesOptions = new LockFilesOptions();
        valids = new Valids();
        testMethod(null, true);
        testMethod(defaultFileSpecs, true);
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        lockFilesOptions = new LockFilesOptions(UNKNOWN);
        valids = new Valids();
        testMethod(null, true);
        testMethod(defaultFileSpecs, true);
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }


    // CHANGELIST
    @Test
    public void explicitConstructorChangelistIdDefault() throws Throwable {
        lockFilesOptions = new LockFilesOptions(DEFAULT);
        valids = new Valids();
        valids.changelistIdGet = DEFAULT;
        valids.changelistId = DEFAULT;
        testMethod(null, true);
        testMethod(defaultFileSpecs, true);
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }

    @Test
    public void explicitConstructorChangelistIdPending() throws Throwable {
        lockFilesOptions = new LockFilesOptions(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(null, true);
        testMethod(defaultFileSpecs, true);
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }

    @Test
    public void explicitConstructorChangelistIdNegativeTwo() throws Throwable {
        lockFilesOptions = new LockFilesOptions(-2);
        valids = new Valids();
        valids.changelistIdGet = -2;
        valids.changelistId = -2;
        testMethod(null, true);
        testMethod(defaultFileSpecs, true);
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }

    @Test
    public void setChangelistId() throws Throwable {
        lockFilesOptions = new LockFilesOptions();
        lockFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(null, true);
        testMethod(defaultFileSpecs, true);
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }

    @Test
    public void stringConstructorChangelistId() throws Throwable {
        lockFilesOptions = new LockFilesOptions("-c" + pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistId = pendingChangelist.getId();
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }

    @Test
    public void setImmutableFalseChangelistId() throws Throwable {
        lockFilesOptions = new LockFilesOptions();
        lockFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(null, false);
        lockFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }

    @Test
    public void setImmutableTrueChangelistId() throws Throwable {
        lockFilesOptions = new LockFilesOptions();
        lockFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(null, false);
        lockFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = pendingChangelist.getId();
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }


    // SETTER RETURNS
    @Test
    public void setterReturns() throws Throwable {
        lockFilesOptions = new LockFilesOptions();
        assertEquals(LockFilesOptions.class, lockFilesOptions.setChangelistId(DEFAULT).getClass());
    }


    // OVERRIDE STRING CONSTRUCTOR
    @Test
    public void overrideStringConstructor() throws Throwable {
        lockFilesOptions = new LockFilesOptions("-c" + pendingChangelist.getId());
        lockFilesOptions.setChangelistId(DEFAULT);
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = DEFAULT;
        valids.changelistId = pendingChangelist.getId();
        testMethod(null, false);
        testMethod(pendingFileSpecs, false);
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

    private static void testMethod(List<IFileSpec> fileSpecs, boolean useOldMethod) throws Throwable {

        assertEquals(valids.immutable, lockFilesOptions.isImmutable());
        assertEquals(valids.changelistIdGet, lockFilesOptions.getChangelistId());

        client.unlockFiles(defaultFileSpecs, DEFAULT, true);
        client.unlockFiles(pendingFileSpecs, pendingChangelist.getId(), true);

        if (useOldMethod) {

            client.lockFiles(fileSpecs, lockFilesOptions.getChangelistId());

        } else {

            client.lockFiles(fileSpecs, lockFilesOptions);

        }

        List<IFileSpec> openedDefaultFileSpecs = client.openedFiles(defaultFileSpecs, new OpenedFilesOptions());
        List<IFileSpec> openedPendingFileSpecs = client.openedFiles(pendingFileSpecs, new OpenedFilesOptions());


        if (fileSpecs == null) {

            if (valids.changelistId == pendingChangelist.getId()) {

                assertFalse(openedDefaultFileSpecs.get(0).isLocked());
                assertTrue(openedPendingFileSpecs.get(0).isLocked());

            }
            if (valids.changelistId == DEFAULT) {

                assertTrue(openedDefaultFileSpecs.get(0).isLocked());
                assertFalse(openedPendingFileSpecs.get(0).isLocked());

            }
            if (valids.changelistId < DEFAULT) {

                assertTrue(openedDefaultFileSpecs.get(0).isLocked());
                assertTrue(openedPendingFileSpecs.get(0).isLocked());

            }

        } else {

            if (fileSpecs.equals(defaultFileSpecs)) {

                if (valids.changelistId == pendingChangelist.getId()) {

                    assertFalse(openedDefaultFileSpecs.get(0).isLocked());
                    assertFalse(openedPendingFileSpecs.get(0).isLocked());

                } else {

                    assertTrue(openedDefaultFileSpecs.get(0).isLocked());
                    assertFalse(openedPendingFileSpecs.get(0).isLocked());

                }

            }
            if (fileSpecs.equals(pendingFileSpecs)) {

                if (valids.changelistId == DEFAULT) {

                    assertFalse(openedDefaultFileSpecs.get(0).isLocked());
                    assertFalse(openedPendingFileSpecs.get(0).isLocked());

                } else {

                    assertFalse(openedDefaultFileSpecs.get(0).isLocked());
                    assertTrue(openedPendingFileSpecs.get(0).isLocked());

                }

            }
        }

    }

    @Ignore
    private static class Valids {

        private boolean immutable = false;
        private int changelistIdGet = UNKNOWN;
        private int changelistId = UNKNOWN;

    }

}