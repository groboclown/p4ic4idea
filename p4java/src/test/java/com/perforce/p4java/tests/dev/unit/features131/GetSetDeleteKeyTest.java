/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.option.server.KeyOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the 'p4 key name'
 * Test the 'p4 key name value'
 * Test the 'p4 key -d name'
 * Test the 'p4 key -i name'
 */
@Jobs({"job062031"})
@TestId("Dev131_GetSetDeleteKeyTest")
public class GetSetDeleteKeyTest extends P4JavaTestCase {

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @BeforeClass
    public static void beforeAll() throws Exception {
        // initialization code (before each test).
        server = getServerAsSuper();
        assertNotNull(server);
    }

    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @AfterClass
    public static void afterAll() {
       afterEach(server);
    }

    /**
     * Test the 'p4 key name value'
     * Test the 'p4 key name'
     */
    @Test
    public void testSetAndGetKeys() {

        String result = null;

        try {
            // Set 10 keys
            for (int i = 0; i < 10; i++) {
                result = server.setKey("testkey" + i, "" + i,
                        new KeyOptions());
                assertNotNull(result);
            }

            // Get 10 keys
            for (int i = 0; i < 10; i++) {
                result = server.getKey("testkey" + i);
                assertNotNull(result);
                assertEquals("" + i, result);
            }

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test the 'p4 key -i name'
     */
    @Test
    public void testIncrementKeys() {

        String result = null;

        try {
            // Set 10 keys
            for (int i = 100; i < 110; i++) {
                result = server.setKey("testkey" + i, "" + i,
                        new KeyOptions());
                assertNotNull(result);
            }

            // Increment 10 keys
            for (int i = 100; i < 110; i++) {
                result = server.setKey("testkey" + i, null,
                        new KeyOptions().setIncrementKey(true));
            }

            // Get 10 keys
            for (int i = 100; i < 110; i++) {
                result = server.getKey("testkey" + i);
                assertNotNull(result);
                assertEquals("" + (i + 1), result);
            }

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test the 'p4 key -d name'
     */
    @Test
    public void testDeleteKeys() {

        String result = null;

        try {
            // Set 10 keys
            for (int i = 200; i < 210; i++) {
                result = server.setKey("testkey" + i, "" + i,
                        new KeyOptions());
                assertNotNull(result);
            }

            // Delete 10 keys
            for (int i = 200; i < 210; i++) {
                result = server.deleteKey("testkey" + i);
            }

            // Get 10 keys, should be "0"
            for (int i = 200; i < 210; i++) {
                result = server.getKey("testkey" + i);
                assertNotNull(result);
                assertEquals("0", result);
            }

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

}
