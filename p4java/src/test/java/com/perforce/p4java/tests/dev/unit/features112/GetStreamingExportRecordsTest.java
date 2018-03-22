/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 export' streaming command.
 */
@Jobs({"job037798"})
@TestId("Dev112_GetStreamingExportRecordsTest")
public class GetStreamingExportRecordsTest extends P4JavaTestCase {

    private static IClient client = null;
    private Integer journal = 0;

    public static class SimpleCallbackHandler implements IStreamingCallback {
        int expectedKey = 0;
        GetStreamingExportRecordsTest testCase = null;

        public SimpleCallbackHandler(GetStreamingExportRecordsTest testCase,
                                     int key) {
            if (testCase == null) {
                throw new NullPointerException(
                        "null testCase passed to CallbackHandler constructor");
            }
            this.expectedKey = key;
            this.testCase = testCase;
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

        public boolean handleResult(Map<String, Object> resultMap, int key)
                throws P4JavaException {
            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            if (resultMap == null) {
                fail("null result map in handleResult");
            }
            return true;
        }
    }

    ;

    public static class ListCallbackHandler implements IStreamingCallback {

        int expectedKey = 0;
        GetStreamingExportRecordsTest testCase = null;
        List<Map<String, Object>> resultsList = null;

        public ListCallbackHandler(GetStreamingExportRecordsTest testCase,
                                   int key, List<Map<String, Object>> resultsList) {
            this.expectedKey = key;
            this.testCase = testCase;
            this.resultsList = resultsList;
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

        public boolean handleResult(Map<String, Object> resultMap, int key)
                throws P4JavaException {
            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            if (resultMap == null) {
                fail("null resultMap passed to handleResult callback");
            }
            this.resultsList.add(resultMap);
            return true;
        }

        public List<Map<String, Object>> getResultsList() {
            return this.resultsList;
        }
    }

    ;

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @BeforeClass
    public static void beforeAll() throws Exception {
        // initialization code (before each test).
        server = getServerAsSuper();
        assertNotNull(server);
        client = server.getClient("p4TestUserWS");
        assertNotNull(client);
        server.setCurrentClient(client);
    }

    @Before
    public void beforeEach() throws Exception{
        journal = new Integer(server.getCounter("journal"));
    }

    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }

    /**
     * Test 'p4 export' streaming command - no skip.
     */
    @Test
    public void testNoSkip() {

        try {

            List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
            int key = this.getRandomInt();
            ListCallbackHandler handler = new ListCallbackHandler(this, key,
                    resultsList);

            server.execStreamingMapCommand("export", new String[]{"-l1000000",
                    "-j" + journal, "-Ftable=db.traits"}, null, handler, key);

            assertNotNull(resultsList);
            assertTrue(resultsList.size() > 0);

            Map<String, Object> dataMap = resultsList.get(0);
            assertNotNull(dataMap);
            Object dataObject = dataMap.get("TTvalue");
            assertNotNull(dataObject);
            assertTrue(dataObject instanceof String);

        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    /**
     * Test 'p4 export' streaming command - skip field range.
     */
    @Test
    public void testSkipFieldRange() {

        try {

            List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
            int key = this.getRandomInt();
            ListCallbackHandler handler = new ListCallbackHandler(this, key,
                    resultsList);

            HashMap<String, Object> inMap = new HashMap<String, Object>();
            Map<String, Object> skipParams = new HashMap<String, Object>();
            skipParams.put("startField", "op");
            skipParams.put("stopField", "func");
            inMap.put(CmdSpec.EXPORT.toString(), skipParams);

            server.execStreamingMapCommand("export", new String[]{"-l1000000",
                    "-j" + journal, "-Ftable=db.traits"}, inMap, handler, key);

            assertNotNull(resultsList);
            assertTrue(resultsList.size() > 0);

            Map<String, Object> dataMap = resultsList.get(0);
            assertNotNull(dataMap);
            Object dataObject = dataMap.get("TTvalue");
            assertNotNull(dataObject);
            assertTrue(dataObject instanceof byte[]);

        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    /**
     * Test 'p4 export' streaming command - skip field pattern.
     */
    @Test
    public void testSkipFieldPattern() {

        try {

            List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
            int key = this.getRandomInt();
            ListCallbackHandler handler = new ListCallbackHandler(this, key,
                    resultsList);

            HashMap<String, Object> inMap = new HashMap<String, Object>();
            Map<String, Object> skipParams = new HashMap<String, Object>();
            skipParams.put("fieldPattern", "^[A-Z]{2}\\w+");
            inMap.put(CmdSpec.EXPORT.toString(), skipParams);

            server.execStreamingMapCommand("export", new String[]{"-l1000000",
                    "-j" + journal, "-Ftable=db.traits"}, inMap, handler, key);

            assertNotNull(resultsList);
            assertTrue(resultsList.size() > 0);

            Map<String, Object> dataMap = resultsList.get(0);
            assertNotNull(dataMap);
            Object dataObject = dataMap.get("TTvalue");
            assertNotNull(dataObject);
            assertTrue(dataObject instanceof byte[]);

        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }
}
