/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer moveFile method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#moveFile(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.option.server.MoveFileOptions)
 */
public class MoveFileOptions extends Options {
	
	/**
	 * Options: -c[changelist], -n, -f, -t[type], -k
	 */
	public static final String OPTIONS_SPECS = "i:c:clz b:n b:f b:k s:t";
	
	/**
	 * If not IChangelist.UNKNOWN, the files are opened in the numbered
	 * pending changelist instead of the 'default' changelist.
	 * Corresponds to the -c flag.
	 */
	protected int changelistId = IChangelist.UNKNOWN;
	
	/**
	 * If true, don't actually perform the move, just return what would
	 * happen if the move was performed. Corresponds to the -n flag.
	 */
	protected boolean listOnly = false;
	
	/**
	 * If true, force a move to an existing target file;
	 * the file must be synced and not opened.  Note that the originating
	 * source file will no longer be synced to the client. Corresponds
	 * to the -f flag.
	 */
	protected boolean force = false;
	
	/**
	 * if true, bypasses the client file rename. This option can be
	 * used to tell the server that the user has already renamed a file on
	 * the client. The use of this option can confuse the server if you
	 * are wrong about the client's contents. Only works for 2009.2 and later
	 * servers; earlier servers will produce a RequestException if you set
	 * this true. Corresponds to the -k flag.
	 */
	protected boolean noClientMove = false;
	
	/**
	 * If not null, the file is reopened as that filetype. Corresponds
	 * to the -t flag.
	 */
	protected String fileType = null;

	/**
	 * Default constructor.
	 */
	public MoveFileOptions() {
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
	public MoveFileOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public MoveFileOptions(int changelistId, boolean listOnly, boolean force,
			boolean noClientMove, String fileType) {
		super();
		this.changelistId = changelistId;
		this.listOnly = listOnly;
		this.force = force;
		this.noClientMove = noClientMove;
		this.fileType = fileType;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
												this.getChangelistId(),
												this.isListOnly(),
												this.isForce(),
												this.isNoClientMove(),
												this.getFileType());
		return this.optionList;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public MoveFileOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public boolean isListOnly() {
		return listOnly;
	}

	public MoveFileOptions setListOnly(boolean listOnly) {
		this.listOnly = listOnly;
		return this;
	}

	public boolean isForce() {
		return force;
	}

	public MoveFileOptions setForce(boolean force) {
		this.force = force;
		return this;
	}

	public boolean isNoClientMove() {
		return noClientMove;
	}

	public MoveFileOptions setNoClientMove(boolean noClientMove) {
		this.noClientMove = noClientMove;
		return this;
	}

	public String getFileType() {
		return fileType;
	}

	public MoveFileOptions setFileType(String fileType) {
		this.fileType = fileType;
		return this;
	}
}
