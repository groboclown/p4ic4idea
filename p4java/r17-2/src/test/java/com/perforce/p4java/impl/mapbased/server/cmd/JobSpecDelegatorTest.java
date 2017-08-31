package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.JOBSPEC;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.core.IJobSpec.IJobSpecField;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * Tests JobSpecDelegator.
 */
public class JobSpecDelegatorTest extends AbstractP4JavaUnitTest {

    /** The job spec delegator. */
    private JobSpecDelegator jobSpecDelegator;
    /** Simple matcher. */
    private static final CommandLineArgumentMatcher SIMPLE_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-o" });

    /** Example value from p4 -p <P4PORT> -ztag jobspec -o. */
    private static final String VALUES0 =
            "Status open/triaged/inprogress/review/fixed/closed/punted/suspended/"
          + "duplicate/obsolete/reopened";

    /** Example value from p4 -p <P4PORT> -ztag jobspec -o. */
    private static final String PRESETS0 = "Status open,fix/fixed";
    /** Example value from p4 -p <P4PORT> -ztag jobspec -o. */
    private static final String FIELDS0 = "101 Job word 32 required";
    /** Example value from p4 -p <P4PORT> -ztag jobspec -o. */
    private static final String FIELDS1 = "102 Status select 10 required";

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        jobSpecDelegator = new JobSpecDelegator(server);
    }

    /**
     * Test job spec connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testJobSpecConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBSPEC.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        jobSpecDelegator.getJobSpec();
    }

    /**
     * Test job spec access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testJobSpecAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBSPEC.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        jobSpecDelegator.getJobSpec();
    }

    /**
     * Test job spec request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testJobSpecRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBSPEC.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        jobSpecDelegator.getJobSpec();
    }

    /**
     * Test job spec p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testJobSpecP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBSPEC.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        jobSpecDelegator.getJobSpec();
    }

    /**
     * Test job spec.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testJobSpec() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBSPEC.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenReturn(buildValidGetResultMap());
        IJobSpec jobSpec = jobSpecDelegator.getJobSpec();
        verify(server).execMapCmdList(eq(JOBSPEC.toString()), argThat(SIMPLE_MATCHER), eq(null));
        List<IJobSpecField> jsfList = jobSpec.getFields();
        assertNotNull(jsfList);
        assertEquals(2, jsfList.size());
        assertField0(jsfList.get(0));
        assertField1(jsfList.get(1));

        Map<String, String> presets = jobSpec.getPresets();
        assertEquals("open,fix/fixed", presets.get("Status"));
        List<String> expectedValues = Arrays
                .asList(new String[] { "open", "triaged", "inprogress", "review", "fixed", "closed",
                        "punted", "suspended", "duplicate", "obsolete", "reopened" });
        Map<String, List<String>> values = jobSpec.getValues();
        assertNotNull(values.get("Status"));
        assertEquals(expectedValues, values.get("Status"));
    }

    /**
     * Assert field0 is as expected.
     *
     * @param field the field
     */
    private void assertField0(final IJobSpecField field) {
        final int code = 101;
        final int length = 32;
        assertEquals("Job", field.getName());
        assertEquals(code, field.getCode());
        assertEquals(length, field.getLength());
        assertEquals("word", field.getDataType());
        assertEquals("required", field.getFieldType());
    }

    /**
     * Assert field1 is as expected.
     *
     * @param field the field
     */
    private void assertField1(final IJobSpecField field) {
        final int code = 102;
        final int length = 10;
        assertEquals("Status", field.getName());
        assertEquals(code, field.getCode());
        assertEquals(length, field.getLength());
        assertEquals("select", field.getDataType());
        assertEquals("required", field.getFieldType());
    }

    /**
     * Builds the valid result map for get spec.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidGetResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("Values0", VALUES0);
        result.put("Presets0", PRESETS0);
        result.put("Fields0", FIELDS0);
        result.put("Fields1", FIELDS1);
        results.add(result);
        return results;
    }
}