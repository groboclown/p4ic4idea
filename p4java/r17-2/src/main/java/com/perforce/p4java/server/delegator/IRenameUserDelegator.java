package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;

/**
 * Interface to handle the RenameUser command.
 */
public interface IRenameUserDelegator {
    /**
     * Completely renames a user, modifying all database records which mention
     * the user.
     * <p>
     * <p>
     * This includes all workspaces, labels, branches, streams, etc. which are
     * owned by the user, all pending, shelved, and committed changes created by
     * the user, any files that the user has opened or shelved, any fixes that
     * the user made to jobs, any properties that apply to the user, any groups
     * that the user is in, and the user record itself.
     * <p>
     * <p>
     * The username is not changed in descriptive text fields (such as job
     * descriptions, change descriptions, or workspace descriptions), only where
     * it appears as the owner or user field of the database record.
     * <p>
     * <p>
     * Protection table entries that apply to the user are updated only if the
     * Name: field exactly matches the user name; if the Name: field contains
     * wildcards, it is not modified.
     * <p>
     * <p>
     * The only job field that is processed is attribute code 103. If you have
     * included the username in other job fields they will have to be processed
     * separately.
     * <p>
     * <p>
     * The full semantics of this operation are found in the main 'p4 help'
     * documentation.
     * <p>
     * <p>
     * This method requires 'super' access granted by 'p4 protect'.
     *
     * @param oldUserName the old user name to be changed.
     * @param newUserName the new user name to be changed to.
     * @return non-null result message string from the reload operation.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2014.1
     */
    String renameUser(String oldUserName, String newUserName) throws P4JavaException;
}
