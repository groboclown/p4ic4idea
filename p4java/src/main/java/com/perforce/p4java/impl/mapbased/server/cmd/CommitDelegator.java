package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.CommitAction;
import com.perforce.p4java.graph.ICommit;
import com.perforce.p4java.graph.IGraphObject;
import com.perforce.p4java.impl.generic.graph.Commit;
import com.perforce.p4java.impl.generic.graph.GraphObject;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.GraphCommitLogOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.delegator.ICommitDelegator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwRequestExceptionIfConditionFails;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseDataList;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.*;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.toServerMessage;
import static com.perforce.p4java.server.CmdSpec.GRAPH;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CommitDelegator extends BaseDelegator implements ICommitDelegator {

	/**
	 * Instantiates a new graph commit delegator.
	 *
	 * @param server the server
	 */
	public CommitDelegator(final IOptionsServer server) {
		super(server);
	}

	@Override
	public ICommit getCommitObject(String sha) throws P4JavaException {

		List<Map<String, Object>> resultMaps = execMapCmdList(
				GRAPH,
				Parameters.processParameters(
						null, null, new String[]{"cat-file", "commit", sha}, server),
				null);

		List<ICommit> commits = parseCommitList(resultMaps);

		// should only return a single result
		if(commits != null && !commits.isEmpty()) {
			return commits.get(0);
		}

		return null;
	}

	@Override
	public ICommit getCommitObject(String sha, String repo) throws P4JavaException {

		List<Map<String, Object>> resultMaps = execMapCmdList(
				GRAPH,
				Parameters.processParameters(
						null, null, new String[]{"cat-file", "-n", repo, "commit", sha}, server),
				null);

		List<ICommit> commits = parseCommitList(resultMaps);

		// should only return a single result
		if(commits != null && !commits.isEmpty()) {
			return commits.get(0);
		}

		return null;
	}

	@Override
	public InputStream getBlobObject(String repo, String sha) throws P4JavaException {

		InputStream inputStream = execStreamCmd(
				GRAPH,
				Parameters.processParameters(
						null, null, new String[]{"cat-file", "-n", repo, "blob", sha}, server)
		);

		return inputStream;
	}

	@Override
	public IGraphObject getGraphObject(String sha) throws P4JavaException {

		List<Map<String, Object>> resultMaps = execMapCmdList(
				CmdSpec.GRAPH,
				Parameters.processParameters(
						null, null, new String[]{"cat-file", "-t", sha}, server),
				null);

		if (!nonNull(resultMaps)) {
			return null;
		}

		String rsha = null;
		String type = null;

		for (Map<String, Object> map : resultMaps) {
			// p4ic4idea: use IServerMessage
			IServerMessage message = toServerMessage(map);
			if (nonNull(message)) {

				// Check for errors
				ResultMapParser.handleErrors(message);

				// p4ic4idea: this line wasn't doing anything that the above checks weren't doing.
				//throwRequestExceptionIfConditionFails(!message.isError(), message);
				try {
					if (map.containsKey(SHA)) {
						rsha = parseString(map, SHA);
					}
					if (map.containsKey(TYPE)) {
						type = parseString(map, TYPE);
					}
				// p4ic4idea: do not handle Throwable unless you're really, really careful.
				//} catch (Throwable thr) {
				} catch (Exception thr) {
					Log.exception(thr);
				}
			}
		}

		return new GraphObject(rsha, type);
	}

	/**
	 * Returns a List<IGraphCommitLog> encapsulating a commit logs which holds the
	 * data retrieved as part of the 'p4 graph log -n command'
	 *
	 * @param options Various options supported by the command
	 * @return List<IGraphCommitLog>
	 * @throws P4JavaException
	 */
	@Override
	public List<ICommit> getGraphCommitLogList(GraphCommitLogOptions options) throws P4JavaException {

		List<Map<String, Object>> resultMaps = execMapCmdList(
				GRAPH, Parameters.processParameters(options, server), null);

		if (!nonNull(resultMaps)) {
			return null;
		}

		return parseCommitList(resultMaps);
	}

	private List<ICommit> parseCommitList(List<Map<String, Object>> resultMaps) throws P4JavaException {

		List<ICommit> list = new ArrayList<>();

		if (!nonNull(resultMaps)) {
			return null;
		}

		for (Map<String, Object> map : resultMaps) {

			String commit = null;
			String tree = null;
			CommitAction action = CommitAction.UNKNOWN;
			List<String> parent = new ArrayList<>();
			String author = null;
			String authorEmail = null;
			Date date = null;
			String committer = null;
			String committerEmail = null;
			Date committerDate = null;
			String description = null;

			if (!nonNull(map) || map.isEmpty()) {
				return null;
			}

			// Check for errors
			ResultMapParser.handleErrorStr(map);

			try {
				if (map.containsKey(COMMIT)) {
					commit = parseString(map, COMMIT);
				}
				if (map.containsKey(TREE)) {
					tree = parseString(map, TREE);
				}
				if (map.containsKey(ACTION)) {
					action = CommitAction.parse(parseString(map, ACTION));
				}

				parent = parseDataList(map, PARENT);

				if (map.containsKey(AUTHOR)) {
					author = parseString(map, AUTHOR);
				}
				if (map.containsKey(AUTHOR_EMAIL)) {
					authorEmail = parseString(map, AUTHOR_EMAIL);
				}
				if (map.containsKey(DATE)) {
					date = new Date(parseLong(map, DATE) * 1000);
				}

				if (map.containsKey(COMMITTER)) {
					committer = parseString(map, COMMITTER);
				}
				if (map.containsKey(COMMITTER_EMAIL)) {
					committerEmail = parseString(map, COMMITTER_EMAIL);
				}
				if (map.containsKey(COMMITTER_DATE)) {
					committerDate = new Date(parseLong(map, COMMITTER_DATE) * 1000);
				}

				if (map.containsKey(GRAPH_DESC)) {
					description = parseString(map, GRAPH_DESC);
				}
			} catch (Throwable thr) {
				Log.exception(thr);
			}

			Commit entry = new Commit(commit, tree, action, parent, author, authorEmail, date, committer, committerEmail, committerDate, description);
			list.add(entry);
		}

		return list;
	}
}
