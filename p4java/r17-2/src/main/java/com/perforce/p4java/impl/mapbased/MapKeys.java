/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased;

/**
 * Helper class containing all known map keys for the map-based
 * implementation as static String fields. Note that the task here is
 * made harder by case and tense variants, etc.; normal names with suffix
 * are the commonest, typically upper-case first character and the
 * rest lower-case.<p>
 * <p>
 * NOTE: not yet in general usage; will be used more with
 * refactoring over time -- HR.
 * <p>
 * FIXME: we need something similar for common map values -- HR.
 */

public class MapKeys {
	// Special characters
	public static final String COLON_LF = ":\n";
	public static final String COLON_SPACE = ": ";
	public static final String CR = "\r";
	public static final String LF = "\n";
	public static final String TAB = "\t";
	public static final String DOUBLE_LF = "\n\n";
	public static final String EMPTY = "";

	// Order of definitions below is NOT significant...
	public static final String ACCESS_KEY = "Access";
	public static final String ACCESSED_KEY = "Accessed";
	public static final String ACTION_KEY = "Action";
	public static final String ADDRESS_KEY = "Address";
	public static final String ADDRESS_LC_KEY = "address";
	public static final String ALTROOTS_KEY = "AltRoots";
	public static final String ARGS_LC_KEY = "args";
	public static final String BRANCH_KEY = "Branch";
	public static final String BRANCH_LC_KEY = "branch";
	public static final String CHANGE_KEY = "Change";
	public static final String CLIENT_KEY = "Client";
	public static final String CLIENT_LC_KEY = "client";
	public static final String CODE_LC_KEY = "code";    // note lower-case first char
	public static final String COMMAND_LC_KEY = "command";    // note lower-case first char
	public static final String DATE_KEY = "Date";
	public static final String DEPOT_KEY = "Depot";
	public static final String DEPTH_LC_KEY = "depth";
	public static final String DESC_LC_KEY = "desc";
	public static final String DESCRIPTION_KEY = "Description";
	public static final String EMAIL_KEY = "Email";
	public static final String EXTRA_LC_KEY = "extra";
	public static final String EXTRATAG_KEY = "extraTag";
	public static final String EXTRATAGTYPE_KEY = "extraTagType";
	public static final String FILES_KEY = "Files";
	public static final String FULLNAME_KEY = "FullName";
	public static final String GROUP_KEY = "Group";
	public static final String GROUP_LC_KEY = "group";
	public static final String HOST_KEY = "Host";
	public static final String HOST_LC_KEY = "host";
	public static final String ID_LC_KEY = "id";    // note lower-case first char
	public static final String IGNORED_KEY = "Ignored";
	public static final String ISOWNER_LC_KEY = "isOwner";
	public static final String ISSUBGROUP_LC_KEY = "isSubGroup";
	public static final String ISUNLOADED_KEY = "IsUnloaded";
	public static final String JOB_KEY = "Job";
	public static final String JOBS_KEY = "Jobs";
	public static final String JOBVIEW_KEY = "JobView";
	public static final String LABEL_KEY = "Label";
	public static final String LABEL_LC_KEY = "label";    // note lower-case first char
	public static final String LINEEND_KEY = "LineEnd";
	public static final String MAP_KEY = "Map";
	public static final String MAP_LC_KEY = "map";
	public static final String MAXLOCKTIME_KEY = "MaxLockTime";
	public static final String MAXLOCKTIME_LC_KEY = "maxLockTime";
	public static final String MAXRESULTS_KEY = "MaxResults";
	public static final String MAXRESULTS_LC_KEY = "maxResults";
	public static final String MAXSCANROWS_KEY = "MaxScanRows";
	public static final String MAXSCANROWS_LC_KEY = "maxScanRows";
	public static final String NAME_KEY = "Name";
	public static final String NAME_LC_KEY = "name";
	public static final String OPTIONS_KEY = "Options";
	public static final String OWNER_KEY = "Owner";
	public static final String OWNER_LC_KEY = "owner";
	public static final String OWNERS_KEY = "Owners";
	public static final String PARENT_KEY = "Parent";
	public static final String PASSWORD_CHANGE_KEY = "PasswordChange";
	public static final String PASSWORD_CHANGE_LC_KEY = "passwordChange";
	public static final String PASSWORD_KEY = "Password";
	public static final String PASSWORD_TIMEOUT_KEY = "PasswordTimeout";
	public static final String PASSWORD_TIMEOUT_LC_KEY = "passTimeout"; // especially egregious, this one...
	public static final String PATHS_KEY = "Paths";
	public static final String PROG_LC_KEY = "prog";
	public static final String PROTECTIONS_KEY = "Protections";
	public static final String REMAPPED_KEY = "Remapped";
	public static final String REVIEWS_KEY = "Reviews";
	public static final String REVISION_KEY = "Revision";
	public static final String ROOT_KEY = "Root";
	public static final String SERVER_NAME_KEY = "ServerName";
	public static final String SERVERID_KEY = "ServerID";
	public static final String SPEC_MAP_KEY = "SpecMap";
	public static final String STATUS_KEY = "Status";
	public static final String STATUS_LC_KEY = "status";    // note lower-case first char
	public static final String STREAM_DEPTH = "StreamDepth";
	public static final String STREAM_KEY = "Stream";
	public static final String STREAMATCHANGE_KEY = "StreamAtChange";
	public static final String SUBGROUPS_KEY = "Subgroups";
	public static final String SUBMITOPTIONS_KEY = "SubmitOptions";
	public static final String SUFFIX_KEY = "Suffix";
	public static final String SUFFIX_LC_KEY = "suffix";
	public static final String TICKET_EXPIRATION = "TicketExpiration";
	public static final String TIME_LC_KEY = "time";    // note lower-case first char
	public static final String TIMEOUT_KEY = "Timeout";
	public static final String TIMEOUT_LC_KEY = "timeout";
	public static final String TRIGGERS_KEY = "Triggers";
	public static final String TYPE_KEY = "Type";
	public static final String TYPE_LC_KEY = "type";
	public static final String UPDATE_KEY = "Update";
	public static final String UPDATED_KEY = "Updated";
	public static final String USER_KEY = "User";
	public static final String USER_LC_KEY = "user";    // note lower-case first char
	public static final String USERS_KEY = "Users";
	public static final String UTF16_LC_KEY = "utf16";
	public static final String VALUE_KEY = "Value";
	public static final String VIEW_KEY = "View";

	public static final String REPO_KEY = "Repo";
	public static final String REPO_NAME_KEY = "RepoName";
	public static final String CREATED_KEY = "Created";
	public static final String PUSHED_KEY = "Pushed";
	public static final String FORKED_FROM_KEY = "ForkedFrom";
	public static final String DEFAULT_BRANCH_KEY = "DefaultBranch";
	public static final String MIRRORED_FROM_KEY = "MirroredFrom";
	public static final String VIEW_DEPOT_TYPE = "ViewDepotType";
}
