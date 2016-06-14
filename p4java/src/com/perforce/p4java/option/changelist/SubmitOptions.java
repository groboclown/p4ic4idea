/**
 * 
 */
package com.perforce.p4java.option.changelist;

import java.util.ArrayList;
import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IChangelist.submit method.
 */
public class SubmitOptions extends Options {
	
	/**
	 * Options: -r, -s
	 */
	public static final String OPTIONS_SPECS = "b:r b:s";
	
	/**
	 * If true, submitted files will remain open (on the client's
	 * default changelist) after the submit has completed.
	 * Corresponds to the -r flag.
	 */
	protected boolean reOpen = false;
	
	/**
	 * If not null, should contain a list of job IDs for jobs that will have
	 * their status changed to fixed or "jobStatus", below. No corresponding
	 * flag.<p>
	 * 
	 * This list will override any jobs already in the changelist's local
	 * job list (usually the result of being fixed elsewhere and picked up
	 * during a refresh of the changelist).
	 */
	protected List<String> jobIds = null;
	
	/**
	 * If not null, should contain a string to which
	 * the jobs in the jobIds list (or local jobs list) will be set on a successful submit; if
	 * null, the jobs will be marked "fixed". Corresponds to the -s flag
	 * in conjunction with jobs field additions.
	 */
	protected String jobStatus = null;

	/**
	 * Default constructor.
	 */
	public SubmitOptions() {
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
	public SubmitOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit value constructor.
	 */
	public SubmitOptions(boolean reOpen, List<String> jobIds,
			String jobStatus) {
		super();
		this.reOpen = reOpen;
		this.jobIds = jobIds;
		this.jobStatus = jobStatus;
	}

	/**
	 * Note that the implementation of the various options here is less straightforward
	 * than for typical server-based Options classes due to the lack of one-to-one correspondence
	 * between options and flags and the need for strict option ordering.
	 * 
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = new ArrayList<String>();
		this.optionList.addAll(this.processFields(OPTIONS_SPECS,
									this.isReOpen(),
									this.getJobStatus() == null? false : true));
		return this.optionList;
	}

	public boolean isReOpen() {
		return reOpen;
	}

	public SubmitOptions setReOpen(boolean reOpen) {
		this.reOpen = reOpen;
		return this;
	}

	public List<String> getJobIds() {
		return jobIds;
	}

	public SubmitOptions setJobIds(List<String> jobIds) {
		this.jobIds = jobIds;
		return this;
	}

	public String getJobStatus() {
		return jobStatus;
	}

	public SubmitOptions setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
		return this;
	}
}
