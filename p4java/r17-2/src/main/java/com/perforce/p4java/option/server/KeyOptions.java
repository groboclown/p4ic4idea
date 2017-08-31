/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer's setKey method.
 */
public class KeyOptions extends Options {
	
	/**
	 * Options: -u, -d, -i
	 */
	public static final String OPTIONS_SPECS = "b:u b:d b:i";
	
	protected boolean undocKey = false;

	/**
	 * If true, delete the counter. Corresponds to the -d flag.
	 */
	protected boolean delete = false;
	
	/**
	 * If true, increment the key and return the new value. This option is used
	 * instead of a value argument and can only be used with numeric key values.
	 * Corresponds to the -i flag.<p>
	 */
	protected boolean incrementKey = false;

	/**
	 * Default constructor.
	 */
	public KeyOptions() {
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
	public KeyOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit value constructor.
	 */
	public KeyOptions(boolean delete, boolean incrementCounter) {
		super();
		this.delete = delete;
		this.incrementKey = incrementCounter;
	}
	
	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isUndocKey(),
								this.isDelete(),
								this.isIncrementKey());

		return this.optionList;
	}

	public boolean isUndocKey() {
		return undocKey;
	}

	public KeyOptions setUndocKey(boolean undocKey) {
		this.undocKey = undocKey;
		return this;
	}

	public boolean isDelete() {
		return delete;
	}

	public KeyOptions setDelete(boolean delete) {
		this.delete = delete;
		return this;
	}

	public boolean isIncrementKey() {
		return incrementKey;
	}

	public KeyOptions setIncrementKey(boolean incrementKey) {
		this.incrementKey = incrementKey;
		return this;
	}
}
