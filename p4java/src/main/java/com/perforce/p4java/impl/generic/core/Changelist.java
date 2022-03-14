/**
 *
 */
package com.perforce.p4java.impl.generic.core;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.option.server.GetChangelistDiffsOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.callback.IStreamingCallback;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// p4ic4idea: use server messages
import com.perforce.p4java.impl.mapbased.rpc.msg.ServerMessage;
import com.perforce.p4java.impl.mapbased.rpc.msg.ServerMessage.SingleServerMessage;
import com.perforce.p4java.server.ISingleServerMessage;

// p4ic4idea: add Collections for extra fun.
import java.util.Collections;

/**
 * Simple default generic implementation class for the IChangelist interface.
 */

public class Changelist extends ChangelistSummary implements IChangelist {

	protected IOptionsServer serverImpl = null;
	protected List<IFileSpec> fileSpecs = null;
	protected List<String> jobIds = null;

	/**
	 * The default description string for new changelists created by
	 * newChangelist.
	 */
	public static final String DEFAULT_DESCRIPTION = "New changelist created by P4Java";

	/**
	 * Return a new local Changelist object with default values. Default values
	 * for all fields not mentioned in the parameter list are as given for the
	 * default Changelist and ChangelistSummary default constructors; the main
	 * exception is the user name, which is filled in with the current user name.<p>
	 * <p>
	 * Note that this object is a local object only -- you must subsequently call
	 * the client's createChangelist method to also create it on the server (or use
	 * the Factory.createChangelist convenience method).<p>
	 *
	 * @param server      non-null IServer object to be associated with this changelist.
	 * @param clientName  non-null name of the client to be associated with this changelist
	 * @param description if not null, the changelist description string; if null, defaults to
	 *                    Changelist.DEFAULT_DESCRIPTION.
	 * @return new local Changelist object.
	 */
	public static Changelist newChangelist(IServer server, String clientName,
	                                       String description) {
		if (server == null) {
			throw new NullPointerError("null server passed to Changelist.newChangelist");
		}
		if (!(server instanceof Server)) {
			throw new P4JavaError(
					"IOptionsServer passed to Changelist.newChangelist does not implement 'Server' class");
		}
		if (clientName == null) {
			throw new NullPointerError("null client name passed to Changelist.newChangelist");
		}

		return new Changelist(
				IChangelist.UNKNOWN,
				clientName,
				server.getUserName(),
				ChangelistStatus.NEW,
				null,
				description == null ? DEFAULT_DESCRIPTION : description,
				false,
				(Server) server
		);
	}

	/**
	 * Return a new local Changelist object with default values by calling
	 * newChangelist with server and client name values taken from the passed-in
	 * client object.
	 * <p>
	 * Note that this object is a local object only -- you must subsequently call
	 * the client's createChangelist method to also create it on the server (or use
	 * the Factory.createChangelist convenience method).<p>
	 *
	 * @param client      non-null client to be associated with
	 * @param description if not null, the changelist description string; if null, defaults to
	 *                    Changelist.DEFAULT_DESCRIPTION.
	 * @return new local Changelist object.
	 */
	public static Changelist newChangelist(IClient client, String description) {
		if (client == null) {
			throw new NullPointerError("null client passed to Changelist.newChangelist");
		}
		if (client.getServer() == null) {
			throw new NullPointerError(
					"client has no server associated with it in Changelist.newChangelist");
		}

		return newChangelist(client.getServer(), client.getName(), description);
	}

	/**
	 * Default constructor; calls default superclass constructor.<p>
	 * <p>
	 * Actual users of this constructor need to ensure that the super-super-class
	 * ServeResource fields are set appropriately after calling this constructor.
	 */
	public Changelist() {
		super();
	}

