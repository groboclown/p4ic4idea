package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.FIXES;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Fix;
import com.perforce.p4java.option.server.GetFixesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IFixesDelegator;

/**
 * Implementation of a delegator to support 'p4 fixes'.
 */
public class FixesDelegator extends BaseDelegator implements IFixesDelegator {
    
    /**
     * Instantiates a new fixes delegator.
     *
     * @param server the server
     */
    public FixesDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFix> getFixList(final List<IFileSpec> fileSpecs, final int changeListId,
            final String jobId, final boolean includeIntegrations, final int maxFixes)
            throws ConnectionException, RequestException, AccessException {

        try {
            // Note the hack below to get backwards compatibility with the
            // need to let IChangelist.DEFAULT be similar to saying *no*
            // changelist, which was arguably wrong and is fixed in the
            // new version where DEFAULT is OK. See job 040703.
            int resolvedChangelistId = changeListId;
            if (changeListId == IChangelist.DEFAULT) {
                resolvedChangelistId = IChangelist.UNKNOWN;
            }

            GetFixesOptions getFixesOptions = new GetFixesOptions()
                    .setChangelistId(resolvedChangelistId)
                    .setIncludeIntegrations(includeIntegrations).setJobId(jobId)
                    .setMaxFixes(maxFixes);
            return getFixes(fileSpecs, getFixesOptions);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public List<IFix> getFixes(final List<IFileSpec> fileSpecs, final GetFixesOptions opts)
            throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(FIXES,
                processParameters(opts, fileSpecs, server), null);

        return ResultListBuilder.buildNonNullObjectListFromCommandResultMaps(resultMaps,
                new Function<Map, IFix>() {
                    @Override
                    public IFix apply(Map map) {
                        return new Fix(map);
                    }
                }
        );
    }
}
