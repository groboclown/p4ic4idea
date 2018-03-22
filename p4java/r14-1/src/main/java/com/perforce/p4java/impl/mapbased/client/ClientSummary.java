/**
 * 
 */
package com.perforce.p4java.impl.mapbased.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.impl.generic.client.ClientSubmitOptions;
import com.perforce.p4java.impl.generic.core.ServerResource;
import com.perforce.p4java.impl.mapbased.MapKeys;

/**
 * Default implementation class for the IClientSummary interface.
 */
public class ClientSummary extends ServerResource implements IClientSummary {

	protected String name = null;
	protected Date accessed = null;
	protected Date updated = null;
	protected String description = null;
	protected String hostName = null;
	protected String ownerName = null;
	protected String root = null;
	protected ClientLineEnd lineEnd = ClientLineEnd.LOCAL;
	protected IClientOptions options = null;
	protected IClientSubmitOptions submitOptions = null;
	protected List<String> alternateRoots = null;
	protected String stream = null;
	protected String serverId = null;
	protected int streamAtChange = IChangelist.UNKNOWN;
	protected boolean unloaded = false;
	
	/**
	 * Default constructor. Sets all fields to null except lineEnd,
	 * which is set to ClientLineEnd.LOCAL. Sets ServerResource
	 * superclass fields to indicate complete and not refereshable or
	 * updateable. Intended mostly for use with "pure" ClientSummary
	 * objects.
	 */
	public ClientSummary() {
		super(false, false);
	}
	
	/**
	 * Construct a new ClientSummary object whose ServerResource fields
	 * depend on the passed-in summaryOnly parameter. If summaryOnly is false,
	 * this object is complete, updateable, and refreshable; otherwise, it's 
	 * complete and neither updateable nor refresheable. Intended mostly for use
	 * with extended ClientSummary objects such as the full Client class.
	 */
	public ClientSummary(boolean summaryOnly) {
		super(!summaryOnly, !summaryOnly);
	}
	
	/**
	 * Explicit-value constructor.  Intended mostly for use with "pure" ClientSummary
	 * objects. Sets ServerResource superclass fields to indicate complete and neither
	 * refreshable nor updateable.
	 */
	public ClientSummary(String name, Date accessed, Date updated,
			String description, String hostName, String ownerName, String root,
			ClientLineEnd lineEnd, IClientOptions options,
			IClientSubmitOptions submitOptions,
			List<String> alternateRoots) {
		super(false, false);
		this.name = name;
		this.accessed = accessed;
		this.updated = updated;
		this.description = description;
		this.hostName = hostName;
		this.ownerName = ownerName;
		this.root = root;
		this.lineEnd = lineEnd;
		this.options = options;
		this.submitOptions = submitOptions;
		this.alternateRoots = alternateRoots;
	}

	/**
	 * Explicit-value constructor.  Intended mostly for use with "pure" ClientSummary
	 * objects. Sets ServerResource superclass fields to indicate complete and neither
	 * refreshable nor updateable.
	 */
	public ClientSummary(String name, Date accessed, Date updated,
			String description, String hostName, String ownerName, String root,
			ClientLineEnd lineEnd, IClientOptions options,
			IClientSubmitOptions submitOptions,
			List<String> alternateRoots, String stream) {
		super(false, false);
		this.name = name;
		this.accessed = accessed;
		this.updated = updated;
		this.description = description;
		this.hostName = hostName;
		this.ownerName = ownerName;
		this.root = root;
		this.lineEnd = lineEnd;
		this.options = options;
		this.submitOptions = submitOptions;
		this.alternateRoots = alternateRoots;
		this.stream = stream;
	}

	/**
	 * Clone a client summary by copying all fields. If clientSummary is null,
	 * this is equivalent to calling the default constructor.
	 */
	public ClientSummary(IClientSummary clientSummary) {
		super(false, false);
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
		this.streamAtChange = clientSummary.getStreamAtChange();
	}
	