	/**
	 * Construct a changelist implementation given an explicit set of initial field values.<p>
	 * <p>
	 * This constructor requires a Server object as its serverImpl parameter;
	 * note that any server object returned by the ServerFactory will work, as long
	 * as it's downcast to Server. If it doesn't cast cleanly, then it is not
	 * suitable for use here.
	 */
	public Changelist(int id, String clientId, String username,
	                  ChangelistStatus status, Date date, String description,
	                  boolean shelved, Server serverImpl) {
		super(false, true, true, true, serverImpl);
		this.id = id;
		this.clientId = clientId;
		this.username = username;
		this.status = status;
		this.date = date;
		this.description = description;
		this.shelved = shelved;
		this.serverImpl = serverImpl;
	}

	/**
	 * Construct a changelist implementation given an explicit set of initial field values.<p>
	 * <p>
	 * This constructor requires a Server object as its serverImpl parameter;
	 * note that any server object returned by the ServerFactory will work, as long
	 * as it's downcast to Server. If it doesn't cast cleanly, then it is not
	 * suitable for use here.
	 */
	public Changelist(int id, String clientId, String username,
	                  ChangelistStatus status, Date date, String description,
	                  boolean shelved, Server serverImpl, Visibility visibility) {
		super(false, true, true, true, serverImpl);
		this.id = id;
		this.clientId = clientId;
		this.username = username;
		this.status = status;
		this.date = date;
		this.description = description;
		this.shelved = shelved;
		this.serverImpl = serverImpl;
		this.visibility = visibility;
	}

	/**
	 * Construct a new Changelist using the passed-in changelist as a template.
	 * If summary and server are not null and refresh is true, perform a refresh
	 * from the Perforce server to initialize the full changelist.<p>
	 * <p>
	 * If changelist is null, this is equivalent to calling the default constructor.
	 *
	 * @throws ConnectionException if the Perforce server is unreachable or is not connected.
	 * @throws RequestException    if the Perforce server encounters an error during its processing of
	 *                             the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */
	public Changelist(IChangelistSummary summary, IOptionsServer server, boolean refresh)
			throws ConnectionException, RequestException, AccessException {
		super(summary);
		super.setRefreshable(true);
		super.updateable = true;
		super.server = server;
		//Set server impl if specified server is an impl
		if (server instanceof Server) {
			this.serverImpl = server;
		}
		if ((summary != null) && (server != null) && refresh) {
			this.refresh();
		}
	}

	@Deprecated
	public Changelist(IChangelistSummary summary, IServer server, boolean refresh)
			throws ConnectionException, RequestException, AccessException {
		this(summary, (IOptionsServer)server, refresh);
	}

	/**
	 * Construct a changelist impl from the passed-in map and serverImpl parameters.
	 * Calls super(map, false, serverImpl) and additionally sets job ids associated
	 * with this changelist, if any.<p>
	 * <p>
	 * This constructor requires a Server object as its serverImpl parameter;
	 * note that any server object returned by the ServerFactory will work, as long
	 * as it's downcast to Server. If it doesn't cast cleanly, then it is not
	 * suitable for use here.
	 */
	public Changelist(Map<String, Object> map, IOptionsServer serverImpl) {
		super(map, false, serverImpl);
		this.serverImpl = serverImpl;

		if (map != null) {
			if (map.containsKey(JOBS_KEY + "0")) {
				// Get (and cache) the associated job IDs for this changelist

				this.jobIds = new ArrayList<String>();

				for (int job = 0; job >= 0; job++) {
					String jobId = (String) map.get(JOBS_KEY + job);

					if (jobId == null) {
						break;
					} else {
						this.jobIds.add(jobId);
					}
				}
			}
		}
	}

