package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PASSWORD;
import static com.perforce.p4java.server.CmdSpec.LOGIN;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.LoginOptions;

/**
 * Tests the LoginDelegator.
 *
 * @see LoginDelegatorExceptionsTest
 */
public class LoginDelegatorTest extends AbstractP4JavaUnitTest {

    /** The login delegator. */
    private LoginDelegator loginDelegator;
    /** Empty matcher. */
    private static final CommandLineArgumentMatcher EMPTY_MATCHER = new CommandLineArgumentMatcher(
            new String[] {});
    /** Status matcher. */
    private static final CommandLineArgumentMatcher STATUS_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-s" });

    /** Example value. */
    private static final String TEST_HOST = "host";
    /** Example value. */
    private static final String TEST_USER = "testuser";
    /** Opt matcher. */
    private static final CommandLineArgumentMatcher OPT_MATCHER = 
            new CommandLineArgumentMatcher(
                    new String[] { "-a", "-p", "-h" + TEST_HOST });
    /** Opt matcher with user. */
    private static final CommandLineArgumentMatcher USER_OPT_MATCHER = 
            new CommandLineArgumentMatcher(
                    new String[] { "-a", "-p", "-h" + TEST_HOST, TEST_USER });

    /** All hosts matcher. */
    private static final CommandLineArgumentMatcher ALL_HOSTS_MATCHER = 
            new CommandLineArgumentMatcher(
                    new String[] { "-a" });

    /** Example value. */
    private static final String TEST_PASSWORD = "password";

    /** Example value. */
    private static final String STATUS_NOT_LOGGED_IN = 
            "Perforce password (%'P4PASSWD'%) invalid or unset.";

    /** Example value. */
    private static final String STATUS_LOGGED_IN = 
            "User %user% ticket expires in %hours% hours %minutes% minutes.";

