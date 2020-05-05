package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.core.IStreamSummary.IOptions;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;



public class DeleteStreamTest {
    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        helper.createDepot(server, "Ace", DepotType.STREAM, null, "ace/...");
        helper.createStream(server, "//Ace/main", Type.MAINLINE, null);

        client.setStream("//Ace/main");
        client.update();

        // create another user for ownership related tests
        helper.createUser(server, "otherUser");
        server.setUserName(ts.getUser());
    }


    @Before
    public void setup() throws Throwable {
        List<String> paths = new ArrayList<String>();
        paths.add("//Ace/dev");
        List<IStreamSummary> streams = server.getStreams(paths, null);
        if (streams.size() == 0) {
            helper.createStream(server, "//Ace/dev", Type.DEVELOPMENT, "//Ace/main");
        }

        paths.clear();
        paths.add("//Ace/otherDev");
        streams = server.getStreams(paths, null);
        if (streams.size() == 0) {
            server.setUserName("otherUser");
            IStream otherStream = helper.createStream(server, "//Ace/otherDev", Type.DEVELOPMENT, "//Ace/main");
            IOptions opts = otherStream.getOptions();
            opts.setLocked(true);
            otherStream.setOptions(opts);
            otherStream.update();
            server.setUserName(ts.getUser());
        }
    }


    /**
     * just make sure the darn thing works
     * @throws Exception
     */
    @Test
    public void simple() throws Exception {
        server.deleteStream("//Ace/dev", null);
        List<String> paths = new ArrayList<String>();
        paths.add("//Ace/dev");
        List<IStreamSummary> streams = server.getStreams(paths, null);

        assertThat("stream not deleted", streams.size(), is(0));
    }


    /**
     * verify the force flag works
     * @throws Exception
     */
    @Test
    public void force() throws Exception {
        StreamOptions opts = new StreamOptions();
        opts.setForceUpdate(true);

        server.deleteStream("//Ace/otherDev", opts);

        List<String> paths = new ArrayList<String>();
        paths.add("//Ace/otherDev");
        List<IStreamSummary> streams = server.getStreams(paths, null);

        assertThat("stream not deleted", streams.size(), is(0));
    }


    /**
     * verify the we get a RequestException without the force flag
     * @throws Exception
     */
    @Test
    public void failForce() throws Exception {
        try {
            server.deleteStream("//Ace/otherDev", null);

            List<String> paths = new ArrayList<String>();
            paths.add("//Ace/otherDev");
            List<IStreamSummary> streams = server.getStreams(paths, null);

            assertThat("stream not deleted", streams.size(), is(0));

            fail("should not reach this point");
        } catch (RequestException re) {
            assertThat("wrong error", re.getLocalizedMessage(), containsString("Stream '//Ace/otherDev' is owned by 'otherUser'."));
        }
    }

    @AfterClass
    public static void afterClass() {
        helper.after(ts);
    }
}
