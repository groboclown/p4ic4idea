package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetFixesOptions;

/**
 * Interface for the 'p4 fixes' command.
 */
public interface IFixesDelegator {
    /**
     * Return a list of all Perforce jobs with fix records associated with them,
     * along with the changelist number of the fix. Detailed semantics for this
     * method are given in the main Perforce documentation for the p4 command
     * "fixes".
     * <p>
     *
     * Note that this method (unlike the main file list methods) throws an
     * exception and stops at the first encountered error.
     *
     * @param fileSpecs
     *            if given, restrict output to fixes associated with these files
     * @param changeListId
     *            if positive, only fixes from the numbered changelist are
     *            listed.
     * @param jobId
     *            if non-null, only fixes for the named job are listed
     * @param includeIntegrations
     *            if true, include any fixes made by changelists integrated into
     *            the specified files
     * @param maxFixes
     *            if positive, restrict the list to the first maxFixes fixes
     * @return non-null but possibly empty list of qualifying IFix fixes.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    List<IFix> getFixList(List<IFileSpec> fileSpecs, int changeListId, String jobId,
            boolean includeIntegrations, int maxFixes)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Return a list of all Perforce jobs with fix records associated with them,
     * along with the changelist number of the fix. Detailed semantics for this
     * method are given in the main Perforce documentation for the p4 command
     * "fixes".
     * <p>
     *
     * Note that this method (unlike the main file list methods) throws an
     * exception and stops at the first encountered error.
     *
     * @param fileSpecs
     *            if given, restrict output to fixes associated with these files
     * @param opts
     *            FixListOptions object describing optional parameters; if null,
     *            no options are set
     * @return non-null but possibly empty list of qualifying IFix fixes.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     */
    List<IFix> getFixes(List<IFileSpec> fileSpecs, GetFixesOptions opts) throws P4JavaException;
}