	@Deprecated
	public Changelist(Map<String, Object> map, IServer serverImpl) {
		this(map, (IOptionsServer)serverImpl);
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#isShelved()
	 */
	@Override
	public boolean isShelved() {
		return this.shelved;
	}

	/**
	 * Set the changelist as shelved or not shelved
	 */
	@Override
	public void setShelved(boolean shelved) {
		this.shelved = shelved;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String getClientId() {
		return clientId;
	}

	@Override
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public ChangelistStatus getStatus() {
		return status;
	}

	@Override
	public void setStatus(ChangelistStatus status) {
		this.status = status;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String setDescription(String description) {
		String oldVal = this.description;
		this.description = description;
		return oldVal;
	}

	public IServer getServer() {
		return serverImpl;
	}

	public void setServerImpl(Server serverImpl) {
		this.serverImpl = serverImpl;
	}

	@Override
	public List<IFileSpec> getFiles(boolean refresh)
			throws ConnectionException, RequestException,
			AccessException {
		return getFiles(refresh, false);
	}

	public List<IFileSpec> getFiles(boolean refresh, boolean bypassServer)
			throws ConnectionException, RequestException,
			AccessException {

		if (!refresh && (this.fileSpecs != null)) {
			return this.fileSpecs;
		}

		// Create empty file list when currently null and bypass server
		// is true and refresh is false
		if (bypassServer && !refresh && this.fileSpecs == null) {
			this.fileSpecs = new ArrayList<IFileSpec>();
			return this.fileSpecs;
		}

		if (this.serverImpl == null) {
			throw new RequestException(
					"Changelist not associated with a Perforce server");
		}

		// We need to special-case the default changelist:

		if (this.id == IChangelist.DEFAULT) {
			// Use the "opened" command and list all files for this client
			// in the default changelist. This can be expensive... (HR)

			List<IFileSpec> openList = this.serverImpl.getOpenedFiles(
					null, false, this.clientId, 0, 0);
			List<IFileSpec> defList = new ArrayList<IFileSpec>();
			if (openList != null) {
				for (IFileSpec fSpec : openList) {
					if (fSpec.getChangelistId() == IChangelist.DEFAULT) {
						defList.add(fSpec);
					}
				}
			}
			this.fileSpecs = defList;
		} else {
			this.fileSpecs = this.serverImpl.getChangelistFiles(this.id);
		}

		return this.fileSpecs;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#getDiffs(com.perforce.p4java.core.file.DiffType)
	 */
	@Override
	public InputStream getDiffs(DiffType diffType)
			throws ConnectionException, RequestException, AccessException {
		if (this.serverImpl == null) {
			throw new RequestException("Changelist not associated with a Perforce server");
		}
		return this.serverImpl.getChangelistDiffs(this.id, diffType);
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#getDiffsStream(com.perforce.p4java.option.server.GetChangelistDiffsOptions)
	 */
	@Override
	public InputStream getDiffsStream(GetChangelistDiffsOptions opts) throws P4JavaException {
		if (this.serverImpl == null) {
			throw new RequestException("Changelist not associated with a Perforce server");
		}

		return this.serverImpl.getChangelistDiffs(this.id, opts);
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#getJobIds()
	 */
	@Override
	public List<String> getJobIds()
			throws ConnectionException, RequestException, AccessException {
		List<String> idList = new ArrayList<String>();

		// Don't do this if we're a new (or unknown) changelist:

		if ((serverImpl != null) && (this.id != IChangelist.UNKNOWN)) {

			List<IFix> fixList = this.serverImpl.getFixList(null, this.id, null, false, 0);

			if (fixList != null) {
				for (IFix fix : fixList) {
					if (fix != null) {
						if (fix.getJobId() != null) {
							idList.add(fix.getJobId());
						}
					}
				}

				this.jobIds = idList;
			}
		}
		return idList;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#getCachedJobIdList()
	 */
	@Override
	public List<String> getCachedJobIdList()
			throws ConnectionException, RequestException, AccessException {
		if (this.jobIds != null) {
			return this.jobIds;
		}

		return new ArrayList<String>();
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#getJobs()
	 */
	@Override
	public List<IJob> getJobs()
			throws ConnectionException, RequestException, AccessException {
		List<String> idList = getJobIds();

		List<IJob> jobList = new ArrayList<IJob>();

		if ((idList != null) && (this.serverImpl != null)) {
			for (String id : idList) {
				jobList.add(this.serverImpl.getJob(id));
			}
		}

		return jobList;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#submit(boolean, java.util.List, java.lang.String)
	 */
	@Override
	public List<IFileSpec> submit(boolean reOpen, List<String> jobIds, String jobStatus)
			throws ConnectionException, RequestException, AccessException {
		try {
			return submit(new SubmitOptions()
					.setJobIds(jobIds)
					.setJobStatus(jobStatus)
					.setReOpen(reOpen));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			// p4ic4idea: use original exception
			throw new RequestException(exc);
		}
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#submit(com.perforce.p4java.option.changelist.SubmitOptions)
	 */
	@Override
	public List<IFileSpec> submit(SubmitOptions opts) throws P4JavaException {

		if (this.serverImpl == null) {
			throw new RequestException("Changelist not associated with a Perforce server");
		}

		Map<String, Object> inMap = getInMap(opts);
		List<Map<String, Object>> retMaps = this.serverImpl.execMapCmdList(
				CmdSpec.SUBMIT,
				Parameters.processParameters(
						opts, null, "-i", this.serverImpl),
				inMap);

		List<IFileSpec> fileList = new ArrayList<IFileSpec>();

		// Note the special-casing going on below; this is an artefact of
		// the way the submit returns are just different enough to have to be
		// treated slightly differently from the normal common-and-garden
		// server returns for file-oriented operations -- HR.

		if (retMaps != null) {
			for (Map<String, Object> map : retMaps) {
				if (map.get("submittedChange") != null) {
					this.id = new Integer((String) map.get("submittedChange"));
					this.status = ChangelistStatus.SUBMITTED;

					// p4ic4idea: use ServerMessage for more robust error reporting.
					SingleServerMessage msg = new SingleServerMessage("Submitted as change " + this.id);
					fileList.add(new FileSpec(FileSpecOpStatus.INFO,
							new ServerMessage(
									Collections.<ISingleServerMessage>singletonList(msg)),
							map));
				} else if (map.get("locked") != null) {
					// disregard this message for now -- FIXME -- HR
				} else {
					fileList.add(ResultListBuilder.handleFileReturn(map, serverImpl));
				}
			}
		}

		return fileList;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#submit(com.perforce.p4java.option.changelist.SubmitOptions,
	 * com.perforce.p4java.server.callback.IStreamingCallback, int)
	 */
	@Override
	public void submit(SubmitOptions opts, IStreamingCallback callback, int key) throws P4JavaException {

		if (this.serverImpl == null) {
			throw new RequestException("Changelist not associated with a Perforce server");
		}

		Map<String, Object> inMap = getInMap(opts);
		this.serverImpl.execStreamingMapCommand(
				CmdSpec.SUBMIT.toString(),
				Parameters.processParameters(
						opts, null, "-i", this.serverImpl),
				inMap,
				callback,
				key);
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#submit(boolean)
	 */
	@Override
	public List<IFileSpec> submit(boolean reOpen)
			throws ConnectionException, RequestException, AccessException {
		return submit(reOpen, null, null);
	}

	private void updateFlags() {
	}

	private Map<String, Object> getInMap(SubmitOptions opts) throws P4JavaException {
		Map<String, Object> inMap = new HashMap<String, Object>();

		if ((id == IChangelist.UNKNOWN) || (id == IChangelist.DEFAULT)) {
			inMap.put("Change", "new");
		} else {
			inMap.put("Change", Integer.toString(this.id));
		}

		inMap.put("Client", this.clientId);
		inMap.put("User", this.username);
		inMap.put("Description", this.description);

		// Refresh the files from the server, only if the list is null or empty,
		// in case we only wants to submit the current files in the changelist.
		if (this.fileSpecs == null || this.fileSpecs.isEmpty()) {
			getFiles(true);
		}

		if (this.fileSpecs != null) {
			int i = 0;
			for (IFileSpec spec : this.fileSpecs) {
				inMap.put("Files" + i++, spec.getDepotPathString());
			}
		}

		// Refresh the job Ids list from the server; only if it is not unknown,
		// not default and greater than zero
		if ((id != IChangelist.UNKNOWN) && (id != IChangelist.DEFAULT)) {
			if (id > 0) {
				getJobIds();
			}
		}

		// If there are job ids in the SubmitOptions, assume the user only wants
		// to submit those jobs with this changelist.
		if ((opts != null) && (opts.getJobIds() != null)) {
			int i = 0;
			for (String id : opts.getJobIds()) {
				if (opts.getJobStatus() != null) {
					inMap.put("Jobs" + i++, id + " " + opts.getJobStatus());
				} else {
					inMap.put("Jobs" + i++, id);
				}
			}
		} else if (this.jobIds != null) {
			int i = 0;
			for (String id : this.jobIds) {
				if ((opts != null) && (opts.getJobStatus() != null)) {
					inMap.put("Jobs" + i++, id + " " + opts.getJobStatus());
				} else {
					inMap.put("Jobs" + i++, id);
				}
			}
		}

		return inMap;
	}

	@Override
	public void refresh()
			throws ConnectionException, RequestException, AccessException {
		// Basically, just ask the server about us and fill in the blanks... (and what
		// a waste of a perfectly good IChangelist object :-) ).
		if (this.serverImpl == null) {
			throw new RequestException("Changelist not associated with a Perforce server");
		}

		if (!this.refreshable) {
			throw new UnimplementedError("tried to refresh non-refreshable changelist");
		}

		IChangelist cList = this.serverImpl.getChangelist(this.id);
		if (cList == null) {
			throw new RequestException("Changelist is null when refreshing from the Perforce server");
		}

		this.status = cList.getStatus();
		this.clientId = cList.getClientId();
		this.date = cList.getDate();
		this.description = cList.getDescription();
		this.username = cList.getUsername();
		this.jobIds = cList.getCachedJobIdList();
		this.getFiles(true); // Updated by side-effect...
		updateFlags();
	}

	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update()
	 */
	@Override
	public void update() throws ConnectionException, RequestException, AccessException {
		update(false);
	}

	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update(boolean)
	 */
	@Override
	public void update(boolean force) throws ConnectionException, RequestException, AccessException {
		update(new ChangelistOptions().setForce(force));
	}

	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update(com.perforce.p4java.option.Options)
	 */
	public void update(Options opts) throws ConnectionException, RequestException, AccessException {
		if (this.serverImpl == null) {
			throw new RequestException("Changelist not associated with a Perforce server");
		}
		if (!this.updateable) {
			throw new UnimplementedError("Tried to refresh non-updateable changelist");
		}
		if ((id == IChangelist.UNKNOWN) || (id == IChangelist.DEFAULT)) {
			throw new RequestException("Tried to update new or default changelist");
		}
		if (opts != null) {
			if (!(opts instanceof ChangelistOptions)) {
				throw new RequestException("Options parameter is not an instanceof ChangelistOptions");
			}
		}

		try {
			List<Map<String, Object>> retMaps = this.serverImpl.execMapCmdList(CmdSpec.CHANGE,
					Parameters.processParameters(
							opts, null, new String[]{"-i"}, this.serverImpl),
					InputMapper.map(this));

			if (retMaps != null) {
				for (Map<String, Object> map : retMaps) {
					this.serverImpl.handleErrorStr(map);
				}
			}
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			// p4ic4idea: include root exception
			throw new RequestException(exc);
		}
	}

	/**
	 * @see com.perforce.p4java.core.IChangelist#updateOnServer(boolean)
	 */
	@Override
	public void updateOnServer(boolean refresh)
			throws ConnectionException, RequestException, AccessException {
		this.update();
		if (refresh) {
			this.refresh();
		}
	}

	public List<IFileSpec> getFileSpecs() {
		return this.fileSpecs;
	}

	public void setFileSpecs(List<IFileSpec> fileSpecs) {
		this.fileSpecs = fileSpecs;
	}
}