    /** Example value. */
    private static final String LOGGED_IN = "User %user% logged in.";

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        loginDelegator = new LoginDelegator(server);
    }

    /**
     * Test is dont write ticket.
     */
    @Test
    public void testIsDontWriteTicket() {
        assertTrue("-p option should resolve as true",
                loginDelegator.isDontWriteTicket(LOGIN.name(), new String[] { "-p" }));
        assertTrue("-p option with others should resolve as true", loginDelegator
                .isDontWriteTicket(LOGIN.name(), new String[] { "-q", "-p", "-o", "-i" }));
        assertFalse("blank option should resolve as false",
                loginDelegator.isDontWriteTicket(LOGIN.name(), new String[] { "" }));
        assertFalse("empty option should resolve as false",
                loginDelegator.isDontWriteTicket(LOGIN.name(), new String[] {}));
        assertFalse("null option should resolve as false",
                loginDelegator.isDontWriteTicket(LOGIN.name(), null));
        assertFalse("other options should resolve as false",
                loginDelegator.isDontWriteTicket(LOGIN.name(), new String[] { "-q", "-o", "-i" }));
    }

    /**
     * Test login status.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginStatus() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(STATUS_MATCHER), eq(null)))
                .thenReturn(buildStatusLoggedInResultMap());
        String status = loginDelegator.getLoginStatus();
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(STATUS_MATCHER), eq(null));
        assertTrue(status.startsWith("User " + TEST_USER + " ticket"));
    }

    /**
     * Test login status not logged in.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginStatusNotLoggedIn() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(STATUS_MATCHER), eq(null)))
                .thenReturn(buildStatusNotLoggedInResultMap());
        String status = loginDelegator.getLoginStatus();
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(STATUS_MATCHER), eq(null));
        assertTrue(status.startsWith("Perforce password"));
    }

    /**
     * Test login status null.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginStatusNull() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(STATUS_MATCHER), eq(null)))
                .thenReturn(null);
        String status = loginDelegator.getLoginStatus();
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(STATUS_MATCHER), eq(null));
        assertTrue(isBlank(status));
    }

    /**
     * Test login status empty.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginStatusEmpty() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(STATUS_MATCHER), eq(null)))
                .thenReturn(new ArrayList<>());
        String status = loginDelegator.getLoginStatus();
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(STATUS_MATCHER), eq(null));
        assertTrue(isBlank(status));
    }

    /**
     * Test login ticket from server.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginTicketFromServer() throws P4JavaException {
        Map<String, Object> pwdMap = buildPwd();
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(EMPTY_MATCHER), eq(pwdMap)))
                .thenReturn(buildLoggedIn());
        when(server.getAuthTicket()).thenReturn("auth");
        StringBuffer ticketBuffer = new StringBuffer();
        loginDelegator.login(TEST_PASSWORD, ticketBuffer, null);
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(EMPTY_MATCHER), eq(pwdMap));
        verify(server).getAuthTicket();
        assertEquals("auth", ticketBuffer.toString());
    }

    /**
     * Test login with opt ticket from server.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginOptTicketFromServer() throws P4JavaException {
        LoginOptions options = new LoginOptions().setHost(TEST_HOST).setDontWriteTicket(true)
                .setAllHosts(true);
        Map<String, Object> pwdMap = buildPwd();
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(OPT_MATCHER), eq(pwdMap)))
                .thenReturn(buildLoggedIn());
        when(server.getAuthTicket()).thenReturn("auth");
        StringBuffer ticketBuffer = new StringBuffer();
        loginDelegator.login(TEST_PASSWORD, ticketBuffer, options);
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(OPT_MATCHER), eq(pwdMap));
        verify(server).getAuthTicket();
        assertEquals("auth", ticketBuffer.toString());
    }

    /**
     * Test login pwd with opt from server.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginPwdOptFromServer() throws P4JavaException {
        LoginOptions options = new LoginOptions().setHost(TEST_HOST).setDontWriteTicket(true)
                .setAllHosts(true);
        Map<String, Object> pwdMap = buildPwd();
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(OPT_MATCHER), eq(pwdMap)))
                .thenReturn(buildLoggedIn());
        when(server.getAuthTicket()).thenReturn("auth");
        loginDelegator.login(TEST_PASSWORD, options);
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(OPT_MATCHER), eq(pwdMap));
        verify(server).getAuthTicket();
    }

    /**
     * Test login pwd all hosts.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginPwdAllHosts() throws P4JavaException {
        Map<String, Object> pwdMap = buildPwd();
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(ALL_HOSTS_MATCHER), eq(pwdMap)))
                .thenReturn(buildLoggedIn());
        loginDelegator.login(TEST_PASSWORD, true);
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(ALL_HOSTS_MATCHER), eq(pwdMap));
    }

    /**
     * Test login pwd.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginPwd() throws P4JavaException {
        Map<String, Object> pwdMap = buildPwd();
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(EMPTY_MATCHER), eq(pwdMap)))
                .thenReturn(buildLoggedIn());
        loginDelegator.login(TEST_PASSWORD);
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(EMPTY_MATCHER), eq(pwdMap));
    }

    /**
     * Test login ticket from map.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginTicketFromMap() throws P4JavaException {
        Map<String, Object> pwdMap = buildPwd();
        final String ticket = "mapticket";
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(EMPTY_MATCHER), eq(pwdMap)))
                .thenReturn(buildLoggedIn(ticket));
        StringBuffer ticketBuffer = new StringBuffer();
        loginDelegator.login(TEST_PASSWORD, ticketBuffer, null);
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(EMPTY_MATCHER), eq(pwdMap));
        assertEquals(ticket, ticketBuffer.toString());
    }

    /**
     * Test login user with opt ticket from server.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testLoginUserOptTicketFromServer() throws P4JavaException {
        final int nameCalls = 3;
        IUser user = mock(IUser.class);
        when(user.getLoginName()).thenReturn(TEST_USER, TEST_USER, TEST_USER);
        LoginOptions options = new LoginOptions().setHost(TEST_HOST).setDontWriteTicket(true)
                .setAllHosts(true);
        when(server.execMapCmdList(eq(LOGIN.toString()), argThat(USER_OPT_MATCHER), eq(null)))
                .thenReturn(buildLoggedIn());
        when(server.getAuthTicket(TEST_USER)).thenReturn("auth");
        StringBuffer ticketBuffer = new StringBuffer();
        loginDelegator.login(user, ticketBuffer, options);
        verify(server).execMapCmdList(eq(LOGIN.toString()), argThat(USER_OPT_MATCHER), eq(null));
        verify(server).getAuthTicket(TEST_USER);
        verify(user, times(nameCalls)).getLoginName();
        assertEquals("auth", ticketBuffer.toString());
    }

    /**
     * Test login user null.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = NullPointerException.class)
    public void testLoginUserNull() throws P4JavaException {
        LoginOptions options = new LoginOptions().setHost(TEST_HOST).setDontWriteTicket(true)
                .setAllHosts(true);
        loginDelegator.login((IUser) null, null, options);
    }

    /**
     * Test login user no name.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLoginUserNoName() throws P4JavaException {
        IUser user = mock(IUser.class);
        when(user.getLoginName()).thenReturn("");
        LoginOptions options = new LoginOptions().setHost(TEST_HOST).setDontWriteTicket(true)
                .setAllHosts(true);
        StringBuffer ticketBuffer = new StringBuffer();
        loginDelegator.login(user, ticketBuffer, options);
    }

    /**
     * Builds the password map.
     *
     * @return the map
     */
    private Map<String, Object> buildPwd() {
        Map<String, Object> result = new HashMap<>();
        result.put(PASSWORD, TEST_PASSWORD + "\n");
        return result;
    }

    /**
     * Builds the logged in response.
     *
     * @param ticket
     *            the ticket
     * @return the list
     */
    private List<Map<String, Object>> buildLoggedIn(final String... ticket) {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        if (ticket != null && ticket.length > 0) {
            result.put("ticket", ticket[0]);
        } else {
            result.put("fmt0", LOGGED_IN);
            result.put("code0", "285220165");
            result.put("user", TEST_USER);
        }
        results.add(result);
        return results;
    }

    /**
     * Builds the status logged in result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildStatusLoggedInResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("fmt0", STATUS_LOGGED_IN);
        result.put("code0", "318774598");
        result.put("user", TEST_USER);
        result.put("minutes", "0");
        result.put("hours", "12");
        results.add(result);
        return results;
    }

    /**
     * Builds the status not logged in result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildStatusNotLoggedInResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("fmt0", STATUS_NOT_LOGGED_IN);
        result.put("code0", "807672853");
        results.add(result);
        return results;
    }
}