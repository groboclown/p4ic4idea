package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerInfo;

/**
 * Interface for 'p4 info'.
 */
public interface IInfoDelegator {
    
    /**
     * Gets the server info.
     * @return the server info
     * @throws ConnectionException the connection exception
     * @throws RequestException the request exception
     * @throws AccessException the access exception
     */
    IServerInfo getServerInfo() throws ConnectionException, RequestException, AccessException;
}
