/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer getJobs method.
 */
public class GetJobsOptions extends Options {
	
	/**
	 * Options: -e[jobview], -i, -l, -m[max], -r
	 */
	public static final String OPTIONS_SPECS = "s:e b:i b:l i:m:gtz b:r";
	
	/**
	 * If greater than zero, limit the output to the first maxJobs jobs.
	 * Corresponds to the -m flag.
	 */
	protected int maxJobs = 0;
	
	/**
	 * If true, return full descriptions, otherwise show
	 * only a subset (typically the first 128 characters, but
	 * this is not guaranteed). Corresponds to the -l flag.
	 */
	protected boolean longDescriptions = false;
	
	/**
	 * If true, reverse the normal sort order.
	 * Corresponds to the -r flag.
	 */
	protected boolean reverseOrder = false;
	
	/**
	 * If true, include any fixes made by changelists
	 * integrated into the specified files. Corresponds to
	 * the -i flag.
	 */
	protected boolean includeIntegrated = false;
	
	/**
	 * If not null, this should be a string in format detailed by "p4 help jobview"
	 * used to restrict jobs to those satisfying the job view expression.
	 * Corresponds to the -e flag.
	 */
	protected String jobView = null;

	/**
	 * Default constructor.
	 */
	public GetJobsOptions() {
		super();
	}

	/**
	 * Strings-based constructor; see 'p4 help [command]' for possible options.
	 * <p>
	 * 
	 * <b>WARNING: you should not pass more than one option or argument in each
	 * string parameter. Each option or argument should be passed-in as its own
	 * separate string parameter, without any spaces between the option and the
	 * option value (if any).<b>
	 * <p>
	 * 
	 * <b>NOTE: setting options this way always bypasses the internal options
	 * values, and getter methods against the individual values corresponding to
	 * the strings passed in to this constructor will not normally reflect the
	 * string's setting. Do not use this constructor unless you know what you're
	 * doing and / or you do not also use the field getters and setters.</b>
	 * 
	 * @see com.perforce.p4java.option.Options#Options(java.lang.String...)
	 */
	public GetJobsOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetJobsOptions(int maxJobs, boolean longDescriptions,
			boolean reverseOrder, boolean includeIntegrated, String jobView) {
		super();
		this.maxJobs = maxJobs;
		this.longDescriptions = longDescriptions;
		this.reverseOrder = reverseOrder;
		this.includeIntegrated = includeIntegrated;
		this.jobView = jobView;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.getJobView(),
								this.isIncludeIntegrated(),
								this.isLongDescriptions(),
								this.getMaxJobs(),
								this.isReverseOrder());
		return this.optionList;
	}

	public int getMaxJobs() {
		return maxJobs;
	}

	public GetJobsOptions setMaxJobs(int maxJobs) {
		this.maxJobs = maxJobs;
		return this;
	}

	public boolean isLongDescriptions() {
		return longDescriptions;
	}

	public GetJobsOptions setLongDescriptions(boolean longDescriptions) {
		this.longDescriptions = longDescriptions;
		return this;
	}

	public boolean isReverseOrder() {
		return reverseOrder;
	}

	public GetJobsOptions setReverseOrder(boolean reverseOrder) {
		this.reverseOrder = reverseOrder;
		return this;
	}

	public boolean isIncludeIntegrated() {
		return includeIntegrated;
	}

	public GetJobsOptions setIncludeIntegrated(boolean includeIntegrated) {
		this.includeIntegrated = includeIntegrated;
		return this;
	}

	public String getJobView() {
		return jobView;
	}

	public GetJobsOptions setJobView(String jobView) {
		this.jobView = jobView;
		return this;
	}
}
