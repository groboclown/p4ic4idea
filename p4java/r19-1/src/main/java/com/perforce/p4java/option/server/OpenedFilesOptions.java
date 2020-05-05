/**
 * 
 */
package com.perforce.p4java.option.server;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

/**
 * Options-based method options for IServer and IClient openedFiles method(s).
 * <p>
 * 
 * Note that when used with the IClient openedFiles method, the
 * allClients and clientName options are ignored; the clientName
 * is (of course) filled in using the client's own name.<p>
 * 
 * @see com.perforce.p4java.client.IClient#openedFiles(java.util.List, com.perforce.p4java.option.server.OpenedFilesOptions)
 */
public class OpenedFilesOptions extends Options {
	
	/**
	 * Options: -a, -c[changelist], -C[client], -u[user], -m[max], -s
	 */
	public static final String OPTIONS_SPECS = "b:a i:c:cl s:C s:u i:m:gtz b:s";
	
	/**
	 * If true, list opened files in all clients.
	 * Normally only files opened by the current client are listed.
	 */
	protected boolean allClients = false;
	
	/**
	 * If not null, restrict the list of files to those opened on the named client.
	 */
	protected String clientName = null;
	
	/**
	 * If positive, limit output to the first 'maxFiles' number of files.
	 */
	protected int maxFiles = 0;
	
	/**
	 * If not null, restrict the list of files to those opened by the named user.
	 */
	protected String userName = null;
	
	/**
	 * If non-negative, restrict the list to files opened under
	 * the given changelist#.  Normally files in any changelist (including
	 * the default) are listed.
	 */
	protected int changelistId = IChangelist.UNKNOWN;

	/**
	 * If true, produce 'short' (no revision number or file type) and optimized
	 * output when used with the -a (all clients) option. For large repositories
	 * '-a' can take a long time when compared to '-a -s' options.
	 */
	protected boolean shortOutput = false;

	/**
	 * Default constructor.
	 */
	public OpenedFilesOptions() {
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
	public OpenedFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public OpenedFilesOptions(boolean allClients, String clientName,
			int maxFiles, String userName, int changelistId) {
		super();
		this.allClients = allClients;
		this.clientName = clientName;
		this.maxFiles = maxFiles;
		this.userName = userName;
		this.changelistId = changelistId;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
									this.allClients,
									this.changelistId,
									this.clientName,
									this.userName,
									this.maxFiles,
									this.shortOutput
								);
		return this.optionList;
	}

	public boolean isAllClients() {
		return allClients;
	}

	public OpenedFilesOptions setAllClients(boolean allClients) {
		this.allClients = allClients;
		return this;
	}

	public String getClientName() {
		return clientName;
	}

	public OpenedFilesOptions setClientName(String clientName) {
		this.clientName = clientName;
		return this;
	}

	public int getMaxFiles() {
		return maxFiles;
	}

	public OpenedFilesOptions setMaxFiles(int maxFiles) {
		this.maxFiles = maxFiles;
		return this;
	}

	public String getUserName() {
		return userName;
	}

	public OpenedFilesOptions setUserName(String userName) {
		this.userName = userName;
		return this;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public OpenedFilesOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public boolean isShortOutput() {
		return shortOutput;
	}

	public OpenedFilesOptions setShortOutput(boolean shortOutput) {
		this.shortOutput = shortOutput;
		return this;
	}
}
