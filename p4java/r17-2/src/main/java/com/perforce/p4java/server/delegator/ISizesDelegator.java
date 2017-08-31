package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.file.IFileSize;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetFileSizesOptions;

/**
 * Interface to handle the Sizes command.
 */
public interface ISizesDelegator {
    /**
     * Gets a list of file sizes for one or more files in the depot.<p>
     * <p>
     * For specified file specification, get the depot file name, revision, file
     * count and file size. If you use client syntax for the file specification,
     * the view mapping is used to list the corresponding depot files.
     *
     * @param fileSpecs filespecs to be processed; if null or empty, an empty list is returned.
     * @param opts      GetFileSizesOptions object describing optional parameters; if null, no options
     *                  are set.
     * @return a non-null (but possibly empty) list of file sizes.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2013.2
     */
    List<IFileSize> getFileSizes(
            List<IFileSpec> fileSpecs,
            GetFileSizesOptions opts) throws P4JavaException;
}
