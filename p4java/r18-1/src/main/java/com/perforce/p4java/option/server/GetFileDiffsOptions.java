/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer getFileDiffs methods.
 */
public class GetFileDiffsOptions extends Options implements DiffsOptions<GetFileDiffsOptions> {

	/**
	 * Options: -d[flags], -Od, -q, -t, -u, -dc[n], -du[n], -S[stream], -P[parentStream]
	 */
	public static final String OPTIONS_SPECS = "b:Od b:q b:t b:u b:dn i:dc:dcn b:ds i:du:dcn b:db b:dw b:dl s:S s:P";
	
	/**
	 * If true, limits output to files that differ. Corresponds to -Od.
	 */
	protected boolean outputDifferFilesOnly = false;

	/**
	 * If true, suppresses the display of the header lines of files whose
	 * content and types are identical and suppresses the actual diff for all files.
	 * Corresponds to the -q flag.
	 */
	protected boolean quiet = false;
	
	/**
	 * If true, diff even files with non-text (binary) types. Corresponds
	 * to the -t flag.
	 */
	protected boolean includeNonTextDiffs = false;
	
	/**
	 * If true, use the GNU diff -u format and displays only files that differ.
	 * See the "-u" option in the main diff2 documentation for an explanation.
	 * Corresponds to the -u flag.
	 */
	boolean gnuDiffs = false;
		
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
	 * where "n" is of course the value of unifiedDiff.
	 */
	protected int unifiedDiff = -1;
	
	/** If true, ignore whitespace changes; corresponds to -db. */
	protected boolean ignoreWhitespaceChanges = false;
	
	/** If true, ignore whitespace; corresponds to -dw. */
	protected boolean ignoreWhitespace = false;
	
	/** If true, ignore line endings; corresponds to -dl. */
	protected boolean ignoreLineEndings = false;
	
    /**
     * If not null, makes 'p4 diff2' use a stream's branch view. The source is
     * the stream itself, and the target is the stream's parent. The '-P' flag
     * can be used to specify a parent stream other than the stream's actual
     * parent.
     */
    protected String stream = null;
    
    /**
     * If non-null, specify a parent stream other than the stream's actual
     * parent. Corresponds to -P flag.
     */
    protected String parentStream = null;

	/**
	 * Default constructor.
	 */
	public GetFileDiffsOptions() {
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
	public GetFileDiffsOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetFileDiffsOptions(boolean quiet, boolean includeNonTextDiffs,
			boolean gnuDiffs, boolean rcsDiffs,
			int diffContext, boolean summaryDiff, int unifiedDiff,
			boolean ignoreWhitespaceChanges, boolean ignoreWhitespace,
			boolean ignoreLineEndings) {
		super();
		this.quiet = quiet;
		this.includeNonTextDiffs = includeNonTextDiffs;
		this.gnuDiffs = gnuDiffs;
		this.rcsDiffs = rcsDiffs;
		this.diffContext = diffContext;
		this.summaryDiff = summaryDiff;
		this.unifiedDiff = unifiedDiff;
		this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
		this.ignoreWhitespace = ignoreWhitespace;
		this.ignoreLineEndings = ignoreLineEndings;
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetFileDiffsOptions(boolean quiet, boolean includeNonTextDiffs,
			boolean gnuDiffs, boolean rcsDiffs,
			int diffContext, boolean summaryDiff, int unifiedDiff,
			boolean ignoreWhitespaceChanges, boolean ignoreWhitespace,
			boolean ignoreLineEndings, String stream, String parentStream) {
		super();
		this.quiet = quiet;
		this.includeNonTextDiffs = includeNonTextDiffs;
		this.gnuDiffs = gnuDiffs;
		this.rcsDiffs = rcsDiffs;
		this.diffContext = diffContext;
		this.summaryDiff = summaryDiff;
		this.unifiedDiff = unifiedDiff;
		this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
		this.ignoreWhitespace = ignoreWhitespace;
		this.ignoreLineEndings = ignoreLineEndings;
		this.stream = stream;
		this.parentStream = stream;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
				this.isOutputDifferFilesOnly(),
				this.isQuiet(),
				this.isIncludeNonTextDiffs(),
				this.isGnuDiffs(),
				this.isRcsDiffs(),
				this.getDiffContext(),
				this.isSummaryDiff(),
				this.isUnifiedDiff(),
				this.isIgnoreWhitespaceChanges(),
				this.isIgnoreWhitespace(),
				this.isIgnoreLineEndings(),
				this.getStream(),
				this.getParentStream());

		return this.optionList;
	}

	public boolean isOutputDifferFilesOnly() {
		return outputDifferFilesOnly;
	}

	public GetFileDiffsOptions setOutputDifferFilesOnly(boolean outputDifferFilesOnly) {
		this.outputDifferFilesOnly = outputDifferFilesOnly;
		return this;
	}

	public boolean isQuiet() {
		return quiet;
	}

	public GetFileDiffsOptions setQuiet(boolean quiet) {
		this.quiet = quiet;
		return this;
	}

	public boolean isIncludeNonTextDiffs() {
		return includeNonTextDiffs;
	}

	public GetFileDiffsOptions setIncludeNonTextDiffs(boolean includeNonTextDiffs) {
		this.includeNonTextDiffs = includeNonTextDiffs;
		return this;
	}

	public boolean isGnuDiffs() {
		return gnuDiffs;
	}

	public GetFileDiffsOptions setGnuDiffs(boolean gnuDiffs) {
		this.gnuDiffs = gnuDiffs;
		return this;
	}

	public boolean isRcsDiffs() {
		return rcsDiffs;
	}

	public GetFileDiffsOptions setRcsDiffs(boolean rcsDiffs) {
		this.rcsDiffs = rcsDiffs;
		return this;
	}

	public int getDiffContext() {
		return diffContext;
	}

	public GetFileDiffsOptions setDiffContext(int diffContext) {
		this.diffContext = diffContext;
		return this;
	}

	public boolean isSummaryDiff() {
		return summaryDiff;
	}

	public GetFileDiffsOptions setSummaryDiff(boolean summaryDiff) {
		this.summaryDiff = summaryDiff;
		return this;
	}

	public int isUnifiedDiff() {
		return unifiedDiff;
	}

	public GetFileDiffsOptions setUnifiedDiff(int unifiedDiff) {
		this.unifiedDiff = unifiedDiff;
		return this;
	}

	public boolean isIgnoreWhitespaceChanges() {
		return ignoreWhitespaceChanges;
	}

	public GetFileDiffsOptions setIgnoreWhitespaceChanges(boolean ignoreWhitespaceChanges) {
		this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
		return this;
	}

	public boolean isIgnoreWhitespace() {
		return ignoreWhitespace;
	}

	public GetFileDiffsOptions setIgnoreWhitespace(boolean ignoreWhitespace) {
		this.ignoreWhitespace = ignoreWhitespace;
		return this;
	}

	public boolean isIgnoreLineEndings() {
		return ignoreLineEndings;
	}

	public GetFileDiffsOptions setIgnoreLineEndings(boolean ignoreLineEndings) {
		this.ignoreLineEndings = ignoreLineEndings;
		return this;
	}

    public String getStream() {
        return stream;
    }

    public GetFileDiffsOptions setStream(String stream) {
        this.stream = stream;
        return this;
	}

	public String getParentStream() {
        return parentStream;
	}

	public GetFileDiffsOptions setParentStream(String parentStream) {
        this.parentStream = parentStream;
        return this;
	}
}
