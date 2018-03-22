/*
NOT TESTING:
.applyRule 
.getOptions
.processFields
.processOptions
.setOptions
*/

package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.option.client.UnshelveFilesOptions.OPTIONS_SPECS;
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
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.UnshelveFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class UnshelveFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static IChangelist pendingChangelist = null;
    private static File shelvedFile = null;
    private static List<IFileSpec> shelvedFileSpecs = null;
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

        pendingChangelist = h.createChangelist(server, user, client);

        shelvedFile = new File(client.getRoot() + FILE_SEP + "shelved.txt");
        shelvedFileSpecs = h.addFile(server, user, client, shelvedFile.getAbsolutePath(), "UnshelveFilesOptions", "text");
        h.validateFileSpecs(client.editFiles(shelvedFileSpecs, new EditFilesOptions().setChangelistId(pendingChangelist.getId())));
        h.validateFileSpecs(client.shelveFiles(shelvedFileSpecs, pendingChangelist.getId(), null));

        clobberFile = new File(client.getRoot() + FILE_SEP + "clobber.txt");
        clobberFileSpecs = h.addFile(server, user, client, clobberFile.getAbsolutePath(), "UnshelveFilesOptions", "text");
        h.validateFileSpecs(client.editFiles(clobberFileSpecs, new EditFilesOptions().setChangelistId(pendingChangelist.getId())));
        h.validateFileSpecs(client.shelveFiles(clobberFileSpecs, pendingChangelist.getId(), null));
    }

    @Before
    public void before() throws Throwable {

        // Make sure shelved is not open and does not exist
        client.revertFiles(shelvedFileSpecs, null);
        assertEquals(0, client.openedFiles(shelvedFileSpecs, null).size());
        shelvedFile.delete();
        assertFalse(shelvedFile.exists());

        // Make sure clobber file exists and is writable but not open
        client.revertFiles(clobberFileSpecs, null);
        assertEquals(0, client.openedFiles(clobberFileSpecs, null).size());
        clobberFile.delete();
        h.createFile(clobberFile.getAbsolutePath(), "Edited");
        assertTrue(clobberFile.exists());

    }

    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("b:f b:n", OPTIONS_SPECS);
    }

    @Test
    public void gettersDefaultConstructor() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions();
        assertEquals(false, unshelveFilesOptions.isForceUnshelve());
        assertEquals(false, unshelveFilesOptions.isPreview());
    }

    @Test
    public void gettersExplicitConstructor() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions(true, true);
        assertEquals(true, unshelveFilesOptions.isForceUnshelve());
        assertEquals(true, unshelveFilesOptions.isPreview());
    }

    @Test
    public void gettersAfterSetters() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions();
        unshelveFilesOptions.setForceUnshelve(true);
        unshelveFilesOptions.setPreview(true);
        assertEquals(true, unshelveFilesOptions.isForceUnshelve());
        assertEquals(true, unshelveFilesOptions.isPreview());
    }

    @Test
    public void unshelveFileNullOptionsObject() throws Throwable {
        client.unshelveFiles(shelvedFileSpecs, pendingChangelist.getId(), pendingChangelist.getId(), null);

        assertTrue(shelvedFile.exists());
    }

    @Test
    public void unshelveFileDefaultConstructor() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions();

        client.unshelveFiles(shelvedFileSpecs, pendingChangelist.getId(), pendingChangelist.getId(), unshelveFilesOptions);

        assertTrue(shelvedFile.exists());
    }

    @Test
    public void previewExplicitConstructor() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions(false, true);

        client.unshelveFiles(shelvedFileSpecs, pendingChangelist.getId(), pendingChangelist.getId(), unshelveFilesOptions);

        assertFalse(shelvedFile.exists());
    }

    @Test
    public void previewSetter() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions(false, true);

        client.unshelveFiles(shelvedFileSpecs, pendingChangelist.getId(), pendingChangelist.getId(), unshelveFilesOptions);

        assertFalse(shelvedFile.exists());
    }

    @Test
    public void previewStringConstructor() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions("-n");

        client.unshelveFiles(shelvedFileSpecs, pendingChangelist.getId(), pendingChangelist.getId(), unshelveFilesOptions);

        assertFalse(shelvedFile.exists());
    }

    @Test
    public void forceUnshelveExplicitConstructor() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions(true, false);

        client.unshelveFiles(clobberFileSpecs, pendingChangelist.getId(), pendingChangelist.getId(), unshelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, clobberFileSpecs, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(clobberFile));
        assertEquals("UnshelveFilesOptions", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void forceUnshelveSetter() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions("-f");
        unshelveFilesOptions.setForceUnshelve(true);

        client.unshelveFiles(clobberFileSpecs, pendingChangelist.getId(), pendingChangelist.getId(), unshelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, clobberFileSpecs, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(clobberFile));
        assertEquals("UnshelveFilesOptions", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void forceUnshelveStringConstructor() throws Throwable {
        UnshelveFilesOptions unshelveFilesOptions = new UnshelveFilesOptions("-f");

        client.unshelveFiles(clobberFileSpecs, pendingChangelist.getId(), pendingChangelist.getId(), unshelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, clobberFileSpecs, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(clobberFile));
        assertEquals("UnshelveFilesOptions", bufferedReader.readLine());
        bufferedReader.close();
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }


}