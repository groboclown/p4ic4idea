package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.P4JavaExceptions.rethrowFunction;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.DUPLICATE;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.FunctionWithException;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.DuplicateRevisionsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IDuplicateDelegator;

/**
 * Implementation to handle the Duplicate command.
 */
public class DuplicateDelegator extends BaseDelegator implements IDuplicateDelegator {
    /**
     * Instantiate a new DuplicateDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public DuplicateDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFileSpec> duplicateRevisions(
            final IFileSpec fromFile,
            final IFileSpec toFile,
            final DuplicateRevisionsOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(
                DUPLICATE,
                processParameters(
                        opts,
                        fromFile,
                        toFile,
                        null,
                        server),
                null);

        return buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                rethrowFunction(
                        new FunctionWithException<Map, IFileSpec>() {
                            @Override
                            public IFileSpec apply(Map map) throws P4JavaException {
                                return ResultListBuilder.handleIntegrationFileReturn(
                                        map,
                                        server);
                            }
                        }
                ));
    }
}
