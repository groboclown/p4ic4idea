package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.MONITOR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IServerProcess;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.ServerProcess;
import com.perforce.p4java.option.server.GetServerProcessesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IMonitorDelegator;

/**
 * Implementation to handle the Monitor command.
 */
public class MonitorDelegator extends BaseDelegator implements IMonitorDelegator {
    /**
     * Instantiate a new MonitorDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public MonitorDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IServerProcess> getServerProcesses()
            throws ConnectionException, RequestException, AccessException {
        try {
            return getServerProcesses(null);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public List<IServerProcess> getServerProcesses(final GetServerProcessesOptions opts)
            throws P4JavaException {
        List<String> args = new ArrayList<>(Arrays.asList(new String[]{"show"}));

        String[] options = processParameters(opts, server);
        if (nonNull(options) && options.length > 0) {
            args.addAll(Arrays.asList(options));
        }

        List<Map<String, Object>> resultMaps = execMapCmdList(
                MONITOR,
                args.toArray(new String[args.size()]),
                null);

        return buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                new Function<Map, IServerProcess>() {
                    @Override
                    public IServerProcess apply(Map map) {
                        return new ServerProcess(map);
                    }
                }
        );
    }
}
