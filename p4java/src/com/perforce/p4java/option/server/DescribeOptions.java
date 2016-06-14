/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the different options that can be specified when running a
 * describe on a changelist
 */
public class DescribeOptions extends Options {
	
	/**
	 * Options:
	 */
	public static final String OPTIONS_SPECS = null;

	private boolean outputShelvedDiffs = false;
	private DiffType type = null;

	/**
	 * Default constructor.
	 */
	public DescribeOptions() {
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
	public DescribeOptions(String... options) {
		super(options);
	}

	/**
	 * Create a changelist describe options
	 * 
	 * @param type
	 */
	public DescribeOptions(DiffType type) {
		this(type, false);
	}

	/**
	 * Create a changelist describe options
	 * 
	 * @param type
	 * @param outputShelvedDiffs
	 */
	public DescribeOptions(DiffType type, boolean outputShelvedDiffs) {
		this.type = type;
		this.outputShelvedDiffs = outputShelvedDiffs;
	}

	/**
	 * @return the type
	 */
	public DiffType getType() {
		return this.type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public DescribeOptions setType(DiffType type) {
		this.type = type;
		return this;
	}

	/**
	 * @return the showShelvedDiffs
	 */
	public boolean isOutputShelvedDiffs() {
		return this.outputShelvedDiffs;
	}

	/**
	 * @param showShelvedDiffs
	 *            the showShelvedDiffs to set
	 */
	public DescribeOptions setOutputShelvedDiffs(boolean showShelvedDiffs) {
		this.outputShelvedDiffs = showShelvedDiffs;
		return this;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		throw new P4JavaError("Unimplemented method");
	}
}
