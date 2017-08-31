/**
 *
 */
package com.perforce.p4java.client;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IRepo;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IntegrationOptions;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.core.ListData;
import com.perforce.p4java.impl.mapbased.client.ViewDepotType;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.GetDiffFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.LabelSyncOptions;
import com.perforce.p4java.option.client.LockFilesOptions;
import com.perforce.p4java.option.client.MergeFilesOptions;
import com.perforce.p4java.option.client.ParallelSyncOptions;
import com.perforce.p4java.option.client.PopulateFilesOptions;
import com.perforce.p4java.option.client.ReconcileFilesOptions;
import com.perforce.p4java.option.client.ReopenFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.ResolvedFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.ShelveFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.client.UnlockFilesOptions;
import com.perforce.p4java.option.client.UnshelveFilesOptions;
import com.perforce.p4java.option.server.ListOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.callback.IStreamingCallback;

import java.io.InputStream;
import java.util.List;

/**
 * Extends the lightweight IClientSummary interface to provide a "heavyweight" Perforce
 * client object that has an associated Perforce client views and has the full panoply
 * of Perforce operations defined against it.<p>
 * <p>
 * Perforce clients are described in detail elsewhere, but in summary, a Perforce client object
 * is returned from the Perforce server using one of the getClient(s) methods or by creating a
 * new Perforce client through the newClient server method. Some Perforce operations are done
 * through a Perforce client; some are associated with a Perforce client; and some are done
 * directly through the Perforce server itself.<p>
 * <p>
 * IClient objects are complete, refreshable, and updateable (unlike IClientSummary objects,
 * which are not updateable).
 */

public interface IClient extends IClientSummary {

	/**
	 * Get the Perforce client view associated with this client, if any.
	 *
	 * @return the Perforce client view associated with this client, if any;
	 * null otherwise.
	 */

	ClientView getClientView();

	/**
	 * Set the Perforce client view associated with this client.
	 *
	 * @param clientView new Perforce client view for the client.
	 */
	void setClientView(ClientView clientView);

	/**
	 * Return the IServer object representing the Perforce server associated with this
	 * Perforce client.
	 *
	 * @return the IServer object representing the server associated with this
	 * client, or null if no such server exists or has been set for this client.
	 */

	IServer getServer();

	/**
	 * Set the Perforce server associated with this client.
	 *
	 * @param server the IServer object representing the server associated with this
	 *               client, or null if there's no such server.
	 */
	void setServer(IServer server);

	/**
	 * Sync a Perforce client workspace against the Perforce server.<p>
	 * <p>
	 * Note that this method will fail (throw a RequestException) unless the client
	 * is the associated IServer object's current client.
	 *
	 * @param fileSpecs    files to be synchronized; if empty, synchronize all client files.
	 * @param forceUpdate  if true, forces resynchronization even if the client already
	 *                     has the file, and clobbers writable files.  This flag doesn't affect
	 *                     open files. Corresponds to the p4 sync "-f" flag.
	 * @param noUpdate     causes sync not to update the client workspace, but to
	 *                     display what normally would be updated. Corresponds to the p4 sync "-n" flag.
	 * @param clientBypass bypasses the client file update.  It can be used to
	 *                     make the server believe that a client workspace already has the file.
	 *                     Corresponds to the p4 sync "-k" flag.
	 * @param serverBypass populates the client workspace, but does not update the
	 *                     server to reflect those updates.  Any file that is already synced or
	 *                     opened will be bypassed with a warning message.
	 *                     Corresponds to the p4 sync "-p" flag.
	 * @return non-null list of affected files as IFileSpec elements
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> sync(List<IFileSpec> fileSpecs, boolean forceUpdate, boolean noUpdate,
	                     boolean clientBypass, boolean serverBypass)
			throws ConnectionException, RequestException, AccessException;

	/**
	 * Sync a Perforce client workspace against the Perforce server.<p>
	 *
	 * @param fileSpecs files to be synchronized; if empty, synchronize all client files.
	 * @param syncOpts  SyncOptions object describing sync options; see SyncOptions comments.
	 * @return non-null list of affected files as IFileSpec elements
	 * @throws P4JavaException if any processing errors occur during exection.
	 * @see com.perforce.p4java.option.client.SyncOptions
	 */
	List<IFileSpec> sync(List<IFileSpec> fileSpecs, SyncOptions syncOpts)
			throws P4JavaException;

	/**
	 * Sync a Perforce client workspace against the Perforce server in parallel.<p>
	 *
	 * @param fileSpecs files to be synchronized; if empty, synchronize all client files.
	 * @param syncOpts  SyncOptions object describing sync options; see SyncOptions comments.
	 * @param psyncOpts Options related to parallalisation of sync
	 * @return non-null list of affected files as IFileSpec elements
	 * @throws P4JavaException
	 * @see com.perforce.p4java.option.client.SyncOptions
	 * @see com.perforce.p4java.option.client.ParallelSyncOptions
	 * @since 2017.2
	 */
	List<IFileSpec> syncParallel(List<IFileSpec> fileSpecs, SyncOptions syncOpts, ParallelSyncOptions psyncOpts)
			throws P4JavaException;

	/**
	 * Sync a Perforce client workspace against the Perforce server.<p>
	 * <p>
	 * Note that this method takes an IStreamingCallback parameter, and the results
	 * are sent to the user using the IStreamingCallback handleResult method; see
	 * the IStreamingCallback Javadoc for details. The payload passed to handleResult
	 * is usually the raw map gathered together deep in the RPC protocol layer, and
	 * the user is assumed to have the knowledge and technology to be able to parse
	 * it and use it.<p>
	 *
	 * @param fileSpecs files to be synchronized; if empty, synchronize all client files.
	 * @param syncOpts  SyncOptions object describing sync options; see SyncOptions comments.
	 * @param callback  a non-null IStreamingCallback to be used to process the incoming
	 *                  results.
	 * @param key       an opaque integer key that is passed to the IStreamingCallback callback
	 *                  methods to identify the action as being associated with this specific
	 *                  call.
	 * @throws P4JavaException if any processing errors occur during exection.
	 * @see com.perforce.p4java.option.client.SyncOptions
	 * @since 2012.3
	 */
	void sync(List<IFileSpec> fileSpecs, SyncOptions syncOpts, IStreamingCallback callback, int key)
			throws P4JavaException;

