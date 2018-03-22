package com.perforce.p4java.server.delegator;

import java.io.InputStream;
import java.util.List;

import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetFileDiffsOptions;

/**
 * @author Sean Shou
 * @since 27/09/2016
 */
public interface IDiff2Delegator {
    /**
     * Run diff on the Perforce server of two files in the depot.<p>
     * <p>
     * With a branch view, fromFile and toFile are optional; fromFile limits
     * the scope of the source file set, and toFile limits the scope of the
     * target. If only one file argument is given, it is assumed to be toFile.<p>
     * <p>
     * This method corresponds closely to the standard diff2 command, and that
     * command's documentation should be consulted for the overall and detailed
     * semantics.<p>
     *
     * @param file1          (optional, with a branch view) source file IFileSpec
     * @param file2          (optional, with a branch view) target file IFileSpec
     * @param branchSpecName optional branch spec name
     * @param opts           GetFileDiffsOptions object describing optional parameters; if null, no
     *                       options are set.
     * @return non-null but possibly empty array of file diffs
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     */
    List<IFileDiff> getFileDiffs(
            IFileSpec file1,
            IFileSpec file2,
            String branchSpecName,
            GetFileDiffsOptions opts) throws P4JavaException;

    /**
     * Run diff on the Perforce server of two files in the depot.
     * <p>
     * <p>
     * This method corresponds closely to the standard diff2 command, and that
     * command's documentation should be consulted for the overall and detailed
     * semantics. In particular, the various potentially-valid combinations of
     * branch spec and file specs can be complicated and won't be repeated here.
     * <p>
     *
     * @param file1               optional first file IFileSpec
     * @param file2               optional second file IFileSpec
     * @param branchSpecName      optional branch spec name
     * @param quiet               if true, suppresses the display of the header lines of files
     *                            whose content and types are identical and suppresses the
     *                            actual diff for all files.
     * @param includeNonTextDiffs if true, forces 'p4 diff2' to diff even files with non-text
     *                            (binary) types
     * @param gnuDiffs            see "-u" option in the main diff2 documentation.
     * @return non-null but possibly empty array of file diffs
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    List<IFileDiff> getFileDiffs(
            IFileSpec file1,
            IFileSpec file2,
            String branchSpecName,
            DiffType diffType,
            boolean quiet,
            boolean includeNonTextDiffs,
            boolean gnuDiffs) throws ConnectionException, RequestException, AccessException;

    /**
     * Run diff on the Perforce server of two files in the depot.<p>
     * <p>
     * With a branch view, fromFile and toFile are optional; fromFile limits
     * the scope of the source file set, and toFile limits the scope of the
     * target. If only one file argument is given, it is assumed to be toFile.<p>
     * <p>
     * This method corresponds closely to the standard diff2 command, and that
     * command's documentation should be consulted for the overall and detailed
     * semantics.<p>
     * <p>
     * As with other streams-based IServer methods, callers should ensure that
     * the stream returned here is always explicitly closed after use; if not
     * closed, the stream's associated temporary files managed by P4Java
     * (if they exist) may not be properly deleted.
     *
     * @param file1          (optional, with a branch view) source file IFileSpec
     * @param file2          (optional, with a branch view) target file IFileSpec
     * @param branchSpecName optional branch spec name
     * @param opts           GetFileDiffsOptions object describing optional parameters; if null, no
     *                       options are set.
     * @return non-null but possibly empty InputStream of diffs and headers as returned from the
     * server.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     */
    InputStream getFileDiffsStream(
            IFileSpec file1,
            IFileSpec file2,
            String branchSpecName,
            GetFileDiffsOptions opts) throws P4JavaException;

    /**
     * Run diff on the Perforce server of two files in the depot.
     * <p>
     * <p>
     * This method corresponds closely to the standard diff2 command, and that
     * command's documentation should be consulted for the overall and detailed
     * semantics. In particular, the various potentially-valid combinations of
     * branch spec and file specs can be complicated and won't be repeated here.
     * <p>
     * <p>
     * As with other streams-based IServer methods, callers should ensure that
     * the stream returned here is always explicitly closed after use; if not
     * closed, the stream's associated temporary files managed by P4Java (if
     * they exist) may not be properly deleted.
     *
     * @param file1               optional first file IFileSpec
     * @param file2               optional second file IFileSpec
     * @param branchSpecName      optional branch spec name
     * @param quiet               if true, suppresses the display of the header lines of files
     *                            whose content and types are identical and suppresses the
     *                            actual diff for all files.
     * @param includeNonTextDiffs if true, forces 'p4 diff2' to diff even files with non-text
     *                            (binary) types
     * @param gnuDiffs            see "-u" option in the main diff2 documentation.
     * @return non-null but possibly empty InputStream of diffs and headers as
     * returned from the server.
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    InputStream getServerFileDiffs(
            IFileSpec file1,
            IFileSpec file2,
            String branchSpecName,
            DiffType diffType,
            boolean quiet,
            boolean includeNonTextDiffs,
            boolean gnuDiffs) throws ConnectionException, RequestException, AccessException;
}
