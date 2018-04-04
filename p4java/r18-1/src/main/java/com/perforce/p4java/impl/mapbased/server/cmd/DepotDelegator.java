package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.DEPOT;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IDepotDelegator;
import org.apache.commons.lang3.Validate;

/**
 * DepotDelegator implementation.
 */
public class DepotDelegator extends BaseDelegator implements IDepotDelegator {

    /**
     * Instantiates a new depot delegator.
     *
     * @param server the server
     */
    public DepotDelegator(final IOptionsServer server) {
        super(server);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.server.delegator.IDepotDelegator#createDepot(com.
     * perforce.p4java.core.IDepot)
     */
    @Override
    public String createDepot(@Nonnull final IDepot newDepot) throws P4JavaException {
        Validate.notNull(newDepot);
        List<Map<String, Object>> resultMaps = execMapCmdList(DEPOT, new String[]{"-i"},
                InputMapper.map(newDepot));
        return ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.server.delegator.IDepotDelegator#deleteDepot(java.
     * lang.String)
     */
    @Override
    public String deleteDepot(final String name) throws P4JavaException {
        Validate.notBlank(name, "Delete depot name shouldn't null or empty");

        List<Map<String, Object>> resultMaps = execMapCmdList(DEPOT, new String[]{"-d", name},
                null);
        return ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.server.delegator.IDepotDelegator#getDepot(java.lang.
     * String)
     */
    @Override
    public IDepot getDepot(final String name) throws P4JavaException {
        Validate.notBlank(name, "Depot name shouldn't null or empty");

        List<Map<String, Object>> resultMaps = execMapCmdList(DEPOT, new String[]{"-o", name},
                null);
        return ResultListBuilder.buildNullableObjectFromNonInfoMessageCommandResultMaps(
                resultMaps,
                new Function<Map, IDepot>() {
                    @Override
                    public IDepot apply(Map map) {
                        return new Depot(map);
                    }
                }
        );
    }
}
