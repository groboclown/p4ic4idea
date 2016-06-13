/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer.getRevisionHistory method.<p>
 * 
 * Note that behavior is undefined if both longOutput and truncatedLongOutput are true.
 * If both are false, a short form of the description (prepared by the server) is returned.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getRevisionHistory(java.util.List, com.perforce.p4java.option.server.GetRevisionHistoryOptions)
 */
public class GetRevisionHistoryOptions extends Options {
	
	/**
	 * Options: -c[changelist], -m[max], -h, -i, -l, -L, -s
	 */
	public static final String OPTIONS_SPECS = "i:c:clz i:m:gtz b:h b:i b:l b:L b:s";
	
	/**
	 * If positive, displays only files  submitted at the given changelist number.
	 * Corresponds to -c#.
	 */
	protected int changelistId = IChangelist.UNKNOWN;
	
	/**
	 * If positive, displays at most 'maxRevs' revisions per file of
	 * the file[rev] argument specified. Corresponds to -m.
	 */
	protected int maxRevs = 0;
	
	/**
	 * If true, display file content history instead of file name history.
	 * Corresponds to -h.
	 */
	protected boolean contentHistory = false;
	
	/**
	 * If true, causes inherited file history to be displayed as well.
	 * Corresponds to -i.
	 */
	protected boolean includeInherited = false;
	
	/**
	 * If true, produces long output with the full text of the
	 * changelist descriptions. Corresponds to -l.
	 */
	protected boolean longOutput = false;
	
	/**
	 * If true, produces long output with the full text of the
	 * changelist descriptions truncated to 250 characters.
	 * Corresponds to -L.
	 */
	protected boolean truncatedLongOutput = false;
	
	/**
	 * If true, omit non-contributory integrations;
	 * corresponds to -s.
	 * 
	 * @since 2011.1
	 */
	protected boolean omitNonContributaryIntegrations = false;

	/**
	 * Default constructor.
	 */
	public GetRevisionHistoryOptions() {
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
	public GetRevisionHistoryOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetRevisionHistoryOptions(int changelistId, int maxRevs,
			boolean contentHistory, boolean includeInherited,
			boolean longOutput, boolean truncatedLongOutput) {
		super();
		this.changelistId = changelistId;
		this.maxRevs = maxRevs;
		this.contentHistory = contentHistory;
		this.includeInherited = includeInherited;
		this.longOutput = longOutput;
		this.truncatedLongOutput = truncatedLongOutput;
	}
	
	/**
	 * Explicit-value constructor.
	 * 
	 * @since 2011.1
	 */
	public GetRevisionHistoryOptions(int changelistId, int maxRevs,
			boolean contentHistory, boolean includeInherited,
			boolean longOutput, boolean truncatedLongOutput,
			boolean omitNonContributaryIntegrations) {
		super();
		this.changelistId = changelistId;
		this.maxRevs = maxRevs;
		this.contentHistory = contentHistory;
		this.includeInherited = includeInherited;
		this.longOutput = longOutput;
		this.truncatedLongOutput = truncatedLongOutput;
		this.omitNonContributaryIntegrations = omitNonContributaryIntegrations;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.getChangelistId(),
								this.getMaxRevs(),
								this.isContentHistory(),
								this.isIncludeInherited(),
								this.isLongOutput(),
								this.isTruncatedLongOutput(),
								this.isOmitNonContributaryIntegrations());
		return this.optionList;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public GetRevisionHistoryOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public int getMaxRevs() {
		return maxRevs;
	}

	public GetRevisionHistoryOptions setMaxRevs(int maxRevs) {
		this.maxRevs = maxRevs;
		return this;
	}

	public boolean isContentHistory() {
		return contentHistory;
	}

	public GetRevisionHistoryOptions setContentHistory(boolean contentHistory) {
		this.contentHistory = contentHistory;
		return this;
	}

	public boolean isIncludeInherited() {
		return includeInherited;
	}

	public GetRevisionHistoryOptions setIncludeInherited(boolean includeInherited) {
		this.includeInherited = includeInherited;
		return this;
	}

	public boolean isLongOutput() {
		return longOutput;
	}

	public GetRevisionHistoryOptions setLongOutput(boolean longOutput) {
		this.longOutput = longOutput;
		return this;
	}

	public boolean isTruncatedLongOutput() {
		return truncatedLongOutput;
	}

	public GetRevisionHistoryOptions setTruncatedLongOutput(boolean truncatedLongOutput) {
		this.truncatedLongOutput = truncatedLongOutput;
		return this;
	}

	/**
	 * @since 2011.1
	 */
	public boolean isOmitNonContributaryIntegrations() {
		return omitNonContributaryIntegrations;
	}

	/**
	 * @since 2011.1
	 */
	public GetRevisionHistoryOptions setOmitNonContributaryIntegrations(
			boolean omitNonContributaryIntegrations) {
		this.omitNonContributaryIntegrations = omitNonContributaryIntegrations;
		return this;
	}
}
