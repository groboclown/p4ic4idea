package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.ISTAT;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IStreamIntegrationStatus;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.StreamIntegrationStatus;
import com.perforce.p4java.option.server.StreamIntegrationStatusOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IStatDelegator;

/**
 * Implemetation for 'p4 stat'.
 */
public class StatDelegator extends BaseDelegator implements IStatDelegator {

    /**
     * Instantiates a new stat delegator.
     *
     * @param server the server
     */
    public StatDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public IStreamIntegrationStatus getStreamIntegrationStatus(final String stream,
                                                               final StreamIntegrationStatusOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(ISTAT,
                processParameters(opts, null, new String[]{stream}, server), null);

        return ResultListBuilder.buildNullableObjectFromNonInfoMessageCommandResultMaps(
                resultMaps,
                new Function<Map, IStreamIntegrationStatus>() {
                    @Override
                    public IStreamIntegrationStatus apply(Map map) {
                        return new StreamIntegrationStatus(map);
                    }
                }
        );
    }
}
