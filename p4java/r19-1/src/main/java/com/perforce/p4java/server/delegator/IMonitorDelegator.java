package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.IServerProcess;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetServerProcessesOptions;

import java.util.List;

/**
 * Interface to handle the Monitor command.
 */
public interface IMonitorDelegator {
    /**
     * Return a list of Perforce server processes active on the Perforce server.
     * Will throw a request exception if monitors are not enabled on the target
     * server.
     *
     * @return non-null but possibly-empty list of IServerProcess objects
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */
    List<IServerProcess> getServerProcesses()
            throws ConnectionException, RequestException, AccessException;

    /**
     * Return a list of Perforce server processes active on the Perforce server.
     * Will throw a request exception if monitors are not enabled on the target
     * server.
     *
     * @param opts <code>GetServerProcessesOptions</code> object describing optional parameters; if
     *             null, no options are set.
     * @return list of server processes
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     */
    List<IServerProcess> getServerProcesses(GetServerProcessesOptions opts)
            throws P4JavaException;
}
