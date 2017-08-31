package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.LOGIN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.LoginOptions;

/**
 * Test the login delegator for the exceptions that can be thrown by
 * execMapCmdList.
 * @see LoginDelegatorTest
 */
public class LoginDelegatorExceptionsTest extends AbstractP4JavaUnitTest {

    /** The login delegator. */
    private LoginDelegator loginDelegator;

    /** Password. */
    private static final String PASSWORD = "password";

    /** User name. */
    private static final String USER_NAME = "user";

    /** The user. */
    private IUser user;

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        user = mock(IUser.class);
        when(user.getLoginName()).thenReturn(USER_NAME);
        loginDelegator = new LoginDelegator(server);
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
        when(server.execMapCmdList(eq(LOGIN.toString()), any(String[].class), any(Map.class)))
                .thenThrow(exceptionClass);
    }

    /**
     * Test login pwd connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testLoginPwdConnectionException() throws P4JavaException {
        setUp(ConnectionException.class);
        loginDelegator.login(PASSWORD);
    }

    /**
     * Test login pwd opt connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testLoginPwdOptConnectionException() throws P4JavaException {
        setUp(ConnectionException.class);
        loginDelegator.login(PASSWORD, new LoginOptions());
    }

    /**
     * Test login pwd tick opt connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testLoginPwdTickOptConnectionException() throws P4JavaException {
        setUp(ConnectionException.class);
        loginDelegator.login(PASSWORD, new StringBuffer(), new LoginOptions());
    }

    /**
     * Test login user tick opt connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testLoginUserTickOptConnectionException() throws P4JavaException {
        setUp(ConnectionException.class);
        loginDelegator.login(user, new StringBuffer(), new LoginOptions());
    }

    /**
     * Test login pwd hosts connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testLoginPwdHostsConnectionException() throws P4JavaException {
        setUp(ConnectionException.class);
        loginDelegator.login(PASSWORD, false);
    }

    /**
     * Test login status connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testLoginStatusConnectionException() throws P4JavaException {
        setUp(ConnectionException.class);
        loginDelegator.getLoginStatus();
    }

    /**
     * Test login pwd access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testLoginPwdAccessException() throws P4JavaException {
        setUp(AccessException.class);
        loginDelegator.login(PASSWORD);
    }

    /**
     * Test login pwd opt access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testLoginPwdOptAccessException() throws P4JavaException {
        setUp(AccessException.class);
        loginDelegator.login(PASSWORD, new LoginOptions());
    }

    /**
     * Test login pwd tick opt access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testLoginPwdTickOptAccessException() throws P4JavaException {
        setUp(AccessException.class);
        loginDelegator.login(PASSWORD, new StringBuffer(), new LoginOptions());
    }

    /**
     * Test login user tick opt access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testLoginUserTickOptAccessException() throws P4JavaException {
        setUp(AccessException.class);
        loginDelegator.login(user, new StringBuffer(), new LoginOptions());
    }

    /**
     * Test login pwd hosts access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testLoginPwdHostsAccessException() throws P4JavaException {
        setUp(AccessException.class);
        loginDelegator.login(PASSWORD, false);
    }

    /**
     * Test login status access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testLoginStatusAccessException() throws P4JavaException {
        setUp(AccessException.class);
        loginDelegator.getLoginStatus();
    }

    /**
     * Test login pwd request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLoginPwdRequestException() throws P4JavaException {
        setUp(RequestException.class);
        loginDelegator.login(PASSWORD);
    }

    /**
     * Test login pwd opt request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLoginPwdOptRequestException() throws P4JavaException {
        setUp(RequestException.class);
        loginDelegator.login(PASSWORD, new LoginOptions());
    }

    /**
     * Test login pwd tick opt request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLoginPwdTickOptRequestException() throws P4JavaException {
        setUp(RequestException.class);
        loginDelegator.login(PASSWORD, new StringBuffer(), new LoginOptions());
    }

    /**
     * Test login user tick opt request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLoginUserTickOptRequestException() throws P4JavaException {
        setUp(RequestException.class);
        loginDelegator.login(user, new StringBuffer(), new LoginOptions());
    }

    /**
     * Test login pwd hosts request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLoginPwdHostsRequestException() throws P4JavaException {
        setUp(RequestException.class);
        loginDelegator.login(PASSWORD, false);
    }

    /**
     * Test login status request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLoginStatusRequestException() throws P4JavaException {
        setUp(RequestException.class);
        loginDelegator.getLoginStatus();
    }

    /**
     * Test login pwd p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testLoginPwdP4JavaException() throws P4JavaException {
        setUp(P4JavaException.class);
        loginDelegator.login(PASSWORD);
    }

    /**
     * Test login pwd opt p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testLoginPwdOptP4JavaException() throws P4JavaException {
        setUp(P4JavaException.class);
        loginDelegator.login(PASSWORD, new LoginOptions());
    }

    /**
     * Test login pwd tick opt p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testLoginPwdTickOptP4JavaException() throws P4JavaException {
        setUp(P4JavaException.class);
        loginDelegator.login(PASSWORD, new StringBuffer(), new LoginOptions());
    }

    /**
     * Test login user tick opt p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testLoginUserTickOptP4JavaException() throws P4JavaException {
        setUp(P4JavaException.class);
        loginDelegator.login(user, new StringBuffer(), new LoginOptions());
    }

    /**
     * Test login pwd hosts p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testLoginPwdHostsP4JavaException() throws P4JavaException {
        setUp(P4JavaException.class);
        loginDelegator.login(PASSWORD, false);
    }

    /**
     * Test login status p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testLoginStatusP4JavaException() throws P4JavaException {
        setUp(P4JavaException.class);
        loginDelegator.getLoginStatus();
    }
}