	/**
	 * Sync a Perforce client workspace against the Perforce server in parallel.<p>
	 *
	 * Note that this method takes an IStreamingCallback parameter, and the results
	 * are sent to the user using the IStreamingCallback handleResult method; see
	 * the IStreamingCallback Javadoc for details. The payload passed to handleResult
	 * is usually the raw map gathered together deep in the RPC protocol layer, and
	 * the user is assumed to have the knowledge and technology to be able to parse
	 * it and use it.<p>
	 *
	 * @param fileSpecs files to be synchronized; if empty, synchronize all client files.
	 * @param syncOpts  SyncOptions object describing standard sync options; see SyncOptions comments.
	 * @param callback  a non-null IStreamingCallback to be used to process the incoming
	 *                  results.
	 * @param key       an opaque integer key that is passed to the IStreamingCallback callback
	 *                  methods to identify the action as being associated with this specific
	 *                  call.
	 * @param pSyncOpts The --parallel flag specifies options for for parallel file transfer
	 * @throws P4JavaException
	 * @since 2017.2
	 */
	 void syncParallel(List<IFileSpec> fileSpecs, SyncOptions syncOpts, IStreamingCallback callback,
	                         int key, ParallelSyncOptions pSyncOpts) throws P4JavaException;

	/**
	 * Perform a label sync operation for this client. See the main Perforce
	 * documentation for an explanation of the labelsync operation.
	 *
	 * @param fileSpecs   if non-null, the list of files dor this operation
	 * @param labelName   non-null name of the label to be sync'd
	 * @param noUpdate    if true, just show what would result with the labelsync
	 *                    rather than actually doing it
	 * @param addFiles    if true, add the files in fileSpecs to the label
	 * @param deleteFiles if true, delete the files in fileSpecs from the label
	 * @return non-null list of affected files as IFileSpec elements
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> labelSync(List<IFileSpec> fileSpecs, String labelName, boolean noUpdate,
	                          boolean addFiles, boolean deleteFiles)
			throws ConnectionException, RequestException, AccessException;

	/**
	 * Perform a label sync operation for this client. See the main Perforce
	 * documentation for an explanation of the labelsync operation.
	 *
	 * @param fileSpecs if non-null, the list of files for this operation
	 * @param labelName non-null name of the label to be sync'd
	 * @param opts      possibly-null LabelSyncOptions object describing the
	 *                  specific options for this call
	 * @return non-null list of affected files as IFileSpec elements
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.LabelSyncOptions
	 */

	List<IFileSpec> labelSync(List<IFileSpec> fileSpecs, String labelName, LabelSyncOptions opts)
			throws P4JavaException;

	/**
	 * Create a new changelist for this Perforce client in the associated Perforce server.
	 * The newly-created changelist has no files associated with it (regardless of
	 * whether the passed-in changelist spec has files associated with it); if you
	 * wish to add files to the new changelist, you need to do a reopen on them
	 * explictly after the new changelist is returned.<p>
	 * <p>
	 * The new changelist is returned if the command is successful; this changelist object
	 * will include the new changelist ID.
	 *
	 * @param newChangelist non-null specification for the new changelist; if the
	 *                      newChangelist's ID is not IChangelist.DEFAULT, it is ignored.
	 * @return the new changelist, or null if there was an error or the new changelist
	 * is inaccessible.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	IChangelist createChangelist(IChangelist newChangelist)
			throws ConnectionException, RequestException, AccessException;

	/**
	 * Open one or more Perforce client workspace files for adding to the Perforce server.
	 *
	 * @param fileSpecs    non-null list of files to be opened, in Perforce client or
	 *                     depot or local path format.
	 * @param noUpdate     if true, don't actually do the open, just return the files that
	 *                     would have been opened for addition.
	 * @param changeListId if positive, the opened files are put into the pending
	 *                     changelist identified by changeListId; this changelist must have been
	 *                     previously created. If zero or negative, the file is opened in the 'default'
	 *                     (unnumbered) changelist.
	 * @param fileType     if non-null, the files are added as that filetype.
	 *                     See 'p4 help filetypes' to attempt to make any sense of Perforce file types.
	 * @param useWildcards if true, filenames that contain wildcards are permitted.
	 *                     See the main Perforce documentation for file adding for details.
	 * @return a non-null but possibly-empty list of IFileSpec objects
	 * representing the opened files. Not all fields in these specs
	 * will be valid or set.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> addFiles(List<IFileSpec> fileSpecs, boolean noUpdate, int changeListId,
	                         String fileType, boolean useWildcards)
			throws ConnectionException, AccessException;

	/**
	 * Open one or more Perforce client workspace files for adding to the Perforce server.
	 *
	 * @param fileSpecs non-null list of files to be opened, in Perforce client or
	 *                  depot or local path format.
	 * @param opts      possibly-null AddFilesOptions object specifying method options.
	 * @return a non-null but possibly-empty list of IFileSpec objects
	 * representing the opened files. Not all fields in these specs
	 * will be valid or set.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.AddFilesOptions
	 */
	List<IFileSpec> addFiles(List<IFileSpec> fileSpecs, AddFilesOptions opts)
			throws P4JavaException;

	/**
	 * Open one or more Perforce client workspace files for editing.
	 *
	 * @param fileSpecs          non-null list of files to be opened, in Perforce client or
	 *                           depot or local path format.
	 * @param noUpdate           if true, don't actually do the open, just return the files that
	 *                           would have been opened for edit.
	 * @param bypassClientUpdate if true, the edit bypasses any client file update.
	 *                           Equivalent to the new 2009.2 and later "-k" option. If set true with
	 *                           a Perforce server earlier than 2009.2, will produce a suitable
	 *                           RequestException. Note: this option can cause havoc if you don't
	 *                           use care...
	 * @param changeListId       if positive, the opened files are put into the pending
	 *                           changelist identified by changeListId; this changelist must have been
	 *                           previously created. If zero or negative, the file is opened in the 'default'
	 *                           (unnumbered) changelist.
	 * @param fileType           if non-null, the file is opened as that filetype.
	 *                           Otherwise, the filetype of the previous revision is reused.
	 *                           If the filetype given is a partial filetype, that partial
	 *                           filetype is combined with the previous revision's filetype.
	 *                           See 'p4 help filetypes' to attempt to make any sense of this..
	 * @return a non-null but possibly-empty list of IFileSpec objects
	 * representing the opened files. Not all fields in these specs
	 * will be valid or set.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> editFiles(List<IFileSpec> fileSpecs, boolean noUpdate, boolean bypassClientUpdate,
	                          int changeListId, String fileType)
			throws RequestException, ConnectionException, AccessException;

	/**
	 * Open one or more Perforce client workspace files for editing.
	 *
	 * @param fileSpecs non-null list of files to be opened, in Perforce client or
	 *                  depot or local path format.
	 * @param opts      possibly-null EditFilesOptions object specifying method options.
	 * @return a non-null but possibly-empty list of IFileSpec objects
	 * representing the opened files. Not all fields in these specs
	 * will be valid or set.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.EditFilesOptions
	 */

