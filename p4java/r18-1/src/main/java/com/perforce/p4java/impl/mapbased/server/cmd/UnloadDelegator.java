package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapAsString;
import static com.perforce.p4java.server.CmdSpec.UNLOAD;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.UnloadOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IUnloadDelegator;

/**
 * Implementation to handle the Unload command.
 */
public class UnloadDelegator extends BaseDelegator implements IUnloadDelegator {
    /**
     * Instantiate a new UnloadDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public UnloadDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String unload(final UnloadOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(
                UNLOAD,
                processParameters(opts, server),
                null);

        return parseCommandResultMapAsString(resultMaps);
    }
}
