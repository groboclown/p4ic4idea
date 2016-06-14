/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer getExtendedFiles (a.k.a. "fstat") method.<p>
 * 
 * The various options are too complex to be described in a few sentences here, and
 * the various option arguments reflect this complexity. Note also that several options
 * below (e.g. the "-F" equivalent) may only be available for later-model servers;
 * use of these with earlier servers will typically cause RequestExceptions from the
 * server (at least). Note further that no attempt is made with the default implementation
 * here to sanity check the complex set of options for consistency.<p>
 * 
 * Please consult the main p4 documentation for detailed options
 * semantics and option / server compatibility details.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getExtendedFiles(java.util.List, com.perforce.p4java.option.server.GetExtendedFilesOptions)
 */
public class GetExtendedFilesOptions extends Options {
	
	/**
	 * Options: -F[filter], -m[max], -r, -c, -e[changelist], -O[x], -R[x], -S[x], -A[pattern]
	 * <p>
	 * 
	 * NOTE: the -O[x] and -R[x] options are not processed using the OPTIONS_SPEC string.
	 */
	public static final String OPTIONS_SPECS = "s:F i:m:gtz b:r i:c:cl i:e:cl b:St b:Sd b:Sr b:Sh b:Ss s:A";
	
	/** -F */
	protected String filterString = null;
	
	/** -m */
	protected int maxResults = 0;
	
	/** -r */
	protected boolean reverseSort = false;
	
	/** -c */
	protected int sinceChangelist = IChangelist.UNKNOWN;
	
	/** -e */
	protected int affectedByChangelist = IChangelist.UNKNOWN;
	
	/** -St */
	protected boolean sortByFiletype = false;
	
	/** -Sd */
	protected boolean sortByDate = false;
	
	/** -Sr */
	protected boolean sortByHeadRev = false;
	
	/** -Sh */
	protected boolean sortByHaveRev = false;
	
	/** -Ss */
	protected boolean sortByFileSize = false;
	
	/** -Rx options */
	protected FileStatOutputOptions outputOptions = null;
	
	/** -Ox options */
	protected FileStatAncilliaryOptions ancilliaryOptions = null;
	
	/**
	 * fstat -A pattern (unsupported -- see 'p4 undoc' fstat entry).
	 */
	protected String attributePattern = null;
	
	/**
	 * Default constructor.
	 */
	public GetExtendedFilesOptions() {
		super();	}

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
	public GetExtendedFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor (release 2010.1).
	 */
	public GetExtendedFilesOptions(String filterString, int maxResults,
			boolean reverseSort, int sinceChangelist, int affectedByChangelist,
			boolean sortByFiletype, boolean sortByDate, boolean sortByHeadRev,
			boolean sortByHaveRev, boolean sortByFileSize,
			FileStatOutputOptions outputOptions,
			FileStatAncilliaryOptions ancilliaryOptions) {
		super();
		this.filterString = filterString;
		this.maxResults = maxResults;
		this.reverseSort = reverseSort;
		this.sinceChangelist = sinceChangelist;
		this.affectedByChangelist = affectedByChangelist;
		this.sortByFiletype = sortByFiletype;
		this.sortByDate = sortByDate;
		this.sortByHeadRev = sortByHeadRev;
		this.sortByHaveRev = sortByHaveRev;
		this.sortByFileSize = sortByFileSize;
		this.outputOptions = outputOptions;
		this.ancilliaryOptions = ancilliaryOptions;
	}
	
	/**
	 * Explicit-value constructor.
	 * 
	 * @since 2011.1
	 */

