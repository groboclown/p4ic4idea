/**
 * 
 */
package com.perforce.p4java.impl.generic.core.file;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IRevisionIntegrationData;

/**
 * Simple default generic implementation calls for the IFileRevisionData
 * interface.
 * 
 *
 */

public class FileRevisionData implements IFileRevisionData {
	
	private int revision = 0;
	private int changeListId = IChangelist.UNKNOWN;
	private FileAction action = null;
	private Date date = null;
	private String userName = null;
	private String fileType = null;
	private String description = null;
	private String depotFileName = null;
	private String clientName = null;
	private List<IRevisionIntegrationData> revisionIntegrationData = null;
	
	public class RevisionIntegrationData implements IRevisionIntegrationData {

		private int startFromRev = 0;
		private int endFromRev = 0;
		private String fromFile = null;
		private String howFrom = null;
		
		public RevisionIntegrationData(String startFromRev,
					String endFromRev, String fromFile, String howFrom) {
			
			this.startFromRev = FileSpec.getRevFromString(startFromRev);
			this.endFromRev = FileSpec.getRevFromString(endFromRev);
			this.fromFile = fromFile;
			this.howFrom = howFrom;
		}
		
		public int getEndFromRev() {
			return this.endFromRev;
		}

		public String getFromFile() {
			return this.fromFile;
		}

		public String getHowFrom() {
			return this.howFrom;
		}

		public int getStartFromRev() {
			return this.startFromRev;
		}
		
	}
	
	public FileRevisionData() {
	}
	
	public FileRevisionData(int revision, int changeListId,
			FileAction action, Date date, String userName, String fileType,
			String description, String depotFileName, String clientName) {
		this.revision = revision;
		this.changeListId = changeListId;
		this.action = action;
		this.date = date;
		this.userName = userName;
		this.fileType = fileType;
		this.description = description;
		this.depotFileName = depotFileName;
		this.clientName = clientName;
	}
	
	public FileRevisionData(Map<String, Object> map, int revNo) {
		if (map != null) {
			String revStr = "";
			
			if (map != null) {
				if (revNo >= 0) {
					revStr += revNo;
				}

				this.setRevision(new Integer((String) map.get("rev" + revNo)));
				String changeId = (String) map.get("change" + revStr);
				if (changeId != null) {
					if (changeId.equalsIgnoreCase("default")) {
						this.setChangelistId(IChangelist.DEFAULT);
					} else {
						this.setChangelistId(new Integer(changeId));
					}
				}
				this.setAction(FileAction.fromString((String) map.get("action" + revStr)));
				this.setDate(new Date(Long.parseLong((String) map.get("time" + revStr)) * 1000));
				this.setUserName((String) map.get("user" + revStr));
				this.setFileType((String) map.get("type" + revStr));
				this.setDescription((String) map.get("desc" + revStr));
				this.setDepotFileName((String) map.get("depotFile"));
				this.setClientName((String) map.get("client" + revStr));
				
				int revRev = 0;
				
				String fromFile = (String) map.get("file" + revNo  + "," + revRev);
				
				while (fromFile != null) {
					if (this.revisionIntegrationData == null) {
						this.revisionIntegrationData = new ArrayList<IRevisionIntegrationData>();
					}
					this.revisionIntegrationData.add(new RevisionIntegrationData(
								(String) map.get("srev" + revNo  + "," + revRev),
								(String) map.get("erev" + revNo  + "," + revRev),
								fromFile,
								(String) map.get("how" + revNo  + "," + revRev)
							));
					revRev++;
					fromFile = (String) map.get("file" + revNo  + "," + revRev);
				}
			}
		}
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	public int getChangelistId() {
		return changeListId;
	}

	public void setChangelistId(int changeListId) {
		this.changeListId = changeListId;
	}

	public FileAction getAction() {
		return action;
	}

	public void setAction(FileAction action) {
		this.action = action;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDepotFileName() {
		return depotFileName;
	}

	public void setDepotFileName(String depotFileName) {
		this.depotFileName = depotFileName;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileRevisionData#getRevisionIntegrationData()
	 */
	public List<IRevisionIntegrationData> getRevisionIntegrationData() {
		return this.revisionIntegrationData;
	}
}
