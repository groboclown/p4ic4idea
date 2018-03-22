package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.CHANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
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
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.server.delegator.IChangeDelegator;

/**
 * Tests the ChangeDelegator.
 */
@RunWith(JUnitPlatform.class)
public class ChangeDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_NOT_IN_INFO_RANGE = "168435456";
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "285219021";


    /** The mock changelist options. */
    private ChangelistOptions mockChangelistOptions;

    /** The mock changlist id. */
    private static final int MOCK_LIST_ID = 10;

    /** The change delegator. */
    private IChangeDelegator changeDelegator;

    /** Get params for default changelist. */
    private static final String[] DEFAULT_PARAMS = new String[] { "-o" };

    /** Get params for id changelist. */
    private static final String[] GET_ID_PARAMS = new String[] { "-o",
            String.valueOf(MOCK_LIST_ID) };
    /** Delete params for id changelist. */
    private static final String[] DELETE_ID_PARAMS = new String[] { "-d",
            String.valueOf(MOCK_LIST_ID) };
    
    /** Matcher for calls to delete by id. */
    private static final CommandLineArgumentMatcher DELETE_ID_PARAMS_MATCHER =
            new CommandLineArgumentMatcher(DELETE_ID_PARAMS);
    
    /** Matcher for calls to get by id. */
    private static final CommandLineArgumentMatcher GET_ID_PARAMS_MATCHER =
            new CommandLineArgumentMatcher(GET_ID_PARAMS);
    
    /** Matcher for calls to get by default id. */
    private static final CommandLineArgumentMatcher DEFAULT_ID_PARAMS_MATCHER =
            new CommandLineArgumentMatcher(DEFAULT_PARAMS);

    /**
     * Runs before every test.
     */
    @BeforeEach
    public void beforeEach() {
        server = mock(Server.class);
        changeDelegator = new ChangeDelegator(server);
        mockChangelistOptions = mock(ChangelistOptions.class);
    }

    /**
     * Test that a ConnectionException is thrown for getChangelist when the
     * execMapCmdList throws it.
     * 
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangelistConnectionException() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), any(String[].class), eq(null)))
                .thenThrow(ConnectionException.class);
        try {
            changeDelegator.getChangelist(MOCK_LIST_ID);
            fail("ConnectionException was expected.");
        } catch (ConnectionException conEx) {
            // OK
        } catch (Exception ex) {
            fail("ConnectionException was expected.");
        }
    }

    /**
     * Test that an AccessException is thrown for getChangelist when the
     * execMapCmdList throws it.
     * 
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangelistAccessException() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), any(String[].class), eq(null)))
                .thenThrow(AccessException.class);
        try {
            changeDelegator.getChangelist(MOCK_LIST_ID);
            fail("AccessException was expected.");
        } catch (AccessException accEx) {
            // OK
        } catch (Exception ex) {
            fail("AccessException was expected.");
        }
    }

    /**
     * Test that a RequestException is thrown for getChangelist when the
     * execMapCmdList throws it.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangelistRequestException() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), any(String[].class), eq(null)))
                .thenThrow(RequestException.class);
        try {
            changeDelegator.getChangelist(MOCK_LIST_ID);
            fail("RequestException was expected.");
        } catch (RequestException reqEx) {
            // OK
        } catch (Exception ex) {
            fail("RequestException was expected.");
        }
    }

    /**
     * Test that a P4Exception is thrown for getChangelist when the
     * execMapCmdList throws it.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangelistP4JavaException() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), any(String[].class), eq(null)))
                .thenThrow(P4JavaException.class);
        try {
            changeDelegator.getChangelist(MOCK_LIST_ID);
            fail("P4JavaException was expected.");
        } catch (P4JavaException p4Ex) {
            // OK
        } catch (Exception ex) {
            fail("P4JavaException was expected.");
        }
    }

    /**
     * Test get getChangelist by id returns a change list with the correct id.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetChangelistbyId() throws P4JavaException {
        when(server.execMapCmdList(eq(CHANGE.toString()), argThat(GET_ID_PARAMS_MATCHER), eq(null)))
                .thenReturn(buildChangeList());
        IChangelist changelist = changeDelegator.getChangelist(MOCK_LIST_ID);
        verify(server).execMapCmdList(eq(CHANGE.toString()), argThat(GET_ID_PARAMS_MATCHER),
                eq(null));
        assertEquals(MOCK_LIST_ID, changelist.getId());
    }

    /**
     * Test get changelistby default id.
     * 
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetChangelistbyDefaultId() throws P4JavaException {
        when(server.execMapCmdList(eq(CHANGE.toString()), argThat(DEFAULT_ID_PARAMS_MATCHER),
                eq(null))).thenReturn(buildChangeList());
        changeDelegator.getChangelist(Changelist.DEFAULT);
        verify(server).execMapCmdList(eq(CHANGE.toString()), argThat(DEFAULT_ID_PARAMS_MATCHER),
                eq(null));
    }

    /**
     * Test get getChangelist by id and options returns a change list with the
     * correct id.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangelistbyIdAndOptions() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), argThat(GET_ID_PARAMS_MATCHER), eq(null)))
                .thenReturn(null);
        IChangelist cl = changeDelegator.getChangelist(MOCK_LIST_ID, mockChangelistOptions);
        verify(server).execMapCmdList(eq(CHANGE.toString()), argThat(GET_ID_PARAMS_MATCHER),
                eq(null));
        assertNull(cl, "Change list returned should have been null");
    }

    /**
     * Test get getChangelist by id returns a null change list.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetChangelistbyIdAndOptionsNull() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), argThat(GET_ID_PARAMS_MATCHER), eq(null)))
                .thenReturn(buildNullChangeList());
        IChangelist cl = changeDelegator.getChangelist(MOCK_LIST_ID, mockChangelistOptions);
        verify(server).execMapCmdList(eq(CHANGE.toString()), argThat(GET_ID_PARAMS_MATCHER),
                eq(null));
        assertNull(cl, "Change list returned should have been null");
    }

    /**
     * Test that a ConnectionException is thrown for deletePendingChangelist
     * when the execMapCmdList throws it.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testDeletePendingChangelistConnectionException() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), any(String[].class), eq(null)))
                .thenThrow(ConnectionException.class);
        try {
            changeDelegator.deletePendingChangelist(MOCK_LIST_ID);
            fail("ConnectionException was expected.");
        } catch (ConnectionException conEx) {
            // OK
        } catch (Exception ex) {
            fail("ConnectionException was expected.");
        }
    }

    /**
     * Test that an AccessException is thrown for deletePendingChangelist when
     * the execMapCmdList throws it.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testDeletePendingChangelistAccessException() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), any(String[].class), eq(null)))
                .thenThrow(AccessException.class);
        try {
            changeDelegator.deletePendingChangelist(MOCK_LIST_ID);
            fail("AccessException was expected.");
        } catch (AccessException accEx) {
            // OK
        } catch (Exception ex) {
            fail("AccessException was expected.");
        }
    }

    /**
     * Test that a RequestException is thrown for deletePendingChangelist when
     * the execMapCmdList throws it.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testDeletePendingChangelistRequestException() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), any(String[].class), eq(null)))
                .thenThrow(RequestException.class);
        try {
            changeDelegator.deletePendingChangelist(MOCK_LIST_ID);
            fail("RequestException was expected.");
        } catch (RequestException reqEx) {
            // OK
        } catch (Exception ex) {
            fail("RequestException was expected.");
        }
    }

    /**
     * Test that a P4JavaException is thrown for deletePendingChangelist when
     * the execMapCmdList throws it.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testDeletePendingChangelistP4JavaException() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), any(String[].class), eq(null)))
                .thenThrow(P4JavaException.class);
        try {
            changeDelegator.deletePendingChangelist(MOCK_LIST_ID);
            fail("P4JavaException was expected.");
        } catch (P4JavaException p4Ex) {
            // OK
        } catch (Exception ex) {
            fail("P4JavaException was expected.");
        }
    }

    /**
     * Test delete pending changelist.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testDeletePendingChangelist() throws Exception {
        // given
        when(server.execMapCmdList(eq(CHANGE.toString()), argThat(DELETE_ID_PARAMS_MATCHER),
                eq(null))).thenReturn(buildChangeList(MESSAGE_CODE_IN_INFO_RANGE));
        String deletePendingChangelistId = changeDelegator.deletePendingChangelist(MOCK_LIST_ID);
        assertEquals(String.valueOf(MOCK_LIST_ID), deletePendingChangelistId);
        verify(server).execMapCmdList(eq(CHANGE.toString()), argThat(DELETE_ID_PARAMS_MATCHER),
                eq(null));
    }

    /**
     * Builds the change list.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildChangeList(String... code0) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("fmt0", "%change%");
        if (code0.length == 0) {
            map.put("code0", MESSAGE_CODE_NOT_IN_INFO_RANGE);
        } else {
            map.put("code0", code0[0]);
        }
        map.put("change", "" + MOCK_LIST_ID);
        map.put("Change", "" + MOCK_LIST_ID);
        list.add(map);
        return list;
    }

    /**
     * Builds the null change list.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildNullChangeList() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(null);
        return list;
    }

    /**
     * Test delete pending changelist by Id and option.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testDeletePendingChangelistOption() throws Exception {
        when(server.execMapCmdList(eq(CHANGE.toString()), argThat(DELETE_ID_PARAMS_MATCHER),
                eq(null))).thenReturn(buildChangeList(MESSAGE_CODE_IN_INFO_RANGE));
        String deletePendingChangelistId = changeDelegator.deletePendingChangelist(MOCK_LIST_ID,
                mockChangelistOptions);
        verify(server).execMapCmdList(eq(CHANGE.toString()), argThat(DELETE_ID_PARAMS_MATCHER),
                eq(null));
        assertEquals(String.valueOf(MOCK_LIST_ID), deletePendingChangelistId);
    }
}