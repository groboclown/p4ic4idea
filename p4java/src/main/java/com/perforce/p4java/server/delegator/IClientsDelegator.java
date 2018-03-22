package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetClientsOptions;

/**
 * @author Sean Shou
 * @since 15/09/2016
 */
public interface IClientsDelegator {
    /**
     * Get a list of IClientSummary objects for all Perforce clients known to this Perforce
     * server.<p>
     * <p>
     * Note that this method returns light-weight IClientSummary objects rather than full
     * IClient objects; if you need the heavy-weight IClient objects, you should use getClient().
     * <p>
     * Note also that the returned IClient objects are not "complete", in the sense
     * that implementations are free to leave certain attributes null for performance
     * reasons. In general, at least the client's name, root, description, last modification
     * time are guaranteed correct.
     *
     * @param opts GetClientsOptions object describing optional parameters; if null, no options are
     *             set.
     * @return non-null (but possibly empty) list of Client objects for Perforce clients known to
     * this Perforce server.
     * @throws P4JavaException if any error occurs in the processing of this method.
     */
    List<IClientSummary> getClients(GetClientsOptions opts) throws P4JavaException;


    /**
     * Get a list of IClientSummary objects for all Perforce clients known to this Perforce
     * server.<p>
     * <p>
     * Note that this method returns light-weight IClientSummary objects rather than full
     * IClient objects; if you need the heavy-weight IClient objects, you should use getClient().
     * <p>
     * Note also that the returned IClient objects are not "complete", in the sense
     * that implementations are free to leave certain attributes null for performance
     * reasons. In general, at least the client's name, root, description, last modification
     * time are guaranteed correct.
     *
     * @param userName   user name
     * @param nameFilter limits output to clients whose name matches the nameFilter pattern.
     *                   Corresponds to -enameFilter flag
     * @param maxResults If greater than zero, limit output to the first maxResults number of
     *                   clients.
     * @return non-null (but possibly empty) list of Client objects for Perforce clients known to
     */
    List<IClientSummary> getClients(
            String userName,
            String nameFilter,
            int maxResults)
            throws ConnectionException, RequestException, AccessException;
}
