package com.perforce.p4java.server.delegator;

import java.util.List;
import javax.annotation.Nonnull;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.ObliterateFilesOptions;

/**
 * Interface to handle the Obliterate command.
 */
public interface IObliterateDelegator {
    /**
     * Obliterate permanently remove files and their history from the server.
     * <p>
     * <p>
     * Obliterate retrieves the disk space used by the obliterated files in the
     * archive and clears the files from the metadata that is maintained by the
     * server. Files in client workspaces are not physically affected, but they
     * are no longer under Perforce control.
     * <p>
     * <p>
     * By default, obliterate displays a preview of the results. To execute the
     * operation, you must specify the -y flag (opts.executeObliterate).
     * Obliterate requires 'admin' access, which is granted by 'p4 protect'.
     * <p>
     * <p>
     * The "obliterate" command returns an IObliterateResult for each file
     * passed into the command. Each IObliterateResult object contains a summary
     * of various types of records deleted (or added) and a non-null list of
     * returned filespecs have the equivalent of purgeFile and purgeRev output
     * in the depotPath and endRevision fileds of the associated filespecs, and
     * that no other file spec fields are valid. Sometimes, the server doesn't
     * return any "purgeFile" and "purgeRev" values.
     * <p>
     * <p>
     * Note: error and info messages are stored in filespec objects.
     * <p>
     *
     * @param fileSpecs non-null list of files to be obliterated
     * @param opts      possibly-null ObliterateFilesOptions object specifying method
     *                  options.
     * @return a non-null list of IObliterateResult objects containing the
     * records purged.
     * @throws P4JavaException if an error occurs processing this method and its parameters
     * @since 2011.2
     */
    List<IObliterateResult> obliterateFiles(@Nonnull List<IFileSpec> fileSpecs,
                                            ObliterateFilesOptions opts) throws P4JavaException;
}