	List<IFileSpec> editFiles(List<IFileSpec> fileSpecs, EditFilesOptions opts) throws P4JavaException;

	/**
	 * Revert a open Perforce client workspace files back to the revision previously synced
	 * from the Perforce depot, discarding any pending changelists or integrations that
	 * have been made so far.
	 *
	 * @param fileSpecs           non-null (but possibly-empty) list of files to be reverted
	 * @param noUpdate            if true, don't actually do the revert, just return the files that
	 *                            would have been reverted
	 * @param changeListId        if positive, revert only those files in the pending changelist
	 *                            given in changeListId.
	 * @param revertOnlyUnchanged if true, revert only those files which are opened
	 *                            for edit or integrate and are unchanged or missing.
	 * @param noRefresh           if true, bypass the client file refresh of the reverted files.
	 * @return a non-null but possibly-empty list of IFileSpec objects
	 * representing the reverted files. Not all fields in these specs
	 * will be valid or set.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> revertFiles(List<IFileSpec> fileSpecs, boolean noUpdate, int changeListId,
	                            boolean revertOnlyUnchanged, boolean noRefresh)
			throws ConnectionException, AccessException;

	/**
	 * Revert open Perforce client workspace files back to the revision previously synced
	 * from the Perforce depot, discarding any pending changelists or integrations that
	 * have been made so far.
	 *
	 * @param fileSpecs non-null (but possibly-empty) list of files to be reverted
	 * @param opts      possibly-null RevertFilesOptions object specifying method options.
	 * @return a non-null but possibly-empty list of IFileSpec objects
	 * representing the reverted files. Not all fields in these specs
	 * will be valid or set.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.RevertFilesOptions
	 */

	List<IFileSpec> revertFiles(List<IFileSpec> fileSpecs, RevertFilesOptions opts) throws P4JavaException;

	/**
	 * Open Perforce client workspace files for deletion from a Perforce depot.
	 *
	 * @param fileSpecs    non-null list of files to be opened
	 * @param changeListId if positive, the opened files are put into the pending
	 *                     changelist identified by changeListId; this changelist must have been
	 *                     previously created. If zero or negative, the file is opened in the 'default'
	 *                     (unnumbered) changelist.
	 * @param noUpdate     if true, don't actually do the open, just return the files that
	 *                     would have been opened for deletion.
	 * @return a non-null but possibly-empty list of IFileSpec objects
	 * representing the opened files. Not all fields in these specs
	 * will be valid or set
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> deleteFiles(List<IFileSpec> fileSpecs, int changeListId, boolean noUpdate)
			throws ConnectionException, AccessException;

	/**
	 * Open Perforce client workspace files for deletion from a Perforce depot.
	 *
	 * @param fileSpecs non-null list of files to be opened
	 * @param opts      possibly-null DeleteFilesOptions object specifying method options.
	 * @return a non-null but possibly-empty list of IFileSpec objects
	 * representing the opened files. Not all fields in these specs
	 * will be valid or set
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.DeleteFilesOptions
	 */

	List<IFileSpec> deleteFiles(List<IFileSpec> fileSpecs, DeleteFilesOptions opts)
			throws P4JavaException;

	/**
	 * If one or more Perforce file specs is passed-in, return the opened / locked status
	 * of each file (if known) within an IFileSpec object; otherwise
	 * return a list of all files known to be open for this Perforce client workspace.<p>
	 * <p>
	 * The returned list can be modified with the other arguments as described below.
	 *
	 * @param fileSpecs    if non-empty, determine the status of the specified
	 *                     files; otherwise return all qualifying files known to be open
	 * @param maxFiles     if positive, return only the first maxFiles qualifying files.
	 * @param changeListId if positive, return only files associated with the given
	 *                     changelist ID.
	 * @return non-null but possibly-empty list of qualifying open files. Not all fields
	 * in individual file specs will be valid or make sense to be accessed.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> openedFiles(List<IFileSpec> fileSpecs, int maxFiles, int changeListId)
			throws ConnectionException, AccessException;

	/**
	 * Return a list of files open for this client, optionally restricted to a
	 * specific path and / or changelist.<p>
	 * <p>
	 * Note that if a non-null OpenedFilesOptions object is passed to this method,
	 * the object's clientName field is ignored and the name of the client whose
	 * openedFiles method is being called is used instead; similarly, any
	 * allClient options (a.k.a "-a" flags) are also ignored.
	 *
	 * @param fileSpecs if non-empty, determine the status of the specified
	 *                  files; otherwise return all qualifying files known to be open
	 * @param opts      possibly-null OpenedFilesOptions object object specifying method options.
	 * @return non-null but possibly-empty list of qualifying open files. Not all fields
	 * in individual file specs will be valid or make sense to be accessed.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */

	List<IFileSpec> openedFiles(List<IFileSpec> fileSpecs, OpenedFilesOptions opts)
			throws P4JavaException;

