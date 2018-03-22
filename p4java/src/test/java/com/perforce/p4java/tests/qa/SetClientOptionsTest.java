package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.IStreamSummary.Type.MAINLINE;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary.IClientOptions;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

public class SetClientOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IClient client = null;
    private static IUser user = null;
    private static File p = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(
                h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        h.createDepot(server, "Ace", STREAM, null, "ace/...");
        h.createStream(server, "//Ace/main", MAINLINE, null);

        client = h.createClient(server, "streamclient");
        client.setStream("//Ace/main");
        client.update();

        String testPath = client.getRoot() + FILE_SEP + "dir"
                + FILE_SEP;

        String testFile = "test.txt";

        p = new File(testPath);
        p.mkdirs();

        File f = new File(testPath + testFile);
        f.createNewFile();

        h.addFile(server, user, client, testPath + testFile, "test 2");
    }

    @Test
    public void rmdir() throws Throwable {
        IClientOptions opts = new ClientOptions();
        opts.setRmdir(true);
        client.setOptions(opts);
        client.update();

        client.sync(makeFileSpecList("//Ace/main/..."),
                new SyncOptions());
        client.sync(makeFileSpecList("//Ace/main/...#0"),
                new SyncOptions());

        assertFalse("Directory exists, it should not.", p.exists());
    }
}
