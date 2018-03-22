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
import static com.perforce.p4java.option.client.UnlockFilesOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import com.perforce.p4java.option.client.LockFilesOptions;
import com.perforce.p4java.option.client.UnlockFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class UnlockFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IUser user2 = null;
    private static IClient client = null;
    private static List<IFileSpec> defaultFile = null;
    private static IChangelist pendingChangelist = null;
    private static List<IFileSpec> pendingFile = null;
    private static List<IFileSpec> user2File = null;
    private static List<IFileSpec> allFiles = null;
    private static UnlockFilesOptions unlockFilesOptions = null;
    private static Valids valids = null;

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

        allFiles = new ArrayList<IFileSpec>();

        defaultFile = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "default.txt", "UnlockFilesOptions", "text");
        allFiles.addAll(defaultFile);

        h.validateFileSpecs(client.editFiles(defaultFile, new EditFilesOptions()));

        pendingChangelist = h.createChangelist(server, user, client);

        pendingFile = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "pending.txt", "UnlockFilesOptions", "text");
        allFiles.addAll(pendingFile);

        h.validateFileSpecs(client.editFiles(pendingFile, new EditFilesOptions().setChangelistId(pendingChangelist.getId())));

        user2 = h.createUser(server, "user2");
        server.setUserName(user2.getLoginName());

        user2File = h.addFile(server, user2, client, client.getRoot() + FILE_SEP + "user2.txt", "UnlockFilesOptions", "text");
        allFiles.addAll(user2File);

        h.validateFileSpecs(client.editFiles(user2File, new EditFilesOptions()));

        server.setUserName(user.getLoginName());
    }


    // OPTIONS SPECS
    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("i:c:cl b:f", OPTIONS_SPECS);
    }


    // DEFAULTS
    @Test
    public void defaultConstructor() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions();
        valids = new Valids();
        valids.unlockedFiles.addAll(defaultFile);
        valids.unlockedFiles.addAll(pendingFile);
        testMethod(true);
        assertFilesUnlocked(valids.unlockedFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions(UNKNOWN, false);
        valids = new Valids();
        valids.unlockedFiles.addAll(defaultFile);
        valids.unlockedFiles.addAll(pendingFile);
        testMethod(true);
        assertFilesUnlocked(valids.unlockedFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }


    // CHANGELIST ID
    @Test
    public void explicitConstructorChangelistId() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions(pendingChangelist.getId(), false);
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.unlockedFiles.addAll(pendingFile);
        testMethod(true);
        assertFilesUnlocked(valids.unlockedFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void setChangelistIdPending() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions();
        unlockFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.unlockedFiles.addAll(pendingFile);
        testMethod(true);
        assertFilesUnlocked(valids.unlockedFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void setChangelistIdDefault() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions();
        unlockFilesOptions.setChangelistId(DEFAULT);
        valids = new Valids();
        valids.changelistIdGet = DEFAULT;
        valids.unlockedFiles.addAll(defaultFile);
        testMethod(true);
        assertFilesUnlocked(valids.unlockedFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void stringConstructorChangelistId() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions("-c" + pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.unlockedFiles.addAll(pendingFile);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void setImmutableFalseChangelistId() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions();
        unlockFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        unlockFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.unlockedFiles.addAll(pendingFile);
        testMethod(true);
        assertFilesUnlocked(valids.unlockedFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void setImmutableTrueChangelistId() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions();
        unlockFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        unlockFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = pendingChangelist.getId();
        valids.unlockedFiles.addAll(defaultFile);
        valids.unlockedFiles.addAll(pendingFile);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }


    // FORCE UNLOCK
    @Test
    public void explicitConstructorForceUnlock() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions(UNKNOWN, true);
        valids = new Valids();
        valids.forceUnlock = true;
        valids.unlockedFiles.addAll(allFiles);
        testMethod(true);
        assertFilesUnlocked(valids.unlockedFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void setForceUnlock() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions();
        unlockFilesOptions.setForceUnlock(true);
        valids = new Valids();
        valids.forceUnlock = true;
        valids.unlockedFiles.addAll(allFiles);
        testMethod(true);
        assertFilesUnlocked(valids.unlockedFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void stringConstructorForceUnlock() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions("-f");
        valids = new Valids();
        valids.immutable = true;
        valids.unlockedFiles.addAll(allFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void setImmutableFalseForceUnlock() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions();
        unlockFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        unlockFilesOptions.setForceUnlock(true);
        valids = new Valids();
        valids.forceUnlock = true;
        valids.unlockedFiles.addAll(allFiles);
        testMethod(true);
        assertFilesUnlocked(valids.unlockedFiles);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }

    @Test
    public void setImmutableTrueForceUnlock() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions();
        unlockFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        unlockFilesOptions.setForceUnlock(true);
        valids = new Valids();
        valids.immutable = true;
        valids.forceUnlock = true;
        valids.unlockedFiles.addAll(defaultFile);
        valids.unlockedFiles.addAll(pendingFile);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }


    // SETTER RETURNS
    @Test
    public void setterReturns() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions();
        assertEquals(UnlockFilesOptions.class, unlockFilesOptions.setChangelistId(DEFAULT).getClass());
        assertEquals(UnlockFilesOptions.class, unlockFilesOptions.setForceUnlock(true).getClass());
    }


    // OVERRIDE STRING CONSTRUCTOR
    @Test
    public void overrideStringConstructor() throws Throwable {
        unlockFilesOptions = new UnlockFilesOptions("-c" + pendingChangelist.getId());
        unlockFilesOptions.setChangelistId(DEFAULT);
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = DEFAULT;
        valids.unlockedFiles.addAll(pendingFile);
        testMethod(false);
        assertFilesUnlocked(valids.unlockedFiles);
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

    private static void testMethod(boolean useOldMethod) throws Throwable {

        assertEquals(valids.immutable, unlockFilesOptions.isImmutable());
        assertEquals(valids.changelistIdGet, unlockFilesOptions.getChangelistId());
        assertEquals(valids.forceUnlock, unlockFilesOptions.isForceUnlock());

        List<IFileSpec> lockedFiles = client.lockFiles(defaultFile, new LockFilesOptions());
        h.validateFileSpecs(lockedFiles);

        lockedFiles = client.lockFiles(pendingFile, new LockFilesOptions());
        h.validateFileSpecs(lockedFiles);

        server.setUserName(user2.getLoginName());

        lockedFiles = client.lockFiles(user2File, new LockFilesOptions());
        h.validateFileSpecs(lockedFiles);

        server.setUserName(user.getLoginName());

        List<IFileSpec> openedFiles = client.openedFiles(allFiles, new OpenedFilesOptions());
        h.validateFileSpecs(openedFiles);

        for (IFileSpec openedFile : openedFiles) {

            assertTrue(openedFile.isLocked());

        }

        if (useOldMethod) {

            client.unlockFiles(allFiles, unlockFilesOptions.getChangelistId(), unlockFilesOptions.isForceUnlock());

        } else {

            client.unlockFiles(allFiles, unlockFilesOptions);

        }

    }

    private static void assertFilesUnlocked(List<IFileSpec> unlockFiles) throws Throwable {

        List<IFileSpec> openedFiles = client.openedFiles(allFiles, new OpenedFilesOptions());
        h.validateFileSpecs(openedFiles);

        for (IFileSpec openedFile : openedFiles) {

            boolean shouldBeUnlocked = false;

            for (IFileSpec unlockFile : unlockFiles) {

                if (unlockFile.getOriginalPathString().equals(openedFile.getDepotPathString())) {

                    shouldBeUnlocked = true;
                    break;

                }

            }


            if (shouldBeUnlocked) {

                assertFalse(openedFile.isLocked());

            } else {

                assertTrue(openedFile.isLocked());

            }

        }

    }

    @Ignore
    private static class Valids {

        private boolean immutable = false;
        private int changelistIdGet = UNKNOWN;
        private boolean forceUnlock = false;
        private List<IFileSpec> unlockedFiles = new ArrayList<IFileSpec>();

    }

}