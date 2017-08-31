/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for Perforce unload methods.
 * <p>
 * 
 * Note that the full semantics of these options are found in the main 'p4 help
 * unload' documentation.
 */
public class UnloadOptions extends Options {
	
	/**
	 * Options: p4 unload [-f -L -z] [-c client | -l label | -s stream]
	 * <p>
	 * Options: p4 unload [-f -L -z] [-a|-al|-ac] [-d date | -u user]
	 */
	public static final String OPTIONS_SPECS = "b:f b:L b:z s:c s:l s:s s:a s:d s:u";
	
	
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
	 * If true, specifies that the client, label, or task stream should be
	 * unloaded even if it is locked. Corresponds to the -L flag.
	 */
	protected boolean locked = false;

	/**
	 * If true, specifies that the client or label should be stored in
	 * compressed format. Corresponds to the -z flag.
	 */
	protected boolean compress = false;

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
	 * If not null, specifies set of clients and/or labels are unloaded.
	 * Corresponds to the -a, -al -ac flags.
	 * <p>
	 * 
	 * If it is empty (empty string ""), it would unload all (-a) clients and
	 * labels. If it is "c", it would unload all clients (-ac). If it is "l", it
	 * would unload all labels (-al).
	 * <p>
	 * 
	 * Note that if the -a flag is specified, the specified set of clients
	 * and/or labels are unloaded. Specify -d and a date to unload all clients
	 * and/or labels older than that date. When -a is specified, you must
	 * specify either -d or -u (or both), and you may not specify the -c or -l
	 * flags.
	 */
	protected String all = null;

	/**
	 * If not null, unload all clients and/or labels older than that date.
	 * Corresponds to the -d date flag.
	 * <p>
	 * 
	 * The following are valid Perforce date string formats:
	 * 
	 * <pre>
	 * yyyy/mm/dd
	 * yyyy/mm/dd:hh:mm:ss
	 * yyyy/mm/dd hh:mm:ss
	 * </pre>
	 */
	protected String date = null;

	/**
	 * If not null, unload all clients and/or labels owned by that user.
	 * Corresponds to the -u user flag.
	 */
	protected String user = null;

	/**
	 * Default constructor.
	 */
	public UnloadOptions() {
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
	public UnloadOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit value constructor.
	 */
	public UnloadOptions(boolean force, boolean compress, String client,
			String label) {
		super();
		this.force = force;
		this.compress = compress;
		this.client = client;
		this.label = label;
	}

	/**
	 * Explicit value constructor.
	 */
	public UnloadOptions(boolean force, boolean compress, String all,
			String date, String user) {
		super();
		this.force = force;
		this.compress = compress;
		this.all = all;
		this.date = date;
		this.user = user;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isForce(),
								this.isLocked(),
								this.isCompress(),
								this.getClient(),
								this.getLabel(),
								this.getStream(),
								this.getAll(),
								this.getDate(),
								this.getUser());

		return this.optionList;
	}

	public boolean isForce() {
		return force;
	}

	public UnloadOptions setForce(boolean force) {
		this.force = force;
		return this;
	}

	public boolean isLocked() {
		return locked;
	}

	public UnloadOptions setLocked(boolean locked) {
		this.locked = locked;
		return this;
	}

	public boolean isCompress() {
		return compress;
	}

	public UnloadOptions setCompress(boolean compress) {
		this.compress = compress;
		return this;
	}

	public String getClient() {
		return client;
	}

	public UnloadOptions setClient(String client) {
		this.client = client;
		return this;
	}

	public String getLabel() {
		return label;
	}

	public UnloadOptions setLabel(String label) {
		this.label = label;
		return this;
	}

	public String getStream() {
		return stream;
	}

	public UnloadOptions setStream(String stream) {
		this.stream = stream;
		return this;
	}

	public String getAll() {
		return all;
	}

	public UnloadOptions setAll(String all) {
		this.all = all;
		return this;
	}

	public String getDate() {
		return date;
	}

	public UnloadOptions setDate(String date) {
		this.date = date;
		return this;
	}

	public String getUser() {
		return user;
	}

	public UnloadOptions setUser(String user) {
		this.user = user;
		return this;
	}
}