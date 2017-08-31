/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.server.delegator;

import java.io.InputStream;
import java.util.List;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.DescribeOptions;
import com.perforce.p4java.option.server.GetChangelistDiffsOptions;

/**
 *
 */
public interface IDescribeDelegator {

    /**
     * Get an InputStream onto the file diffs associated with a specific
     * submitted changelist. This method (like the similar "p4 describe"
     * command) will not return diffs for pending changelists.
     * <p>
     *
     * This is one of the guaranteed "live" method on this interface, and will
     * return the diff output as it exists when called (rather than when the
     * underlying implementation object was created). This can be an expensive
     * method to evaluate, and can generate reams and reams (and reams) of
     * output, so don't use it willy-nilly.
     * <p>
     *
     * Note that unlike the corresponding command-line command, which keeps
     * going in the face of errors by moving on to the next file (or whatever),
     * any errors encountered in this method will cause an exception from this
     * method at the first error, so plan accordingly....
     *
     * @param id
     *            the ID of the target changelist.
     * @param opts
     *            GetChangelistDiffsOptions object describing optional
     *            parameters; if null, no options are set.
     * @return InputStream onto the diff stream. Note that while this stream
     *         will not be null, it may be empty.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     */

    InputStream getChangelistDiffs(int id, GetChangelistDiffsOptions opts) throws P4JavaException;

    /**
     * Get an InputStream onto the file diffs associated with a specific
     * submitted changelist. This method (like the similar "p4 describe"
     * command) will not return diffs for pending changelists.
     * <p>
     *
     * This is one of the guaranteed "live" method on this interface, and will
     * return the diff output as it exists when called (rather than when the
     * underlying implementation object was created). This can be an expensive
     * method to evaluate, and can generate reams and reams (and reams) of
     * output, so don't use it willy-nilly.
     * <p>
     *
     * Note that unlike the corresponding command-line command, which keeps
     * going in the face of errors by moving on to the next file (or whatever),
     * any errors encountered in this method will cause an exception from this
     * method at the first error, so plan accordingly....
     *
     * @param id
     *            the ID of the target changelist
     * @param options
     *            DescribeOptions behavioural options for method.
     * @return InputStream onto the diff stream. Note that while this stream
     *         will not be null, it may be empty
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */

    InputStream getChangelistDiffsStream(int id, DescribeOptions options)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Get a list of shelved files associated with a Perforce pending
     * changelist.
     * <p>
     *
     * @param changelistId
     *            numeric pending changelist identifier
     * @return non-null (but possibly empty) list of shelved files associated
     *         with the pending changelist.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2014.1
     */
    List<IFileSpec> getShelvedFiles(final int changelistId) throws P4JavaException;
}
