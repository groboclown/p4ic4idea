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
import static com.perforce.p4java.option.client.ShelveFilesOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
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
import com.perforce.p4java.option.client.ShelveFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class ShelveFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static IChangelist pendingChangelist = null;
    private static List<IFileSpec> unshelvedFile = null;
    private static List<IFileSpec> shelvedFile = null;
    private static List<IFileSpec> changedFile = null;
    private static String changedFilePath = null;

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

        unshelvedFile = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "unshelved.txt", "ShelveFilesOptions", "text");
        h.validateFileSpecs(client.editFiles(unshelvedFile, new EditFilesOptions().setChangelistId(pendingChangelist.getId())));

        shelvedFile = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "shelved.txt", "ShelveFilesOptions", "text");
        h.validateFileSpecs(client.editFiles(shelvedFile, new EditFilesOptions().setChangelistId(pendingChangelist.getId())));
        h.validateFileSpecs(client.shelveFiles(shelvedFile, pendingChangelist.getId(), null));

        changedFilePath = client.getRoot() + FILE_SEP + "changed.txt";
        changedFile = h.addFile(server, user, client, changedFilePath, "ShelveFilesOptions", "text");
    }

    @Before
    public void before() throws Throwable {

        // Make sure unshelved file is not shelved
        client.shelveFiles(unshelvedFile, pendingChangelist.getId(), new ShelveFilesOptions().setDeleteFiles(true));
        assertFalse(h.getExtendedFileSpecs(server, unshelvedFile, pendingChangelist.getId()).get(0).isShelved());

        // Make sure shelved file is shelved
        client.shelveFiles(shelvedFile, pendingChangelist.getId(), null);
        assertTrue(h.getExtendedFileSpecs(server, shelvedFile, pendingChangelist.getId()).get(0).isShelved());

        // Make sure changed file is shelved and changed
        client.revertFiles(changedFile, null);
        client.shelveFiles(changedFile, pendingChangelist.getId(), new ShelveFilesOptions().setDeleteFiles(true));
        assertFalse(h.getExtendedFileSpecs(server, changedFile, pendingChangelist.getId()).get(0).isShelved());
        h.validateFileSpecs(client.editFiles(changedFile, new EditFilesOptions().setChangelistId(pendingChangelist.getId())));
        h.validateFileSpecs(client.shelveFiles(changedFile, pendingChangelist.getId(), null));
        h.createFile(changedFilePath, "Edited");

    }

    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("b:f b:r b:d b:p", OPTIONS_SPECS);
    }

    @Test
    public void gettersDefaultConstructor() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions();
        assertEquals(false, shelveFilesOptions.isDeleteFiles());
        assertEquals(false, shelveFilesOptions.isForceShelve());
        assertEquals(false, shelveFilesOptions.isReplaceFiles());
    }

    @Test
    public void gettersExplicitConstructor() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions(true, true, true, true);
        assertEquals(true, shelveFilesOptions.isDeleteFiles());
        assertEquals(true, shelveFilesOptions.isForceShelve());
        assertEquals(true, shelveFilesOptions.isReplaceFiles());
    }

    @Test
    public void gettersAfterSetters() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions();
        shelveFilesOptions.setDeleteFiles(true);
        shelveFilesOptions.setForceShelve(true);
        shelveFilesOptions.setReplaceFiles(true);
        assertEquals(true, shelveFilesOptions.isDeleteFiles());
        assertEquals(true, shelveFilesOptions.isForceShelve());
        assertEquals(true, shelveFilesOptions.isReplaceFiles());
    }

    @Test
    public void shelveFileNullOptionsObject() throws Throwable {
        client.shelveFiles(unshelvedFile, pendingChangelist.getId(), null);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unshelvedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertTrue(extendedFileSpecs.get(0).isShelved());
    }

    @Test
    public void shelveFileDefaultConstructor() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions();

        client.shelveFiles(unshelvedFile, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, unshelvedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertTrue(extendedFileSpecs.get(0).isShelved());
    }

    @Test
    public void deleteShelvedFileExplicitConstructor() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions(false, false, true, false);

        client.shelveFiles(shelvedFile, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, shelvedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertFalse(extendedFileSpecs.get(0).isShelved());
    }

    @Test
    public void deleteShelvedFileSetter() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions();
        shelveFilesOptions.setDeleteFiles(true);

        client.shelveFiles(shelvedFile, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, shelvedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertFalse(extendedFileSpecs.get(0).isShelved());
    }

    @Test
    public void deleteShelvedFileStringConstructor() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions("-d");

        client.shelveFiles(shelvedFile, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, shelvedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertFalse(extendedFileSpecs.get(0).isShelved());
    }

    @Test
    public void replaceFilesExplicitConstructor() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions(false, true, false, false);

        client.shelveFiles(null, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, changedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertTrue(extendedFileSpecs.get(0).isShelved());

        client.revertFiles(changedFile, null);

        client.unshelveFiles(changedFile, pendingChangelist.getId(), pendingChangelist.getId(), null);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(changedFilePath));
        assertEquals("Edited", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void replaceFilesSetter() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions();
        shelveFilesOptions.setReplaceFiles(true);

        client.shelveFiles(null, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, changedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertTrue(extendedFileSpecs.get(0).isShelved());

        client.revertFiles(changedFile, null);

        client.unshelveFiles(changedFile, pendingChangelist.getId(), pendingChangelist.getId(), null);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(changedFilePath));
        assertEquals("Edited", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void replaceFilesStringConstructor() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions("-r");

        client.shelveFiles(null, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, changedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertTrue(extendedFileSpecs.get(0).isShelved());

        client.revertFiles(changedFile, null);

        client.unshelveFiles(changedFile, pendingChangelist.getId(), pendingChangelist.getId(), null);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(changedFilePath));
        assertEquals("Edited", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void forceShelveExplicitConstructor() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions(true, false, false, false);

        client.shelveFiles(changedFile, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, changedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertTrue(extendedFileSpecs.get(0).isShelved());

        client.revertFiles(changedFile, null);

        client.unshelveFiles(changedFile, pendingChangelist.getId(), pendingChangelist.getId(), null);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(changedFilePath));
        assertEquals("Edited", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void forceShelveSetter() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions();
        shelveFilesOptions.setForceShelve(true);

        client.shelveFiles(changedFile, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, changedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertTrue(extendedFileSpecs.get(0).isShelved());

        client.revertFiles(changedFile, null);

        client.unshelveFiles(changedFile, pendingChangelist.getId(), pendingChangelist.getId(), null);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(changedFilePath));
        assertEquals("Edited", bufferedReader.readLine());
        bufferedReader.close();
    }

    @Test
    public void forceShelveStringConstructor() throws Throwable {
        ShelveFilesOptions shelveFilesOptions = new ShelveFilesOptions("-f");

        client.shelveFiles(changedFile, pendingChangelist.getId(), shelveFilesOptions);

        List<IExtendedFileSpec> extendedFileSpecs = h.getExtendedFileSpecs(server, changedFile, pendingChangelist.getId());

        assertEquals(1, extendedFileSpecs.size());
        assertEquals(VALID, extendedFileSpecs.get(0).getOpStatus());
        assertTrue(extendedFileSpecs.get(0).isShelved());
        assertEquals(pendingChangelist.getId(), extendedFileSpecs.get(0).getChangelistId());

        client.revertFiles(changedFile, null);

        client.unshelveFiles(changedFile, pendingChangelist.getId(), pendingChangelist.getId(), null);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(changedFilePath));
        assertEquals("Edited", bufferedReader.readLine());
        bufferedReader.close();
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

}