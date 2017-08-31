package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwP4JavaErrorIfConditionFails;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.throwRequestExceptionIfErrorMessageFound;
import static com.perforce.p4java.server.CmdSpec.CLIENTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.client.ClientSummary;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IClientsDelegator;

/**
 * @author Sean Shou
 * @since 15/09/2016
 */
public class ClientsDelegator extends BaseDelegator implements IClientsDelegator {
    /**
     * Instantiate a new ClientsDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public ClientsDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IClientSummary> getClients(final GetClientsOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(
                CLIENTS,
                Parameters.processParameters(opts, server),
                null);
        throwP4JavaErrorIfConditionFails(
                nonNull(resultMaps),
                "Null resultMaps in getClientList call");

        List<IClientSummary> specList = new ArrayList<>();
        for (Map<String, Object> map : resultMaps) {
            throwRequestExceptionIfErrorMessageFound(map);
            specList.add(new ClientSummary(map, true));
        }

        return specList;
    }

    @Override
    public List<IClientSummary> getClients(
            final String userName,
            final String nameFilter,
            final int maxResults) throws ConnectionException, RequestException, AccessException {

        checkMinSupportedPerforceVersion(userName, maxResults, nameFilter, "client");

        try {
            GetClientsOptions getClientsOptions = new GetClientsOptions()
                    .setMaxResults(maxResults)
                    .setUserName(userName)
                    .setNameFilter(nameFilter);
            return getClients(getClientsOptions);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }
}
