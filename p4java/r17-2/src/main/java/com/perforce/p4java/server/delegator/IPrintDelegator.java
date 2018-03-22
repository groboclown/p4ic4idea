package com.perforce.p4java.server.delegator;

import java.io.InputStream;
import java.util.List;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetFileContentsOptions;

/**
 * Interface to handle the Print command.
 */
public interface IPrintDelegator {
    InputStream getFileContents(
            List<IFileSpec> fileSpecs,
            boolean allRevs,
            boolean noHeaderLine) throws ConnectionException, RequestException, AccessException;

    /**
     * Return an InputStream onto the contents of one or more revisions of one or more Perforce depot
     * file contents.<p>
     * <p>
     * If file is specified as a Perforce client workspace file name, the client view is used to find
     * the corresponding depot file. If a file argument has a revision, then all files as of that
     * revision are streamed.  If a file argument has a revision range, then only files selected by
     * that revision range are streamed, and the highest revision in the range is used for each file.
     * Normally, only the head revision is printed.<p>
     * <p>
     * The underlying input stream is not guaranteed to support mark() and skip() operations, and in
     * some cases can be absolutely ginormously long it is also not guaranteed to be printable, and
     * will be in the charset encoding stored in the Perforce server.<p>
     * <p>
     * You should close the InputStreamReader after use in order to release any underlying
     * stream-related resources. Failure to do this may lead to the proliferation of temp files or
     * long-term memory wastage or even leaks.<p>
     * <p>
     * Note that unlike the corresponding command-line command, which keeps going in the face of
     * errors by moving on to the next file (or whatever), any errors encountered in this method will
     * cause an exception from this method at the first error, so plan accordingly....
     *
     * @param fileSpecs non-null list of depot or client file specs defining files to be streamed
     * @param opts      GetFileContentsOptions object describing optional parameters; if null, no
     *                  options are set.
     * @return a non-null but possibly-empty InputStream onto the file / revision contents.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    InputStream getFileContents(
            List<IFileSpec> fileSpecs,
            GetFileContentsOptions opts) throws P4JavaException;
}
