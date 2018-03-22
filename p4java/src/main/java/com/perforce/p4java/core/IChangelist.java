/*
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */

package com.perforce.p4java.core;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.server.GetChangelistDiffsOptions;
import com.perforce.p4java.server.callback.IStreamingCallback;

/**
 * Describes a Perforce changelist.<p>
 * 
 * In general, changelists returned from the IServer.getChangelist method (and 
 * any convenience methods wrapping that method) will be complete, refreshable, and
 * updateable, but changelists returned from other methods are not guaranteed to
 * be any of those things.<p>
 * 
 * Note that most of the field setter methods on this interface have local effect only
 * (unless you call update()), and are mostly intended for de-novo changelist creation.
 */

public interface IChangelist extends IServerResource, IChangelistSummary {
	
	/**
	 * Value used to signal an unknown or unallocated changelist ID.
	 */
	
	int UNKNOWN = -1;
	
	/**
	 * Value used to signal a default (pending) changelist
	 */
	
	int DEFAULT = 0;
	
	/**
	 * Changelist types used when running a 'p4 changes' with the -s flag
	 */
	enum Type {
		
		PENDING,
		
		SUBMITTED,
		
		SHELVED;

		/**
		 * Get a lower-case representation of the type;
		 * note the use of the English locale here to work
		 * around issues to do with things like Turkish language
		 * encodings of dotted upper-case "i"'s...
		 * 
		 * @see java.lang.Enum#toString()
		 */
		public String toString() {
			return super.toString().toLowerCase(Locale.ENGLISH);
		}
		
	}
	
	/**
	 * Refresh this changelist directly from the server. All fields including the
	 * files field are refreshed. Will fail with a UnimplementedError being
	 * thrown if this changelist is not refreshable.
	 * 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	void refresh() throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Update the Perforce server's version of this changelist. All known non-null fields
	 * will be updated. Basically, when called with a refresh parameter of false, this is
	 * a convenience method for the IServerResource update().<p>
	 * 
	 * @param refresh if true, does a refresh() behind the scenes after a successful return.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 * 
	 * @deprecated use update optionally followed by refresh()
	 */
	
	void updateOnServer(boolean refresh)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Return a list of Perforce jobs IDs for jobs marked as associated with
	 * this changelist. This method is relatively lightweight compared to the
	 * getJobList method.<p>
	 * 
	 * Note that "associated" here usually means "fixed", but that is not always
	 * the case.
	 * 
	 * @return non-null but potentially empty list of job ID strings
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */

	List<String> getJobIds()
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Return a cached list of job Id's associated with this changelist. This
	 * method is not "live", meaning it's quick and cheap to call, but it does not
	 * necessarily reflect the current jobs associated with the list on the Perforce
	 * server -- it reflects only the jobs associated with the server when the changelist
	 * object was retrieved from the server, or when the last live getJobIdList method
	 * was called. Usually safe to call immediately after retrieving the changelist,
	 * or soon after a live getJobIdList call.
	 * 
	 * @return non-null but potentially empty list of job ID strings
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<String> getCachedJobIdList()
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Return a list of Perforce jobs marked as associated with
	 * this changelist. This method is relatively heavyweight compared to the
	 * getJobID method.<p>
	 * 
	 * Note that "associated" here usually means "fixed", but that is not always
	 * the case.
	 * 
	 * @return non-null but potentially empty list of Perforce jobs
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	List<IJob> getJobs()
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get the list of files associated with this changelist, optionally refreshing
	 * the list from the server.<p>
	 * 
	 * If this is the first time this method has been called on this changelist, or if
	 * refresh is true, the file list is obtained from the server and copied locally
	 * to this object; otherwise, the local copy is returned.<p>
	 * 
	 * Note that the actual file list is returned; this file list is guaranteed
	 * mutable and you can change it "live" (by, for example, deleting files from it
	 * (deleting files is, in fact, the only thing that makes sense from a Perforce point
	 * of view)). Such changes will only be reflected back to the server when a submit is
	 * done; adding files to this list results in undefined behavior.<p>
	 *
	 * The IFileSpec objects returned are not guaranteed to have any fields
	 * except depot path, version, and action valid (and, in fact, if it comes
	 * directly from the server, those are usually the only fields that are valid.<p>
	 * 
	 * Note that the Perforce server side of the submit() processing expects all file lists
	 * to be in canonical depot path form; file specs in the associated submit file list
	 * that don't have a depot spec will be left untouched (this is a Perforce thing,
	 * not a P4Java thing).<p>
	 * 
	 * This is one of the guaranteed "live" method on this interface, and will
	 * return the list as it exists when called (rather than when the underlying
	 * implementation object was created) if refresh is true. This can be an expensive
	 * method to evaluate, so don't use it willy-nilly.
	 * 
	 * @param refresh if true, get a new list from the server rather than return the
	 * 					local copy.
	 * @return a non-null (but possibly-empty) list of files known to be associated with
	 * 				this changelist.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	List<IFileSpec> getFiles(boolean refresh)
				throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get an InputStream onto the file diffs associated with this changelist.<p>
	 * 
	 * This is one of the guaranteed "live" method on this interface, and will
	 * return the diff output as it exists when called (rather than when the underlying
	 * implementation object was created). This can be an expensive method
	 * to evaluate, and can generate reams and reams (and reams) of output,
	 * so don't use it willy-nilly.<p>
	 * 
	 * You should remember to close the returned InputStream when finished with
	 * the stream, in order to release the underlying io resources. Failure to do this
	 * may leave temporary files lying around or cause inefficient memory usage.
	 * 
	 * @return InputStream onto the diff stream. Note that
	 *			while this stream will not be null, it may be empty.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	InputStream getDiffs(DiffType diffType)
				throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get an InputStream onto the file diffs associated with this changelist.<p>
	 * 
	 * This is one of the guaranteed "live" method on this interface, and will
	 * return the diff output as it exists when called (rather than when the underlying
	 * implementation object was created). This can be an expensive method
	 * to evaluate, and can generate reams and reams (and reams) of output,
	 * so don't use it willy-nilly.<p>
	 * 
	 * You should remember to close the returned InputStream when finished with
	 * the stream, in order to release the underlying io resources. Failure to do this
	 * may leave temporary files lying around or cause inefficient memory usage.
	 * 
	 * @param opts GetChangelistDiffs object describing optional parameters; if null, no
	 * 				options are set.
	 * @return InputStream onto the diff stream. Note that
	 *			while this stream will not be null, it may be empty.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	InputStream getDiffsStream(GetChangelistDiffsOptions opts) throws P4JavaException;
		