	/**
	 * Return a list of all Perforce-managed files and versions that the Perforce server
	 * believes this Perforce client workspace has as of the latest sync. If fileSpecs
	 * is given, this method returns, only information on those files is returned.<p>
	 * <p>
	 * <b>Note that this method will return an empty list unless the client is the
	 * IServer object's current client.</b><p>
	 * <p>
	 * Only the depotFile, revision, clientPath, and localPath fields of the returned
	 * file specs are guaranteed to be valid.
	 *
	 * @param fileSpecs file specs; if empty or null, return all qualifying files
	 * @return non-null (but possibly empty) list of IFileSpec objects for the
	 * passed-in arguments.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.t
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> haveList(List<IFileSpec> fileSpecs)
			throws ConnectionException, AccessException;

	/**
	 * Return a list of all Perforce-managed repos that the Perforce server believes
	 * this Perforce client workspace has as of the latest sync. If fileSpecs is given,
	 * this method returns, only information on those files is returned.<p>
	 * <p>
	 * <b>Note that this method will return an empty list unless the client is the
	 * IServer object's current client.</b><p>
	 * <p>
	 * Only the sha, repo name, and branch name fields of the returned file specs
	 * are guaranteed to be valid.
	 *
	 * @param fileSpecs file specs; if empty or null, return all qualifying files
	 * @return non-null (but possibly empty) list of IFileSpec objects for the
	 * passed-in arguments.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.t
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> graphHaveList(List<IFileSpec> fileSpecs)
			throws ConnectionException, AccessException;

	/**
	 * For each of the passed-in file specs, show how the named file maps through
	 * the client view.<p>
	 * <p>
	 * <b>Note that this method will return an empty list unless the client is the
	 * IServer object's current client.</b><p>
	 * <p>
	 * The returned IFiles contain all three of the depot, client, and local
	 * file paths of the corresponding fileSpec array element where appropriate, or the
	 * usual server-generated error message if there was no corresponding mapping. The
	 * main exception to this is the case when no filespecs are given (i.e. the fileSpecs
	 * array is null or empty), when the mappings for '...' (all files in the current directory
	 * and below) are returned (if they exist).
	 *
	 * @param fileSpecs a list of Perforce file specifications; can be empty or even
	 *                  null (see explanation above).
	 * @return a non-null (but possibly empty) list of IFileSpec for the input
	 * filespecs.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> where(List<IFileSpec> fileSpecs)
			throws ConnectionException, AccessException;

	/**
	 * Reopen Perforce files in a new changelist.
	 *
	 * @param fileSpecs    non-null list of files to be reopened
	 * @param changeListId the new changelist ID to be used
	 * @param fileType     if non-null, the file is reopened as that filetype.
	 *                     If the filetype given is a partial filetype, that partial
	 *                     filetype is combined with the current filetype.
	 *                     See 'p4 help filetypes' to attempt to make any sense of this..
	 * @return list of IFileSpec for each specified file
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> reopenFiles(List<IFileSpec> fileSpecs, int changeListId, String fileType)
			throws ConnectionException, AccessException;

	/**
	 * Reopen Perforce files in a new changelist.
	 *
	 * @param fileSpecs non-null list of files to be reopened.
	 * @param opts      possibly-null ReopenFilesOptions object object specifying method options.
	 * @return list of IFileSpec for each specified file.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<IFileSpec> reopenFiles(List<IFileSpec> fileSpecs, ReopenFilesOptions opts)
			throws P4JavaException;

	/**
	 * Integrate ("merge") from one Perforce filespec to another. The semantics
	 * of Perforce merges are complex and are not explained here; please consult
	 * the main Perforce documentation for file merges and the IntegrationOptions
	 * Javdoc comments for details of the less-commonly-used options.
	 *
	 * @param changeListId    if not IChangelist.UNKNOWN, use this as the target changelist
	 * @param showActionsOnly if true, don't actually do the integration, just return
	 *                        the actions that would have been done
	 * @param branchSpec      if not null, use this as the integration branch specification
	 *                        (as in the -b option to integ).
	 * @param integOpts       detailed integration objects. If null, the method will use default
	 *                        option values as described for the IntegrationOptions constructor,
	 *                        all of which are safe for straightforward merge use.
	 * @param fromFile        if not null, use this as the source Perforce filespec
	 * @param toFile          if not null, use this as the target Perforce filespec
	 * @return non-null list of IFileSpec objects describing the intended or
	 * actual integration actions.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> integrateFiles(int changeListId, boolean showActionsOnly,
	                               IntegrationOptions integOpts, String branchSpec,
	                               IFileSpec fromFile, IFileSpec toFile)
			throws ConnectionException, AccessException;

	/**
	 * Integrate ("merge") from one Perforce filespec to another. The semantics
	 * of Perforce merges are complex and are not explained here; please consult
	 * the main Perforce documentation for file merges and the IntegrateFilesOptions
	 * Javdoc comments for details of the less-commonly-used options.
	 *
	 * @param fromFile   fromFile if not null, use this as the source Perforce filespec
	 * @param toFile     toFile if not null, use this as the target Perforce filespec
	 * @param branchSpec if not null, use this as the integration branch specification
	 *                   (as in the -b option to integ)
	 * @param opts       possibly-null IntegrateFilesOptions object specifying method options
	 * @return non-null list of IFileSpec objects describing the intended or
	 * actual integration actions
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.IntegrateFilesOptions
	 */

	List<IFileSpec> integrateFiles(IFileSpec fromFile, IFileSpec toFile, String branchSpec,
	                               IntegrateFilesOptions opts) throws P4JavaException;

	/**
	 * Integrate one set of files (the 'source') into another (the 'target').
	 * The semantics of Perforce integrate are complex and are not explained here;
	 * please consult the main Perforce documentation for file integrations and
	 * the IntegrateFilesOptions Javdoc comments for details of the less-commonly-used
	 * options.<p>
	 * <p>
	 * Note that depending on the specific options passed-in the fromFile can be
	 * null or one file spec; the toFiles list can be null, one or more file specs.
	 *
	 * @param fromFile if not null, use this as the source file.
	 * @param toFiles  if not null, use this as the list of target files.
	 * @param opts     possibly-null IntegrateFilesOptions object specifying method options.
	 * @return non-null (but possibly empty) list of affected files and / or error messages.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 * @since 2011.2
	 */

	List<IFileSpec> integrateFiles(IFileSpec fromFile, List<IFileSpec> toFiles,
	                               IntegrateFilesOptions opts) throws P4JavaException;

