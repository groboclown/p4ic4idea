/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core.file;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FilePath;
import com.perforce.p4java.impl.generic.core.file.FilePath.PathType;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.option.server.MoveFileOptions;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Defines the basic set of information and operations on a Perforce file
 * under P4Java, and performs as the common currency for a lot of file-list
 * based methods, usually encapsulated as Java lists as both input and output
 * for common server and client file-based methods such as where, sync, add, etc.<p>
 * 
 * The various IFileSpec methods generally correspond closely to the similarly-named
 * fields or attributes given in the general Perforce documentation, and will not
 * be discussed in great detail here. Note that many of them are only set in response
 * to a specific server or client method call, and, in general, only the file's path needs
 * to be set to be usable as input to these methods. Conversely, some of the fields
 * defined below are only relevant for integration actions, resolve results, etc.<p>
 * 
 * The main complexity in this interface is the variety of file spec paths and associated
 * gubbins: in general, a Perforce file specification can have all four or none of
 * depot, client, and local paths plus an "original" path (see below) valid (or some
 * such combination), and the IFileSpec methods allow you to create specifications
 * from a specific type of path, and / or to specify which paths in the specification
 * are valid -- see the IFileSpecPathSpec enum defined below. "Original" file paths
 * are the paths used to create the file spec in the first place, and are generally
 * used as the preferred path for operations (but see below). File specs may not
 * even have original paths, and / or may have all four path types set, depending on
 * the circumstances of the file spec's creation and subsequent history.
 * 
 * Additionally, the getPreferedPath() method allows a user to retrieve the "correct"
 * path based on a simple set of rules if you've lost track of which path is valid.<p>
 * 
 * IFileSpec may be extended for other uses (see e.g. IExtendedFileSpec as used
 * by the IServer.getExtendedFiles() method.<p>
 * 
 * Note that although IFileSpec (indirectly) extends the IServerResource interface, current
 * implementations are not refreshable, updateable, or completable, and isComplete() will
 * currently always return true.<p>
 * 
 * Note also that the field setter methods below have local effect only.
 */

public interface IFileSpec extends IFileOperationResult {
	
	/**
	 * Value that signals that no Perforce file revision is (currently)
	 * associated with a file spec.<p>
	 * 
	 * Note that this is distinct from specifying "#none" or "#0" explicitly,
	 * which is dealt with by the NONE_FILE_REVISION below.
	 */
	final int NO_FILE_REVISION = -1;
	
	/**
	 * String representation for the NO_FILE_REVISION value.
	 */
	final String NO_REVISION_STRING = "";
	
	/**
	 * Value used to signal "none" file revision explicitly. Note that
	 * this is distinct from leaving a FileSpec's revision unspecified
	 * with NO_FILE_REVISION.
	 */
	final int NONE_REVISION = 0;
	
	/**
	 * The string used by the Perforce server to signal no revision specified.
	 */
	final String NONE_REVISION_STRING = "none";
	
	/**
	 * The symbolic value used to signal the head revision.
	 */
	final int HEAD_REVISION = -17;
	
	/**
	 * The string used by the Perforce server to signal a head revision.
	 */
	final String HEAD_REVISION_STRING = "head";
	
	/**
	 * The symbolic value used to signal the "have" revision.
	 */
	final int HAVE_REVISION = -18;
	
	/**
	 * The string used by the Perforce server to signal a "have" revision.
	 */
	final String HAVE_REVISION_STRING = "have";
	
	/**
	 * Get the specific depot, client, local, or original Perforce file path
	 * associated with this file spec. Will be null if no such path exists
	 * for this file spec.
	 * 
	 * @param pathType if null or PathType.ORIGINAL, return the original
	 * 			path for this file spec, otherwise return the specified
	 * 			path for this file spec.
	 * @return possibly-null file path
	 */
	FilePath getPath(PathType pathType);
	
	/**
	 * Set a Perforce file path associated with this file spec. If path or
	 * path.getPathType() is null, the passed-in path type is assumed
	 * to be ORIGINAL.
	 * 
	 * @param path candidate file path; may be null.
	 */
	void setPath(FilePath path);
	
	/**
	 * Convenience method for setPath(new FilePath(PathType.ORIGINAL, pathStr));
	 */
	
	void setOriginalPath(String pathStr);
	
	/**
	 * Convenience method for setPath(new FilePath(PathType.DEPOT, pathStr));
	 */
	void setDepotPath(String pathStr);
	
	/**
	 * Convenience method for setPath(new FilePath(PathType.CLIENT, pathStr));
	 */
	void setClientPath(String pathStr);
	
	/**
	 * Convenience method for setPath(new FilePath(PathType.LOCAL, pathStr));
	 */
	void setLocalPath(String pathStr);
	
	/**
	 * Get an annotated string representation of a Perforce file
	 * path associated with this Perforce file spec. "Annotated" in
	 * this context means any known Perforce version / changelist (etc.)
	 * information is appended in standard Perforce format to the end of the
	 * returned string.
	 * 
	 * @param pathType if null or PathType.ORIGINAL, return the original
	 * 			path for this file spec, otherwise return the specified
	 * 			path for this file spec.
	 * @return possibly-null annotated Perforce file path string representation.
	 */
	
	String getAnnotatedPathString(PathType pathType);
	
	/**
	 * Get the unannotated path string for this Perforce file.
	 * 
	 * @param pathType if null or PathType.ORIGINAL, return the original
	 * 			path for this file spec, otherwise return the specified
	 * 			path for this file spec.
	 * @return possibly-null non-annotated Perforce file path string representation.
	 */
	String getPathString(PathType pathType);
	
	/**
	 * Set a specific Perforce file path for this file spec from a passed-in
	 * string. Any file revision / changelist (etc.) information appended to
	 * the pathStr parameter is stripped off before the path is set.
	 * 
	 * @param pathType if null or PathType.ORIGINAL, sets the original
	 * 			path for this file spec, otherwise sets the specified
	 * 			path for this file spec.
	 * @param pathStr possibly-null, possibly-annotated path string
	 */
	void setPathFromString(PathType pathType, String pathStr);
	
	/**
	 * Convenience method for getPath(ORIGINAL).
	 */
	FilePath getOriginalPath();
	
	/**
	 * Convenience method for getPathString(ORIGINAL).
	 */
	String getOriginalPathString();
	
	/**
	 * Convenience method for getPath(DEPOT).
	 */
	FilePath getDepotPath();
	
	/**
	 * Convenience method for getPathString(DEPOT).
	 */
	String getDepotPathString();
	
	/**
	 * Convenience method for getPath(CLIENT).
	 */
	FilePath getClientPath();
	
	/**
	 * Convenience method for getPathString(CLIENT).
	 */
	String getClientPathString();
	
	/**
	 * Convenience method for getPath(LOCAL).
	 */
	FilePath getLocalPath();
	
	/**
	 * Convenience method for getPathString(LOCAL).
	 */
	String getLocalPathString();
	
	/**
	 * Get the start revision for this file spec, if any. Returns
	 * NO_FILE_REVISION if the revision hasn't been set or is unknown.
	 */
	int getStartRevision();
	
	/**
	 * Set the start revision for this file spec.
	 */
	void setStartRevision(int rev);
	
	/**
	 * Get the end revision for this file spec, if any. Returns
	 * NO_FILE_REVISION if the revision hasn't been set or is unknown.
	 */
	int getEndRevision();
	
	/**
	 * Set the end revision for this file spec.
	 */
	void setEndRevision(int rev);
	
	/**
	 * Get the ID of the changelist associated with this file spec. Returns
	 * IChangelist.UNKNOWN if no changelist ID has been set.
	 */
	int getChangelistId();
	
	/**
	 * Set the changelist ID for this file spec.
	 */
	void setChangelistId(int id);
	
	/**
	 * Get the Perforce date of this file spec. Returns null if no date has been set.
	 */
	Date getDate();
	
	/**
	 * Set the date for this file spec.
	 */
	void setDate(Date date);
	
	/**
	 * Returns true if the associated file has been locked.
	 */
	boolean isLocked();
	
	/**
	 * Set the locked status for this file spec.
	 */
	void setLocked(boolean locked);
	
	/**
	 * Get the diff status for this file spec.
	 */
	String getDiffStatus();
	
	/**
	 * Set the diff status for this file spec.
	 */
	void setDiffStatus(String status);
	
	/**
	 * Get the resolve type for this file spec.
	 */
	String getResolveType();
	
	/**
	 * Set the resolve type for this file spec.
	 */
	void setResolveType(String resolveType);

	/**
	 * Get the content resolve type for this file spec.
	 */
	String getContentResolveType();
	
	/**
	 * Set the content resolve type for this file spec.
	 */
	void setContentResolveType(String contentResolveType);
	
	/**
	 * Get the shelved change for this file spec.
	 */
	int getShelvedChange();
	
	/**
	 * Set the shelved change for this file spec.
	 */
	void setShelvedChange(int shelvedChange);

	/**
	 * Returns the file action associated with this file, if any. Returns
	 * null if no such action has been set.
	 */
	FileAction getAction();
	
	/**
	 * Set the file action for this file spec.
	 */
	void setAction(FileAction action);
	
	/**
	 * Get the label string associated with this file spec, if any. Returns
	 * null if no such label string has been set.
	 */
	String getLabel();
	
	/**
	 * Set the label associated with this file spec.
	 */
	void setLabel(String label);
	
	/**
	 * Returns the file type string for this file spec, or null if not known.
	 */
	String getFileType();
	
	/**
	 * Set the file type for this file spec.
	 */
	void setFileType(String type);
	
	/**
	 * Return the Perforce user name of the file's owner, or null if this
	 * is not set or known.
	 */
	String getUserName();
	
	/**
	 * Set the Perforce user name for this file spec.
	 */
	void setUserName(String userName);
	
	/**
	 * Return the name of the client associated with this file spec, or null
	 * if not set or known.
	 */
	String getClientName();
	
	/**
	 * Set the Perforce client name for this file spec.
	 */
	void setClientName(String clientName);
	
	/**
	 * Return if this file spec is not mapped.
	 */
	boolean isUnmap();
	
	/**
	 * Set the unmap status value for this file spec.
	 */
	void setUnmap(boolean unmap);

	/**
	 * Get the "preferred" path for this file spec. A file spec's preferred
	 * path is defined to be the path specified (in order) the original path,
	 * the depot path, the client path, or the local path.<p>
	 * 
	 * @return possibly-null preferred path
	 */

	FilePath getPreferredPath();
	
	/**
	 * Get the annotated "preferred" path string for this file spec. A file spec's preferred
	 * path is defined to be the path specified by (in order) the original path,
	 * the depot path, the client path, or the local path. This method
	 * appends any relevant revision / changelist (etc.) information to the
	 * path string if it exists.<p>
	 * 
	 * @return possibly-null annotated preferred path string.
	 */
	
	String getAnnotatedPreferredPathString();
	
	/**
	 * Get the (unannotated) "preferred" path string for this file spec. A file spec's preferred
	 * path is defined to be the path specified by (in order) the original path,
	 * the depot path, the client path, or the local path.
	 * 
	 * @return possibly-null annotated preferred path string.
	 */
	String getPreferredPathString();
	
	/**
	 * Useful alias for getPreferredPathString().
	 */
	String toString();
	
	// IFileSpec methods:
	
	/**
	 * Get the file this file spec was integrated from if any. Returns null
	 * if no such file.
	 */
	String getFromFile();
	
	/**
	 * Set the from file for this file spec.
	 */
	void setFromFile(String pathStr);
	
	/**
	 * Return the end "from" revision of the last integration, if any.
	 */
	int getEndFromRev();
	
	/**
	 * Set the end from revision for this file spec.
	 */
	void setEndFromRev(int rev);
	
	/**
	 * Return the start "from" revision of the last integration, if any.
	 */
	int getStartFromRev();
	
	/**
	 * Set the end from revision for this file spec.
	 */
	void setStartFromRev(int rev);
	
	/**
	 * Get the "to" file for the integration, if it exists. Returns null
	 * otherwise.
	 */
	String getToFile();
	
	/**
	 * Set the to file for this file spec.
	 */
	void setToFile(String pathStr);
	
	/**
	 * Return the start "to" revision of the last integration, if any.
	 */
	int getStartToRev();
	
	/**
	 * Set the start to revision for this file spec.
	 */
	void setStartToRev(int rev);
	
	/**
	 * Return the end "from" revision of the last integration, if any.
	 */
	int getEndToRev();
	
	/**
	 * Set the end to revision for this file spec.
	 */
	void setEndToRev(int rev);
	
	/**
	 * Return a string representation of how the last resolve happened.
	 * Returns null if unknown or not relevant.
	 */
	String getHowResolved();
	
	/**
	 * Set the "how resolved" string for this file spec.
	 */
	void setHowResolved(String howStr);
	
	/**
	 * Get the work revision.
	 */
	int getWorkRev();
	
	/**
	 * Set the work revision for this file spec.
	 */
	void setWorkRev(int rev);
	
	/**
	 * Get the other action. Returns null if no such action.
	 */
	FileAction getOtherAction();
	
	/**
	 * Set the other action for this file spec.
	 */
	void setOtherAction(FileAction action);
	
	/**
	 * Get the base revision as reported by integ -o or equivalent. This
	 * may be NO_FILE_REVISION if no base revision exists.
	 */
	int getBaseRev();
	
	/**
	 * Set the base revision on this object. Makes no sense for
	 * general users to call this.
	 */
	void setBaseRev(int rev);
	
	/**
	 * Get the base name as reported by integ -o or equivalent. This
	 * may be null if no base name exists.
	 */
	String getBaseName();
	
	/**
	 * Set the base name on this object. Makes no sense for
	 * general users to call this.
	 */
	void setBaseName(String basename);

	/**
	 * Get the base file as reported by resolve -o or equivalent. This
	 * may be null if no base file exists.
	 */
	String getBaseFile();
	
	/**
	 * Set the base file on this object. Makes no sense for
	 * general users to call this.
	 */
	void setBaseFile(String basefile);

	/**
	 * Get the revision history of this Perforce file.<p>
	 * 
	 * Note that the revision history returned may contain more than one file
	 * if the includeInherited option is true; this is why the return is a map
	 * (keyed on depot file path) of revision lists rather than a simple list.<p>
	 * 
	 * Behavior is undefined if both longOutput and truncatedLongOutput are true. If both
	 * are false, a short form of the description (prepared by the server) is returned.
	 * 
	 * @param maxRevs if positive, return at most maxRev revisions for this file.
	 * @param includeInherited if true, causes inherited file history to be returned as well.
	 * @param longOutput if true, return the full descriptions associated with each revision
	 * @param truncatedLongOutput if true, return only the first 250 characters of each description.
	 * @return a non-null map of lists of the revision data for the file.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(int maxRevs,
							boolean contentHistory, boolean includeInherited, boolean longOutput,
							boolean truncatedLongOutput)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get the revision history of this Perforce file.<p>
	 * 
	 * Note that the revision history returned may contain more than one file
	 * if the includeInherited option is true; this is why the return is a map
	 * (keyed on depot file path) of revision lists rather than a simple list.<p>
	 * 
	 * Behavior is undefined if both longOutput and truncatedLongOutput are true in the 
	 * passed-in GetRevisionHistory object. If both are false, a short form of the description
	 * (prepared by the server) is returned.
	 * 
	 * @param opts GetChangelistDiffs object describing optional parameters; if null, no
	 * 				options are set.
	 * @return a non-null map of lists of the revision data for the file.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(GetRevisionHistoryOptions opts)
														throws P4JavaException;
	
	/**
	 * Get the file annotations associated with this file.
	 * 
	 * @param wsOptions DiffType describing the white space option to be used; if null,
	 * 				use default (no options), otherwise must be one of the whitespace
	 * 				options defined by the isWsOption method on DiffType.
	 * @param allResults if true, include both deleted files and lines no longer present
	 *				at the head revision
	 * @param useChangeNumbers if true, annotate with change numbers rather than
	 * 				revision numbers with each line
	 * @param followBranches if true, follow branches.
	 * @return non-null (but possibly-empty) list of IFileAnnotation objects representing
	 * 					this file's version annotations.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	List<IFileAnnotation> getAnnotations(DiffType wsOptions, boolean allResults,
							boolean useChangeNumbers, boolean followBranches)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get the file annotations associated with this file.
	 * 
	 * @param opts GetFileAnnotations object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null (but possibly-empty) list of IFileAnnotation objects representing
	 * 					this file's version annotations.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	List<IFileAnnotation> getAnnotations(GetFileAnnotationsOptions opts) throws P4JavaException;
	
	/**
	 * Get the contents of this specific Perforce file revision from the Perforce
	 * depot as an InputStream. Note that the contents are retrieved from the
	 * depot, not from the (possibly-changed) local Perforce client workspace copy).
	 * This method is basically a convenience wrapper for the IServer.getFileContents()
	 * method.<p>
	 * 
	 * You should close the InputStream after use in order to release any underlying
	 * stream-related resources. Failure to do this may lead to the proliferation of
	 * temp files or long-term memory wastage or even leaks.
	 * 
	 * @param noHeaderLine if true, suppresses the initial line that displays the file name
	 * 				and revision
	 * @return a non-null but possibly-empty InputStream onto the file's contents.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	public InputStream getContents(boolean noHeaderLine)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get the contents of this specific Perforce file revision from the Perforce
	 * depot as an InputStream. Note that the contents are retrieved from the
	 * depot, not from the (possibly-changed) local Perforce client workspace copy).
	 * This method is basically a convenience wrapper for the IOptionsServer.getFileContents()
	 * method.<p>
	 * 
	 * You should close the InputStream after use in order to release any underlying
	 * stream-related resources. Failure to do this may lead to the proliferation of
	 * temp files or long-term memory wastage or even leaks.
	 * 
	 * @param opts GetFileContents object describing optional parameters; if null, no
	 * 				options are set.
	 * @return a non-null but possibly-empty InputStream onto the file's contents.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	public InputStream getContents(GetFileContentsOptions opts) throws P4JavaException;
	
	/**
	 * Move this file if it's already opened for edit or add (the fromFile) to the destination 
	 * file (the toFile). A file can be moved many times before it is submitted; 
	 * moving it back to its original location will reopen it for edit. The full
	 * semantics of this operation (which can be confusing) are found in the
	 * main 'p4 help' documentation.<p>
	 * 
	 * Note that the status of this file once the move has been performed may be
	 * indeterminate; you should discard this file and use the file returned,
	 * if any, or at least test this file's status.<p>
	 * 
	 * Note that this operation is not supported on servers earlier than 2009.1;
	 * any attempt to use this on earlier servers will result in a RequestException
	 * with a suitable message. Similarly, not all underlying IServer implementations
	 * will work with this either, and will also result in a suitable RequestException.
	 * 
	 * @param changelistId if not IChangelist.UNKNOWN, the files are opened in the numbered
	 *			pending changelist instead of the 'default' changelist.
	 * @param listOnly if true, don't actually perform the move, just return what would
	 * 				happen if the move was performed
	 * @param noClientMove if true, bypasses the client file rename. See main IServer
	 * 			moveFiles comments for restrictions.
	 * @param fileType if not null, the file is reopened as that filetype.
	 * @param toFile the target file.
	 * @return list of IFileSpec objects representing the results of this move
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFileSpec> move(int changelistId, boolean listOnly, boolean noClientMove,
										String fileType, IFileSpec toFile)
							throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Move this file if it's already opened for edit or add (the fromFile) to the destination 
	 * file (the toFile). A file can be moved many times before it is submitted; 
	 * moving it back to its original location will reopen it for edit. The full
	 * semantics of this operation (which can be confusing) are found in the
	 * main 'p4 help' documentation.<p>
	 * 
	 * Note that the status of this file once the move has been performed may be
	 * indeterminate; you should discard this file and use the file returned,
	 * if any, or at least test this file's status.<p>
	 * 
	 * Note that this operation is not supported on servers earlier than 2009.1;
	 * any attempt to use this on earlier servers will result in a RequestException
	 * with a suitable message. Similarly, not all underlying IServer implementations
	 * will work with this either, and will also result in a suitable RequestException.
	 * 
	 * @param toFile the target file.
	 * @param opts GetFileContents object describing optional parameters; if null, no
	 * 				options are set.
	 * @return list of IFileSpec objects representing the results of this move.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IFileSpec> move(IFileSpec toFile, MoveFileOptions opts) throws P4JavaException;

	String getRepoName();

	void setRepoName(String repoName);

	String getSha();

	void setSha(String sha);

	String getBranch();

	void setBranch(String branch);

	String getBlobSha();

	void setBlobSha(String sha);

	String getCommitSha();

	void setCommitSha(String sha);

	String getTreeSha();

	void setTreeSha(String sha);

	List<String> getResolveTypes();

	void setResolveTypes(List<String> types);
}
