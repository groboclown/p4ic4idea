/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.core;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IReviewChangelist;

/**
 * Default implementation of the IReviewChange interface.
 */
public class ReviewChangelist implements IReviewChangelist {

	/** The changelist id. */
	private int changelistId = IChangelist.UNKNOWN;
	
	/** The user's user name. */
	private String user = null;

	/** The user's email address. */
	private String email = null;

	/** The user's real name. */
	private String name = null;

	public ReviewChangelist(int changelistId, String user, String email, String name) {
		if (changelistId <= 0) {
			throw new IllegalArgumentException("Invalid changelist number passed to the ReviewChangelist constructor.");
		}
		if (user == null) {
			throw new IllegalArgumentException("Null user passed to the ReviewChangelist constructor.");
		}
		if (email == null) {
			throw new IllegalArgumentException("Null email passed to the ReviewChangelist constructor.");
		}
		if (name == null) {
			throw new IllegalArgumentException("Null name passed to the ReviewChangelist constructor.");
		}

		this.changelistId = changelistId;
		this.user = user;
		this.email = email;
		this.name = name;
	}

	/**
	 * @see com.perforce.p4java.core.IReviewChangelist#getChangelistId()
	 */
	public int getChangelistId() {
		return changelistId;
	}

	/**
	 * @see com.perforce.p4java.core.IReviewChangelist#setChangelistId(int)
	 */
	public void setChangelistId(int changelistId) {
		this.changelistId = changelistId;
	}

	/**
	 * @see com.perforce.p4java.core.IReviewChangelist#getUser()
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @see com.perforce.p4java.core.IReviewChangelist#setUser(java.lang.String)
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @see com.perforce.p4java.core.IReviewChangelist#getEmail()
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @see com.perforce.p4java.core.IReviewChangelist#setEmail(java.lang.String)
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @see com.perforce.p4java.core.IReviewChangelist#getChangelistId()
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see com.perforce.p4java.core.IReviewChangelist#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}
}
