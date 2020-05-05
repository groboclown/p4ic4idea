package com.perforce.p4java.tests.qa;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary.IClientOptions;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class CompressedConnectionTest {

    private static TestServer ts = null;
    private static Helper helper = null;

    private static IClient client = null;

    /**
     * simple setup with one file and a fix
     * @throws Throwable
     */
    @BeforeClass
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.start();

        IOptionsServer server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        client = helper.createClient(server, "client1");
        IClientOptions opts = new ClientOptions();
        opts.setCompress(true);
        client.setOptions(opts);
        client.update();

        server.setCurrentClient(client);
        File testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CompressedConnectionTest", "text");
    }

    /**
     * Sync a file to ensure the compression works
     * @throws Throwable
     */
    @Test
    public void simple() throws Throwable {
        List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//depot/...");
        fileSpec = client.sync(fileSpec, new SyncOptions().setForceUpdate(true));
        helper.validateFileSpecs(fileSpec);
    }

    @AfterClass
    public static void afterClass() {
        helper.after(ts);
    }
}
