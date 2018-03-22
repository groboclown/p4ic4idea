package com.perforce.p4java.tests.qa;


import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.LockFilesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(JUnitPlatform.class)
public class FileSpecTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static IClient client = null;
    private static IClient client2 = null;
    private static IChangelist pendingChangelist = null;


    // setup a server with one open and locked file with two clients and users
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

        helper.addFile(server, user, client, testFile.getAbsolutePath(), "FileSpecTest", "text");

        pendingChangelist = helper.createChangelist(server, user, client);

        List<IFileSpec> fileSpec = helper.editFile(testFile.getAbsolutePath(), "FileSpecTest\nLine2", pendingChangelist, client);

        client.lockFiles(fileSpec, new LockFilesOptions());

        List<IFileSpec> shelvedFiles = client.shelveChangelist(pendingChangelist);
        helper.validateFileSpecs(shelvedFiles);

        helper.createUser(server, "secondUser");
        client2 = helper.createClient(server, "client2");
    }

    // p4 opened always uses "ourLock" whether or not it is locked by the current user/client, so we have to
    // use p4 fstat as well
    @Test
    public void LockedByOther() throws Throwable {
        server.setUserName("secondUser");
        assertThat("Wrong user", server.getUser(null).getLoginName(), is("secondUser"));

        server.setCurrentClient(client2);

        // check using the output from 'p4 opened' first
        OpenedFilesOptions openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setAllClients(true);

        List<IFileSpec> openedFiles = server.getOpenedFiles(null, openedFilesOptions);

        assertThat("No opened files found", openedFiles.size() > 0, is(true));
        for (IFileSpec file : openedFiles) {
            assertThat(file.isLocked(), is(true));
        }

        // now check using the output from 'p4 fstat'
        List<IExtendedFileSpec> fstatFiles = server.getExtendedFiles(openedFiles, new GetExtendedFilesOptions());

        for (IExtendedFileSpec file : fstatFiles) {
            assertThat(file.isLocked(), is(true));
            assertThat(file.isOtherLocked(), is(true));
        }
    }

    @Test
    public void LockedBySelf() throws Throwable {
        server.setUserName(ts.getUser());
        assertThat("Wrong user", server.getUser(null).getLoginName(), is(ts.getUser()));

        server.setCurrentClient(client);

        // check using the output from 'p4 opened' first
        OpenedFilesOptions openedFilesOptions = new OpenedFilesOptions();

        List<IFileSpec> openedFiles = client.openedFiles(null, openedFilesOptions);

        for (IFileSpec file : openedFiles) {
            assertThat(file.isLocked(), is(true));
        }

        // now check using the output from 'p4 fstat'
        List<IExtendedFileSpec> fstatFiles = server.getExtendedFiles(openedFiles, new GetExtendedFilesOptions());

        for (IExtendedFileSpec file : fstatFiles) {
            assertThat(file.isLocked(), is(true));
            assertThat(file.isOtherLocked(), is(false));
        }
    }

    @Test
    public void verifyIsShelved() throws Throwable {
        server.setUserName(ts.getUser());
        assertThat("Wrong user", server.getUser(null).getLoginName(), is(ts.getUser()));
        server.setCurrentClient(client);

        FileStatOutputOptions outputOptions = new FileStatOutputOptions();
        outputOptions.setShelvedFiles(true);

        List<IFileSpec> files = pendingChangelist.getFiles(false);
        List<IExtendedFileSpec> fstatFiles = server.getExtendedFiles(files, new GetExtendedFilesOptions().setOutputOptions(outputOptions)
                .setAffectedByChangelist(pendingChangelist.getId()));

        for (IExtendedFileSpec file : fstatFiles) {
            assertThat("File was null", file.getDepotPath(), notNullValue());
            assertThat("File was not shelved", file.isShelved(), is(true));
        }
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}

