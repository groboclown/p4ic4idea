package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.core.IRepo;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Repo;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.ReposOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IReposDelegator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.server.CmdSpec.REPOS;

public class ReposDelegator extends BaseDelegator implements IReposDelegator {

	/**
	 * Instantiates a new repos delegator.
	 *
	 * @param server - an instance of the currently effective server implementaion
	 */
	public ReposDelegator(IOptionsServer server) {
		super(server);
	}

	/**
	 * Gets the repos.
	 *
	 * @return a list of repos
	 * @throws ConnectionException the connection exception
	 * @throws RequestException    the request exception
	 * @throws AccessException     the access exception
	 */
	@Override
	public List<IRepo> getRepos() throws ConnectionException, RequestException, AccessException {

		List<Map<String, Object>> resultMaps = execMapCmdList(REPOS, new String[0], null);
		return processResults(resultMaps);
	}

	/**
	 * Get the repos with filter options
	 *
	 * @param options Repos filter options
	 * @return a list of repos
	 * @throws P4JavaException
	 */
	@Override
	public List<IRepo> getRepos(ReposOptions options) throws P4JavaException {

		List<Map<String, Object>> resultMaps = execMapCmdList(REPOS, Parameters.processParameters(options, server), null);
		return processResults(resultMaps);
	}

	/**
	 * Gets the repos mapped within the client's view.
	 *
	 * @param client the name of the client workspace
	 * @return a list of repos
	 * @throws ConnectionException
	 * @throws RequestException
	 * @throws AccessException
	 */
	@Override
	public List<IRepo> getRepos(@Nonnull String client) throws ConnectionException, RequestException, AccessException {

		if (!client.equals(server.getCurrentClient().getName())) {
			server.setCurrentClient(server.getClient(client));
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(REPOS, new String[]{"-C"}, null);
		return processResults(resultMaps);
	}

	private List<IRepo> processResults(List<Map<String, Object>> resultMaps) throws AccessException, RequestException {
		List<IRepo> metadataArray = new ArrayList<>();

		if (nonNull(resultMaps)) {
			for (Map<String, Object> map : resultMaps) {
				if (!ResultMapParser.handleErrorStr(map)) {
					metadataArray.add(new Repo(map));
				}
			}
		}

		return metadataArray;
	}
}
