package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.JOB;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Job;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * Tests the JobDelegator.
 */
public class JobDelegatorTest extends AbstractP4JavaUnitTest {
    
    /** The job delegator. */
    private JobDelegator jobDelegator;
    
    /** Example value. */
    private static final String TEST_JOB = "job001234";
    
    /** Example value. */
    private static final String TEST_USER = "testuser";
    
    /** Example value. */
    private static final String TEST_DESC = "test description";
    /** Create matcher. */
    private static final CommandLineArgumentMatcher CREATE_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-i" });
    
    /** Delete matcher. */
    private static final CommandLineArgumentMatcher DELETE_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-d", TEST_JOB });
    
    /** Get matcher. */
    private static final CommandLineArgumentMatcher GET_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-o", TEST_JOB });
    
    /** Update matcher. */
    private static final CommandLineArgumentMatcher UPDATE_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-i" });
    
    /** Example value. */
    private static final Map<String, Object> CREATE_FIELD_MAP = new HashMap<>();
    
    /** Example value. */
    private static final Map<String, Object> UPDATE_FIELD_MAP = new HashMap<>();
    
    /** The update job. */
    private IJob updateJob;

    static {
        CREATE_FIELD_MAP.put("Status", "open");
        CREATE_FIELD_MAP.put("Description", TEST_DESC);
        CREATE_FIELD_MAP.put("User", TEST_USER);
        CREATE_FIELD_MAP.put("Job", "new");

        UPDATE_FIELD_MAP.put("Job", TEST_JOB);
        UPDATE_FIELD_MAP.put("Status", "open");
        UPDATE_FIELD_MAP.put("Description", TEST_DESC);
        UPDATE_FIELD_MAP.put("User", TEST_USER);
    }

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        jobDelegator = new JobDelegator(server);
        updateJob = new Job(server, UPDATE_FIELD_MAP);
    }

    /**
     * Test job create null.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = NullPointerException.class)
    public void testJobCreateNull() throws P4JavaException {
        jobDelegator.createJob(null);
    }
    
    /**
     * Test job update null.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = NullPointerException.class)
    public void testJobUpdateNull() throws P4JavaException {
        jobDelegator.updateJob(null);
    }
    
    /**
     * Test job get null.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = NullPointerException.class)
    public void testJobGetNull() throws P4JavaException {
        jobDelegator.getJob(null);
    }
    
    /**
     * Test job get empty.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testJobGetEmpty() throws P4JavaException {
        jobDelegator.getJob("");
    }
    
    /**
     * Test job delete null.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = NullPointerException.class)
    public void testJobDeleteNull() throws P4JavaException {
        jobDelegator.deleteJob(null);
    }
    
    /**
     * Test job delete empty.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testJobDeleteEmpty() throws P4JavaException {
        jobDelegator.deleteJob("");
    }
    
    /**
     * Test create job connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testJobCreateConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(CREATE_MATCHER),
                eq(CREATE_FIELD_MAP))).thenThrow(ConnectionException.class);
        jobDelegator.createJob(CREATE_FIELD_MAP);
    }

    /**
     * Test create job access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testJobCreateAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(CREATE_MATCHER),
                eq(CREATE_FIELD_MAP))).thenThrow(AccessException.class);
        jobDelegator.createJob(CREATE_FIELD_MAP);
    }

    /**
     * Test create job request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testJobCreateRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(CREATE_MATCHER),
                eq(CREATE_FIELD_MAP))).thenThrow(RequestException.class);
        jobDelegator.createJob(CREATE_FIELD_MAP);
    }

    /**
     * Test create job P4Java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testJobCreateP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(CREATE_MATCHER),
                eq(CREATE_FIELD_MAP))).thenThrow(P4JavaException.class);
        jobDelegator.createJob(CREATE_FIELD_MAP);
    }

    /**
     * Test delete job connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testJobDeleteConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        jobDelegator.deleteJob(TEST_JOB);
    }

    /**
     * Test delete job access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testJobDeleteAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        jobDelegator.deleteJob(TEST_JOB);
    }

    /**
     * Test delete job request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testJobDeleteRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        jobDelegator.deleteJob(TEST_JOB);
    }

    /**
     * Test delete job P4Java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testJobDeleteP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        jobDelegator.deleteJob(TEST_JOB);
    }

    /**
     * Test get job connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testJobGetConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(GET_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        jobDelegator.getJob(TEST_JOB);
    }

    /**
     * Test get job access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testJobGetAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(GET_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        jobDelegator.getJob(TEST_JOB);
    }

    /**
     * Test get job request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testJobGetRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(GET_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        jobDelegator.getJob(TEST_JOB);
    }

    /**
     * Test get job P4Java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testJobGetP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(GET_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        jobDelegator.getJob(TEST_JOB);
    }

    /**
     * Test update job connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testJobUpdateConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(UPDATE_MATCHER),
                eq(UPDATE_FIELD_MAP))).thenThrow(ConnectionException.class);
        jobDelegator.updateJob(updateJob);
    }

    /**
     * Test update job access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testJobUpdateAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(UPDATE_MATCHER),
                eq(UPDATE_FIELD_MAP))).thenThrow(AccessException.class);
        jobDelegator.updateJob(updateJob);
    }

    /**
     * Test update job request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testJobUpdateRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(UPDATE_MATCHER),
                eq(UPDATE_FIELD_MAP))).thenThrow(RequestException.class);
        jobDelegator.updateJob(updateJob);
    }

    /**
     * Test update job P4Java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testJobUpdateP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(UPDATE_MATCHER),
                eq(UPDATE_FIELD_MAP))).thenThrow(P4JavaException.class);
        jobDelegator.updateJob(updateJob);
    }

    /**
     * Test job create.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testJobCreate() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(CREATE_MATCHER),
                eq(CREATE_FIELD_MAP))).thenReturn(buildValidCreateResultMap());
        when(server.execMapCmdList(eq(JOB.toString()), argThat(GET_MATCHER), eq(null)))
                .thenReturn(buildValidGetResultMap());
        IJob job = jobDelegator.createJob(CREATE_FIELD_MAP);
        verify(server).execMapCmdList(eq(JOB.toString()), argThat(CREATE_MATCHER),
                eq(CREATE_FIELD_MAP));
        verify(server).execMapCmdList(eq(JOB.toString()), argThat(GET_MATCHER), eq(null));
        assertJob(job);
    }

    /**
     * Test job get.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testJobGet() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(GET_MATCHER), eq(null)))
                .thenReturn(buildValidGetResultMap());
        IJob job = jobDelegator.getJob(TEST_JOB);
        verify(server).execMapCmdList(eq(JOB.toString()), argThat(GET_MATCHER), eq(null));
        assertJob(job);
    }

    /**
     * Test job delete.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testJobDelete() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenReturn(buildValidDeleteResultMap());
        String result = jobDelegator.deleteJob(TEST_JOB);
        verify(server).execMapCmdList(eq(JOB.toString()), argThat(DELETE_MATCHER), eq(null));
        assertTrue(result.startsWith("Job " + TEST_JOB + " deleted"));
    }

    /**
     * Test job update.
     *
     * @throws P4JavaException the p4 java exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testJobUpdate() throws P4JavaException {
        when(server.execMapCmdList(eq(JOB.toString()), argThat(GET_MATCHER), eq(null)))
                .thenReturn(buildValidGetResultMap());
        when(server.execMapCmdList(eq(JOB.toString()), argThat(UPDATE_MATCHER),
                any(Map.class))).thenReturn(buildValidUpdateNoChangeResultMap());
        IJob job = jobDelegator.getJob(TEST_JOB);
        job.setDescription("changed");
        String result = jobDelegator.updateJob(job);
        // TODO This should result in a change on the server but in reality 
        // because of https://jira.perforce.com:8443/browse/P4JAVA-1091 we mock
        // it is not changed to agree with the current behaviour.
        assertTrue(result.startsWith("Job " + TEST_JOB + " not changed"));
    }

    /**
     * Assert job.
     *
     * @param job the job
     */
    private void assertJob(final IJob job) {
        assertEquals(TEST_JOB, job.getId());
        assertEquals(TEST_DESC, job.getDescription());
    }

    /**
     * Builds the valid result map for create.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidCreateResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("fmt0", "Job %job% saved.");
        result.put("code0", "285219082");
        result.put("job", TEST_JOB);
        results.add(result);
        return results;
    }

    /**
     * Builds the valid result map for delete.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidDeleteResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("fmt0", "Job %job% deleted.");
        result.put("code0", "285219084");
        result.put("job", TEST_JOB);
        results.add(result);
        return results;
    }
    
    /**
     * Builds the valid result map for update not changed.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidUpdateNoChangeResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("fmt0", "Job %job% not changed.");
        result.put("code0", "285219083");
        result.put("job", TEST_JOB);
        results.add(result);
        return results;
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
}