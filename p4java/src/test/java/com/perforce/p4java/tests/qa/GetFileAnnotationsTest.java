package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(JUnitPlatform.class)
public class GetFileAnnotationsTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static File testFile = null;

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

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "GetFileAnnotationsTest\n", "text");

        IChangelist pendingChangelist = helper.createChangelist(server, user, client);

        IFileSpec sourceSpec = new FileSpec(testFile.getAbsolutePath());
        String targetFile = "//depot/bar.txt";
        IFileSpec targetSpec = new FileSpec(targetFile);
        IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
        integOpts.setDisplayBaseDetails(true);
        integOpts.setChangelistId(pendingChangelist.getId());

        List<IFileSpec> integedFiles = client.integrateFiles(sourceSpec, targetSpec, null, integOpts);
        helper.validateFileSpecs(integedFiles);
        pendingChangelist.submit(false);

        // add an edit
        pendingChangelist = helper.createChangelist(server, user, client);
        helper.editFile(client.getRoot() + FILE_SEP + "bar.txt", "GetFileAnnotationsTest\nLine2\n", pendingChangelist, client);
        pendingChangelist.submit(false);

        // bring it back
        pendingChangelist = helper.createChangelist(server, user, client);
        integOpts.setChangelistId(pendingChangelist.getId());
        integedFiles = client.integrateFiles(targetSpec, sourceSpec, null, integOpts);
        helper.validateFileSpecs(integedFiles);

        ResolveFilesAutoOptions rOpts = new ResolveFilesAutoOptions();
        rOpts.setSafeMerge(true);
        client.resolveFilesAuto(makeFileSpecList(testFile.getAbsolutePath()), rOpts);
        pendingChangelist.submit(false);
    }

    @DisplayName("test the -I flag")
    @Test
    public void followIntegrations() throws Throwable {
        GetFileAnnotationsOptions opts = new GetFileAnnotationsOptions();
        opts.setFollowAllIntegrations(true);
        List<IFileAnnotation> annotations = server.getFileAnnotations(makeFileSpecList(testFile.getAbsolutePath()), opts);

        assertThat("wrong number of lines", annotations.size(), is(2));
        assertThat("wrong change number", annotations.get(0).getLower(), is(1));
        assertThat("wrong change number", annotations.get(1).getLower(), is(3));

        assertThat("wrong number of sources", annotations.get(0).getAllIntegrations().size(), is(1));
        assertThat("wrong number of sources", annotations.get(1).getAllIntegrations().size(), is(2));
    }

    @DisplayName("test sans flags")
    @Test
    public void basic() throws Throwable {
        GetFileAnnotationsOptions opts = new GetFileAnnotationsOptions();
        List<IFileAnnotation> annotations = server.getFileAnnotations(makeFileSpecList(testFile.getAbsolutePath()), opts);

        assertThat("wrong number of lines", annotations.size(), is(2));
        assertThat("wrong change number", annotations.get(0).getLower(), is(1));
        assertThat("wrong change number", annotations.get(1).getLower(), is(2));
    }

    @DisplayName("test the -i flag")
    @Test
    public void followBranches() throws Throwable {
        GetFileAnnotationsOptions opts = new GetFileAnnotationsOptions();
        opts.setFollowBranches(true);
        List<IFileAnnotation> annotations = server.getFileAnnotations(makeFileSpecList(testFile.getAbsolutePath()), opts);

        assertThat("wrong number of lines", annotations.size(), is(2));
        assertThat("wrong change number", annotations.get(0).getLower(), is(1));
        assertThat("wrong change number", annotations.get(1).getLower(), is(4));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}
