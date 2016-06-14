/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer getDirectories method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getDirectories(java.util.List, com.perforce.p4java.option.server.GetDirectoriesOptions)
 */
public class GetDirectoriesOptions extends Options {
	
	/**
	 * Options: -C, -D, -H, -S[stream]
	 */
	public static final String OPTIONS_SPECS = "b:C b:D b:H s:S";
	
	/**
	 * If true, limit the returns to directories that are mapped in
	 * the current Perforce client workspace. Corresponds to -C.
	 */
	protected boolean clientOnly = false;
	
	/**
	 * If true, includes directories with only deleted files.
	 * Corresponds to -D.
	 */
	protected boolean deletedOnly = false;
	
	/**
	 *  If true, lists directories of files on the 'have' list.
	 *  Corresponds to -H.
	 */
	protected boolean haveListOnly = false;

    /**
     * If non-null, limits output to depot directories mapped in a stream's
     * client view. Corresponds to the "-S stream" flag.
     */
    protected String stream = null;

    /**
	 * Default constructor -- sets all fields to false.
	 */
	public GetDirectoriesOptions() {
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
	public GetDirectoriesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetDirectoriesOptions(boolean clientOnly, boolean deletedOnly,
			boolean haveListOnly) {
		super();
		this.clientOnly = clientOnly;
		this.deletedOnly = deletedOnly;
		this.haveListOnly = haveListOnly;
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetDirectoriesOptions(boolean clientOnly, boolean deletedOnly,
			boolean haveListOnly, String stream) {
		super();
		this.clientOnly = clientOnly;
		this.deletedOnly = deletedOnly;
		this.haveListOnly = haveListOnly;
		this.stream = stream;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
										this.isClientOnly(),
										this.isDeletedOnly(),
										this.isHaveListOnly(),
										this.getStream());
		return this.optionList;
	}

	public boolean isClientOnly() {
		return clientOnly;
	}

	public GetDirectoriesOptions setClientOnly(boolean clientOnly) {
		this.clientOnly = clientOnly;
		return this;
	}

	public boolean isDeletedOnly() {
		return deletedOnly;
	}

	public GetDirectoriesOptions setDeletedOnly(boolean deletedOnly) {
		this.deletedOnly = deletedOnly;
		return this;
	}

	public boolean isHaveListOnly() {
		return haveListOnly;
	}

	public GetDirectoriesOptions setHaveListOnly(boolean haveListOnly) {
		this.haveListOnly = haveListOnly;
		return this;
	}

	public String getStream() {
    	return stream;
	}

	public GetDirectoriesOptions setStream(String stream) {
    	this.stream = stream;
    	return this;
	}
}
