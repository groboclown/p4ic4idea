package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Interface to handle the Annotate command.
 */
public interface IFileAnnotateDelegator {
    /**
     * @param fileSpecs        non-null list of file specs to be annotated.
     * @param diffType         If non-null, use the <code>DiffType</code> value to determine whitespace
     *                         options.
     * @param allResults       If true, include both deleted files and lines no longer present
     *                         at the head revision; corresponds to the -a flag.
     * @param useChangeNumbers If true, annotate with change numbers rather than revision numbers
     *                         with each line; correspond to the -c flag.
     * @param followBranches   If true, follow branches; corresponds to the -f flag.
     * @return list of file annotations
     * @throws ConnectionException connection errors
     * @throws RequestException server request errors
     * @throws AccessException access restrictions
     */
    List<IFileAnnotation> getFileAnnotations(
            List<IFileSpec> fileSpecs,
            @Nonnull DiffType diffType,
            boolean allResults,
            boolean useChangeNumbers,
            boolean followBranches) throws ConnectionException, RequestException, AccessException;

    /**
     * Get a list of revision annotations for the specified files.
     *
     * @param fileSpecs non-null list of file specs to be annotated.
     * @param opts      GetFileAnnotationsOptions object describing optional parameters; if null, no
     *                  options are set.
     * @return non-null (but possibly-empty) list of IFileAnnotation objects representing version
     * annotations for the passed-in file specs.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    List<IFileAnnotation> getFileAnnotations(
            List<IFileSpec> fileSpecs,
            GetFileAnnotationsOptions opts) throws P4JavaException;
}
