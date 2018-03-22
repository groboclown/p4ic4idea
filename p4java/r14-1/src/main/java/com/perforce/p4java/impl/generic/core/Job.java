/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.core;

import java.util.Map;

import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServer;

/**
 * Simple generic default implementation class for the IJob interface.
 *
 *
 */

public class Job extends ServerResource implements IJob {
	
	/** The max description length for "short" or summary descriptions */
	public int SHORT_DESCR_LENGTH = 128;
	
	private String id = null;
	private Map<String, Object> rawFields = null;
	private String description = null;
	private IJobSpec jobSpec = null;
	
	/**
	 * Simple factory method for creating a new Job class.
	 * 
	 * @param server non-null IServer to be associated with this job.
	 * @param map non-null fields map for the job to be created.
	 * @return new Job object
	 */
	public static Job newJob(IServer server, Map<String, Object> map) {
		if (server == null) {
			throw new NullPointerError("null server passed to Job.newJob()");
		}
		if (map == null) {
			throw new NullPointerError("null map passed to Job.newJob()");
		}
		return new Job(server, map);
	}
	
	public Job(IServer server, Map<String, Object> map) {
		this(server, map, false);
	}
	
	public Job(IServer server, Map<String, Object> map, boolean longDescriptions) {
		super(true, true);
		this.server = server;
		
		// Now try to retrieve a handful of "standard" fields
		// if we can...
		
		if (map != null) {
			this.id = getJobIdString(map);
			this.description = getDescriptionString(map, longDescriptions);

			// Remove the 'specFormatted' field.
			// See job072366 for more detail.
			map.remove("specFormatted");
		}

		// Assign the raw fields
		this.rawFields = map;
	}
	
	/**
	 * This method will refresh by getting the complete job model. If this
	 * refresh is successful then this job will be marked as complete.
	 * 
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#refresh()
	 */
	public void refresh() throws ConnectionException, RequestException,
			AccessException {
		IServer refreshServer = this.server;
		String refreshId = this.id;
		if (refreshServer != null && refreshId != null) {
			IJob refreshedJob = refreshServer.getJob(refreshId);
			if (refreshedJob != null) {
				this.description = refreshedJob.getDescription();
				this.rawFields = refreshedJob.getRawFields();
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.IJob#updateOnServer()
	 * 
	 * NOTE: do not use this method if the server field has not been set.
	 */
	public String updateOnServer()
				throws ConnectionException, RequestException, AccessException {
		if (this.server == null) {
			throw new NullPointerError("Null server field in Job.updateOnServer");
		}
		
		return this.server.updateJob(this);
	}
	
	public void update()
			throws ConnectionException, RequestException, AccessException {
		this.server.updateJob(this);
	}

	/**
	 * @see com.perforce.p4java.core.IJob#getDescription()
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @see com.perforce.p4java.core.IJob#getId()
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * @see com.perforce.p4java.core.IJob#getJobSpec()
	 */
	public IJobSpec getJobSpec() {
		return this.jobSpec;
	}

	/**
	 * @see com.perforce.p4java.core.IJob#getRawFields()
	 */
	public Map<String, Object> getRawFields() {
		return this.rawFields;
	}

	/**
	 * @see com.perforce.p4java.core.IJob#setId(java.lang.String)
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @see com.perforce.p4java.core.IJob#setRawFields(java.util.Map)
	 */
	public void setRawFields(Map<String, Object> rawFields) {
		this.rawFields = rawFields;
	}

	/**
	 * @see com.perforce.p4java.core.IJob#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @see com.perforce.p4java.core.IJob#setJobSpec(com.perforce.p4java.core.IJobSpec)
	 */
	public void setJobSpec(IJobSpec jobSpec) {
		this.jobSpec = jobSpec;
	}
	
	protected String getJobIdString(Map<String, Object> map) {
		String candidate = (String) map.get("Job");
		
		if (candidate == null) {
			candidate = (String) map.get("job");
			if (candidate == null) {
				candidate = (String) map.get("JobId");
			}
		}
		
		return candidate;
	}
	
	protected String getDescriptionString(Map<String, Object> map, boolean longDescriptions) {
		String candidate = (String) map.get("Description");
		
		if (candidate == null) {
			candidate = (String) map.get("description");
			if (candidate == null) {
				candidate = (String) map.get("Desc");
			}
		}
		
		if ((candidate != null) && !longDescriptions && (candidate.length() > SHORT_DESCR_LENGTH)) {
			return candidate.substring(0, SHORT_DESCR_LENGTH - 1);
		}
		return candidate;
	}
}
