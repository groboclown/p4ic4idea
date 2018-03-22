package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.INFO;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IServerInfo;

/**
 * Tests the InfoDelegator.
 */
public class InfoDelegatorTest extends AbstractP4JavaUnitTest {

    /** The info delegator. */
    private InfoDelegator infoDelegator;
    /** Simple matcher. */
    private static final CommandLineArgumentMatcher INFO_MATCHER = new CommandLineArgumentMatcher(
            new String[] {});

    /** Example test value. */
    private static final String INTEG = "3";

    /** Example test value. */
    private static final String VERSION =
            "P4D/LINUX26X86_64/2013.2.PREP-TEST_ONLY/677508 (2013/07/26)";

    /** Example test value. */
    private static final String HOST = "test-host";

    /** Example test value. */
    private static final String WD = "/tmp";

    /** Example test value. */
    private static final String ROOT = "/root";

    /** Example test value. */
    private static final String CLIENT_ADDR = "10.1.3.84";

    /** Example test value. */
    private static final String CLIENT_NAME = "*unknown*";

    /** Example test value. */
    private static final String SERVER_ADDR = "eng-p4java-vm.perforce.com:20132";

    /** Example test value. */
    private static final String LICENSE = "License";

    /** Example test value. */
    private static final String SERVER_IP = "127.0.0.1";

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        infoDelegator = new InfoDelegator(server);
    }

    /**
     * Test info connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testInfoConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(INFO.toString()), argThat(INFO_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        infoDelegator.getServerInfo();
    }

    /**
     * Test info access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testInfoAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(INFO.toString()), argThat(INFO_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        infoDelegator.getServerInfo();
    }

    /**
     * Test info request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testInfoRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(INFO.toString()), argThat(INFO_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        infoDelegator.getServerInfo();
    }

    /**
     * Test info p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testInfoP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(INFO.toString()), argThat(INFO_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        infoDelegator.getServerInfo();
    }

    /**
     * Test info.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testInfo() throws P4JavaException {
        when(server.execMapCmdList(eq(INFO.toString()), argThat(INFO_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        assertInfo(infoDelegator.getServerInfo());
    }

    /**
     * Assert info.
     *
     * @param info
     *            the info
     */
    private void assertInfo(final IServerInfo info) {
        assertEquals(INTEG, info.getIntegEngine());
        assertEquals(VERSION, info.getServerVersion());
        assertEquals(HOST, info.getClientHost());
        assertEquals(WD, info.getClientCurrentDirectory());
        assertEquals(CLIENT_NAME, info.getClientName());
        assertEquals(SERVER_ADDR, info.getServerAddress());
        assertEquals(ROOT, info.getServerRoot());
        assertEquals(LICENSE, info.getServerLicense());
        assertEquals(CLIENT_ADDR, info.getClientAddress());
        assertEquals(SERVER_IP, info.getServerLicenseIp());
    }

    /**
     * Builds the valid result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("integEngine", INTEG);
        result.put("serverVersion", VERSION);
        result.put("clientHost", HOST);
        result.put("clientCwd", WD);
        result.put("clientName", CLIENT_NAME);
        result.put("serverAddress", SERVER_ADDR);
        result.put("serverRoot", ROOT);
        result.put("serverLicense", LICENSE);
        result.put("clientAddress", CLIENT_ADDR);
        result.put("serverLicense-ip", SERVER_IP);
        results.add(result);
        return results;
    }
}