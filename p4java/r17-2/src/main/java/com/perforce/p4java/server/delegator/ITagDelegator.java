package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.TagFilesOptions;

/**
 * Interface to handle the Tag command.
 */
public interface ITagDelegator {
    /**
     * Tag files with a Perforce label.
     *
     * @param fileSpecs non-null list of files to be tagged.
     * @param labelName non-null label name to use for the tagging.
     * @param listOnly  if true, don't do the actual tag, just return the list of
     *                  files that would have been tagged.
     * @param delete    if true, delete the label tag from the files.
     * @return a non-null (but possibly empty) list of affected file specs
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    List<IFileSpec> tagFiles(
            List<IFileSpec> fileSpecs,
            String labelName,
            boolean listOnly,
            boolean delete)
            throws ConnectionException, RequestException, AccessException;

    /**
     * @param fileSpecs non-null list of files to be tagged.
     * @param labelName non-null label name to use for the tagging.
     * @param opts      TagFilesOptions object describing optional parameters; if null, no options are
     *                  set.
     * @return a non-null (but possibly empty) list of affected file specs.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    List<IFileSpec> tagFiles(
            List<IFileSpec> fileSpecs,
            String labelName,
            TagFilesOptions opts) throws P4JavaException;
}
