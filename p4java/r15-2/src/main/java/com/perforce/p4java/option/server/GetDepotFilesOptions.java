/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Simple options for the IOptionsServer.getDepotFiles method.
 */
public class GetDepotFilesOptions extends Options {

	/**
	 * Options: -a, -m[max]
	 */
	public static final String OPTIONS_SPECS = "b:a i:m:gtz";
	
	/**
	 * If positive, return maxResults or fewer files; note: this is
	 * an UNDOC feature and may not be supported on all servers.
	 * Corresponds to the undoc -m flag.
	 */
	protected int maxResults = 0;
	
	/**
	 * If true, display all revisions within the specific range, rather
	 * than just the highest revision in the range. Corresponds to -a.
	 */
	protected boolean allRevs = false;
	
	/**
	 * Default constructor;
	 */
	public GetDepotFilesOptions() {
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
	public GetDepotFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit value constructor.
	 * 
	 * @param allRevs the value for this object's allRevs field.
	 */
	public GetDepotFilesOptions(boolean allRevs) {
		this.allRevs = allRevs;
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetDepotFilesOptions(int maxResults, boolean allRevs) {
		super();
		this.maxResults = maxResults;
		this.allRevs = allRevs;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.allRevs,
								this.getMaxResults());
		return this.optionList;
	}

	public boolean isAllRevs() {
		return allRevs;
	}
	public GetDepotFilesOptions setAllRevs(boolean allRevs) {
		this.allRevs = allRevs;
		return this;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public GetDepotFilesOptions setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}
}
