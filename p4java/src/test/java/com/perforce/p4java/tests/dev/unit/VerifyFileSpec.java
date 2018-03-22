package com.perforce.p4java.tests.dev.unit;

import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.impl.generic.core.Changelist;

public class VerifyFileSpec {

	//These are the last
	public FileAction expAction = null;
	public int expChangelistId = Changelist.DEFAULT;
	public String expClientName = null;
	public String expUserName = null;
	public String expFileType = null;
	public String expOriginalPath = null;
	public String expPreferredPath = null;
	public String expClientPath = null;
	public FileSpecOpStatus expOpStatus = null;
	public String expStatusMessage = null;
	public int expFileRev = 0;
	
	public void setExpAction(FileAction expAction) {
		this.expAction = expAction;
	}
	
	public FileAction getExpAction() {
		return this.expAction;
	}
	
	public void setExpChangelistId(int expChangelistId) {
		this.expChangelistId = expChangelistId;
	}
	
	public int getExpChangelistId() {
		return this.expChangelistId;
	}
	
	public void setExpUserName(String expUserName) {
		this.expUserName = expUserName;
	}
	
	public String getExpUserName() {
		return this.expUserName;
	}
	
	public void setExpFileType(String expFileType) {
		this.expFileType = expFileType;
	}
	
	public String getExpFileType() {
		return this.expFileType;
	}
	
	public void setExpOriginalPath(String expOriginalPath) {
		this.expOriginalPath = expOriginalPath;
	}
	
	public String getExpOriginalPath() {
		return this.expOriginalPath;
	}
	
	public void setExpClientName(String expClientName) {
		this.expClientName = expClientName;
	}
	
	public String getExpClientName() {
		return this.expClientName;
	}

	public void setExpPreferredPath(String expPreferredPath) {
		this.expPreferredPath = expPreferredPath;
	}
	
	public String getExpPreferredPath() {
		return this.expPreferredPath;
	}

	public void setExpClientPath(String expClientPath) {
		this.expClientPath = expClientPath;
	}
	
	public String getExpClientPath() {
		return this.expClientPath;
	}

	public void setExpOpStatus(FileSpecOpStatus expOpStatus) {
		this.expOpStatus = expOpStatus;
	}
	
	public FileSpecOpStatus getExpOpStatus() {
		return this.expOpStatus;
	}
	
	public String getExpStatusMessage() {
		return this.expStatusMessage;
	}

	public void setExpStatusMessage(String expStatusMessage) {
		this.expStatusMessage = expStatusMessage;
	}
	
	public int getExpFileRev() {
		return this.expFileRev;
	}

	public void setExpFileRev(int expFileRev) {
		this.expFileRev = expFileRev;
	}

}