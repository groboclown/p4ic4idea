package com.perforce.p4java.option.server;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

public class Login2Options extends Options {

	/**
	 * Options:
	 *     p4 login2 [ -p -R ] [ -h host ] [ -S state ] [ -m method ] [ username ]
	 *     p4 login2 -s [ -a | -h host ] [ username ]
	 */
	public static final String OPTIONS_SPECS = "b:p b:R s:h s:S s:m b:s b:a";

	/**
	 * If true, the second factor authorization persists even after the ticket has expired
	 */
	protected boolean persist = false;

	/**
	 * If true, the second factor authentication can be restarted, allowing users to re-request a OTP, etc
	 */
	protected boolean restart = false;

	/**
	 * If not null, causes the server to issue a ticket that is valid on the
	 * specified host (IP address). This flag can only be used when the login
	 * request is for another user. Corresponds to -h flag.
	 */
	protected String host = null;

	/**
	 * For non-interactive clients, the -S flag can be used to execute each
	 * step of the second factor authentication individually.
	 */
	protected String state = null;

	/**
	 * If not null, the chosen method provided to the -m flag
	 */
	protected String method = null;

	/**
	 * If true, displays the second factor authorization status for the user
	 */
	protected boolean status = false;

	/**
	 * If true, the ticket is valid on all hosts; corresponds to -a flag.
	 */
	protected boolean allHosts = false;

	/**
	 * Default constructor.
	 */
	public Login2Options() {
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
	public Login2Options(String... options) {
		super(options);
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
				this.persist,
				this.restart,
				this.host,
				this.state,
				this.method,
				this.status,
				this.allHosts);
		return this.optionList;
	}



	public boolean isPersist() {
		return persist;
	}

	public Login2Options setPersist(boolean persist) {
		this.persist = persist;
		return this;
	}

	public boolean isRestart() {
		return restart;
	}

	public Login2Options setRestart(boolean restart) {
		this.restart = restart;
		return this;
	}

	public String getHost() {
		return host;
	}

	public Login2Options setHost(String host) {
		this.host = host;
		return this;
	}

	public String getState() {
		return state;
	}

	public Login2Options setState(String state) {
		this.state = state;
		return this;
	}

	public String getMethod() {
		return host;
	}

	public Login2Options setMethod(String method) {
		this.method = method;
		return this;
	}

	public boolean isStatus() {
		return status;
	}

	public Login2Options setStatus(boolean status) {
		this.status = status;
		return this;
	}

	public boolean isAllHosts() {
		return allHosts;
	}

	public Login2Options setAllHosts(boolean allHosts) {
		this.allHosts = allHosts;
		return this;
	}
}