/**
 *
 */
package com.perforce.p4java.impl.mapbased.client;

import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.client.delegator.IWhereDelegator;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IRepo;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IntegrationOptions;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.generic.core.ListData;
import com.perforce.p4java.impl.generic.core.MapEntry;
import com.perforce.p4java.impl.mapbased.client.cmd.WhereDelegator;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientHelper;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.impl.mapbased.server.cmd.IListDelegator;
import com.perforce.p4java.impl.mapbased.server.cmd.ListDelegator;
import com.perforce.p4java.impl.mapbased.server.cmd.ReposDelegator;
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
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.callback.IStreamingCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.impl.mapbased.MapKeys.VIEW_DEPOT_TYPE;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.handleFileReturn;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.handleIntegrationFileReturn;

/**
 * Default implementation of the generic parts of an IClient interface.<p>
 * <p>
 * Note that this version is very much tied to the map-based server implementation,
 * and can (generally) only be used with that server type. This is not an onerous
 * requirement, however, as all IServer objects returned from the ServerFactory
 * are map-based implementations.<p>
 */

public class Client extends ClientSummary implements IClient {

	public static final String MERGE_TMP_FILENAME_KEY = "P4JMergeTmpFile";
	public static final String MERGE_START_FROM_REV_KEY = "P4JMergeStartFromRev";
	public static final String MERGE_END_FROM_REV_KEY = "P4JMergeEndFromRev";

	/**
	 * What a new client created by the newClient method uses as a description
	 * if nothing is passed in explicitly.
	 */
	public static final String DEFAULT_DESCRIPTION = "New Client created by P4Java";

	private Server serverImpl = null;
	private ClientView clientView = null;
	private ArrayList<String> changeView = null;
	private ViewDepotType viewDepotType;

	// The delegators for running perforce commands
	private IWhereDelegator whereDelegator = null;

	/**
	 * Convenience method to return a new Client object with certain default values
	 * filled in.<p>
	 * <p>
	 * Only the fields corresponding to the parameters here can be explicitly set
	 * on creation; all others are given defaults that can be changed
	 * or added later. These defaults are as given for the default Client
	 * and ClientSummary constructors; exceptions include the client's user
	 * name, which is set to server.getUserName (which may cause issues later
	 * down the line if that wasn't set).<p>
	 * <p>
	 * Note that this object is a local object only -- you must subsequently call
	 * the server's createClient method to also create it on the server (or use
	 * the Factory.createClient convenience method).<p>
	 * <p>
	 * Note that this method is pretty simple-minded about client names, paths, etc.
	 * -- anything that contains unexpected spaces or weird characters (etc.) may
	 * cause it to get confused, in which case you're better off constructing things
	 * by hand or pre-massaging parameters yourself.
	 *
	 * @param server      non-null IOptionsServer to be associated with the client.
	 * @param name        non-null client name.
	 * @param description if not null, the client description field to be used; if null,
	 *                    DEFAULT_DESCRIPTION will be used as a default.
	 * @param root        if not null, use this as the new client's root; if null, use the server's
	 *                    working directory if its getWorkingDirectory method returns non-null,
	 *                    otherwise use the JVM's current working directory as determine by the
	 *                    user.dir system property.
	 * @param paths       if not null, use this as the list of view map depot / client
	 *                    paths, in the order given, and according to the format in
	 *                    MapEntry.parseViewMappingString; defaults to a single entry,
	 *                    "//depot/... //clientname/depot/..." if not given.
	 * @return new non-null local Client object.
	 */

	public static Client newClient(IOptionsServer server, String name, String description,
								   String root, String[] paths) {
		String rootDir = root;
		Server serverImpl = null;
		String userName = null;

		if (server == null) {
			throw new NullPointerError("null server passed to Client.newClient");
		}
		if (name == null) {
			throw new NullPointerError("null client name passed to Client.newClient");
		}
		if (!(server instanceof Server)) {
			throw new P4JavaError(
					"IOptionsServer passed to Client.newClient does not implement 'Server' class");
		}

		serverImpl = ((Server) server);

		if (rootDir == null) {
			rootDir = serverImpl.getWorkingDirectory() == null ?
					System.getProperty("user.dir") :
					serverImpl.getWorkingDirectory();
			if (rootDir == null) {
				throw new P4JavaError(
						"unable to determine root directory for new client");
			}
		}

		userName = server.getUserName();

		if (paths == null) {
			paths = new String[]{
					"//depot/... //" + name + "/depot/..."
			};
		}

		Client client = new Client(server);
		client.setName(name);
		client.setDescription(description == null ? DEFAULT_DESCRIPTION : description);
		client.setOwnerName(userName);
		client.setRoot(rootDir);
		ClientView clientView = new ClientView();

		clientView.setClient(client);
		List<IClientViewMapping> viewMappings = new ArrayList<IClientViewMapping>();
		int i = 0;
		for (String mapping : paths) {
			if (mapping == null) {
				throw new NullPointerError("null mapping string passed to Client.newClient");
			}
			viewMappings.add(new ClientView.ClientViewMapping(i, mapping));
			i++;
		}
		clientView.setEntryList(viewMappings);

		client.setClientView(clientView);
		return client;
	}

	/**
	 * Default constructor. Clients constructed with this constructor will need
	 * a suitable setServer call to set the underlying server link before being able
	 * to do much beyond setting local fields. Note that we also need to set
	 * the various IServerResource fields appropriately, as IClientSummary objects
	 * are not completable, refreshable, or updateable, but full IClient objects
	 * are (at least) refreshable and updateable.<p>
	 * <p>
	 * ClientSummary fields are set as noted in the ClientSummary default constructor
	 * comments.
	 */
	@Deprecated
	public Client() {
		super();
		super.refreshable = true;
		super.updateable = true;
	}

	/**
	 * Note that any IServer object returned by the ServerFactory will work for the serverImpl
	 * parameter; if not, a suitable cast exception will be thrown.<p>
	 * <p>
	 * ClientSummary fields are set as noted in the ClientSummary default constructor
	 * comments.
	 *
	 * @param server an IServer server returned from the server factory.
	 */

	public Client(IServer server) {
		super();
		super.refreshable = true;
		super.updateable = true;
		setServer(server);
	}

	/**
	 * Construct a new Client object from explicit fields.<p>
	 * <p>
	 * Note that any IServer object returned by the ServerFactory will work for the serverImpl
	 * parameter; if not, a suitable cast exception will be thrown.<p>
	 */

	public Client(String name, Date accessed, Date updated, String description,
				  String hostName, String ownerName, String root,
				  ClientLineEnd lineEnd, IClientOptions options,
				  IClientSubmitOptions submitOptions, List<String> alternateRoots,
				  IServer serverImpl, ClientView clientView) {
		super(name, accessed, updated, description, hostName, ownerName, root,
				lineEnd, options, submitOptions, alternateRoots);
		setServer(serverImpl);
		this.clientView = clientView;
	}

