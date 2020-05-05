package com.perforce.p4java.impl.mapbased.server.cmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.delegator.ICounterDelegator;

public class CounterDelegatorTest extends AbstractP4JavaUnitTest {
    private ICounterDelegator counterDelegator;

    private List<IFileSpec> mockFileSpecs;
    private IFileSpec mockFileSpec;
    private String mockCounterName = "myCounter";
    private String mockCounterValue = "42";

    @Before
    public void beforeEach() {
        server = mock(Server.class);
        counterDelegator = new CounterDelegator(server);

        mockFileSpecs = new ArrayList<IFileSpec>();
        mockFileSpec = mock(IFileSpec.class);
        mockFileSpecs.add(mockFileSpec);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getCounterCheckConnectionException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { mockCounterName }), (Map<String, Object>) any()))
                        .thenThrow(ConnectionException.class);

        try {
            counterDelegator.getCounter(mockCounterName);
        } catch (ConnectionException e) {
            return;
        }
        fail("Did not return ConnectionException");
    }

    /**
     * Check that an AccessException is correctly thrown from an internal
     * method.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getCounterCheckAccessException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { mockCounterName }), (Map<String, Object>) any()))
                        .thenThrow(AccessException.class);

        try {
            counterDelegator.getCounter(mockCounterName);
        } catch (AccessException e) {
            return;
        }
        fail("Did not return AccessException");
    }

    /**
     * Check that a RequestException is correctly thrown from an internal
     * method.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getCounterCheckRequestException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { mockCounterName }), (Map<String, Object>) any()))
                        .thenThrow(RequestException.class);

        try {
            counterDelegator.getCounter(mockCounterName);
        } catch (RequestException e) {
            return;
        }
        fail("Did not return RequestException");
    }

    /**
     * Check that a RequestException is correctly thrown when an internal method
     * throws a P4JavaException.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getCounterCheckP4JavaException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { mockCounterName }), (Map<String, Object>) any()))
                        .thenThrow(P4JavaException.class);

        try {
            counterDelegator.getCounter(mockCounterName);
        } catch (RequestException e) {
            return;
        }
        fail("Did not return RequestException");
    }

    /**
     * Check that a non-blank counter name is returned.
     * 
     * @throws Exception
     *             If something goes wrong.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getCounterReturnNonBlankCounterName() throws Exception {
        // given
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("value", mockCounterValue);
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(map);

        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { mockCounterName }), (Map<String, Object>) any()))
                        .thenReturn(list);

        // when
        String counter = counterDelegator.getCounter(mockCounterName);
        // then
        assertThat(counter, is(mockCounterValue));
    }

    @Test
    public void getCounterWhenCounterNameIsBlank() throws Exception {
        try {
            counterDelegator.getCounter("", new CounterOptions());
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("Did not throw exception with blank counter name");

    }

    @SuppressWarnings("unchecked")
    @Test
    public void getCounterWithOptionsReturnNonBlankCounterName() throws Exception {
        // given
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("value", mockCounterValue);
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(map);

        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { mockCounterName }), (Map<String, Object>) any()))
                        .thenReturn(list);

        // when
        String counter = counterDelegator.getCounter(mockCounterName, new CounterOptions());
        // then
        assertThat(counter, is(mockCounterValue));
    }

    @Test
    public void setCounterBlankNameShouldFail() throws Exception {

        try {
            counterDelegator.setCounter("", mockCounterValue, true);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("Did not throw exception if counter name was blank");
    }

    @Test
    public void setCounterBlankValueShouldFail() throws Exception {

        try {
            counterDelegator.setCounter(mockCounterName, "", true);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("Did not throw exception if counter value was blank");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setCounterThrowConnectionException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { mockCounterName, mockCounterValue }),
                (Map<String, Object>) any())).thenThrow(ConnectionException.class);

        try {
            counterDelegator.setCounter(mockCounterName, mockCounterValue, new CounterOptions());
        } catch (ConnectionException e) {
            return;
        }
        fail("Did not return ConnectionException");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setCounterThrowAccessException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { mockCounterName, mockCounterValue }),
                (Map<String, Object>) any())).thenThrow(AccessException.class);

        try {
            counterDelegator.setCounter(mockCounterName, mockCounterValue, new CounterOptions());
        } catch (AccessException e) {
            return;
        }
        fail("Did not return AccessException");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setCounterThrowRequestException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { mockCounterName, mockCounterValue }),
                (Map<String, Object>) any())).thenThrow(RequestException.class);

        try {
            counterDelegator.setCounter(mockCounterName, mockCounterValue, new CounterOptions());
        } catch (RequestException e) {
            return;
        }
        fail("Did not return RequestException");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setCounterP4ExceptionThrowsRequestException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { "-f", mockCounterName, mockCounterValue }),
                (Map<String, Object>) any())).thenThrow(P4JavaException.class);

        try {
            counterDelegator.setCounter(mockCounterName, mockCounterValue, true);
        } catch (RequestException e) {
            return;
        } catch (Exception e) {
            fail("Threw " + e.getClass().getName() + " instead of RequestException");
        }
        fail("Did not return RequestException");
    }

    @Test
    public void deleteCounterCheckIllegalArgument() throws Exception {
        try {
            counterDelegator.deleteCounter("", true);
        } catch (IllegalArgumentException e) {
            return;
        } catch (Throwable e) {
            // Ignore.
        }
        fail("Delete with blank name did not return IllegalArgumentException");
    }

    @Test
    public void deleteCounterCheckConnectionException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { "-f", "-d", mockCounterName }), eq((Map<String, Object>) null)))
                        .thenThrow(ConnectionException.class);

        try {
            counterDelegator.deleteCounter(mockCounterName, true);
        } catch (ConnectionException e) {
            return;
        } catch (Throwable e) {
            // Ignore.
        }
        fail("Did not throw ConnectionException");
    }

    @Test
    public void deleteCounterCheckAccessException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { "-f", "-d", mockCounterName }), eq((Map<String, Object>) null)))
                        .thenThrow(AccessException.class);

        try {
            counterDelegator.deleteCounter(mockCounterName, true);
        } catch (AccessException e) {
            return;
        } catch (Throwable e) {
            // Ignore.
        }
        fail("Did not throw AccessException");
    }

    @Test
    public void deleteCounterCheckRequestException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { "-f", "-d", mockCounterName }), eq((Map<String, Object>) null)))
                        .thenThrow(RequestException.class);

        try {
            counterDelegator.deleteCounter(mockCounterName, true);
        } catch (RequestException e) {
            return;
        } catch (Throwable e) {
            // Ignore.
        }
        fail("Did not throw RequestException");
    }

    @Test
    public void deleteCounterCheckRequestExceptionWhenP4JavaException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.COUNTER.toString()),
                eq(new String[] { "-f", "-d", mockCounterName }), eq((Map<String, Object>) null)))
                        .thenThrow(P4JavaException.class);

        try {
            counterDelegator.deleteCounter(mockCounterName, true);
        } catch (RequestException e) {
            return;
        } catch (Throwable e) {
            // Ignore.
        }
        fail("Did not throw RequestException");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteCounter() throws Exception {
        // given
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("value", mockCounterValue);
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(map);

        when(server.execMapCmdList(any(String.class), any(String[].class),
                (Map<String, Object>) any(Map.class))).thenReturn(list);
        // when
        counterDelegator.deleteCounter(mockCounterName, true);
        // then
        String[] args = new String[] { "-f", "-d", mockCounterName };
        verify(server).execMapCmdList(eq("counter"), eq(args), eq((Map<String, Object>) null));
    }
}
