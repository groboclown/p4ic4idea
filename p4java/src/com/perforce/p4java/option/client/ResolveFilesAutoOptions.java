/**
 *
 */
package com.perforce.p4java.option.client;

import java.util.Iterator;
import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IClient.resolveFilesAuto.<p>
 * 
 * Note that absolutely no sanity checking is performed in the current default
 * implementation for clashing options, etc.
 *
 * @see com.perforce.p4java.client.IClient#resolveFilesAuto(java.util.List, com.perforce.p4java.option.client.ResolveFilesAutoOptions)
 */
public class ResolveFilesAutoOptions extends Options {

    /**
     * Options: -n, -s, -af, -at, -ay, -as, -Aa, -Ab, -Ac, -Ad, -Am, -At, -c[changelist], -t, -db, -dw, -dl, -o
     */
    public static final String OPTIONS_SPECS = "b:n b:s b:af b:as b:at b:ay b:Aa b:Ab b:Ac b:Ad b:Am b:At i:c:cl b:t b:db b:dw b:dl b:o";

    /** If true, only do "safe" resolves, as documented for the p4 "-as" option. */
    protected boolean safeMerge = false;

    /**
     * If true, automatically accept "their" changes, as documented for the p4
     * "-at" option.
     */
    protected boolean acceptTheirs = false;

    /**
     * If true, automatically accept "your" changes, as documented for the p4
     * "-ay" option.
     */
    protected boolean acceptYours = false;

    /**
     * If true, don't do the actual resolve, just return the actions that would
     * have been performed for the resolve. Corresponds to the '-n' option.
     */
    protected boolean showActionsOnly = false;

    /**
     * If true, skip this file. Corresponds to the '-s' option.
     */
    protected boolean skipFile = false;

    /**
     * Forces auto-mode resolve to accept the merged file even if there are
     * conflicts. Corresponds to the -af option.
     */
    protected boolean forceResolve = false;

    
    /** Resolve file attribute changes. Corresponds to the '-Aa' option. */
    protected boolean resolveFileAttributeChanges = false;
    
    /** Resolve file branching. Corresponds to the '-Ab' option. */
    protected boolean resolveFileBranching = false;

    /** Resolve file content changes. Corresponds to the '-Ac' option. */
    protected boolean resolveFileContentChanges = false;

    /** Resolve file deletions. Corresponds to the '-Ad' option. */
    protected boolean resolveFileDeletions = false;

    /** Resolve moved and renamed files. Corresponds to the '-Am' option. */
    protected boolean resolveMovedFiles = false;

    /** Resolve filetype changes. Corresponds to the '-At' option. */
    protected boolean resolveFiletypeChanges = false;

    /**
     * Limits 'p4 resolve' to the files in changelist#. Corresponds to
     * '-c changelist#'.
     */
    protected int changelistId = IChangelist.UNKNOWN;

    /**
     * If true, forces 'p4 resolve' to attempt a textual merge, even for files
     * with non-text (binary) types. Corresponds to the '-t' option.
     */
    protected boolean forceTextualMerge = false;
    
    /**
     * If true, ignores whitespace-only changes (for instance, a tab replaced by
     * eight spaces). Corresponds to the '-db' option.
     */
    protected boolean ignoreWhitespaceChanges = false;

    /**
     * If true, ignores whitespace altogether (for instance, deletion of tabs or
     * other whitespace). Corresponds to the '-dw' option.
     */
    protected boolean ignoreWhitespace = false;
    
    /**
     * If true, ignores differences in line-ending convention. Corresponds to
     * the '-dl' option.
     */
    protected boolean ignoreLineEndings = false;
    
    /**
     * If true, show the base file name and revision to be used during the merge.
     * Corresponds to the '-o' option.
     */
    protected boolean showBase = false;
    
