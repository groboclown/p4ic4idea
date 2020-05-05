package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwRequestExceptionIfConditionFails;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.server.CmdSpec.INFO;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.ServerInfo;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;

// p4ic4idea: use IServerMessage
import com.perforce.p4java.server.IServerMessage;

import com.perforce.p4java.server.delegator.IInfoDelegator;

/**
 * Implementation for 'p4 info' commands.
 */
public class InfoDelegator extends BaseDelegator implements IInfoDelegator {

    /**
     * Instantiates a new info delegator.
     *
     * @param server
     *            the server
     */
    public InfoDelegator(final IOptionsServer server) {
        super(server);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.perforce.p4java.server.delegator.IInfoDelegator#getServerInfo()
     */
    @Override
    public IServerInfo getServerInfo()
            throws ConnectionException, RequestException, AccessException {
        ServerInfo serverInfo = new ServerInfo();
        List<Map<String, Object>> resultMaps = execMapCmdList(INFO, new String[0], null);

        if (nonNull(resultMaps)) {
            List<Map<String, Object>> validMaps = new ArrayList<>();
            for (int i = 0; i < resultMaps.size(); i++) {
                Map<String, Object> map = resultMaps.get(i);
                // p4ic4idea: use IServerMessage
                ResultMapParser.handleErrors(ResultMapParser.toServerMessage(map));
                validMaps.add(map);
            }
            serverInfo = new ServerInfo(validMaps);
        }
        return serverInfo;
    }
}