	/**
	 * Automatically resolve the results of a previousPerforce file integration.<p>
	 * <p>
	 * Note that this is currently a very limited version of the full Perforce resolve feature,
	 * corresponding only to (some of) the various auto-resolve features, meaning this method
	 * will never invoke (or need to invoke) end user interaction. More extensive versions
	 * of the resolve command will be surfaced as needed.<p>
	 * <p>
	 * This method notionally returns an IFileSpec, as it's closely
	 * related to the integ method and shares many of its return values, but
	 * there are several limitations in the use of the returned IFileSpecs.
	 * In general, what is returned from this method is a mixture of resolution
	 * info messages (i.e. messages from the server that spell out what would
	 * or did happen during the resolve), and "true" filespecs. In the latter
	 * case, the filespec has a very limited set of valid fields: only client path,
	 * from file, and the from revisions are guaranteed to be valid. In the former
	 * case, since the info messages do NOT correspond one-to-one with the input
	 * file specs that caused the messages, consumers need to explicitly search each
	 * returned info message string for the relevant file path or name. This is an
	 * unfortunate artifact of the Perforce server's implementation of this command.<p>
	 * <p>
	 * Note: results and behavior are undefined if clashing or inconsistent options
	 * are used with this method. In general, the behavior of (e.g.) setting both
	 * acceptYours and acceptTheirs true will be whatever the Perforce server makes
	 * of it (usually an error), but that's not guaranteed....
	 * <p>
	 * Note also that having safeMerge, acceptTheirs, acceptYours, and forceResolve
	 * all set to false results in "-am" behavior.
	 *
	 * @param fileSpecs       files to be resolved; if null or empty, all files marked as
	 *                        needing resolution will be processed
	 * @param safeMerge       if true, only do "safe" resolves, as documented for the p4 "-as" option
	 * @param acceptTheirs    if true, automatically accept "their" changes, as documented
	 *                        for the p4 "-at" option
	 * @param acceptYours     if true, automatically accept "your" changes, as documented
	 *                        for the p4 "-ay" option
	 * @param showActionsOnly if true, don't do the actual resolve, just return the
	 *                        actions that would have been performed for the resolve
	 * @param forceResolve    forces auto-mode resolve to accept the merged file even if
	 *                        there are conflicts. This option results in a merged file that may
	 *                        contain Perforce conflict markers; these markers should be edited out
	 *                        of the file manually before it's submitted (unless you actually want
	 *                        them there...).
	 * @return non-null but possibly-empty list of integration file specs for the resolve;
	 * see note above on the semantics of this list.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> resolveFilesAuto(List<IFileSpec> fileSpecs, boolean safeMerge,
	                                 boolean acceptTheirs, boolean acceptYours, boolean showActionsOnly,
	                                 boolean forceResolve)
			throws ConnectionException, AccessException;

	/**
	 * Automatically resolve the results of a previousPerforce file integration.<p>
	 * <p>
	 * Note that this is currently a very limited version of the full Perforce resolve feature,
	 * corresponding only to (some of) the various auto-resolve features, meaning this method
	 * will never invoke (or need to invoke) end user interaction. More extensive versions
	 * of the resolve command will be surfaced as needed.<p>
	 * <p>
	 * This method notionally returns an IFileSpec, as it's closely
	 * related to the integ method and shares many of its return values, but
	 * there are several limitations in the use of the returned IFileSpecs.
	 * In general, what is returned from this method is a mixture of resolution
	 * info messages (i.e. messages from the server that spell out what would
	 * or did happen during the resolve), and "true" filespecs. In the latter
	 * case, the filespec has a very limited set of valid fields: only client path,
	 * from file, and the from revisions are guaranteed to be valid. In the former
	 * case, since the info messages do NOT correspond one-to-one with the input
	 * file specs that caused the messages, consumers need to explicitly search each
	 * returned info message string for the relevant file path or name. This is an
	 * unfortunate artifact of the Perforce server's implementation of this command.<p>
	 * <p>
	 * Note: results and behavior are undefined if clashing or inconsistent options
	 * are used with this method. In general, the behavior of (e.g.) setting both
	 * acceptYours and acceptTheirs true will be whatever the Perforce server makes
	 * of it (usually an error), but that's not guaranteed....
	 * <p>
	 * Note also that having safeMerge, acceptTheirs, acceptYours, and forceResolve
	 * all set to false in the associated ResolveFilesAutoOptions object results in
	 * "-am" behavior.
	 *
	 * @param fileSpecs files to be resolved; if null or empty, all files marked as
	 *                  needing resolution will be processed
	 * @param opts      possibly-null ResolveFilesAutoOptions object specifying method option
	 * @return non-null but possibly-empty list of integration file specs for the resolve;
	 * see note above on the semantics of this list
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.ResolveFilesAutoOptions
	 */

	List<IFileSpec> resolveFilesAuto(List<IFileSpec> fileSpecs, ResolveFilesAutoOptions opts)
			throws P4JavaException;

	/**
	 * Resolve a file integration by using the contents of the sourceStream InputStream
	 * as the resolve result.<p>
	 * <p>
	 * Note that this method assumes that the target and source resolve makes
	 * sense, and does little or no sanity- or error-checking. In particular, it
	 * will happily accept binary and text integration resolves whether they
	 * make sense or not; you should check the getHowResolved() method on the
	 * returned filespec to ensure that it did what you expected (typically this
	 * means checking that it was resolved using the "edit from" resolution rather
	 * than, say "ignored").<p>
	 * <p>
	 * Use of this method will normally result in an integration edit record
	 * in the target file's history whether you've actually done an edit or not, so
	 * it is up to the consumer to weed out the various straight copy resolve cases
	 * and only use this for cases of more complex merges that actually did involve
	 * an edit (in reality or not).<p>
	 * <p>
	 * Note that the IFileSpec returned will generally not have a
	 * valid depot or client file paths -- you must use the toFile and fromFile methods
	 * on the returns here.
	 *
	 * @param targetFile   non-null file to be resolved.
	 * @param sourceStream non-null InputStream containing the resolved file's new contents.
	 * @return possibly-null IFileSpec representing the result of the resolve
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 */

	IFileSpec resolveFile(IFileSpec targetFile, InputStream sourceStream)
			throws ConnectionException, RequestException, AccessException;

