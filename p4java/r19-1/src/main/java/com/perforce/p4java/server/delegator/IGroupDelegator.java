/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;

/**
 * Define the supported p4 groups commands in p4java.
 */
public interface IGroupDelegator {

    /**
     * Delete a Perforce user group from the Perforce server.
     *
     * @param group
     *            non-null group to be deleted.
     * @param opts
     *            which delete options to be applied
     * @return possibly-null status message string as returned from the server
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request.
     * @throws AccessException
     *             if the Perforce server denies access to the caller.
     */
    String deleteUserGroup(IUserGroup group, UpdateUserGroupOptions opts) throws P4JavaException;

    /**
     * Create a new Perforce user group on the Perforce server.
     *
     * @param group
     *            non-null IUserGroup to be created.
     * @param opts
     *            which create options to be applied
     * @return possibly-null status message string as returned from the server
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request.
     * @throws AccessException
     *             if the Perforce server denies access to the caller.
     */
    String createUserGroup(final IUserGroup group, final UpdateUserGroupOptions opts)
            throws P4JavaException;

    /**
     * Get the named Perforce user group. Note that since the Perforce server
     * usually interprets asking for a non-existent group as equivalent to
     * asking for a template for a new user group, you will normally always get
     * back a result here. It is best to first use the getUserGroups method to
     * see if the group exists, then use this method to retrieve a specific
     * group once you know it exists.
     * 
     * TODO: once we have finished the delegators, IOptionServer should be made
     * to extend IServer this definition can then be removed as there is no
     * Options object for getUserGroup
     *
     * @param name
     *            non-null group name.
     * @return IUserGroup representing the named user group if it exists on the
     *         server; null otherwise (but see note in main comments above).
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request.
     * @throws AccessException
     *             if the Perforce server denies access to the caller.
     */
    IUserGroup getUserGroup(final String name)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Update a Perforce user group on the Perforce server.
     *
     * @param group
     *            non-null user group to be updated.
     * @param opts
     *            which update options to be applied
     * @return possibly-null status message string as returned from the server
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request.
     * @throws AccessException
     *             if the Perforce server denies access to the caller.
     */
    String updateUserGroup(final IUserGroup group, final UpdateUserGroupOptions opts)
            throws P4JavaException;
}
