/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetBranchSpecsOptions;

/**
 *
 */
public interface IBranchesDelegator {
    /**
     * Get a list of all summary Perforce branch specs known to the Perforce
     * server.
     * <p>
     *
     * Note that the IBranchSpecSummary objects returned here do not have branch
     * view specs; you must call the getBranchSpec method on a specific branch
     * to get valid view specs for a branch.
     *
     * @param opts
     *            object describing optional parameters; if null, no options are
     *            set.
     * @return non-null (but possibly-empty) list of IBranchSpecSummary objects.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     */
    List<IBranchSpecSummary> getBranchSpecs(final GetBranchSpecsOptions opts)
            throws P4JavaException;
    
    /**
     * Old style getBranchSpecs api call.
     * xdeprecated use getBranchSpecs(final GetBranchSpecsOptions opts) instead
     * @param userName
     * @param nameFilter
     * @param maxReturns
     * @return
     * @throws ConnectionException
     * @throws RequestException
     * @throws AccessException
     */
    List<IBranchSpecSummary> getBranchSpecs(final String userName, final String nameFilter,
            final int maxReturns) throws ConnectionException, RequestException, AccessException; 
}
