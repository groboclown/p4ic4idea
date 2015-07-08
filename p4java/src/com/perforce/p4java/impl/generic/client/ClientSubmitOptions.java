/**
 * 
 */
package com.perforce.p4java.impl.generic.client;

import com.perforce.p4java.client.IClientSummary.IClientSubmitOptions;

/**
 * Simple generic IClientSubmitOptions implementation class. Attempts
 * to enforce the mutual-exclusivity of the options, but this can easily
 * be circumvented.
 */

public class ClientSubmitOptions implements IClientSubmitOptions {
	
	private boolean submitunchanged = false;
	private boolean submitunchangedReopen = false;
	private boolean revertunchanged = false;
	private boolean revertunchangedReopen = false;
	private boolean leaveunchanged = false;
	private boolean leaveunchangedReopen = false;
	
	/**
	 * Default constructor; sets all fields to false.
	 */
	public ClientSubmitOptions() {
	}
	
	/**
	 * Attempts to construct a ClientSubmitOptions object from a typical p4 cmd options string,
	 * e.g. "revertunchanged+reopen", or from a map returned from the
	 * server (where it's more commonly in the format "revertunchangedReopen".
	 * If optString is null, this is equivalent to calling the default constructor.<p>
	 * 
	 * Note that the optString parser is fairly permissive in what it will accept;
	 * for example, the string "submitunchanged submitunchanged+reopen" is accepted
	 * just fine even though it's a little redundant, and no real attempt is made to
	 * enforce the mutual exclusivity of the options.
	 */
	public ClientSubmitOptions(String optString) {
		if (optString != null) {
			String opts[] = optString.split(" ");
			
			for (String str : opts) {
				if (str.equalsIgnoreCase(SUBMIT_UNCHANGED)) {
					this.submitunchanged = true;
				} else if (str.equalsIgnoreCase(SUBMIT_UNCHANGED_REOPEN) || str.equalsIgnoreCase(SUBMIT_UNCHANGED + "Reopen")) {
					this.submitunchangedReopen = true;
					this.submitunchanged = true;
				} else if (str.equalsIgnoreCase(REVERT_UNCHANGED)) {
					this.revertunchanged = true;
				} else if (str.equalsIgnoreCase(REVERT_UNCHANGED_REOPEN) || str.equalsIgnoreCase(REVERT_UNCHANGED + "Reopen")) {
					this.revertunchangedReopen = true;
					this.revertunchanged = true;
				} else if (str.equalsIgnoreCase(LEAVE_UNCHANGED)) {
					this.leaveunchanged = true;
				} else if (str.equalsIgnoreCase(LEAVE_UNCHANGED_REOPEN) || str.equalsIgnoreCase(LEAVE_UNCHANGED + "Reopen")) {
					this.leaveunchangedReopen = true;
					this.leaveunchanged = true;
				}
			}
		}
	}
	
	/**
	 * Explicit-value constructor. Note that nonsensical constructs
	 * like submitunchanged = false and submitunchangedReopen = true
	 * are accepted silently; also, no attempt is made to enforce
	 * mutual exclusivity here.
	 */
	public ClientSubmitOptions(boolean submitunchanged,
			boolean submitunchangedReopen, boolean revertunchanged,
			boolean revertunchangedReopen, boolean leaveunchanged,
			boolean leaveunchangedReopen) {
		this.submitunchanged = submitunchanged;
		this.submitunchangedReopen = submitunchangedReopen;
		this.revertunchanged = revertunchanged;
		this.revertunchangedReopen = revertunchangedReopen;
		this.leaveunchanged = leaveunchanged;
		this.leaveunchangedReopen = leaveunchangedReopen;
	}
	
	/**
	 * Return a Perforce-standard representation of these options.
	 * This string is in the same format as used by the
	 * ClientSubmitOptions(String optionsString) constructor.
	 * Mirrors p4 cmd's behaviour in omitting non-set values.
	 */
	public String toString() {
		return ((this.submitunchanged ? (SUBMIT_UNCHANGED + (this.submitunchangedReopen ? REOPEN : "") + " ") : "")
				+ (this.revertunchanged ? (REVERT_UNCHANGED + (this.revertunchangedReopen ? REOPEN : "") + " ") : "")
				+ (this.leaveunchanged ? (LEAVE_UNCHANGED + (this.leaveunchangedReopen ? REOPEN : "") + " ") : "")).trim();
	}

	public boolean isSubmitunchanged() {
		return submitunchanged;
	}
	public void setSubmitunchanged(boolean submitunchanged) {
		resetOptions();
		this.submitunchanged = submitunchanged;
	}
	public boolean isSubmitunchangedReopen() {
		return submitunchangedReopen;
	}
	public void setSubmitunchangedReopen(boolean submitunchangedReopen) {
		resetOptions();
		this.submitunchangedReopen = submitunchangedReopen;
	}
	public boolean isRevertunchanged() {
		return revertunchanged;
	}
	public void setRevertunchanged(boolean revertunchanged) {
		resetOptions();
		this.revertunchanged = revertunchanged;
	}
	public boolean isRevertunchangedReopen() {
		return revertunchangedReopen;
	}
	public void setRevertunchangedReopen(boolean revertunchangedReopen) {
		resetOptions();
		this.revertunchangedReopen = revertunchangedReopen;
	}
	public boolean isLeaveunchanged() {
		return leaveunchanged;
	}
	public void setLeaveunchanged(boolean leaveunchanged) {
		resetOptions();
		this.leaveunchanged = leaveunchanged;
	}
	public boolean isLeaveunchangedReopen() {
		return leaveunchangedReopen;
	}
	public void setLeaveunchangedReopen(boolean leaveunchangedReopen) {
		resetOptions();
		this.leaveunchangedReopen = leaveunchangedReopen;
	}
	
	protected void resetOptions() {
		this.leaveunchanged = false;
		this.leaveunchangedReopen = false;
		this.revertunchanged = false;
		this.revertunchangedReopen = false;
		this.submitunchanged  = false;
		this.submitunchangedReopen = false;
	}
}