	/**
	 * Resolve a file integration by using the contents of the sourceStream InputStream
	 * as the resolve result.<p>
	 * <p>
	 * Note that this method assumes that the target and source resolve makes
	 * sense, and does little or no sanity- or error-checking. In particular, it
	 * will happily accept binary and text integration resolves whether they
	 * make sense or not; you should check the getHowResolved() method on the
	 * returned filespec to ensure that it did what you expected (typically this
	 * means checking that it was resolved using the "edit from" resolution rather
	 * than, say "ignored").<p>
	 * <p>
	 * Use of this method will normally result in an integration edit record
	 * in the target file's history whether you've actually done an edit or not, so
	 * it is up to the consumer to weed out the various straight copy resolve cases
	 * and only use this for cases of more complex merges that actually did involve
	 * an edit (in reality or not).<p>
	 * <p>
	 * Note that the IFileSpec returned will generally not have a
	 * valid depot or client file paths -- you must use the toFile and fromFile methods
	 * on the returns here.
	 *
	 * @param targetFile      non-null file to be resolved.
	 * @param sourceStream    non-null InputStream containing the resolved file's new contents.
	 * @param useTextualMerge attempt a textual merge even for binary files
	 * @param startFromRev    oldest revision to be resolved, or -1 for no limit
	 * @param endFromRev      newest revision to be resolved, or -1 for no limit
	 * @return possibly-null IFileSpec representing the result of the resolve
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 */

	IFileSpec resolveFile(IFileSpec targetFile, InputStream sourceStream, boolean useTextualMerge,
	                      int startFromRev, int endFromRev)
			throws ConnectionException, RequestException, AccessException;

	/**
	 * Return a list of files resolved but not submitted for this client. Note that
	 * the returned list has only the localPath field set (i.e. depot, client, and
	 * generic paths are not set). This reflects the returned values from the server
	 * itself.
	 *
	 * @param fileSpecs        optional filespecs to be processed
	 * @param showBaseRevision if true, reports the revision used as the base during
	 *                         resolve (this seems to have no effect).
	 * @return list of files that have been resolved but not yet submitted
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> resolvedFiles(List<IFileSpec> fileSpecs, boolean showBaseRevision)
			throws ConnectionException, AccessException;

	/**
	 * Return a list of files resolved but not submitted for this client. Note that
	 * the returned list has only the localPath field set (i.e. depot, client, and
	 * generic paths are not set). This reflects the returned values from the server
	 * itself.
	 *
	 * @param fileSpecs fileSpecs optional filespecs to be processed
	 * @param opts      possibly-null ResolveFilesAutoOptions object specifying method options
	 * @return list of files that have been resolved but not yet submitted
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.ResolvedFilesOptions
	 */

	List<IFileSpec> resolvedFiles(List<IFileSpec> fileSpecs, ResolvedFilesOptions opts)
			throws P4JavaException;

	/**
	 * Lock an opened file against changelist submission.<p>
	 * <p>
	 * The open files named are locked in the Perforce depot, preventing any
	 * user other than the current user on the current client from
	 * submitting changes to the files.  If a file is already locked
	 * then the lock request is rejected.  If no file specs are given
	 * then lock all files currently open in the changelist number given
	 * if it is IChangelist.DEFAULT or > 0.<p>
	 * <p>
	 * Note that the file specs returned are only partially filled out; the
	 * Perforce server seems to only return path information for this command.
	 *
	 * @param fileSpecs    candidate file specs
	 * @param changeListId if IChangelist.DEFAULT or larger than zero, lock
	 *                     all files associated this changelist ID
	 * @return non-null but possibly-empty list of locked file specs or errors
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> lockFiles(List<IFileSpec> fileSpecs, int changeListId)
			throws ConnectionException, AccessException;

	/**
	 * Lock an opened file against changelist submission.<p>
	 * <p>
	 * The open files named are locked in the Perforce depot, preventing any
	 * user other than the current user on the current client from
	 * submitting changes to the files.  If a file is already locked
	 * then the lock request is rejected.  If no file specs are given
	 * then lock all files currently open in the changelist number given
	 * if it is IChangelist.DEFAULT or > 0.<p>
	 * <p>
	 * Note that the file specs returned are only partially filled out; the
	 * Perforce server seems to only return path information for this command.
	 *
	 * @param fileSpecs candidate file specs
	 * @param opts      possibly-null LockFilesOptions object specifying method options
	 * @return non-null but possibly-empty list of locked file specs or errors
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.LockFilesOptions
	 */

	List<IFileSpec> lockFiles(List<IFileSpec> fileSpecs, LockFilesOptions opts)
			throws P4JavaException;

	/**
	 * Release locked files but leave them open.<p>
	 * <p>
	 * If the files are open in a specific pending changelist other than
	 * 'default', then the changeListId parameter is required to specify the
	 * pending changelist.  If no file name is given then all files in the
	 * designated changelist are unlocked.<p>
	 * <p>
	 * Note that the file specs returned are only partially filled out; the
	 * Perforce server seems to only return path information for this command.
	 *
	 * @param fileSpecs    candidate file specs
	 * @param changeListId if IChangelist.DEFAULT or larger than zero, lock
	 *                     all files associated this changelist ID
	 * @param force        force the lock on non-owned filespecs. Requires appropriate
	 *                     permissions.
	 * @return non-null but possibly-empty list of unlocked file specs or errors
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> unlockFiles(List<IFileSpec> fileSpecs, int changeListId, boolean force)
			throws ConnectionException, AccessException;

	/**
	 * Release locked files but leave them open.<p>
	 * <p>
	 * Note that the file specs returned are only partially filled out; the
	 * Perforce server seems to only return path information for this command.
	 *
	 * @param fileSpecs candidate file specs
	 * @param opts      possibly-null UnlockFilesOptions object specifying method options
	 * @return non-null but possibly-empty list of unlocked file specs or errors
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 */

	List<IFileSpec> unlockFiles(List<IFileSpec> fileSpecs, UnlockFilesOptions opts)
			throws P4JavaException;

