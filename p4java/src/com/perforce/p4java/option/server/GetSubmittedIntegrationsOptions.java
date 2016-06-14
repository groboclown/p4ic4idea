/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer getSubmittedIntegrations method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getSubmittedIntegrations(java.util.List, com.perforce.p4java.option.server.GetSubmittedIntegrationsOptions)
 */
public class GetSubmittedIntegrationsOptions extends Options {
	/**
	 * Options: -b[branch], -r
	 */
	public static final String OPTIONS_SPECS = "s:b b:r";

	/**
	 * If not null, only files integrated from the
	 * source to target files in the branch view are shown.
	 * Corresponds to -b.
	 */
	protected String branchSpec = null;
	
	/**
	 * If true, reverse the mappings in the branch view, with the
	 * target files and source files exchanging place.
	 * Corresponds to -r.
	 */
	protected boolean reverseMappings = false;
	
	/**
	 * Default constructor.
	 */
	public GetSubmittedIntegrationsOptions() {
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
	public GetSubmittedIntegrationsOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetSubmittedIntegrationsOptions(String branchSpec,
			boolean reverseMappings) {
		super();
		this.branchSpec = branchSpec;
		this.reverseMappings = reverseMappings;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.getBranchSpec(),
								this.isReverseMappings());

		return this.optionList;
	}

	public String getBranchSpec() {
		return branchSpec;
	}

	public GetSubmittedIntegrationsOptions setBranchSpec(String branchSpec) {
		this.branchSpec = branchSpec;
		return this;
	}

	public boolean isReverseMappings() {
		return reverseMappings;
	}

	public GetSubmittedIntegrationsOptions setReverseMappings(boolean reverseMappings) {
		this.reverseMappings = reverseMappings;
		return this;
	}
}
