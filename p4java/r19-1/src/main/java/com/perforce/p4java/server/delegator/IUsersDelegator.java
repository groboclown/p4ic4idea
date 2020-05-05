package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetUsersOptions;

/**
 * Interface to handle the Users command.
 */
public interface IUsersDelegator {
    /**
     * Get a list of Perforce users known to this Perforce server. Note that
     * maxUsers and the user list are supposed to be mutually exclusive in
     * usage, but this is not enforced by P4Java as the restriction doesn't
     * make much sense and may be lifted in the Perforce server later.<p>
     * <p>
     * Note that this implementation differs a bit from the p4 command line
     * version in that it simply doesn't return any output for unmatched users.
     *
     * @param userList if non-null, restrict output to users matching the passed-in list of users.
     * @param opts     GetUsersOptions object describing optional parameters; if null, no options are
     *                 set
     * @return non-null (but possibly empty) list of non-null IUserSummary objects representing the
     * underlying Perforce users (if any).
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    List<IUserSummary> getUsers(
            List<String> userList,
            GetUsersOptions opts) throws P4JavaException;

    /**
     * Get a list of Perforce users known to this Perforce server. Note that
     * maxUsers and the user list are supposed to be mutually exclusive in
     * usage, but this is not enforced by P4Java as the restriction doesn't make
     * much sense and may be lifted in the Perforce server later.
     * <p>
     * <p>
     * Note that this implementation differs a bit from the p4 command line
     * version in that it simply doesn't return any output for unmatched users.
     *
     * @param userList if non-null, restrict output to users matching the passed-in
     *                 list of users.
     * @param maxUsers if positive, only return the first maxUsers users.
     * @return non-null (but possibly empty) list of non-null IUserSummary
     * objects representing the underlying Perforce users (if any).
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    List<IUserSummary> getUsers(
            List<String> userList,
            int maxUsers) throws ConnectionException, RequestException, AccessException;
}
