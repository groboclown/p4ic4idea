package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.P4JavaExceptions.rethrowFunction;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.OPENED;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.common.function.FunctionWithException;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IOpenedDelegator;

/**
 * Implementation for p4 opened.
 */
public class OpenedDelegator extends BaseDelegator implements IOpenedDelegator {

    /**
     * Instantiates a new opened delegator.
     *
     * @param server the server
     */
    public OpenedDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFileSpec> getOpenedFiles(
            final List<IFileSpec> fileSpecs,
            final boolean allClients,
            final String clientName,
            final int maxFiles,
            final int changeListId) throws ConnectionException, AccessException {

        try {
            OpenedFilesOptions openedFilesOptions = new OpenedFilesOptions(
                    allClients,
                    clientName,
                    maxFiles,
                    null,
                    changeListId);

            return getOpenedFiles(fileSpecs, openedFilesOptions);
        } catch (final ConnectionException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            // TODO Why does this exception behave differently to return as 
            // empty list?
            Log.warn("Unexpected exception in IServer.openedFiles: %s", exc);
            return Collections.emptyList();
        }
    }

    @Override
    public List<IFileSpec> getOpenedFiles(
            final List<IFileSpec> fileSpecs,
            final OpenedFilesOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(OPENED,
                processParameters(opts, fileSpecs, server), null);

        return ResultListBuilder.buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                rethrowFunction(new FunctionWithException<Map, IFileSpec>() {
                    @Override
                    public IFileSpec apply(Map map) throws P4JavaException {
                        return ResultListBuilder.handleFileReturn(map, server);
                    }
                })
        );
    }
}
