package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.IRevListCommit;
import com.perforce.p4java.impl.generic.graph.RevListCommit;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.GraphRevListOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IGraphRevListDelegator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.COMMIT;
import static com.perforce.p4java.server.CmdSpec.GRAPH;

/**
 * This class acts as a delegator that executes the command 'p4 graph rev-list'
 */
public class GraphRevListDelegator extends BaseDelegator implements IGraphRevListDelegator {


    /**
     * Instantiates a new graph rev list delegator.
     *
     * @param server the server
     */
    public GraphRevListDelegator(final IOptionsServer server) {
        super(server);
    }

    /**
     * Returns a List encapsulating a RevListCommit which holds the
     * data retrieved as part of the 'p4 graph rev-list -n'
     *
     * @param options
     * @return
     * @throws P4JavaException
     */
    @Override
    public List<IRevListCommit> getGraphRevList(GraphRevListOptions options) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(
                GRAPH, Parameters.processParameters(options, server), null);

        if (!nonNull(resultMaps)) {
            return null;
        }

        String commit = null;
        List<IRevListCommit> graphRevList = new ArrayList<>();

        for (Map<String, Object> map : resultMaps) {

            ResultMapParser.handleErrorStr(map);

            try {
                if (map.containsKey(COMMIT)) {
                    commit = parseString(map, COMMIT);
                }
            } catch (Throwable thr) {
                Log.exception(thr);
            }
            graphRevList.add(new RevListCommit(commit));
        }

        return graphRevList;
    }
}
