package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.server.CmdSpec.DEPOTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IDepotsDelegator;

/**
 * Implementation to handle depots commands.
 */
public class DepotsDelegator extends BaseDelegator implements IDepotsDelegator {
    
    /**
     * Instantiates a new depots delegator.
     *
     * @param server the server
     */
    public DepotsDelegator(final IOptionsServer server) {
        super(server);
    }

    /* (non-Javadoc)
     * @see com.perforce.p4java.server.delegator.IDepotsDelegator#getDepots()
     */
    @Override
    public List<IDepot> getDepots() throws ConnectionException, RequestException, AccessException {
        List<IDepot> metadataArray = new ArrayList<>();

        List<Map<String, Object>> resultMaps = execMapCmdList(DEPOTS, new String[0], null);

        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                if (!ResultMapParser.handleErrorStr(map)) {
                    metadataArray.add(new Depot(map));
                }
            }
        }

        return metadataArray;
    }
}
