package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;

// TODO: Auto-generated Javadoc
/**
 * The Interface IDepotsDelegator.
 */
public interface IDepotsDelegator {

    /**
     * Gets the depots.
     *
     * @return the depots
     * @throws ConnectionException
     *             the connection exception
     * @throws RequestException
     *             the request exception
     * @throws AccessException
     *             the access exception
     */
    List<IDepot> getDepots() throws ConnectionException, RequestException, AccessException;
}
