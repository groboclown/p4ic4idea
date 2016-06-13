/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options-based method options for IClient reopenFiles method(s).<p>
 * 
 * Recognized options (depending non server version, etc.):
 * -c changelist# -t filetype
 */
public class ReopenFilesOptions extends Options {
	
	/**
	 * Options: -c[changelist], -t[filetype], -Q[charset]
	 */
	public static final String OPTIONS_SPECS = "i:c:cl s:t s:Q";
	
	/** The changelist to be reopened to; if non-negative, specifies which changelist
	 * to reopen onto */
	protected int changelistId = IChangelist.DEFAULT;
	
	/** If not null, the file is reopened as the given filetype. */
	protected String fileType = null;

	/**
	 * If non-null, the files are reopened using that charset; corresponds to the
	 * undoc '-Q' flag. Please see the 'Versioned character set' section of the
	 * 'p4 help undoc' command for more info.<p>
	 * 
	 * Note that you must set the server.filecharset configurable to 1 in an
	 * unicode Perforce server in order to version the charset of individual
	 * unicode files along with the filetype.
	 */
	protected String charset = null;

	/**
	 * Default constructor.
	 */
	public ReopenFilesOptions() {
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
	public ReopenFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public ReopenFilesOptions(int changeListId, String fileType) {
		super();
		this.changelistId = changeListId;
		this.fileType = fileType;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
									this.changelistId,
									this.fileType,
									this.charset);
		return this.optionList;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public ReopenFilesOptions setChangelistId(int changeListId) {
		this.changelistId = changeListId;
		return this;
	}

	public String getFileType() {
		return fileType;
	}

	public ReopenFilesOptions setFileType(String fileType) {
		this.fileType = fileType;
		return this;
	}

	public String getCharset() {
		return charset;
	}

	public ReopenFilesOptions setCharset(String charset) {
		this.charset = charset;
		return this;
	}
}
