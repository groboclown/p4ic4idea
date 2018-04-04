/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer's getPropertyValues methods.
 */
public class GetPropertyOptions extends Options {
	
	/**
	 * Options: [-A] [-n name [-s sequence] [-u user | -g group]] [-F filter -T taglist -m max]
	 */
	public static final String OPTIONS_SPECS = "b:A s:n i:s:gtz s:u s:g s:F s:T i:m:gtz";
	
	/**
	 * If true, specifies that properties for all users and groups should be
	 * listed. This option requires the user to have 'admin' access granted by
	 * 'p4 protect'. Corresponds to the -A flag.
	 */
	protected boolean listAll = false;

	/**
	 * If not null, use this as the name of this property.
	 * Corresponds to the -n flag.
	 */
	protected String name = null;
	
	/**
	 * If greater than zero, use this as the sequence number of this property.
	 * If the sequence is not specified, it defaults to 1.
	 * Corresponds to the -s flag.
	 */
	protected int sequence = 0;

	/**
	 * If not null, use this as the user to whom this property applies.
	 * Corresponds to the -u flag.
	 */
	protected String user = null;

	/**
	 * If not null, use this as the user group to which this property applies.
	 * Corresponds to the -g flag.
	 */
	protected String group = null;

    /**
     * If non-null, limits output to properties matching the filter pattern.
     * The filter is composed of an exact field name and a field value pattern.
     * (i.e. -F"name=test-property*"). This option can only be used with tagged
     * format (ztag). Corresponds to '-F' flag.
     */
    protected String filter = null;
	
    /**
     * If non-null, limit the fields that are returned to the tagged format
     * output fields. Separate multiple tagged format output fields with commas
     * (i.e. -T"name,sequence,value,time"). This option can only be used with
     * tagged format (-ztag). Corresponds to '-T' flag.
     */
    protected String fields = null;
	
	/**
	 * If greater than zero, limits output to the first 'max' number of
	 * properties. Corresponds to -m flag.
	 */
	protected int max = 0;
	
	/**
	 * Default constructor.
	 */
	public GetPropertyOptions() {
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
	public GetPropertyOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit value constructor.
	 */
	public GetPropertyOptions(boolean listAll, String name,
			int sequence, String user, String group, String filter,
			String fields, int max) {
		super();
		this.listAll = listAll;
		this.name = name;
		this.sequence = sequence;
		this.user = user;
		this.group = group;
		this.filter = filter;
		this.fields = fields;
		this.max = max;
	}
	
	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isListAll(),
								this.getName(),
								this.getSequence(),
								this.getUser(),
								this.getGroup(),
								this.getFilter(),
								this.getFields(),
								this.getMax());

		return this.optionList;
	}

	public boolean isListAll() {
		return this.listAll;
	}

	public GetPropertyOptions setListAll(boolean listAll) {
		this.listAll = listAll;
		return this;
	}

	public String getName() {
		return this.name;
	}

	public GetPropertyOptions setName(String name) {
		this.name = name;
		return this;
	}

	public int getSequence() {
		return this.sequence;
	}

	public GetPropertyOptions setSequence(int sequence) {
		this.sequence = sequence;
		return this;
	}

	public String getUser() {
		return this.user;
	}

	public GetPropertyOptions setUser(String user) {
		this.user = user;
		return this;
	}

	public String getGroup() {
		return this.group;
	}

	public GetPropertyOptions setGroup(String group) {
		this.group = group;
		return this;
	}

	public String getFilter() {
		return this.filter;
	}

	public GetPropertyOptions setFilter(String filter) {
		this.filter = filter;
		return this;
	}

	public String getFields() {
		return this.fields;
	}

	public GetPropertyOptions setFields(String fields) {
		this.fields = fields;
		return this;
	}

	public int getMax() {
		return this.max;
	}

	public GetPropertyOptions setMax(int max) {
		this.max = max;
		return this;
	}
}
