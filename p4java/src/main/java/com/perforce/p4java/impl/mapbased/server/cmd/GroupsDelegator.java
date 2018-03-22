package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.server.CmdSpec.GROUPS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.GetUserGroupsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IGroupsDelegator;

/**
 * @author Sean Shou
 * @since 12/09/2016
 */
public class GroupsDelegator extends BaseDelegator implements IGroupsDelegator {

    /**
     * Instantiate a new GroupsDelegator for the given server implemention.
     * 
     * @param server
     *            the server to delegate for
     */
    public GroupsDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IUserGroup> getUserGroups(final String userOrGroupName,
            final GetUserGroupsOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(GROUPS,
                Parameters.processParameters(opts, null, userOrGroupName, server), null);

        Map<String, UserGroup> userGroupMap = new HashMap<>();
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                ResultMapParser.handleErrorStr(map);
                UserGroup ugImpl;
                // The server returns the results not as a series of rows, each
                // row
                // representing a single group, but as a series of rows, each
                // row representing
                // a single *user*, meaning we need to do reverse correlation.
                // At least they come
                // back sorted by group name from the server, but even still,
                // this makes for
                // less than optimal decoding...
                String groupName = parseString(map, MapKeys.GROUP_LC_KEY);
                if (userGroupMap.containsKey(groupName)) {
                    ugImpl = userGroupMap.get(groupName);
                } else {
                    ugImpl = new UserGroup();
                    ugImpl.setName(groupName);
                    userGroupMap.put(groupName, ugImpl);
                }

                try {
                    String userName = parseString(map, MapKeys.USER_LC_KEY);
                    String maxScanRows = parseString(map, MapKeys.MAXSCANROWS_LC_KEY);
                    String maxLockTime = parseString(map, MapKeys.MAXLOCKTIME_LC_KEY);
                    String timeout = parseString(map, MapKeys.TIMEOUT_LC_KEY);
                    String passwordTimeout = parseString(map, MapKeys.PASSWORD_TIMEOUT_LC_KEY);
                    String maxResults = parseString(map, MapKeys.MAXRESULTS_LC_KEY);
                    String isOwner = parseString(map, MapKeys.ISOWNER_LC_KEY);
                    String isSubGroup = parseString(map, MapKeys.ISSUBGROUP_LC_KEY);

                    if ("1".equals(isOwner)) {
                        ugImpl.addOwner(userName);
                    }

                    if ("1".equals(isSubGroup)) {
                        ugImpl.addSubgroup(userName);
                    } else {
                        ugImpl.addUser(userName);
                    }

                    if (isNotBlank(maxScanRows)) {
                        ugImpl.setMaxScanRows(Integer.parseInt(maxScanRows));
                    }

                    if (isNotBlank(maxLockTime)) {
                        ugImpl.setMaxLockTime(Integer.parseInt(maxLockTime));
                    }

                    if (isNotBlank(timeout)) {
                        ugImpl.setTimeout(Integer.parseInt(timeout));
                    }

                    if (isNotBlank(maxResults)) {
                        ugImpl.setMaxResults(Integer.parseInt(maxResults));
                    }

                    if (isNotBlank(passwordTimeout)) {
                        ugImpl.setPasswordTimeout(Integer.parseInt(passwordTimeout));
                    }
                } catch (Throwable thr) {
                    Log.warn("Unexpected exception in ServerImpl.getUserGroups: %s",
                            thr.getMessage());
                    Log.exception(thr);
                }
            }
        }
        return new ArrayList<IUserGroup>(userGroupMap.values());
    }

    /**
     * Implemented on behalf of legacy clients.
     * 
     * @see com.perforce.p4java.server.IServer#getUserGroups(String, boolean, boolean, int)
     */
    public List<IUserGroup> getUserGroups(final String userOrGroupName, final boolean indirect,
            final boolean displayValues, final int maxGroups)
            throws ConnectionException, RequestException, AccessException {

        try {
            return getUserGroups(userOrGroupName, new GetUserGroupsOptions().setIndirect(indirect)
                    .setDisplayValues(displayValues).setMaxGroups(maxGroups));
        } catch (final ConnectionException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }
}