	/**
	 * Server map constructor. Attempts to construct a new ClientSummary
	 * object from the passed-in map, which is assumed to have come from
	 * a Perforce server in response to a client list command. If map is null,
	 * this is equivalent to calling the default constructor.<p>
	 * 
	 * Note that fields set here may be overridden in a full Client constructor,
	 * as the field keys and formats can be subtly (and not so subtly) different
	 * in maps returned from (say) getClientList() and getClient(). If summaryOnly
	 * is false, this map is assumed to be from a full client retrieval, meaning
	 * some of the fields retrieved in the full Client constructor are not
	 * set here. Otherwise, it attempts to retrieve all known ClientSummary fields.
	 */
	public ClientSummary(Map<String, Object> map, boolean summaryOnly) {
		super(false, false);
		if (map != null) {
			try{
				if (summaryOnly){
					this.name = (String) map.get("client"); // vs. "Client" for full version...
					// Irritatingly-different date formats used here...
					this.updated = new Date(Long.parseLong((String) map.get("Update")) * 1000);
					this.accessed = new Date(Long.parseLong((String) map.get("Access")) * 1000);
					this.description = (String) map.get("Description");
				}
				this.hostName = (String) map.get("Host");
				this.ownerName = (String) map.get("Owner");
				this.root = (String) map.get("Root");
				this.lineEnd = ClientLineEnd.getValue((String) map.get("LineEnd"));
				this.options = new ClientOptions((String) map.get("Options"));
				
				String submitOptions = (String) map.get("SubmitOptions");
				if( submitOptions != null) {
					this.submitOptions = new ClientSubmitOptions(submitOptions);
				}
				
				// Retrieve the alternate roots:
				
				for (int i = 0; ; i++) {
					String altRoot = (String) map.get(MapKeys.ALTROOTS_KEY + i);
					if (altRoot == null) {
						break;
					}
					
					if (this.alternateRoots == null) {
						this.alternateRoots = new ArrayList<String>();
					}
					this.alternateRoots.add((String) map.get(MapKeys.ALTROOTS_KEY + i));
				}
				
				// Get the stream's path in a stream depot, of the form //depotname/streamname,
				// to which this client's view will be dedicated.
				if (map.get("Stream") != null) {
					this.stream = (String)map.get("Stream");
				}

				this.serverId = (String)map.get("ServerID");
				
				if (map.get("AtChange") != null) {
					this.streamAtChange = new Integer((String) map.get("AtChange"));
				}

				if (map.get("IsUnloaded") != null
						&& ((String) map.get("IsUnloaded")).equals("1")) {
					this.unloaded = true;
				}
				
			} catch (Exception exc) {
				Log.error("Format error in ClientSummary constructor: "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}
		}
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getAccessed() {
		return accessed;
	}
	public void setAccessed(Date accessed) {
		this.accessed = accessed;
	}
	public Date getUpdated() {
		return updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getOwnerName() {
		return ownerName;
	}
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
	public String getRoot() {
		return root;
	}
	public void setRoot(String root) {
		this.root = root;
	}
	public ClientLineEnd getLineEnd() {
		return lineEnd;
	}
	public void setLineEnd(ClientLineEnd lineEnd) {
		this.lineEnd = lineEnd;
	}
	public IClientOptions getOptions() {
		return options;
	}
	public void setOptions(IClientOptions options) {
		this.options = options;
	}
	public IClientSubmitOptions getSubmitOptions() {
		return submitOptions;
	}
	public void setSubmitOptions(IClientSubmitOptions submitOptions) {
		this.submitOptions = submitOptions;
	}
	public List<String> getAlternateRoots() {
		return this.alternateRoots;
	}
	public void setAlternateRoots(List<String> alternateRoots) {
		this.alternateRoots = alternateRoots;
	}
	public String getStream() {
		return stream;
	}
	public void setStream(String stream) {
		this.stream = stream;
	}
	
	/**
	 * @see com.perforce.p4java.client.IClientSummary#isStream()
	 */
	public boolean isStream() {
		if (stream != null) {
			// It should be a depot path
			if (stream.startsWith("//")) {
				return true;
			}
		}
		return false;
	}

	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public int getStreamAtChange() {
		return this.streamAtChange;
	}

	public void setStreamAtChange(int streamAtChange) {
		this.streamAtChange = streamAtChange;
	}

	/**
	 * @see com.perforce.p4java.client.IClientSummary#isUnloaded()
	 */
	public boolean isUnloaded() {
		return unloaded;
	}
}
