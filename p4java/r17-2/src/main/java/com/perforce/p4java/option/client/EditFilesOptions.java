/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options subclass for use with IClient editFiles method(s).
 * 
 * @see com.perforce.p4java.client.IClient#editFiles(java.util.List, com.perforce.p4java.option.client.EditFilesOptions)
 */
public class EditFilesOptions extends Options {
	
	/**
	 * Options: -n, -k, -c[changelist], -t[filetype], -Q[charset]
	 */
	public static final String OPTIONS_SPECS = "b:n b:k i:c:gtz s:t s:Q";
	
	/**
	 * If true, don't actually do the edit, just return the files that
	 * would have been opened for edit. Corresponds to the -n flag.
	 */
	protected boolean noUpdate = false;
	
	/**
	 * If true, bypass updating the client. Corresponds to the -k flag.
	 */
	protected boolean bypassClientUpdate = false;
	
	/**
	 * If positive, the opened files are put into the pending
	 * changelist identified by changelistId (this changelist must have been
	 * previously created for this to succeed). If zero or negative, the
	 * file is opened in the 'default' (unnumbered) changelist.
	 * Corresponds to the -c flag.
	 */
	protected int changelistId = 0;
	
	/**
	 * If non-null, the files are added as that filetype. See 'p4 help filetypes'
	 * to attempt to make any sense of Perforce file types.
	 * Corresponds to the -t flag.
	 */
	protected String fileType = null;

	/**
	 * If non-null, the files are edited using that charset; corresponds to the
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
	public EditFilesOptions() {
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
	public EditFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public EditFilesOptions(boolean noUpdate, boolean bypassClientUpdate,
			int changelistId, String fileType) {
		super();
		this.noUpdate = noUpdate;
		this.bypassClientUpdate = bypassClientUpdate;
		this.changelistId = changelistId;
		this.fileType = fileType;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.noUpdate,
								this.bypassClientUpdate,
								this.changelistId,
								this.fileType,
								this.charset);
		return this.optionList;
	}

	public boolean isNoUpdate() {
		return noUpdate;
	}

	public EditFilesOptions setNoUpdate(boolean noUpdate) {
		this.noUpdate = noUpdate;
		return this;
	}

	public boolean isBypassClientUpdate() {
		return bypassClientUpdate;
	}

	public EditFilesOptions setBypassClientUpdate(boolean bypassClientUpdate) {
		this.bypassClientUpdate = bypassClientUpdate;
		return this;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public EditFilesOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public String getFileType() {
		return fileType;
	}

	public EditFilesOptions setFileType(String fileType) {
		this.fileType = fileType;
		return this;
	}

	public String getCharset() {
		return charset;
	}

	public EditFilesOptions setCharset(String charset) {
		this.charset = charset;
		return this;
	}
}
