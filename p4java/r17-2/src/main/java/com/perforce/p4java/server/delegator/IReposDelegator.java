package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.IRepo;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.ReposOptions;

import java.util.List;

public interface IReposDelegator {

	/**
	 * Gets the repos.
	 *
	 * @return a list of repos
	 * @throws ConnectionException
	 *             the connection exception
	 * @throws RequestException
	 *             the request exception
	 * @throws AccessException
	 *             the access exception
	 */
	List<IRepo> getRepos() throws ConnectionException, RequestException, AccessException;

	/**
	 * Get the repos with filter options
	 *
	 * @param options Repos filter options
	 * @return a list of repos
	 * @throws P4JavaException an api exception
	 */
	List<IRepo> getRepos(ReposOptions options) throws P4JavaException;

	/**
	 * Gets the repos mapped within the client's view.
	 *
	 * @param clientName the name of the client workspace
	 * @return a list of repos
	 * @throws ConnectionException
	 * @throws RequestException
	 * @throws AccessException
	 */
	List<IRepo> getRepos(String clientName) throws ConnectionException, RequestException, AccessException;
}
