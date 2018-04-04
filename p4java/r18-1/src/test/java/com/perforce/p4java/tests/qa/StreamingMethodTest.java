package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.server.ServerFactory.getOptionsServer;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.ConnectionNotConnectedException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.test.TestServer;


public class StreamingMethodTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;


    // our test handlers
    public static class StreamingCallbackHandler implements IStreamingCallback {

        int expectedKey = 0;
        List<Map<String, Object>> results = null;

        public StreamingCallbackHandler(int key, List<Map<String, Object>> results) {

            this.expectedKey = key;
            this.results = results;

        }

        public boolean startResults(int key) throws P4JavaException {

            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }

            return true;

        }

        public boolean endResults(int key) throws P4JavaException {

            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }

            return true;

        }

        public boolean handleResult(Map<String, Object> resultMap, int key) throws P4JavaException {

            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }

            if (resultMap == null) {
                fail("null result map in handleResult");
            }

            results.add(resultMap);

            return true;

        }
    }

    ;

    public static class ExceptingCallbackHandler extends StreamingCallbackHandler {

        public ExceptingCallbackHandler(int key, List<Map<String, Object>> results) {
            super(key, results);
        }

        @Override
        public boolean handleResult(Map<String, Object> resultMap, int key) throws P4JavaException {

            throw new RequestException("message", key);

        }

    }

    // simple setup with one file and a couple clients
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

        h.createClient(server, "client2");
        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "StreamingMethodTest");
    }

    // we should get some clients if this is working
    @Test
    public void basic() throws Throwable {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        StreamingCallbackHandler handler = new StreamingCallbackHandler(123, results);
        server.execStreamingMapCommand("clients", null, null, handler, 123);

        assertEquals(2, handler.results.size());
    }

    // verify that we can get an exception
    @Test(expected = RequestException.class)
    public void badCommandException() throws Throwable {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        StreamingCallbackHandler handler = new StreamingCallbackHandler(124, results);
        server.execStreamingMapCommand("asdasf", new String[]{}, null, handler, 124);

        fail("should not reach this point");
    }

    // verify that we can get an exception
    @Test(expected = ConnectionNotConnectedException.class)
    public void badServerException() throws Throwable {
        IOptionsServer server2 = getOptionsServer("p4java://imaginaryfriend:1666", null);

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        StreamingCallbackHandler handler = new StreamingCallbackHandler(124, results);
        server2.execStreamingMapCommand("changes", new String[]{}, null, handler, 124);

        fail("should not reach this point");
    }

    // currently there is no way to get information on exceptions thrown inside the handlers
    @Ignore
    public void handlerExceptionHandling() {

        try {

            List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
            StreamingCallbackHandler handler = new StreamingCallbackHandler(125, results);
            server.execStreamingMapCommand("branch", new String[]{"newBranch"}, null, handler, 125);

        } catch (Throwable t) {

            h.error(t);

        }
    }

    // verify that we can get an exception
    @Test(expected = NullPointerError.class)
    public void nullCommand() throws Throwable {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        StreamingCallbackHandler handler = new StreamingCallbackHandler(124, results);
        server.execStreamingMapCommand(null, new String[]{}, null, handler, 124);

        fail("should not reach this point");
    }

    // verify that we can get an exception
    @Test(expected = NullPointerError.class)
    public void nullHandlerCommand() throws Throwable {
        server.execStreamingMapCommand("branches", null, null, null, 124);

        fail("should not reach this point");
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

}