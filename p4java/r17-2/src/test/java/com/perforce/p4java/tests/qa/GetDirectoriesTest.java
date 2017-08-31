package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.server.GetDirectoriesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static com.perforce.p4java.common.base.StringHelper.format;
import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.impl.generic.core.Stream.newStream;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(JUnitPlatform.class)
public class GetDirectoriesTest {
    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;

    private static IStream mainline = null;
    private static IStream dev = null;
    private static IClient defaultClient;

    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        defaultClient = helper.createClient(server, "client1");
        server.setCurrentClient(defaultClient);

        File dir = new File(defaultClient.getRoot(), "dir");
        dir.mkdirs();
        File testFile = new File(defaultClient.getRoot(), "dir" + FILE_SEP + "foo.txt");
        helper.addFile(server, user, defaultClient, testFile.getAbsolutePath(), "IntegrationsOptionsTest", "text");

        IChangelist pendingChangelist = helper.createChangelist(server, user, defaultClient);
        String targetFile = "//depot/bar.txt";
        IFileSpec sourceSpec = new FileSpec(testFile.getAbsolutePath());
        IFileSpec targetSpec = new FileSpec(targetFile);
        IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
        integOpts.setDisplayBaseDetails(true);
        integOpts.setChangelistId(pendingChangelist.getId());

        List<IFileSpec> integedFiles = defaultClient.integrateFiles(sourceSpec, targetSpec, null, integOpts);
        helper.validateFileSpecs(integedFiles);
        pendingChangelist.submit(false);

        pendingChangelist = helper.createChangelist(server, user, defaultClient);
        helper.editFile(testFile.getAbsolutePath(), "IntegrateFilesOptionsTest\nLine2", pendingChangelist, defaultClient);
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
        IStream dev2 = newStream(server, "//streams/dev2",
                "development", "//streams/dev", null, null, null, null, null, null);
        server.createStream(dev2);

        IClient client = helper.createClient(server, "client2");
        client.setStream("//streams/mainline");
        client.update();

        File streamTestFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        helper.addFile(server, user, client, streamTestFile.getAbsolutePath(), "IntegrationsOptionsTest", "text");
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        server.setCurrentClient(defaultClient);
    }

    @DisplayName("verify the -S flag works - validates job046696")
    @Test
    public void getDirsInStreamMatch() throws Throwable {
        GetDirectoriesOptions opts = new GetDirectoriesOptions();
        opts.setStream(mainline.getStream());

        List<IFileSpec> paths = server.getDirectories(makeFileSpecList("//streams/*"), opts);
        helper.validateFileSpecs(paths);

        assertThat("wrong number of paths", paths.size(), is(1));
    }

    @DisplayName("verify the -S flag works, use a classic depot with the -S flag - validates job046696")
    @Test
    public void getDirsInStreamNonStream() throws Throwable {
        GetDirectoriesOptions opts = new GetDirectoriesOptions();
        opts.setStream(dev.getStream());

        String depotPath = "//depot/*";
        List<IFileSpec> paths = server.getDirectories(makeFileSpecList(depotPath), opts);
        helper.validateFileSpecs(paths, FileSpecOpStatus.ERROR);

        assertThat("wrong number of paths", paths.size(), is(1));
        assertThat(paths.get(0).getStatusMessage(), is(format("%s - no such file(s).", depotPath)));
    }

    @DisplayName("verify the -S flag works, use a stream that should have no hits - validates job046696")
    @Test
    public void getDirsInStreamNoMatch() throws Throwable {
        GetDirectoriesOptions opts = new GetDirectoriesOptions();
        opts.setStream(dev.getStream());

        String depotPath = "//streams/*";
        List<IFileSpec> paths = server.getDirectories(makeFileSpecList(depotPath), opts);
        helper.validateFileSpecs(paths, FileSpecOpStatus.ERROR);

        assertThat("wrong number of paths", paths.size(), is(1));
        assertThat(paths.get(0).getStatusMessage(), is(format("%s - no such file(s).", depotPath)));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}