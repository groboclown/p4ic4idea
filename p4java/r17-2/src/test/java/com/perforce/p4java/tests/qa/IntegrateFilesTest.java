package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.file.FileAction.BRANCH;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.impl.generic.core.Stream.newStream;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class IntegrateFilesTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;
    private static File streamTestFile = null;
    private static IChangelist pendingChangelist = null;
    private static String targetFile = "//depot/bar.txt";

    private static IStream mainline = null;
    private static IStream dev = null;
    private static IStream dev2 = null;

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

        // add some streams data
        IDepot depot = new Depot(
                "streams",
                server.getUserName(),
                null,
                "A stream depot of great importance",
                STREAM,
                null,
                ".p4s",
                "streams/..."
        );
        server.createDepot(depot);

        mainline = newStream(server, "//streams/mainline",
                "mainline", null, null, null, null, null, null, null);
        server.createStream(mainline);
        dev = newStream(server, "//streams/dev",
                "development", "//streams/mainline", null, null, null, null, null, null);
        server.createStream(dev);
        dev2 = newStream(server, "//streams/dev2",
                "development", "//streams/dev", null, null, null, null, null, null);
        server.createStream(dev2);

        client = h.createClient(server, "client2");
        client.setStream("//streams/mainline");
        client.update();

        streamTestFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, streamTestFile.getAbsolutePath(), "IntegrationsOptionsTest", "text");

        client = server.getClient("client1");
        server.setCurrentClient(client);
    }

    @Before
    public void before() {

        try {

            client = server.getClient("client2");
            server.setCurrentClient(client);
            client.revertFiles(makeFileSpecList("//..."), null);

            client = server.getClient("client1");
            server.setCurrentClient(client);
            client.revertFiles(makeFileSpecList("//..."), null);

        } catch (Throwable t) {

            h.error(t);

        }

    }

    // make sure the base rev information is available
    // validates job046698
    @Test
    public void baseRevOptions() throws Throwable {
        client = server.getClient("client1");
        server.setCurrentClient(client);
        client.sync(null, null);

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
    }

    // verify the -S flag works
    // validates job046698
    @Test
    public void integThroughStreamSpec() throws Throwable {
        client = server.getClient("client2");
        server.setCurrentClient(client);

        client.setStream(dev.getStream());
        client.update();
        client.sync(null, null);

        IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
        integOpts.setStream(dev.getStream());
        integOpts.setReverseMapping(true);
        integOpts.setDisplayBaseDetails(true);

        List<IFileSpec> integedFiles = client.integrateFiles(null, null, null, integOpts);
        h.validateFileSpecs(integedFiles);

        for (IFileSpec file : integedFiles) {
            if (file.getOpStatus() == VALID) {
                assertEquals("Wrong fromfile", "//streams/mainline/foo.txt", file.getFromFile());
                assertEquals("Wrong action", BRANCH, file.getAction());
            }
        }
    }


    // make sure the base rev information is available
    @Test
    public void integToSpecifiedParent() throws Throwable {
        client = server.getClient("client2");
        server.setCurrentClient(client);

        client.setStream(dev2.getStream());
        client.update();
        client.sync(null, null);

        IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
        integOpts.setStream(dev2.getStream());
        integOpts.setParentStream(mainline.getStream());
        integOpts.setReverseMapping(true);
        integOpts.setDisplayBaseDetails(true);

        List<IFileSpec> integedFiles = client.integrateFiles(null, null, null, integOpts);
        h.validateFileSpecs(integedFiles);

        for (IFileSpec file : integedFiles) {
            if (file.getOpStatus() == VALID) {
                assertEquals("Wrong fromfile", "//streams/mainline/foo.txt", file.getFromFile());
                assertEquals("Wrong action", BRANCH, file.getAction());
            }
        }
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	