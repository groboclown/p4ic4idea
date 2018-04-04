/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.option.server.GetCountersOptions;
import com.perforce.p4java.option.server.GetKeysOptions;
import com.perforce.p4java.option.server.KeyOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the 'p4 keys -e[nameFilter] -e[nameFilter] -e[nameFilter] ...'
 */
@Jobs({"job062715"})
@TestId("Dev131_KeysFiltersTest")
public class KeysFiltersTest extends P4JavaTestCase {
    /**
     * @BeforeClass annotation to a method to be run before all the tests in a
     * class.
     */
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        server = getServerAsSuper();
        assertNotNull(server);
    }

    /**
     * @AfterClass annotation to a method to be run after all the tests in a
     * class.
     */
    @AfterClass
    public static void oneTimeTearDown() {
        afterEach(server);
    }

    /**
     * Test processing multiple filters.
     */
    @Test
    public void testProcessingFilters() {

        String[] filters = new String[]{"abc", "xyz", "best", "worst"};

        try {
            GetKeysOptions opts = new GetKeysOptions();
            opts.setNameFilters(filters);
            opts.setNameFilter("blah");

            List<String> options = opts.processOptions(server);
            assertNotNull(options);

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test the 'p4 keys -e[nameFilter] -e[nameFilter] -e[nameFilter] ...'
     */
    @Test
    public void testGetKeysWithFilters() {

        String result = null;

        try {
            // Set keys
            for (int i = 0; i < 3; i++) {
                result = server.setKey("Xtestkey" + i, "10" + i,
                        new KeyOptions());
                assertNotNull(result);
            }
            for (int i = 0; i < 3; i++) {
                result = server.setKey("Ytestkey" + i, "10" + i,
                        new KeyOptions());
                assertNotNull(result);
            }
            for (int i = 0; i < 3; i++) {
                result = server.setKey("Ztestkey" + i, "10" + i,
                        new KeyOptions());
                assertNotNull(result);
            }

            // Get max number (9) of keys with 3 filters
            Map<String, String> counters = server
                    .getCounters(new GetCountersOptions()
                            .setUndocCounter(true)
                            .setMaxResults(9)
                            .setNameFilters(
                                    new String[]{
                                            "Xtestkey*",
                                            "Ytestkey*",
                                            "Ztestkey*"}));

            assertNotNull(counters);
            assertEquals(9, counters.size());

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }
}
