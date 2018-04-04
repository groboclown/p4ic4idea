package com.perforce.p4java.impl.mapbased.client;

import com.perforce.p4java.Log;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.impl.generic.client.ClientSubmitOptions;
import com.perforce.p4java.impl.generic.core.ServerResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseInt;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.common.base.StringHelper.firstNonBlank;
import static com.perforce.p4java.impl.mapbased.MapKeys.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
	protected List<String> alternateRoots = new ArrayList<>();
	protected String stream = null;
	protected String serverId = null;
	protected int streamAtChange = IChangelist.UNKNOWN;
	protected boolean unloaded = false;

	protected String type = null;
	protected String backup = null;

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
	public ClientSummary(final boolean summaryOnly) {
		super(!summaryOnly, !summaryOnly);
	}

	/**
	 * Clone a client summary by copying all fields. If clientSummary is null,
	 * this is equivalent to calling the default constructor.
	 */
	public ClientSummary(final IClientSummary clientSummary) {
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
		this.type = clientSummary.getType();
		this.backup = clientSummary.getBackup();
	}

	/**
	 * Server map constructor. Attempts to construct a new ClientSummary
	 * object from the passed-in map, which is assumed to have come from
	 * a Perforce server in response to a client list command. If map is null,
	 * this is equivalent to calling the default constructor.<p>
	 * <p>
	 * Note that fields set here may be overridden in a full Client constructor,
	 * as the field keys and formats can be subtly (and not so subtly) different
	 * in maps returned from (say) getClientList() and getClient(). If summaryOnly
	 * is false, this map is assumed to be from a full client retrieval, meaning
	 * some of the fields retrieved in the full Client constructor are not
	 * set here. Otherwise, it attempts to retrieve all known ClientSummary fields.
	 */
	public ClientSummary(final Map<String, Object> map, final boolean summaryOnly) {
		super(false, false);
		if (nonNull(map)) {
			try {
				if (summaryOnly) {
					name = firstNonBlank(parseString(map, CLIENT_KEY), parseString(map, CLIENT_LC_KEY));
					// Irritatingly-different date formats used here...
					updated = new Date(parseLong(map, UPDATE_KEY) * 1000);
					accessed = new Date(parseLong(map, ACCESS_KEY) * 1000);
					description = parseString(map, DESCRIPTION_KEY);
				}
				hostName = parseString(map, HOST_KEY);
				ownerName = parseString(map, OWNER_KEY);
				root = parseString(map, ROOT_KEY);
				lineEnd = ClientLineEnd.getValue(parseString(map, LINEEND_KEY));
				options = new ClientOptions(parseString(map, OPTIONS_KEY));

				String submitOptions = parseString(map, SUBMITOPTIONS_KEY);
				if (nonNull(submitOptions)) {
					this.submitOptions = new ClientSubmitOptions(submitOptions);
				}

				// Retrieve the alternate roots:

				for (int i = 0; ; i++) {
					String altRoot = parseString(map, ALTROOTS_KEY + i);
					if (isBlank(altRoot)) {
						break;
					}

					alternateRoots.add(parseString(map, ALTROOTS_KEY + i));
				}

				// Get the stream's path in a stream depot, of the form //depotname/streamname,
				// to which this client's view will be dedicated.
				if (nonNull(map.get(STREAM_KEY))) {
					stream = parseString(map, STREAM_KEY);
				}

				serverId = parseString(map, SERVERID_KEY);

				if (nonNull(map.get(STREAMATCHANGE_KEY))) {
					streamAtChange = parseInt(map, STREAMATCHANGE_KEY);
				}

				if (nonNull(map.get(ISUNLOADED_KEY))
						&& "1".equals(parseString(map, ISUNLOADED_KEY))) {
					unloaded = true;
				}

				type = parseString(map, TYPE_KEY);

				backup = parseString(map, CLIENT_BACKUP_KEY);
			} catch (Exception exc) {
				Log.error("Format error in ClientSummary constructor: %s", exc.getLocalizedMessage());
				Log.exception(exc);
			}
		}
	}

	/**
	 * Explicit-value constructor.  Intended mostly for use with "pure" ClientSummary
	 * objects. Sets ServerResource superclass fields to indicate complete and neither
	 * refreshable nor updateable.
	 */
	public ClientSummary(
			final String name,
			final Date accessed,
			final Date updated,
			final String description,
			final String hostName,
			final String ownerName,
			final String root,
			final ClientLineEnd lineEnd,
			final IClientOptions options,
			final IClientSubmitOptions submitOptions,
			final List<String> alternateRoots) {

		this(name, accessed, updated, description, hostName, ownerName, root, lineEnd, options, submitOptions, alternateRoots, null, null);
	}

	/**
	 * Explicit-value constructor.  Intended mostly for use with "pure" ClientSummary
	 * objects. Sets ServerResource superclass fields to indicate complete and neither
	 * refreshable nor updateable.
	 */
	public ClientSummary(
			final String name,
			final Date accessed,
			final Date updated,
			final String description,
			final String hostName,
			final String ownerName,
			final String root,
			final ClientLineEnd lineEnd,
			final IClientOptions options,
			final IClientSubmitOptions submitOptions,
			final List<String> alternateRoots,
			final String stream,
			final String type) {

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
		this.type = type;
	}

	public Date getAccessed() {
		return accessed;
	}

	public void setAccessed(Date accessed) {
		this.accessed = accessed;
	}

	public List<String> getAlternateRoots() {
		return this.alternateRoots;
	}

	public void setAlternateRoots(List<String> alternateRoots) {
		this.alternateRoots = alternateRoots;
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

	public ClientLineEnd getLineEnd() {
		return lineEnd;
	}

	public void setLineEnd(ClientLineEnd lineEnd) {
		this.lineEnd = lineEnd;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IClientOptions getOptions() {
		return options;
	}

	public void setOptions(IClientOptions options) {
		this.options = options;
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

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getStream() {
		return stream;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public int getStreamAtChange() {
		return streamAtChange;
	}

	public void setStreamAtChange(int streamAtChange) {
		this.streamAtChange = streamAtChange;
	}

	public IClientSubmitOptions getSubmitOptions() {
		return submitOptions;
	}

	public void setSubmitOptions(IClientSubmitOptions submitOptions) {
		this.submitOptions = submitOptions;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	@Override
	public boolean isUnloaded() {
		return unloaded;
	}

	@Override
	public boolean isStream() {
		if (isNotBlank(stream)) {
			if (isDepotPathSyntax()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String getBackup() {
		return this.backup;
	}

	@Override
	public void setBackup(String backup) {
		this.backup = backup;
	}

	private boolean isDepotPathSyntax() {
		return stream.startsWith("//");
	}
}
