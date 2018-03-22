package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.admin.ServerConfigurationValue.ConfigType.OPTION;
import static com.perforce.p4java.admin.ServerConfigurationValue.ConfigType.DEFAULT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.admin.ServerConfigurationValue;
import com.perforce.p4java.admin.ServerConfigurationValue.ConfigType;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.CmdSpec;

/**
 * Tests the ConfigureDelegator.
 */
@RunWith(JUnitPlatform.class)
public class ConfigureDelegatorTest extends AbstractP4JavaUnitTest {

    /** The configure delegator. */
    private ConfigureDelegator configureDelegator;

    /** An example server name. */
    private static final String SERVER_NAME = "Test";

    /** Expected arguments for configure show with a server name. */
    private static final String[] SERVER_NAME_ARGS = new String[] { "show", SERVER_NAME };

    /** Expected arguments for configure show. */
    private static final String[] SHOW_ARGS = new String[] { "show" };

    /** Example path. */
    private static final String PATH = "/opt/perforce";

    /** Example version. */
    private static final String VERSION = "20132";

    /** Example config. */
    private static final String P4JOURNAL = "P4JOURNAL";

    /** Example config. */
    private static final String JOURNAL = "journal";

    /** Example config. */
    private static final String LOG = "log.txt";

    /** Example config. */
    private static final String MONITOR = "monitor";

    /** Example config. */
    private static final String MONITOR_VALUE = "1";

    /** Example config. */
    private static final String INTEG = "dm.integ.engine";

    /** Example config. */
    private static final String INTEG_VALUE = "3";

    /** Example config. */
    private static final String R_OPTION = "r";

    /** Example config. */
    private static final String P_OPTION = "p";

    /** Example config. */
    private static final String L_OPTION = "L";

    /** Example config. */
    private static final String ANY = "any";

    /** Example config. */
    private static final String ANY_VALUE = "anyValue";

    /** Expected arguments for configure show with any. */
    private static final String[] SHOW_ANY_ARGS = new String[] { "show", ANY };

    /** Matcher for server name. */
    private static final CommandLineArgumentMatcher SERVER_NAME_MATCHER =
            new CommandLineArgumentMatcher(SERVER_NAME_ARGS);
    
    /** Matcher for show any. */
    private static final CommandLineArgumentMatcher SHOW_ANY_MATCHER =
            new CommandLineArgumentMatcher(SHOW_ANY_ARGS);
    
    /** Matcher for show. */
    private static final CommandLineArgumentMatcher SHOW_MATCHER =
            new CommandLineArgumentMatcher(SHOW_ARGS);
    
