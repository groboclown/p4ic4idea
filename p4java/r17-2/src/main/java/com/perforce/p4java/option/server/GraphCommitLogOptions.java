package com.perforce.p4java.option.server;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

/**
 * This class is used tp encapsulate all the information
 * that forms the options part of the 'p4 graph log' command
 * <p>
 * Usage: log -n repo [ -u user -A date-B date -m N -N N -X N ] [ commit... ]
 * <p>
 */
public class GraphCommitLogOptions extends Options {

	public static final String GRAPH_COMMITLOG_COMMAND_PART = "log";
	public static final String OPTIONS_SPECS = "s:n s:u i:m:gtz s:A s:B i:N:gtz i:X:gtz";

	/**
	 * The repo against which the 'p4 graph log' command is issued
	 */
	private String repo;

	/**
	 * User who has made the commits
	 */
	private String user;

	/**
	 * The maximum number of items to be returned by the graph log command
	 */
	private int maxResults;

	/**
	 * Date starting from when the commit logs will be fetched
	 */
	private String startDate;

	/**
	 * Date used as the end date up to when the commit logs will be fetched
	 */
	private String endDate;

	/**
	 * A lower bound for the numbmer of parents a commit is expected to have as part of the fetch
	 */
	private int minNumberOfParents;

	/**
	 * An upper bound for the number of parents a commit is expected to have as part of the fetch
	 */
	private int maxNumberOfParents;

	/**
	 * Additional commit SHA values that can be used to filter the search
	 */
	private String[] commitValue;

	/**
	 * Default constructor
	 */
	public GraphCommitLogOptions() {

	}

	/**
	 * Constructs a GraphCommitLogOptions with the given arguments
	 *
	 * @param repo
	 * @param maxResults
	 * @param startDate
	 * @param endDate
	 * @param minNumberOfParents
	 * @param maxNumberOfParents
	 * @param commitValue
	 */
	public GraphCommitLogOptions(String repo, int maxResults, String startDate, String endDate,
	                             int minNumberOfParents, int maxNumberOfParents, String... commitValue) {
		this.repo = repo;
		this.maxResults = maxResults;
		this.startDate = startDate;
		this.endDate = endDate;
		this.minNumberOfParents = minNumberOfParents;
		this.maxNumberOfParents = maxNumberOfParents;
		this.commitValue = commitValue;
	}

	/**
	 * @param server possibly-null IServer representing the Perforce server the
	 *               options are to be used against. If this parameter is null, it
	 *               is acceptable to throw an OptionsException, but it is also
	 *               possible to ignore it and do the best you can with what you've
	 *               got...
	 * @return
	 * @throws OptionsException
	 */
	@Override
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS, this.repo,
				this.user, this.maxResults, this.startDate,
				this.endDate, this.minNumberOfParents, this.maxNumberOfParents);
		if (commitValue != null && commitValue.length > 0) {
			for (String commitValueItem : commitValue) {
				this.optionList.add(commitValueItem);
			}
		}
		this.optionList.add(0, GRAPH_COMMITLOG_COMMAND_PART);
		return this.optionList;
	}

	/**
	 * Sets the mandatory option value for option -n
	 * which defines the repository against which the
	 * command is
	 *
	 * @param repo
	 * @return
	 */
	public void setRepo(String repo) {
		this.repo = repo;
	}

	/**
	 * Returns the repo used as part of option -n
	 *
	 * @return
	 */
	public String getRepo() {
		return this.repo;
	}

	/**
	 * Sets the optional option value for option -m
	 *
	 * @param maxResults
	 * @return
	 */
	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	/**
	 * Returns the maximum results expected from the log command
	 *
	 * @return
	 */
	public int getMaxResults() {
		return this.maxResults;
	}

	/**
	 * Sets the optional argument of commit SHA values
	 *
	 * @param commitValue
	 * @return
	 */
	public void setCommitValue(String... commitValue) {
		this.commitValue = commitValue;
	}

	/**
	 * Returns the commit SHA values
	 *
	 * @return
	 */
	public String[] getCommitValue() {
		return this.commitValue;
	}

	/**
	 * The value expected by the -A option
	 * The lower date limit of the logs retrieved.
	 *
	 * @param startDate
	 * @return
	 */
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	/**
	 * Returns The lower date limit of the logs retrieved.
	 *
	 * @return
	 */
	public String getStartDate() {
		return this.startDate;
	}

	/**
	 * The value expected by -B option
	 * The upper date limit of logs retrieved
	 *
	 * @param endDate
	 * @return
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	/**
	 * Returns the he upper date limit of logs retrieved
	 *
	 * @return
	 */
	public String getEndDate() {
		return this.endDate;
	}

	/**
	 * The value expected by the -N option.
	 * The minimum number of parents the fetched log commit is expected to have
	 *
	 * @param minimumNumberOfParents
	 * @return
	 */
	public void setMinParents(int minimumNumberOfParents) {
		this.minNumberOfParents = minimumNumberOfParents;
	}

	/**
	 * Returns the minimum number of parents the fetched log commit is expected to have
	 *
	 * @return
	 */
	public int getMinParents() {
		return this.minNumberOfParents;
	}

	/**
	 * The value expected by -X option
	 * The maximum number of parents the fetched log commit is expected to have.
	 *
	 * @param maximumNumberOfParents
	 * @return
	 */
	public void setMaxParents(int maximumNumberOfParents) {
		this.maxNumberOfParents = maximumNumberOfParents;
	}

	/**
	 * Returns the maximum number of parents the fetched log commit is expected to have.
	 *
	 * @return
	 */
	public int getMaxParents() {
		return this.maxNumberOfParents;
	}

	/**
	 * The value expected by -u option
	 * The user who is responsible for the commits retrieved  by p4 log
	 *
	 * @param user
	 * @return
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Returns the user of the commit
	 *
	 * @return
	 */
	public String getUser() {
		return this.user = user;
	}
}
