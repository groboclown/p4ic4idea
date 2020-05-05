/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core;

/**
 * Describes a Perforce review changelist record.<p>
 * 
 * Full semantics can be found in the associated full Perforce p4 command line
 * documentation for the 'p4 review' command.
 */

public interface IReviewChangelist {

	/**
	 * Gets the changelist id.
	 *
	 * @return the changelist id
	 */
	int getChangelistId();

	/**
	 * Sets the changelist id.
	 *
	 * @param changelistId the new changelist id
	 */
	void setChangelistId(int changelistId);

	/**
	 * Gets the user's user name.
	 *
	 * @return the user's user name
	 */
	String getUser();

	/**
	 * Sets the user's user name.
	 *
	 * @param user the user's user name
	 */
	void setUser(String user);

	/**
	 * Gets the user's email address.
	 *
	 * @return the user's email address
	 */
	String getEmail();

	/**
	 * Sets the user's email address.
	 *
	 * @param email the user's email address
	 */
	void setEmail(String email);

	/**
	 * Gets the user's real name.
	 *
	 * @return the user's real name
	 */
	String getName();

	/**
	 * Sets the user's real name
	 *
	 * @param name the user's real name
	 */
	void setName(String name);
}
