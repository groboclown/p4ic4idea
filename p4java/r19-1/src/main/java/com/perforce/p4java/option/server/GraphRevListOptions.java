package com.perforce.p4java.option.server;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

/**
 * Encapsulates the option values as required by the
 * 'p4 graph rev-list' command
 */
public class GraphRevListOptions extends Options {

	public static final String REVLIST_COMMAND_PART = "rev-list";
	public static final String OPTIONS_SPECS = "s:n i:m:gtz";

	/**
	 * The repo against which the rev-list command is issued
	 */
	private String depot;

	/**
	 * The maximum number of items to be returned by the graph rev-list command
	 */
	private int maxValue;

	/**
	 * Additional commit SHA values that can be searched by the graph rev-list command
	 */
	private String[] commitValue;

	public GraphRevListOptions() {

	}

	/**
	 * @param server possibly-null IServer representing the Perforce server the
	 *               options are to be used against. If this parameter is null, it
	 *               is acceptable to throw an OptionsException, but it is also
	 *               possible to ignore it and do the best you can with what you've
	 *               got...
	 * @return list of options strings associated with this Option.
	 * @throws OptionsException
	 */
	@Override
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS, this.depot, this.maxValue);
		if (commitValue != null && commitValue.length > 0) {
			StringBuilder sb = new StringBuilder();
			for (String commitValueItem : commitValue) {
				this.optionList.add(commitValueItem);
			}
		}
		this.optionList.add(0, REVLIST_COMMAND_PART);
		return this.optionList;
	}

	public GraphRevListOptions(String depot, int maxValue, String... commitValue) {
		this.depot = depot;
		this.maxValue = maxValue;
		this.commitValue = commitValue;
	}

	/**
	 * Sets the mandatory option value for option -n
	 *
	 * @param depot - The repo against which the rev-list command is issued
	 * @return GraphRevListOptions with depot set
	 */
	public GraphRevListOptions withDepot(String depot) {
		this.depot = depot;
		return this;
	}

	/**
	 * Sets the optional option value for option -m
	 *
	 * @param maxValue - The maximum number of items to be returned by the graph rev-list command
	 * @return GraphRevListOptions with max value set
	 */
	public GraphRevListOptions withMaxValue(int maxValue) {
		this.maxValue = maxValue;
		return this;
	}

	/**
	 * Sets the optional argument of commit SHA values
	 *
	 * @param commitValue - Additional commit SHA values that can be searched by the graph rev-list command
	 * @return GraphRevListOptions with commit value set
	 */
	public GraphRevListOptions withCommitValue(String... commitValue) {
		this.commitValue = commitValue;
		return this;
	}
}
