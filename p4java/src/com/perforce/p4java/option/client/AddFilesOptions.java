/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * IClient addFiles method Options definitions.
 * 
 * @see com.perforce.p4java.client.IClient#addFiles(java.util.List, com.perforce.p4java.option.client.AddFilesOptions) 
 */
public class AddFilesOptions extends Options {
	
	/**
	 * Options: -n, -c[changelist], -t[filetype], -f, -I, -Q[charset]
	 */
	public static final String OPTIONS_SPECS = "b:n i:c:gtz s:t b:f b:I s:Q";
	
	/**
	 * If true, don't actually do the add, just return the files that
	 * would have been opened for addition.
	 */
	protected boolean noUpdate = false;
	
	/**
	 * If positive, the opened files are put into the pending
	 * changelist identified by changelistId (this changelist must have been
	 * previously created for this to succeed). If zero or negative, the
	 * file is opened in the 'default' (unnumbered) changelist.
	 */
	protected int changelistId = 0;
	
	/**
	 * If non-null, the files are added as that filetype.
	 * See 'p4 help filetypes' to attempt to make any sense of Perforce file types.
	 */
	protected String fileType = null;
	
	/**
	 * If true, filenames that contain wildcards are permitted.
	 * See the main Perforce documentation for file adding for details.
	 */
	protected boolean useWildcards = false;

	/**
	 * If true, informs the client that it should not perform any ignore checking.
	 */
	protected boolean noIgnoreChecking = false;
	
	/**
	 * If non-null, the files are added using that charset; corresponds to the
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
	public AddFilesOptions() {
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
	public AddFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public AddFilesOptions(boolean noUpdate, int changelistId, String fileType,
			boolean useWildcards) {
		super();
		this.noUpdate = noUpdate;
		this.changelistId = changelistId;
		this.fileType = fileType;
		this.useWildcards = useWildcards;
	}

	/**
	 * Explicit-value constructor.
	 */
	public AddFilesOptions(boolean noUpdate, int changelistId, String fileType,
			boolean useWildcards, boolean noIgnoreChecking) {
		super();
		this.noUpdate = noUpdate;
		this.changelistId = changelistId;
		this.fileType = fileType;
		this.useWildcards = useWildcards;
		this.noIgnoreChecking = noIgnoreChecking;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.noUpdate,
								this.changelistId,
								this.fileType,
								this.useWildcards,
								this.noIgnoreChecking,
								this.charset);
		return this.optionList;
	}

	public boolean isNoUpdate() {
		return noUpdate;
	}

	public AddFilesOptions setNoUpdate(boolean noUpdate) {
		this.noUpdate = noUpdate;
		return this;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public AddFilesOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public String getFileType() {
		return fileType;
	}

	public AddFilesOptions setFileType(String fileType) {
		this.fileType = fileType;
		return this;
	}

	public boolean isUseWildcards() {
		return useWildcards;
	}

	public AddFilesOptions setUseWildcards(boolean useWildcards) {
		this.useWildcards = useWildcards;
		return this;
	}

	public boolean isNoIgnoreChecking() {
		return noIgnoreChecking;
	}
	
	public AddFilesOptions setNoIgnoreChecking(boolean noIgnoreChecking) {
		this.noIgnoreChecking = noIgnoreChecking;
		return this;
	}

	public String getCharset() {
		return charset;
	}

	public AddFilesOptions setCharset(String charset) {
		this.charset = charset;
		return this;
	}
}
