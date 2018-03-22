package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Some simple tests for the global UsageOptions class.
 */
@TestId("Dev101_UsageOptionsTest")
public class UsageOptionsTest extends P4JavaTestCase {
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private IOptionsServer server = null;

    @Test
    public void testUsageOptionsDefaults() throws Exception {
        server = getServer();
        UsageOptions serverOpts = server.getUsageOptions();
        assertNotNull(serverOpts);
        assertNotNull(serverOpts.getUnsetClientName());
        assertEquals(PropertyDefs.CLIENT_UNSET_NAME_DEFAULT,
                server.getUsageOptions().getUnsetClientName());
        assertNotNull(serverOpts.getUnsetUserName());
        assertEquals(PropertyDefs.USER_UNSET_NAME_DEFAULT,
                server.getUsageOptions().getUnsetUserName());
        assertNotNull(serverOpts.getProgramName());
        assertEquals(PropertyDefs.PROG_NAME_DEFAULT,
                server.getUsageOptions().getProgramName());
        assertNotNull(serverOpts.getProgramVersion());
        assertEquals(PropertyDefs.PROG_VERSION_DEFAULT,
                server.getUsageOptions().getProgramVersion());
        assertNotNull(serverOpts.getWorkingDirectory());
        assertEquals(System.getProperty(UsageOptions.WORKING_DIRECTORY_PROPNAME),
                server.getUsageOptions().getWorkingDirectory());
    }

    @Test
    public void testUsageOptions() throws Exception {
        final String unsetClientName = "xyzabc";
        final String unsetUserName = "abcdefghjijklmno-test34";
        final String clientErrMsg = "must create client '" + unsetClientName + "' to access local files.";
        final String userErrMsg = "Access for user '" + unsetUserName + "' has not been enabled";

        thrown.expect(AccessException.class);
        thrown.expectMessage(containsString(userErrMsg));


        server = getServer();
        server.setCurrentClient(null);
        UsageOptions opts = new UsageOptions(null).setUnsetClientName(unsetClientName).setUnsetUserName(unsetUserName);
        server.setUsageOptions(opts);
        List<IFileSpec> files = server.getDepotFiles(FileSpecBuilder.makeFileSpecList("..."), null);
        assertNotNull(files);
        assertTrue(files.size() == 1);
        assertNotNull(files.get(0).getStatusMessage());
        assertThat(files.get(0).getStatusMessage(), containsString(clientErrMsg));
        server.setUserName(null);
        server.getDepotFiles(FileSpecBuilder.makeFileSpecList("..."), null);
        fail("Did not get expected access exception");
    }

    @After
    public void afterEach() {
        if (server != null) {
            this.endServerSession(server);
        }
    }
}
