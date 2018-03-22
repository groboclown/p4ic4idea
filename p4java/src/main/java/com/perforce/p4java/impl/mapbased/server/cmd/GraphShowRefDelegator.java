package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.IGraphRef;
import com.perforce.p4java.impl.generic.graph.GraphRef;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.GraphShowRefOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IGraphShowRefDelegator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.NAME;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REPO_NAME;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.SHA;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TYPE;
import static com.perforce.p4java.server.CmdSpec.GRAPH;

public class GraphShowRefDelegator extends BaseDelegator implements IGraphShowRefDelegator {

	/**
	 * Basic constructor, taking a server object.
	 *
	 * @param server - an instance of the currently effective server implementaion
	 */
	public GraphShowRefDelegator(IOptionsServer server) {
		super(server);
	}

	@Override
	public List<IGraphRef> getGraphShowRefs(GraphShowRefOptions opts) throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(
				GRAPH, Parameters.processParameters(opts, server), null);

		if (!nonNull(resultMaps)) {
			return null;
		}

		String repo = null;
		String type = null;
		String sha = null;
		String name = null;
		List<IGraphRef> graphRefList = new ArrayList<>();

		for (Map<String, Object> map : resultMaps) {

			ResultMapParser.handleErrorStr(map);

			try {
				if (map.containsKey(REPO_NAME)) {
					repo = parseString(map, REPO_NAME);
				}
				if (map.containsKey(TYPE)) {
					type = parseString(map, TYPE);
				}
				if (map.containsKey(SHA)) {
					sha = parseString(map, SHA);
				}
				if (map.containsKey(NAME)) {
					name = parseString(map, NAME);
				}
			} catch (Throwable thr) {
				Log.exception(thr);
			}
			graphRefList.add(new GraphRef(repo, type, sha, name));
		}

		return graphRefList;
	}
}
