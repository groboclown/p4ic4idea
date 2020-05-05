/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core;

import java.util.Map;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;

/**
 * The minimal Perforce job interface. The format and semantics of Perforce jobs
 * can vary greatly between servers and installations, and are described fully
 * by an associated IJobSpec interface, which should be used to fully interpret
 * and work with jobs for a specific server. This particular interface (IJob)
 * is really intended to let a consumer get a summary list only; further details may
 * need to be retrieved from the jobspec and associated elements and interfaces.<p>
 * 
 * Note that what constitutes the job ID and description may not always even be accurate,
 * as it's intuited under the covers using a few shopworn rules of thumb. In any case,
 * the list is returned in the order returned from the Perforce server, and the raw fields
 * map is assumed to contain authoritative field values.<p>
 * 
 * IJob objects are currently always complete, are updateable, but not refreshable. Setter
 * methods below will not affect the corresponding job on the Perforce server unless
 * an update is performed.
 *
 *
 */

public interface IJob extends IServerResource {

	/**
	 * get job name
	 * @return job name
	 */
	String getId();

	/**
	 * set job name
	 * @param id
	 */
	void setId(String id);
	
	String getDescription();
	void setDescription(String description);
	
	IJobSpec getJobSpec();
	void setJobSpec(IJobSpec jobSpec);
	
	Map<String, Object> getRawFields();
	void setRawFields(Map<String, Object> rawFields);
	
	/**
	 * Update the underlying Perforce job associated with this object in
	 * the Perforce server.<p>
	 * 
	 * Basically a convenience method for IServer.updateJob(job).
	 * 
	 * @deprecated use the IServerResource update method instead if possible.
	 * 
	 * @return possibly-null Perforce server-generated status resulting from operation.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String updateOnServer() throws ConnectionException, RequestException, AccessException;
}
