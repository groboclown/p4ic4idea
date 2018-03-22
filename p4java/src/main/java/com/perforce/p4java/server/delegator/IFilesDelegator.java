package com.perforce.p4java.server.delegator;

import java.util.List;
import javax.annotation.Nonnull;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetDepotFilesOptions;

/**
 * Interface for an implementation for 'p4 files'.
 */
public interface IFilesDelegator {
    
    /**
     * List all Perforce depot files known to the Perforce server that conform
     * to the passed-in wild-card file specification(s).
     * <p>
     *
     * If client file names are given as file spec arguments the current
     * Perforce client view mapping is used to list the corresponding depot
     * files, if the client and view exist (if not, the results are undefined).
     * <p>
     *
     * Normally, the head revision of each matching file is listed, but you can
     * change this by specifying specific revisions or revision ranges. If the
     * file spec argument includes a revision, then all files as of that
     * revision are returned. If the file spec argument has a revision range,
     * then only files selected by that revision range are returned, and the
     * highest revision in the range is used for each file. If
     * GetDepotFilesOptions.allRevs is true, all revisions within the specific
     * range, rather than just the highest revision in the range, are returned.
     * <p>
     *
     * See 'p4 help revisions' for help specifying revisions.
     * <p>
     *
     * Note that the IFileSpec objects returned will have null client and local
     * path components.
     *
     * @param fileSpecs the file specs
     * @param allRevs the all revs
     * @return the depot files
     * @throws ConnectionException the connection exception
     * @throws AccessException the access exception
     */
    List<IFileSpec> getDepotFiles(@Nonnull List<IFileSpec> fileSpecs,
            boolean allRevs) throws ConnectionException, AccessException;
    
    /**
     * List all Perforce depot files known to the Perforce server that conform
     * to the passed-in wild-card file specification(s).
     * <p>
     *
     * If client file names are given as file spec arguments the current
     * Perforce client view mapping is used to list the corresponding depot
     * files, if the client and view exist (if not, the results are undefined).
     * <p>
     *
     * Normally, the head revision of each matching file is listed, but you can
     * change this by specifying specific revisions or revision ranges. If the
     * file spec argument includes a revision, then all files as of that
     * revision are returned. If the file spec argument has a revision range,
     * then only files selected by that revision range are returned, and the
     * highest revision in the range is used for each file. If
     * GetDepotFilesOptions.allRevs is true, all revisions within the specific
     * range, rather than just the highest revision in the range, are returned.
     * <p>
     *
     * See 'p4 help revisions' for help specifying revisions.
     * <p>
     *
     * Note that the IFileSpec objects returned will have null client and local
     * path components.
     *
     * @param fileSpecs
     *            a non-null list of one or more IFileSpecs to be used to
     *            qualify Perforce depot files
     * @param opts
     *            GetDepotFilesOptions describing the associated options; if
     *            null, no options are set.
     * @return a non-null (but possible empty) list of all qualifying depot
     *         files
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     */
    List<IFileSpec> getDepotFiles(@Nonnull List<IFileSpec> fileSpecs,
            GetDepotFilesOptions opts) throws P4JavaException;
}
