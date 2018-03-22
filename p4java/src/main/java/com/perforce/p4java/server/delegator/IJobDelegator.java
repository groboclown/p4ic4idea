package com.perforce.p4java.server.delegator;

import java.util.Map;

import javax.annotation.Nonnull;

import com.perforce.p4java.core.IJob;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;

/**
 * Interface for 'p4 job'.
 */
public interface IJobDelegator {
    /**
     * Create a new Perforce job in the Perforce server corresponding to the
     * passed-in Perforce job fields (which in turn should correspond to at
     * least the mandatory fields defined in the reigning Perforce job spec).
     * <p>
     *
     * Perforce job semantics, field count and layout, etc., are to some extent
     * free-form and specified for each server by the associated job spec
     * (retrievable using the getJobSpec() method below), so map fields are
     * passed to the Perforce server exactly as passed to the create method in
     * the job's field map, so you need to know the field names and semantics
     * given by the associated job spec. This includes setting the relevant job
     * ID field to "new", but otherwise, no checking is done on fields in this
     * method against the job spec (this may be added later).
     * <p>
     *
     * @param fieldMap
     *            non-null field map defining the new job in the Perforce
     *            server.
     * @return returns an IJob representing the newly-created job, if
     *         successful.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    IJob createJob(@Nonnull Map<String, Object> fieldMap)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Delete a job from the Perforce server. Note that this method does not
     * change the status of the associated job locally, just on the Perforce
     * server.
     *
     * @param jobId
     *            ID of the job to be deleted.
     * @return possibly-null status message as returned from the server; this
     *         may include form trigger output pre-pended and / or appended to
     *         the "normal" message.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    String deleteJob(String jobId) throws ConnectionException, RequestException, AccessException;

    /**
     * Get a specific job. Note that some implementations of the underlying
     * server do not return null if you ask for a job that doesn't exist; you
     * must do your own checking to see of what's returned represents a real job
     * or not.
     *
     * @param jobId
     *            non-null job Id.
     * @return IJob for the named job; null if no such job.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    IJob getJob(String jobId) throws ConnectionException, RequestException, AccessException;

    /**
     * Update a Perforce job on the Perforce server. Note that <i>only</i> the
     * associated raw fields map is used for field values; the main description
     * and ID fields are actually ignored.
     * <p>
     *
     * The returned string will contain whatever the Perforce server returned in
     * response to this command; in general, if the update fails, an exception
     * will be thrown, meaning that the returned string represents success only.
     * There are two success states -- either the job was saved or it didn't
     * need saving (it was the same after updating). Consumers should parse this
     * accordingly.
     *
     * @param job
     *            non-null candidate for updating.
     * @return possibly-null status message as returned from the server; this
     *         may include form trigger output pre-pended and / or appended to
     *         the "normal" message.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    String updateJob(@Nonnull IJob job)
            throws ConnectionException, RequestException, AccessException;
}
