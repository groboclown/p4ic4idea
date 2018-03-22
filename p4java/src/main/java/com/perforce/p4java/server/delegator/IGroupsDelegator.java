/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetUserGroupsOptions;

/**
 * Define the supported p4 groups commands in p4java.
 */
public interface IGroupsDelegator {

    /**
     * Get a list of Perforce user groups from the server.
     * <p>
     *
     * Note that the Perforce server considers it an error to have both indirect
     * and displayValues parameters set true; this will cause the server to
     * throw a RequestException with an appropriate usage message.
     *
     * @param userOrGroupName
     *            if non-null, restrict the list to the specified group or
     *            username.
     * @param opts
     *            GetUserGroupsOptions object describing optional parameters; if
     *            null, no options are set
     * @return a non-null but possibly-empty list of qualifying groups.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     */
    public List<IUserGroup> getUserGroups(final String userOrGroupName,
            final GetUserGroupsOptions opts) throws P4JavaException;
}
