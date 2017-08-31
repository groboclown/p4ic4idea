package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.core.IChangelist.Type.SUBMITTED;
import static com.perforce.p4java.server.CmdSpec.CHANGES;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetChangelistsOptions;

/**
 * Tests ChangesDelegator.
 */
@RunWith(JUnitPlatform.class)
public class ChangesDelegatorTest extends AbstractP4JavaUnitTest {

    /** The changes delegator. */
    private ChangesDelegator changesDelegator;

    /** The mock file specs. */
    private List<IFileSpec> mockFileSpecs;

    /** The mock file spec. */
    private IFileSpec mockFileSpec;

    /** The Constant mockChangelistId. */
    private static final int MOCK_CHANGE_ID = 1;

    /** The Constant path. */
    private static final String PATH = "//depot/";

    /** The Constant description. */
    private static final String DESCRIPTION = "Test change list";

    /** The Constant client. */
    private static final String CLIENT = "client1";

    /** Sample params. */
    private static final String[] SERVER_ARGS = new String[] { "-i", "-l", "-c" + CLIENT, "-m1",
            "-ssubmitted", PATH };

    /** Sample non-integrate. */
    private static final String[] SERVER_ARGS_NON_INT = new String[] { "-l", "-c" + CLIENT, "-m1",
            "-ssubmitted", PATH };

    /** Sample non-integrate, non-desc. */
    private static final String[] SERVER_ARGS_NON_INT_DESC = new String[] { "-c" + CLIENT, "-m1",
            "-ssubmitted", PATH };

    /** Sample not submitted. */
    private static final String[] SERVER_ARGS_NOT_SUB = new String[] { "-i", "-l", "-c" + CLIENT,
            "-m1", PATH };

    /** Sample spec params. */
    private static final String[] SPEC_PARAMS = new String[] { PATH };

    /** Matcher for spec. */
    private static final CommandLineArgumentMatcher SPEC_PARAMS_MATCHER =
            new CommandLineArgumentMatcher(SPEC_PARAMS);
    
    /** Matcher for server. */
    private static final CommandLineArgumentMatcher SERVER_ARGS_PARAMS_MATCHER =
            new CommandLineArgumentMatcher(SERVER_ARGS);
    
    /** Matcher for server not submitted. */
    private static final CommandLineArgumentMatcher NOT_SUB_ARGS_PARAMS_MATCHER =
            new CommandLineArgumentMatcher(SERVER_ARGS_NOT_SUB);
    
    /** Matcher for server not integrated. */
    private static final CommandLineArgumentMatcher NON_INT_ARGS_PARAMS_MATCHER =
            new CommandLineArgumentMatcher(SERVER_ARGS_NON_INT);
    
    /** Matcher for server not integrated/description. */
    private static final CommandLineArgumentMatcher NON_INT_DESC_ARGS_PARAMS_MATCHER =
            new CommandLineArgumentMatcher(SERVER_ARGS_NON_INT_DESC);

