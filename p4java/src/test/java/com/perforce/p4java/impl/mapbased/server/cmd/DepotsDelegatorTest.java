package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.DEPOTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * Tests the DepotsDelegator.
 */
@RunWith(JUnitPlatform.class)
public class DepotsDelegatorTest extends AbstractP4JavaUnitTest {
    /** Name. */
    private static final String TEST_DEPOT = "TestDepot";

    /** Map. */
    private static final String TEST_DEPOT_MAP = TEST_DEPOT + "/...";

    /** Description. */
    private static final String TEST_DEPOT_DESC = "TestDescription";
    
    /** The depots delegator. */
    private DepotsDelegator depotsDelegator;

    /** Matcher with no params. */
    private static final CommandLineArgumentMatcher EMPTY_MATCHER = new CommandLineArgumentMatcher(
            new String[] {});

    /**
     * Runs before every test.
     */
    @Before
    public void before() {
        server = mock(Server.class);
        depotsDelegator = new DepotsDelegator(server);
    }

    /**
     * Test get depot.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDepots() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOTS.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenReturn(buildDepotsMap());
        List<IDepot> result = depotsDelegator.getDepots();
        verify(server).execMapCmdList(eq(DEPOTS.toString()), argThat(EMPTY_MATCHER), eq(null));
        assertNotNull(result);
        assertEquals(1, result.size());
        IDepot depot = result.get(0);
        assertEquals(TEST_DEPOT, depot.getName());
        assertEquals(TEST_DEPOT_DESC, depot.getDescription());
        assertEquals(TEST_DEPOT_MAP, depot.getMap());
        assertEquals(DepotType.SPEC, depot.getDepotType());
    }
    
    /**
     * Test get depots non found.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testGetDepotsNonFound() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOTS.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenReturn(buildNoDepotsMap());
        List<IDepot> result = depotsDelegator.getDepots();
        verify(server).execMapCmdList(eq(DEPOTS.toString()), argThat(EMPTY_MATCHER), eq(null));
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    /**
     * Test get access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testGetAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOTS.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        depotsDelegator.getDepots();
    }

    /**
     * Test get connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testGetConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOTS.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        depotsDelegator.getDepots();
    }

    /**
     * Test get request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testGetRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOTS.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        depotsDelegator.getDepots();
    }
    
    /**
     * Builds a mock server return with a single depot.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildDepotsMap() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("desc", TEST_DEPOT_DESC);
        resultMap.put("name", TEST_DEPOT);
        resultMap.put("map", TEST_DEPOT_MAP);
        resultMap.put("type", "spec");
        resultList.add(resultMap);
        return resultList;
    }
    
    /**
     * Builds a mock server return with no depots.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildNoDepotsMap() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        return resultList;
    }
}