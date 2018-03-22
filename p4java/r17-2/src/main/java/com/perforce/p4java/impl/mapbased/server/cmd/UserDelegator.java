package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleErrorStr;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.isExistClientOrLabelOrUser;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.isInfoMessage;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapAsString;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString;
import static com.perforce.p4java.server.CmdSpec.USER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IUserDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the User command.
 */
public class UserDelegator extends BaseDelegator implements IUserDelegator {
    /**
     * Instantiate a new UserDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public UserDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String createUser(@Nonnull final IUser user, final boolean force)
            throws ConnectionException, RequestException, AccessException {

        Validate.notNull(user);
        return updateUser(user, force);
    }

    @Override
    public String createUser(@Nonnull final IUser user, final UpdateUserOptions opts)
            throws P4JavaException {

        return updateUser(user, opts);
    }

    @Override
    public String updateUser(@Nonnull IUser user, final boolean force)
            throws ConnectionException, RequestException, AccessException {

        try {
            return updateUser(user, new UpdateUserOptions(force));
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public String updateUser(@Nonnull final IUser user, final UpdateUserOptions opts)
            throws P4JavaException {

        Validate.notNull(user);
        Validate.notBlank(user.getLoginName());

        List<Map<String, Object>> resultMaps = execMapCmdList(
                USER,
                processParameters(opts, null, "-i", server),
                InputMapper.map(user));

        return parseCommandResultMapAsString(resultMaps);
    }

    @Override
    public String deleteUser(final String userName, final boolean force)
            throws ConnectionException, RequestException, AccessException {

        try {
            return deleteUser(userName, new UpdateUserOptions(force));
        } catch (final ConnectionException | RequestException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public String deleteUser(final String userName, final UpdateUserOptions opts)
            throws P4JavaException {

        Validate.notBlank(userName, "User name should not null or empty");
        List<Map<String, Object>> resultMaps = execMapCmdList(
                USER,
                processParameters(opts, null, new String[]{"-d", userName}, server),
                null);

        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }

    @Override
    public IUser getUser(final String userName)
            throws ConnectionException, RequestException, AccessException {
        String[] args = new String[] { "-o" };
        if (isNotBlank(userName)) {
            args = new String[] { "-o", userName };
        }

        List<Map<String, Object>> resultMaps = execMapCmdList(USER, args, null);
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                handleErrorStr(map);
                if (!isInfoMessage(map)) {
                    if (isExistClientOrLabelOrUser(map)) {
                        return new User(map, server);
                    }
                }
            }
        }

        return null;
    }
}
