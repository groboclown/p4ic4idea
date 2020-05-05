package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.ChangelistOptions;

/**
 * Interface for ChangeDelegator implementations.
 */
public interface IChangeDelegator {

    /**
     * Get a specific Perforce changelist from a Perforce server.
     * <p>
     * 
     * Corresponds fairly well to the p4 command-line command "change -o", and
     * (like "change -o") does <i>not</i> include the associated changelist
     * files (if any) in the returned changelist object -- you must use
     * getChangelistFiles (or similar) to properly populate the changelist for
     * submission, for example.
     *
     * @param id
     *            the Perforce changelist ID; if id is IChangelist.DEFAULT, get
     *            the default changelist for the current client (if available)
     * @return non-null IChangelist describing the changelist; if no such
     *         changelist, a RequestException is thrown.
     * @throws ConnectionException
     *             the connection exception
     * @throws RequestException
     *             the request exception
     * @throws AccessException
     *             the access exception
     */
    IChangelist getChangelist(int id) throws ConnectionException, RequestException, AccessException;

    /**
     * Delete a pending Perforce changelist. Throws a P4JavaException if the
     * changelist was associated with opened files or was not a pending
     * changelist.
     * <p>
     * 
     * Note: any IChangelist object associated with the given changelist will no
     * longer be valid after this operation, and using that object may cause
     * undefined results or even global disaster -- you must ensure that the
     * object is not used again improperly.
     *
     * @param id
     *            the ID of the Perforce pending changelist to be deleted.
     * @return possibly-null operation result message string from the Perforce
     *         server
     * @throws ConnectionException
     *             the connection exception
     * @throws RequestException
     *             the request exception
     * @throws AccessException
     *             the access exception
     */
    String deletePendingChangelist(int id)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Delete a pending Perforce changelist. Throws a P4JavaException if the
     * changelist was associated with opened files or was not a pending
     * changelist.
     * <p>
     *
     * Note: any IChangelist object associated with the given changelist will no
     * longer be valid after this operation, and using that object may cause
     * undefined results or even global disaster -- you must ensure that the
     * object is not used again improperly.
     *
     * @param id
     *            the ID of the Perforce pending changelist to be deleted.
     * @param opts
     *            ChangelistOptions object describing optional parameters; if
     *            null, no options are set.
     * @return possibly-null operation result message string from the Perforce
     *         server
     * @throws P4JavaException
     *             if any error occurs in the processing of this method
     */
    String deletePendingChangelist(int id, ChangelistOptions opts) throws P4JavaException;

    /**
     * Get a specific Perforce changelist from a Perforce server.
     * <p>
     *
     * Corresponds fairly well to the p4 command-line command "change -o", and
     * (like "change -o") does <i>not</i> include the associated changelist
     * files (if any) in the returned changelist object -- you must use
     * getChangelistFiles (or similar) to properly populate the changelist for
     * submission, for example.
     *
     * @param id
     *            the Perforce changelist ID; if id is IChangelist.DEFAULT, get
     *            the default changelist for the current client (if available)
     * @param opts
     *            ChangelistOptions object describing optional parameters; if
     *            null, no options are set.
     * @return non-null IChangelist describing the changelist; if no such
     *         changelist, a RequestException is thrown.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method
     */
    IChangelist getChangelist(int id, ChangelistOptions opts) throws P4JavaException;
}
