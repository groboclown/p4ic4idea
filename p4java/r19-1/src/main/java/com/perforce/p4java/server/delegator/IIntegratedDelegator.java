package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetSubmittedIntegrationsOptions;

/**
 * Interface for intregrations.
 */
public interface IIntegratedDelegator {
    /**
     * Get a list of submitted integrations for the passed-in filespecs.
     *
     * @param fileSpecs
     *            if null or ommitted, all qualifying depot files are used.
     * @param branchSpec
     *            if non-null, only files integrated from the source to target
     *            files in the branch view are shown. Qualified files are
     *            displayed even if they were integrated without using the
     *            branch view itself.
     * @param reverseMappings
     *            if true,reverses the mappings in the branch view, with the
     *            target files and source files exchanging place. This requires
     *            the branchSpec to be non-null.
     * @return a non-null but possibly empty list of IFileSpec representing
     *         qualifying integrations.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    List<IFileSpec> getSubmittedIntegrations(List<IFileSpec> fileSpecs, String branchSpec,
            boolean reverseMappings) throws ConnectionException, RequestException, AccessException;

    /**
     * Get a list of submitted integrations for the passed-in filespecs.
     *
     * @param fileSpecs
     *            if null or omitted, all qualifying depot files are used.
     * @param opts
     *            GetSubmittedIntegrations object describing optional
     *            parameters; if null, no options are set.
     * @return a non-null but possibly empty list of IFileSpec representing
     *         qualifying integrations.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     */
    List<IFileSpec> getSubmittedIntegrations(List<IFileSpec> fileSpecs,
            GetSubmittedIntegrationsOptions opts) throws P4JavaException;
}
