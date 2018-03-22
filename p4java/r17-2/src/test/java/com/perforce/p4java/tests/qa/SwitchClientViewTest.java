package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.IStreamSummary.Type.MAINLINE;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class SwitchClientViewTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;

    // a file
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

        server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "SwitchClientViewTest", "text");

        client = h.createClient(server, "CLIENT2");

        h.createDepot(server, "Ace", STREAM, null, "ace/...");
        h.createStream(server, "//Ace/main", MAINLINE, null);

        client = h.createClient(server, "streamclient");
        client.setStream("//Ace/main");
        client.update();

        server.setCurrentClient(server.getClient("client1"));
    }


    @Before
    public void setup() {
        try {

            client.revertFiles(makeFileSpecList("//..."), null);
            server.switchClientView("CLIENT2", "client1", null);

        } catch (Throwable t) {

            h.error(t);

        }
    }


    // verify the stream option
    @Test
    public void simple() throws Throwable {
        server.switchClientView("streamclient", "client1", null);

        IClient cli = server.getClient("client1");
        ClientView view = cli.getClientView();

        assertThat("wrong view", view.getEntry(0).getLeft(), containsString("//Ace/main/..."));
        assertNull("incorrectly has stream", cli.getStream());
    }


    // verify that we can force switches
    @Test(expected = RequestException.class)
    public void failedForcedSwitch() throws Throwable {
        client.editFiles(makeFileSpecList("//depot/..."), null);


        SwitchClientViewOptions opts = new SwitchClientViewOptions();
        opts.setForce(false);

        server.switchClientView("streamclient", "client1", opts);

        fail("should have received a request exception");
    }


    // verify that we can force switches
    @Test
    public void forcedSwitch() throws Throwable {
        client.editFiles(makeFileSpecList("//depot/..."), null);


        SwitchClientViewOptions opts = new SwitchClientViewOptions();
        opts.setForce(true);

        server.switchClientView("streamclient", "client1", opts);

        List<IFileSpec> opened = server.getOpenedFiles(null, null);

        assertThat("wrong number of files", opened.size(), equalTo(1));
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
