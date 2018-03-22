/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.server.IServer;
import org.apache.commons.lang3.Validate;

/**
 * Simple generic implementation class for the IUserGroup interface.
 */

public class UserGroup extends ServerResource implements IUserGroup {
    /**
     * How the Perforce server represents an unset group value as a string.
     */
    private static final String UNSET_STR = "unset";

    /**
     * How the Perforce server represents an unlimited group value as a string.
     */
    private static final String UNLIMITED_STR = "unlimited";

    private String name = null;
    private int maxResults = UNSET;
    private int maxScanRows = UNSET;
    /* TimeUnit: milliseconds */
    private int maxLockTime = UNSET;
    /* TimeUnit: seconds */
    private int timeout = UNSET;
    /* TimeUnit: seconds */
    private int passwordTimeout = UNSET;
    private boolean subGroup = false;
    private List<String> subgroups = new ArrayList<>();
    private List<String> owners = new ArrayList<>();
    private List<String> users = new ArrayList<>();

    /**
     * Simple convenience factory method to return a new local UserGroup object.
     * <p>
     *
     * All fields not passed as parameters here default to the defaults applied
     * by the associated default UserGroup constructor.
     *
     * @param name
     *            non-null name for the UserGroup.
     * @param users
     *            possibly-null list of users to be associated with the group.
     * @return new user group.
     */
    public static UserGroup newUserGroup(@Nonnull final String name,
            @Nonnull final List<String> users) {
        Validate.notNull(name);

        UserGroup group = new UserGroup();
        group.setName(name);
        group.setUsers(users);

        return group;
    }

    /**
     * @deprecated Please use method <code>addUser(String user)</code>
     */
    public void setUsers(List<String> users) {
        if (nonNull(users)) {
            this.users.addAll(users);
        }
    }

    /**
     * Default constructor. Sets all fields to null, UNSET, or false. Sets
     * superclass IServerResource fields complete, completeable, refereable and
     * updateable to true.
     */
    public UserGroup() {
        super(true, true);
    }

    /**
     * Construct a new user group impl from the passed-in map. Note that this
     * map must come from the Perforce "group" command or exact equivalent;
     * using a map passed back by (e.g.) the Perforce "groups" (note the plural)
     * command will fail due to the way the Perforce server returns group lists
     * rather than individual groups. Calling this with a null map argument is
     * equivalent to calling the default constructor.
     * <p>
     *
     * Sets superclass IServerResource fields complete, completeable, refereable
     * and updateable to true.
     */
    public UserGroup(@Nullable final Map<String, Object> map) {
        super(true, true);
        if (nonNull(map)) {
            try {
                name = (String) map.get(MapKeys.GROUP_KEY);
                maxLockTime = parseGroupIntValue((String) map.get(MapKeys.MAXLOCKTIME_KEY));
                maxResults = parseGroupIntValue((String) map.get(MapKeys.MAXRESULTS_KEY));
                maxScanRows = parseGroupIntValue((String) map.get(MapKeys.MAXSCANROWS_KEY));
                timeout = parseGroupIntValue((String) map.get(MapKeys.TIMEOUT_KEY));
                passwordTimeout = parseGroupIntValue(
                        (String) map.get(MapKeys.PASSWORD_TIMEOUT_KEY));

                addToListIfKeyIsNotExistInMap(users, map, MapKeys.USERS_KEY);
                addToListIfKeyIsNotExistInMap(owners, map, MapKeys.OWNERS_KEY);
                addToListIfKeyIsNotExistInMap(subgroups, map, MapKeys.SUBGROUPS_KEY);
            } catch (Throwable thr) {
                Log.warn("Unexpected exception in UserGroup constructor: %s", thr.getMessage());
                Log.exception(thr);
            }
        }
    }

    /**
     * Parse a Perforce server-side string representing a user group integer
     * value (such as timeout). Copes with "unset" and "unlimited" properly.
     */
    public int parseGroupIntValue(String str) {
        if (isNotBlank(str)) {
            if (UNSET_STR.equalsIgnoreCase(str)) {
                return UNSET;
            } else if (UNLIMITED_STR.equalsIgnoreCase(str)) {
                return UNLIMITED;
            } else {
                return new Integer(str);
            }
        }
        return UNSET;
    }

    private void addToListIfKeyIsNotExistInMap(@Nonnull final List<String> lists,
            @Nonnull Map<String, Object> map, @Nonnull String key) {

        for (int i = 0;; i++) {
            if (!map.containsKey(key + i)) {
                break;
            } else {
                lists.add((String) map.get(key + i));
            }
        }
    }

    public int getMaxLockTime() {
        return this.maxLockTime;
    }

    public void setMaxLockTime(int maxLockTimeOfMilliSeconds) {
        this.maxLockTime = maxLockTimeOfMilliSeconds;
    }

    public int getMaxResults() {
        return this.maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getMaxScanRows() {
        return this.maxScanRows;
    }

    public void setMaxScanRows(int maxScanRows) {
        this.maxScanRows = maxScanRows;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getOwners() {
        return this.owners;
    }

    public int getPasswordTimeout() {
        return passwordTimeout;
    }

    public void setPasswordTimeout(int passwordTimeoutOfSeconds) {
        this.passwordTimeout = passwordTimeoutOfSeconds;
    }

    public List<String> getSubgroups() {
        return this.subgroups;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeoutOfSeconds) {
        this.timeout = timeoutOfSeconds;
    }

    public List<String> getUsers() {
        return this.users;
    }

    public boolean isSubGroup() {
        return this.subGroup;
    }

    public void setSubGroup(boolean subGroup) {
        this.subGroup = subGroup;
    }

    @Override
    public void refresh() throws ConnectionException, RequestException, AccessException {
        IServer refreshServer = server;
        String refreshName = getName();
        if (nonNull(refreshServer) && isNotBlank(refreshName)) {
            IUserGroup refreshedUserGroup = refreshServer.getUserGroup(refreshName);
            if (nonNull(refreshedUserGroup)) {
                maxLockTime = refreshedUserGroup.getMaxLockTime();
                name = refreshedUserGroup.getName();
                maxResults = refreshedUserGroup.getMaxResults();
                maxScanRows = refreshedUserGroup.getMaxScanRows();
                owners = refreshedUserGroup.getOwners();
                subGroup = refreshedUserGroup.isSubGroup();
                timeout = refreshedUserGroup.getTimeout();
                subgroups = refreshedUserGroup.getSubgroups();
                users = refreshedUserGroup.getUsers();
            }
        }
    }

    @Override
    public void update() throws ConnectionException, RequestException, AccessException {
        server.updateUserGroup(this, false);
    }

    /**
     * @deprecated Please use method <code>addSubgroup(String subgroup)</code>
     */
    public void setSubgroups(List<String> subgroups) {
        if (nonNull(subgroups)) {
            this.subgroups.addAll(subgroups);
        }
    }

    /**
     * @deprecated Please use method <code>addOwner(String owner)</code>
     */
    public void setOwners(List<String> owners) {
        if (nonNull(owners)) {
            this.owners.addAll(owners);
        }
    }

    public UserGroup addOwner(final String owner) {
        if (isNotBlank(owner)) {
            owners.add(owner);
        }

        return this;
    }

    public UserGroup addSubgroup(final String subgroup) {
        if (isNotBlank(subgroup)) {
            subgroups.add(subgroup);
        }

        return this;
    }

    public UserGroup addUser(final String user) {
        if (isNotBlank(user)) {
            users.add(user);
        }

        return this;
    }
}