	/**
	 * Return a list of files that differ in some (arbitrarily complex)
	 * way from depot. See the help documentation for the p4 diff command
	 * using the "-sx" (-sa, -sl, etc.) options for a full discussion
	 * of the options used below.<p>
	 * <p>
	 * Note that this method returns lists of files, and is not useful
	 * to get the diffs themselves (at least not directly).<p>
	 * <p>
	 * Note that the diff status returned for the unopenedWithStatus option
	 * is retrievable from the filespec with the getDiffStatus() method.<p>
	 * <p>
	 * Note that you must have at least one of the various "-s" options set
	 * to true.
	 *
	 * @param maxFiles
	 * @param fileSpecs              candidate file specs; may be null.
	 * @param diffNonTextFiles       include non-text files in the diff lists
	 * @param openedDifferentMissing corresponds to the "-sa" option for the p4 command line.
	 * @param openedForIntegrate     corresponds to the "-sb" option for the p4 command line.
	 * @param unopenedMissing        corresponds to the "-sd" option for the p4 command line.
	 * @param unopenedDifferent      corresponds to the "-se" option for the p4 command line.
	 * @param unopenedWithStatus     corresponds to the "-sl" option for the p4 command line.
	 * @param openedSame             corresponds to the "-sr" option for the p4 command line.
	 * @return non-null but possibly-empty list of qualifying filepecs.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> getDiffFiles(List<IFileSpec> fileSpecs, int maxFiles, boolean diffNonTextFiles,
	                             boolean openedDifferentMissing, boolean openedForIntegrate, boolean unopenedMissing,
	                             boolean unopenedDifferent, boolean unopenedWithStatus, boolean openedSame)
			throws ConnectionException, RequestException, AccessException;

	/**
	 * Return a list of files that differ in some (arbitrarily complex)
	 * way from depot. See the help documentation for the p4 diff command
	 * using the "-sx" (-sa, -sl, etc.) options for a full discussion
	 * of the options used below.<p>
	 * <p>
	 * Note that this method returns lists of files, and is not useful
	 * to get the diffs themselves (at least not directly).<p>
	 * <p>
	 * Note that the diff status returned for the unopenedWithStatus option
	 * is retrievable from the filespec with the getDiffStatus() method.<p>
	 * <p>
	 * Note that you must have at least one of the various "-s" options set
	 * to true.
	 *
	 * @param fileSpecs candidate file specs; may be null.
	 * @param opts      possibly-null GetDiffFilesOptions object specifying method options
	 * @return non-null but possibly-empty list of qualifying filepecs.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 */

	List<IFileSpec> getDiffFiles(List<IFileSpec> fileSpecs, GetDiffFilesOptions opts)
			throws P4JavaException;

	/**
	 * Shelve files in a changelist.
	 *
	 * @param fileSpecs    list of files to be shelved; if null or empty, shelve all files
	 *                     in the changelist
	 * @param changelistId changelistId ID of the changelist containing the files to be shelved.
	 *                     Can not be IChangelist.DEFAULT or IChangelist.UNKNOWN (both of
	 *                     which will cause server usage errors to be returned if used).
	 * @param opts         possibly-null ShelveFilesOptions object specifying method options
	 * @return non-null but possibly empty list of file specs representing the
	 * server's response
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.ShelveFilesOptions
	 */

	List<IFileSpec> shelveFiles(List<IFileSpec> fileSpecs, int changelistId, ShelveFilesOptions opts)
			throws P4JavaException;

	/**
	 * Unshelve file(s) from a shelf.
	 *
	 * @param fileSpecs          list of files to be unshelved; if null or empty, shelve all files
	 *                           in the changelist
	 * @param sourceChangelistId id of changelist containing shelved files to unshelve
	 * @param targetChangelistId id of changelist unshelved files will be placed in
	 * @param opts               possibly-null UnshelveFilesOptions object specifying method options
	 * @return non-null but possibly empty list of file specs representing the
	 * server's response
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 */

	List<IFileSpec> unshelveFiles(List<IFileSpec> fileSpecs, int sourceChangelistId,
	                              int targetChangelistId, UnshelveFilesOptions opts) throws P4JavaException;

	/**
	 * Update/replace/delete shelved file(s) from a pending changelist.
	 *
	 * @param changelistId ID of the changelist containing the files to be shelved.
	 *                     Can not be IChangelist.DEFAULT or IChangelist.UNKNOWN (both of
	 *                     which will cause server usage errors to be returned if used).
	 * @param fileSpecs    list of files to be shelved; if null or empty, shelve all files
	 *                     in the changelist
	 * @param forceUpdate  if true, update the named shelved files.
	 * @param replace      if true, replace the named shelved files.
	 * @param discard      if truem discard the named shelved files.
	 * @return non-null but possibly empty list of file specs representing the
	 * server's response.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */
	List<IFileSpec> shelveChangelist(int changelistId,
	                                 List<IFileSpec> fileSpecs, boolean forceUpdate, boolean replace,
	                                 boolean discard) throws ConnectionException, RequestException,
			AccessException;

	/**
	 * Shelve the file(s) in a pending changelist.<p>
	 * <p>
	 * This performs a 'p4 shelve -i' command when called.
	 *
	 * @param list non-null changelist to be shelved.
	 * @return non-null but possibly empty list of file specs representing the
	 * server's response.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */
	List<IFileSpec> shelveChangelist(IChangelist list)
			throws ConnectionException, RequestException, AccessException;

	/**
	 * Unshelve file(s) from a shelved changelist
	 *
	 * @param shelveChangelistId id of changelist containing shelved files to unshelve (-s)
	 * @param fileSpecs          optional list of file specs to limit unshelving to
	 * @param clientChangelistId id of changelist to unshelve the files into (-c)
	 * @param forceOverwrite     force clobbering of files currently writeable but unopened (-f)
	 * @param previewOnly        don't actually unshelve but get the file specs that would be
	 *                           unshelved (-n)
	 * @return non-null but possibly empty list of file specs representing the
	 * server's response.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */
	List<IFileSpec> unshelveChangelist(int shelveChangelistId,
	                                   List<IFileSpec> fileSpecs, int clientChangelistId,
	                                   boolean forceOverwrite, boolean previewOnly)
			throws ConnectionException, RequestException, AccessException;

