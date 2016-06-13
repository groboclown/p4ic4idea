/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options subclass for the IServer.getFixList method.
 */

public class GetFixesOptions extends Options {

	/**
	 * Options: c[changelist], -j[job], -i, m[max]
	 */
	public static final String OPTIONS_SPECS = "i:c:cl s:j b:i i:m:gtz";
	
	/**
	 * If non-negative, only fixes from the numbered changelist are listed.
	 * Corresponds to -c.
	 */
	protected int changelistId = IChangelist.UNKNOWN;
	
	/**
	 * If non-null, only fixes for the named job are listed.
	 * Corresponds to -j.
	 */
	protected String jobId = null;
	
	/**
	 * If true, include any fixes made by changelists integrated
	 * into the specified files. Corresponds to -i.
	 */
	protected boolean includeIntegrations = false;
	
	/**
	 * If positive, restrict the list to the first maxFixes fixes.
	 * Corresponds to -m.
	 */
	protected int maxFixes = 0;
	
	/**
	 * Default constructor.
	 */
	public GetFixesOptions() {
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
	public GetFixesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetFixesOptions(int changelistId, String jobId,
			boolean includeIntegrations, int maxFixes) {
		super();
		this.changelistId = changelistId;
		this.jobId = jobId;
		this.includeIntegrations = includeIntegrations;
		this.maxFixes = maxFixes;
	}
	
	/**
	 * IServer.getFixList-specific options processing. Uses the generic Options.processFields
	 * method to process options according to the static OPTIONS_SPECS field; will bypass
	 * processing if this.optionList is non-null; will set this.optionList if processing
	 * succeeds.
	 * 
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	
	@Override
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.changelistId,
								this.jobId,
								this.includeIntegrations,
								this.maxFixes);
		
		return this.optionList;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public GetFixesOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public String getJobId() {
		return jobId;
	}

	public GetFixesOptions setJobId(String jobId) {
		this.jobId = jobId;
		return this;
	}

	public boolean isIncludeIntegrations() {
		return includeIntegrations;
	}

	public GetFixesOptions setIncludeIntegrations(boolean includeIntegrations) {
		this.includeIntegrations = includeIntegrations;
		return this;
	}

	public int getMaxFixes() {
		return maxFixes;
	}

	public GetFixesOptions setMaxFixes(int maxFixes) {
		this.maxFixes = maxFixes;
		return this;
	}
}
