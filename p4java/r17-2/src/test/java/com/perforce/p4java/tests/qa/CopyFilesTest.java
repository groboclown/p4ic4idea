package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IBranchMapping;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.BranchSpec;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.fail;

@RunWith(JUnitPlatform.class)
public class CopyFilesTest {
    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static IClient client = null;

    private static IStream mainline = null;
    private static IStream dev = null;
    private static IStream dev2 = null;

    // simple setup with one file with multiple revs
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

        client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CopyFilesTest");
        testFile = new File(client.getRoot(), "foo.c");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CopyFilesTest");
        testFile = new File(client.getRoot(), "foo.helper");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CopyFilesTest");

        // create a branch spec
        ViewMap<IBranchMapping> viewMap = new ViewMap<>();
        viewMap.addEntry(new BranchSpec.BranchViewMapping(0, "//depot/foo... //depot/bar..."));

        IBranchSpec newBranch = new BranchSpec(
                "fooToBar",
                ts.getUser(),
                "branch test branch",
                false,
                null,
                null,
                viewMap
        );

        server.createBranchSpec(newBranch);

        // the following files/branches are used to test that p4java can handle the
        // actions sent by the server
        testFile = new File(client.getRoot(), "baz.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CopyFilesTest");
        testFile = new File(client.getRoot(), "baz.c");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CopyFilesTest");
        testFile = new File(client.getRoot(), "baz.helper");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CopyFilesTest");

        viewMap = new ViewMap<>();
        viewMap.addEntry(new BranchSpec.BranchViewMapping(0, "//depot/baz... //depot/quux..."));

        newBranch = new BranchSpec(
                "bazToQuux",
                ts.getUser(),
                "branch test branch",
                false,
                null,
                null,
                viewMap
        );

        server.createBranchSpec(newBranch);

        IntegrateFilesOptions opts = new IntegrateFilesOptions();
        IChangelist pendingChangelist = helper.createChangelist(server, user, client);
        opts.setChangelistId(pendingChangelist.getId());

        client.integrateFiles(null, null, "bazToQuux", opts);

        pendingChangelist.submit(false);

        pendingChangelist = helper.createChangelist(server, user, client);
        helper.deleteFile("//depot/baz.txt", pendingChangelist, client);

        pendingChangelist = helper.createChangelist(server, user, client);
        helper.editFile(client.getRoot() + Helper.FILE_SEP + "baz.c", "CopyFilesTest2", pendingChangelist, client);
        pendingChangelist.submit(false);

        testFile = new File(client.getRoot(), "baz.java");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CopyFilesTest");

        // streams test data
        // add some streams data
        IDepot depot = new Depot(
                "streams",
                server.getUserName(),
                null,
                "A stream depot of great importance",
                DepotType.STREAM,
                null,
                ".p4s",
                "streams/..."
        );
        server.createDepot(depot);

        mainline = Stream.newStream(server, "//streams/mainline",
                "mainline", null, null, null, null, null, null, null);
        server.createStream(mainline);
        dev = Stream.newStream(server, "//streams/dev",
                "development", "//streams/mainline", null, null, null, null, null, null);
        server.createStream(dev);
        dev2 = Stream.newStream(server, "//streams/dev2",
                "development", "//streams/dev", null, null, null, null, null, null);
        server.createStream(dev2);

        client = helper.createClient(server, "client2");
        client.setStream("//streams/mainline");
        client.update();

