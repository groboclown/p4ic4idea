/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core.file;

import java.util.List;

/**
 * Record stats returned by the obliterateFiles method. Obliterate removes files
 * and their history from the depot. The Perforce server returns information
 * about how many various types of records were deleted (or added).
 */

public interface IObliterateResult {
	
	/**
	 * Get the list of filespecs purged
	 */
	List<IFileSpec> getFileSpecs();

	/**
	 * Get the number of integration records added
	 */
	int getIntegrationRecAdded();
	
	/**
	 * Get the number of integration records deleted
	 */
	int getLabelRecDeleted();

	/**
	 * Get the number of client records deleted
	 */
	int getClientRecDeleted();

	/**
	 * Get the number of integration records deleted
	 */
	int getIntegrationRecDeleted();

	/**
	 * Get the number of working records deleted
	 */
	int getWorkingRecDeleted();

	/**
	 * Get the number of revision records deleted
	 */
	int getRevisionRecDeleted();
	
	/**
	 * Is report only
	 */
	boolean isReportOnly();
}
