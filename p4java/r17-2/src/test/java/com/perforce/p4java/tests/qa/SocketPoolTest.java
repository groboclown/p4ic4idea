package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.server.ServerFactory.getOptionsServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.test.TestServer;

public class SocketPoolTest {

    private static TestServer ts = null;
    private static Helper h = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();
    }

    public String getPid(String s) {
        String pid = null;
        int pidIndex = s.indexOf("pid");
        pid = s.substring(pidIndex, pidIndex + 9);
        return pid;
    }

    /*
	 * Test enabling of socket pool
	 *
	 * Sets socketPoolsize=5, logs 2 jobs and checks p4d server log that both
	 * jobs are created with the same process id.
	 */
    @Test
    public void testSocketPoolSize() throws Throwable {
        String programName = "job000001";
        String programName2 = "job000002";
        String pid1 = null;
        String pid2 = null;

        Properties properties = new Properties();
        properties.setProperty("socketPoolSize", "5");

        IOptionsServer server = getOptionsServer(
                "p4java://localhost:" + ts.getPort(), properties);

        server.connect();
        IUser user = server.getUser("user");
        h.addJob(server, user, "test job1");
        h.addJob(server, user, "test job2");

        boolean programNameFound = false;

        BufferedReader reader = new BufferedReader(new FileReader(
                ts.getLog()));
        String line = null;

        while ((line = reader.readLine()) != null) {

            if (line.contains(programName)) {
                pid1 = getPid(line);
                programNameFound = true;
            }

            if (line.contains(programName2)) {
                pid2 = getPid(line);
                programNameFound = true;
                break;
            }
        }

        reader.close();

        assertEquals(pid1, pid2);
        assertTrue("Program name property not found in log file.",
                programNameFound);
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

}
