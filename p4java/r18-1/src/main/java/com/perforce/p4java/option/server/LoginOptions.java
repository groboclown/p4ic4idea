/**
 * 
 */
package com.perforce.p4java.option.server;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

/**
 * Options subclass for IOptionsServer.login.
 */

public class LoginOptions extends Options {
	
	/**
	 * Options: -a, -p, -h[host]
	 */
	public static final String OPTIONS_SPECS = "b:a b:p s:h b:2";
	
	/**
	 * If true, the ticket is valid on all hosts; corresponds to -a flag.
	 */
	protected boolean allHosts = false;

	/**
	 * If true, don't write the ticket to file; corresponds to -p flag.
	 */
	protected boolean dontWriteTicket = false;

	/**
	 * If not null, causes the server to issue a ticket that is valid on the
	 * specified host (IP address). This flag can only be used when the login
	 * request is for another user. Corresponds to -h flag.
	 */
	protected String host = null;

	protected boolean twoFactor = false;
	
	/**
	 * Default constructor.
	 */
	public LoginOptions() {
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
	public LoginOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 * 
	 * @param allHosts
	 */
	public LoginOptions(boolean allHosts) {
		super();
		this.allHosts = allHosts;
	}

	/**
	 * Explicit-value constructor.
	 * 
	 * @param allHosts
	 * @param dontWriteTicket
	 */
	public LoginOptions(boolean allHosts, boolean dontWriteTicket) {
		super();
		this.allHosts = allHosts;
		this.dontWriteTicket = dontWriteTicket;
	}

	/**
	 * Explicit-value constructor.
	 * 
	 * @param allHosts
	 * @param dontWriteTicket
	 * @param host
	 */
	public LoginOptions(boolean allHosts, boolean dontWriteTicket, String host) {
		super();
		this.allHosts = allHosts;
		this.dontWriteTicket = dontWriteTicket;
		this.host = host;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
											this.allHosts,
											this.dontWriteTicket,
											this.host,
					this.twoFactor);
		return this.optionList;
	}

	public boolean isAllHosts() {
		return allHosts;
	}

	public LoginOptions setAllHosts(boolean allHosts) {
		this.allHosts = allHosts;
		return this;
	}

	public boolean isDontWriteTicket() {
		return dontWriteTicket;
	}

	public LoginOptions setDontWriteTicket(boolean dontWriteTicket) {
		this.dontWriteTicket = dontWriteTicket;
		return this;
	}

	public String getHost() {
		return host;
	}

	public LoginOptions setHost(String host) {
		this.host = host;
		return this;
	}

	public boolean isTwoFactor() {
		return twoFactor;
	}

	public LoginOptions setTwoFactor(boolean twoFactor) {
		this.twoFactor = twoFactor;
		return this;
	}
}
