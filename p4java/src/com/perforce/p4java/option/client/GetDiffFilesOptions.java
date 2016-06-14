/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options subclass for the IClient.getDiffFiles method.
 * 
 * @see com.perforce.p4java.client.IClient#getDiffFiles(java.util.List, com.perforce.p4java.option.client.GetDiffFilesOptions)
 */
public class GetDiffFilesOptions extends Options {
	
	/**
	 * Options: -m[max], -t, -s[x]
	 */
	public static final String OPTIONS_SPECS = "i:m:gtz b:t b:sa b:sb b:sd b:se b:sl b:sr";
	
	/**
	 * If non-zero, return only this many results. Corresponds to -m flag.
	 */
	protected int maxFiles = 0;
	
	/**
	 * If true, diff non-text files. Corresponds to -t flag.
	 */
	protected boolean diffNonTextFiles = false;
	
	/**
	 * If true, report opened files that are different from the revision in the depot,
	 * or missing. Corresponds to -sa.
	 */
	protected boolean openedDifferentMissing = false;
	
	/**
	 * If true, report files that are opened for integrate. Corresponds to -sb flag.
	 */
	protected boolean openedForIntegrate = false;
	
	/**
	 * If true, report unopened files that are missing on the client. Corresponds
	 * to -sd.
	 */
	protected boolean unopenedMissing = false;
	
	/**
	 * If true, report unopened files that are different from the revision
	 * in the depot. Corresponds to -se flag.
	 */
	protected boolean unopenedDifferent = false;
	
	/**
	 * If true, report every unopened file, along with the status of
	 * 'same, 'diff', or 'missing' as compared to its
	 * revision in the depot. Corresponds to -sl.
	 */
	protected boolean unopenedWithStatus = false;
	
	/**
	 * If true, report opened files that are the same as the revision in the depot.
	 * Corresponds to -sr.
	 */
	protected boolean openedSame = false;

	/**
	 * Default constructor.
	 */
	public GetDiffFilesOptions() {
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
	public GetDiffFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetDiffFilesOptions(int maxFiles, boolean diffNonTextFiles,
			boolean openedDifferentMissing, boolean openedForIntegrate,
			boolean unopenedMissing, boolean unopenedDifferent,
			boolean unopenedWithStatus, boolean openedSame) {
		super();
		this.maxFiles = maxFiles;
		this.diffNonTextFiles = diffNonTextFiles;
		this.openedDifferentMissing = openedDifferentMissing;
		this.openedForIntegrate = openedForIntegrate;
		this.unopenedMissing = unopenedMissing;
		this.unopenedDifferent = unopenedDifferent;
		this.unopenedWithStatus = unopenedWithStatus;
		this.openedSame = openedSame;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
										this.getMaxFiles(),
										this.isDiffNonTextFiles(),
										this.isOpenedDifferentMissing(),
										this.isOpenedForIntegrate(),
										this.isUnopenedMissing(),
										this.isUnopenedDifferent(),
										this.isUnopenedWithStatus(),
										this.isOpenedSame()
								);
		
		return this.optionList;
	}

	public int getMaxFiles() {
		return maxFiles;
	}

	public GetDiffFilesOptions setMaxFiles(int maxFiles) {
		this.maxFiles = maxFiles;
		return this;
	}

	public boolean isDiffNonTextFiles() {
		return diffNonTextFiles;
	}

	public GetDiffFilesOptions setDiffNonTextFiles(boolean diffNonTextFiles) {
		this.diffNonTextFiles = diffNonTextFiles;
		return this;
	}

	public boolean isOpenedDifferentMissing() {
		return openedDifferentMissing;
	}

	public GetDiffFilesOptions setOpenedDifferentMissing(boolean openedDifferentMissing) {
		this.openedDifferentMissing = openedDifferentMissing;
		return this;
	}

	public boolean isOpenedForIntegrate() {
		return openedForIntegrate;
	}

	public GetDiffFilesOptions setOpenedForIntegrate(boolean openedForIntegrate) {
		this.openedForIntegrate = openedForIntegrate;
		return this;
	}

	public boolean isUnopenedMissing() {
		return unopenedMissing;
	}

	public GetDiffFilesOptions setUnopenedMissing(boolean unopenedMissing) {
		this.unopenedMissing = unopenedMissing;
		return this;
	}

	public boolean isUnopenedDifferent() {
		return unopenedDifferent;
	}

	public GetDiffFilesOptions setUnopenedDifferent(boolean unopenedDifferent) {
		this.unopenedDifferent = unopenedDifferent;
		return this;
	}

	public boolean isUnopenedWithStatus() {
		return unopenedWithStatus;
	}

	public GetDiffFilesOptions setUnopenedWithStatus(boolean unopenedWithStatus) {
		this.unopenedWithStatus = unopenedWithStatus;
		return this;
	}

	public boolean isOpenedSame() {
		return openedSame;
	}

	public GetDiffFilesOptions setOpenedSame(boolean openedSame) {
		this.openedSame = openedSame;
		return this;
	}
}
