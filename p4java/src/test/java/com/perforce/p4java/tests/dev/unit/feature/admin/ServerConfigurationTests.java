/**
 *
 */
package com.perforce.p4java.tests.dev.unit.feature.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.admin.ServerConfigurationValue;
import com.perforce.p4java.admin.ServerConfigurationValue.ConfigType;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Basic tests for the IOptionsServer server configuration admin features. Needs
 * to run as super user.
 */
@TestId("Admin_ServerConfigurationTests")
public class ServerConfigurationTests extends P4JavaTestCase {

    /**
     * Runs before every test. Make sure at least 1 configuration value exists.
     */
    @Before
    public void before() {
        try {
            final String SERVER_NAME = "xyz";
            final String CONFIG_NAME = "minClientMessage";
            IOptionsServer server = getServerAsSuper();
            server.setOrUnsetServerConfigurationValue(SERVER_NAME + "#" + CONFIG_NAME, "Test");
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        } finally {
            if (server != null) {
                this.endServerSession(server);
            }
        }
    }

    /**
     * Config "show" tests -- not much we can really do here at the best of
     * times except ensure we see some very basic values. Makes the rather dodgy
     * assumption that a value for P4JOURNAL exists and that it's an environment
     * type, and that an entry exists for server "xyz". This may fail in the
     * future...
     */
    @Test
    public void testShowServerConfiguration() {
        IOptionsServer server = null;
        final String SERVER_NAME = "xyz";
        final String CONFIG_NAME = "minClientMessage";

        try {
            server = getServerAsSuper();
            assertNotNull("Null server returned by getServerAsSuper", server);

            List<ServerConfigurationValue> configValues = server.showServerConfiguration(null,
                    null);
            assertNotNull("null config values list returned by showServerConfguration()",
                    configValues);
            assertTrue("zero-sized config values list returned", configValues.size() > 0);

            boolean found = false;
            for (ServerConfigurationValue value : configValues) {
                assertNotNull("null config value object in list", value);
                assertNotNull("null config value name in list", value.getName());
                assertNotNull("null config value value in list", value.getValue());
                assertNotNull("null config value type in list", value.getType());

                if ("P4JOURNAL".equalsIgnoreCase(value.getName())) {
                    found = true;
                    assertEquals("P4JOURNAL type was not DEFAULT: " + value.getType().toString(),
                            ConfigType.DEFAULT.toString(), value.getType().toString());
                }
            }
            assertTrue("P4JOURNAL config value not found", found);

            configValues = server.showServerConfiguration(SERVER_NAME, null);
            assertNotNull("null config values list returned by showServerConfguration()",
                    configValues);
            assertTrue("zero-sized config values list returned", configValues.size() > 0);
            found = false;
            for (ServerConfigurationValue value : configValues) {
                assertNotNull("null config value object in list", value);
                assertNotNull("null config value name in list", value.getName());
                assertNotNull("null config value value in list", value.getValue());
                assertNotNull("null config value type in list", value.getType());
                assertNotNull("null config server name value in list", value.getServerName());

                if (CONFIG_NAME.equalsIgnoreCase(value.getName())) {
                    found = true;
                    assertEquals(
                            "minClientMessage type was not CONFIGURE: "
                                    + value.getType().toString(),
                            ConfigType.CONFIGURE.toString(), value.getType().toString());
                }
            }
            assertTrue(CONFIG_NAME + " config value not found", found);

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        } finally {
            if (server != null) {
                this.endServerSession(server);
            }
        }
    }

    /**
     * Quick and dirty test of trying to set a server config value. This is a
     * fairly fragile test and may break if the corresponding server or config
     * values are changed or disappear, etc.
     */
    @Test
    public void testSetServerConfigurationValue() {
        IOptionsServer server = null;
        final String SERVER_NAME = "xyz";
        final String CONFIG_NAME = "minClientMessage";
        final String CONFIG_VALUE = "test value " + this.getRandomName(" message ");

        try {
            server = getServerAsSuper();
            assertNotNull("Null server returned by getServerAsSuper", server);

            String retVal = server.setOrUnsetServerConfigurationValue(
                    SERVER_NAME + "#" + CONFIG_NAME, CONFIG_VALUE);
            assertTrue(retVal != null && retVal.contains(CONFIG_NAME)
                    && retVal.contains("test value Admin_ServerConfigurationTests"));
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        } finally {
            if (server != null) {
                this.endServerSession(server);
            }
        }
    }
}
