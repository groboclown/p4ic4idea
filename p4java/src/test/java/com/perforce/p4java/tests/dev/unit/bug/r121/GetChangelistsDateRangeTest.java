package com.perforce.p4java.tests.dev.unit.bug.r121;

import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Test getting changelists from a date range.
 */
@Jobs({"job053580"})
@TestId("Dev121_GetChangelistsDateRangeTest")
public class GetChangelistsDateRangeTest extends P4JavaRshTestCase {
    
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetChangelistsDateRangeTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() throws Exception {
        setupServer(p4d.getRSHURL(), userName, password, true, props);
        client = getClient(server);
    }

    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @After
    public void tearDown() {
        // cleanup code (after each test).
        if (server != null) {
            this.endServerSession(server);
        }
    }

    /**
     * Test getting changelists from a date range.
     */
    @Test
    public void testGetChangelistsFromDateRange() throws Exception {
        // File path date range
        //String path = "//depot/...@2014/03/01,@now";
        String path = oneWeekBefore() + ",@now";
        List<IFileSpec> files = FileSpecBuilder.makeFileSpecList(path);
        List<IChangelistSummary> changeSummaries = server.getChangelists(files, new GetChangelistsOptions());
        assertNotNull(changeSummaries);
    }

    private String oneWeekBefore() {
        FastDateFormat dateFormat = FastDateFormat.getInstance("@yyyy/MM/dd");

        Calendar now = Calendar.getInstance();
        now.add(Calendar.WEEK_OF_YEAR, -1);

        return dateFormat.format(now);
    }
}