        File streamTestFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, streamTestFile.getAbsolutePath(), "IntegrationsOptionsTest", "text");

        client = server.getClient("client1");
        server.setCurrentClient(client);
    }

    @BeforeEach
    public void reset() throws Throwable {
        List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//...");
        client.revertFiles(fileSpec, null);

        client = server.getClient("client1");
        server.setCurrentClient(client);
    }

    // verify that we get a warning about the missing file
    @Test
    public void usingFilespec() throws Exception {
        IFileSpec sourceSpec = new FileSpec("//depot/foo...");
        IFileSpec targetSpec = new FileSpec("//depot/bar...");
        List<IFileSpec> copyList = client.copyFiles(sourceSpec, targetSpec, null, null);

        assertThat("wrong number of files", copyList.size(), is(3));

        verifyUseBranchSpecCopyListFiles(copyList);
    }

    private void verifyUseBranchSpecCopyListFiles(List<IFileSpec> copyList) {
        for (IFileSpec file : copyList) {
            if (file.getOpStatus() == FileSpecOpStatus.VALID) {
                assertThat("wrong file action", file.getAction(), is(FileAction.BRANCH));
            } else if (file.getOpStatus() == FileSpecOpStatus.INFO) {
                assertThat(file.getStatusMessage(), startsWith("can't change mode of file"));
            }
        }
    }

    // using branch spec
    @Test
    public void usingBranchspec() throws Throwable {

        CopyFilesOptions opts = new CopyFilesOptions();
        opts.setBidirectional(true);

        IFileSpec sourceSpec = new FileSpec("//depot/foo...");
        IFileSpec targetSpec = new FileSpec("//depot/bar...");
        List<IFileSpec> copyList = client.copyFiles(sourceSpec, targetSpec, "fooToBar", opts);

        assertThat("wrong number of files", copyList.size(), is(3));

        verifyUseBranchSpecCopyListFiles(copyList);
    }

    // filespec with rev
    @Test
    public void usingFilespecWithRev() throws Exception {
        IFileSpec sourceSpec = new FileSpec("//depot/foo...@2");
        IFileSpec targetSpec = new FileSpec("//depot/bar...");
        List<IFileSpec> copyList = client.copyFiles(sourceSpec, targetSpec, null, null);

        assertThat("wrong number of files", copyList.size(), is(2));

        verifyUseBranchSpecCopyListFiles(copyList);
    }

    // branchspec with rev
    @Test
    public void usingBranchspecWithRev() throws Exception {
        CopyFilesOptions opts = new CopyFilesOptions();
        opts.setBidirectional(true);

        IFileSpec sourceSpec = new FileSpec("//depot/foo...@2");
        IFileSpec targetSpec = new FileSpec("//depot/bar...");
        List<IFileSpec> copyList = client.copyFiles(sourceSpec, targetSpec, "fooToBar", opts);

        assertThat("wrong number of files", copyList.size(), is(2));

        verifyUseBranchSpecCopyListFiles(copyList);
    }

    // branchspec with rev
    @Test
    public void usingOnlySourceWithBranchspec() throws Exception {
        CopyFilesOptions opts = new CopyFilesOptions();
        opts.setBidirectional(true);

        IFileSpec sourceSpec = new FileSpec("//depot/foo...@2");
        List<IFileSpec> copyList = client.copyFiles(sourceSpec, null, "fooToBar", opts);

        assertThat("wrong number of files", copyList.size(), is(2));

    }

    // branchspec with rev
    @Test
    public void usingOnlyTargetWithBranchspec() throws Exception {
        CopyFilesOptions opts = new CopyFilesOptions();
        opts.setReverseMapping(true);

        IFileSpec targetSpec = new FileSpec("//depot/bar...");
        List<IFileSpec> copyList = client.copyFiles(null, targetSpec, "fooToBar", opts);

        assertThat("wrong number of files", copyList.size(), is(1));

    }

    // filespec with rev
    @Test
    public void preview() throws Exception {
        CopyFilesOptions opts = new CopyFilesOptions();
        opts.setNoUpdate(true);

        IFileSpec sourceSpec = new FileSpec("//depot/foo...");
        IFileSpec targetSpec = new FileSpec("//depot/bar...");
        List<IFileSpec> copyList = client.copyFiles(sourceSpec, targetSpec, null, opts);

        assertThat("wrong number of files", copyList.size(), is(3));

        verifyUseBranchSpecCopyListFiles(copyList);

        List<IFileSpec> opened = client.openedFiles(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
        assertThat("should be none", opened.size(), is(0));
    }

    // make sure a delete, a branch, and an integ occurs
    @Test
    public void properCopyBehavior() throws Exception {
        CopyFilesOptions opts = new CopyFilesOptions();
        opts.setBidirectional(true);

        IFileSpec sourceSpec = new FileSpec("//depot/baz...");
        IFileSpec targetSpec = new FileSpec("//depot/quux...");
        List<IFileSpec> copyList = client.copyFiles(sourceSpec, targetSpec, "bazToQuux", opts);

        assertThat("wrong number of files", copyList.size(), is(3));

        for (IFileSpec file : copyList) {
            if (file.getOpStatus() == FileSpecOpStatus.VALID) {
                if (file.getDepotPathString().startsWith("//depot/quux.txt"))
                    assertThat("wrong action for quux.txt", file.getAction(), is(FileAction.DELETE));
                if (file.getDepotPathString().startsWith("//depot/quux.c"))
                    assertThat("wrong action for quux.txt", file.getAction(), is(FileAction.INTEGRATE));
                if (file.getDepotPathString().startsWith("//depot/quux.java"))
                    assertThat("wrong action for quux.java", file.getAction(), is(FileAction.BRANCH));
            } else if (file.getOpStatus() == FileSpecOpStatus.ERROR) {
                fail("wrong action: FileSpecOpStatus.ERROR");
            }
        }
    }

    // filespec with rev
    @Test
    public void failure() throws Exception {
        IFileSpec sourceSpec = new FileSpec("//depot/foo...");
        IFileSpec targetSpec = new FileSpec("//depot/foo...");
        List<IFileSpec> copyList = client.copyFiles(sourceSpec, targetSpec, null, null);

        assertThat("wrong number of files", copyList.size(), is(1));
        assertThat("wasn't an error", copyList.get(0).getOpStatus(), is(FileSpecOpStatus.ERROR));
    }

    // verify the -S flag works
    @Test
    public void usingStreamSpec() throws Exception {
        CopyFilesOptions opts = new CopyFilesOptions();
        opts.setStream(dev.getStream());
        opts.setReverseMapping(true);

        List<IFileSpec> copyList = client.copyFiles(null, null, null, opts);

        assertThat("wrong number of files", copyList.size(), is(1));

        verifyUseBranchSpecCopyListFiles(copyList);
    }

    // verify the -S flag works
    @Test
    public void usingSpecifiedParent() throws Exception {
        CopyFilesOptions opts = new CopyFilesOptions();
        opts.setStream(dev2.getStream());
        opts.setParentStream(mainline.getStream());
        opts.setReverseMapping(true);

        List<IFileSpec> copyList = client.copyFiles(null, null, null, opts);

        assertThat("wrong number of files", copyList.size(), is(1));

        verifyUseBranchSpecCopyListFiles(copyList);
    }

    @AfterAll
    public static void afterClass() throws Throwable {
        helper.after(ts);
    }
}
