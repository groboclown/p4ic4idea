// NOT FULLY IMPLEMENTED

package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.PropertyDefs.PROG_NAME_KEY;
import static com.perforce.p4java.server.ServerFactory.getServer;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.test.TestServer;


public class ServerFactoryTest {

    private static TestServer ts = null;
    private static Helper h = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();
    }


    // PROPERTIES
    @Test
    public void properties() throws Throwable {
        String programName = "program_name";
        Properties properties = new Properties();
        properties.put(PROG_NAME_KEY, programName);

        IServer server = getServer("p4java://localhost:" + ts.getPort(), properties);

        server.connect();

        boolean programNameFound = false;

        BufferedReader reader = new BufferedReader(new FileReader(ts.getLog()));
        String line = null;

        while ((line = reader.readLine()) != null) {

            if (line.contains(programName)) {

                programNameFound = true;
                break;

            }
        }

        reader.close();

        assertTrue("Program name property not found in log file.", programNameFound);
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

}