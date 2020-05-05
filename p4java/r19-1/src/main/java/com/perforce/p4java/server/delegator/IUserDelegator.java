package com.perforce.p4java.server.delegator;

import javax.annotation.Nonnull;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.UpdateUserOptions;

/**
 * Interface to handle the User command.
 */
public interface IUserDelegator {
    /**
     * Create a new Perforce user on the Perforce server.
     *
     * @param user  non-null IUser defining the new user to be created.
     * @param force if true, force the creation of any named user; requires admin
     *              privileges,
     * @return possibly-null status message string as returned from the server
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    String createUser(@Nonnull IUser user, boolean force)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Create a new Perforce user on the Perforce server.
     *
     * @param user non-null IUser defining the new user to be created.
     * @param opts UpdateUserOptions object describing optional parameters; if
     *             null, no options are set
     * @return possibly-null status message string as returned from the server
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    String createUser(@Nonnull IUser user, UpdateUserOptions opts)
            throws P4JavaException;

    /**
     * Update a Perforce user on the Perforce server.
     *
     * @param user  non-null IUser defining the user to be updated
     * @param force if true, force update for users other than the caller.
     *              Requires super user / admin privileges (enforced by the
     *              server).
     * @return possibly-null status message string as returned from the server
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    String updateUser(@Nonnull IUser user, boolean force)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Update a Perforce user on the Perforce server.
     *
     * @param user non-null IUser defining the new user to be updated.
     * @param opts UpdateUserOptions object describing optional parameters; if
     *             null, no options are set
     * @return possibly-null status message string as returned from the server
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    String updateUser(@Nonnull IUser user, UpdateUserOptions opts)
            throws P4JavaException;

    /**
     * Delete a named Perforce user from the Perforce server.
     *
     * @param userName non-null name of the user to be deleted.
     * @param force    if true, force deletion for users other than the caller.
     *                 Requires super user / admin privileges (enforced by the
     *                 server).
     * @return possibly-null status message string as returned from the server
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    String deleteUser(String userName, boolean force)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Delete a named Perforce user from the Perforce server
     *
     * @param userName non-null name of the user to be deleted.
     * @param opts     UpdateUserOptions object describing optional parameters; if
     *                 null, no options are set
     * @return possibly-null status message string as returned from the server
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    String deleteUser(String userName, UpdateUserOptions opts)
            throws P4JavaException;

    /**
     * Get the user details of a specific Perforce user from the Perforce
     * server.
     *
     * @param userName if null, get the current user details, otherwise use the
     *                 passed-in user name.
     * @return IUser details for the user, or null if no such user is known.
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    IUser getUser(String userName)
            throws ConnectionException, RequestException, AccessException;
}
