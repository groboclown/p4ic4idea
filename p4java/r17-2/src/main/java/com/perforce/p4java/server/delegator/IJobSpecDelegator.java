package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;

/**
 * Interface for jobspec commands.
 */
public interface IJobSpecDelegator {
    
    /**
     * Return the Perforce jobspec associated with this Perforce server.
     * <p>
     *
     * @return possibly-null IJobSpec representing the unserlying Perforc
     *         server's jobspec.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     */
    IJobSpec getJobSpec() throws ConnectionException, RequestException, AccessException;
}
