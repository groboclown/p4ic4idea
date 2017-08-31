package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.OpenedFilesOptions;

/**
 * Interface for p4 opened.
 */
public interface IOpenedDelegator {

    /**
     * If one or more Perforce file specs is passed-in, return the opened /
     * locked status of each file (if known) within an IFileSpec object;
     * otherwise return a list of all files known to be open for this Perforce
     * client workspace.
     * <p>
     *
     * The returned list can be modified with the other arguments as described
     * below.
     *
     * @param fileSpecs
     *            if non-empty, determine the status of the specified files;
     *            otherwise return all qualifying files known to be open
     * @param allClients
     *            if true, return results for all known clients rather than the
     *            current client (if any).
     * @param clientName
     *            if non-null, return results for the named client only.
     * @param maxFiles
     *            if positive, return only the first maxFiles qualifying files.
     * @param changeListId
     *            if positive, return only files associated with the given
     *            changelist ID; if IChangelist.DEFAULT, retrieve files open
     *            associated with the default changelist.
     * @return non-null but possibly-empty list of qualifying open files. Not
     *         all fields in individual file specs will be valid or make sense
     *         to be accessed.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    List<IFileSpec> getOpenedFiles(List<IFileSpec> fileSpecs, boolean allClients, String clientName,
            int maxFiles, int changeListId) throws ConnectionException, AccessException;

    /**
     * If one or more Perforce file specs is passed-in, return the opened /
     * locked status of each file (if known) within an IFileSpec object;
     * otherwise return a list of all files known to be open for this Perforce
     * client workspace.
     * <p>
     *
     * @param fileSpecs
     *            if non-empty, determine the status of the specified files;
     *            otherwise return all qualifying files known to be open
     * @param opts
     *            possibly-null OpenedFilesOptions object object specifying
     *            method options.
     * @return non-null but possibly-empty list of qualifying open files. Not
     *         all fields in individual file specs will be valid or make sense
     *         to be accessed.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     */
    List<IFileSpec> getOpenedFiles(List<IFileSpec> fileSpecs, OpenedFilesOptions opts)
            throws P4JavaException;
}