    /**
     * Before each.
     */
    @BeforeEach
    public void beforeEach() {
        server = mock(Server.class);
        changesDelegator = new ChangesDelegator(server);
        mockFileSpecs = new ArrayList<>();
        mockFileSpec = mock(IFileSpec.class);
        when(mockFileSpec.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
        when(mockFileSpec.getAnnotatedPreferredPathString()).thenReturn(PATH);
        mockFileSpecs.add(mockFileSpec);
    }

    /**
     * Test get changes file spec empty options.
     * 
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetChangesFileSpecEmptyOptions() throws P4JavaException {
        when(server.execMapCmdList(eq(CHANGES.toString()), argThat(SPEC_PARAMS_MATCHER), eq(null)))
                .thenReturn(null);
        List<IChangelistSummary> changeLists = changesDelegator.getChangelists(mockFileSpecs,
                new GetChangelistsOptions());
        verify(server).execMapCmdList(eq(CHANGES.toString()), argThat(SPEC_PARAMS_MATCHER),
                eq(null));
        assertNotNull(changeLists);
        assertTrue(changeLists.size() == 0);
    }

    /**
     * Test get changes file spec options.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetChangesFileSpecOptions() throws P4JavaException {
        when(server.execMapCmdList(eq(CHANGES.toString()), argThat(SPEC_PARAMS_MATCHER), eq(null)))
                .thenReturn(buildChangesList("submitted"));
        List<IChangelistSummary> changeLists = changesDelegator.getChangelists(mockFileSpecs,
                new GetChangelistsOptions());
        verify(server).execMapCmdList(eq(CHANGES.toString()), argThat(SPEC_PARAMS_MATCHER),
                eq(null));
        assertNotNull(changeLists);
        assertTrue(changeLists.size() == 1);
        assertChangeListSummary(changeLists.get(0), ChangelistStatus.SUBMITTED);
    }

    /**
     * Test get changes.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetChanges() throws P4JavaException {
        when(server.execMapCmdList(eq(CHANGES.toString()), argThat(SERVER_ARGS_PARAMS_MATCHER),
                eq(null))).thenReturn(buildChangesList("submitted"));
        List<IChangelistSummary> changeLists = changesDelegator.getChangelists(1, mockFileSpecs,
                CLIENT, "", true, SUBMITTED, true);
        verify(server).execMapCmdList(eq(CHANGES.toString()), argThat(SERVER_ARGS_PARAMS_MATCHER),
                eq(null));
        assertNotNull(changeLists);
        assertTrue(changeLists.size() == 1);
        assertChangeListSummary(changeLists.get(0), ChangelistStatus.SUBMITTED);
    }

    /**
     * Test get changes non int.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetChangesNonInt() throws P4JavaException {
        when(server.execMapCmdList(eq(CHANGES.toString()), argThat(NON_INT_ARGS_PARAMS_MATCHER),
                eq(null))).thenReturn(buildChangesList("submitted"));
        List<IChangelistSummary> changeLists = changesDelegator.getChangelists(1, mockFileSpecs,
                CLIENT, "", false, SUBMITTED, true);
        verify(server).execMapCmdList(eq(CHANGES.toString()), argThat(NON_INT_ARGS_PARAMS_MATCHER),
                eq(null));
        assertNotNull(changeLists);
        assertTrue(changeLists.size() == 1);
        assertChangeListSummary(changeLists.get(0), ChangelistStatus.SUBMITTED);
    }

    /**
     * Test get changes non int non desc.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetChangesNonIntNonDesc() throws P4JavaException {
        when(server.execMapCmdList(eq(CHANGES.toString()),
                argThat(NON_INT_DESC_ARGS_PARAMS_MATCHER), eq(null)))
                        .thenReturn(buildChangesList("submitted"));
        List<IChangelistSummary> changeLists = changesDelegator.getChangelists(1, mockFileSpecs,
                CLIENT, "", false, SUBMITTED, false);
        verify(server).execMapCmdList(eq(CHANGES.toString()),
                argThat(NON_INT_DESC_ARGS_PARAMS_MATCHER), eq(null));
        assertNotNull(changeLists);
        assertTrue(changeLists.size() == 1);
        assertChangeListSummary(changeLists.get(0), ChangelistStatus.SUBMITTED);
    }

    /**
     * Test get changes args.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetChangesArgs() throws P4JavaException {
        when(server.execMapCmdList(eq(CHANGES.toString()), argThat(SERVER_ARGS_PARAMS_MATCHER),
                eq(null))).thenReturn(buildChangesList("submitted"));
        List<IChangelistSummary> changeLists = changesDelegator.getChangelists(1, mockFileSpecs,
                CLIENT, "", true, true, false, true);
        verify(server).execMapCmdList(eq(CHANGES.toString()), argThat(SERVER_ARGS_PARAMS_MATCHER),
                eq(null));
        assertNotNull(changeLists);
        assertTrue(changeLists.size() == 1);
        assertChangeListSummary(changeLists.get(0), ChangelistStatus.SUBMITTED);
    }

    /**
     * Test get changes args not submitted.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetChangesArgsNotSubmitted() throws P4JavaException {
        when(server.execMapCmdList(eq(CHANGES.toString()), argThat(NOT_SUB_ARGS_PARAMS_MATCHER),
                eq(null))).thenReturn(buildChangesList("pending"));
        List<IChangelistSummary> changeLists = changesDelegator.getChangelists(1, mockFileSpecs,
                CLIENT, "", true, false, false, true);
        verify(server).execMapCmdList(eq(CHANGES.toString()), argThat(NOT_SUB_ARGS_PARAMS_MATCHER),
                eq(null));
        assertNotNull(changeLists);
        assertTrue(changeLists.size() == 1);
        assertChangeListSummary(changeLists.get(0), ChangelistStatus.PENDING);
    }

    /**
     * Assert change list summary.
     *
     * @param summary
     *            the summary
     * @param expectedStatus
     *            the expected status
     */
    private void assertChangeListSummary(final IChangelistSummary summary,
            final ChangelistStatus expectedStatus) {
        assertEquals(DESCRIPTION, summary.getDescription());
        assertEquals(CLIENT, summary.getClientId());
        assertEquals(MOCK_CHANGE_ID, summary.getId());
        assertEquals(expectedStatus, summary.getStatus());
    }

    /**
     * Test get changes access exception.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangesAccessException() throws Exception {
        when(server.execMapCmdList(eq(CHANGES.toString()), any(String[].class), eq(null)))
                .thenThrow(AccessException.class);
        try {
            changesDelegator.getChangelists(1, mockFileSpecs, CLIENT, "", true, false, false, true);
            fail("AccessException was expected.");
        } catch (AccessException accEx) {
            // OK
        } catch (Exception ex) {
            fail("AccessException was expected.");
        }
    }

    /**
     * Test get changes connection exception.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangesConnectionException() throws Exception {
        when(server.execMapCmdList(eq(CHANGES.toString()), any(String[].class), eq(null)))
                .thenThrow(ConnectionException.class);
        try {
            changesDelegator.getChangelists(1, mockFileSpecs, CLIENT, "", true, false, false, true);
            fail("ConnectionException was expected.");
        } catch (ConnectionException e) {
            // OK
        } catch (Exception ex) {
            fail("ConnectionException was expected.");
        }
    }

    /**
     * Test get changes request exception.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangesRequestException() throws Exception {
        when(server.execMapCmdList(eq(CHANGES.toString()), any(String[].class), eq(null)))
                .thenThrow(RequestException.class);
        try {
            changesDelegator.getChangelists(1, mockFileSpecs, CLIENT, "", true, false, false, true);
            fail("RequestException was expected.");
        } catch (RequestException e) {
            // OK
        } catch (Exception ex) {
            fail("RequestException was expected.");
        }
    }

    /**
     * Test get changes p4 java exception.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangesP4JavaException() throws Exception {
        when(server.execMapCmdList(eq(CHANGES.toString()), any(String[].class), eq(null)))
                .thenThrow(P4JavaException.class);
        try {
            changesDelegator.getChangelists(1, mockFileSpecs, CLIENT, "", true, false, false, true);
            fail("RequestException was expected.");
        } catch (RequestException e) {
            // OK
        } catch (Exception ex) {
            fail("RequestException was expected.");
        }
    }

    /**
     * Builds the changes.
     *
     * @param status
     *            the status
     * @return the list
     */
    private List<Map<String, Object>> buildChangesList(final String status) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("path", PATH);
        map.put("change", String.valueOf(MOCK_CHANGE_ID));
        map.put("changeType", "public");
        map.put("client", CLIENT);
        map.put("time", "1480417191");
        map.put("status", status);
        map.put("desc", DESCRIPTION);
        list.add(map);
        return list;
    }
}