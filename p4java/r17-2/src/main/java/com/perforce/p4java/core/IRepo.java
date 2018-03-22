package com.perforce.p4java.core;

import java.util.Date;

public interface IRepo extends IServerResource {

	/**
	 * Get the repo's name.
	 */
	String getName();

	/**
	 * Get the Perforce user name of the repo's owner.
	 */
	String getOwnerName();

	/**
	 * Get the date the repo was created.
	 */
	Date getCreatedDate();

	/**
	 * Get the date the repo was last pushed.
	 */
	Date getPushedDate();

	/**
	 * Get the description associated with this repo.
	 */
	String getDescription();

	/**
	 * Set the description associated with this repo.
	 *
	 * @param description new repo description string.
	 */
	void setDescription(String description);

	String getForkedFrom();

	void setForkedFrom(String forkedFrom);

	String getDefaultBranch();

	void setDefaultBranch(String defaultBranch);

	String getMirroredFrom();

	void setMirroredFrom(String mirroredFrom);
}
