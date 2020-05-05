package com.perforce.p4java.option.server;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

public class GraphShowRefOptions extends Options {

	public static final String SHOWREF_COMMAND_PART = "show-ref";
	public static final String OPTIONS_SPECS = "s:n i:m:gtz b:a s:u s:t";

	/**
	 * The repo against which the rev-list command is issued
	 */
	private String repo;

	/**
	 * The maximum number of items to be returned by the graph rev-list command
	 */
	private int maxValue;

	private boolean a;

	private String user;

	private String type;

	public GraphShowRefOptions() {

	}

	/**
	 * @param server possibly-null IServer representing the Perforce server the
	 *               options are to be used against. If this parameter is null, it
	 *               is acceptable to throw an OptionsException, but it is also
	 *               possible to ignore it and do the best you can with what you've
	 *               got...
	 * @return list of options strings associated with this Option
	 * @throws OptionsException
	 */
	@Override
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS, this.repo, this.maxValue, this.a, this.user, this.type);
		this.optionList.add(0, SHOWREF_COMMAND_PART);
		return this.optionList;
	}

	public GraphShowRefOptions(String repo, int maxValue, boolean a, String user, String type) {
		this.repo = repo;
		this.maxValue = maxValue;
		this.a = a;
		this.user = user;
		this.type = type;
	}

	/**
	 * Sets the mandatory option value for option -n {repo}
	 *
	 * @param repo - The repo against which the rev-list command is issued
	 * @return GraphShowRefOptions with repo filter set
	 */
	public GraphShowRefOptions setRepo(String repo) {
		this.repo = repo;
		return this;
	}

	/**
	 * Sets the optional option value for option -m {max}
	 *
	 * @param maxValue - The maximum number of items to be returned by the graph rev-list command
	 * @return GraphShowRefOptions with max value filter set
	 */
	public GraphShowRefOptions setMaxValue(int maxValue) {
		this.maxValue = maxValue;
		return this;
	}

	/**
	 * Sets the optional option value for option -a
	 *
	 * @param a - true or false
	 * @return GraphShowRefOptions with 'A' filter set
	 */
	public GraphShowRefOptions setA(boolean a) {
		this.a = a;
		return this;
	}

	/**
	 * Sets the optional option value for option -u {user}
	 *
	 * @param user - user to filter with
	 * @return GraphShowRefOptions with use filter set
	 */
	public GraphShowRefOptions setUser(String user) {
		this.user = user;
		return this;
	}

	/**
	 * Sets the optional option value for option -t {type}
	 *
	 * @param type - options type
	 * @return GraphShowRefOptions with type filter set
	 */
	public GraphShowRefOptions setType(String type) {
		this.type = type;
		return this;
	}

}