    /** Matcher for set name. */
    private static final CommandLineArgumentMatcher SET_NAME_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { RpcFunctionMapKey.SET,
                            SERVER_NAME + "#" + MONITOR + "=" + MONITOR_VALUE });
    
    /** Matcher for set. */
    private static final CommandLineArgumentMatcher SET_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { RpcFunctionMapKey.SET, MONITOR + "=" + MONITOR_VALUE });
    
    /** Matcher for unset name. */
    private static final CommandLineArgumentMatcher UNSET_NAME_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { RpcFunctionMapKey.UNSET, SERVER_NAME + "#" + MONITOR });
    
    /** Matcher for unset. */
    private static final CommandLineArgumentMatcher UNSET_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { RpcFunctionMapKey.UNSET, MONITOR });

    /**
     * Before each.
     */
    @BeforeEach
    public void beforeEach() {
        server = mock(Server.class);
        configureDelegator = new ConfigureDelegator(server);
    }

    /**
     * Test show server configuration for name.
     * 
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testShowServerConfigurationForName() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(SERVER_NAME_MATCHER),
                eq(null))).thenReturn(buildShowList(SERVER_NAME));
        List<ServerConfigurationValue> values = configureDelegator
                .showServerConfiguration(SERVER_NAME, null);
        verify(server).execMapCmdList(eq(CmdSpec.CONFIGURE.toString()),
                argThat(SERVER_NAME_MATCHER), eq(null));
        assertValues(values, SERVER_NAME);
    }

    /**
     * Test show server configuration for value.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testShowServerConfigurationForValue() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(SHOW_ANY_MATCHER),
                eq(null))).thenReturn(buildShowList(null));
        List<ServerConfigurationValue> values = configureDelegator.showServerConfiguration(null,
                ANY);
        verify(server).execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(SHOW_ANY_MATCHER),
                eq(null));
        assertValues(values, null);
        assertFound(values, new ServerConfigurationValue(ANY, ConfigType.OPTION, ANY, ANY_VALUE));
    }

    /**
     * Test show server configuration without name or value.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testShowServerConfiguration() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(SHOW_MATCHER),
                eq(null))).thenReturn(buildShowList(null));
        List<ServerConfigurationValue> values = configureDelegator.showServerConfiguration(null,
                null);
        verify(server).execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(SHOW_MATCHER),
                eq(null));
        assertValues(values, null);
    }

    /**
     * Test set with server name.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testSetWithServerName() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(SET_NAME_MATCHER),
                eq(null)))
                        .thenReturn(getMockServerResponse(RpcFunctionMapKey.SET, SERVER_NAME,
                                MONITOR, MONITOR_VALUE));
        String result = configureDelegator
                .setOrUnsetServerConfigurationValue(SERVER_NAME + "#" + MONITOR, MONITOR_VALUE);
        verify(server).execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(SET_NAME_MATCHER),
                eq(null));
        assertTrue(result.startsWith(
                String.format("For server '%s', configuration variable '%s' set to '%s'",
                        SERVER_NAME, MONITOR, MONITOR_VALUE)));
    }

    /**
     * Test set without server name.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testSetWithoutServerName() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(SET_MATCHER),
                eq(null))).thenReturn(
                        getMockServerResponse(RpcFunctionMapKey.SET, null, MONITOR, MONITOR_VALUE));
        String result = configureDelegator.setOrUnsetServerConfigurationValue(MONITOR,
                MONITOR_VALUE);
        verify(server).execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(SET_MATCHER),
                eq(null));
        assertTrue(result.startsWith(
                String.format("For server '%s', configuration variable '%s' set to '%s'", null,
                        MONITOR, MONITOR_VALUE)));
    }

    /**
     * Test unset with server name.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testUnSetWithServerName() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(UNSET_NAME_MATCHER),
                eq(null))).thenReturn(
                        getMockServerResponse(RpcFunctionMapKey.UNSET, SERVER_NAME, MONITOR, null));
        String result = configureDelegator
                .setOrUnsetServerConfigurationValue(SERVER_NAME + "#" + MONITOR, null);
        verify(server).execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(UNSET_NAME_MATCHER),
                eq(null));
        assertTrue(result.startsWith(String.format(
                "For server '%s', configuration variable '%s' removed", SERVER_NAME, MONITOR)));
    }

    /**
     * Test unset without server name.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testUnSetWithoutServerName() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(UNSET_MATCHER),
                eq(null))).thenReturn(
                        getMockServerResponse(RpcFunctionMapKey.UNSET, null, MONITOR, null));
        String result = configureDelegator.setOrUnsetServerConfigurationValue(MONITOR, null);
        verify(server).execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), argThat(UNSET_MATCHER),
                eq(null));
        assertTrue(result.startsWith(String
                .format("For server '%s', configuration variable '%s' removed", null, MONITOR)));
    }

    /**
     * Test unset unknown.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testUnSetUnknown() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.CONFIGURE.toString()), any(String[].class), eq(null)))
                .thenReturn(getMockServerErrorResponse(MONITOR));
        String result = configureDelegator.setOrUnsetServerConfigurationValue(MONITOR, null);
        assertTrue(result.startsWith(
                String.format("Configuration variable '%s' did not have a value.", MONITOR)));
    }

    /**
     * Gets the mock server error response.
     *
     * @param name
     *            the name
     * @return the mock server error response
     */
    private List<Map<String, Object>> getMockServerErrorResponse(final String name) {
        List<Map<String, Object>> retVal = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("fmt0", "Configuration variable '%config%' did not have a value.");
        map.put("code0", "822221347");
        map.put("config", name);
        retVal.add(map);
        return retVal;
    }

    /**
     * Gets the mock server response.
     * 
     * @param action
     *            the action
     * @param serverName
     *            the server name
     * @param name
     *            the name
     * @param value
     *            the value
     * @return the mock server response
     */
    private List<Map<String, Object>> getMockServerResponse(final String action,
            final String serverName, final String name, final String value) {
        List<Map<String, Object>> retVal = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("Action", action);
        map.put("ServerName", serverName);
        map.put("Name", name);
        if (RpcFunctionMapKey.SET.equals(action)) {
            map.put("Value", value);
        }
        retVal.add(map);
        return retVal;
    }

    /**
     * Test set blank name.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testSetBlankName() throws P4JavaException {
        try {
            configureDelegator.setOrUnsetServerConfigurationValue("", "value");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        } catch (Exception e) {
            fail("Expected IllegalArgumentException was " + e);
        }
    }

    /**
     * Test set null name.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testSetNullName() throws P4JavaException {
        try {
            configureDelegator.setOrUnsetServerConfigurationValue(null, "value");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        } catch (Exception e) {
            fail("Expected NullPointerException was " + e);
        }
    }

    /**
     * Assert that all the values returned match according to the mocked server
     * response.
     *
     * @param values
     *            the values
     * @param serverName
     *            the server name
     */
    private void assertValues(final List<ServerConfigurationValue> values,
            final String serverName) {
        final int allValues = 7;
        final int nameValues = 6;
        assertNotNull(values);
        assertEquals(serverName == null ? allValues : nameValues, values.size());
        assertFound(values,
                new ServerConfigurationValue(serverName, ConfigType.OPTION, R_OPTION, PATH));
        assertFound(values,
                new ServerConfigurationValue(serverName, ConfigType.OPTION, P_OPTION, VERSION));
        assertFound(values,
                new ServerConfigurationValue(serverName, ConfigType.DEFAULT, P4JOURNAL, JOURNAL));
        assertFound(values,
                new ServerConfigurationValue(serverName, ConfigType.OPTION, L_OPTION, LOG));
        assertFound(values, new ServerConfigurationValue(serverName, ConfigType.CONFIGURE, MONITOR,
                MONITOR_VALUE));
        assertFound(values,
                new ServerConfigurationValue(serverName, ConfigType.TUNABLE, INTEG, INTEG_VALUE));
    }

    /**
     * Assert that a ServerConfigurationValue is in the list of values.
     *
     * @param values
     *            the values
     * @param expected
     *            the expected
     */
    private void assertFound(final List<ServerConfigurationValue> values,
            final ServerConfigurationValue expected) {
        boolean found = false;
        for (ServerConfigurationValue value : values) {
            if (expected.getName().equals(value.getName())
                    && expected.getType().equals(value.getType())
                    && expected.getValue().equals(value.getValue())
                    && ((expected.getServerName() == null && value.getServerName() == null)
                            || (expected.getServerName().equals(value.getServerName())))) {
                found = true;
                break;
            }
        }
        if (!found) {
            fail(String.format("Did not find expected name=%s, type=%s, value=%s",
                    expected.getName(), expected.getType().toString(), expected.getValue()));
        }
    }

    /**
     * Builds the show list (mock return from the Server).
     *
     * @param serverName
     *            the server name
     * @return the list
     */
    private List<Map<String, Object>> buildShowList(final String serverName) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(buildMap(R_OPTION, PATH, OPTION.toString().toLowerCase(), serverName));
        list.add(buildMap(P_OPTION, VERSION, OPTION.toString().toLowerCase(), serverName));
        list.add(buildMap(P4JOURNAL, JOURNAL, DEFAULT.toString().toLowerCase(), serverName));
        list.add(buildMap(L_OPTION, LOG, OPTION.toString().toLowerCase(), serverName));
        list.add(buildMap(MONITOR, MONITOR_VALUE, ConfigType.CONFIGURE.toString().toLowerCase(),
                serverName));
        list.add(buildMap(INTEG, INTEG_VALUE,
                ConfigType.TUNABLE.toString().toLowerCase() + " (configure)", serverName));
        if (serverName == null) {
            list.add(buildMap(ANY, ANY_VALUE, OPTION.toString().toLowerCase(), ANY));
        }
        return list;
    }

    /**
     * Utility to build a map to put in a mocked server response.
     *
     * @param name
     *            the name
     * @param value
     *            the value
     * @param type
     *            the type
     * @param serverName
     *            the server name
     * @return the map
     */
    private Map<String, Object> buildMap(final String name, final String value, final String type,
            final String serverName) {
        Map<String, Object> map = new HashMap<>();
        map.put("Type", type.toString().toLowerCase());
        map.put("Value", value);
        map.put("Name", name);
        map.put("ServerName", serverName);
        return map;
    }
}