package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.admin.ILogTail;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.LogTailOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetLogtailTest {

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
        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetLogtailtTest", "text");
    }


    // basic log fetching query
    @Test
    public void getLog() throws Throwable {
        ILogTail log = server.getLogTail(null);

        assertThat("log text missing", log.getData().get(0), containsString("Perforce db files in"));
        assertThat("log name missing", log.getLogFilePath(), containsString("log.txt"));
        assertNotNull(log.getOffset());
    }

    @Test
    public void startingOffset() throws Throwable {
        LogTailOptions opts = new LogTailOptions();
        opts.setStartingOffset(1000);
        ILogTail log = server.getLogTail(opts);

        assertThat("log text missing", log.getData().get(0), containsString("Perforce server info"));
        assertThat("log text missing", log.getData().get(0), not(containsString("Perforce db files in")));
        assertThat("log name missing", log.getLogFilePath(), containsString("log.txt"));
        assertNotNull(log.getOffset());
    }


    @Test
    public void blockSize() throws Throwable {
        LogTailOptions opts = new LogTailOptions();
        opts.setBlockSize(8);
        opts.setMaxBlocks(1);
        opts.setStartingOffset(0);
        ILogTail log = server.getLogTail(opts);

        assertThat("log text missing", log.getData().get(0), containsString("Perforce"));
        assertThat("log text missing", log.getData().get(0).length(), equalTo(8));

        assertThat("log name missing", log.getLogFilePath(), containsString("log.txt"));
        assertNotNull(log.getOffset());
    }


    @Test
    public void maxBlocks() throws Throwable {
        LogTailOptions opts = new LogTailOptions();
        opts.setMaxBlocks(100);
        opts.setBlockSize(10);
        opts.setStartingOffset(0);
        ILogTail log = server.getLogTail(opts);

        String fullLog = "";

        for (String str : log.getData()) {

            fullLog = fullLog.concat(str);

        }

        assertThat("log text missing", fullLog, containsString("Perforce db files in"));
        assertThat("log name missing", log.getLogFilePath(), containsString("log.txt"));
        assertNotNull(log.getOffset());
    }


    @Test
    public void getters() throws Throwable {
        LogTailOptions opts = new LogTailOptions();

        assertEquals("wrong max blocks", 0, opts.getMaxBlocks());
        assertEquals("wrong offset", -1, opts.getStartingOffset());
        assertEquals("wrong size", 0, opts.getBlockSize());

        opts.setMaxBlocks(100);
        opts.setBlockSize(10);
        opts.setStartingOffset(0);

        assertEquals("wrong max blocks", 100, opts.getMaxBlocks());
        assertEquals("wrong offset", 0, opts.getStartingOffset());
        assertEquals("wrong size", 10, opts.getBlockSize());
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
