/**
 * 
 */
package com.perforce.p4java.impl.generic.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.server.IServer;

/**
 * Default implementation of the IChangelistSummary interface.
 */

public class ChangelistSummary extends ServerResource implements IChangelistSummary {
	
	protected static final String CHANGE_KEY = "Change";
	protected static final String NEW_KEY = "new";
	protected static final String CLIENT_KEY = "Client";
	protected static final String USER_KEY = "User";
	protected static final String STATUS_KEY = "Status";
	protected static final String DATE_KEY = "Date";
	protected static final String DESCRIPTION_KEY = "Description";
	protected static final String JOBS_KEY = "Jobs";
	protected static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
	
	protected int id = IChangelist.UNKNOWN;
	protected String clientId = null;
	protected String username = null;
	protected ChangelistStatus status = null;
	protected Date date = null;
	protected String description = null;
	protected boolean shelved = false;
	protected Visibility visibility = null;
	
	/**
	 * Default constructor -- sets all fields to false or null, id to
	 * IChangelist.UNKNOWN, and calls the default ServerResource constructor.
	 */
	public ChangelistSummary() {
	}
	
	/**
	 * Explicit-value pass-through constructor for the ServerResource fields. Usually
	 * used by IChangelistSummary extensions.
	 */
	public ChangelistSummary(boolean complete, boolean completable,
			boolean refreshable, boolean updateable, IServer server) {
		super(refreshable, updateable, server);
	}
	
	/**
	 * Explicit-value constructor; calls the default ServerResource constructor.
	 */
	public ChangelistSummary(int id, String clientId, String username,
			ChangelistStatus status, Date date, String description,
			boolean shelved) {
		this.id = id;
		this.clientId = clientId;
		this.username = username;
		this.status = status;
		this.date = date;
		this.description = description;
		this.shelved = shelved;
	}
	
	/**
	 * Construct a new ChangelistSummary from the passed-in summary. If summary
	 * is null, this is equivalent to calling the default constructor.
	 */
	
	public ChangelistSummary(IChangelistSummary summary) {
		if (summary != null) {
			this.id = summary.getId();
			this.clientId = summary.getClientId();
			this.username = summary.getUsername();
			this.status = summary.getStatus();
			this.date = summary.getDate();
			this.description = summary.getDescription();
			this.shelved = summary.isShelved();
		}
	}
	
	/**
	 * Convenience constructor, equivalent to this(map, summaryOnly, null).
	 */
	public ChangelistSummary(Map<String, Object> map, boolean summaryOnly) {
		this(map, summaryOnly, null);
	}
	
	/**
	 * Construct a ChangelistSummary from a suitable map returned from
	 * the Perforce server. If map is null, this is equivalent to calling
	 * the default constructor; otherwise, if summaryOnly is true, the map
	 * is assumed to come from a "p4 changes" command and processed accordingly,
	 * with the superclass ServerResource fields set accordingly; otherwise
	 * the map is assumed to come from a full changelist command and the
	 * superclass fields are also set appropriately for the full changelist.
	 * The server parameter is ignored for summaryOnly objects.<p>
	 * 
	 * Note that map keys returned from the Perforce server are sometimes different
	 * for summary fields and full fields, so you have to be clear about where the
	 * map came from to get accurate results.
	 */
	public ChangelistSummary(Map<String, Object> map, boolean summaryOnly, IServer server) {
		super();
		if (map != null) {
			if (summaryOnly) {
				try {
					// Note use of lower-case keys here; this is the only
					// place lower-case fields are used for this...
					
					this.id = new Integer((String) map.get("change"));
					this.clientId = (String) map.get("client");
					this.username = (String) map.get("user");
					this.status = ChangelistStatus.fromString((String) map.get("status"));
					this.date = ((String) map.get("time") == null ?
							null : new Date(Long.parseLong((String) map.get("time")) * 1000));
					this.description = (String) map.get("desc");
					this.shelved = map.containsKey("shelved");
					if (map.containsKey("changeType")) {
						this.visibility = Visibility.fromString(((String) map.get("changeType")).toUpperCase());
					}
				} catch (Throwable thr) {
					Log.error("Unexpected exception in ChangelistSummary constructor: "
							+ thr.getLocalizedMessage());
					Log.exception(thr);
				}
			} else {
				this.server = server;
				this.refreshable = true;
				this.updateable = true;
				
				try {
					String idString = (String) map.get(CHANGE_KEY);

					if ((idString != null) && (idString.equalsIgnoreCase(NEW_KEY))) {
						this.id = IChangelist.DEFAULT;
					} else {
						try {
							this.id = new Integer(idString);
						} catch (Exception exc) {
							Log.exception(exc);
							this.id = IChangelist.UNKNOWN;
						}
					}
					this.clientId = (String) map.get(CLIENT_KEY);
					this.username = (String) map.get(USER_KEY);
					this.status = ChangelistStatus.fromString((String) map.get(STATUS_KEY));
					
					// Note that this is about the only place that Perforce sends
					// an actual formatted date string back; everywhere else it's
					// a long; here it's in the yyyy/mm/dd hh:mm:ss format -- HR.
					
					String dateStr = (String) map.get(DATE_KEY);
					if (dateStr == null) {
						this.date = new Date();
					} else {
						try {
							this.date = new SimpleDateFormat(DATE_FORMAT).parse(dateStr);
						} catch (ParseException pe) {
							Log.error("Date parse error in Changelist constructor: "
									+ pe.getLocalizedMessage());
						}
					}
					
					this.description = (String) map.get(DESCRIPTION_KEY);
					if (map.containsKey("Type")) {
						this.visibility = Visibility.fromString(((String) map.get("Type")).toUpperCase());
					}
				} catch (Throwable thr) {
					Log.error("Unexpected exception in ChangelistSummary constructor: "
							+ thr.getLocalizedMessage());
					Log.exception(thr);
				}
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#getId()
	 */
	public int getId() {
		return id;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#setId(int)
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#getClientId()
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#setClientId(java.lang.String)
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#getUsername()
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#setUsername(java.lang.String)
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#getStatus()
	 */
	public ChangelistStatus getStatus() {
		return status;
	}

	public void setStatus(ChangelistStatus status) {
		this.status = status;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#getDate()
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#setDate(java.util.Date)
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#getDescription()
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#setDescription(java.lang.String)
	 */
	public String setDescription(String description) {
		String oldDesc = this.description;
		this.description = description;
		return oldDesc;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#isShelved()
	 */
	public boolean isShelved() {
		return shelved;
	}

	/**
	 * @see com.perforce.p4java.core.IChangelistSummary#setShelved(boolean)
	 */
	public void setShelved(boolean shelved) {
		this.shelved = shelved;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}
}
