package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.INTEGRATED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetSubmittedIntegrationsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IIntegratedDelegator;

/**
 * Implementation for integrated delegator.
 */
public class IntegratedDelegator extends BaseDelegator implements IIntegratedDelegator {

    /**
     * Instantiates a new integrated delegator.
     *
     * @param server
     *            the server
     */
    public IntegratedDelegator(final IOptionsServer server) {
        super(server);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.perforce.p4java.server.delegator.IIntegratedDelegator#
     * getSubmittedIntegrations(java.util.List, java.lang.String, boolean)
     */
    @Override
    public List<IFileSpec> getSubmittedIntegrations(final List<IFileSpec> fileSpecs,
            final String branchSpec, final boolean reverseMappings)
            throws ConnectionException, RequestException, AccessException {

        try {
            GetSubmittedIntegrationsOptions submittedIntegrationsOptions =
                    new GetSubmittedIntegrationsOptions(
                    branchSpec, reverseMappings);
            return getSubmittedIntegrations(fileSpecs, submittedIntegrationsOptions);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.perforce.p4java.server.delegator.IIntegratedDelegator#
     * getSubmittedIntegrations(java.util.List,
     * com.perforce.p4java.option.server.GetSubmittedIntegrationsOptions)
     */
    @Override
    public List<IFileSpec> getSubmittedIntegrations(final List<IFileSpec> fileSpecs,
            final GetSubmittedIntegrationsOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(INTEGRATED,
                processParameters(opts, fileSpecs, server), null);

        List<IFileSpec> integratedList = new ArrayList<>();
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                integratedList
                        .add(ResultListBuilder.handleIntegrationFileReturn(map, false, server));
            }
        }

        return integratedList;
    }
}
