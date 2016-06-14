/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer.getReviewChangelists method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getReviewChangelists(com.perforce.p4java.option.server.GetReviewChangelistsOptions)
 */
public class GetReviewChangelistsOptions extends Options {
	
	/**
	 * Options: -c[changelist], -t[counter]
	 */
	public static final String OPTIONS_SPECS = "i:c:clz s:t";
	
	/**
	 * If greater than zero, lists changelists that have not been reviewed
	 * before, equal or above the specified changelist#.
	 * Corresponds to -c changelist#.
	 */
	protected int changelistId = IChangelist.UNKNOWN;

	/**
	 * If not null, lists changelists that have not been reviewed before, above
	 * the specified counter's changelist#.<p>
	 * 
	 * Note, if both the 'changelistId' and 'counter' options are specified, the
	 * 'p4 review' sets the counter to that changelist# and produces no output.
	 * This functionality has been superceded by the 'p4 counter' command.
	 * Corresponds to -t counter.
	 */
	protected String counter = null;
	
	/**
	 * Default constructor.
	 */
	public GetReviewChangelistsOptions() {
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
	public GetReviewChangelistsOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetReviewChangelistsOptions(int changelistId, String counter) {
		super();
		this.changelistId = changelistId;
		this.counter = counter;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
				this.getChangelistId(),
				this.getCounter());
		return this.optionList;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public GetReviewChangelistsOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public String getCounter() {
		return counter;
	}

	public GetReviewChangelistsOptions setCounter(String counter) {
		this.counter = counter;
		return this;
	}
}
