package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.DEPOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * Test for DepotDelegator.
 *
 */
public class DepotDelegatorTest extends AbstractP4JavaUnitTest {

    /** The depot delegator. */
    private DepotDelegator depotDelegator;

    /** Name. */
    private static final String TEST_DEPOT = "TestDepot";

    /** Owner. */
    private static final String TEST_USER = "TestUser";

    /** Map. */
    private static final String TEST_DEPOT_MAP = TEST_DEPOT + "/...";

    /** Description. */
    private static final String TEST_DEPOT_DESC = "TestDescription";

    /** Matcher for create. */
    private static final CommandLineArgumentMatcher CREATE_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-i" });

    /** Matcher for delete. */
    private static final CommandLineArgumentMatcher DELETE_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-d", TEST_DEPOT });

    /** Matcher for get. */
    private static final CommandLineArgumentMatcher GET_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-o", TEST_DEPOT });

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        depotDelegator = new DepotDelegator(server);
    }

    /**
     * Test create null depot.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullDepot() throws P4JavaException {
        depotDelegator.createDepot(null);
    }

    /**
     * Test create depot.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testCreateDepot() throws P4JavaException {
        IDepot depot = buildDepot();
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(CREATE_MATCHER),
                eq(InputMapper.map(depot)))).thenReturn(buildDepotCreatedMap());
        String result = depotDelegator.createDepot(depot);
        verify(server).execMapCmdList(eq(DEPOT.toString()), argThat(CREATE_MATCHER),
                eq(InputMapper.map(depot)));
        assertEquals("Depot " + TEST_DEPOT + " saved.", result);
    }

    /**
     * Test delete depot.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testDeleteDepot() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenReturn(buildDepotDeletedMap());
        String result = depotDelegator.deleteDepot(TEST_DEPOT);
        verify(server).execMapCmdList(eq(DEPOT.toString()), argThat(DELETE_MATCHER), eq(null));
        assertEquals("Depot " + TEST_DEPOT + " deleted.", result);
    }

    /**
     * Test delete depot not empty.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testDeleteDepotNotEmpty() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenReturn(buildDepotNotEmptyMap());
        try {
            depotDelegator.deleteDepot(TEST_DEPOT);
        } catch (RequestException e) {
            assertTrue(e.getMessage().startsWith("Depot " + TEST_DEPOT + " isn't empty."));
        }
    }

    /**
     * Test get depot.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDepot() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(GET_MATCHER), eq(null)))
                .thenReturn(buildGetMap());
        IDepot result = depotDelegator.getDepot(TEST_DEPOT);
        verify(server).execMapCmdList(eq(DEPOT.toString()), argThat(GET_MATCHER), eq(null));
        assertEquals(TEST_DEPOT, result.getName());
        assertEquals(TEST_USER, result.getOwnerName());
        assertEquals(TEST_DEPOT_DESC, result.getDescription());
        assertEquals(TEST_DEPOT_MAP, result.getMap());
        assertEquals(DepotType.SPEC, result.getDepotType());
    }

    /**
     * Test create access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testCreateAccessException() throws P4JavaException {
        IDepot depot = buildDepot();
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(CREATE_MATCHER),
                eq(InputMapper.map(depot)))).thenThrow(AccessException.class);
        depotDelegator.createDepot(depot);
    }

    /**
     * Test create connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testCreateConnectionException() throws P4JavaException {
        IDepot depot = buildDepot();
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(CREATE_MATCHER),
                eq(InputMapper.map(depot)))).thenThrow(ConnectionException.class);
        depotDelegator.createDepot(depot);
    }

    /**
     * Test create request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testCreateRequestException() throws P4JavaException {
        IDepot depot = buildDepot();
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(CREATE_MATCHER),
                eq(InputMapper.map(depot)))).thenThrow(RequestException.class);
        depotDelegator.createDepot(depot);
    }

    /**
     * Test delete access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testDeleteAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        depotDelegator.deleteDepot(TEST_DEPOT);
    }

    /**
     * Test delete connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testDeleteConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        depotDelegator.deleteDepot(TEST_DEPOT);
    }

    /**
     * Test delete request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testDeleteRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(DELETE_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        depotDelegator.deleteDepot(TEST_DEPOT);
    }

    /**
     * Test get access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testGetAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(GET_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        depotDelegator.getDepot(TEST_DEPOT);
    }

    /**
     * Test get connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testGetConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(GET_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        depotDelegator.getDepot(TEST_DEPOT);
    }

    /**
     * Test get request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testGetRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(DEPOT.toString()), argThat(GET_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        depotDelegator.getDepot(TEST_DEPOT);
    }

    /**
     * Builds the depot.
     *
     * @return the i depot
     */
    private IDepot buildDepot() {
        IDepot depot = new Depot(TEST_DEPOT, TEST_USER, null, TEST_DEPOT_DESC, DepotType.LOCAL,
                null, null, TEST_DEPOT_MAP);
        return depot;
    }

    /**
     * Builds the depot created map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildDepotCreatedMap() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("depotName", TEST_DEPOT);
        resultMap.put("fmt0", "Depot %depotName% saved.");
        resultMap.put("code0", "285219026");
        resultList.add(resultMap);
        return resultList;
    }

    /**
     * Builds the get map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildGetMap() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("Owner", TEST_USER);
        resultMap.put("Description", TEST_DEPOT_DESC);
        resultMap.put("Depot", TEST_DEPOT);
        resultMap.put("Map", TEST_DEPOT_MAP);
        resultMap.put("Type", "spec");
        resultList.add(resultMap);
        return resultList;
    }

    /**
     * Builds the depot deleted map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildDepotDeletedMap() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("depotName", TEST_DEPOT);
        resultMap.put("fmt0", "Depot %depotName% deleted.");
        resultMap.put("code0", "285219028");
        resultList.add(resultMap);
        return resultList;
    }

    /**
     * Builds the depot deleted map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildDepotNotEmptyMap() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("fmt0",
                "Depot %depot% isn't empty. To delete a depot, all file revisions "
                    + "must be removed and all lazy copy references from other depots must "
                    + "be severed. Use '%'p4 obliterate'%' or '%'p4 snap'%' to break file "
                    + "linkages from other depots, then clear this depot with '%'p4 obliterate'%', "
                    + "then retry the deletion.");
        resultMap.put("code0", "822417475");
        resultMap.put("depot", TEST_DEPOT);
        resultList.add(resultMap);
        return resultList;
    }
}