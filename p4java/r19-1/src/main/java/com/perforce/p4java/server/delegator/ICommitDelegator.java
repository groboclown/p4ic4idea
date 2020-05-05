package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.ICommit;
import com.perforce.p4java.graph.IGraphObject;
import com.perforce.p4java.option.server.GraphCommitLogOptions;

import java.io.InputStream;
import java.util.List;

public interface ICommitDelegator {

	/**
	 * Usage: cat-file commit {object-sha}
	 * <p>
	 * (requires 'super' permission)
	 *
	 * @param sha graph SHA
	 * @return graph commit object
	 * @throws P4JavaException API error
	 */
	ICommit getCommitObject(String sha) throws P4JavaException;

	/**
	 * Usage: cat-file -n {repo} commit {object-sha}
	 *
	 * @param sha  graph SHA
	 * @param repo graph repo
	 * @return graph commit object
	 * @throws P4JavaException API error
	 */
	ICommit getCommitObject(String sha, String repo) throws P4JavaException;

	/**
	 * Usage: cat-file -n {repo} blob {object-sha}
	 *
	 * @param sha  graph SHA
	 * @param repo graph repo
	 * @return InputStream of the graph file
	 * @throws P4JavaException API error
	 */
	InputStream getBlobObject(String repo, String sha) throws P4JavaException;

	/**
	 * Usage: cat-file -t {object-sha}
	 *
	 * @param sha  graph SHA
	 * @return graph object
	 * @throws P4JavaException API error
	 */
	IGraphObject getGraphObject(String sha) throws P4JavaException;

	/**
	 * Usage: log -n {repo} {object-sha}
	 *
	 * @param options graph log options
	 * @return list of commit objects
	 * @throws P4JavaException API error
	 */
	List<ICommit> getGraphCommitLogList(GraphCommitLogOptions options) throws P4JavaException;
}
