package com.perforce.p4java.server.delegator;

import java.util.List;
import javax.annotation.Nonnull;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetDirectoriesOptions;

/**
 * Interface for an implementation to handle dirs command.
 */
public interface IDirsDelegator {
    
    /**
     * List any directories matching the passed-in file specifications
     * and other options.
     *
     * @param fileSpecs the file specs
     * @param clientOnly the client only
     * @param deletedOnly the deleted only
     * @param haveListOnly the have list only
     * @return the directories
     * @throws ConnectionException the connection exception
     * @throws AccessException the access exception
     */
    List<IFileSpec> getDirectories(@Nonnull List<IFileSpec> fileSpecs,
            boolean clientOnly, boolean deletedOnly, boolean haveListOnly)
            throws ConnectionException, AccessException;

    /**
     * List any directories matching the passed-in file specifications.
     *
     * @param fileSpecs
     *            non-null list of file specifications.
     * @param opts
     *            GetDirectoriesOptions object describing optional parameters;
     *            if null, no options are set.
     * @return non-null but possibly empty list of qualifying directory file
     *         specs; only the getPath() path will be valid.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     */
    List<IFileSpec> getDirectories(List<IFileSpec> fileSpecs, GetDirectoriesOptions opts)
            throws P4JavaException;
}
