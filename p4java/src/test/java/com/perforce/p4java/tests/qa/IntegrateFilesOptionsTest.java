package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class IntegrateFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;
    private static IChangelist pendingChangelist = null;
    private static String targetFile = "//depot/bar.txt";

    // One file branched once with an outstanding edit to integrate
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
        h.addFile(server, user, client, testFile.getAbsolutePath(), "IntegrationsOptionsTest", "text");

        pendingChangelist = h.createChangelist(server, user, client);

        IFileSpec sourceSpec = new FileSpec(testFile.getAbsolutePath());
        IFileSpec targetSpec = new FileSpec(targetFile);
        IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
        integOpts.setDisplayBaseDetails(true);
        integOpts.setChangelistId(pendingChangelist.getId());

        List<IFileSpec> integedFiles = client.integrateFiles(sourceSpec, targetSpec, null, integOpts);
        h.validateFileSpecs(integedFiles);
        pendingChangelist.submit(false);

        pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), "IntegrateFilesOptionsTest\nLine2", pendingChangelist, client);
        pendingChangelist.submit(false);
    }

    // make sure the base rev information is available
    @Test
    public void baseRevOptions() throws Throwable {
        IFileSpec sourceSpec = new FileSpec(testFile.getAbsolutePath());
        IFileSpec targetSpec = new FileSpec(targetFile);
        IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
        integOpts.setDisplayBaseDetails(true);

        List<IFileSpec> integedFiles = client.integrateFiles(sourceSpec, targetSpec, null, integOpts);
        h.validateFileSpecs(integedFiles);

        for (IFileSpec file : integedFiles) {
            if (file.getOpStatus() == VALID) {
                assertEquals("Wrong base", "//depot/foo.txt", file.getBaseName());
                assertEquals("Wrong rev", 1, file.getBaseRev());
            }
        }
        ;
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	