package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.IStreamSummary.Type.DEVELOPMENT;
import static com.perforce.p4java.core.IStreamSummary.Type.MAINLINE;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.admin.ILogTail;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IStreamIntegrationStatus;
import com.perforce.p4java.core.IStreamIntegrationStatus.ICachedState;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.StreamIntegrationStatusOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetStreamIntegrationStatusTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;

    // simple setup with one file
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

        h.createDepot(server, "Ace", STREAM, null, "ace/...");
        h.createStream(server, "//Ace/main", MAINLINE, null);

        client.setStream("//Ace/main");
        client.update();

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetStreamIntegrationStatusTest", "text");

        h.createStream(server, "//Ace/dev", DEVELOPMENT, "//Ace/main");
    }


    // just make sure the darn thing works
    @Test
    public void simple() throws Throwable {
        IStreamIntegrationStatus istat = server.getStreamIntegrationStatus("//Ace/dev", null);
        assertNotNull(istat);
        assertFalse("incorrect state", istat.isIntegFromParent());
        assertFalse("incorrect state", istat.isIntegToParent());
        assertNull("should no be set", istat.getIntegFromParentHow());
        assertThat("incorrect how", istat.getIntegToParentHow(), containsString("copy"));
    }


    // verify istat from child to parent
    @Test
    public void reverse() throws Throwable {
        StreamIntegrationStatusOptions opts = new StreamIntegrationStatusOptions();
        opts.setParentToStream(true);

        IStreamIntegrationStatus istat = server.getStreamIntegrationStatus("//Ace/dev", opts);

        ILogTail log = server.getLogTail(null);
        assertThat("flag not seen", log.getData().get(0), containsString("user-istat -r"));

        assertFalse("incorrect state", istat.isIntegToParent());
        assertTrue("incorrect state", istat.isIntegFromParent());
        assertThat("incorrect how", istat.getIntegFromParentHow(), containsString("merge"));
        assertNull("should not be set", istat.getIntegToParentHow());

        assertNotNull(istat);
    }


    // verify istat in both directions
    @Test
    public void bidirectional() throws Throwable {
        StreamIntegrationStatusOptions opts = new StreamIntegrationStatusOptions();
        opts.setBidirectional(true);

        IStreamIntegrationStatus istat = server.getStreamIntegrationStatus("//Ace/dev", opts);
        assertNotNull(istat);

        assertFalse("incorrect state", istat.isIntegToParent());
        assertTrue("incorrect state", istat.isIntegFromParent());

        assertThat("incorrect how", istat.getIntegFromParentHow(), containsString("merge"));
        assertThat("incorrect how", istat.getIntegToParentHow(), containsString("copy"));

        istat.isFirmerThanParent();
        assertTrue("incorrect flow", istat.isChangeFlowsToParent());
        assertTrue("incorrect flow", istat.isChangeFlowsFromParent());

        istat.getType();
        assertThat("incorrect stream", istat.getStream(), containsString("//Ace/dev"));
        assertThat("incorrect parent", istat.getParent(), containsString("//Ace/main"));
        assertThat("incorrectly cached", istat.getToResult(), containsString("cache"));
        assertThat("incorrectly cached", istat.getFromResult(), anyOf(containsString("query"), containsString("cache")));

        List<ICachedState> state = istat.getCachedStates();
        assertEquals("no state should be returned", 0, state.size());
    }


    @Test
    public void cached() throws Throwable {
        StreamIntegrationStatusOptions opts = new StreamIntegrationStatusOptions();
        opts.setBidirectional(true);
        opts.setNoRefresh(true);

        IStreamIntegrationStatus istat = server.getStreamIntegrationStatus("//Ace/dev", opts);
        assertNotNull(istat);

        assertNull("incorrect how", istat.getIntegFromParentHow());
        assertNull("incorrect how", istat.getIntegToParentHow());

        istat.isFirmerThanParent();
        assertTrue("incorrect flow", istat.isChangeFlowsToParent());
        assertTrue("incorrect flow", istat.isChangeFlowsFromParent());

        istat.getType();
        assertThat("incorrect stream", istat.getStream(), containsString("//Ace/dev"));
        assertThat("incorrect parent", istat.getParent(), containsString("//Ace/main"));
        assertNull("cached by definition", istat.getToResult());
        assertNull("cached by definition", istat.getFromResult());

        List<ICachedState> state = istat.getCachedStates();
        assertEquals("wrong change", 0, state.get(1).getCopyParent());
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	