    /**
     * Default constructor.
     */
    public ResolveFilesAutoOptions() {
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
    public ResolveFilesAutoOptions(String... options) {
        super(options);
    }

    /**
     * Explicit-value constructor.
     */
    public ResolveFilesAutoOptions(boolean showActionsOnly, boolean safeMerge,
            boolean acceptTheirs, boolean acceptYours, boolean forceResolve) {
        super();
        this.showActionsOnly = showActionsOnly;
        this.safeMerge = safeMerge;
        this.acceptTheirs = acceptTheirs;
        this.acceptYours = acceptYours;
        this.forceResolve = forceResolve;
    }

    /**
     * Explicit-value constructor.
     */
    public ResolveFilesAutoOptions(boolean showActionsOnly, boolean safeMerge,
            boolean acceptTheirs, boolean acceptYours, boolean forceResolve,
            boolean resolveFileBranching, boolean resolveFileContentChanges,
            boolean resolveFileDeletions, boolean resolveMovedFiles,
            boolean resolveFiletypeChanges, int changelistId) {
        super();
        this.showActionsOnly = showActionsOnly;
        this.safeMerge = safeMerge;
        this.acceptTheirs = acceptTheirs;
        this.acceptYours = acceptYours;
        this.forceResolve = forceResolve;
        this.resolveFileBranching = resolveFileBranching;
        this.resolveFileContentChanges = resolveFileContentChanges;
        this.resolveFileDeletions = resolveFileDeletions;
        this.resolveMovedFiles = resolveMovedFiles;
        this.resolveFiletypeChanges = resolveFiletypeChanges;
        this.changelistId = changelistId;
    }

    /**
     * Explicit-value constructor.
     */
    public ResolveFilesAutoOptions(boolean showActionsOnly, boolean safeMerge,
            boolean acceptTheirs, boolean acceptYours, boolean forceResolve,
            boolean resolveFileBranching, boolean resolveFileContentChanges,
            boolean resolveFileDeletions, boolean resolveMovedFiles,
            boolean resolveFiletypeChanges, int changelistId,
            boolean forceTextualMerge, boolean ignoreWhitespaceChanges,
            boolean ignoreWhitespace, boolean ignoreLineEndings) {
        super();
        this.showActionsOnly = showActionsOnly;
        this.safeMerge = safeMerge;
        this.acceptTheirs = acceptTheirs;
        this.acceptYours = acceptYours;
        this.forceResolve = forceResolve;
        this.resolveFileBranching = resolveFileBranching;
        this.resolveFileContentChanges = resolveFileContentChanges;
        this.resolveFileDeletions = resolveFileDeletions;
        this.resolveMovedFiles = resolveMovedFiles;
        this.resolveFiletypeChanges = resolveFiletypeChanges;
        this.changelistId = changelistId;
        this.forceTextualMerge = forceTextualMerge;
        this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
        this.ignoreWhitespace = ignoreWhitespace;
        this.ignoreLineEndings = ignoreLineEndings;
    }

    /**
     * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
     */
    public List<String> processOptions(IServer server) throws OptionsException {
        this.optionList = this.processFields(OPTIONS_SPECS,
                this.isShowActionsOnly(), this.isSkipFile(),
                this.isForceResolve(),
                this.isSafeMerge(), this.isAcceptTheirs(),
                this.isAcceptYours(),
                this.isResolveFileAttributeChanges(),
                this.isResolveFileBranching(),
                this.isResolveFileContentChanges(),
                this.isResolveFileDeletions(), this.isResolveMovedFiles(),
                this.isResolveFiletypeChanges(), this.getChangelistId(),
                this.isForceTextualMerge(), this.isIgnoreWhitespaceChanges(),
                this.isIgnoreWhitespace(), this.isIgnoreLineEndings(),
                this.isShowBase());

        // Combine the -A flags, if more than one is set
        StringBuilder sb = new StringBuilder();
        if (this.optionList != null) {
            // Use an iterator for safe removal of elements in a collection
            for (Iterator<String> it = this.optionList.iterator(); it.hasNext();) {
                String s = it.next();
                if (s.startsWith("-A")) {
                    if (s.length() == 3) {
                        char c = s.charAt(2);
                        sb.append(c);
                    }
                    it.remove();
                }
            }
        }
        if (sb != null && sb.length() > 0) {
            optionList.add("-A" + sb.toString());
        }

        return this.optionList;
    }

    public boolean isSafeMerge() {
        return safeMerge;
    }

    public ResolveFilesAutoOptions setSafeMerge(boolean safeMerge) {
        this.safeMerge = safeMerge;
        return this;
    }

    public boolean isAcceptTheirs() {
        return acceptTheirs;
    }

    public ResolveFilesAutoOptions setAcceptTheirs(boolean acceptTheirs) {
        this.acceptTheirs = acceptTheirs;
        return this;
    }

    public boolean isAcceptYours() {
        return acceptYours;
    }

    public ResolveFilesAutoOptions setAcceptYours(boolean acceptYours) {
        this.acceptYours = acceptYours;
        return this;
    }

    public boolean isShowActionsOnly() {
        return showActionsOnly;
    }

    public ResolveFilesAutoOptions setShowActionsOnly(boolean showActionsOnly) {
        this.showActionsOnly = showActionsOnly;
        return this;
    }

    public boolean isSkipFile() {
        return skipFile;
    }

    public ResolveFilesAutoOptions setSkipFile(boolean skipFile) {
        this.skipFile = skipFile;
        return this;
    }

    public boolean isForceResolve() {
        return forceResolve;
    }

    public ResolveFilesAutoOptions setForceResolve(boolean forceResolve) {
        this.forceResolve = forceResolve;
        return this;
    }

    public boolean isResolveFileBranching() {
        return resolveFileBranching;
    }

    public ResolveFilesAutoOptions setResolveFileBranching(
            boolean resolveFileBranching) {
        this.resolveFileBranching = resolveFileBranching;
        return this;
    }

    public boolean isResolveFileContentChanges() {
        return resolveFileContentChanges;
    }

    public ResolveFilesAutoOptions setResolveFileContentChanges(
            boolean resolveFileContentChanges) {
        this.resolveFileContentChanges = resolveFileContentChanges;
        return this;
    }

    public boolean isResolveFileDeletions() {
        return resolveFileDeletions;
    }

    public ResolveFilesAutoOptions setResolveFileDeletions(
            boolean resolveFileDeletions) {
        this.resolveFileDeletions = resolveFileDeletions;
        return this;
    }

    public boolean isResolveMovedFiles() {
        return resolveMovedFiles;
    }

    public ResolveFilesAutoOptions setResolveMovedFiles(
            boolean resolveMovedFiles) {
        this.resolveMovedFiles = resolveMovedFiles;
        return this;
    }

    public boolean isResolveFiletypeChanges() {
        return resolveFiletypeChanges;
    }

    public ResolveFilesAutoOptions setResolveFiletypeChanges(
            boolean resolveFiletypeChanges) {
        this.resolveFiletypeChanges = resolveFiletypeChanges;
        return this;
    }

    public boolean isResolveFileAttributeChanges() {
        return resolveFileAttributeChanges;
    }

    public ResolveFilesAutoOptions setResolveFileAttributeChanges(
            boolean resolveFileAttributeChanges) {
        this.resolveFileAttributeChanges = resolveFileAttributeChanges;
        return this;
    }
    public ResolveFilesAutoOptions setResolveResolveType(String type, boolean enable) {
    	if (type.equals("attributes"))
    		return setResolveFileAttributeChanges(enable);
    	if (type.equals("branch"))
    		return setResolveFileBranching(enable);
    	if (type.equals("content"))
    		return setResolveFileContentChanges(enable);
    	if (type.equals("delete"))
    		return setResolveFileDeletions(enable);
    	if (type.equals("filetype"))
    		return setResolveFiletypeChanges(enable);
    	if (type.equals("move"))
    		return setResolveMovedFiles(enable);
    	// unknown resolve type; could throw but maybe better to just ignore it
    	return this;
    }
    public int getChangelistId() {
        return changelistId;
    }

    public ResolveFilesAutoOptions setChangelistId(int changelistId) {
        this.changelistId = changelistId;
        return this;
    }
    
    public boolean isForceTextualMerge() {
        return forceTextualMerge;
    }

    public ResolveFilesAutoOptions setForceTextualMerge(boolean forceTextualMerge) {
        this.forceTextualMerge = forceTextualMerge;
        return this;
    }

    public boolean isIgnoreWhitespaceChanges() {
    	return ignoreWhitespaceChanges;
    }
    
    public ResolveFilesAutoOptions setIgnoreWhitespaceChanges(boolean ignoreWhitespaceChanges) {
    	this.ignoreWhitespaceChanges = ignoreWhitespaceChanges;
    	return this;
    }

    public boolean isIgnoreWhitespace() {
    	return ignoreWhitespace;
    }
    
    public ResolveFilesAutoOptions setIgnoreWhitespace(boolean ignoreWhitespace) {
    	this.ignoreWhitespace = ignoreWhitespace;
    	return this;
    }

    public boolean isIgnoreLineEndings() {
    	return ignoreLineEndings;
    }
    
    public ResolveFilesAutoOptions setIgnoreLineEndings(boolean ignoreLineEndings) {
    	this.ignoreLineEndings = ignoreLineEndings;
    	return this;
    }
    
    public boolean isShowBase() {
    	return showBase;
    }
    
    public ResolveFilesAutoOptions setShowBase(boolean showBase) {
    	this.showBase = showBase;
    	return this;
    }
}
