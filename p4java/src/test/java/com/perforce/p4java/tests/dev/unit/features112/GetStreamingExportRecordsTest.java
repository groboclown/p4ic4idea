/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.tests.dev.UnitTestDevServerManager;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 export' streaming command.
 */
@Jobs({"job037798"})
@TestId("Dev112_GetStreamingExportRecordsTest")
public class GetStreamingExportRecordsTest extends P4JavaTestCase {
    // p4ic4idea: use local server
    @BeforeClass
    public static void oneTimeSetUp() {
        UnitTestDevServerManager.INSTANCE.startTestClass();
    }
    @AfterClass
    public static void oneTimeTearDown() {
        UnitTestDevServerManager.INSTANCE.endTestClass();
    }

    private static IClient client = null;
    private Integer journal = 0;

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
