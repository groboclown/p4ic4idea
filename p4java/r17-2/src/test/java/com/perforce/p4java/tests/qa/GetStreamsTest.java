package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.IStreamSummary.Type.DEVELOPMENT;
import static com.perforce.p4java.core.IStreamSummary.Type.MAINLINE;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetStreamsTest {

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

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetStreamstTest", "text");

        h.createDepot(server, "Ace", STREAM, null, "ace/...");
        h.createStream(server, "//Ace/main", MAINLINE, null);
        h.createStream(server, "//Ace/dev", DEVELOPMENT, "//Ace/main");
        h.createStream(server, "//Ace/dev2", DEVELOPMENT, "//Ace/main");
        h.createStream(server, "//Ace/sub", DEVELOPMENT, "//Ace/dev");
    }


    // verify job050292: no exception when running against a non-stream depot
    @Test
    public void nonStreamDepot() throws Throwable {
        List<String> streamPaths = new ArrayList<String>();
        streamPaths.add("//depot/*");
        List<IStreamSummary> streams = server.getStreams(streamPaths, null);

        assertThat("incorrect number of streams", streams.size(), equalTo(0));
    }


    // list some streams
    @Test
    public void listStreams() throws Throwable {
        List<IStreamSummary> streams = server.getStreams(null, null);
        assertThat("incorrect number of streams", streams.size(), equalTo(4));
    }


    // list just one stream
    @Test
    public void maxResults() throws Throwable {
        GetStreamsOptions opts = new GetStreamsOptions();
        opts.setMaxResults(1);

        List<IStreamSummary> streams = server.getStreams(null, opts);

        assertThat("incorrect number of streams", streams.size(), equalTo(1));
    }


    // verify we can get just the set of fields we want
    @Test
    public void fields() throws Throwable {
        GetStreamsOptions opts = new GetStreamsOptions();
        opts.setFields("Stream,Owner");
        opts.setMaxResults(1);

        List<String> paths = new ArrayList<String>();
        paths.add("//Ace/main");
        List<IStreamSummary> streams = server.getStreams(paths, opts);

        assertThat("incorrect number of streams", streams.size(), equalTo(1));
        assertNull("should not have seen stream type", streams.get(0).getType());
        assertNull("should not have seen stream parent", streams.get(0).getParent());
        assertThat("stream field missing", streams.get(0).getStream(), containsString("//Ace"));
    }


    // verify we can get just the set of fields we want
    @Test
    public void fieldsJustOwner() throws Throwable {
        GetStreamsOptions opts = new GetStreamsOptions();
        opts.setFields("Owner");
        opts.setMaxResults(1);

        List<String> paths = new ArrayList<String>();
        paths.add("//Ace/dev");
        List<IStreamSummary> streams = server.getStreams(paths, opts);

        assertThat("incorrect number of streams", streams.size(), equalTo(1));
        assertNull("should not have seen stream type", streams.get(0).getType());
        assertNull("should not have seen stream parent", streams.get(0).getParent());
        assertNull("should not have seen stream parent", streams.get(0).getStream());
        assertThat("owner field missing", streams.get(0).getOwnerName(), containsString(ts.getUser()));
    }


    // verify we can get just the set of fields we want
    @Test
    public void fieldsJustType() throws Throwable {
        GetStreamsOptions opts = new GetStreamsOptions();
        opts.setFields("Type");
        opts.setMaxResults(1);

        List<String> paths = new ArrayList<String>();
        paths.add("//Ace/dev");
        List<IStreamSummary> streams = server.getStreams(paths, opts);

        assertThat("incorrect number of streams", streams.size(), equalTo(1));
        assertNull("should not have seen stream owner", streams.get(0).getOwnerName());
        assertNull("should not have seen stream parent", streams.get(0).getParent());
        assertNull("should not have seen stream parent", streams.get(0).getStream());
        assertThat("type field missing", streams.get(0).getType().toString(), containsString("DEVELOPMENT"));
    }


    // verify we can get just the set of fields we want
    @Test
    public void pathArgument() throws Throwable {
        List<String> paths = new ArrayList<String>();
        paths.add("//Ace/dev*");
        List<IStreamSummary> streams = server.getStreams(paths, null);

        assertThat("incorrect number of streams", streams.size(), equalTo(2));
    }


    // verify we can get streams via filters
    @Test
    public void filters() throws Throwable {
        GetStreamsOptions opts = new GetStreamsOptions();
        opts.setFilter("Parent=//Ace/main");

        List<IStreamSummary> streams = server.getStreams(null, opts);

        assertThat("incorrect number of streams", streams.size(), equalTo(2));
    }


    // verify we can get streams via filters
    @Test
    public void notFilters() throws Throwable {
        GetStreamsOptions opts = new GetStreamsOptions();
        opts.setFilter("Stream=//Ace/* ^Type=development");

        List<IStreamSummary> streams = server.getStreams(null, opts);

        assertThat("incorrect number of streams", streams.size(), equalTo(1));
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
