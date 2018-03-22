/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer's setCounter method.
 */
public class CounterOptions extends Options {
	
	/**
	 * Options: -u, -f, -d, -i
	 */
	public static final String OPTIONS_SPECS = "b:u b:f b:d b:i";
	
	protected boolean undocCounter = false;

	/**
	 * If true, set or delete counters used by Perforce, as listed
	 * in 'p4 help counter'. Corresponds to the -f flag.
	 */
	protected boolean perforceCounter = false;
	
	/**
	 * If true, delete the counter. Corresponds to the -d flag.
	 */
	protected boolean delete = false;
	
	/**
	 * If true, increment the counter and return the new value. This option is
	 * used instead of a value argument and can only be used with numeric key
	 * values. Corresponds to the -i flag.<p>
	 * 
	 * Note: will work only with 10.1 or later servers.
	 */
	protected boolean incrementCounter = false;

	/**
	 * Default constructor.
	 */
	public CounterOptions() {
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
	public CounterOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit value constructor.
	 */
	public CounterOptions(boolean perforceCounter, boolean delete,
			boolean incrementCounter) {
		super();
		this.perforceCounter = perforceCounter;
		this.delete = delete;
		this.incrementCounter = incrementCounter;
	}
	
	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isUndocCounter(),
								this.isPerforceCounter(),
								this.isDelete(),
								this.isIncrementCounter());

		return this.optionList;
	}

	public boolean isUndocCounter() {
		return undocCounter;
	}

	public CounterOptions setUndocCounter(boolean undocCounter) {
		this.undocCounter = undocCounter;
		return this;
	}

	public boolean isPerforceCounter() {
		return perforceCounter;
	}

	public CounterOptions setPerforceCounter(boolean perforceCounter) {
		this.perforceCounter = perforceCounter;
		return this;
	}

	public boolean isDelete() {
		return delete;
	}

	public CounterOptions setDelete(boolean delete) {
		this.delete = delete;
		return this;
	}

	public boolean isIncrementCounter() {
		return incrementCounter;
	}

	public CounterOptions setIncrementCounter(boolean incrementCounter) {
		this.incrementCounter = incrementCounter;
		return this;
	}
}
