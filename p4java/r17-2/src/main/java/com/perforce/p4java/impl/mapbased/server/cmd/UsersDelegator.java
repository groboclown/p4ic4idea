package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.USERS;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.UserSummary;
import com.perforce.p4java.option.server.GetUsersOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IUsersDelegator;

/**
 * Implementation to handle the Users command.
 */
public class UsersDelegator extends BaseDelegator implements IUsersDelegator {
    /**
     * Instantiate a new UsersDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public UsersDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IUserSummary> getUsers(
            final List<String> userList,
            final GetUsersOptions opts) throws P4JavaException {

        String[] users = null;
        if (nonNull(userList)) {
            users = userList.toArray(new String[userList.size()]);
        }

        List<Map<String, Object>> resultMaps = execMapCmdList(
                USERS,
                processParameters(opts, null, users, server),
                null);

        return buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                new Function<Map, IUserSummary>() {
                    @Override
                    public IUserSummary apply(Map map) {
                        return new UserSummary(map, true);
                    }
                }
        );
    }

    @Override
    public List<IUserSummary> getUsers(
            final List<String> userList,
            final int maxUsers)
            throws ConnectionException, RequestException, AccessException {

        try {
            return getUsers(userList, new GetUsersOptions().setMaxUsers(maxUsers));
        } catch (final ConnectionException | RequestException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }
}