	/**
	 * Submit this changelist. Will fail with a suitable request exception if this
	 * is not a pending changelist associated with the current client.<p>
	 * 
	 * If the submit is successful, the status of the underlying changelist will be
	 * updated to reflect the new status. Other fields will not be automatically
	 * updated and need to be refreshed with the refresh() method if you need to
	 * access them live.<p>
	 * 
	 * Note that the list of filespecs returned from the submit will contain only
	 * summary filespecs for successful files -- generally only the depot path, action,
	 * and revisions fields will be valid; other fields may be null or undefined
	 * depending on the server and client implementations. That is, do not rely on
	 * the returned filespec list for anything other than depot paths.
	 * 
	 * @param reOpen if true, reopen the submitted files for editing after a successful submit.
	 * @return list of affected file specs and / or info / error messages from the Perforce server
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFileSpec> submit(boolean reOpen)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Submit this changelist and associate it with the passed-in jobs.
	 * Will fail with a suitable request exception if this is not a pending
	 * changelist associated with the current client.<p>
	 * 
	 * If the submit is successful, the status of the underlying changelist will be
	 * updated to reflect the new status. Other fields will not be automatically
	 * updated and need to be refreshed with the refresh() method if you need to
	 * access them live.<p>
	 * 
	 * Note that the list of filespecs returned from the submit will contain only
	 * summary filespecs for successful files -- generally only the depot path, action,
	 * and revisions fields will be valid; other fields may be null or undefined
	 * depending on the server and client implementations. That is, do not rely on
	 * the returned filespec list for anything other than depot paths.<p>
	 * 
	 * Note also that any jobIds and / or job status arguments will override any
	 * jobIds already associated with this object on submission (i.e. jobs in the
	 * local object's job list).
	 * 
	 * @param reOpen if true, reopen the submitted files for editing after a successful submit.
	 * @param jobIds if non-null, contains a list of job IDs for jobs that will have their status
	 * 				changed to fixed or "jobStatus", below. This list will override any existing
	 * 				list attached to the changelist as a result of a refresh operation after
	 * 				fixing a job, etc.
	 * @param jobStatus if jobIds is non-null, contains a string to which
	 * 				the jobs in the jobIds list will be set on a successful submit; if
	 * 				null the jobs will be marked fixed.
	 * @return list of affected file specs and / or info / error messages from the Perforce server
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFileSpec> submit(boolean reOpen, List<String> jobIds, String jobStatus)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Submit this changelist and associate it with any jobs in the passed-in options.
	 * Will fail with a suitable request exception if this is not a pending
	 * changelist associated with the current client.<p>
	 * 
	 * If the submit is successful, the status of the underlying changelist will be
	 * updated to reflect the new status. Other fields will not be automatically
	 * updated and need to be refreshed with the refresh() method if you need to
	 * access them live.<p>
	 * 
	 * Note that the list of filespecs returned from the submit will contain only
	 * summary filespecs for successful files -- generally only the depot path, action,
	 * and revisions fields will be valid; other fields may be null or undefined
	 * depending on the server and client implementations. That is, do not rely on
	 * the returned filespec list for anything other than depot paths.<p>
	 * 
	 * Note also that any jobIds and / or job status arguments given to the SubmitOptions
	 * argument will override any jobIds already associated with this object on submission
	 * (i.e. jobs in the local object's job list).
	 * 
	 * @param opts SubmitOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return list of affected file specs and / or info / error messages from the Perforce server.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IFileSpec> submit(SubmitOptions opts) throws P4JavaException;

	/**
	 * Submit this changelist and associate it with any jobs in the passed-in options.
	 * Will fail with a suitable request exception if this is not a pending
	 * changelist associated with the current client.<p>
	 * 
	 * Note that this method takes an IStreamingCallback parameter, and the results
	 * are sent to the user using the IStreamingCallback handleResult method; see
	 * the IStreamingCallback Javadoc for details. The payload passed to handleResult
	 * is usually the raw map gathered together deep in the RPC protocol layer, and
	 * the user is assumed to have the knowledge and technology to be able to parse
	 * it and use it.<p>
	 *
	 * @since 2012.3
	 * 
	 * @param opts SubmitOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @param callback a non-null IStreamingCallback to be used to process the incoming
	 * 				results.
	 * @param key an opaque integer key that is passed to the IStreamingCallback callback
	 * 				methods to identify the action as being associated with this specific
	 * 				call.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	void submit(SubmitOptions opts, IStreamingCallback callback, int key) throws P4JavaException;
}
