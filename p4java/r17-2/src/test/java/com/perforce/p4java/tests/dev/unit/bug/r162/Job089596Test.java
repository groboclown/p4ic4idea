package com.perforce.p4java.tests.dev.unit.bug.r162;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.core.IJob;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

@RunWith(JUnitPlatform.class)
public class Job089596Test extends P4JavaTestCase {
    private static String jobName = "job" + System.currentTimeMillis();
    private static final String INITIAL_JOB_DESCRIPTION = "Temporary test job(" + jobName + ") for 'Job089596Test";
    private static final String UPDATE_JOB_DESCRIPTION = INITIAL_JOB_DESCRIPTION + " - changed";


    @BeforeAll
    public static void beforeAll() throws Exception {
        server = getServer();
        assertThat(server, notNullValue());

        server.registerCallback(createCommandCallback());
        server.connect();
        setUtf8CharsetIfServerSupportUnicode(server);
        server.setUserName(getUserName());

        server.login(getPassword(), new LoginOptions());
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

    @AfterAll
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
