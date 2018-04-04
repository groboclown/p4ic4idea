package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetReviewsOptions;


/**
 * Interface to handle the Reviews command.
 */
public interface IReviewsDelegator {
    /**
     * Get a list of all users who have subscribed to review the named files.
     * <p>
     * Note that the returned IUserSummary objects will have null access and
     * update dates associated with them.
     *
     * @param fileSpecs if not null, use this list as the list of named files rather
     *                  than all files.
     * @param opts      GetReviewsOptions object describing optional parameters; if
     *                  null, no options are set.
     * @return non-null but possibly empty list of IUserSummary objects; note
     * that these objects will have null update and access fields.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    List<IUserSummary> getReviews(
            List<IFileSpec> fileSpecs,
            GetReviewsOptions opts) throws P4JavaException;

    /**
     * Get a list of all users who have subscribed to review the named files,
     * the files in the numbered changelist, or all files by default.
     * <p>
     * <p>
     * Note that the returned IUserSummary objects will have null access and
     * update dates associated with them.
     *
     * @param changelistId if not IChangelist.UNKNOWN, use this changelist ID.
     * @param fileSpecs    if not null, use this list as the list of named files rather
     *                     than all files.
     * @return non-null but possibly empty list of IUserSummary objects; note
     * that these objects will have null update and access fields.
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    List<IUserSummary> getReviews(
            int changelistId,
            List<IFileSpec> fileSpecs)
            throws ConnectionException, RequestException, AccessException;
}
