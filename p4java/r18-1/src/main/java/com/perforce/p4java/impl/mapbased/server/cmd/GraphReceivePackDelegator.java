package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.GraphReceivePackOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IGraphReceivePackDelegator;

import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.server.CmdSpec.GRAPH;

/**
 * Delegator class that delegates receive-pack command execution to the server.
 * <p>
 * Usages:
 * p4 graph receive-pack    -n //graph/scm-plugin -F master=5631932f5cdf6c3b829911b6fe5ab42d436d74da (uses the force option)
 * p4 graph receive-pack    -n //graph/scm-plugin -i scm-api-plugin.git/objects/pack/pack-156db553fe00511509f8395aaeb0eed2f0871e9c.pack
 * -r master=5631932f5cdf6c3b829911b6fe5ab42d436d74da (without the force option and having to provide -r)
 */
public class GraphReceivePackDelegator extends BaseDelegator implements IGraphReceivePackDelegator {

	/**
	 * Instantiates a new graph commit log delegator.
	 *
	 * @param server the server
	 */
	public GraphReceivePackDelegator(final IOptionsServer server) {
		super(server);
	}

	/**
	 * Invokes the receive-pack command on the sever.
	 *
	 * @param options
	 * @throws P4JavaException
	 */
	@Override
	public void doGraphReceivePack(GraphReceivePackOptions options) throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(
				GRAPH, Parameters.processParameters(options, server), null);

		if (nonNull(resultMaps)) {
			for (Map<String, Object> map : resultMaps) {
				ResultMapParser.handleErrorStr(map);
			}
		}
	}
}
