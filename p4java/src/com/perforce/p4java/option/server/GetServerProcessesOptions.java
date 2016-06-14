/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for Perforce IOptionsServer.getServerProcesses method.<p>
 * 
 * See 'p4 help monitor' for help specifying the monitor 'show' flags.
 */
public class GetServerProcessesOptions extends Options {
	
	/**
	 * Options: -a, -e, -l, -s R/T/P/I
	 */
	public static final String OPTIONS_SPECS = "b:a b:e b:l s:s";
	
	/**
	 * If true, includes the command args. Corresponds to the -a flag.
	 */
	protected boolean includeCmdArgs = false;

	/**
	 * If true, includes the command environment. For each process, client
	 * application (if known), host address and client name are displayed.
	 * Corresponds to the -e flag.
	 */
	protected boolean includeCmdEnv = false;

	/**
	 * If true, displays long output, including the full username and argument
	 * list. Corresponds to the -l flag.
	 */
	protected boolean longOutput = false;

	/**
	 * If non-null, restricts the display to processes in the indicated state:
	 * [R]unning, [T]erminated, [P]aused, or [I]dle. Corresponds to -s R/T/P/I
	 * flag.
	 */
	protected String processState = null;

	/**
	 * Default constructor.
	 */
	public GetServerProcessesOptions() {
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
	public GetServerProcessesOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit value constructor.
	 */
	public GetServerProcessesOptions(boolean includeCmdArgs, boolean includeCmdEnv,
			boolean longOutput, String processState) {
		super();
		this.includeCmdArgs = includeCmdArgs;
		this.includeCmdEnv = includeCmdEnv;
		this.longOutput = longOutput;
		this.processState = processState;
	}
	
	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isIncludeCmdArgs(),
								this.isIncludeCmdEnv(),
								this.isLongOutput(),
								this.getProcessState());

		return this.optionList;
	}

	public boolean isIncludeCmdArgs() {
		return includeCmdArgs;
	}

	public GetServerProcessesOptions setIncludeCmdArgs(boolean includeCmdArgs) {
		this.includeCmdArgs = includeCmdArgs;
		return this;
	}

	public boolean isIncludeCmdEnv() {
		return includeCmdEnv;
	}

	public GetServerProcessesOptions setIncludeCmdEnv(boolean includeCmdEnv) {
		this.includeCmdEnv = includeCmdEnv;
		return this;
	}

	public boolean isLongOutput() {
		return longOutput;
	}

	public GetServerProcessesOptions setLongOutput(boolean longOutput) {
		this.longOutput = longOutput;
		return this;
	}

	public String getProcessState() {
		return processState;
	}

	public GetServerProcessesOptions setProcessState(String processState) {
		this.processState = processState;
		return this;
	}
}
