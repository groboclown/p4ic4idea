package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.DuplicateRevisionsOptions;

/**
 * Interface to handle the Duplicate command.
 */
public interface IDuplicateDelegator {
    /**
     * Duplicate revisions with integration history (unsupported).<p>
     * <p>
     * Duplicate revisions as if they always existed. All aspects of the source
     * revisions are mirrored to the target revisions, including changelist
     * number, date, attributes, and contents. The target revision must not
     * already exist and the target file must not be opened (for any operation)
     * on any client.<p>
     * <p>
     * Note that integration records are duplicated as well. 'p4 duplicate'
     * followed by a 'p4 obliterate' (of the source revisions) is in effect a
     * deep rename operation, with any source revision in client workspace or
     * labels forgotten. The full semantics of this operation are found in the
     * main 'p4 help duplicate' documentation.
     *
     * @param fromFile non-null source file.
     * @param toFile   non-null target file.
     * @param opts     possibly-null CopyFilesOptions object specifying method options.
     * @return non-null but possibly empty list of duplicated file info/error messages.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2012.2
     */
    List<IFileSpec> duplicateRevisions(
            IFileSpec fromFile,
            IFileSpec toFile,
            DuplicateRevisionsOptions opts) throws P4JavaException;
}
