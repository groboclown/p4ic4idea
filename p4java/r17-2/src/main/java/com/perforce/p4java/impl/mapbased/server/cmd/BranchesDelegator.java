package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.BRANCHES;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.BranchSpecSummary;
import com.perforce.p4java.option.server.GetBranchSpecsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IBranchesDelegator;

/**
 * @author Sean Shou
 * @since 21/09/2016
 */
public class BranchesDelegator extends BaseDelegator implements IBranchesDelegator {

    /**
     * Build a new VerifyDelegtor object and keep the server object for
     * <p>
     * using in the command processing.
     *
     * @param server - the currently effective server implementation
     */
    public BranchesDelegator(IOptionsServer server) {

        super(server);

    }

    @Override
    public List<IBranchSpecSummary> getBranchSpecs(final GetBranchSpecsOptions opts)
            throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(BRANCHES,
                processParameters(opts, server), null);

        return ResultListBuilder.buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                new Function<Map, IBranchSpecSummary>() {
                    @Override
                    public IBranchSpecSummary apply(Map map) {
                        return new BranchSpecSummary(map, true);
                    }
                });
    }

    @Override
    public List<IBranchSpecSummary> getBranchSpecs(
            final String userName,
            final String nameFilter,
            final int maxReturns) throws ConnectionException, RequestException, AccessException {

        checkMinSupportedPerforceVersion(userName, maxReturns, nameFilter, "branch");
        try {
            GetBranchSpecsOptions getBranchSpecsOptions = new GetBranchSpecsOptions()
                    .setMaxResults(maxReturns)
                    .setNameFilter(nameFilter)
                    .setUserName(userName);
            return getBranchSpecs(getBranchSpecsOptions);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }
}
