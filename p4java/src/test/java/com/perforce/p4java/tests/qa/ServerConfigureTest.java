package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.server.ServerFactory.getOptionsServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.admin.ServerConfigurationValue;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.test.TestServer;

public class ServerConfigureTest {

    private static TestServer ts = null;
    private static Helper h = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();
    }

    /*
	 * Method to set and check config values
	 */
    public void setConfigurable(String configName, String configValue) {

        try {

            IOptionsServer server = getOptionsServer(
                    "p4java://localhost:" + ts.getPort(), null);

            server.connect();
            String status = server.setOrUnsetServerConfigurationValue(configName,
                    configValue);

            boolean configureNameFound = false;

            if (configName.contains("#")) {

            } else {
                assertTrue(status.contains("'" + configName + "'" + " set to "
                        + "'" + configValue + "'"));
            }

            BufferedReader reader = new BufferedReader(new FileReader(
                    ts.getLog()));
            String line = null;

            while ((line = reader.readLine()) != null) {

                if (line.contains("user-configure set " + configName + "="
                        + configValue)) {

                    configureNameFound = true;
                    break;
                }
            }

            reader.close();

            assertTrue("Configuration variable '" + configName + "' and '"
                            + configValue + "' not found in log file.",
                    configureNameFound);

        } catch (Throwable t) {

            h.error(t);

        }

    }

    /*
	 * Method to unset config variable
	 */
    public void unsetConfigurable(String configName) {

        try {

            IOptionsServer server = getOptionsServer(
                    "p4java://localhost:" + ts.getPort(), null);

            server.connect();
            server.setOrUnsetServerConfigurationValue(configName, null);
            boolean configureNameFound = false;

            BufferedReader reader = new BufferedReader(new FileReader(
                    ts.getLog()));
            String line = null;

            while ((line = reader.readLine()) != null) {

                if (line.contains("unset " + configName)) {

                    configureNameFound = true;
                    break;

                }
            }

            reader.close();
            assertTrue(
                    "Unsetting configuration variable not found in log file.",
                    configureNameFound);

        } catch (Throwable t) {

            h.error(t);

        }

    }

    public List<ServerConfigurationValue> showConfiguration(String namedServer,
                                                            String configName) {

        //if namedServer passed instead of configName, log will be different
        if (configName == null) {
            configName = namedServer;
        }

        List<ServerConfigurationValue> list = null;
        try {

            boolean shown = false;
            String serverName = "p4java://localhost:" + ts.getPort();
            IOptionsServer server = getOptionsServer(serverName,
                    null);

            server.connect();
            list = server.showServerConfiguration(namedServer, configName);

            BufferedReader reader = new BufferedReader(new FileReader(
                    ts.getLog()));
            String line = null;

            while ((line = reader.readLine()) != null) {

                if (line.contains("user-configure show " + configName)) {

                    shown = true;
                    break;

                }
            }

            reader.close();
            assertTrue("Config value not found in log file.", shown);

        } catch (Throwable t) {
            h.error(t);
        }
        return list;
    }

    /*
	 * Test setting monitor variable. Equivalent to: p4 configure set monitor=1
	 */
    @Test
    public void setMonitor() {
        setConfigurable("monitor", "1");
    }

    /*
	 * Test setting journal buffer size. Equivalent to: p4 configure
	 * dbjournal.bufsize=32K
	 */
    @Test
    public void setJournalBufferSize() {
        setConfigurable("dbjournal.bufsize", "32K");
    }

    /*
	 * Test setting db fsync variable. Equivalent to: p4 configure
	 * dbopen.nofsync=1
	 */
    @Test
    public void setNoDbFsync() {
        setConfigurable("dbopen.nofsync", "1");
    }

    /*
	 * Test setting annotate maxsize. Equivalent to: p4 configure
	 * dm.annotate.maxsize=20M
	 */
    @Test
    public void setAnnotateMaxSize() {
        setConfigurable("dm.annotate.maxsize", "20");
    }

    /*
	 * Test setting dm.domain.accessupdate. Equivalent to: p4 configure
	 * dm.domain.accessupdate=600
	 */
    @Test
    public void setDomainAccessUpdate() {
        setConfigurable("dm.domain.accessupdate", "600");
    }

    /*
	 * Test setting dm.domain.accessforce. Equivalent to: p4 configure
	 * dm.domain.accessforce=4600
	 */
    @Test
    public void setDomainAccessForce() {
        setConfigurable("dm.domain.accessforce", "4600");
    }

    /*
	 * Test setting max revs for grep. Equivalent to: p4 configure
	 * dm.grep.maxrevs=20K
	 */
    @Test
    public void setGrepMaxRevs() {
        setConfigurable("dm.grep.maxrevs", "20K");
    }

    /*
	 * Test setting max files for shelving. Equivalent to: p4 configure
	 * dm.shelve.maxfiles=20M
	 */
    @Test
    public void setShelveMaxFiles() {
        setConfigurable("dm.shelve.maxfiles", "20M");
    }

    /*
	 * Test setting max files for shelving. Equivalent to: p4 configure
	 * dm.shelve.maxsize=1
	 */
    @Test
    public void setShelveMaxSize() {
        setConfigurable("dm.shelve.maxsize", "1");
    }

    /*
	 * Test setting dm.user.accessupdate. Equivalent to: p4 configure
	 * dm.user.accessupdate=600
	 */
    @Test
    public void setUserAccessUpdate() {
        setConfigurable("dm.user.accessupdate", "600");
    }

    /*
	 * Test setting dm.user.accessforce. Equivalent to: p4 configure
	 * dm.user.accessforce=9600
	 */
    @Test
    public void setUserAccessForce() {
        setConfigurable("dm.user.accessforce", "9600");
    }

    /*
	 * Test setting dm.user.noautocreate.Equivalent to: p4 configure
	 * dm.user.noautocreate=1
	 */
    @Test
    public void setUserNoAutoCreate() {
        setConfigurable("dm.user.noautocreate", "1");
    }

    /*
	 * Test setting filesys.binaryscan. Equivalent to: p4 configure
	 * filesys.binaryscan=10K
	 */
    @Test
    public void setFileSysBinaryScan() {
        setConfigurable("filesys.binaryscan", "10K");
    }

    /*
	 * Test setting filesys.bufsize. Equivalent to: p4 configure
	 * filesys.bufsize=10K
	 */
    @Test
    public void setFileSysBufsize() {
        setConfigurable("filesys.bufsize", "10K");
    }

    /*
	 * Test setting filesys.extendlowmark. Equivalent to: p4 configure
	 * filesys.extendlowmark=64K
	 */
    @Test
    public void setFileSysExtendLowmark() {
        setConfigurable("filesys.extendlowmark", "64K");
    }

    /*
	 * Test setting filetype.maxtextsize. Equivalent to: p4 configure
	 * filetype.maxtextsize=30M
	 */
    @Test
    public void setFileTypeMaxTextSize() {
        setConfigurable("filetype.maxtextsize", "30M");
    }

    /*
	 * Test setting P4NAME with a very long name
	 */
    @Test
    public void setLongP4Name() {
        setConfigurable("myServer#P4NAME", "This is a very long name to test server"
                + "configure feature in p4java lets "
                + "see if it work its just "
                + "a test no need to be alramed no one"
                + " would really name a server with such a long name");
    }
    
    /*
	 * Test unsetting a variable. Equivalent of: p4 configure unset monitor
	 */
    @Test
    public void unsetValue() {
        unsetConfigurable("P4NAME");
    }

    /*
	 * Test setting security variable. Equivalent to: p4 configure security=0
	 */
    @Test
    public void setSecurity() {
        setConfigurable("security", "0");
    }

    /*
	 * Test showing monitor variable. Equivalent to: p4 configure show monitor
	 */
    @Test
    public void showMonitor() {
        List<ServerConfigurationValue> result = showConfiguration(null,
                "monitor");
        assertEquals("1", result.get(0).getValue());
    }

    /*
	 * Test show all variables for a named server
	 */
    @Test
    public void showAllNamedServer() {
        setConfigurable("myServer#P4NAME", "myServer");
        setConfigurable("myServer#monitor", "0");
        List<ServerConfigurationValue> result = showConfiguration("myServer",
                null);
        boolean foundMonitorZero = false;
        for(ServerConfigurationValue value:result){
        	if ( "myServer".equals(value.getServerName()) &&
        			"monitor".equals(value.getName()) &&
        			"0".equals(value.getValue())) {
        		foundMonitorZero = true;
        		break;
        	}
        }
        assertTrue("myServer does not have a monitor configurable value of 0, "
            + result, foundMonitorZero);
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}