	/**
	 * Submit a shelved changelist without transferring files or modifying the
	 * workspace. The shelved change must be owned by the person submitting the
	 * change, but the client may be different. However, files shelved to a
	 * stream target may only be submitted by a stream client that is mapped to
	 * the target stream. In addition, files shelved to a non-stream target
	 * cannot be submitted by a stream client. To submit a shelved change, all
	 * files in the shelved change must be up to date and resolved. No files may
	 * be open in any workspace at the same change number. Client submit options
	 * (ie revertUnchanged, etc) will be ignored. If the submit is successful,
	 * the shelved change and files are cleaned up, and are no longer available
	 * to be unshelved or submitted.<p>
	 * <p>
	 * This performs a 'p4 submit -e shelvedChange#' command when called.
	 *
	 * @param shelvedChangelistId the shelved changelist to be submited.
	 * @return non-null but possibly empty list of file specs representing the
	 * server's response.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 * @since 2013.2
	 */
	List<IFileSpec> submitShelvedChangelist(int shelvedChangelistId) throws P4JavaException;

	/**
	 * Schedule resolve and integration actions to make the target file identical to
	 * the source file based only on the differences between the two (i.e. ignoring
	 * prior integration history).<p>
	 * <p>
	 * Corresponds to the 'p4 copy' command.
	 *
	 * @param fromFile   if not null, use this as the source file.
	 * @param toFile     if not null, use this as the target file.
	 * @param branchSpec if not null, use this as the copy branch specification.
	 * @param opts       possibly-null CopyFilesOptions object specifying method options.
	 * @return non-null (but possibly empty) list of affected files and / or error messages.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 * @since 2011.1
	 */
	List<IFileSpec> copyFiles(IFileSpec fromFile, IFileSpec toFile, String branchSpec,
	                          CopyFilesOptions opts) throws P4JavaException;

	/**
	 * Copies one set of files (the 'source') into another (the	'target').<p>
	 * <p>
	 * Note that depending on the specific options passed-in the fromFile can be
	 * null or one file spec; the toFiles list can be null, one or more file specs.
	 * The full semantics of this operation are found in the main 'p4 help copy'
	 * documentation.
	 *
	 * @param fromFile if not null, use this as the source file.
	 * @param toFiles  if not null, use this as the list of target files.
	 * @param opts     possibly-null CopyFilesOptions object specifying method options.
	 * @return non-null (but possibly empty) list of affected files and / or error messages.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 * @since 2011.2
	 */
	List<IFileSpec> copyFiles(IFileSpec fromFile, List<IFileSpec> toFiles,
	                          CopyFilesOptions opts) throws P4JavaException;

	/**
	 * Merges changes from one set of files (the 'source') into another (the 'target').
	 * It is a simplified form of the 'p4 integrate' command. The semantics of
	 * Perforce merges are complex and are not explained here; please consult
	 * the main Perforce documentation for file merges and the MergeFilesOptions
	 * Javdoc comments for details of the less-commonly-used options.<p>
	 * <p>
	 * Note that depending on the specific options passed-in the fromFile can be
	 * null or one file spec; the toFiles list can be null, one or more file specs.
	 *
	 * @param fromFile if not null, use this as the source file.
	 * @param toFiles  if not null, use this as the list of target files.
	 * @param opts     possibly-null MergeFilesOptions object specifying method options
	 * @return non-null list of IFileSpec objects describing the intended or
	 * actual merge actions
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @since 2011.2
	 */

	List<IFileSpec> mergeFiles(IFileSpec fromFile, List<IFileSpec> toFiles,
	                           MergeFilesOptions opts) throws P4JavaException;

	/**
	 * Open files for add, delete, and/or edit to reconcile client with workspace
	 * changes made outside of Perforce.<p>
	 * <p>
	 * 'p4 reconcile' finds unopened files in a client's workspace and detects the
	 * following:<p>
	 * <p>
	 * 1. files in depot missing from workspace, but still on have list.<p>
	 * 2. files on workspace that are not in depot.<p>
	 * 3. files modified in workpace that are not opened for edit.<p>
	 * <p>
	 * By default, the files matching each condition above in the path are reconciled
	 * by opening files for delete (scenario 1), add (scenario 2), and/or edit (scenario 3).
	 * The -e, -a, and -d flags may be used to limit to a subset of these operations.
	 * If no file arguments are given, reconcile and status default to the current
	 * working directory.
	 *
	 * @param fileSpecs non-null list of files to be opened, in Perforce client or
	 *                  depot or local path format.
	 * @param opts      possibly-null ReconcileFilesOptions object specifying method options.
	 * @return a non-null but possibly-empty list of IFileSpec objects
	 * representing the opened files. Not all fields in these specs
	 * will be valid or set.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 * @see com.perforce.p4java.option.client.ReconcileFilesOptions
	 * @since 2012.2
	 */
	List<IFileSpec> reconcileFiles(List<IFileSpec> fileSpecs, ReconcileFilesOptions opts)
			throws P4JavaException;

	/**
	 * Branches a set of files (the 'source') into another depot location (the
	 * 'target') in a single step. The new files are created immediately, without
	 * requiring a 'p4 submit' or a client workspace.<p>
	 * <p>
	 * Note that depending on the specific options passed-in the fromFile can be
	 * null or one file spec; the toFiles list can be null, one or more file specs.
	 * The full semantics of this operation are found in the main 'p4 help populate'
	 * documentation.
	 *
	 * @param fromFile if not null, use this as the source file.
	 * @param toFiles  if not null, use this as the list of target files.
	 * @param opts     possibly-null PopulateFilesOptions object specifying method options.
	 * @return non-null (but possibly empty) list of affected files and / or error messages.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 * @since 2012.3
	 */
	List<IFileSpec> populateFiles(IFileSpec fromFile, List<IFileSpec> toFiles,
	                              PopulateFilesOptions opts) throws P4JavaException;

	/**
	 * Gets the repos mapped within the client's view.
	 *
	 * @return list of repos
	 * @throws ConnectionException
	 * @throws RequestException
	 * @throws AccessException
	 * @since 2017.1
	 */
	List<IRepo> getRepos() throws ConnectionException, RequestException, AccessException;

	/**
	 * Returns the ViewDepotType set in this client spec
	 *
	 * @return ViewDepotType
	 * @since 2017.1
	 */
	ViewDepotType getViewDepotType();

	/**
	 * Returns list data filtered by client
	 *
	 * @param fileSpecs List of File specs
	 * @param options List options
	 * @return ListData
	 * @throws P4JavaException
	 * @since 2017.2
	 */
	ListData getListData(List<IFileSpec> fileSpecs, ListOptions options) throws P4JavaException;
}
