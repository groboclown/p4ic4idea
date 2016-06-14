/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for Perforce reload methods.
 * <p>
 * 
 * Note that the full semantics of these options are found in the main 'p4 help
 * reload' documentation.
 */
public class ReloadOptions extends Options {
	
	/**
	 * Options: [-f] [-c client | -l label | -s stream]
	 */
	public static final String OPTIONS_SPECS = "b:f s:c s:l s:s";
	
	
	/**
	 * If true, forces the unloading of the specified client or label.
	 * Corresponds to the -f flag.
	 * <p>
	 * 
	 * By default, users can only unload their own clients or labels. The -f
	 * flag requires 'admin' access, which is granted by 'p4 protect'.
	 */
	protected boolean force = false;

	/**
	 * If not null, unload the specified client. Corresponds to the -c client
	 * flag.
	 */
	protected String client = null;

	/**
	 * If not null, unload the specified label. Corresponds to the -l label
	 * flag.
	 */
	protected String label = null;

	/**
	 * If not null, unload the specified task stream. Corresponds to the -s
	 * label flag.
	 */
	protected String stream = null;

	/**
	 * Default constructor.
	 */
	public ReloadOptions() {
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
	public ReloadOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit value constructor.
	 */
	public ReloadOptions(boolean force, String client, String label) {
		super();
		this.force = force;
		this.client = client;
		this.label = label;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isForce(),
								this.getClient(),
								this.getLabel(),
								this.getStream());

		return this.optionList;
	}

	public boolean isForce() {
		return force;
	}

	public ReloadOptions setForce(boolean force) {
		this.force = force;
		return this;
	}

	public String getClient() {
		return client;
	}

	public ReloadOptions setClient(String client) {
		this.client = client;
		return this;
	}

	public String getLabel() {
		return label;
	}

	public ReloadOptions setLabel(String label) {
		this.label = label;
		return this;
	}

	public String getStream() {
		return stream;
	}

	public ReloadOptions setStream(String stream) {
		this.stream = stream;
		return this;
	}
}