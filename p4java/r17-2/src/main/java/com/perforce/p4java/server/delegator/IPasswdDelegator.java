package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;

/**
 * Implementation to handle the Passwd command.
 */
public interface IPasswdDelegator {
    /**
     * Change a user's password on the server. After a password is changed for a
     * user, the user must login again with the new password. Specifying a
     * username as an argument to this command requires 'super' access granted
     * by 'p4 protect'
     * <p>
     * <p>
     * Note: setting the 'newPassword' to null or empty will delete the
     * password.
     *
     * @param oldPassword possibly-null or possibly-empty user's old password. If null
     *                    or empty, it assumes the current password is not set.
     * @param newPassword non-null and non-empty user's new password.
     * @param userName    possibly-null possibly-null name of the target user whose
     *                    password will be changed to the new password. If null, the
     *                    current user will be used. If non-null, this command requires
     *                    'super' access granted by 'p4 protect'.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    String changePassword(
            String oldPassword,
            String newPassword,
            String userName) throws P4JavaException;
}
