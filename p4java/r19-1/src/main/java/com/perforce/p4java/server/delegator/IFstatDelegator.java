package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;

/**
 * Interface for 'p4 fstat'.
 */
public interface IFstatDelegator {
    /**
     * Return a list of everything Perforce knows about a possibly very large
     * set of Perforce files.
     * <p>
     *
     * This method is not intended for general use, and is not documented in
     * detail here; consult the main Perforce fstat command documentation for
     * detailed help. In particular, the various options are too complex to be
     * described in a few sentences here, and the various option arguments
     * reflect this complexity. Note that setting both sinceChangelist and
     * affectedByChangelist to zero or a positive value will cause usage errors
     * from the server (these are currently intended to be mutually-exclusive
     * options).
     * <p>
     *
     * This method can be a real server and bandwidth resource hog, and should
     * be used as sparingly as possible; alternatively, try to use it with as
     * narrow a set of file specs as possible.
     *
     * @param fileSpecs
     *            non-null list of Perforce file specification(s)
     * @param maxFiles
     *            if positive, restrict the output to the first maxReturns
     *            files. Implementations are free to ignore this parameter if
     *            necessary (and return all qualifying results).
     * @param sinceChangelist
     *            if larger than or equal to zero, display only files affected
     *            since the given changelist number; zero is equivalent to
     *            IChangelist.DEFAULT.
     * @param affectedByChangelist
     *            if larger than or equal to zero, display only files affected
     *            by the given changelist number; zero is equivalent to
     *            IChangelist.DEFAULT.
     * @param outputOptions
     *            if non-null, specifies the oputput options to be used
     * @param ancilliaryOptions
     *            if non-null, specifies the ancilliary output options to be
     *            used
     * @return a non-null (but possibly empty) list of qualifying files and
     *         associated stat info
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    List<IExtendedFileSpec> getExtendedFiles(List<IFileSpec> fileSpecs, int maxFiles,
            int sinceChangelist, int affectedByChangelist, FileStatOutputOptions outputOptions,
            FileStatAncilliaryOptions ancilliaryOptions)
            throws ConnectionException, AccessException;

    /**
     * Return a list of everything Perforce knows about a set of Perforce files.
     * <p>
     *
     * This method is not intended for general use, and is not documented in
     * detail here; consult the main Perforce fstat command documentation for
     * detailed help.
     *
     * This method can be a real server and bandwidth resource hog, and should
     * be used as sparingly as possible; alternatively, try to use it with as
     * narrow a set of file specs as possible.
     *
     * @param fileSpecs
     *            non-null list of Perforce file specification(s).
     * @param opts
     *            GetExtendedFilesOptions object describing optional parameters;
     *            if null, no options are set.
     * @return non-null (but possibly empty) list of qualifying files and
     *         associated stat info.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     */
    List<IExtendedFileSpec> getExtendedFiles(List<IFileSpec> fileSpecs,
            GetExtendedFilesOptions opts) throws P4JavaException;
}
