package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwAccessExceptionIfConditionFails;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PASSWORD;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.LOGIN;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ILoginDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation for 'p4 login'.
 */
public class LoginDelegator extends BaseDelegator implements ILoginDelegator {

    /**
     * Instantiates a new login delegator.
     *
     * @param server
     *            the server
     */
    public LoginDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public String getLoginStatus() throws P4JavaException {
        String statusStr = EMPTY;
        List<Map<String, Object>> resultMaps = execMapCmdList(LOGIN, new String[] { "-s" }, null);

        if (nonNull(resultMaps) && !resultMaps.isEmpty()) {
            Map<String, Object> firstResultMap = resultMaps.get(0);
            statusStr = ResultMapParser.getInfoStr(firstResultMap);
            if (isBlank(statusStr)) {
                // it's probably an error message:
                statusStr = ResultMapParser.getErrorStr(firstResultMap);
            }
        }

        return isBlank(statusStr) ? EMPTY : statusStr; // guaranteed non-null
                                                       // return
    }

    @Override
    public void login(final String password)
            throws ConnectionException, RequestException, AccessException, ConfigException {

        login(password, false);
    }

    @Override
    public void login(final String password, final boolean allHosts)
            throws ConnectionException, RequestException, AccessException, ConfigException {

        try {
            login(password, new LoginOptions().setAllHosts(allHosts));
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public boolean isDontWriteTicket(final String cmd, final String[] cmdArgs) {
        if (isNotBlank(cmd)) {
            if (LOGIN.toString().equalsIgnoreCase(cmd)) {
                if (nonNull(cmdArgs)) {
                    for (String arg : cmdArgs) {
                        if (isNotBlank(arg) && "-p".equals(arg)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void login(final String password, final LoginOptions opts) throws P4JavaException {
        login(password, null, opts);
    }

    @Override
    public void login(final String password, final StringBuffer ticket, final LoginOptions opts)
            throws P4JavaException {

        String actualPassword = password;
        if (isNotBlank(password)) {
            actualPassword = password + "\n";
        }

        LoginOptions actualOpts = opts;
        if (isNull(opts)) {
            actualOpts = new LoginOptions();
        }

        Map<String, Object> pwdMap = new HashMap<>();
        pwdMap.put(PASSWORD, actualPassword);

        List<Map<String, Object>> resultMaps = execMapCmdList(LOGIN,
                processParameters(actualOpts, server), pwdMap);

        String authTicket = null;
        String retVal = EMPTY;
        if (nonNull(resultMaps) && !resultMaps.isEmpty()) {
            Map<String, Object> firstResultMap = resultMaps.get(0);
            if (nonNull(firstResultMap) && !ResultMapParser.handleErrorStr(firstResultMap)
                    && ResultMapParser.isInfoMessage(firstResultMap)) {

                retVal = ResultMapParser.getInfoStr(firstResultMap);
            } else if (firstResultMap.containsKey(RpcFunctionMapKey.TICKET)) {
                authTicket = (String) firstResultMap.get(RpcFunctionMapKey.TICKET);
            }
        }

        // At this point, either login is successful or no login is necessary.
        // Handle login with actualPassword not set on the server (code0 =
        // 268442937)
        // If the passed-in 'actualPassword' parameter is not null/empty and
        // the return message indicates login not required ("'login' not
        // necessary, no actualPassword set for this user."), throw access
        // exception.
        if (isNotBlank(actualPassword) && isNotBlank(retVal)) {
            throwAccessExceptionIfConditionFails(!server.isLoginNotRequired(retVal), retVal);
        }

        // Note: if the ticket StringBuffer is non-null the auth ticket will
        // be appended to the end the of the buffer. If the buffer originally
        // has content it will remain there.
        if (isBlank(authTicket)) {
            authTicket = server.getAuthTicket();
        }
        if (nonNull(ticket) && isNotBlank(authTicket)) {
            ticket.append(authTicket);
        }
    }

    @Override
    public void login(@Nonnull final IUser user, final StringBuffer ticket, final LoginOptions opts)
            throws P4JavaException {

        Validate.notNull(user);
        Validate.notBlank(user.getLoginName(), "Login name shouldn't null or empty");

        List<Map<String, Object>> resultMaps = execMapCmdList(LOGIN,
                processParameters(opts, null, user.getLoginName(), server), null);

        String authTicket = null;

        if (nonNull(resultMaps) && !resultMaps.isEmpty()) {
            for (Map<String, Object> map : resultMaps) {
                if (nonNull(map)) {
                    if (map.containsKey(RpcFunctionMapKey.TICKET)) {
                        authTicket = (String) map.get(RpcFunctionMapKey.TICKET);
                    } else {
                        ResultMapParser.handleErrorStr(map);
                    }
                }
            }
        }

        // Note: if the ticket StringBuffer is non-null the auth ticket will
        // be appended to the end the of the buffer. If the buffer originally
        // has content it will remain there.
        if (isBlank(authTicket)) {
            authTicket = server.getAuthTicket(user.getLoginName());
        }
        if (nonNull(ticket) && isNotBlank(authTicket)) {
            ticket.append(authTicket);
        }
    }
}