	/**
	 * Construct a new Client object from explicit fields.<p>
	 * <p>
	 * Note that any IServer object returned by the ServerFactory will work for the serverImpl
	 * parameter; if not, a suitable cast exception will be thrown.<p>
	 */

	public Client(String name, Date accessed, Date updated, String description,
				  String hostName, String ownerName, String root,
				  ClientLineEnd lineEnd, IClientOptions options,
				  IClientSubmitOptions submitOptions, List<String> alternateRoots,
				  IServer serverImpl, ClientView clientView, String stream, String type) {
		super(name, accessed, updated, description, hostName, ownerName, root,
				lineEnd, options, submitOptions, alternateRoots, stream, type);
		setServer(serverImpl);
		this.clientView = clientView;
	}

	/**
	 * Construct a suitable Client object from an IServer and a map
	 * returned from the Perforce server. If map is null, this is equivalent
	 * to calling the Client(IServer serverImpl) constructor.<p>
	 * <p>
	 * Note that any IServer object returned by the ServerFactory will work for the serverImpl
	 * parameter; if not, a suitable cast exception will be thrown.<p>
	 */

	public Client(IServer serverImpl, Map<String, Object> map) {
		super(map, false);
		super.refreshable = true;
		super.updateable = true;
		setServer(serverImpl);
		this.viewDepotType = ViewDepotType.LOCAL;

		// Extract fields from the map that aren't in the client spec set;
		// but note that maps that come back from getClientList() can use different
		// field names and field formats from the maps that come back from the
		// getClient() method(s). This complicates things a bit and helps explain
		// the brute-force duplication of the original ClientSummary map parsing below.

		if (map != null) {
			this.name = (String) map.get("Client");

			// Try to retrieve the view map; it comes to us as a series of
			// map entries starting with "View" and followed by a number, e.g. "View9".
			// These view numbers *must* be used to set the order of each individual
			// map entry on the view map, as order is (very) significant and must be
			// preserved.

			ClientView viewImpl = new ClientView();
			ArrayList<IClientViewMapping> mappingList = new ArrayList<IClientViewMapping>();
			viewImpl.setEntryList(mappingList);
			this.clientView = viewImpl;
			String pfx = "View";

			for (int i = 0; map.containsKey(pfx + i); i++) {
				String key = pfx + i;

				String[] parts = MapEntry.parseViewMappingString((String) map.get(key));

				if (parts.length < 2) {
					throw new P4JavaError(
							"bad client view mapping string in Client constructor: "
									+ (String) map.get(key));
				}

				mappingList.add(new ClientView.ClientViewMapping(i, parts[0], parts[1]));
			}

			// Add change view entries to list if found.

			ArrayList<String> changeViewImpl = new ArrayList<>();
			pfx = "ChangeView";

			for (int i = 0; map.containsKey(pfx + i); i++) {
				String key = pfx + i;

				String changeEntry = (String) map.get(key);

				if (changeEntry == null || changeEntry.isEmpty()) {
					throw new P4JavaError("null or empty change view mapping string in Client constructor.");
				}

				changeViewImpl.add(changeEntry);
			}
			this.changeView = changeViewImpl;

			// Description strings *sometimes* come back with a trailing newline (that wasn't
			// there when we created the client description), which
			// is annoying because we can't just simply use trim() -- there's no rule that
			// says that descriptions can't start or end with whitespace -- so we kludge up
			// the following to get rid of just the trailing newline if it's there -- HR.

			this.description = (String) map.get("Description");
			if ((this.description != null) && (this.description.length() > 1) && this.description.endsWith("\n")) {
				this.description = this.description.substring(0, this.description.length() - 1);
			}
			try {
				// Different format here to what's in ClientSummary.
				if (map.get("Access") != null) {
					this.accessed =
							new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse((String) map.get("Access"));
				}
			} catch (Exception exc) {
				Log.error("Access date parse error in Client constructor "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}
			try {
				if (map.get("Update") != null) {
					this.updated =
							new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse((String) map.get("Update"));
				}
			} catch (Exception exc) {
				Log.error("Update date parse error in Client constructor "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}

			viewImpl.setClient(this);
			if (map.get(VIEW_DEPOT_TYPE) != null) {
				this.viewDepotType = ViewDepotType.fromString(map.get(VIEW_DEPOT_TYPE).toString());
			}
		}
	}

	/**
	 * Construct a new Client object using the passed-in client summary object as a partial
	 * template. Note that this client object will need to have its serverImpl before any
	 * refreshes, updates, etc., are done against it.<p>
	 * <p>
	 * If summary is null, this is equivalent to calling the default constructor. If clientSummary
	 * is not null, and refresh is false, the relevant ClientSummary superclass is initialized
	 * by copying the passed-in summary fields. If clientSummary is not null and refresh is true,
	 * the Client is constructed by calling refresh() using the clientSummary's name; if refresh
	 * is false, the client view will be as for the default constructor.
	 *
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	public Client(IClientSummary clientSummary, boolean refresh)
			throws ConnectionException, RequestException, AccessException {
		this(clientSummary, null, refresh);
	}

	/**
	 * Construct a new Client object using the passed-in client summary object as a partial
	 * template along with the passed-in IServer object.<p>
	 * <p>
	 * Note that any IServer object returned by the ServerFactory will work for the serverImpl
	 * parameter; if not, a suitable cast exception will be thrown.<p>
	 * <p>
	 * If summary is null, this is equivalent to calling the default constructor. If clientSummary
	 * is not null, and refresh is false, the relevant ClientSummary superclass is initialized
	 * by copying the passed-in summary fields. If clientSummary is not null and refresh is true,
	 * the Client is constructed by calling refresh() using the clientSummary's name; if refresh
	 * is false, the client view will be as for the default constructor.
	 *
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws RequestException    if the Perforce server encounters an error during
	 *                             its processing of the request
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */
	public Client(IClientSummary clientSummary, IServer serverImpl, boolean refresh)
			throws ConnectionException, RequestException, AccessException {
		super(false);
		setServer(serverImpl);
		if (clientSummary != null) {
			if (refresh) {
				if (clientSummary.getName() == null) {
					throw new NullPointerError(
							"Null label name in client summary object passed to Client constructor");
				}

				this.name = clientSummary.getName();
				this.refresh();
			} else {
				this.name = clientSummary.getName();
				this.accessed = clientSummary.getAccessed();
				this.updated = clientSummary.getUpdated();
				this.description = clientSummary.getDescription();
				this.hostName = clientSummary.getHostName();
				this.ownerName = clientSummary.getOwnerName();
				this.root = clientSummary.getRoot();
				this.lineEnd = clientSummary.getLineEnd();
				this.options = clientSummary.getOptions();
				this.submitOptions = clientSummary.getSubmitOptions();
				this.alternateRoots = clientSummary.getAlternateRoots();
				this.stream = clientSummary.getStream();
				this.serverId = clientSummary.getServerId();
			}
		}
	}

	private void init(IServer serverImpl) {
		whereDelegator = new WhereDelegator(serverImpl, this);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#getServer()
	 */
	@Override
	public IServer getServer() {
		return this.serverImpl;
	}

	/**
	 * Completing a client calls {@link #refresh()}. A no op for the new IClient object.
	 *
	 * @see #refresh()
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#complete()
	 */
	@Override
	public void complete() throws ConnectionException, RequestException,
			AccessException {
	}

	/**
	 * This method will refresh by getting the complete client model. If this
	 * refresh is successful then this client will be marked as complete.
	 *
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#refresh()
	 */
	@Override
	public void refresh() throws ConnectionException, RequestException,
			AccessException {
		IServer refreshServer = this.serverImpl;
		String refreshName = null;
		refreshName = this.getName();
		if (refreshServer != null && refreshName != null) {
			IClient refreshedClient = refreshServer.getClient(refreshName);
			if (refreshedClient != null) {
				setName(refreshName);
				setAccessed(refreshedClient.getAccessed());
				setUpdated(refreshedClient.getUpdated());
				setAlternateRoots(refreshedClient.getAlternateRoots());
				setClientView(refreshedClient.getClientView());
				setChangeView(refreshedClient.getChangeView());
				setDescription(refreshedClient.getDescription());
				setHostName(refreshedClient.getHostName());
				setLineEnd(refreshedClient.getLineEnd());
				setOptions(refreshedClient.getOptions());
				setOwnerName(refreshedClient.getOwnerName());
				setRoot(refreshedClient.getRoot());
				setSubmitOptions(refreshedClient.getSubmitOptions());
				setUpdated(refreshedClient.getUpdated());
				setStream(refreshedClient.getStream());
				setServerId(refreshedClient.getServerId());
				setType(refreshedClient.getType());
			}
		}
	}

	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update()
	 */
	@Override
	public void update() throws ConnectionException, RequestException, AccessException {
		if (serverImpl != null) {
			this.serverImpl.updateClient(this);
		} else {
			throw new NullPointerError(
					"Attempted to update client with no associated server set on client");
		}
	}

	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update(boolean)
	 */
	@Override
	public void update(boolean force) throws ConnectionException, RequestException, AccessException {
		if (serverImpl != null) {
			this.serverImpl.updateClient(this, force);
		} else {
			throw new NullPointerError(
					"Attempted to update client with no associated server set on client");
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#getClientView()
	 */
	@Override
	public ClientView getClientView() {
		return this.clientView;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#setClientView(com.perforce.p4java.impl.generic.client.ClientView)
	 */
	@Override
	public void setClientView(ClientView clientView) {
		this.clientView = clientView;
	}

	@Override
	public ArrayList<String> getChangeView() {
		return this.changeView;
	}

	@Override
	public void setChangeView(ArrayList<String> changeView) {
		this.changeView = changeView;
	}

	/**
	 * Note that this will fail with a class cast exception if the passed-in
	 * server is not a mapbased ServerImpl object.
	 *
	 * @param server ServerImpl server object.
	 */
	@Override
	public void setServer(IServer server) {
		this.serverImpl = (Server) server;
		init(server);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#sync(java.util.List, boolean, boolean, boolean, boolean)
	 */
	@Override
	public List<IFileSpec> sync(List<IFileSpec> fileSpecs, boolean forceUpdate, boolean noUpdate,
								boolean clientBypass, boolean serverBypass)
			throws ConnectionException, RequestException, AccessException {

		try {
			return sync(fileSpecs, new SyncOptions(forceUpdate, noUpdate, clientBypass, serverBypass));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#sync(java.util.List, com.perforce.p4java.option.client.SyncOptions)
	 */
	@Override
	public List<IFileSpec> sync(List<IFileSpec> fileSpecs, SyncOptions syncOpts)
			throws P4JavaException {
		List<IFileSpec> specList = new ArrayList<IFileSpec>();

		if ((this.serverImpl.getCurrentClient() == null)
				|| !this.serverImpl.getCurrentClient().getName().equalsIgnoreCase(this.getName())) {
			throw new RequestException(
					"Attempted to sync a client that is not the server's current client");
		}

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(CmdSpec.SYNC,
				Parameters.processParameters(
						syncOpts, fileSpecs, this.server),
				null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				specList.add(handleFileReturn(map, serverImpl));
			}
		}

		return specList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#syncParallel(List, SyncOptions, ParallelSyncOptions)
	 */
	@Override
	public List<IFileSpec> syncParallel(List<IFileSpec> fileSpecs, SyncOptions syncOpts,
										ParallelSyncOptions pSyncOpts) throws P4JavaException {
		List<IFileSpec> specList = new ArrayList<IFileSpec>();

		if ((this.serverImpl.getCurrentClient() == null)
				|| !this.serverImpl.getCurrentClient().getName().equalsIgnoreCase(this.getName())) {
			throw new RequestException(
					"Attempted to sync a client that is not the server's current client");
		}

		String[] syncOptions = ClientHelper.buildParallelOptions(serverImpl, fileSpecs, syncOpts, pSyncOpts);

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(CmdSpec.SYNC.toString(),
				syncOptions, null, pSyncOpts.getCallback());

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				specList.add(handleFileReturn(map, serverImpl));
			}
		}

		return specList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#sync(java.util.List, com.perforce.p4java.option.client.SyncOptions, com.perforce.p4java.server.callback.IStreamingCallback, int)
	 */
	@Override
	public void sync(List<IFileSpec> fileSpecs, SyncOptions syncOpts, IStreamingCallback callback, int key)
			throws P4JavaException {

		if ((this.serverImpl.getCurrentClient() == null)
				|| !this.serverImpl.getCurrentClient().getName().equalsIgnoreCase(this.getName())) {
			throw new RequestException(
					"Attempted to sync a client that is not the server's current client");
		}

		this.serverImpl.execStreamingMapCommand(CmdSpec.SYNC.toString(),
				Parameters.processParameters(
						syncOpts, fileSpecs, this.server),
				null,
				callback,
				key);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#sync(java.util.List, com.perforce.p4java.option.client.SyncOptions, com.perforce.p4java.server.callback.IStreamingCallback, int)
	 */
	@Override
	public void syncParallel(List<IFileSpec> fileSpecs, SyncOptions syncOpts, IStreamingCallback callback,
							 int key, ParallelSyncOptions pSyncOpts)
			throws P4JavaException {

		if ((this.serverImpl.getCurrentClient() == null)
				|| !this.serverImpl.getCurrentClient().getName().equalsIgnoreCase(this.getName())) {
			throw new RequestException(
					"Attempted to sync a client that is not the server's current client");
		}

		String[] syncOptions = ClientHelper.buildParallelOptions(serverImpl, fileSpecs, syncOpts, pSyncOpts);

		this.serverImpl.execStreamingMapCommand(CmdSpec.SYNC.toString(),
				syncOptions, null, callback, key, pSyncOpts.getCallback());
	}

	/**
	 * @see com.perforce.p4java.client.IClient#labelSync(java.util.List, java.lang.String, boolean, boolean, boolean)
	 */
	@Override
	public List<IFileSpec> labelSync(List<IFileSpec> fileSpecs, String labelName,
									 boolean noUpdate, boolean addFiles, boolean deleteFiles)
			throws ConnectionException, RequestException, AccessException {
		try {
			return labelSync(fileSpecs, labelName, new LabelSyncOptions(noUpdate, addFiles, deleteFiles));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#labelSync(java.util.List, java.lang.String, com.perforce.p4java.option.client.LabelSyncOptions)
	 */
	@Override
	public List<IFileSpec> labelSync(List<IFileSpec> fileSpecs, String labelName, LabelSyncOptions opts)
			throws P4JavaException {
		List<IFileSpec> specList = new ArrayList<IFileSpec>();

		if (labelName == null) {
			throw new NullPointerError(
					"null label name passed to Client.labelSync()");
		}

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(CmdSpec.LABELSYNC,
				Parameters.processParameters(
						opts, fileSpecs, "-l" + labelName, this.server),
				null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				specList.add(handleFileReturn(map, serverImpl));
			}
		}

		return specList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#createChangelist(com.perforce.p4java.core.IChangelist)
	 */
	@Override
	public IChangelist createChangelist(IChangelist newChangelist)
			throws ConnectionException, RequestException, AccessException {

		if (this.getName() == null) {
			throw new NullPointerError("Null client name in newChangelist method call");
		} else if (newChangelist == null) {
			throw new NullPointerError("Null new change list specification in newChangelist method call");
		} else if (newChangelist.getId() != IChangelist.UNKNOWN) {
			throw new RequestException("New changelist ID must be set to IChangelist.UNKNOWN");
		}

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(CmdSpec.CHANGE,
				new String[]{"-i"},
				InputMapper.map(newChangelist));

		if (resultMaps != null) {
			int id = IChangelist.UNKNOWN;
			for (Map<String, Object> map : resultMaps) {
				if (!this.serverImpl.handleErrorStr(map)) {
					if (map.containsKey("change")) {
						// Do this the easy way -- it's in the RPC map output
						String changeStr = (String) map.get("change");
						if (changeStr != null) {
							// skip the initial "Change " bit
							int i = changeStr.indexOf(" ");
							if ((i > 0) && (i < changeStr.length())) {
								try {
									id = new Integer(changeStr.substring(i + 1));
								} catch (Exception exc) {
									Log.error("Unexpected exception in Client.newChangelist: "
											+ exc.getLocalizedMessage());
									Log.exception(exc);
								}
							}
						}
					} else {
						String infoStr = this.serverImpl.getInfoStr(map);

						if ((infoStr != null) && infoStr.contains("Change ") && infoStr.contains(" created")) {
							String[] strs = infoStr.split(" ");

							if ((strs.length >= 3) && (strs[1] != null)) {
								id = IChangelist.UNKNOWN;
								try {
									id = new Integer(strs[1]);
								} catch (Exception exc) {
									Log.error("Unexpected exception in Client.newChangelist: "
											+ exc.getLocalizedMessage());
									Log.exception(exc);
								}
							}
						}
					}
				}
				if (id != IChangelist.UNKNOWN) {
					return this.serverImpl.getChangelist(id);
				}
			}
		}
		return null;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#addFiles(java.util.List, boolean, int, java.lang.String, boolean)
	 */
	@Override
	public List<IFileSpec> addFiles(List<IFileSpec> fileSpecs,
									boolean noUpdate, int changeListId, String fileType, boolean useWildcards)
			throws ConnectionException, AccessException {
		try {
			return addFiles(fileSpecs, new AddFilesOptions(
					noUpdate, changeListId, fileType, useWildcards));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.addFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#addFiles(java.util.List, com.perforce.p4java.option.client.AddFilesOptions)
	 */
	@Override
	public List<IFileSpec> addFiles(List<IFileSpec> fileSpecs, AddFilesOptions opts)
			throws P4JavaException {
		List<IFileSpec> resultList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.ADD,
				Parameters.processParameters(
						opts, fileSpecs, this.serverImpl),
				null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				resultList.add(handleFileReturn(map, serverImpl));
			}
		}
		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#deleteFiles(java.util.List, int, boolean)
	 */
	@Override
	public List<IFileSpec> deleteFiles(List<IFileSpec> fileSpecs,
									   int changeListId, boolean noUpdate)
			throws ConnectionException, AccessException {
		try {
			return deleteFiles(fileSpecs,
					new DeleteFilesOptions(changeListId, noUpdate, false));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.deleteFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#deleteFiles(java.util.List, com.perforce.p4java.option.client.DeleteFilesOptions)
	 */
	@Override
	public List<IFileSpec> deleteFiles(List<IFileSpec> fileSpecs, DeleteFilesOptions opts)
			throws P4JavaException {
		List<IFileSpec> resultList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.DELETE,
				Parameters.processParameters(
						opts, fileSpecs, this.serverImpl),
				null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				resultList.add(handleFileReturn(map, serverImpl));
			}
		}
		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#editFiles(java.util.List, boolean, boolean, int, java.lang.String)
	 */
	@Override
	public List<IFileSpec> editFiles(List<IFileSpec> fileSpecs,
									 boolean noUpdate, boolean bypassClientUpdate, int changeListId, String fileType)
			throws RequestException, ConnectionException, AccessException {
		final int MINIMUM_OPTION_K_SERVER_VERSION = 20092;    // minimum version number supporting bypassClientUpdate

		if (bypassClientUpdate) {
			if (this.serverImpl.getServerVersionNumber() < MINIMUM_OPTION_K_SERVER_VERSION) {
				throw new RequestException(
						"edit option 'bypassClientUpdate' only supported on servers 2009.2 and later");
			}
		}

		try {
			return editFiles(fileSpecs, new EditFilesOptions(
					noUpdate, bypassClientUpdate, changeListId, fileType));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#editFiles(java.util.List, com.perforce.p4java.option.client.EditFilesOptions)
	 */
	@Override
	public List<IFileSpec> editFiles(List<IFileSpec> fileSpecs, EditFilesOptions opts) throws P4JavaException {
		List<IFileSpec> resultList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.EDIT,
				Parameters.processParameters(
						opts, fileSpecs, this.serverImpl),
				null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				resultList.add(handleFileReturn(map, serverImpl));
			}
		}
		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#revertFiles(java.util.List, boolean, int, boolean, boolean)
	 */
	@Override
	public List<IFileSpec> revertFiles(List<IFileSpec> fileSpecs, boolean noUpdate,
									   int changeListId, boolean revertOnlyUnchanged, boolean noRefresh)
			throws ConnectionException, AccessException {
		try {
			return revertFiles(fileSpecs, new RevertFilesOptions(
					noUpdate, changeListId, revertOnlyUnchanged, noRefresh));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.revertFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#revertFiles(java.util.List, com.perforce.p4java.option.client.RevertFilesOptions)
	 */
	@Override
	public List<IFileSpec> revertFiles(List<IFileSpec> fileSpecs, RevertFilesOptions opts)
			throws P4JavaException {
		List<IFileSpec> resultList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.REVERT,
				Parameters.processParameters(
						opts, fileSpecs, null, false, this.serverImpl),
				null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				resultList.add(handleFileReturn(map, serverImpl));
			}
		}
		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#haveList(java.util.List)
	 */
	@Override
	public List<IFileSpec> haveList(List<IFileSpec> fileSpecs)
			throws ConnectionException, AccessException {

		return haveList(fileSpecs, null);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#graphHaveList(java.util.List)
	 */
	@Override
	public List<IFileSpec> graphHaveList(List<IFileSpec> fileSpecs)
			throws ConnectionException, AccessException {

		return haveList(fileSpecs, new String[]{"--graph-only"});
	}

	private List<IFileSpec> haveList(List<IFileSpec> fileSpecs, String[] filter)
			throws ConnectionException, AccessException {

		List<IFileSpec> haveList = new ArrayList<IFileSpec>();

		if ((this.serverImpl.getCurrentClient() == null)
				|| !this.serverImpl.getCurrentClient().getName().equalsIgnoreCase(this.getName())) {
			return haveList;
		}

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.HAVE, Server.getPreferredPathArray(filter, fileSpecs), null);

		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				haveList.add(handleFileReturn(result, serverImpl));
			}
		}

		return haveList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#reopenFiles(java.util.List, int, java.lang.String)
	 */
	@Override
	public List<IFileSpec> reopenFiles(List<IFileSpec> fileSpecs, int changeListId,
									   String fileType) throws ConnectionException, AccessException {

		try {
			return reopenFiles(fileSpecs,
					new ReopenFilesOptions().setChangelistId(changeListId).setFileType(fileType));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.reopenFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#reopenFiles(java.util.List, com.perforce.p4java.option.client.ReopenFilesOptions)
	 */
	@Override
	public List<IFileSpec> reopenFiles(List<IFileSpec> fileSpecs, ReopenFilesOptions opts)
			throws P4JavaException {
		List<IFileSpec> reopenList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.REOPEN,
				Parameters.processParameters(
						opts, fileSpecs, null, false, this.serverImpl),
				null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				reopenList.add(handleFileReturn(map, serverImpl));
			}
		}
		return reopenList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#where(java.util.List)
	 */
	@Override
	public List<IFileSpec> where(List<IFileSpec> fileSpecs) throws ConnectionException, AccessException {
		return whereDelegator.where(fileSpecs);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#localWhere(java.util.List)
	 */
	@Override
	public List<IFileSpec> localWhere(List<IFileSpec> fileSpecs) {
		return whereDelegator.localWhere(fileSpecs);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#openedFiles(java.util.List, int, int)
	 */
	@Override
	public List<IFileSpec> openedFiles(List<IFileSpec> fileSpecs, int maxFiles, int changeListId)
			throws ConnectionException, AccessException {

		try {
			return this.openedFiles(fileSpecs,
					new OpenedFilesOptions(false, this.getName(), maxFiles, null, changeListId));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.openedFiless: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#openedFiles(java.util.List, com.perforce.p4java.option.server.OpenedFilesOptions)
	 */
	@Override
	public List<IFileSpec> openedFiles(List<IFileSpec> fileSpecs, OpenedFilesOptions opts)
			throws P4JavaException {
		if (opts == null) {
			return this.serverImpl.getOpenedFiles(fileSpecs,
					new OpenedFilesOptions().setClientName(this.name));
		} else {
			// Need to clone (not quite literally) the opts so we don't change the original:
			if (opts.getOptions() != null) {
				List<String> optsStrings = opts.getOptions();
				return this.serverImpl.getOpenedFiles(fileSpecs, new OpenedFilesOptions(
						optsStrings.toArray(new String[optsStrings.size()])
				));
			} else {
				return this.serverImpl.getOpenedFiles(fileSpecs,
						opts.setAllClients(false).setClientName(this.getName()));
			}
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#integrateFiles(int, boolean, com.perforce.p4java.core.file.IntegrationOptions, java.lang.String, com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec)
	 */
	@Override
	public List<IFileSpec> integrateFiles(int changeListId, boolean showActionsOnly,
										  IntegrationOptions integOpts, String branchSpec,
										  IFileSpec fromFile, IFileSpec toFile)
			throws ConnectionException, AccessException {
		try {
			if (integOpts == null) {
				integOpts = new IntegrationOptions(); // Just being generous
			}
			boolean integrateAroundDeletedRevs = false;
			boolean rebranchSourceAfterDelete = false;
			boolean deleteTargetAfterDelete = false;
			boolean integrateAllAfterReAdd = false;

			String[] deleteOpts = integOpts.getDeletedOptions();
			if (deleteOpts != null) {
				for (String opt : deleteOpts) {
					if (opt != null) {
						if (opt.equals("d")) integrateAroundDeletedRevs = true;
						if (opt.equals("Di")) integrateAllAfterReAdd = true;
						if (opt.equals("Dt")) rebranchSourceAfterDelete = true;
						if (opt.equals("Ds")) deleteTargetAfterDelete = true;
					}
				}
			}

			return integrateFiles(fromFile, toFile, branchSpec,
					new IntegrateFilesOptions(
							changeListId, // changelistId
							integOpts.isBidirectionalInteg(),    // bidirectionalInteg
							integrateAroundDeletedRevs,        // integrateAroundDeletedRevs
							rebranchSourceAfterDelete,        // rebranchSourceAfterDelete
							deleteTargetAfterDelete,        // deleteTargetAfterDelete
							integrateAllAfterReAdd,            // integrateAllAfterReAdd
							integOpts.isForce(),        // forceIntegration
							integOpts.isUseHaveRev(),    // useHaveRev
							integOpts.isBaselessMerge(),    // doBaselessMerge
							integOpts.isDisplayBaseDetails(),    // displayBaseDetails
							showActionsOnly,                // showActionsOnly
							integOpts.isReverseMapping(),    // reverseMapping
							integOpts.isPropagateType(),    // propagateType
							integOpts.isDontCopyToClient(),    // dontCopyToClient
							integOpts.getMaxFiles()            // maxFiles
					));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.integrateFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#integrateFiles(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, java.lang.String, com.perforce.p4java.option.client.IntegrateFilesOptions)
	 */
	@Override
	public List<IFileSpec> integrateFiles(IFileSpec fromFile, IFileSpec toFile, String branchSpec,
										  IntegrateFilesOptions opts) throws P4JavaException {

		// Set the server's current client to this client
		IClient currentClient = this.serverImpl.getCurrentClient();
		this.serverImpl.setCurrentClient(this);
		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.INTEG,
				Parameters.processParameters(
						opts, fromFile, toFile, branchSpec, this.serverImpl),
				null);
		// Set the server's current client back to the previous one
		this.serverImpl.setCurrentClient(currentClient);

		return getIntegrationFilesFromReturn(resultMaps);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#integrateFiles(com.perforce.p4java.core.file.IFileSpec, java.util.List, com.perforce.p4java.option.client.IntegrateFilesOptions)
	 */
	@Override
	public List<IFileSpec> integrateFiles(IFileSpec fromFile, List<IFileSpec> toFiles,
										  IntegrateFilesOptions opts) throws P4JavaException {

		// Set the server's current client to this client
		IClient currentClient = this.serverImpl.getCurrentClient();
		this.serverImpl.setCurrentClient(this);
		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.INTEG,
				Parameters.processParameters(
						opts, fromFile, toFiles, null, this.serverImpl),
				null);

		// Set the server's current client back to the previous one
		this.serverImpl.setCurrentClient(currentClient);

		return getIntegrationFilesFromReturn(resultMaps);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#resolveFilesAuto(java.util.List, boolean, boolean, boolean, boolean, boolean)
	 */
	@Override
	public List<IFileSpec> resolveFilesAuto(List<IFileSpec> fileSpecs, boolean safeMerge,
											boolean acceptTheirs, boolean acceptYours, boolean showActionsOnly,
											boolean forceResolve)
			throws ConnectionException, AccessException {
		try {
			return resolveFilesAuto(fileSpecs, new ResolveFilesAutoOptions()
					.setAcceptTheirs(acceptTheirs)
					.setAcceptYours(acceptYours)
					.setForceResolve(forceResolve)
					.setSafeMerge(safeMerge)
					.setShowActionsOnly(showActionsOnly)
			);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.resolveFilesAuto: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#resolveFilesAuto(java.util.List, com.perforce.p4java.option.client.ResolveFilesAutoOptions)
	 */
	@Override
	public List<IFileSpec> resolveFilesAuto(List<IFileSpec> fileSpecs, ResolveFilesAutoOptions opts)
			throws P4JavaException {

		String amFlag = null;
		if ((opts == null)
				|| (!opts.isAcceptTheirs()
				&& !opts.isAcceptYours()
				&& !opts.isForceResolve()
				&& !opts.isSafeMerge())) {
			amFlag = "-am";
		}

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.RESOLVE,
				Parameters.processParameters(
						opts, fileSpecs, amFlag, serverImpl), null);

		return getIntegrationFilesFromReturn(resultMaps);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#resolveFile(com.perforce.p4java.core.file.IFileSpec, java.io.InputStream)
	 */
	@Override
	public IFileSpec resolveFile(IFileSpec targetFile, InputStream sourceStream)
			throws ConnectionException, RequestException, AccessException {
		return resolveFile(targetFile, sourceStream, true, -1, -1);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#resolveFile(com.perforce.p4java.core.file.IFileSpec, java.io.InputStream)
	 */
	@Override
	public IFileSpec resolveFile(IFileSpec targetFileSpec, InputStream sourceStream, boolean useTextualMerge,
								 int startFromRev, int endFromRev)
			throws ConnectionException, RequestException, AccessException {

		if (targetFileSpec == null) {
			throw new NullPointerError("Null target file spec passed to IClient.resolveFile");
		}

		if (sourceStream == null) {
			throw new NullPointerError("Null source stream passed to IClient.resolveFile");
		}

		if (!(this.serverImpl instanceof com.perforce.p4java.impl.mapbased.rpc.RpcServer)) {
			throw new RequestException(
					"Request not supported by the current P4Java implementation;"
							+ " use an RPC-based pure Java implementation if possible");
		}

		Map<String, Object> resolveMap = new HashMap<String, Object>();
		IFileSpec fileSpec = null;
		File tmpFile = null;

		if (serverImpl != null) {
			try {
				String defaultTmpDir = System.getProperty("java.io.tmpdir");
				String tmpDirProp = serverImpl.getProperties().getProperty(PropertyDefs.P4JAVA_TMP_DIR_KEY, defaultTmpDir);
				tmpFile = File.createTempFile("p4java", ".tmp", new File(tmpDirProp));

				if (tmpFile != null) {
					FileOutputStream outStream = new FileOutputStream(tmpFile);
					byte[] bytes = new byte[1024];
					int bytesRead;
					while ((bytesRead = sourceStream.read(bytes)) > 0) {
						outStream.write(bytes, 0, bytesRead);
					}
					outStream.close();
					resolveMap.put(MERGE_TMP_FILENAME_KEY, tmpFile.getPath());
					resolveMap.put(MERGE_START_FROM_REV_KEY, new Integer(startFromRev));
					resolveMap.put(MERGE_END_FROM_REV_KEY, new Integer(endFromRev));
				}

				List<String> args = new ArrayList<String>();
				if (useTextualMerge) {
					args.add("-t");
				}

				// make sure we don't get a client-ActionResolve call
				final int MINIMUM_ACTION_RESOLVE_SERVER_VERSION = 20111;    // minimum version number supporting bypassClientUpdate
				if (this.serverImpl.getServerVersionNumber() >= MINIMUM_ACTION_RESOLVE_SERVER_VERSION) {
					if (startFromRev != -1 || endFromRev != -1) {
						args.add("-Ac");
					}
				}
				args.add(targetFileSpec.getAnnotatedPreferredPathString());

				List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(CmdSpec.RESOLVE, args.toArray(new String[0]), resolveMap);

				if (resultMaps != null) {
					// This returns *two* entries in normal cases...
					if (resultMaps.size() > 1) {
						return handleIntegrationFileReturn(resultMaps.get(1), true, serverImpl);
					} else {
						return handleIntegrationFileReturn(resultMaps.get(0), false, serverImpl);
					}
				}
			} catch (IOException exc) {
				Log.error("local file I/O error on resolve: " + exc.getMessage());
				Log.exception(exc);
				throw new P4JavaError("local file I/O error on resolve: " + exc.getMessage());
			} finally {
				if (tmpFile != null) {
					tmpFile.delete();
				}
			}
		}

		return fileSpec;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#resolvedFiles(java.util.List, boolean)
	 */
	@Override
	public List<IFileSpec> resolvedFiles(List<IFileSpec> fileSpecs,
										 boolean showBaseRevision) throws ConnectionException, AccessException {
		try {
			return resolvedFiles(fileSpecs,
					new ResolvedFilesOptions().setShowBaseRevision(showBaseRevision));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.resolvedFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#resolvedFiles(java.util.List, com.perforce.p4java.option.client.ResolvedFilesOptions)
	 */
	@Override
	public List<IFileSpec> resolvedFiles(List<IFileSpec> fileSpecs, ResolvedFilesOptions opts)
			throws P4JavaException {

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.RESOLVED,
				Parameters.processParameters(opts, fileSpecs, serverImpl),
				null);

		return getIntegrationFilesFromReturn(resultMaps);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#lockFiles(java.util.List, int)
	 */
	@Override
	public List<IFileSpec> lockFiles(List<IFileSpec> fileSpecs, int changeListId)
			throws ConnectionException, AccessException {
		try {
			return lockFiles(fileSpecs, new LockFilesOptions().setChangelistId(changeListId));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.lockFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#lockFiles(java.util.List, com.perforce.p4java.option.client.LockFilesOptions)
	 */
	@Override
	public List<IFileSpec> lockFiles(List<IFileSpec> fileSpecs, LockFilesOptions opts)
			throws P4JavaException {
		List<IFileSpec> lockedList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.LOCK,
				Parameters.processParameters(opts, fileSpecs, serverImpl),
				null);
		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				lockedList.add(handleFileReturn(result, serverImpl));
			}
		}

		return lockedList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#unlockFiles(java.util.List, int, boolean)
	 */
	@Override
	public List<IFileSpec> unlockFiles(List<IFileSpec> fileSpecs, int changeListId, boolean force)
			throws ConnectionException, AccessException {
		try {
			return unlockFiles(fileSpecs,
					new UnlockFilesOptions().setChangelistId(changeListId).setForceUnlock(force));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.lockFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#unlockFiles(java.util.List, com.perforce.p4java.option.client.UnlockFilesOptions)
	 */
	@Override
	public List<IFileSpec> unlockFiles(List<IFileSpec> fileSpecs, UnlockFilesOptions opts)
			throws P4JavaException {
		List<IFileSpec> unlockedList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.UNLOCK,
				Parameters.processParameters(opts, fileSpecs, serverImpl),
				null);
		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				unlockedList.add(handleFileReturn(result, serverImpl));
			}
		}

		return unlockedList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#getDiffFiles(java.util.List, int, boolean, boolean, boolean, boolean, boolean, boolean, boolean)
	 */
	@Override
	public List<IFileSpec> getDiffFiles(List<IFileSpec> fileSpecs,
										int maxFiles, boolean diffNonTextFiles, boolean openedDifferentMissing,
										boolean openedForIntegrate, boolean unopenedMissing,
										boolean unopenedDifferent, boolean unopenedWithStatus, boolean openedSame)
			throws ConnectionException, RequestException, AccessException {
		try {
			return getDiffFiles(fileSpecs, new GetDiffFilesOptions()
					.setMaxFiles(maxFiles)
					.setDiffNonTextFiles(diffNonTextFiles)
					.setOpenedDifferentMissing(openedDifferentMissing)
					.setOpenedForIntegrate(openedForIntegrate)
					.setUnopenedMissing(unopenedMissing)
					.setUnopenedDifferent(unopenedDifferent)
					.setUnopenedWithStatus(unopenedWithStatus)
					.setOpenedSame(openedSame)
			);
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.getDiffFiles: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#getDiffFiles(java.util.List, com.perforce.p4java.option.client.GetDiffFilesOptions)
	 */
	@Override
	public List<IFileSpec> getDiffFiles(List<IFileSpec> fileSpecs, GetDiffFilesOptions opts)
			throws P4JavaException {
		List<IFileSpec> diffList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.DIFF,
				Parameters.processParameters(opts, fileSpecs, serverImpl),
				null);
		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				diffList.add(handleFileReturn(result, serverImpl));
			}
		}

		return diffList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#shelveFiles(java.util.List, int, com.perforce.p4java.option.client.ShelveFilesOptions)
	 */
	@Override
	public List<IFileSpec> shelveFiles(List<IFileSpec> fileSpecs, int changelistId,
									   ShelveFilesOptions opts) throws P4JavaException {
		List<IFileSpec> resultList = new ArrayList<IFileSpec>();

		String changelistString = null;
		if (changelistId == IChangelist.DEFAULT) {
			changelistString = "-cdefault";
		} else if (changelistId > 0) {
			changelistString = "-c" + changelistId;
		}
		// Set the server's current client to this client
		IClient currentClient = this.serverImpl.getCurrentClient();
		this.serverImpl.setCurrentClient(this);
		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.SHELVE,
				Parameters.processParameters(
						opts, fileSpecs, changelistString, serverImpl),
				null);
		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				resultList.add(handleFileReturn(result, serverImpl));
			}
		}
		// Set the server's current client back to the previous one
		this.serverImpl.setCurrentClient(currentClient);

		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#unshelveFiles(java.util.List, int, int, com.perforce.p4java.option.client.UnshelveFilesOptions)
	 */
	@Override
	public List<IFileSpec> unshelveFiles(List<IFileSpec> fileSpecs, int sourceChangelistId,
										 int targetChangelistId, UnshelveFilesOptions opts) throws P4JavaException {
		List<IFileSpec> resultList = new ArrayList<IFileSpec>();

		if (sourceChangelistId <= 0) {
			throw new RequestException(
					"Source changelist ID must be greater than zero");
		}

		String sourceChangelistString = "-s" + sourceChangelistId;
		String targetChangelistString = null;
		if (targetChangelistId == IChangelist.DEFAULT) {
			targetChangelistString = "-cdefault";
		} else if (targetChangelistId > 0) {
			targetChangelistString = "-c" + targetChangelistId;
		}

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.UNSHELVE,
				Parameters.processParameters(
						opts,
						fileSpecs,
						new String[]{
								sourceChangelistString,
								targetChangelistString},
						serverImpl),
				null);
		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				resultList.add(handleFileReturn(result, serverImpl));
			}
		}

		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#shelveChangelist(int,
	 * java.util.List, boolean, boolean, boolean)
	 */
	@Override
	public List<IFileSpec> shelveChangelist(int changelistId,
											List<IFileSpec> fileSpecs, boolean forceUpdate, boolean replace,
											boolean discard) throws ConnectionException, RequestException,
			AccessException {
		try {
			return shelveFiles(fileSpecs, changelistId, new ShelveFilesOptions()
					.setDeleteFiles(discard)
					.setForceShelve(forceUpdate)
					.setReplaceFiles(replace));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.shelveChangelist: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#shelveChangelist(com.perforce.p4java.core.IChangelist)
	 */
	@Override
	public List<IFileSpec> shelveChangelist(IChangelist list) throws ConnectionException,
			RequestException, AccessException {
		if (list == null) {
			throw new NullPointerError("Null changelist specification in shelveChangelist method call");
		}

		// Set the server's current client to this client
		IClient currentClient = this.serverImpl.getCurrentClient();
		this.serverImpl.setCurrentClient(this);
		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(CmdSpec.SHELVE,
				new String[]{"-i"},
				InputMapper.map(list, true));

		List<IFileSpec> resultList = new ArrayList<IFileSpec>();
		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				resultList.add(handleFileReturn(result, serverImpl));
			}
		}
		// Set the server's current client back to the previous one
		this.serverImpl.setCurrentClient(currentClient);

		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#unshelveChangelist(int,
	 * java.util.List, int, boolean, boolean)
	 */
	@Override
	public List<IFileSpec> unshelveChangelist(int shelveChangelistId,
											  List<IFileSpec> fileSpecs, int clientChangelistId,
											  boolean forceOverwrite, boolean previewOnly)
			throws ConnectionException, RequestException, AccessException {
		if (shelveChangelistId <= 0) {
			throw new RequestException(
					"Shelve changelist ID must be greater than zero");
		}

		try {
			return unshelveFiles(fileSpecs, shelveChangelistId, clientChangelistId,
					new UnshelveFilesOptions()
							.setForceUnshelve(forceOverwrite)
							.setPreview(previewOnly));
		} catch (ConnectionException exc) {
			throw exc;
		} catch (AccessException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			Log.warn("Unexpected exception in IClient.unshelveChangelist: " + exc);
			return new ArrayList<IFileSpec>();
		}
	}

	/**
	 * @see com.perforce.p4java.client.IClient#submitShelvedChangelist(int)
	 */
	@Override
	public List<IFileSpec> submitShelvedChangelist(int shelvedChangelistId) throws P4JavaException {

		if (shelvedChangelistId <= 0) {
			throw new RequestException(
					"Shelved changelist ID must be greater than zero");
		}

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(CmdSpec.SUBMIT,
				new String[]{"-e", "" + shelvedChangelistId},
				null);

		List<IFileSpec> resultList = new ArrayList<IFileSpec>();
		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				resultList.add(handleFileReturn(result, serverImpl));
			}
		}
		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#copyFiles(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, java.lang.String, com.perforce.p4java.option.client.CopyFilesOptions)
	 */
	@Override
	public List<IFileSpec> copyFiles(IFileSpec fromFile, IFileSpec toFile, String branchSpec,
									 CopyFilesOptions opts) throws P4JavaException {

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.COPY,
				Parameters.processParameters(
						opts, fromFile, toFile, branchSpec, this.serverImpl),
				null);

		return getIntegrationFilesFromReturn(resultMaps);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#copyFiles(com.perforce.p4java.core.file.IFileSpec, java.util.List, com.perforce.p4java.option.client.CopyFilesOptions)
	 */
	@Override
	public List<IFileSpec> copyFiles(IFileSpec fromFile, List<IFileSpec> toFiles,
									 CopyFilesOptions opts) throws P4JavaException {

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.COPY,
				Parameters.processParameters(
						opts, fromFile, toFiles, null, this.serverImpl),
				null);

		return getIntegrationFilesFromReturn(resultMaps);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#mergeFiles(com.perforce.p4java.core.file.IFileSpec, java.util.List, com.perforce.p4java.option.client.MergeFilesOptions)
	 */
	@Override
	public List<IFileSpec> mergeFiles(IFileSpec fromFile, List<IFileSpec> toFiles,
									  MergeFilesOptions opts) throws P4JavaException {

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.MERGE,
				Parameters.processParameters(
						opts, fromFile, toFiles, null, this.serverImpl),
				null);

		return getIntegrationFilesFromReturn(resultMaps);
	}

	/**
	 * Get the integration files from the return results.
	 */
	private List<IFileSpec> getIntegrationFilesFromReturn(List<Map<String, Object>> maps) throws P4JavaException {
		List<IFileSpec> integList = new ArrayList<IFileSpec>();
		if (maps != null) {
			for (Map<String, Object> result : maps) {
				integList.add(handleIntegrationFileReturn(result, serverImpl));
			}
		}
		return integList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#reconcileFiles(java.util.List, com.perforce.p4java.option.client.ReconcileFilesOptions)
	 */
	@Override
	public List<IFileSpec> reconcileFiles(List<IFileSpec> fileSpecs, ReconcileFilesOptions opts)
			throws P4JavaException {
		List<IFileSpec> resultList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.RECONCILE,
				Parameters.processParameters(
						opts, fileSpecs, this.serverImpl),
				null);

		if (resultMaps != null) {
			for (Map<String, Object> map : resultMaps) {
				resultList.add(handleFileReturn(map, serverImpl));
			}
		}
		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.IClient#reconcileFiles(List, ReconcileFilesOptions, IStreamingCallback, int)
	 */
	@Override
	public void reconcileFiles(List<IFileSpec> fileSpecs, ReconcileFilesOptions opts, IStreamingCallback callback, int key)
			throws P4JavaException {

		if ((this.serverImpl.getCurrentClient() == null)
				|| !this.serverImpl.getCurrentClient().getName().equalsIgnoreCase(this.getName())) {
			throw new RequestException(
					"Attempted to reconcile a client that is not the server's current client");
		}

		this.serverImpl.execStreamingMapCommand(
				CmdSpec.RECONCILE.toString(),
				Parameters.processParameters(
						opts, fileSpecs, this.serverImpl),
				null,
				callback,
				key);
	}

	/**
	 * @see com.perforce.p4java.client.IClient#populateFiles(com.perforce.p4java.core.file.IFileSpec, java.util.List, com.perforce.p4java.option.client.PopulateFilesOptions)
	 */
	@Override
	public List<IFileSpec> populateFiles(IFileSpec fromFile, List<IFileSpec> toFiles,
										 PopulateFilesOptions opts) throws P4JavaException {

		List<Map<String, Object>> resultMaps = this.serverImpl.execMapCmdList(
				CmdSpec.POPULATE,
				Parameters.processParameters(
						opts, fromFile, toFiles, null, this.serverImpl),
				null);

		return getIntegrationFilesFromReturn(resultMaps);
	}

	/*
	 * @see com.perforce.p4java.server.IServer#getRepos()
	 */
	@Override
	public List<IRepo> getRepos() throws ConnectionException, RequestException, AccessException {
		ReposDelegator reposDelegator = new ReposDelegator(this.serverImpl);
		return reposDelegator.getRepos(this.getName());
	}

	/**
	 * @see IClient#getViewDepotType()
	 */
	@Override
	public ViewDepotType getViewDepotType() {
		return viewDepotType;
	}

	/**
	 * @param fileSpecs List of File specs
	 * @param options   List options
	 * @return data fetched from p4 list command
	 * @throws P4JavaException API error
	 */
	@Override
	public ListData getListData(List<IFileSpec> fileSpecs, ListOptions options) throws P4JavaException {
		IListDelegator listDelegator = new ListDelegator(this.serverImpl);
		return listDelegator.getListData(fileSpecs, options, this.getName());
	}
}
