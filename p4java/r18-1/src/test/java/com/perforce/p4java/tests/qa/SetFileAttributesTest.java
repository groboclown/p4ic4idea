package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
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
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.SetFileAttributesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class SetFileAttributesTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;

    private static IChangelist pendingChange = null;

    // a file
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
        List<IFileSpec> createdFileSpecs = h.createFile(testFile.getAbsolutePath(), "SetFileAttributeTest");
        IChangelist changelist = h.createChangelist(server, user, client);
        AddFilesOptions addFilesOptions = new AddFilesOptions();
        addFilesOptions.setUseWildcards(true);
        addFilesOptions.setChangelistId(changelist.getId());
        addFilesOptions.setFileType("text");
        client.addFiles(createdFileSpecs, addFilesOptions);

        // set the  propagated attribute
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("p_attrib_1", "p_aValue_1");
        SetFileAttributesOptions sfOpts = new SetFileAttributesOptions();
        sfOpts.setPropagateAttributes(true);
        server.setFileAttributes(createdFileSpecs, hm, sfOpts);

        changelist.refresh();
        changelist.submit(false);

        // we need an edit so we can see an attribute propagate
        pendingChange = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "SetFileAttributesTest", pendingChange, client);
        pendingChange.submit(false);

        pendingChange = h.createChangelist(server, user, client);
    }

    // revert open file and remove attributes between tests
    @Before
    public void cleanup() {

        try {
            List<IFileSpec> fileSpec = makeFileSpecList("//depot/...");
            client.revertFiles(fileSpec, null);
            h.editFile(testFile.getAbsolutePath(), "SetFileAttributesTest", pendingChange, client);

            HashMap<String, String> hm = new HashMap<String, String>();
            hm.put("attrib_1", null);

            SetFileAttributesOptions sfOpts = new SetFileAttributesOptions();
            sfOpts.setSetOnSubmittedFiles(true);

            fileSpec = makeFileSpecList("//depot/...#1");
            server.setFileAttributes(fileSpec, hm, sfOpts);
        } catch (Throwable t) {

        }

    }

    // make sure an attribute is set even with null options
    @Test
    public void nullSetFileAttributesOptions() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());

        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("attrib_1", "aValue_1");

        server.setFileAttributes(fileSpec, hm, null);

        FileStatAncilliaryOptions aOpts = new FileStatAncilliaryOptions();
        aOpts.setShowAttributes(true);

        FileStatOutputOptions oOpts = new FileStatOutputOptions();
        oOpts.setOpenedFiles(true);

        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();
        opts.setAncilliaryOptions(aOpts);
        opts.setOutputOptions(oOpts);

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertTrue("Wrong number of file specs", results.size() == 1);
        assertTrue("Wrong number of attributes", results.get(0).getAttributes().size() == 2);
        assertTrue("Key not found", results.get(0).getAttributes().containsKey("attrib_1"));
        assertEquals("aValue_1", new String(results.get(0).getAttributes().get("attrib_1")));
        assertTrue("Key not found", results.get(0).getAttributes().containsKey("p_attrib_1"));
        assertEquals("p_aValue_1", new String(results.get(0).getAttributes().get("p_attrib_1")));
    }

    // set an attribute on a submitted file with the force flag
    @Test
    public void forceSet() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath() + "#1");

        // set the attribute
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("attrib_1", "aValue_1");

        SetFileAttributesOptions sfOpts = new SetFileAttributesOptions();
        sfOpts.setSetOnSubmittedFiles(true);

        server.setFileAttributes(fileSpec, hm, sfOpts);

        // setup the fstat call
        FileStatAncilliaryOptions aOpts = new FileStatAncilliaryOptions();
        aOpts.setShowAttributes(true);

        FileStatOutputOptions oOpts = new FileStatOutputOptions();
        oOpts.setOpenedFiles(true);

        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();
        opts.setAncilliaryOptions(aOpts);
        opts.setOutputOptions(oOpts);

        // actually run the query
        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertTrue("Wrong number of file specs", results.size() == 1);
        assertTrue("Wrong number of attributes", results.get(0).getAttributes().size() == 2);
        assertTrue("Key not found", results.get(0).getAttributes().containsKey("attrib_1"));
        assertEquals("aValue_1", new String(results.get(0).getAttributes().get("attrib_1")));
        assertTrue("Key not found", results.get(0).getAttributes().containsKey("p_attrib_1"));
        assertEquals("p_aValue_1", new String(results.get(0).getAttributes().get("p_attrib_1")));
    }

    // set an attribute on a submitted file with the force flag
    @Test
    public void propagateSet() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath() + "#1");

        // setup the fstat call
        FileStatAncilliaryOptions aOpts = new FileStatAncilliaryOptions();
        aOpts.setShowAttributes(true);

        FileStatOutputOptions oOpts = new FileStatOutputOptions();

        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();
        opts.setAncilliaryOptions(aOpts);
        opts.setOutputOptions(oOpts);

        // verify #1 is correct
        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertTrue("Wrong number of file specs", results.size() == 1);
        assertTrue(results.get(0).getAttributes().containsKey("p_attrib_1"));
        assertEquals("p_aValue_1", new String(results.get(0).getAttributes().get("p_attrib_1")));

        // verify #2
        fileSpec = makeFileSpecList(testFile.getAbsolutePath() + "#2");
        results = server.getExtendedFiles(fileSpec, opts);

        assertTrue("Wrong number of file specs", results.size() == 1);
        assertTrue(results.get(0).getAttributes().containsKey("p_attrib_1"));
        assertEquals("p_aValue_1", new String(results.get(0).getAttributes().get("p_attrib_1")));
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
