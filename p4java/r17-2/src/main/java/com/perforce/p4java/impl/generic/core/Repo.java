package com.perforce.p4java.impl.generic.core;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IRepo;

import java.util.Date;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.common.base.StringHelper.firstNonBlank;
import static com.perforce.p4java.impl.mapbased.MapKeys.*;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substring;

public class Repo extends ServerResource implements IRepo {

	private String name = null;
	private String ownerName = null;
	private Date createdDate = null;
	private Date pushedDate = null;
	private String forkedFrom = null;
	private String description = null;
	private String defaultBranch = null;
	private String mirroredFrom = null;

	public Repo() {
		super(false, false);
	}

	public Repo(final Map<String, Object> repoMap) {
		super(false, false);

		if (nonNull(repoMap)) {
			try {
				name = firstNonBlank(parseString(repoMap, NAME_LC_KEY), parseString(repoMap, REPO_KEY));

				ownerName = firstNonBlank(parseString(repoMap, OWNER_LC_KEY), parseString(repoMap, OWNER_KEY));

				if (nonNull(repoMap.get(CREATED_KEY))) {
					createdDate = new Date(parseLong(repoMap, CREATED_KEY) * 1000);
				}
				if (nonNull(repoMap.get(PUSHED_KEY))) {
					pushedDate = new Date(parseLong(repoMap, PUSHED_KEY) * 1000);
				}

				forkedFrom = parseString(repoMap, FORKED_FROM_KEY);

				description = firstNonBlank(parseString(repoMap, DESC_LC_KEY), parseString(repoMap, DESCRIPTION_KEY));
				if (isNotBlank(description) && (description.length() > 1) && endsWith(description, "\n")) {
					description = substring(description, 0, description.length() - 1);
				}

				defaultBranch = parseString(repoMap, DEFAULT_BRANCH_KEY);

				mirroredFrom = parseString(repoMap, MIRRORED_FROM_KEY);

			} catch (Throwable thr) {
				Log.error("Unexpected exception in Repo constructor: %s", thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
	}

	public Repo(final String name, final Date createdDate, final Date pushedDate) {
		this(name, null, createdDate, pushedDate, null);
	}

	public Repo(final String name, final String ownerName, final Date createdDate, final Date pushedDate, final String description) {
		this.name = name;
		this.ownerName = ownerName;
		this.createdDate = createdDate;
		this.pushedDate = pushedDate;
		this.description = description;
	}

	/**
	 * Get the repo's name.
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Get the Perforce user name of the repo's owner.
	 */
	@Override
	public String getOwnerName() {
		return ownerName;
	}

	/**
	 * Get the date the repo was created.
	 */
	@Override
	public Date getCreatedDate() {
		return createdDate;
	}

	/**
	 * Get the date the repo was last pushed.
	 */
	@Override
	public Date getPushedDate() {
		return pushedDate;
	}

	/**
	 * Get the description associated with this repo.
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description associated with this repo.
	 *
	 * @param description new repo description string.
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getForkedFrom() {
		return forkedFrom;
	}

	@Override
	public void setForkedFrom(String forkedFrom) {
		this.forkedFrom = forkedFrom;
	}

	@Override
	public String getDefaultBranch() {
		return defaultBranch;
	}

	@Override
	public void setDefaultBranch(String defaultBranch) {
		this.defaultBranch = defaultBranch;
	}

	@Override
	public String getMirroredFrom() {
		return mirroredFrom;
	}

	@Override
	public void setMirroredFrom(String mirroredFrom) {
		this.mirroredFrom = mirroredFrom;
	}
}
