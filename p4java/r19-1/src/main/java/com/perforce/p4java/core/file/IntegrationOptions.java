/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core.file;

/**
 * A helper class to gather together some of the (infinitely complex)
 * less-common options available to the integrate method in one object.
 * This relieves us of having a single integrate method with something
 * like thirteen separate parameters....<p>
 * 
 * No attempt is made here to explain the various options and their
 * semantics -- please consult the main Perforce integ command 
 * documentation for a full explanation -- but see also the explicit-value
 * constructor below for a summary of option meanings.
 * 
 *
 */

public class IntegrationOptions {

	private boolean useHaveRev = false;
	private boolean baselessMerge = false;
	private boolean displayBaseDetails = false;;
	private boolean propagateType = false;
	private boolean dontCopyToClient = false;
	private boolean force = false;
	private boolean bidirectionalInteg = false;
	private boolean reverseMapping = false;
	private String[] deletedOptions = null;
	private int maxFiles = -1;
	
	/**
	 * Default constructor; will generate a suitable IntegrationOptions
	 * object with typically-safe and useful default options values for straightforward
	 * file-to-file integrations.
	 */
	public IntegrationOptions() {
	}
	
	/**
	 * Explicit-value constructor.
	 * 
	 * @param useHaveRev causes the target files to be left at the revision currently
	 * 			on the client (the '#have' revision)
	 * @param baselessMerge enables integration between files that have no integration history
	 * @param displayBaseDetails displays the base file name and revision which will
	 * 			be used in subsequent resolves if a resolve is needed
	 * @param propagateType makes the source file's filetype propagate to the target file
	 * @param dontCopyToClient makes the integration work faster by not copying newly
	 * 			branched files to the client
	 * @param force if true, forces integrate to act without regard for previous
	 * 			integration history
	 * @param bidirectionalInteg causes the branch view to work bidirectionally,
	 * 			where the scope of the command is limited to integrations whose
	 * 			'from' files match fromFile[revRange]
	 * @param reverseMapping reverse the mappings in the branch view, with the
	 * 			target files and source files exchanging place. The branchSpec parameter
	 *			will be required in the associated integration call.
	 * @param deletedOptions if non-null, must contain zero or more non-null entries
	 * 			with individual values "d", "Dt", "Ds", or "Di"; null, inconsistent, or conflicting
	 * 			option values here will have unspecified and potentially incorrect effects.
	 */
	public IntegrationOptions(boolean useHaveRev, boolean baselessMerge,
					boolean displayBaseDetails, boolean propagateType,
					boolean dontCopyToClient, boolean force,
					boolean bidirectionalInteg, boolean reverseMapping,
					String[] deletedOptions) {
		this.useHaveRev = useHaveRev;
		this.baselessMerge = baselessMerge;
		this.displayBaseDetails = displayBaseDetails;
		this.propagateType = propagateType;
		this.dontCopyToClient = dontCopyToClient;
		this.force = force;
		this.bidirectionalInteg = bidirectionalInteg;
		this.reverseMapping = reverseMapping;
		this.deletedOptions = deletedOptions;
	}
	
	public boolean isUseHaveRev() {
		return this.useHaveRev;
	}
	
	public void setUseHaveRev(boolean useHaveRev) {
		this.useHaveRev = useHaveRev;
	}
	
	public boolean isBaselessMerge() {
		return this.baselessMerge;
	}
	
	public void setBaselessMerge(boolean baselessMerge) {
		this.baselessMerge = baselessMerge;
	}
	
	public boolean isDisplayBaseDetails() {
		return this.displayBaseDetails;
	}
	
	public void setDisplayBaseDetails(boolean displayBaseDetails) {
		this.displayBaseDetails = displayBaseDetails;
	}
	
	public boolean isPropagateType() {
		return this.propagateType;
	}
	
	public void setPropagateType(boolean propagateType) {
		this.propagateType = propagateType;
	}
	
	public boolean isDontCopyToClient() {
		return this.dontCopyToClient;
	}
	
	public void setDontCopyToClient(boolean dontCopyToClient) {
		this.dontCopyToClient = dontCopyToClient;
	}
	
	public boolean isForce() {
		return this.force;
	}
	
	public void setForce(boolean force) {
		this.force = force;
	}
	
	public boolean isBidirectionalInteg() {
		return this.bidirectionalInteg;
	}
	
	public void setBidirectionalInteg(boolean bidirectionalInteg) {
		this.bidirectionalInteg = bidirectionalInteg;
	}
	
	public String[] getDeletedOptions() {
		return this.deletedOptions;
	}
	
	public void setDeletedOptions(String[] deletedOptions) {
		this.deletedOptions = deletedOptions;
	}

	public boolean isReverseMapping() {
		return this.reverseMapping;
	}

	public void setReverseMapping(boolean reverseMapping) {
		this.reverseMapping = reverseMapping;
	}
	
	public int getMaxFiles() {
		return this.maxFiles;
	}
	
	public void setMaxFiles(int maxFiles) {
		this.maxFiles = maxFiles;
	}
	
}
