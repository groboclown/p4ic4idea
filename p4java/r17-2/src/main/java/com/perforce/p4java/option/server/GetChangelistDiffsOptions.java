/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer.getChangelistDiffs method(s).
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getChangelistDiffs(int, com.perforce.p4java.option.server.GetChangelistDiffsOptions)
 */
public class GetChangelistDiffsOptions extends Options implements DiffsOptions<GetChangelistDiffsOptions> {
	
	/**
	 * Options: "-S", "-dn", "-dc[n]", "-ds", "-du[n]" "-db", "-dw", "-dl"
	 */
	public static final String OPTIONS_SPECS = "b:S b:dn i:dc:dcn b:ds i:du:dcn b:db b:dw b:dl";
	
	/** If true, output diffs of shelved files for the changelist */
	protected boolean outputShelvedDiffs = false;
	
	/** If true, use RCS diff; corresponds to -dn. */
	protected boolean rcsDiffs = false;
	
	/**
	 * If positive, specifies the number of context diff lines;
	 * if zero, lets server pick context number; if negative,
	 * no options are generated. Corresponds to -dc[n], with -dc
	 * generated for diffContext == 0, -dcn for diffContext > 0,
	 * where "n" is of course the value of diffContext.
	 */
	protected int diffContext = -1;
	
	/** If true, perform summary diff; corresponds to -ds. */
	protected boolean summaryDiff = false;
	
	/** If true, do a unified diff; corresponds to -du[n] with -du
	 * generated for unifiedDiff == 0, -dun for unifiedDiff > 0,
	 * where "n" is of course the value of unifiedDiff. */
	protected int unifiedDiff = -1;
	
	/** If true, ignore whitespace changes; corresponds to -db. */
	protected boolean ignoreWhitespaceChanges = false;
	
	/** If true, ignore whitespace; corresponds to -dw. */
	protected boolean ignoreWhitespace = false;
	
	/** If true, ignore line endings; corresponds to -dl. */
	protected boolean ignoreLineEndings = false;

	/**
	 * Default constructor.
	 */
	public GetChangelistDiffsOptions() {
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
	public GetChangelistDiffsOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetChangelistDiffsOptions(boolean outputShelvedDiffs,
			boolean rcsDiffs, int diffContext, boolean summaryDiff,
			int unifiedDiff, boolean ignoreWhitespaceChanges,
			boolean ignoreWhitespace, boolean ignoreLineEndings) {
		super();
		this.outputShelvedDiffs = outputShelvedDiffs;
		this.rcsDiffs = rcsDiffs;
		this.diffContext = diffContext;
		this.summaryDiff = summaryDiff;
		this.unifiedDiff = unifiedDiff;
		this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
		this.ignoreWhitespace = ignoreWhitespace;
		this.ignoreLineEndings = ignoreLineEndings;
	}

	/** 
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isOutputShelvedDiffs(),
								this.isRcsDiffs(),
								this.getDiffContext(),
								this.isSummaryDiff(),
								this.isUnifiedDiff(),
								this.isIgnoreWhitespaceChanges(),
								this.isIgnoreWhitespace(),
								this.isIgnoreLineEndings());
		
		return this.optionList;
	}

	public boolean isOutputShelvedDiffs() {
		return outputShelvedDiffs;
	}

	public GetChangelistDiffsOptions setOutputShelvedDiffs(boolean outputShelvedDiffs) {
		this.outputShelvedDiffs = outputShelvedDiffs;
		return this;
	}

	public boolean isRcsDiffs() {
		return rcsDiffs;
	}

	public GetChangelistDiffsOptions setRcsDiffs(boolean rcsDiffs) {
		this.rcsDiffs = rcsDiffs;
		return this;
	}

	public int getDiffContext() {
		return diffContext;
	}

	public GetChangelistDiffsOptions setDiffContext(int diffContext) {
		this.diffContext = diffContext;
		return this;
	}

	public boolean isSummaryDiff() {
		return summaryDiff;
	}

	public GetChangelistDiffsOptions setSummaryDiff(boolean summaryDiff) {
		this.summaryDiff = summaryDiff;
		return this;
	}

	public int isUnifiedDiff() {
		return unifiedDiff;
	}

	public GetChangelistDiffsOptions setUnifiedDiff(int unifiedDiff) {
		this.unifiedDiff = unifiedDiff;
		return this;
	}

	public boolean isIgnoreWhitespaceChanges() {
		return ignoreWhitespaceChanges;
	}

	public GetChangelistDiffsOptions setIgnoreWhitespaceChanges(boolean ignoreWhitespaceChanges) {
		this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
		return this;
	}

	public boolean isIgnoreWhitespace() {
		return ignoreWhitespace;
	}

	public GetChangelistDiffsOptions setIgnoreWhitespace(boolean ignoreWhitespace) {
		this.ignoreWhitespace = ignoreWhitespace;
		return this;
	}

	public boolean isIgnoreLineEndings() {
		return ignoreLineEndings;
	}

	public GetChangelistDiffsOptions setIgnoreLineEndings(boolean ignoreLineEndings) {
		this.ignoreLineEndings = ignoreLineEndings;
		return this;
	}
}
