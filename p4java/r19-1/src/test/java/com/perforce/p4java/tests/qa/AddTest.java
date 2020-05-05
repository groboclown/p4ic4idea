// NOT FULLY IMPLEMENTED

package com.perforce.p4java.tests.qa;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

public class AddTest {
    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;

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
    }

    // COPYRIGHT SYMBOL
    @Test
    public void copyrightSymbol() throws Throwable {
        List<IFileSpec> file = h.addFile(server, user, client,
                client.getRoot() + Helper.FILE_SEP + "copyright.java", "©", "utf8");
        List<IFileSpec> depotFile = server.getDepotFiles(file, null);
        assertThat(depotFile.get(0).getFileType(), is("utf8"));
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}