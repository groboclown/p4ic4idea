package com.perforce.p4java.option.server;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

/**
 * Options required by the repos command
 *
 * -e|-E filter
 * -m max
 * -u user
 * -O owner
 * --from=origin
 */
public class ReposOptions extends Options {

	public static final String OPTIONS_SPECS = "i:m:gtz s:u s:O s:e";

	/**
	 * If non-null, limits output to repos whose name matches
	 * the nameFilter pattern. Corresponds to -enameFilter flag
	 */
	protected String nameFilter = null;

	/**
	 * If greater than zero, limit output to the first maxResults
	 * number of repos. Corresponds to -m flag.
	 */
	protected int maxResults = 0;

	/**
	 * If non-null, limit qualifying repos to those owned by the named user.
	 * Corresponds to -u name flag.
	 */
	protected String user = null;

	/**
	 * If non-null, limit qualifying repos to those owned by the named owner.
	 * Corresponds to -O name flag.
	 */
	protected String owner = null;

	/**
	 * Default constructor; sets all fields to null, zero, or false.
	 */
	public ReposOptions() {
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
	public ReposOptions(String... options) {
		super(options);
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
				this.getMaxResults(),
				this.getUser(),
				this.getOwner(),
				this.getNameFilter());
		return this.optionList;
	}

	public String getNameFilter() {
		return nameFilter;
	}

	public ReposOptions setNameFilter(String nameFilter) {
		this.nameFilter = nameFilter;
		return this;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public ReposOptions setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	public String getUser() {
		return user;
	}

	public ReposOptions setUser(String user) {
		this.user = user;
		return this;
	}

	public String getOwner() {
		return owner;
	}

	public ReposOptions setOwner(String owner) {
		this.owner = owner;
		return this;
	}

}
