/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the Perforce IOptionsServer.addTrust method.
 */
public class TrustOptions extends Options {
	
	/** Options: -f, -n, -y, -r. */
	public static final String OPTIONS_SPECS = "b:f b:n b:y b:r";

	/**
	 * Forcibly install fingerprint even if differs. Corresponds to the -f flag.
	 */
	protected boolean force = false;

	/**
	 * Like no option but prompting automatically refused. Corresponds to the -n flag.
	 */
	protected boolean autoRefuse = false;
	
	/**
	 * Like no option but prompting automatically accepted. Corresponds to the -y flag.
	 */
	protected boolean autoAccept = false;

	/**
	 * Specifies that a replacement fingerprint is to be affected. Corresponds to the -r flag.
	 */
	protected boolean replacement = false;

	/**
	 * Default constructor.
	 */
	public TrustOptions() {
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
	public TrustOptions(String... options) {
		super(options);
	}
	
	/**
	 * Creates a new TrustOptions object, given the 'force', 'autoRefuse' and
	 * 'autoAccept' flags.
	 *
	 * @param force true/false to forcibly install fingerprint
	 * @param autoRefuse true/false to automatically refuse yes/no prompting
	 * @param autoAccept true/false to automatically accept yes/no prompting
	 */
	public TrustOptions(boolean force, boolean autoRefuse, boolean autoAccept) {
		super();
		this.force = force;
		this.autoRefuse = autoRefuse;
		this.autoAccept = autoAccept;
	}
	
	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isForce(),
								this.isAutoRefuse(),
								this.isAutoAccept(),
								this.isReplacement());

		return this.optionList;
	}

	/**
	 * Checks if is true/false to forcibly install fingerprint.
	 *
	 * @return true, if is to forcibly install fingerprint
	 */
	public boolean isForce() {
		return force;
	}

	/**
	 * Sets true/false to forcibly install fingerprint.
	 *
	 * @param force true/false to forcibly install fingerprint.
	 * @return the trust options
	 */
	public TrustOptions setForce(boolean force) {
		this.force = force;
		return this;
	}

	/**
	 * Checks if is true/false to automatically refuse yes/no prompting.
	 *
	 * @return true, if is to automatically refuse yes/no prompting
	 */
	public boolean isAutoRefuse() {
		return autoRefuse;
	}

	/**
	 * Sets true/false to automatically refuse yes/no prompting.
	 *
	 * @param autoRefuse true/false to automatically refuse yes/no prompting
	 * @return the trust options
	 */
	public TrustOptions setAutoRefuse(boolean autoRefuse) {
		this.autoRefuse = autoRefuse;
		return this;
	}

	/**
	 * Checks if is to automatically accept yes/no prompting.
	 *
	 * @return true, if is to automatically accept yes/no prompting
	 */
	public boolean isAutoAccept() {
		return autoAccept;
	}

	/**
	 * Sets true/false to automatically accept yes/no prompting.
	 *
	 * @param autoAccept true/false to automatically accept yes/no prompting
	 * @return the trust options
	 */
	public TrustOptions setAutoAccept(boolean autoAccept) {
		this.autoAccept = autoAccept;
		return this;
	}

	/**
	 * Checks if a replacement fingerprint is to be affected.
	 *
	 * @return true, if a replacement fingerprint is to be affected
	 */
	public boolean isReplacement() {
		return replacement;
	}

	/**
	 * Sets true/false that a replacement fingerprint is to be affected
	 *
	 * @param replacement true/false that a replacement fingerprint is to be affected.
	 * @return the trust options
	 */
	public TrustOptions setReplacement(boolean replacement) {
		this.replacement = replacement;
		return this;
	}
}