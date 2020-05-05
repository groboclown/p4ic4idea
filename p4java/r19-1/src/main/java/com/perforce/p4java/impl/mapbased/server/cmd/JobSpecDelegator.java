package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.JOBSPEC;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.JobSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IJobSpecDelegator;

/**
 * Implementation for jobspec commands.
 */
public class JobSpecDelegator extends BaseDelegator implements IJobSpecDelegator {
    
    /**
     * Instantiates a new job spec delegator.
     *
     * @param server the server
     */
    public JobSpecDelegator(final IOptionsServer server) {
        super(server);
    }

    /* (non-Javadoc)
     * @see com.perforce.p4java.server.delegator.IJobSpecDelegator#getJobSpec()
     */
    @SuppressWarnings("unchecked")
    @Override
    public IJobSpec getJobSpec() throws ConnectionException, RequestException, AccessException {
        List<Map<String, Object>> resultMaps = execMapCmdList(JOBSPEC, new String[] { "-o" }, null);
        return ResultListBuilder.buildNullableObjectFromNonInfoMessageCommandResultMaps(
                resultMaps,
                new Function<Map, IJobSpec>() {
                    @Override
                    public IJobSpec apply(Map map) {
                        return new JobSpec(map, server);
                    }
                }
        );
    }
}
