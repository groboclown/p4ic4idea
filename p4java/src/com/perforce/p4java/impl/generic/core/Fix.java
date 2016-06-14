/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.core;

import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFix;

/**
 * Simple generic default implementation class for the IFix interface.
 */

public class Fix extends ServerResource implements IFix {

	private String jobId = null;
	private int changeListId = IChangelist.UNKNOWN;
	private String clientName = null;
	private Date date = null;
	private String status = null;
	private String userName = null;
	private String action = null;
	
	public Fix() {
		super(false, false);
	}
	
	public Fix(Map<String, Object> map) {
		super(false, false);
		
		// Note: some fix recs use upper-case initial field names, others don't.
		// I should probably have the engineer who thought this was appropriate
		// executed, but I'll just leave it alone now...
		
		if (map != null) {
			try {
				this.jobId =
					((String) map.get("Job") == null ? (String) map.get("job") : (String) map.get("Job"));
				
				String idString =
					((String) map.get("Change") == null ? (String) map.get("change") : (String) map.get("Change"));
				if ((idString != null) && (idString.equalsIgnoreCase("new"))) {
					this.changeListId = IChangelist.DEFAULT;
				} else {
					try {
						this.changeListId = new Integer(idString);
					} catch (Exception exc) {
						this.changeListId = IChangelist.UNKNOWN;
					}
				}
				
				String dateStr = (String) map.get("Date");
				if (dateStr != null) {
					this.date = new Date((new Long(dateStr)) * 1000);
				}
				
				this.status =
					((String) map.get("Status") == null ? (String) map.get("status") : (String) map.get("Status"));
				this.clientName = (String) map.get("Client");
				this.userName = (String) map.get("User");
				this.action =
					((String) map.get("Action") == null ? (String) map.get("action") : (String) map.get("Action"));
			} catch (Exception exc) {
				Log.error("Unknown conversion error in Fix: " + exc);
				Log.exception(exc);
			}
		}
	}
	
	/**
	 * @see com.perforce.p4java.core.IFix#getChangelistId()
	 */
	public int getChangelistId() {
		return this.changeListId;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#getJobId()
	 */
	public String getJobId() {
		return this.jobId;
	}
	
	/**
	 * @see com.perforce.p4java.core.IFix#getClientName()
	 */
	public String getClientName() {
		return this.clientName;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#getDate()
	 */
	public Date getDate() {
		return this.date;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#getStatus()
	 */
	public String getStatus() {
		return this.status;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#getUserName()
	 */
	public String getUserName() {
		return this.userName;
	}
	
	/**
	 * @see com.perforce.p4java.core.IFix#getAction()
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#setJobId(java.lang.String)
	 */
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#setChangelistId(int)
	 */
	public void setChangelistId(int changeListId) {
		this.changeListId = changeListId;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#setClientName(java.lang.String)
	 */
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#setDate(java.util.Date)
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#setStatus(java.lang.String)
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#setUserName(java.lang.String)
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @see com.perforce.p4java.core.IFix#setAction(java.lang.String)
	 */
	public void setAction(String action) {
		this.action = action;
	}

}
