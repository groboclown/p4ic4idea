package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.core.IChangelist.UNKNOWN;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.option.client.SyncOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class SyncOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File unsyncedFile = null;
    private static List<IFileSpec> unsyncedFileSpecs = null;
    private static File clobberFile = null;
    private static List<IFileSpec> clobberFileSpecs = null;

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

        unsyncedFile = new File(client.getRoot() + FILE_SEP + "unsynced.txt");
        unsyncedFileSpecs = h.addFile(server, user, client, unsyncedFile.getAbsolutePath(), "SyncOptions", "text");

        clobberFile = new File(client.getRoot() + FILE_SEP + "clobber.txt");
        clobberFileSpecs = h.addFile(server, user, client, clobberFile.getAbsolutePath(), "SyncOptions", "text");
    }

    @Before
    public void before() throws Throwable {

        // Make sure unsyned file is not synced
        client.sync(makeFileSpecList(unsyncedFile.getAbsolutePath() + "#none"), null);
        unsyncedFile.delete();
        assertFalse(unsyncedFile.exists());
        assertEquals(-1, h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN).get(0).getHaveRev());

        // Make sure clobber file exists and is writable
        clobberFile.delete();
        assertFalse(clobberFile.exists());
        h.createFile(clobberFile.getAbsolutePath(), "Edited");
        assertTrue(clobberFile.exists());

    }

    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("b:f b:n b:k b:p b:q b:s", OPTIONS_SPECS);
    }

    @Test
    public void gettersDefaultConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions();
        assertEquals(false, syncOptions.isClientBypass());
        assertEquals(false, syncOptions.isForceUpdate());
        assertEquals(false, syncOptions.isNoUpdate());
        assertEquals(false, syncOptions.isServerBypass());
    }

    @Test
    public void gettersExplicitConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions(true, true, true, true);
        assertEquals(true, syncOptions.isClientBypass());
        assertEquals(true, syncOptions.isForceUpdate());
        assertEquals(true, syncOptions.isNoUpdate());
        assertEquals(true, syncOptions.isServerBypass());
    }

    @Test
    public void gettersAfterSetters() throws Throwable {
        SyncOptions syncOptions = new SyncOptions();
        syncOptions.setClientBypass(true);
        syncOptions.setForceUpdate(true);
        syncOptions.setNoUpdate(true);
        syncOptions.setServerBypass(true);
        assertEquals(true, syncOptions.isClientBypass());
        assertEquals(true, syncOptions.isForceUpdate());
        assertEquals(true, syncOptions.isNoUpdate());
        assertEquals(true, syncOptions.isServerBypass());
    }

    @Test
    public void syncNullOptionsObject() throws Throwable {
        client.sync(unsyncedFileSpecs, null);

        assertTrue(unsyncedFile.exists());
    }

    @Test
    public void syncDefaults() throws Throwable {
        SyncOptions syncOptions = new SyncOptions();

        client.sync(unsyncedFileSpecs, syncOptions);

        assertTrue(unsyncedFile.exists());
    }

    @Test
    public void syncDefaultsOldMethod() throws Throwable {
        client.sync(unsyncedFileSpecs, false, false, false, false);

        assertTrue(unsyncedFile.exists());
    }

    @Test
    public void clientBypassExplicitConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions(false, false, true, false);

        client.sync(unsyncedFileSpecs, syncOptions);

        assertFalse(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void clientBypassSetter() throws Throwable {
        SyncOptions syncOptions = new SyncOptions();
        syncOptions.setClientBypass(true);

        client.sync(unsyncedFileSpecs, syncOptions);

        assertFalse(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void clientBypassStringConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions("-k");

        client.sync(unsyncedFileSpecs, syncOptions);

        assertFalse(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void clientBypassOldMethod() throws Throwable {
        client.sync(unsyncedFileSpecs, false, false, true, false);

        assertFalse(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void forceUpdateExplicitConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions(true, false, false, false);

        client.sync(clobberFileSpecs, syncOptions);

        assertTrue(clobberFile.exists());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(clobberFile));
        assertEquals("SyncOptions", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void forceUpdateSetter() throws Throwable {
        SyncOptions syncOptions = new SyncOptions();
        syncOptions.setForceUpdate(true);

        client.sync(clobberFileSpecs, syncOptions);

        assertTrue(clobberFile.exists());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(clobberFile));
        assertEquals("SyncOptions", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void forceUpdateStringConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions("-f");

        client.sync(clobberFileSpecs, syncOptions);

        assertTrue(clobberFile.exists());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(clobberFile));
        assertEquals("SyncOptions", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void forceUpdateOldMethod() throws Throwable {
        client.sync(clobberFileSpecs, true, false, false, false);

        assertTrue(clobberFile.exists());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(clobberFile));
        assertEquals("SyncOptions", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void noUpdateExplicitConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions(false, true, false, false);

        client.sync(unsyncedFileSpecs, syncOptions);

        assertFalse(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(-1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void noUpdateSetter() throws Throwable {
        SyncOptions syncOptions = new SyncOptions();
        syncOptions.setNoUpdate(true);

        client.sync(unsyncedFileSpecs, syncOptions);

        assertFalse(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(-1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void noUpdateStringConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions("-n");

        client.sync(unsyncedFileSpecs, syncOptions);

        assertFalse(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(-1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void noUpdateOldMethod() throws Throwable {
        client.sync(unsyncedFileSpecs, false, true, false, false);

        assertFalse(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(-1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void serverBypassExplicitConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions(false, false, false, true);

        client.sync(unsyncedFileSpecs, syncOptions);

        assertTrue(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(-1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void serverBypassSetter() throws Throwable {
        SyncOptions syncOptions = new SyncOptions();
        syncOptions.setServerBypass(true);

        client.sync(unsyncedFileSpecs, syncOptions);

        assertTrue(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(-1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void serverBypassStringConstructor() throws Throwable {
        SyncOptions syncOptions = new SyncOptions("-p");

        client.sync(unsyncedFileSpecs, syncOptions);

        assertTrue(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(-1, extendedFileSpecs.get(0).getHaveRev());
    }

    @Test
    public void serverBypassOldMethod() throws Throwable {
        client.sync(unsyncedFileSpecs, false, false, false, true);

        assertTrue(unsyncedFile.exists());

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unsyncedFileSpecs, UNKNOWN);

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertEquals(-1, extendedFileSpecs.get(0).getHaveRev());
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }


}