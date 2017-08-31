/**
 * 
 */
package com.perforce.p4java.impl.generic.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.impl.mapbased.MapKeys;

/**
 * Default implementation class for IUserSummary interface.
 */

public class UserSummary extends ServerResource implements IUserSummary {

	private String loginName = null;
	private String email = null;
	private String fullName = null;
	private Date access = null;
	private Date update = null;
	protected UserType type = null;
	protected Date ticketExpiration = null;
	protected Date passwordChange = null;
	
	static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
	
	/**
	 * Default constructor; sets all fields to null or zero.
	 */
	public UserSummary() {
	}
	
	/**
	 * Explicit-value constructor.
	 */
	
	public UserSummary(String loginName, String email, String fullName,
			Date access, Date update) {
		this.loginName = loginName;
		this.email = email;
		this.fullName = fullName;
		this.access = access;
		this.update = update;
	}
	
	/**
	 * Explicit-value constructor.
	 */
	
	public UserSummary(String loginName, String email, String fullName,
			Date access, Date update, UserType type) {
		this.loginName = loginName;
		this.email = email;
		this.fullName = fullName;
		this.access = access;
		this.update = update;
		this.type = type;
	}

	/**
	 * Construct a UserSummary from the passed-in map and summaryOnly values.
	 * The map must have been returned from the Perforce server in response
	 * to a getUsers() or getUser (etc.) call; is summaryOnly is true, this
	 * is treated as a map that came from the getUseres method.<p>
	 * 
	 * If map is null, this is equivalent to calling the default constructor.
	 */
	public UserSummary(Map<String, Object> map, boolean summaryOnly) {
		super(!summaryOnly, !summaryOnly);
		
		if (map != null) {
			try{
				this.loginName = (String) map.get("User");
				this.email = (String) map.get("Email");
				this.fullName = (String) map.get("FullName");
				if (map.containsKey(MapKeys.TYPE_KEY)) {
					this.type = UserType.fromString(((String) map.get(MapKeys.TYPE_KEY)).toUpperCase());
				}
				if (summaryOnly) {
					this.update = new Date(Long.parseLong((String) map
							.get(MapKeys.UPDATE_KEY)) * 1000);
					this.access = new Date(Long.parseLong((String) map
							.get(MapKeys.ACCESS_KEY)) * 1000);
					if (map.get(MapKeys.TICKET_EXPIRATION) != null) {
						this.ticketExpiration = new Date(Long.parseLong((String) map
							.get(MapKeys.TICKET_EXPIRATION)) * 1000);
					}
					if (map.get(MapKeys.PASSWORD_CHANGE_KEY) != null) {
						this.passwordChange = new Date(Long.parseLong((String) map
							.get(MapKeys.PASSWORD_CHANGE_KEY)) * 1000);
					}
				} else {
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
					if (map.containsKey(MapKeys.UPDATE_KEY)) {
						this.update = simpleDateFormat
								.parse((String) map.get(MapKeys.UPDATE_KEY));
					}
					if (map.containsKey(MapKeys.ACCESS_KEY)) {
						this.access = simpleDateFormat
								.parse((String) map.get(MapKeys.ACCESS_KEY));
					}
					if (map.get(MapKeys.PASSWORD_CHANGE_LC_KEY) != null) {
						this.passwordChange = simpleDateFormat
								.parse((String) map.get(MapKeys.PASSWORD_CHANGE_LC_KEY));
					}
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception in UserSummary constructor: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#getAccess()
	 */
	public Date getAccess() {
		return this.access;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#getEmail()
	 */
	public String getEmail() {
		return this.email;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#getFullName()
	 */
	public String getFullName() {
		return this.fullName;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#getLoginName()
	 */
	public String getLoginName() {
		return this.loginName;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#getUpdate()
	 */
	public Date getUpdate() {
		return this.update;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#setAccess(java.util.Date)
	 */
	public void setAccess(Date access) {
		this.access = access;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#setEmail(java.lang.String)
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#setFullName(java.lang.String)
	 */
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#setLoginName(java.lang.String)
	 */
	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#setUpdate(java.util.Date)
	 */
	public void setUpdate(Date update) {
		this.update = update;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#getType()
	 */
	public UserType getType() {
		return type;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#setType(com.perforce.p4java.core.IUserSummary.UserType)
	 */
	public void setType(UserType type) {
		this.type = type;
	}


	/**
	 * @see com.perforce.p4java.core.IUserSummary#getTicketExpiration()
	 */
	public Date getTicketExpiration() {
		return ticketExpiration;
	}

	/**
	 * @see com.perforce.p4java.core.IUserSummary#getPasswordChange()
	 */
	public Date getPasswordChange() {
		return passwordChange;
	}
}
