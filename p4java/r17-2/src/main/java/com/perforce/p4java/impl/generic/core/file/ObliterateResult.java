/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core.file;

import java.util.ArrayList;
import java.util.List;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.exception.NullPointerError;

/**
 * The various record stats returned by the obliterateFiles method. Obliterate
 * removes files and their history from the depot. The Perforce server returns
 * information about how many various types of records were deleted (or added).
 */

public class ObliterateResult implements IObliterateResult {

	private List<IFileSpec> fileSpecs = new ArrayList<IFileSpec>();
	private int integrationRecAdded = 0;
	private int labelRecDeleted = 0;
	private int clientRecDeleted = 0;
	private int integrationRecDeleted = 0;
	private int workingRecDeleted = 0;
	private int revisionRecDeleted = 0;
	private boolean reportOnly = false;

	/**
	 * Explicit parameterized constructor
	 */
	public ObliterateResult(List<IFileSpec> fileSpecs, int integrationRecAdded,
			int labelRecDeleted, int clientRecDeleted,
			int integrationRecDeleted, int workingRecDeleted,
			int revisionRecDeleted, boolean reportOnly) {

		if (fileSpecs == null) {
			throw new NullPointerError("null fileSpecs passed to ObliterateResult constructor");
		}
		this.fileSpecs = fileSpecs;
		this.integrationRecAdded = integrationRecAdded;
		this.labelRecDeleted = labelRecDeleted;
		this.clientRecDeleted = clientRecDeleted;
		this.integrationRecDeleted = integrationRecDeleted;
		this.workingRecDeleted = workingRecDeleted;
		this.revisionRecDeleted = revisionRecDeleted;
		this.reportOnly = reportOnly;
	}

	/**
	 * @see com.perforce.p4java.core.file.IObliterateResult#getFileSpecs()
	 */
	public List<IFileSpec> getFileSpecs() {
		return fileSpecs;
	}

	/**
	 * @see com.perforce.p4java.core.file.IObliterateResult#getIntegrationRecAdded()
	 */
	public int getIntegrationRecAdded() {
		return integrationRecAdded;
	}

	/**
	 * @see com.perforce.p4java.core.file.IObliterateResult#getLabelRecDeleted()
	 */
	public int getLabelRecDeleted() {

		return labelRecDeleted;
	}

	/**
	 * @see com.perforce.p4java.core.file.IObliterateResult#getClientRecDeleted()
	 */
	public int getClientRecDeleted() {

		return clientRecDeleted;
	}

	/**
	 * @see com.perforce.p4java.core.file.IObliterateResult#getIntegrationRecDeleted()
	 */
	public int getIntegrationRecDeleted() {

		return integrationRecDeleted;
	}

	/**
	 * @see com.perforce.p4java.core.file.IObliterateResult#getWorkingRecDeleted()
	 */
	public int getWorkingRecDeleted() {

		return workingRecDeleted;
	}

	/**
	 * @see com.perforce.p4java.core.file.IObliterateResult#getRevisionRecDeleted()
	 */
	public int getRevisionRecDeleted() {

		return revisionRecDeleted;
	}

	/**
	 * @see com.perforce.p4java.core.file.IObliterateResult#isReportOnly()
	 */
	public boolean isReportOnly() {

		return reportOnly;
	}
}
