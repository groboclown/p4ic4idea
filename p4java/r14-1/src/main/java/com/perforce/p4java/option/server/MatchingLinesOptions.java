/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options objects for the IOptionsServer getMatchingLines command. Note that
 * not all "p4 grep" options are currently implemented or recognized; see the
 * comments below for a list of recognized and non-recognized options and their
 * semantics.
 * 
 * <pre>
 *  The -a flag searches all revisions within the specific range, rather
 * 	than just the highest revision in the range.
 * 
 * 	The -i flag causes the pattern matching to be case insensitive, by
 * 	default matching is case sensitive.
 * 
 * 	The -n flag displays the matching line number after the file revision
 * 	number,  by default output of matched files consist of the revision
 * 	and the matched line separated by a colon ':'.
 * 
 * 	The -v flag displays files with non-matching lines.
 * 
 * 	The -F flag is used to interpret the pattern as a fixed string.
 * 
 * 	The -G flag is used to interpret the pattern as a regular expression,
 * 	the default behavior.
 * 
 * 	The -t flag instructs grep to treat binary files as text.  By default
 * 	only files of type text are selected for pattern matching.
 * 
 * 	The -A <num> flag displays num lines of trailing context after
 * 	matching lines.
 * 
 * 	The -B <num> flag displays num lines of leading context before
 * 	matching lines.
 * 
 * 	The -C <num> flag displays num lines of output context.
 * 	
 * 	The -s flag suppresses error messages that result from abandoning
 * 	files that have a maximum number of characters in a single line that
 * 	are greater than 4096.  By default grep will abandon these files and
 * 	report an error.
 * 	
 * 	The -L flag changes the output to display the name of each selected
 * 	file from which no output would normally have been displayed. The
 * 	scanning will stop on the first match.
 * 
 * 	The -l flag changes the output to display the name of each selected
 * 	file from which output would normally have been displayed. The
 * 	scanning will stop on the first match.
 * </pre>
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getMatchingLines(java.util.List, java.lang.String, com.perforce.p4java.option.server.MatchingLinesOptions)
 */
public class MatchingLinesOptions extends Options {
	
	/**
	 * Options: -a, -i, -n, -v, -A[n], -B[n], -C[n], -t, -F|-G
	 */
	public static final String OPTIONS_SPECS = "b:a b:i b:n b:v i:A:gtz i:B:gtz i:C:gtz b:t b:F";

	/** Corresponds to the p4 grep "-a" option */
	protected boolean allRevisions = false;
	
	/** Corresponds to the p4 grep -i option */
	protected boolean caseInsensitive = false;
	
	/** Corresponds to the p4 grep -n option */
	protected boolean includeLineNumbers = false;
	
	/** Corresponds to the p4 grep -v option */
	protected boolean nonMatchingLines = false;
	
	/** Corresponds to the p4 grep -t option */
	protected boolean searchBinaries = false;
	
	/** Corresponds to the p4 grep -C option; if zero, option is off */
	protected int outputContext = 0;
	
	/** Corresponds to the p4 grep -A option; if zero, option is off */
	protected int trailingContext = 0;
	
	/** Corresponds to the p4 grep -B option; if zero, option is off */
	protected int leadingContext = 0;
	
	/** Corresponds to the p4 grep -F and -G options: if true, corresponds to -F;
	 * if false, to -G */
	protected boolean fixedPattern = false;

	/**
	 * Default constructor.
	 */
	public MatchingLinesOptions() {
		super();
	}

	/**
	 * Explicit value constructor.
	 */
	public MatchingLinesOptions(boolean allRevisions,
			boolean caseInsensitive, boolean includeLineNumbers,
			boolean nonMatchingLines, boolean searchBinaries,
			int outputContext, int trailingContext, int leadingContext,
			boolean fixedPattern) {
		super();
		this.allRevisions = allRevisions;
		this.caseInsensitive = caseInsensitive;
		this.includeLineNumbers = includeLineNumbers;
		this.nonMatchingLines = nonMatchingLines;
		this.searchBinaries = searchBinaries;
		this.outputContext = outputContext;
		this.trailingContext = trailingContext;
		this.leadingContext = leadingContext;
		this.fixedPattern = fixedPattern;
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
	public MatchingLinesOptions(String ... options) {
		super(options);
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.allRevisions,
								this.caseInsensitive,
								this.includeLineNumbers,
								this.nonMatchingLines,
								this.trailingContext,
								this.leadingContext,
								this.outputContext,
								this.searchBinaries,
								this.fixedPattern);
		return this.optionList;
	}

	public boolean isAllRevisions() {
		return allRevisions;
	}

	public MatchingLinesOptions setAllRevisions(boolean allRevisions) {
		this.allRevisions = allRevisions;
		return this;
	}

	public boolean isCaseInsensitive() {
		return caseInsensitive;
	}

	public MatchingLinesOptions setCaseInsensitive(boolean caseInsensitive) {
		this.caseInsensitive = caseInsensitive;
		return this;
	}

	public boolean isIncludeLineNumbers() {
		return includeLineNumbers;
	}

	public MatchingLinesOptions setIncludeLineNumbers(boolean includeLineNumbers) {
		this.includeLineNumbers = includeLineNumbers;
		return this;
	}

	public boolean isNonMatchingLines() {
		return nonMatchingLines;
	}

	public MatchingLinesOptions setNonMatchingLines(boolean nonMatchingLines) {
		this.nonMatchingLines = nonMatchingLines;
		return this;
	}

	public boolean isSearchBinaries() {
		return searchBinaries;
	}

	public MatchingLinesOptions setSearchBinaries(boolean searchBinaries) {
		this.searchBinaries = searchBinaries;
		return this;
	}

	public int getOutputContext() {
		return outputContext;
	}

	public MatchingLinesOptions setOutputContext(int outputContext) {
		this.outputContext = outputContext;
		return this;
	}

	public int getTrailingContext() {
		return trailingContext;
	}

	public MatchingLinesOptions setTrailingContext(int trailingContext) {
		this.trailingContext = trailingContext;
		return this;
	}

	public int getLeadingContext() {
		return leadingContext;
	}

	public MatchingLinesOptions setLeadingContext(int leadingContext) {
		this.leadingContext = leadingContext;
		return this;
	}

	public boolean isFixedPattern() {
		return fixedPattern;
	}

	public MatchingLinesOptions setFixedPattern(boolean fixedPattern) {
		this.fixedPattern = fixedPattern;
		return this;
	}
}
