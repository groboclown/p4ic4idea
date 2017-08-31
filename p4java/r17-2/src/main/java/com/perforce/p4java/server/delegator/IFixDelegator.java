package com.perforce.p4java.server.delegator;

import java.util.List;
import javax.annotation.Nonnull;

import com.perforce.p4java.core.IFix;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.FixJobsOptions;

/**
 * Interface for implementations of 'p4 fix'.
 */
public interface IFixDelegator {

    /**
     * Mark each named job as being fixed by the changelist number given with
     * changeListId.
     *
     * @param jobIds
     *            non-null non-empty list of affected job IDs.
     * @param changeListId
     *            changelist ID for affected changelist.
     * @param opts
     *            FixJobsOptions object describing optional parameters; if null,
     *            no options are set.
     * @return list of affected fixes.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     */
    List<IFix> fixJobs(@Nonnull List<String> jobIds, int changeListId, FixJobsOptions opts)
            throws P4JavaException;

    /**
     * Mark each named job as being fixed by the changelist number given with
     * changeListId.
     *
     * @param jobIds
     *            non-null non-empty list of affected job IDs.
     * @param changeListId
     *            changelist ID for affected changelist.
     * @param status
     *            the status
     * @param delete
     *            whether to delete.
     * @return list of affected fixes.
     * @throws ConnectionException
     *             the connection exception
     * @throws RequestException
     *             the request exception
     * @throws AccessException
     *             the access exception
     */
    List<IFix> fixJobs(List<String> jobIds, int changeListId, String status, boolean delete)
            throws ConnectionException, RequestException, AccessException;
}
