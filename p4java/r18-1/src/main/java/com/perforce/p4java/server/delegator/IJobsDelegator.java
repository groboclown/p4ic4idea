package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetJobsOptions;

/**
 * Interface for 'p4 jobs'.
 */
public interface IJobsDelegator {

    /**
     * Return a list of Perforce jobs. Note that (as discussed in the IJob
     * comments) Perforce jobs can have a wide variety of fields, formats,
     * semantics, etc., and this method can return a list that may have to be
     * unpacked at the map level by the consumer to make any sense of it.
     * <p>
     *
     * Note that this method (unlike the main file list methods) throws an
     * exception and stops at the first encountered error.
     *
     * @param fileSpecs
     *            if given, return only jobspecs affecting the given file(s)
     * @param maxJobs
     *            if positive, return only up to maxJobs results
     * @param longDescriptions
     *            if true, return full descriptions, otherwise show only a
     *            subset (typically the first 128 characters, but this is not
     *            guaranteed).
     * @param reverseOrder
     *            if true, reverse the normal sort order
     * @param includeIntegrated
     *            if true, include any fixes made by changelists integrated into
     *            the specified files
     * @param jobView
     *            if non-null, a string in format detailed by "p4 help jobview"
     *            used to restrict jobs to those satisfying the job view
     *            expression.
     * @return a non-null (but possibly-empty) list of qualifying Perforce jobs
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    List<IJob> getJobs(List<IFileSpec> fileSpecs, int maxJobs, boolean longDescriptions,
            boolean reverseOrder, boolean includeIntegrated, String jobView)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Return a list of Perforce jobs. Note that (as discussed in the IJob
     * comments) Perforce jobs can have a wide variety of fields, formats,
     * semantics, etc., and this method can return a list that may have to be
     * unpacked at the map level by the consumer to make any sense of it.
     * <p>
     *
     * Note that this method (unlike the main file list methods) throws an
     * exception and stops at the first encountered error.
     *
     * @param fileSpecs
     *            if given, return only jobspecs affecting the given file(s).
     * @param opts
     *            GetJobsOptions object describing optional parameters; if null,
     *            no options are set.
     * @return a non-null (but possibly-empty) list of qualifying Perforce jobs.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     */
    List<IJob> getJobs(List<IFileSpec> fileSpecs, GetJobsOptions opts) throws P4JavaException;
}
