package com.perforce.p4java.tests.dev.unit.features111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Basic sanity tests for the new Streaming interface. Not related
 * at all to 'streams'.
 */
@TestId("Features102_StreamingMethodsTest")
public class StreamingMethodsTest extends P4JavaTestCase {
    private static final int TIME_OUT_IN_SECONDS = 60;

    @BeforeClass
    public static void beforeEach() throws Exception {
        Properties rpcTimeOutProperties = configRpcTimeOut("StreamingMethodsTest", TIME_OUT_IN_SECONDS);
        server = getServer(serverUrlString, rpcTimeOutProperties, null, null);
    }

    @AfterClass
    public static void afterEach() throws Exception {
        afterEach(server);
    }

    /**
     * Just test that nothing goes really obviously wrong; otherwise,
     * no real checks done...
     */
    @Test
    public void testBasicStreaming() throws Exception {
        final String depotPath = "//depot/p4java_stream/...";
        executeStreamCmd(server, "files", new String[]{depotPath});
        executeStreamCmd(server, "jobs", null);
        executeStreamCmd(server, "clients", null);
        executeStreamCmd(server, "client", new String[]{"-o", "Job036870TestClient6570"});
    }

    private void executeStreamCmd(IOptionsServer server, String cmdName, String[] cmdArgs) throws P4JavaException {
        int key = this.getRandomInt();
        SimpleCallbackHandler handler = new SimpleCallbackHandler(this, key);
        server.execStreamingMapCommand(cmdName, cmdArgs, null, handler, key);
    }

    /**
     * Test a canonical client listing -- sanity check only, really.
     */
    @Test
    public void testClientsStreamingCommand() throws Exception {
        List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
        int key = this.getRandomInt();
        server = getServer();
        ListCallbackHandler handler = new ListCallbackHandler(this, key, resultsList);
        server.execStreamingMapCommand("clients", null, null, handler, key);

        List<IClientSummary> clients = server.getClients(null);
        assertNotNull(clients);
        assertEquals("number of clients retrieved differed",
                clients.size(), resultsList.size());
    }

    /**
     * Test a single client retrieval using both streaming and non-streaming
     * methods.
     */
    @Test
    public void testClientRetrievalWithStreaming() throws Exception {
        final String clientName = "p4java_stream_dev";
        List<Map<String, Object>> resultsList = new ArrayList<>();
        int key = this.getRandomInt();
        IClient client01 = server.getClient(clientName);
        assertNotNull("null client returned", client01);
        assertEquals(clientName, client01.getName());

        ListCallbackHandler handler = new ListCallbackHandler(this, key, resultsList);
        server.execStreamingMapCommand("client", new String[]{"-o", clientName}, null, handler, key);
        assertEquals("retrieved unexpected number of results", 1, resultsList.size());
        Client client02 = new Client(server, resultsList.get(0));

        // Just do a cursory check that things are looking OK; some of these
        // may fail under pathological circumstances:

        assertEquals("client names differed", client01.getName(), client02.getName());
        assertEquals("client access dates differed", client01.getAccessed(), client02.getAccessed());
        assertEquals("client descriptions differed", client01.getDescription(), client02.getDescription());
        assertEquals("client hosts differed", client01.getHostName(), client02.getHostName());
        assertEquals("client owner name differed", client01.getOwnerName(), client02.getOwnerName());
        assertEquals("client roots differed", client01.getRoot(), client02.getRoot());
    }

    private void fails(String msg) {
        fail(msg);
    }

    public static class SimpleCallbackHandler implements IStreamingCallback {
        int expectedKey = 0;
        StreamingMethodsTest testCase = null;

        public SimpleCallbackHandler(StreamingMethodsTest testCase, int key) {
            if (testCase == null) {
                throw new NullPointerException(
                        "null testCase passed to CallbackHandler constructor");
            }
            this.expectedKey = key;
            this.testCase = testCase;
        }

        public boolean startResults(int key) throws P4JavaException {
            if (key != this.expectedKey) {
                this.testCase.fails("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            return true;
        }

        public boolean endResults(int key) throws P4JavaException {
            if (key != this.expectedKey) {
                this.testCase.fails("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            return true;
        }

        public boolean handleResult(Map<String, Object> resultMap, int key)
                throws P4JavaException {
            if (key != this.expectedKey) {
                this.testCase.fails("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            if (resultMap == null) {
                this.testCase.fails("null result map in handleResult");
            }
            return true;
        }
    }

    public static class ListCallbackHandler implements IStreamingCallback {

        int expectedKey = 0;
        StreamingMethodsTest testCase = null;
        List<Map<String, Object>> resultsList = null;

        public ListCallbackHandler(StreamingMethodsTest testCase, int key,
                                   List<Map<String, Object>> resultsList) {
            this.expectedKey = key;
            this.testCase = testCase;
            this.resultsList = resultsList;
        }

        public boolean startResults(int key) throws P4JavaException {
            if (key != this.expectedKey) {
                this.testCase.fails("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            return true;
        }

        public boolean endResults(int key) throws P4JavaException {
            if (key != this.expectedKey) {
                this.testCase.fails("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            return true;
        }

        public boolean handleResult(Map<String, Object> resultMap, int key)
                throws P4JavaException {
            if (key != this.expectedKey) {
                this.testCase.fails("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            if (resultMap == null) {
                this.testCase.fails("null resultMap passed to handleResult callback");
            }
            this.resultsList.add(resultMap);
            return true;
        }


        public List<Map<String, Object>> getResultsList() {
            return this.resultsList;
        }
    }
}
