package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.LOGOUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.LoginOptions;

/**
 * Tests the LogoutDelegator.
 */
public class LogoutDelegatorTest extends AbstractP4JavaUnitTest {

    /** The logout delegator. */
    private LogoutDelegator logoutDelegator;
    /** Empty matcher. */
    private static final CommandLineArgumentMatcher EMPTY_MATCHER = new CommandLineArgumentMatcher(
            new String[] {});

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        logoutDelegator = new LogoutDelegator(server);
    }

    /**
     * Sets up the server with an exception.
     *
     * @param exceptionClass
     *            the new up
     * @throws ConnectionException
     *             the connection exception
     * @throws AccessException
     *             the access exception
     * @throws RequestException
     *             the request exception
     */
    private void setUp(final Class<? extends P4JavaException> exceptionClass)
            throws ConnectionException, AccessException, RequestException {
        when(server.execMapCmdList(eq(LOGOUT.toString()), any(String[].class), any(Map.class)))
                .thenThrow(exceptionClass);
        when(server.getAuthTicket()).thenReturn("auth");
    }

    /**
     * Test logout connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testLogoutConnectionException() throws P4JavaException {
        setUp(ConnectionException.class);
        logoutDelegator.logout();
    }

    /**
     * Test logout access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testLogoutAccessException() throws P4JavaException {
        setUp(AccessException.class);
        logoutDelegator.logout();
    }

    /**
     * Test logout request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLogoutRequestException() throws P4JavaException {
        setUp(RequestException.class);
        logoutDelegator.logout();
    }

    /**
     * Test logout p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testLogoutP4JavaException() throws P4JavaException {
        setUp(P4JavaException.class);
        logoutDelegator.logout();
    }

    /**
     * Test logout opt connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testLogoutOptConnectionException() throws P4JavaException {
        setUp(ConnectionException.class);
        logoutDelegator.logout(null);
    }

    /**
     * Test logout opt access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testLogoutOptAccessException() throws P4JavaException {
        setUp(AccessException.class);
        logoutDelegator.logout(null);
    }

    /**
     * Test logout opt request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLogoutOptRequestException() throws P4JavaException {
        setUp(RequestException.class);
        logoutDelegator.logout(null);
    }

    /**
     * Test logout opt p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testLogoutOptP4JavaException() throws P4JavaException {
        setUp(P4JavaException.class);
        logoutDelegator.logout(null);
    }

    /**
     * Test logout.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLogout() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGOUT.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenReturn(null);
        when(server.getAuthTicket()).thenReturn("auth");
        logoutDelegator.logout();
        verify(server).execMapCmdList(eq(LOGOUT.toString()), argThat(EMPTY_MATCHER), eq(null));
        verify(server).setAuthTicket(null);
        verify(server).getAuthTicket();
    }

    /**
     * Test logout opt.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLogoutOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGOUT.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenReturn(null);
        when(server.getAuthTicket()).thenReturn("auth");
        logoutDelegator.logout(new LoginOptions());
        verify(server).execMapCmdList(eq(LOGOUT.toString()), argThat(EMPTY_MATCHER), eq(null));
        verify(server).setAuthTicket(null);
        verify(server).getAuthTicket();
    }
}