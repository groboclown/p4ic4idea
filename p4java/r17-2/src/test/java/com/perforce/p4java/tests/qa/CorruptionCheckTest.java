package com.perforce.p4java.tests.qa;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

@RunWith(JUnitPlatform.class)
public class CorruptionCheckTest {
    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static IClient client = null;

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
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CorruptionCheckTest", "text");
    }

    @DisplayName("we should see an error saying the sync failed as don't ignore corrupted file")
    @Test
    public void corruptedFile() throws Exception {
        server.setOrUnsetServerConfigurationValue("lbr.verify.out", "1");
        // mangle the MD% hash stored in the db
        importMangleRecord(ts);

        List<IFileSpec> results = client.sync(null, new SyncOptions().setForceUpdate(true));

        IFileSpec firstFileSpec = results.get(1);
        assertThat(firstFileSpec.getOpStatus(), is(FileSpecOpStatus.ERROR));
        assertThat(firstFileSpec.getStatusMessage(), containsString("corrupted during transfer (or bad on the server)"));
    }

    @DisplayName("we should saying the sync success as ignore corrupted files")
    @Test
    public void ignoreCorruptedFile() throws Throwable {
        server.setOrUnsetServerConfigurationValue("lbr.verify.out", "0");

        // mangle the MD% hash stored in the db
        importMangleRecord(ts);

        List<IFileSpec> results = client.sync(null, new SyncOptions().setForceUpdate(true));
        helper.validateFileSpecs(results);
    }

    private void importMangleRecord(TestServer ts) throws Exception{
        ts.importRecord("@rv@ 8 @db.revhx@ @//depot/foo.txt@ 1 0 0 1 1297895927 1297895927 4BDAAF230EDDFC2BF8F9DC4107746AAA 19 0 0 @//depot/foo.txt@ @1.1@ 0\n");
        ts.importRecord("@rv@ 8 @db.rev@ @//depot/foo.txt@ 1 0 0 1 1297895927 1297895927 4BDAAF230EDDFC2BF8F9DC4107746AAA 19 0 0 @//depot/foo.txt@ @1.1@ 0\n");
    }

    @AfterAll
    public static void afterClass() throws Exception {
        helper.after(ts);
    }
}
