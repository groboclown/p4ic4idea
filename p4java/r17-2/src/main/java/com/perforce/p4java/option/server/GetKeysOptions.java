/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer's getKeys method.
 */
public class GetKeysOptions extends Options {
	
    /**
     * Options: -u, -e[nameFilter] ..., -m[max]
     */
    public static final String OPTIONS_SPECS = "b:u s:e s[]:e i:m:gtz";
	
	
	protected boolean undocKey = false;

    /**
     * If non-null, limits output to keys whose name matches
     * the nameFilter pattern. Corresponds to '-e nameFilter' flag
     */
    protected String nameFilter = null;

    /**
     * If non-null, limits output to keys whose name matches
     * any of the nameFilter patterns. Corresponds to the multiple
     * '-u -e nameFilter -e nameFilter -e nameFilter ...' flags.
     */
    protected String[] nameFilters = null;

    /**
     * If greater than zero, limit output to the first maxResults
     * number of keys. Corresponds to '-m max' flag.
     */
    protected int maxResults = 0;

    /**
	 * Default constructor.
	 */
	public GetKeysOptions() {
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
	public GetKeysOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit value constructor.
	 */
	public GetKeysOptions(boolean undocKey, String nameFilter,
			int maxResults) {
		super();
		this.undocKey = undocKey;
		this.nameFilter = nameFilter;
		this.maxResults = maxResults;
	}
	
	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isUndocKey(),
								this.getNameFilter(),
								this.getNameFilters(),
								this.getMaxResults());

		return this.optionList;
	}

	public boolean isUndocKey() {
		return undocKey;
	}

	public GetKeysOptions setUndocKey(boolean undocKey) {
		this.undocKey = undocKey;
		return this;
	}

	public String getNameFilter() {
		return nameFilter;
	}

	public GetKeysOptions setNameFilter(String nameFilter) {
		this.nameFilter = nameFilter;
		return this;
	}

	public String[] getNameFilters() {
		return nameFilters;
	}

	public GetKeysOptions setNameFilters(String[] nameFilters) {
		this.nameFilters = nameFilters;
		return this;
	}
	
	public int getMaxResults() {
		return maxResults;
	}

	public GetKeysOptions setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}

}
