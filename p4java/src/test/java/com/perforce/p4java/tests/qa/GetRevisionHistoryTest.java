package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetRevisionHistoryTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;
    private static IChangelist pendingChangelist = null;
    private static String targetFile = "//depot/bar.txt";

    // One file branched with an edit that is integrated back
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
        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetFileAnnotationsTest\n", "text");

        pendingChangelist = h.createChangelist(server, user, client);

        IFileSpec sourceSpec = new FileSpec(testFile.getAbsolutePath());
        IFileSpec targetSpec = new FileSpec(targetFile);
        IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
        integOpts.setDisplayBaseDetails(true);
        integOpts.setChangelistId(pendingChangelist.getId());

        List<IFileSpec> integedFiles = client.integrateFiles(sourceSpec, targetSpec, null, integOpts);
        h.validateFileSpecs(integedFiles);
        pendingChangelist.submit(false);

        // add an edit
        pendingChangelist = h.createChangelist(server, user, client);
        h.editFile(client.getRoot() + FILE_SEP + "bar.txt", "GetFileAnnotationsTest\nLine2\n", pendingChangelist, client);
        pendingChangelist.submit(false);

        // bring it back
        pendingChangelist = h.createChangelist(server, user, client);
        integOpts.setChangelistId(pendingChangelist.getId());
        integedFiles = client.integrateFiles(targetSpec, sourceSpec, null, integOpts);
        h.validateFileSpecs(integedFiles);

        ResolveFilesAutoOptions rOpts = new ResolveFilesAutoOptions();
        rOpts.setSafeMerge(true);
        client.resolveFilesAuto(makeFileSpecList(testFile.getAbsolutePath()), rOpts);
        pendingChangelist.submit(false);
    }

    // test the -s flag
    @Test
    public void ignoreSourceRecords() throws Throwable {
        GetRevisionHistoryOptions opts = new GetRevisionHistoryOptions();
        opts.setOmitNonContributaryIntegrations(true);

        List<IFileSpec> specs = makeFileSpecList(targetFile);
        Map<IFileSpec, List<IFileRevisionData>> map = server.getRevisionHistory(specs, opts);

        for (IFileSpec fSpec : map.keySet()) {

            assertNotNull(fSpec);
            List<IFileRevisionData> revList = map.get(fSpec);
            assertNotNull(revList);

            assertNull(revList.get(0).getRevisionIntegrationDataList());
            assertNotNull(revList.get(1).getRevisionIntegrationDataList());
            assertThat("branch from", containsString(revList.get(1).getRevisionIntegrationDataList().get(0).getHowFrom()));

        }
    }

    // no flags
    @Test
    public void basic() throws Throwable {
        GetRevisionHistoryOptions opts = new GetRevisionHistoryOptions();

        List<IFileSpec> specs = makeFileSpecList(targetFile);
        Map<IFileSpec, List<IFileRevisionData>> map = server.getRevisionHistory(specs, opts);

        for (IFileSpec fSpec : map.keySet()) {

            assertNotNull(fSpec);
            List<IFileRevisionData> revList = map.get(fSpec);
            assertNotNull(revList);

            assertNotNull(revList.get(0).getRevisionIntegrationDataList());
            assertThat("copy into", containsString(revList.get(0).getRevisionIntegrationDataList().get(0).getHowFrom()));
            assertNotNull(revList.get(1).getRevisionIntegrationDataList());
            assertThat("branch from", containsString(revList.get(1).getRevisionIntegrationDataList().get(0).getHowFrom()));

        }
        ;
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	