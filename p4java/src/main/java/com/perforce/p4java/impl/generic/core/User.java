/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core;

import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IReviewSubscription;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServer;

/**
 * Simple default IUser implementation class.
 */

public class User extends UserSummary implements IUser {
	
	private String password = null;
	private String jobView = null;
	private ViewMap<IReviewSubscription> reviewSubscriptions = null;
	
	/**
	 * Simple convenience factory method to create a new local User object
	 * with default (null) jobView and reviewSubscriptions fields.
	 * 
	 * @param name non-null user name.
	 * @param email user's email address.
	 * @param fullName user's full name.
	 * @param password user's password (usually ignored).
	 * @return new local user object.
	 */
	public static User newUser(String name, String email, String fullName, String password) {
		return new User(name, email, fullName, null, null, password, null, null);
	}
	
	/**
	 * Default constructor -- sets all summary and extended
	 * fields to null.
	 */
	public User() {
	}

	/**
	 * Explicit-value constructor.
	 */
	
	public User(String loginName, String email, String fullName,
			Date access, Date update, String password, String jobView,
			ViewMap<IReviewSubscription> reviewSubscriptions) {
		super(loginName, email, fullName, access, update);
		this.password = password;
		this.jobView = jobView;
		this.reviewSubscriptions = reviewSubscriptions;
	}
	
	/**
	 * Explicit-value constructor.
	 */
	
	public User(String loginName, String email, String fullName,
			Date access, Date update, String password, String jobView,
			UserType type,
			ViewMap<IReviewSubscription> reviewSubscriptions) {
		super(loginName, email, fullName, access, update, type);
		this.password = password;
		this.jobView = jobView;
		this.reviewSubscriptions = reviewSubscriptions;
	}

	/**
	 * Constructs a User from the passed-in map. The map
	 * is assumed to come from a suitable call on IServer; if
	 * the map is null this is equivalent to calling the
	 * default constructor.
	 */
	public User(Map<String, Object> map, IServer server) {
		super(map, false);
		this.server = server;
		if (map != null) {
			final String JOBVIEW_KEY = "JobView";
			final String PASSWORD_KEY = "Password";	// FIXME: check this... -- HR.
			final String REVIEW_KEY_PFX = "Reviews";

			// Only concerned about the Reviews*, JobView, and Password
			// keys here:
			
			try {
				this.jobView = (String) map.get(JOBVIEW_KEY);
				this.password = (String) map.get(PASSWORD_KEY);
				
				for (int i = 0; map.containsKey(REVIEW_KEY_PFX + i); i++) {
					if (this.reviewSubscriptions == null) {
						this.reviewSubscriptions = new ViewMap<IReviewSubscription>();
					}
					this.reviewSubscriptions.addEntry(
							new ReviewSubscription(i, (String) map.get(REVIEW_KEY_PFX + i)));
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception in User constructor: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.IUser#getPassword()
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @see com.perforce.p4java.core.IUser#setPassword(java.lang.String)
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @see com.perforce.p4java.core.IUser#getJobView()
	 */
	public String getJobView() {
		return jobView;
	}

	/**
	 * @see com.perforce.p4java.core.IUser#setJobView(java.lang.String)
	 */
	public void setJobView(String jobView) {
		this.jobView = jobView;
	}

	/**
	 * @see com.perforce.p4java.core.IUser#getReviewSubscriptions()
	 */
	public ViewMap<IReviewSubscription> getReviewSubscriptions() {
		return reviewSubscriptions;
	}

	/**
	 * @see com.perforce.p4java.core.IUser#setReviewSubscriptions(com.perforce.p4java.core.ViewMap)
	 */
	public void setReviewSubscriptions(ViewMap<IReviewSubscription> reviewSubscriptions) {
		this.reviewSubscriptions = reviewSubscriptions;
	}
	
	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#refresh()
	 */
	public void refresh() throws ConnectionException, RequestException,
										AccessException {
		IServer refreshServer = this.server;
		String refreshName = this.getLoginName();
		if (refreshServer != null && refreshName != null) {
			IUser refreshedUser = refreshServer.getUser(refreshName);
			if (refreshedUser != null) {
				this.setLoginName(refreshedUser.getLoginName());
				this.setEmail(refreshedUser.getEmail());
				this.setAccess(refreshedUser.getAccess());
				this.setUpdate(refreshedUser.getUpdate());
				this.setFullName(refreshedUser.getFullName());
				this.setJobView(refreshedUser.getJobView());
				this.setPassword(refreshedUser.getPassword());
				this.setReviewSubscriptions(refreshedUser.getReviewSubscriptions());
			}
		}
	}
	
	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update()
	 */
	public void update()
					throws ConnectionException, RequestException, AccessException {
		this.server.updateUser(this, false);
	}
	
	public void update(boolean force) throws ConnectionException, RequestException, AccessException {
		this.server.updateUser(this, true);
	}
}
