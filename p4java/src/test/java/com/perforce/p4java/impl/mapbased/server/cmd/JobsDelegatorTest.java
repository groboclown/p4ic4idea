package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.JOBS;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.Before;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetJobsOptions;

/**
 * Tests the JobsDelegator.
 */
public class JobsDelegatorTest extends AbstractP4JavaUnitTest {
    
    /** The jobs delegator. */
    private JobsDelegator jobsDelegator;

    /** Example values. */
    private static final String FILE_SPEC = "//depot/main/revisions.h";
    /** Simple matcher. */
    private static final CommandLineArgumentMatcher SIMPLE_MATCHER = new CommandLineArgumentMatcher(
            new String[] { FILE_SPEC });
    
    /** Matcher for use with params. */
    private static final CommandLineArgumentMatcher PARAM_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-i", "-l", "-m1", "-r", FILE_SPEC });
    /** Example value. */
    private static final String TEST_JOB = "job001234";

    /** Example value. */
    private static final String TEST_USER = "testuser";

    /** Example value. */
    private static final String TEST_DESC = "test description";

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        jobsDelegator = new JobsDelegator(server);
    }

    /**
     * Test jobs opt connection exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testJobsOptConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        jobsDelegator.getJobs(specs, new GetJobsOptions());
    }

    /**
     * Test jobs opt access exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testJobsOptAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        jobsDelegator.getJobs(specs, new GetJobsOptions());
    }

    /**
     * Test jobs opt request exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testJobsOptRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        jobsDelegator.getJobs(specs, new GetJobsOptions());
    }

    /**
     * Test jobs opt p4 java exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testJobsOptP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        jobsDelegator.getJobs(specs, new GetJobsOptions());
    }

    /**
     * Test jobs connection exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testJobsConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(PARAM_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        jobsDelegator.getJobs(specs, 1, true, true, true, null);
    }

    /**
     * Test jobs access exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testJobsAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(PARAM_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        jobsDelegator.getJobs(specs, 1, true, true, true, null);
    }

    /**
     * Test jobs request exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testJobsRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(PARAM_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        jobsDelegator.getJobs(specs, 1, true, true, true, null);
    }

    /**
     * Test jobs p4 java exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testJobsP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(PARAM_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        jobsDelegator.getJobs(specs, 1, true, true, true, null);
    }

    /**
     * Test jobs opt.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testJobsOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenReturn(buildValidGetResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        List<IJob> jobs = jobsDelegator.getJobs(specs, new GetJobsOptions());
        verify(server).execMapCmdList(eq(JOBS.toString()), argThat(SIMPLE_MATCHER), eq(null));
        assertJobs(jobs);
    }

    /**
     * Test jobs specified options. Verify that the options get expanded out
     * to the correct server call.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testJobsSpecifiedOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(PARAM_MATCHER), eq(null)))
                .thenReturn(buildValidGetResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        GetJobsOptions options = new GetJobsOptions();
        // Setting these options should expand out to the same param call
        options.setMaxJobs(1).setLongDescriptions(true).setReverseOrder(true)
                .setIncludeIntegrated(true);
        List<IJob> jobs = jobsDelegator.getJobs(specs, options);
        verify(server).execMapCmdList(eq(JOBS.toString()), argThat(PARAM_MATCHER), eq(null));
        assertJobs(jobs);
    }

    /**
     * Test jobs.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testJobs() throws P4JavaException {
        when(server.execMapCmdList(eq(JOBS.toString()), argThat(PARAM_MATCHER), eq(null)))
                .thenReturn(buildValidGetResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(FILE_SPEC);
        List<IJob> jobs = jobsDelegator.getJobs(specs, 1, true, true, true, null);
        verify(server).execMapCmdList(eq(JOBS.toString()), argThat(PARAM_MATCHER), eq(null));
        assertJobs(jobs);
    }

    /**
     * Builds the valid result map for get.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidGetResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("Status", "open");
        result.put("Description", TEST_DESC);
        result.put("Job", TEST_JOB);
        result.put("User", TEST_USER);
        results.add(result);
        return results;
    }

    /**
     * Assert jobs.
     *
     * @param jobs
     *            the jobs
     */
    private void assertJobs(final List<IJob> jobs) {
        assertNotNull(jobs);
        assertEquals(1, jobs.size());
        IJob job = jobs.get(0);
        assertEquals(TEST_JOB, job.getId());
        assertEquals(TEST_DESC, job.getDescription());
    }
}