package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.IGraphListTree;
import com.perforce.p4java.server.delegator.IGraphListTreeDelegator;
import com.perforce.p4java.impl.generic.graph.GraphListTree;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.server.IOptionsServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseInt;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.MODE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.NAME;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.SHA;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TYPE;
import static com.perforce.p4java.server.CmdSpec.GRAPH;

public class GraphListTreeDelegator extends BaseDelegator implements IGraphListTreeDelegator {

	/**
	 * Basic constructor, taking a server object.
	 *
	 * @param server - an instance of the currently effective server implementaion
	 */
	public GraphListTreeDelegator(IOptionsServer server) {
		super(server);
	}

	@Override
	public List<IGraphListTree> getGraphListTree(String sha) throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(
				GRAPH,
				Parameters.processParameters(
						null, null, new String[]{"ls-tree", sha}, server),
				null);

		if (!nonNull(resultMaps)) {
			return null;
		}

		int mode = -1;
		String type = null;
		String rsha = null;
		String name = null;

		List<IGraphListTree> graphListTreeList = new ArrayList<>();

		Map<String, Object> map = resultMaps.get(0);
		int index = 0;
		while (nonNull(map.get(SHA + index))) {
			try {
				if (map.containsKey(MODE + index)) {
					mode = parseInt(map, MODE + index);
				}
				if (map.containsKey(TYPE + index)) {
					type = parseString(map, TYPE + index);
				}
				if (map.containsKey(SHA + index)) {
					rsha = parseString(map, SHA + index);
				}
				if (map.containsKey(NAME + index)) {
					name = parseString(map, NAME + index);
				}
			} catch (Throwable thr) {
				Log.exception(thr);
			}

			graphListTreeList.add(new GraphListTree(mode, type, rsha, name));
			index++;
		}

		return graphListTreeList;
	}
}