/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core;

import java.util.Date;

/**
 * Describes a Perforce job fix record.<p>
 * 
 * Full semantics can be found in the associated full Perforce
 * p4 command line documentation for the 'p4 fix' command. Not all fields
 * are valid for all fixes or circumstances -- test for nullness before
 * using...<p>
 * 
 * Fixes are currently always complete and not refreshable or updateable through
 * the IServerResource methods. Setter methods below will <i>not</i> cause the corresponding
 * fix in the Perforce server to be updated without an explicit update call.
 */

public interface IFix extends IServerResource {
	String getJobId();
	void setJobId(String jobId);
	
	int getChangelistId();
	void setChangelistId(int changelistId);
	
	Date getDate();
	void setDate(Date date);
	
	String getClientName();
	void setClientName(String clientName);
	
	String getUserName();
	void setUserName(String userName);
	
	String getStatus();
	void setStatus(String status);
	
	String getAction();
	void setAction(String action);
}
