package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.option.client.ResolveFilesAutoOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static java.lang.System.err;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.admin.ILogTail;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

public class ResolveFilesAutoOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File sourceFile = null;
    private static List<IFileSpec> sourceFileSpecs = null;
    private static String sourceContent = "source";
    private static File targetFile = null;
    private static List<IFileSpec> targetFileSpecs = null;
    private static String targetContent = "target";
    private static File conflictFile = null;
    private static List<IFileSpec> conflictFileSpecs = null;
    private static String conflictContent = "conflict1";
    private static String conflictContent2 = "conflict2";
    private static String conflictContent3 = "conflict3";
    private static String conflictMergedContent = null;
    private static File sourceBinFile = null;
    private static List<IFileSpec> sourceBinFileSpecs = null;
    private static File targetBinFile = null;
    private static List<IFileSpec> targetBinFileSpecs = null;
    private static String targetBinContent = "2222";
    private static String sourceBinContent = "1111";
    private static File sourceDeletedFile = null;
    private static File targetDeletedFile = null;
    private static List<IFileSpec> sourceDeletedFileSpecs = null;
    private static List<IFileSpec> targetDeletedFileSpecs = null;

    private static File sourceMoveFile = null;
    private static List<IFileSpec> sourceMoveFileSpecs = null;
    private static List<IFileSpec> sourceMovedFileSpecs = null;
    private static List<IFileSpec> targetMovedFileSpecs = null;

    private static File sourceTypeFile = null;
    private static File targetTypeFile = null;
    private static List<IFileSpec> sourceTypeFileSpecs = null;
    private static List<IFileSpec> targetTypeFileSpecs = null;

    private static IChangelist pendingChangelist = null;

    @BeforeClass
    public static void beforeClass() {
        err.println("I am in ResolveFiles...");
        h = new Helper();

        try {

            ts = new TestServer();
            ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
            ts.start();

            server = h.getServer(ts);
            server.setUserName(ts.getUser());
            server.connect();

            user = server.getUser(ts.getUser());

            client = h.createClient(server, "client1");
            server.setCurrentClient(client);

            sourceFile = new File(client.getRoot() + FILE_SEP + "source.txt");
            sourceFileSpecs = h.addFile(server, user, client, sourceFile.getAbsolutePath(), sourceContent, "text");

            targetFile = new File(client.getRoot() + FILE_SEP + "target.txt");
            targetFileSpecs = h.addFile(server, user, client, targetFile.getAbsolutePath(), targetContent, "text");

            sourceBinFile = new File(client.getRoot() + FILE_SEP + "source.bin");
            targetBinFile = new File(client.getRoot() + FILE_SEP + "target.bin");
            sourceBinFileSpecs = h.addFile(server, user, client, sourceBinFile.getAbsolutePath(), sourceBinContent, "binary");
            targetBinFileSpecs = h.addFile(server, user, client, targetBinFile.getAbsolutePath(), targetBinContent, "binary");

            // deleted file for testing delete resolves
            sourceDeletedFile = new File(client.getRoot() + FILE_SEP + "source.del");
            targetDeletedFile = new File(client.getRoot() + FILE_SEP + "target.del");
            sourceDeletedFileSpecs = h.addFile(server, user, client, sourceDeletedFile.getAbsolutePath(), "deleteme");
            targetDeletedFileSpecs = h.addFile(server, user, client, targetDeletedFile.getAbsolutePath(), "deleteme");

            pendingChangelist = h.createChangelist(server, user, client);
            h.deleteFile(sourceDeletedFile.getAbsolutePath(), pendingChangelist, client);
            pendingChangelist.submit(false);

            // move a file for testing move resolves
            File sourceDir = new File(client.getRoot() + FILE_SEP + "source");
            sourceDir.mkdirs();
            sourceMoveFile = new File(client.getRoot() + FILE_SEP + "source" + FILE_SEP + "foo.mov");
            sourceMoveFileSpecs = h.addFile(server, user, client, sourceMoveFile.getAbsolutePath(), "moveme");
            sourceMovedFileSpecs = makeFileSpecList("//depot/source/bar.mov");
            targetMovedFileSpecs = makeFileSpecList("//depot/target/foo.mov");

            pendingChangelist = h.createChangelist(server, user, client);
            IntegrateFilesOptions iOpts = new IntegrateFilesOptions().setChangelistId(pendingChangelist.getId());
            client.integrateFiles(sourceMoveFileSpecs.get(0), targetMovedFileSpecs.get(0), null, iOpts);
            pendingChangelist.submit(false);

            pendingChangelist = h.createChangelist(server, user, client);
            MoveFileOptions mOpts = new MoveFileOptions().setChangelistId(pendingChangelist.getId());
            client.editFiles(sourceMoveFileSpecs, null);
            server.moveFile(sourceMoveFileSpecs.get(0), sourceMovedFileSpecs.get(0), mOpts);
            pendingChangelist.submit(false);

            // setup a file type resolve
            sourceTypeFile = new File(client.getRoot() + FILE_SEP + "source.typ");
            targetTypeFile = new File(client.getRoot() + FILE_SEP + "target.typ");
            sourceTypeFileSpecs = h.addFile(server, user, client, sourceTypeFile.getAbsolutePath(), "type change", "text+w");
            targetTypeFileSpecs = h.addFile(server, user, client, targetTypeFile.getAbsolutePath(), "type change");

            // make a conflict. this should always be the last part of the setup because it wants to leave a file in an open state.
            conflictFile = new File(client.getRoot() + FILE_SEP + "conflict.txt");
            conflictFileSpecs = h.addFile(server, user, client, conflictFile.getAbsolutePath(), conflictContent, "text");
            pendingChangelist = h.createChangelist(server, user, client);
            h.editFile(conflictFile.getAbsolutePath(), conflictContent2, pendingChangelist, client);
            pendingChangelist.submit(null);
            client.sync(makeFileSpecList(conflictFile.getAbsolutePath() + "#1"), null);
            pendingChangelist = h.createChangelist(server, user, client);

            conflictMergedContent = ">>>> ORIGINAL " + conflictFileSpecs.get(0).getOriginalPathString() + "#1\n" +
                    conflictContent + "==== THEIRS " + conflictFileSpecs.get(0).getOriginalPathString() + "#2\n" +
                    conflictContent2 + "==== YOURS //" + client.getName() + "/" + conflictFile.getName() + "\n" +
                    conflictContent3;

        } catch (Throwable t) {

            h.error(t);

        }

    }

    @Before
    public void before() throws Throwable {

        client.revertFiles(makeFileSpecList("//..."), null);
        assertEquals(0, server.getOpenedFiles(targetFileSpecs, null).size());
        assertEquals(0, server.getOpenedFiles(targetBinFileSpecs, null).size());

        h.validateFileSpecs(client.integrateFiles(sourceFileSpecs.get(0), targetFileSpecs.get(0), null, new IntegrateFilesOptions().setDoBaselessMerge(true)));
        h.validateFileSpecs(client.integrateFiles(sourceBinFileSpecs.get(0), targetBinFileSpecs.get(0), null, new IntegrateFilesOptions().setDoBaselessMerge(true)));

        h.editFile(conflictFile.getAbsolutePath(), conflictContent3, pendingChangelist, client);
        assertEquals(1, server.getOpenedFiles(conflictFileSpecs, null).size());
        pendingChangelist.submit(null);

    }

    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("b:n b:s b:af b:as b:at b:ay b:Aa b:Ab b:Ac b:Ad b:Am b:At i:c:cl b:t b:db b:dw b:dl b:o", OPTIONS_SPECS);
    }

    @Test
    public void gettersDefaultConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        assertEquals(false, resolveFilesAutoOptions.isAcceptTheirs());
        assertEquals(false, resolveFilesAutoOptions.isAcceptYours());
        assertEquals(false, resolveFilesAutoOptions.isForceResolve());
        assertEquals(false, resolveFilesAutoOptions.isSafeMerge());
        assertEquals(false, resolveFilesAutoOptions.isShowActionsOnly());
    }

    @Test
    public void gettersExplicitConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions(true, true, true, true, true);
        assertEquals(true, resolveFilesAutoOptions.isAcceptTheirs());
        assertEquals(true, resolveFilesAutoOptions.isAcceptYours());
        assertEquals(true, resolveFilesAutoOptions.isForceResolve());
        assertEquals(true, resolveFilesAutoOptions.isSafeMerge());
        assertEquals(true, resolveFilesAutoOptions.isShowActionsOnly());
    }

    @Test
    public void gettersAfterSetters() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setAcceptTheirs(true);
        resolveFilesAutoOptions.setAcceptYours(true);
        resolveFilesAutoOptions.setForceResolve(true);
        resolveFilesAutoOptions.setSafeMerge(true);
        resolveFilesAutoOptions.setShowActionsOnly(true);
        assertEquals(true, resolveFilesAutoOptions.isAcceptTheirs());
        assertEquals(true, resolveFilesAutoOptions.isAcceptYours());
        assertEquals(true, resolveFilesAutoOptions.isForceResolve());
        assertEquals(true, resolveFilesAutoOptions.isSafeMerge());
        assertEquals(true, resolveFilesAutoOptions.isShowActionsOnly());
    }

    @Test
    public void resolveAutoNullOptionsObject() throws Throwable {
        client.resolveFilesAuto(targetFileSpecs, null);

        verifyTargetFileContent(targetFile, targetContent);
    }

    @Test
    public void resolveAutoDefaults() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);
    }

    @Test
    public void resolveAutoDefaultsOldMethod() throws Throwable {
        client.resolveFilesAuto(targetFileSpecs, false, false, false, false, false);

        verifyTargetFileContent(targetFile, targetContent);
    }

    @Test
    public void acceptTheirsExplicitConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions(false, false, true, false, false);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, sourceContent);
    }

    @Test
    public void acceptTheirsSetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setAcceptTheirs(true);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, sourceContent);
    }

    @Test
    public void acceptTheirsStringConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-at");

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, sourceContent);
    }

    @Test
    public void acceptYoursExplicitConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions(false, false, false, true, false);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);
    }

    @Test
    public void acceptYoursSetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setAcceptYours(true);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);
    }

    @Test
    public void acceptYoursStringConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-ay");

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);
    }

    @Test
    public void forceResolveExplicitConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions(false, false, false, false, true);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictMergedContent);
    }

    @Test
    public void forceResolveSetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setForceResolve(true);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictMergedContent);
    }

    @Test
    public void forceResolveStringConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-af");

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictMergedContent);
    }

    @Test
    public void safeMergeExplicitConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions(false, true, false, false, false);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictContent3);
    }

    @Test
    public void safeMergeSetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setSafeMerge(true);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictContent3);
    }

    @Test
    public void safeMergeStringConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-as");

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictContent3);
    }

    @Test
    public void specifyChangelistSetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setChangelistId(pendingChangelist.getId());

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictContent3);
    }

    @Test
    public void showActionsOnlyExplicitConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions(true, false, true, false, false);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);
    }

    @Test
    public void showActionsOnlySetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setShowActionsOnly(true);
        resolveFilesAutoOptions.setAcceptTheirs(true);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);
    }

    @Test
    public void showActionsOnlyStringConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-n", "-at");

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);
    }

    @Test
    public void binaryResolveSansFlag() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setForceTextualMerge(false);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(targetBinFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(targetBinFile, targetBinContent);
    }

    @Test
    public void forceDiffSetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setForceTextualMerge(true);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(targetBinFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(targetBinFile, targetBinContent);
    }

    @Test
    public void forceDiffStringConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-t");

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(targetBinFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(targetBinFile, targetBinContent);
    }

    @Test
    public void ignoreWhiteSpaceChangesSetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setIgnoreWhitespaceChanges(true);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictContent3);

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-db"));
    }

    @Test
    public void ignoreWhiteSpaceChangesConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-db");

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(conflictFile, conflictContent3);

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-db"));
    }

    @Test
    public void ignoreWhiteSpaceSetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setIgnoreWhitespace(true);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictContent3);

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-dw"));
    }

    @Test
    public void ignoreWhiteSpaceConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-dw");

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(conflictFile, conflictContent3);

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-dw"));
    }

    @Test
    public void ignoreLineEndingsSetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setIgnoreLineEndings(true);

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);

        verifyTargetFileContent(conflictFile, conflictContent3);

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-dl"));
    }

    @Test
    public void ignoreLineEndingsConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-dl");

        client.resolveFilesAuto(targetFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(targetFile, targetContent);

        client.resolveFilesAuto(conflictFileSpecs, resolveFilesAutoOptions);
        verifyTargetFileContent(conflictFile, conflictContent3);

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-dl"));
    }


    // Verify that -Ab flag restricts the resolve to only structural resolves
    @Test
    public void resolveBranchesOnlyConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-Ab");
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();
        iOpts.setBranchResolves(true);

        client.integrateFiles(sourceFileSpecs.get(0), new FileSpec("//depot/ignore.me"), null, iOpts);
        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//depot/..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 2, resolvedFiles.size());
        assertThat("incorrect file", resolvedFiles.get(0).getClientPathString(), containsString("ignore.me"));
        assertThat("incorrect file", resolvedFiles.get(0).getFromFile(), containsString(sourceFileSpecs.get(0).getOriginalPathString()));
        assertThat("incorrect file", resolvedFiles.get(1).getFromFile(), containsString(sourceFileSpecs.get(0).getOriginalPathString()));

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-Ab"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("branch"));
    }

    // Verify that -Ab flag restricts the resolve to only structural resolves
    @Test
    public void resolveBranchesOnlySetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setResolveFileBranching(true);
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();
        iOpts.setBranchResolves(true);

        client.integrateFiles(sourceFileSpecs.get(0), new FileSpec("//depot/ignore.me"), null, iOpts);
        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//depot/..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 2, resolvedFiles.size());
        assertThat("incorrect file", resolvedFiles.get(0).getClientPathString(), containsString("ignore.me"));
        assertThat("incorrect file", resolvedFiles.get(0).getFromFile(), containsString(sourceFileSpecs.get(0).getOriginalPathString()));
        assertThat("incorrect file", resolvedFiles.get(1).getFromFile(), containsString(sourceFileSpecs.get(0).getOriginalPathString()));

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-Ab"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("branch"));
    }


    // Verify that -Ac flag restricts the resolve to only content resolves
    @Test
    public void resolveContentOnlyConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-Ac");
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();
        iOpts.setBranchResolves(true);

        client.integrateFiles(sourceFileSpecs.get(0), new FileSpec("//depot/ignore.me"), null, iOpts);

        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 9, resolvedFiles.size()); // would be 11 with the branch resolve;
        assertThat("incorrect type", resolvedFiles.get(0).getContentResolveType(), containsString("3waytext"));
        assertThat("incorrect type", resolvedFiles.get(3).getContentResolveType(), containsString("2wayraw"));

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-Ac"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("content"));
    }

    // Verify that -Ac flag restricts the resolve to only content resolves
    @Test
    public void resolveContentOnlySetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setResolveFileContentChanges(true);
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();
        iOpts.setBranchResolves(true);

        client.integrateFiles(sourceFileSpecs.get(0), new FileSpec("//depot/ignore.me"), null, iOpts);

        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 9, resolvedFiles.size()); // would be 11 with the branch resolve;

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-Ac"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("content"));
    }

    // Verify that branch and content resolves will be listed at the same time if needs be
    @Test
    public void resolveBranchesAndContent() throws Throwable {
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();
        iOpts.setBranchResolves(true);

        client.integrateFiles(sourceFileSpecs.get(0), new FileSpec("//depot/ignore.me"), null, iOpts);

        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//..."), null);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 11, resolvedFiles.size()); // would be 11 with the branch resolve;

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("content"));
    }

    // Verify that -Ad flag restricts the resolve to only content resolves
    @Test
    public void resolveDeleteOnlyConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-Ad");
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();
        iOpts.setDeleteResolves(true);

        client.integrateFiles(sourceDeletedFileSpecs.get(0), targetDeletedFileSpecs.get(0), null, iOpts);

        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 2, resolvedFiles.size()); // would be 11 without filter flag;

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-Ad"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("delete"));
    }

    // Verify that -Ad flag restricts the resolve to only content resolves
    @Test
    public void resolveDeleteOnlySetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setResolveFileDeletions(true);
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();
        iOpts.setDeleteResolves(true);

        client.integrateFiles(sourceDeletedFileSpecs.get(0), targetDeletedFileSpecs.get(0), null, iOpts);

        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 2, resolvedFiles.size()); // would be 11 without filter flag;

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-Ad"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("delete"));
    }


    // Verify that -Am flag restricts the resolve to only content resolves
    @Test
    public void resolveMoveOnlyConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-Am");
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();

        client.integrateFiles(new FileSpec("//depot/source/..."), new FileSpec("//depot/target/..."), null, iOpts);

        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 2, resolvedFiles.size()); // would be 11 without filter flag;

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-Am"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("move"));
    }


    // Verify that -Am flag restricts the resolve to only content resolves
    @Test
    public void resolveMoveOnlySetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setResolveMovedFiles(true);
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();

        client.integrateFiles(new FileSpec("//depot/source/..."), new FileSpec("//depot/target/..."), null, iOpts);

        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 2, resolvedFiles.size()); // would be 11 without filter flag;

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-Am"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("move"));
    }


    // Verify that -At flag restricts the resolve to only filetype resolves
    @Test
    public void resolveFiletypeOnlySetter() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
        resolveFilesAutoOptions.setResolveFiletypeChanges(true);
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();
        iOpts.setForceIntegration(true);

        client.integrateFiles(sourceTypeFileSpecs.get(0), targetTypeFileSpecs.get(0), null, iOpts);

        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 2, resolvedFiles.size()); // would be 11 without filter flag;

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-At"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("filetype"));
    }


    // Verify that -At flag restricts the resolve to only filetype resolves
    @Test
    public void resolveFiletypeOnlyConstructor() throws Throwable {
        ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions("-At");
        IntegrateFilesOptions iOpts = new IntegrateFilesOptions();
        iOpts.setForceIntegration(true);

        client.integrateFiles(sourceTypeFileSpecs.get(0), targetTypeFileSpecs.get(0), null, iOpts);

        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(makeFileSpecList("//..."), resolveFilesAutoOptions);
        assertNotNull(resolvedFiles);
        assertEquals("wrong number of specs", 2, resolvedFiles.size()); // would be 11 without filter flag;

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("-At"));

        assertThat("wrong type", resolvedFiles.get(0).getResolveType(), containsString("filetype"));
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

    private static void verifyTargetFileContent(File target, String theoreticalContent) throws Throwable {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(target));
        String line = "";
        String experimentalContent = "";

        while ((line = bufferedReader.readLine()) != null) {

            if (experimentalContent != "") {

                experimentalContent += "\n";

            }

            experimentalContent += line;
        }

        bufferedReader.close();

        assertEquals(theoreticalContent, experimentalContent);

    }

}