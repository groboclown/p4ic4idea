/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.DeleteBranchSpecOptions;
import com.perforce.p4java.option.server.GetBranchSpecOptions;

import javax.annotation.Nonnull;

/**
 *
 */
public interface IBranchDelegator {
    /**
     * Get a specific named Perforce branch spec from the Perforce server.
     * <p>
     *
     * Note that since the Perforce server usually interprets asking for a
     * non-existent branch spec as equivalent to asking for a template for a new
     * branch spec, you will normally always get back a result here. It is best
     * to first use the getBranchSpecList method to see if the branch spec
     * exists, then use this method to retrieve a specific branch spec once you
     * know it exists.
     *
     * @param name
     *            non-null Perforce branch name.
     * @param opts
     *            GetBranchSpecOptions object describing optional parameters; if
     *            null, no options are set.
     * @return potentially-null IBranchSpec for the named Perforce branch spec.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     * @since 2011.2
     */
    IBranchSpec getBranchSpec(final String name, final GetBranchSpecOptions opts)
            throws P4JavaException;

    /**
     * Delete a named Perforce branch spec from the Perforce server.
     *
     * @param branchSpecName
     *            non-null name of the branch spec to be deleted.
     * @param opts
     *            DeleteBranchSpecOptions object describing optional parameters;
     *            if null, no options are set.
     * @return non-null result message string from the Perforce server; this may
     *         include form trigger output pre-pended and / or appended to the
     *         "normal" message
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     */
    String deleteBranchSpec(final String branchSpecName,
            final DeleteBranchSpecOptions opts)
            throws P4JavaException;

    /**
     * Get the branch spec for the given name.
     * 
     * @param name
     *            the name of the branch
     * @return a populated branch spec object
     * @throws ConnectionException
     *             when there is an error talking to the Helix server
     * @throws RequestException
     *             when there is a problem with the data provided in the request
     * @throws AccessException
     *             when access to the branch command is not authorised
     */
    IBranchSpec getBranchSpec(final String name)
            throws ConnectionException, RequestException, AccessException;

    /**
     * TODO: This should be moved up to Server and changed to delegate to
     * createBranchSpec with an options class. This would also allow for a force
     * option.
     * 
     * @param branchSpec
     *            the spec object containing the branch data fields.
     * @return The name of the newly create branch spec
     * @throws ConnectionException
     *             when there is an error talking to the Helix server
     * @throws RequestException
     *             when there is a problem with the data provided in the request
     * @throws AccessException
     *             when access to the branch command is not authorised
     */
    String createBranchSpec(@Nonnull final IBranchSpec branchSpec)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Update the data fields in an existing branch spec.
     * 
     * @param branchSpec
     *            the branch data
     * @return the name of the updated spec
     * @throws ConnectionException
     *             when there is an error talking to the Helix server
     * @throws RequestException
     *             when there is a problem with the data provided in the request
     * @throws AccessException
     *             when access to the branch command is not authorised
     */
    String updateBranchSpec(@Nonnull final IBranchSpec branchSpec)
            throws ConnectionException, RequestException, AccessException;

    /**
     * Delete a branch spec specifing whether it should be a forced operation.
     * 
     * @param branchSpecName
     *            the name of the spec to delete
     * @param force
     *            whether to force the operation through
     * @return name of the deleted spec
     * @throws ConnectionException
     *             when there is an error talking to the Helix server
     * @throws RequestException
     *             when there is a problem with the data provided in the request
     * @throws AccessException
     *             when access to the branch command is not authorised
     * @deprecated use {@link IBranchDelegator#deleteBranchSpec(String, DeleteBranchSpecOptions)} instead
     */
    String deleteBranchSpec(final String branchSpecName, final boolean force)
            throws ConnectionException, RequestException, AccessException;
}
