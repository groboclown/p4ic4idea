/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer getFileAnnotations method.<p>
 * 
 * Note that this class can take both DiffType args and the string
 * versions (-dw, etc.); mixing them up carelessly probably isn't a
 * good idea.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getFileAnnotations(java.util.List, com.perforce.p4java.option.server.GetFileAnnotationsOptions)
 */
public class GetFileAnnotationsOptions extends Options {
	
	/**
	 * Options: -a, -c, -i, -db, -dw, -dl, -I, -q, -t
	 */
	public static final String OPTIONS_SPECS = "b:a b:c b:i b:db b:dw b:dl b:I b:q b:t";
	
	/**
	 * If true, include both deleted files and lines no longer present
	 * at the head revision; corresponds to the -a flag.
	 */
	protected boolean allResults = false;
	
	/**
	 * If true, annotate with change numbers rather than revision numbers
	 * with each line; correspond to the -c flag.
	 */
	protected boolean useChangeNumbers = false;
	
	/**
	 *  If true, follow branches; corresponds to the -f flag.
	 */
	protected boolean followBranches = false;
	
	/**
	 * If non-null, use the DiffType value to determine whitespace
	 * options.
	 */
	protected DiffType wsOpts = null;
	
	/**
	 * If true, ignore whitespace changes; corresponds to -db.
	 */
	protected boolean ignoreWhitespaceChanges = false;
	
	/**
	 * If true, ignore whitespace; corresponds to -dw.
	 */
	protected boolean ignoreWhitespace = false;
	
	/**
	 * If true, ignore line endisngs; corresponds to -dl.
	 */
	protected boolean ignoreLineEndings = false;
	
	/**
	 * If true, follows all integrations into the file;
	 * corresponds to -I.
	 */
	protected boolean followAllIntegrations = false;

	/**
	 * If true, suppresses the one-line header that is displayed by	default
	 * for each file; corresponds to -q.
	 */
	protected boolean suppressHeader = false;

	/**
	 * If true, forces 'p4 annotate' to display binary files;
	 * corresponds to -t.
	 */
	protected boolean showBinaryContent = false;

	/**
	 * Default constructor.
	 */
	public GetFileAnnotationsOptions() {
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
	public GetFileAnnotationsOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit value constructor.
	 */
	public GetFileAnnotationsOptions(boolean allResults,
			boolean useChangeNumbers, boolean followBranches, DiffType wsOpts) {
		super();
		this.allResults = allResults;
		this.useChangeNumbers = useChangeNumbers;
		this.followBranches = followBranches;
		this.wsOpts = wsOpts;
	}

	/**
	 * Explicit value constructor.
	 */
	public GetFileAnnotationsOptions(boolean allResults,
			boolean useChangeNumbers, boolean followBranches,
			boolean ignoreWhitespaceChanges, boolean ignoreWhitespace,
			boolean ignoreLineEndings) {
		super();
		this.allResults = allResults;
		this.useChangeNumbers = useChangeNumbers;
		this.followBranches = followBranches;
		this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
		this.ignoreWhitespace = ignoreWhitespace;
		this.ignoreLineEndings = ignoreLineEndings;
	}
	
	/**
	 * Explicit value constructor.
	 */
	public GetFileAnnotationsOptions(boolean allResults,
			boolean useChangeNumbers, boolean followBranches,
			boolean ignoreWhitespaceChanges, boolean ignoreWhitespace,
			boolean ignoreLineEndings, boolean followAllIntegrations) {
		super();
		this.allResults = allResults;
		this.useChangeNumbers = useChangeNumbers;
		this.followBranches = followBranches;
		this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
		this.ignoreWhitespace = ignoreWhitespace;
		this.ignoreLineEndings = ignoreLineEndings;
		this.followAllIntegrations = followAllIntegrations;
	}

	/**
	 * If the wsOpts field is non-null, those values will override
	 * the corresponding explicit boolean fields.
	 * 
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		if (wsOpts != null) {
			switch (wsOpts) {
				case IGNORE_WS:
					this.setIgnoreWhitespace(true);
					break;
				case IGNORE_LINE_ENDINGS:
					this.setIgnoreLineEndings(true);
					break;
				case IGNORE_WS_CHANGES:
					this.setIgnoreWhitespaceChanges(true);
					break;
				default:
					break;
			}
		}
		this.optionList = this.processFields(OPTIONS_SPECS,
												this.isAllResults(),
												this.isUseChangeNumbers(),
												this.isFollowBranches(),
												this.isIgnoreWhitespaceChanges(),
												this.isIgnoreWhitespace(),
												this.isIgnoreLineEndings(),
												this.isFollowAllIntegrations(),
												this.isSuppressHeader(),
												this.isShowBinaryContent());
		return this.optionList;
	}

	public boolean isAllResults() {
		return allResults;
	}

	public GetFileAnnotationsOptions setAllResults(boolean allResults) {
		this.allResults = allResults;
		return this;
	}

	public boolean isUseChangeNumbers() {
		return useChangeNumbers;
	}

	public GetFileAnnotationsOptions setUseChangeNumbers(boolean useChangeNumbers) {
		this.useChangeNumbers = useChangeNumbers;
		return this;
	}

	public boolean isFollowBranches() {
		return followBranches;
	}

	public GetFileAnnotationsOptions setFollowBranches(boolean followBranches) {
		this.followBranches = followBranches;
		return this;
	}

	public DiffType getWsOpts() {
		return wsOpts;
	}

	public GetFileAnnotationsOptions setWsOpts(DiffType wsOpts) {
		this.wsOpts = wsOpts;
		return this;
	}

	public boolean isIgnoreWhitespaceChanges() {
		return ignoreWhitespaceChanges;
	}

	public GetFileAnnotationsOptions setIgnoreWhitespaceChanges(boolean ignoreWhitespaceChanges) {
		this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
		return this;
	}

	public boolean isIgnoreWhitespace() {
		return ignoreWhitespace;
	}

	public GetFileAnnotationsOptions setIgnoreWhitespace(boolean ignoreWhitespace) {
		this.ignoreWhitespace = ignoreWhitespace;
		return this;
	}

	public boolean isIgnoreLineEndings() {
		return ignoreLineEndings;
	}

	public GetFileAnnotationsOptions setIgnoreLineEndings(boolean ignoreLineEndings) {
		this.ignoreLineEndings = ignoreLineEndings;
		return this;
	}

	public boolean isFollowAllIntegrations() {
		return followAllIntegrations;
	}

	public GetFileAnnotationsOptions setFollowAllIntegrations(boolean followAllIntegrations) {
		this.followAllIntegrations = followAllIntegrations;
		return this;
	}

	public boolean isSuppressHeader() {
		return suppressHeader;
	}

	public GetFileAnnotationsOptions setSuppressHeader(boolean suppressHeader) {
		this.suppressHeader = suppressHeader;
		return this;
	}

	public boolean isShowBinaryContent() {
		return showBinaryContent;
	}

	public GetFileAnnotationsOptions setShowBinaryContent(boolean showBinaryContent) {
		this.showBinaryContent = showBinaryContent;
		return this;
	}
}
