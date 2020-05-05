package com.perforce.p4java.tests.dev.unit.bug.r162;

import com.perforce.p4java.core.IJob;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUnicodeFileTypeOnNonUnicodeEnabledServerTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;


public class Job089596Test extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job089596Test.class.getSimpleName());

    private static String jobName = "job" + System.currentTimeMillis();
    private static final String INITIAL_JOB_DESCRIPTION = "Temporary test job(" + jobName + ") for 'Job089596Test";
    private static final String UPDATE_JOB_DESCRIPTION = INITIAL_JOB_DESCRIPTION + " - changed";


    @BeforeClass
    public static void beforeAll() throws Exception {
        setupServer(p4d.getRSHURL(), userName, password, true, null);
    }

    @Test
    public void testUpdateJobDescription() throws Exception {
        // given
        IJob newJob = server.createJob(createJobMap());
        String actualDescription = retrieveJobDescription(newJob.getId());
        assertThat(actualDescription, is(INITIAL_JOB_DESCRIPTION + "\n"));

        // when
        newJob.setDescription(UPDATE_JOB_DESCRIPTION);
        server.updateJob(newJob);

        //then
        actualDescription = retrieveJobDescription(newJob.getId());
        assertThat(actualDescription, is(UPDATE_JOB_DESCRIPTION + "\n"));
    }


    private Map<String, Object> createJobMap() {
        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put("Job", jobName);
        jobMap.put("Description", INITIAL_JOB_DESCRIPTION);
        jobMap.put("Status", "open");
        jobMap.put("User", getUserName());

        return jobMap;
    }

    private String retrieveJobDescription(String jobId) throws Exception {
        IJob job = server.getJob(jobId);
        return job.getDescription();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
