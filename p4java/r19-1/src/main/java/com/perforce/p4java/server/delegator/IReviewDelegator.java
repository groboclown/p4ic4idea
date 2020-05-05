package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IReviewChangelist;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetReviewChangelistsOptions;

/**
 * Interface to handle the Review command.
 */
public interface IReviewDelegator {
    /**
     * Get a list of all submitted changelists equal or above a provided
     * changelist number that have not been reviewed before.
     * <p>
     * <p>
     * If only the 'changelistId' option is provided, return a list of
     * changelists that have not been reviewed before, equal or above the
     * specified changelist#.
     * <p>
     * <p>
     * If only the 'counter' option is provided, return a list of changelists
     * that have not been reviewed before, above the specified counter's
     * changelist#.
     * <p>
     * <p>
     * If both the 'changelistId' and 'counter' options are specified, 'p4
     * review' sets the counter to that changelist# and produces no output. This
     * functionality has been superceded by the 'p4 counter' command. The user
     * must have permission to set counters.
     *
     * @param opts GetReviewChangelistsOptions object describing optional
     *             parameters; if null, no options are set.
     * @return non-null but possibly empty list of IReviewChangelist objects;
     * note that these objects will have null update and access fields.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2012.2
     */
    List<IReviewChangelist> getReviewChangelists(GetReviewChangelistsOptions opts)
            throws P4JavaException;
}
