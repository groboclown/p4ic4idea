package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.BRANCH;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.BranchSpec;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.option.server.DeleteBranchSpecOptions;
import com.perforce.p4java.option.server.GetBranchSpecOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IBranchDelegator;
import org.apache.commons.lang3.Validate;

/**
 * @author Sean Shou
 * @since 21/09/2016
 */
public class BranchDelegator extends BaseDelegator implements IBranchDelegator {

    /**
     * Instantiate a new BranchDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public BranchDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String createBranchSpec(@Nonnull final IBranchSpec branchSpec)
            throws ConnectionException, RequestException, AccessException {

        Validate.notNull(branchSpec);

        try {
            List<Map<String, Object>> resultMaps = execMapCmdList(BRANCH, new String[]{"-i"},
                    InputMapper.map(branchSpec));
            return ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public IBranchSpec getBranchSpec(final String name)
            throws ConnectionException, RequestException, AccessException {
        try {
            return getBranchSpec(name, new GetBranchSpecOptions());
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public IBranchSpec getBranchSpec(final String name, final GetBranchSpecOptions opts)
            throws P4JavaException {
        Validate.notBlank(name, "Branch spec name shouldn't blank");

        List<Map<String, Object>> resultMaps = execMapCmdList(BRANCH,
                processParameters(opts, null, new String[]{"-o", name}, server), null);

        return ResultListBuilder.buildNullableObjectFromNonInfoMessageCommandResultMaps(
                resultMaps,
                new Function<Map, IBranchSpec>() {
                    @Override
                    public IBranchSpec apply(Map map) {
                        return new BranchSpec(map, server);
                    }
                }
        );
    }

    @Override
    public String updateBranchSpec(@Nonnull final IBranchSpec branchSpec)
            throws ConnectionException, RequestException, AccessException {
        Validate.notNull(branchSpec);

        try {
            List<Map<String, Object>> resultMaps = execMapCmdList(
                    BRANCH,
                    new String[]{"-i"},
                    InputMapper.map(branchSpec));

            return ResultMapParser.parseCommandResultMapAsString(resultMaps);
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public String deleteBranchSpec(final String branchSpecName, final boolean force)
            throws ConnectionException, RequestException, AccessException {

        try {
            return deleteBranchSpec(branchSpecName, new DeleteBranchSpecOptions(force));
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public String deleteBranchSpec(final String branchSpecName, final DeleteBranchSpecOptions opts)
            throws P4JavaException {
        Validate.notBlank(branchSpecName, "Branch spec name shouldn't blank");

        List<Map<String, Object>> resultMaps = execMapCmdList(
                BRANCH,
                processParameters(
                        opts,
                        null,
                        new String[]{"-d", branchSpecName},
                        server),
                null);

        return ResultMapParser.parseCommandResultMapAsString(resultMaps);
    }
}
