package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapAsString;
import static com.perforce.p4java.server.CmdSpec.RELOAD;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.ReloadOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IReloadDelegator;

/**
 * Interface to handle the Reload command.
 */
public class ReloadDelegator extends BaseDelegator implements IReloadDelegator {
    /**
     * Instantiate a new ReloadDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public ReloadDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String reload(final ReloadOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(
                RELOAD,
                processParameters(opts, server),
                null);

        return parseCommandResultMapAsString(resultMaps);
    }
}
