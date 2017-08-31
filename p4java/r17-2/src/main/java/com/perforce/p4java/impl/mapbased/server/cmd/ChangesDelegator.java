package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.core.IChangelist.Type.PENDING;
import static com.perforce.p4java.core.IChangelist.Type.SUBMITTED;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.CHANGES;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IChangelist.Type;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.ChangelistSummary;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IChangesDelegator;

/**
 * ChangesDelegator implementation.
 */
public class ChangesDelegator extends BaseDelegator implements IChangesDelegator {

    /**
     * Instantiates a new changes delegator.
     *
     * @param server the server
     */
    public ChangesDelegator(final IOptionsServer server) {
        super(server);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.server.delegator.IChangesDelegator#getChangelists(
     * int, java.util.List, java.lang.String, java.lang.String, boolean,
     * com.perforce.p4java.core.IChangelist.Type, boolean)
     */
    @Override
    public List<IChangelistSummary> getChangelists(
            final int maxMostRecent,
            final List<IFileSpec> fileSpecs,
            final String clientName,
            final String userName,
            final boolean includeIntegrated,
            final Type type,
            final boolean longDesc)
            throws ConnectionException, RequestException, AccessException {

        try {
            GetChangelistsOptions getChangelistsOptions = new GetChangelistsOptions()
                    .setClientName(clientName)
                    .setIncludeIntegrated(includeIntegrated)
                    .setLongDesc(longDesc)
                    .setMaxMostRecent(maxMostRecent)
                    .setType(type)
                    .setUserName(userName);

            return getChangelists(fileSpecs, getChangelistsOptions);
        } catch (final ConnectionException | RequestException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.server.delegator.IChangesDelegator#getChangelists(
     * int, java.util.List, java.lang.String, java.lang.String, boolean,
     * boolean, boolean, boolean)
     */
    @Override
    public List<IChangelistSummary> getChangelists(
            final int maxMostRecent,
            final List<IFileSpec> fileSpecs,
            final String clientName,
            final String userName,
            final boolean includeIntegrated,
            final boolean submittedOnly,
            final boolean pendingOnly,
            final boolean longDesc) throws ConnectionException, RequestException, AccessException {

        Type type = null;
        if (submittedOnly) {
            type = SUBMITTED;
        } else if (pendingOnly) {
            type = PENDING;
        }
        return getChangelists(
                maxMostRecent,
                fileSpecs,
                clientName,
                userName,
                includeIntegrated,
                type,
                longDesc);
    }

    /**
     * Get a list of Perforce changelist summary objects from the Perforce
     * server.
     *
     * @param fileSpecs if non-empty, limits the results to changelists that affect
     *                  the specified files. If the file specification includes a
     *                  revision range, limits its results to submitted changelists
     *                  that affect those particular revisions
     * @param opts      GetChangelistsOptions object describing optional parameters;
     *                  if null, no options are set.
     * @return a non-null (but possibly empty) list of qualifying changelists.
     * @throws P4JavaException if any error occurs in the processing of this method
     */
    public List<IChangelistSummary> getChangelists(
            final List<IFileSpec> fileSpecs,
            final GetChangelistsOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(
                CHANGES,
                processParameters(opts, fileSpecs, server),
                null);

        return ResultListBuilder.buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                new Function<Map, IChangelistSummary>() {
                    @Override
                    public IChangelistSummary apply(Map map) {
                        return new ChangelistSummary(map, true);
                    }
                });
    }
}
