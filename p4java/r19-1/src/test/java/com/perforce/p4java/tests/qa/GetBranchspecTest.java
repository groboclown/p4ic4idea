package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.GetBranchSpecOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;








import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.IStreamSummary.Type.DEVELOPMENT;
import static com.perforce.p4java.core.IStreamSummary.Type.MAINLINE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

public class GetBranchspecTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        IClient client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "ProxytTest", "text");

        h.addBranchspec(server, user, "branch1", "//depot/foo...", "//depot/bar...");
        h.addBranchspec(server, user, "BRANCH2", "//depot/foo...", "//depot/baz...");

        h.createDepot(server, "Ace", STREAM, null, "ace/...");
        h.createStream(server, "//Ace/main", MAINLINE, null);
        h.createStream(server, "//Ace/dev", DEVELOPMENT, "//Ace/main");
        h.createStream(server, "//Ace/subDev", DEVELOPMENT, "//Ace/dev");
    }


    /**
     * verify -S flag support
     * @throws Throwable
     */
    @Test
    public void streamBranchspecPrototype() throws Throwable {
        GetBranchSpecOptions opts = new GetBranchSpecOptions();
        opts.setStream("//Ace/dev");
        IBranchSpec branch = server.getBranchSpec("tmp", opts);

        assertThat("incorrect view", branch.getBranchView().getEntry(0).getLeft(), containsString("//Ace/dev/..."));
        assertThat("incorrect view", branch.getBranchView().getEntry(0).getRight(), containsString("//Ace/main/..."));
    }

    /**
     * verify -P flag support
     * @throws Throwable
     */
    @Test
    public void streamBranchspecPrototypeWithOtherParent() throws Throwable {
        GetBranchSpecOptions opts = new GetBranchSpecOptions();
        opts.setStream("//Ace/subDev");
        opts.setParentStream("//Ace/main");
        IBranchSpec branch = server.getBranchSpec("tmp", opts);

        assertThat("incorrect view", branch.getBranchView().getEntry(0).getLeft(), containsString("//Ace/subDev/..."));
        assertThat("incorrect view", branch.getBranchView().getEntry(0).getRight(), containsString("//Ace/main/..."));
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