	public GetExtendedFilesOptions(String filterString, int maxResults,
			boolean reverseSort, int sinceChangelist, int affectedByChangelist,
			boolean sortByFiletype, boolean sortByDate, boolean sortByHeadRev,
			boolean sortByHaveRev, boolean sortByFileSize,
			FileStatOutputOptions outputOptions,
			FileStatAncilliaryOptions ancilliaryOptions, String attributePattern) {
		super();
		this.filterString = filterString;
		this.maxResults = maxResults;
		this.reverseSort = reverseSort;
		this.sinceChangelist = sinceChangelist;
		this.affectedByChangelist = affectedByChangelist;
		this.sortByFiletype = sortByFiletype;
		this.sortByDate = sortByDate;
		this.sortByHeadRev = sortByHeadRev;
		this.sortByHaveRev = sortByHaveRev;
		this.sortByFileSize = sortByFileSize;
		this.outputOptions = outputOptions;
		this.ancilliaryOptions = ancilliaryOptions;
		this.attributePattern = attributePattern;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
										this.getFilterString(),
										this.getMaxResults(),
										this.isReverseSort(),
										this.getSinceChangelist(),
										this.getAffectedByChangelist(),
										this.isSortByFiletype(),
										this.isSortByDate(),
										this.isSortByHeadRev(),
										this.isSortByHaveRev(),
										this.isSortByFileSize(),
										this.getAttributePattern());
		if (this.getOutputOptions() != null) {
			List<String> strs = this.getOutputOptions().toStrings();
			if ((strs != null) && (strs.size() > 0)) {
				this.optionList.addAll(this.getOutputOptions().toStrings());
			}
		}
		if (this.getAncilliaryOptions() != null) {
			List<String> strs = this.getAncilliaryOptions().toStrings();
			if ((strs != null) && (strs.size() > 0)) {
				this.optionList.addAll(this.getAncilliaryOptions().toStrings());
			}
		}
		return this.optionList;
	}

	public String getFilterString() {
		return filterString;
	}

	public GetExtendedFilesOptions setFilterString(String filterString) {
		this.filterString = filterString;
		return this;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public GetExtendedFilesOptions setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	public boolean isReverseSort() {
		return reverseSort;
	}

	public GetExtendedFilesOptions setReverseSort(boolean reverseSort) {
		this.reverseSort = reverseSort;
		return this;
	}

	public int getSinceChangelist() {
		return sinceChangelist;
	}

	public GetExtendedFilesOptions setSinceChangelist(int sinceChangelist) {
		this.sinceChangelist = sinceChangelist;
		return this;
	}

	public int getAffectedByChangelist() {
		return affectedByChangelist;
	}

	public GetExtendedFilesOptions setAffectedByChangelist(int affectedByChangelist) {
		this.affectedByChangelist = affectedByChangelist;
		return this;
	}

	public boolean isSortByFiletype() {
		return sortByFiletype;
	}

	public GetExtendedFilesOptions setSortByFiletype(boolean sortByFiletype) {
		this.sortByFiletype = sortByFiletype;
		return this;
	}

	public boolean isSortByDate() {
		return sortByDate;
	}

	public GetExtendedFilesOptions setSortByDate(boolean sortByDate) {
		this.sortByDate = sortByDate;
		return this;
	}

	public boolean isSortByHeadRev() {
		return sortByHeadRev;
	}

	public GetExtendedFilesOptions setSortByHeadRev(boolean sortByHeadRev) {
		this.sortByHeadRev = sortByHeadRev;
		return this;
	}

	public boolean isSortByHaveRev() {
		return sortByHaveRev;
	}

	public GetExtendedFilesOptions setSortByHaveRev(boolean sortByHaveRev) {
		this.sortByHaveRev = sortByHaveRev;
		return this;
	}

	public boolean isSortByFileSize() {
		return sortByFileSize;
	}

	public GetExtendedFilesOptions setSortByFileSize(boolean sortByFileSize) {
		this.sortByFileSize = sortByFileSize;
		return this;
	}

	public FileStatOutputOptions getOutputOptions() {
		return outputOptions;
	}

	public GetExtendedFilesOptions setOutputOptions(FileStatOutputOptions outputOptions) {
		this.outputOptions = outputOptions;
		return this;
	}

	public FileStatAncilliaryOptions getAncilliaryOptions() {
		return ancilliaryOptions;
	}

	public GetExtendedFilesOptions setAncilliaryOptions(FileStatAncilliaryOptions ancilliaryOptions) {
		this.ancilliaryOptions = ancilliaryOptions;
		return this;
	}

	public String getAttributePattern() {
		return attributePattern;
	}

	public GetExtendedFilesOptions setAttributePattern(String attributePattern) {
		this.attributePattern = attributePattern;
		return this;
	}
}
