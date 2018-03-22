package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.SearchJobsOptions;

/**
 * Interface to handle the Search command.
 */
public interface ISearchDelegator {
    /**
     * Search for jobs that contain the specified words in the search engine's
     * index.
     * <p>
     * <p>
     * Note that this is an 'undoc' Perforce command.
     * <p>
     * <p>
     * See also 'p4 help index'.
     *
     * @param words non-null words to be searched.
     * @param opts  SearchJobsOptions object describing optional parameters; if
     *              null, no options are set.
     * @return non-null (but possibly-empty) list of job IDs.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    List<String> searchJobs(String words, SearchJobsOptions opts) throws P4JavaException;
}
