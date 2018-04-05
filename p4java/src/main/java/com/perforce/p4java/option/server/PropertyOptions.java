/**
 * 
 */
package com.perforce.p4java.option.server;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

/**
 * Options class for IOptionsServer's setProperty and deleteProperty methods.
 */
public class PropertyOptions extends Options {
	
	/**
	 * Options: -n name -v value [-s sequence] [-u user | -g group]
	 */
	public static final String OPTIONS_SPECS = "s:n s:v s:s s:u s:g";
	
	/**
	 * If not null, use this as the name of this property.
	 * Corresponds to the -n flag.
	 */
	protected String name = null;
	
	/**
	 * If not null, use this as the value of this property.
	 * Corresponds to the -v flag.
	 */
	protected String value = null;
	
	/**
	 * If greater than zero, use this as the sequence number of this property.
	 * If the sequence is not specified, it defaults to 1.
	 * Corresponds to the -s flag.
	 */
	protected String sequence = null;

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
	 * Default constructor.
	 */
	public PropertyOptions() {
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
	public PropertyOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit value constructor.
	 */
	public PropertyOptions(String name, String value, String sequence,
			String user, String group) {
		super();
		this.name = name;
		this.value = value;
		this.sequence = sequence;
		this.user = user;
		this.group = group;
	}
	
	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.getName(),
								this.getValue(),
								this.getSequence(),
								this.getUser(),
								this.getGroup());

		return this.optionList;
	}

	public String getName() {
		return this.name;
	}

	public PropertyOptions setName(String name) {
		this.name = name;
		return this;
	}

	public String getValue() {
		return this.value;
	}

	public PropertyOptions setValue(String value) {
		this.value = value;
		return this;
	}

	public String getSequence() {
		return this.sequence;
	}

	public PropertyOptions setSequence(String sequence) {
		this.sequence = sequence;
		return this;
	}

	public String getUser() {
		return this.user;
	}

	public PropertyOptions setUser(String user) {
		this.user = user;
		return this;
	}

	public String getGroup() {
		return this.group;
	}

	public PropertyOptions setGroup(String group) {
		this.group = group;
		return this;
	}
}
