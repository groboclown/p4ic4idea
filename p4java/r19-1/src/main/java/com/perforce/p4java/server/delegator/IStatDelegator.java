package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.IStreamIntegrationStatus;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.StreamIntegrationStatusOptions;

/**
 * Interface for 'p4 stat' imlementation.
 */
public interface IStatDelegator {
    /**
     * Get a stream's cached integration status with respect to its parent. If
     * the cache is stale, either because newer changes have been submitted or
     * the stream's branch view has changed, 'p4 istat' checks for pending
     * integrations and updates the cache before showing status.
     * <p>
     *
     * Pending integrations are shown only if they are expected by the stream;
     * that is, only if they are warranted by the stream's type and its
     * fromParent/toParent flow options. (See 'p4 help stream'.)
     * <p>
     *
     * @param stream
     *            the stream's path in a stream depot, of the form
     *            //depotname/streamname.
     * @param opts
     *            StreamIntegrationStatusOptions object describing optional
     *            parameters; if null, no options are set.
     * @return potentially-null IStreamIntegrationStatus object representing the
     *         stream's cached integration status with respect to its parent.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2011.2
     */
    IStreamIntegrationStatus getStreamIntegrationStatus(String stream,
            StreamIntegrationStatusOptions opts) throws P4JavaException;
}
