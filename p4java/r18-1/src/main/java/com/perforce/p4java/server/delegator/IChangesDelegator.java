package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IChangelist.Type;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetChangelistsOptions;

/**
 * Interface for a ChangesDelegator implementation.
 */
public interface IChangesDelegator {
    
    /**
     * Gets the changelists.
     *
     * @param maxMostRecent the max most recent
     * @param fileSpecs the file specs
     * @param clientName the client name
     * @param userName the user name
     * @param includeIntegrated the include integrated
     * @param type the type
     * @param longDesc the long desc
     * @return the changelists
     * @throws ConnectionException the connection exception
     * @throws RequestException the request exception
     * @throws AccessException the access exception
     */
    List<IChangelistSummary> getChangelists(int maxMostRecent, List<IFileSpec> fileSpecs,
            String clientName, String userName, boolean includeIntegrated, Type type,
            boolean longDesc) throws ConnectionException, RequestException, AccessException;

    /**
     * Gets the changelists.
     *
     * @param maxMostRecent the max most recent
     * @param fileSpecs the file specs
     * @param clientName the client name
     * @param userName the user name
     * @param includeIntegrated the include integrated
     * @param submittedOnly the submitted only
     * @param pendingOnly the pending only
     * @param longDesc the long desc
     * @return the changelists
     * @throws ConnectionException the connection exception
     * @throws RequestException the request exception
     * @throws AccessException the access exception
     */
    List<IChangelistSummary> getChangelists(int maxMostRecent,
            List<IFileSpec> fileSpecs, String clientName, String userName,
            boolean includeIntegrated, boolean submittedOnly, boolean pendingOnly,
            boolean longDesc) throws ConnectionException, RequestException, AccessException;
    /**
     * Get a list of Perforce changelist summary objects from the Perforce server.
     *
     * @param fileSpecs if non-empty, limits the results to changelists that affect the specified
     *                  files.  If the file specification includes a revision range, limits its
     *                  results to submitted changelists that affect those particular revisions
     * @param opts      GetChangelistsOptions object describing optional parameters; if null, no
     *                  options are set.
     * @return a non-null (but possibly empty) list of qualifying changelists.
     * @throws P4JavaException if any error occurs in the processing of this method
     */
    List<IChangelistSummary> getChangelists(List<IFileSpec> fileSpecs,
            GetChangelistsOptions opts) throws P4JavaException;
}
