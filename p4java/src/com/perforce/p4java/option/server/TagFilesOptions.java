/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer.tagFiles method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#tagFiles(java.util.List, java.lang.String, com.perforce.p4java.option.server.TagFilesOptions)
 */
public class TagFilesOptions extends Options {
	
	/**
	 * Options: -d, -n
	 */
	public static final String OPTIONS_SPECS = "b:n b:d";
	
	protected boolean listOnly = false;
	protected boolean delete = false;

	/**
	 * Default constructor.
	 */
	public TagFilesOptions() {
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
	public TagFilesOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit-value constructor.
	 */
	public TagFilesOptions(boolean listOnly, boolean delete) {
		super();
		this.listOnly = listOnly;
		this.delete = delete;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS, this.isListOnly(), this.isDelete());
		return this.optionList;
	}

	public boolean isListOnly() {
		return listOnly;
	}

	public TagFilesOptions setListOnly(boolean listOnly) {
		this.listOnly = listOnly;
		return this;
	}

	public boolean isDelete() {
		return delete;
	}

	public TagFilesOptions setDelete(boolean delete) {
		this.delete = delete;
		return this;
	}
}
